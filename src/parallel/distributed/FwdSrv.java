package parallel.distributed;

import parallel.*;
import java.io.*;
import java.net.*;
import java.util.*;


/**
 * class implements a forwarding server. It receives objects from long-lived
 * client socket connections (by default listening on port 8901) or from 
 * short-lived client socket connections (by default listening on port 8902), 
 * and forwards them to a single server (synchronizing on the socket channel in 
 * the case of requests arriving from long-lived connections, and asynchronously
 * by opening new socket connections to the single server in the case of client
 * requests arriving from short-lived socket connections). The usefulness of the
 * FwdSrv is exactly the overhead of indirection it adds to the client/server
 * topology: by having several FwdSrv servers connected to the same central
 * server, and clients connecting to the forwarding servers instead of the 
 * central one, many more clients may be connected to the central server. Also,
 * notice the use of socket pools to improve resource utilization; assuming the
 * central server allows long-lived connections (i.e. the central server does 
 * not itself close a connection after it has received and responded to the 
 * first request arriving on this connection), the FwdSrv acts as a short-lived
 * connections multi-plexer, maintaining a few sockets to the central server 
 * that can be re-used by short-lived client connections. The FwdSrv is agnostic
 * to the central server that will be forwarding requests to or the clients that
 * will be connecting to it, or the kind of request/response objects that it 
 * will be forwarding. It only requires that for every request it forwards, it
 * must receive a response from the central server to send back to the client.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class FwdSrv {
	private static int _longLivedConnPort=8901;
	private static int _shortLivedConnPort=8902;
	private static String _host2Fwd="localhost";
	private static int _port2Fwd=-1;
	private static Socket _forwardSocket;  // for long-lived fwd connections
	private static ObjectInputStream _forwardOIS;  // for long-lived fwd conns
	private static ObjectOutputStream _forwardOOS;  // for long-lived fwd conns
	private static List _freeFwdConns = new ArrayList();  // List<SocketIOS s> is a cache
	private static final int _MAX_WORKING_SHORT_LIVED_CONNS=1000;
	private static int _numWorkingFwdConns=0;
	private static final int _MAX_FREE_SHORT_LIVED_FWD_CONNS=10;  // cache-size of socket connections to the central server
	private static final Object _sync = new Object();  // used for synchronizing long-lived conns
	private static DynamicAsynchTaskExecutor _executor;  // used for short-lived conns only

	
	/**
	 * invoke as:
	 * <CODE>java -cp &lt;classpath&gt; parallel.distributed.FwdSrv [forwardinghost(localhost):]&lt;forwardingport&gt; [long-lived-conns-port(8901)] [short-lived-conns-port(8902)]</CODE>
	 * @param args 
	 */
	public static void main(String[] args) {
		StringTokenizer st = new StringTokenizer(args[0],":");
		if (st.countTokens()>=2) {
			_host2Fwd = st.nextToken();
		}
		_port2Fwd = Integer.parseInt(st.nextToken());
		try {
			_forwardSocket = new Socket(_host2Fwd, _port2Fwd);
			_forwardOOS = new ObjectOutputStream(_forwardSocket.getOutputStream());
			_forwardOOS.flush();
			_forwardOIS = new ObjectInputStream(_forwardSocket.getInputStream());
		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("FwdSrv: couldn't create socket connection to "+
				                 _host2Fwd+" on port "+_port2Fwd);
			System.exit(-1);
		}
		if (args.length>1) {
			_longLivedConnPort = Integer.parseInt(args[1]);
		}
		if (args.length>2) {
			_shortLivedConnPort = Integer.parseInt(args[2]);
		}
		try {
			_executor = DynamicAsynchTaskExecutor.newDynamicAsynchTaskExecutor(2, _MAX_WORKING_SHORT_LIVED_CONNS);
		}
		catch (ParallelException e) {
			e.printStackTrace();
			System.err.println("FwdSrv: couldn't start ");
		}
		startLongLivedConnectionsHandlerThread();
		startShortLivedConnectionsHandlerThread();
	}
	
	
	/**
	 * creates and starts a new <CODE>LLCHdlrThread</CODE>.
	 */
	private static void startLongLivedConnectionsHandlerThread() {
		LLCHdlrThread llcthread = new LLCHdlrThread();
		llcthread.start();
	}

	
	/**
	 * creates and starts a thread whose <CODE>run()</CODE> method creates a 
	 * server-socket listening on <CODE>_shortLivedConnPort</CODE> that listens
	 * for incoming socket connections (assuming there are no more than 
	 * <CODE>_MAX_WORKING_SHORT_LIVED_CONNS</CODE> concurrently running; otherwise
	 * waits until the number of such connections drops below this threshold). For
	 * every connection it accepts, starts a new SLConnThread to handle it.
	 */
	private static void startShortLivedConnectionsHandlerThread() {
		Thread slcthread = new Thread(new Runnable() {
		  public void run() {
				try {
					ServerSocket ss = new ServerSocket(_shortLivedConnPort);
					while (true) {
						Socket s = ss.accept();
						// wait until the number of concurrent working conns has dropped enough
						synchronized (FwdSrv.class) {
							while (_numWorkingFwdConns>_MAX_WORKING_SHORT_LIVED_CONNS) {
								try {
									FwdSrv.class.wait();
								}
								catch (InterruptedException e) {
									Thread.currentThread().interrupt();
								}
							}
							++_numWorkingFwdConns;
						}
						SLConnRunnable slr = new SLConnRunnable(s);
						_executor.execute(slr);
					}
				}
				catch (Exception e) {
					// e.printStackTrace();
					System.err.println("FwdSrv: short-lived connections handler exits.");
				}
			}
		});
		slcthread.start();
	}
	

	/**
	 * returns a <CODE>SocketIOS</CODE> object containing a socket connection to 
	 * the central server, together with its i/o streams. First, looks up the 
	 * cache of free socket connections to see if a free connection exists, and 
	 * if not, it creates and returns a new one.
	 * @return SocketIOS 
	 */
	private static synchronized SocketIOS getForwardingSocket() {
		int sz = _freeFwdConns.size();
		if (sz>0) {
			SocketIOS freesock=null;
			// find an open free socket
			for(; sz>0; sz--) {
			  freesock = (SocketIOS) _freeFwdConns.remove(sz-1);
				if (freesock._s==null || freesock._s.isClosed()) {
					freesock._ois=null;
					freesock._oos=null;
				} else break;  // found a free one
			}
			// remove free connections above the threshold
			if (sz>_MAX_FREE_SHORT_LIVED_FWD_CONNS+1) {
				for (int i=sz-2; i>=_MAX_FREE_SHORT_LIVED_FWD_CONNS;i--) {
					SocketIOS si = (SocketIOS) _freeFwdConns.remove(i);
					try {
						if (!si._s.isClosed()) {
							si._s.shutdownOutput();
							si._s.close();
						}
						si._ois=null;
						si._oos=null;
					}
					catch (IOException e) {
						// silently ignore
					}
				}
			}
			if (freesock!=null) {
				return freesock;
			}
		}
		// OK, now create a new socket and return it
		try {
			Socket s = new Socket(_host2Fwd,_port2Fwd);
			ObjectOutputStream fos = new ObjectOutputStream(s.getOutputStream());
			fos.flush();
			ObjectInputStream fis = new ObjectInputStream(s.getInputStream());
			SocketIOS sios = new SocketIOS(s,fis,fos);
			return sios;
		}
		catch (Exception e) {
			return null;
		}
	}
	
	
	/**
	 * releases the socket connection (together with its i/o streams) encapsulated
	 * in the parameter. If there are only a few connections to the server in the
	 * cache, then instead of releasing the connection, it adds it to the cache of
	 * free connections to the central server.
	 * @param fsios SocketIOS
	 */
	private synchronized static void releaseForwardingSocket(SocketIOS fsios) {
		final int sz = _freeFwdConns.size();
		if (sz<_MAX_FREE_SHORT_LIVED_FWD_CONNS) {
			_freeFwdConns.add(fsios);
		}
		else {  // release this connection
			try {
				fsios._s.shutdownOutput();
				fsios._s.close();
				fsios._ois=null;
				fsios._oos=null;
			}
			catch (Exception e) {
				// silently ignore
			}
		}
	}
 	
	
	/**
	 * auxiliary inner-class is the Runnable that handles short-lived client 
	 * connections. Its <CODE>run()</CODE> method gets a connection to the 
	 * central server (by calling <CODE>FwdSrv.getForwardingSocket()</CODE>), 
	 * reads an object from the client socket connection, sends the object to the 
	 * central server, gets back a response, forwards the response to the client, 
	 * releases the connection to the central server by calling 
	 * <CODE>FwdSrv.releaseForwardingSocket()</CODE>, shuts-down the client socket
	 * and returns.
	 */
	private static class SLConnRunnable implements Runnable {
		private final Socket _s;
		private final ObjectInputStream _ois;
		private final ObjectOutputStream _oos;
		
		/**
		 * single constructor.
		 * @param s Socket
		 * @throws IOException if initializing the client connection fails. 
		 */
		SLConnRunnable(Socket s) throws IOException {
			_s = s;
			_oos = new ObjectOutputStream(_s.getOutputStream());
			_oos.flush();
			_ois = new ObjectInputStream(_s.getInputStream());
		}

		
		/**
		 * the run method of the thread.
		 */
		public void run() {
			SocketIOS fsios=null;
			ObjectInputStream fois=null;
			ObjectOutputStream foos=null;			
			try {
				// first, open connection to server
				fsios = getForwardingSocket();
				fois = fsios._ois;
				foos = fsios._oos;
				// read request from client
				Object req = _ois.readObject();
				// forward request to server
				foos.writeObject(req);
				foos.flush();
				// read response from server
				Object res = fois.readObject();
				// forward response to client
				_oos.writeObject(res);
				_oos.flush();
			}
			catch (Exception e) {
				System.err.println("SLConnThread: exception caught, closing socket.");
			}
			finally {
				// reduce the number of working conns
				synchronized (FwdSrv.class) {
					if (--_numWorkingFwdConns<_MAX_WORKING_SHORT_LIVED_CONNS)
						FwdSrv.class.notifyAll();  // notify any waiting threads on this resource
				}
				if (fsios!=null) {
					releaseForwardingSocket(fsios);
				}
				if (_s!=null) {
					try {
						_s.shutdownOutput();
						_s.close();
					}
					catch (IOException e) {
						// silently ignore
					}
				}
			}
		}
	}


	/**
	 * auxiliary inner-class is the thread that handles long-lived client 
	 * connections. Its <CODE>run()</CODE> loop reads an object from the client 
	 * socket connection, sends the object to the central server atomically via 
	 * the <CODE>_forwardSocket</CODE> connection, gets back a response, and
	 * forwards the response to the client. If the connection to the socket is
	 * lost (because maybe the central server closed the connection), it attempts
	 * to re-open it; if it fails, shuts-down the long-lived connections request
	 * forwarding mechanism.
	 */
	private static class LLConnThread extends Thread {
		private final Socket _s;
		private final ObjectInputStream _ois;
		private final ObjectOutputStream _oos;
		
		/**
		 * single constructor.
		 * @param s Socket
		 * @throws IOException if socket initialization to client fails. 
		 */
		LLConnThread(Socket s) throws IOException {
			_s = s;
			_oos = new ObjectOutputStream(_s.getOutputStream());
			_oos.flush();
			_ois = new ObjectInputStream(_s.getInputStream());
		}

		
		/**
		 * the run-loop of the thread.
		 */
		public void run() {
			while (true) {
				synchronized (_sync) {
					if (_forwardSocket==null || _forwardSocket.isClosed()) {
						// try to re-establish connection
						// if attempt fails, quit and stop accepting connections on the long-
						// lived connections port
						try {
							_forwardSocket.shutdownOutput();
							_forwardSocket.close();
						}
						catch (Exception e) {
							// silently fails
						}
						try {
							_forwardSocket = new Socket(_host2Fwd,_port2Fwd);
							_forwardOOS = new ObjectOutputStream(_forwardSocket.getOutputStream());
							_forwardOOS.flush();
							_forwardOIS = new ObjectInputStream(_forwardSocket.getInputStream());
						}
						catch (Exception e) {
							System.err.println("FwdSrv: long-lived connection to fwd-server lost and cannot be re-established"+
								                 ", will stop accepting long-lived connections from clients");
							LLCHdlrThread.stopLLCHdlrThread();
						}
					}
				}  // end synchronized block
				try {
					// read object from client
					Object req = _ois.readObject();
					Object res = null;
					// forward object to fwd-server, then read response atomically
					synchronized (_sync) {
						try {
							_forwardOOS.writeObject(req);
							_forwardOOS.flush();
							res = _forwardOIS.readObject();
						}
						catch (Exception e) {
							System.err.println("FwdSrv: long-lived connection to fwd-server lost, shutting down");
							try {
								try {
									_forwardSocket.shutdownOutput();
									_forwardSocket.close();
								}
								catch (Exception e2) {
									// silent
								}
								LLCHdlrThread.stopLLCHdlrThread();
								_s.shutdownOutput();
								_s.close();
								return;
							}
							catch (Exception e3) {
								// silently ignore
							}
						}  // end catch clause sending/retrieving data from fwd-server
					}
					// send response back to client
					_oos.writeObject(res);
					_oos.flush();
				}
				catch (Exception e) {
					System.err.println("connection to client lost, shutting down");
					try {
					_s.shutdownOutput();
					_s.close();
					}
					catch (Exception e2) {
						// silent
					}
					return;
				}
			}  // while true
		}
	}
	
	
	/**
	 * auxiliary inner-class starts a server-socket that listens for incoming
	 * client connections, and for each connection, starts an 
	 * <CODE>LLConnThread</CODE> thread to handle it. Notice that since we are
	 * dealing with long-lived connection clients, it makes little sense to use
	 * an executor (thread-pool) as each client connection will likely remain 
	 * running for very long times, and thus each thread created for handling a
	 * client connection is not likely to be reusable.
	 */	
	private static class LLCHdlrThread extends Thread {
		private static boolean _cont=true;
		
		public void run() {
			try {
				ServerSocket ss = new ServerSocket(_longLivedConnPort);
				do {
					Socket s = ss.accept();
					LLConnThread llt = new LLConnThread(s);
					llt.start();
				} while (getCont());
			}
			catch (Exception e) {
				// e.printStackTrace();
			}
			System.err.println("FwdSrv: long-lived connections handler exits.");
		}
		
		static synchronized boolean getCont() { return _cont; }
		static synchronized void stopLLCHdlrThread() { _cont=false; }
	}
		
	
	/**
	 * auxiliary inner-class representing a data-structure holding a socket and
	 * its associated i/o streams.
	 */
	private static class SocketIOS {
		private final Socket _s;
		private ObjectInputStream _ois;
		private ObjectOutputStream _oos;
		SocketIOS(Socket s, ObjectInputStream ois, ObjectOutputStream oos) {
			_s = s;
			_ois=ois;
			_oos=oos;
		}
	}
}
