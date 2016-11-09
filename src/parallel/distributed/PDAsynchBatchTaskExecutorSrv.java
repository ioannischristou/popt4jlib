package parallel.distributed;

import parallel.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Server class allows clients or workers to connect to this process and submit
 * TaskObjects for asynchronous processing to any of the available workers in 
 * the network of connected workers. A worker process connects to this process 
 * on default port 7980 and by doing so declares itself available for processing 
 * an array of <CODE>TaskObject</CODE> objects, encapsulated in a
 * <CODE>TaskObjectsAsynchExecutionRequest</CODE>. The server then expects 
 * clients to connect to its clients-listening port (default 7981) for requests
 * to serve.
 * The server may also become itself client to another server in the network,
 * and if this is the case, then, whenever another client submits a request,
 * if the workers connected to this server are all busy, it will try submitting
 * the request to the other server to which it is a client (unless
 * the other server is also the client that originated or forwarded the request),
 * until it gets a response. This ability means that servers can (and should)
 * form ring topologies, instead of simple chains.
 * Notice that in this implementation, if a worker fails to run a batch job, 
 * it is removed from the pool of available workers, and the connection to it is 
 * closed. For details see the method
 * <CODE>PDAsynchBTEWListener.runObject(TaskObjectsAsynchExecutionRequest req)</CODE>.
 * Further, the server can be started with the option of sending to all workers
 * connecting to it, an "initialization command", which assuming the workers are
 * also run with the option of accepting first this init-cmd, they will run it
 * before starting their thread-pool (so that the initialization is known to the
 * threads in the thread-pool). This initialization command, in the form of an
 * <CODE>PDAsynchInitCmd</CODE>, will have to arrive from a client connected to  
 * the server before any worker has connected to this server (this is contrary 
 * to the design of <CODE>PDBTExec[Inited[Clt|Wrk]|SingleCltWrkInitSrv]</CODE> 
 * classes where the workers and single client may connect to the server in any
 * order.) Any worker connecting to the server before a client has specified the
 * initialization command, the worker will pause until a client specifies the 
 * init-cmd, which is then sent to the worker; during this time, the server does
 * not respond to worker incoming socket connection requests.
 * The full Computing Policies are as follows:
 * If a worker connection is lost during processing a batch of tasks, the batch
 * will be re-submitted once more to the next available worker, as soon as such
 * a worker becomes available. Similarly, if a worker fails to process a batch
 * of tasks and returns a <CODE>FailedReply</CODE> object back to this server,
 * the server will attempt one more time to re-submit the batch to another 
 * worker as soon as such a worker becomes available and drops its connection 
 * from this "loser" worker. If the same batch of jobs fails to be executed by 
 * two different workers, the server sends back to the client that submitted the 
 * job, a <CODE>FailedReply</CODE> to indicate the job cannot be successfully 
 * completed.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class PDAsynchBatchTaskExecutorSrv {
  private HashMap _workers;  // map<Socket s, PDAsynchBTEListener listener>
  private int _workersPort = 7980;  // default port
  private int _clientsPort = 7981;  // default port
  private static final int _NUM_ATTEMPTS = 10;  // num attempts to iterate over
                                                // available worker connections
                                                // to try to find an idle one.
	private static final int _MAX_WIN_POLL_ATTEMPTS = 50;  // num workers to poll 
	                                                       // after a free one has
	                                                       // been found
  private static PDAsynchBatchTaskExecutorClt _otherKnownServer = null;
	// next variables indicate if an init-cmd should be sent to workers upon
	// connecting to the server
	private static PDAsynchInitCmd _initCmd = null;
	private static boolean _sendInitCmd = false;
	private static final Object _sync = new Object();

	
  /**
   * sole constructor.
   * @param wport int the port workers (PDAsynchBatchTaskExecutorWrk) connect to.
   * @param cport int the port clients (PDAsynchBatchTaskExecutorClt) connect to.
   */
  private PDAsynchBatchTaskExecutorSrv(int wport, int cport) {
    _workers = new HashMap();
    _workersPort = wport;
    _clientsPort = cport;
  }


  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; parallel.distributed.PDAsynchBatchTaskExecutorSrv [workers_port(7980)] [clients_port(7981)] [send_init_cmd(false)] [other_server_ip_address,otherserver_ip_port] </CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    int wport = 7980;  // default port
    int cport = 7981;
    if (args.length>0) {
      try {
        wport = Integer.parseInt(args[0]);
      }
      catch (Exception e) {
        e.printStackTrace();
        usage();
        System.exit(-1);
      }
      if (args.length>1) {
        try {
          cport = Integer.parseInt(args[1]);
        }
        catch (Exception e) {
          e.printStackTrace();
          usage();
          System.exit(-1);
        }
      }
    }
    PDAsynchBatchTaskExecutorSrv server = new PDAsynchBatchTaskExecutorSrv(wport, cport);
		if (args.length>2) {
			try {
				_sendInitCmd = Boolean.parseBoolean(args[2]);
			}
			catch (Exception e) {
				e.printStackTrace();
				usage();
				System.exit(-1);
			}
		}
    if (args.length>3) {
      try {
        for (int i = 3; i < args.length; i += 2) {
          String other_host_name = args[i];
          int other_host_port = Integer.parseInt(args[i + 1]);
          PDAsynchBatchTaskExecutorSrv.setSingleOtherServer(other_host_name, other_host_port);
        }
      }
      catch (Exception e) {
        e.printStackTrace();
        usage();
        System.exit(-1);
      }
    }
    try {
      server.run();
    }
    catch (Exception e) {
      e.printStackTrace();
      System.err.println("Server exits due to exception.");
    }
  }


  private void run() throws IOException {
    AsynchWThread workersListeningThread = new AsynchWThread(_workersPort);
    workersListeningThread.start();
    AsynchCThread clientsListeningThread = new AsynchCThread(_clientsPort);
    clientsListeningThread.start();
  }
	
	
	private static PDAsynchInitCmd getInitCmd() {
		synchronized (_sync) {
			return _initCmd;
		}
	}
	private static void setInitCmd(PDAsynchInitCmd cmd) {
		synchronized (_sync) {
			_initCmd = cmd;
			_sync.notifyAll();
		}
	}


  /**
   * may only be called once from <CODE>main(args)</CODE> at startup.
   * @param host String
   * @param port int
	 * @throws IllegalStateException if method has been called before
	 * @throws IOException if the other known server is not yet up and running
   */
  private static void setSingleOtherServer(String host, int port) throws IOException, IllegalStateException {
		if (_otherKnownServer!=null) throw new IllegalStateException("other known server has been set already");
		PDAsynchBatchTaskExecutorClt.setHostPort(host, port);
		_otherKnownServer = PDAsynchBatchTaskExecutorClt.getInstance();
  }


	/**
	 * searches (up to <CODE>_NUM_ATTEMPTS</CODE> times) among all its worker 
	 * connections, to find a connection to the worker with the most free threads.
	 * In each iteration, if the best worker connection reports at least one free 
	 * thread, it sends the tasks to this connection. Otherwise, it tries to send 
	 * the tasks to the connection to the other known server, if it has one such 
	 * connection, and if the tasks were not submitted from this server.
	 * Notice that by this method, in the long run, optimal load balancing is 
	 * expected (in the face of unknown task execution times), since it is always 
	 * the lightest loaded worker that will get the tasks to be executed.
	 * To counter the drawback in the face of many workers connected to this 
	 * server, that is, that it polls all of them to find the best one, the method
	 * only polls up to <CODE>_MAX_WIN_POLL_ATTEMPTS</CODE> workers after it has 
	 * found a free worker.
	 * Method is only called from <CODE>RRObject</CODE> objects. NOT part of the 
	 * public API.
	 * @param originating_clients Vector // Vector&lt;PDAsynchBatchTaskExecutorClt&gt;
	 * @param tasks TaskObject[]
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws PDAsynchBatchTaskExecutorException 
	 */
  void submitWork(Vector originating_clients, TaskObject[] tasks) 
		throws IOException, ClassNotFoundException, PDAsynchBatchTaskExecutorException {
    Set workers2rm = new HashSet();  // Set<PDAsynchBTEWListener>
    PDAsynchBTEWListener t = null;
		PDAsynchBTEWListener t_last_resort = null;
		utils.Messenger mger = utils.Messenger.getInstance();
    mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): finding an available worker connection",1);
    // 1. find a worker (via Round-Robin)
    int count = 0;
    while (++count<_NUM_ATTEMPTS) {
      workers2rm.clear();
			int best = 0;
			synchronized (this) {  // must protect _workers and the iterator
	      Iterator sit = _workers.keySet().iterator();
				int winners=0;
			  while (sit.hasNext()) {
				  Socket s = (Socket) sit.next();
					PDAsynchBTEWListener tit = (PDAsynchBTEWListener) _workers.get(s);
	        int num_avail = tit.getNumAvailableThreads();
					if (num_avail>best) {
						best = num_avail;
						t = tit;
					}
					else if (num_avail==0) {
						t_last_resort=tit; // record last worker even if busy, so as to send 
						                   // tasks to this one if everything else fails
					}
					if (num_avail<0) {
						if (tit.isConnectionLost()) workers2rm.add(tit);			
					} 
					else if (best>0 && ++winners>=_MAX_WIN_POLL_ATTEMPTS) break;
					// after an available worker has been found, poll no more than _MAX_WIN_POLL_ATTEMPTS
				}
	      // remove any "lost connections" worker listeners
		    Iterator it = workers2rm.iterator();
			  while (it.hasNext()) _workers.remove(it.next());
			}
			if (best>0) {
				count=_NUM_ATTEMPTS;  // found one
			}
    }
    if (t==null) {  // failed to find an available worker
      mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): no available workers...",1);
			if (_otherKnownServer!=null) {
        mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): trying other known server",1);
        try {
          String clientipaddress_port = _otherKnownServer.getHostIPAddress() + "_" + _otherKnownServer.getPort();
          if (contains(originating_clients,clientipaddress_port)) {
            mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): "+
                     "tasks have been created or forwarded from the other known server that is being checked",1);
		        throw new PDAsynchBatchTaskExecutorException("no available worker or known srv could undertake work");
          }
					// ok, try other server
          mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): "+
                                              "forwarding tasks to: "+clientipaddress_port,1);
          originating_clients.addElement(clientipaddress_port);
          _otherKnownServer.submitWork(originating_clients, tasks);
          return;
        }
        catch (Exception e) {  // other server failed, send to last resort worker
					e.printStackTrace();
        }
			} 
			else if (t_last_resort!=null) {  // submit to last working, busy worker
				t = t_last_resort;
			} 
			else  // failed completely
        throw new PDAsynchBatchTaskExecutorException("no available worker or known srv could undertake work");
    }
    mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): found a worker",1);
    // 2. submit tasks and get back results
    TaskObjectsAsynchExecutionRequest req = new TaskObjectsAsynchExecutionRequest(originating_clients, tasks);
    mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): created the TaskObjectsAsynchExecutionRequest to send",1);
		submitWork(req, t);
    mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): finished running submitWork(req,ois,oos)",1);
  }


  private void submitWork(TaskObjectsAsynchExecutionRequest req, PDAsynchBTEWListener t)
      throws IOException, PDAsynchBatchTaskExecutorException {
    utils.Messenger mger = utils.Messenger.getInstance();
		mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(req,t): sending request",1);
    t.runObject(req);
    mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(req,t): response received",1);
  }


  /**
   * adds a new worker to the network.
   * @param s Socket
   */
  private void addNewWorkerConnection(Socket s) {
    try {
      PDAsynchBTEWListener lt = new PDAsynchBTEWListener(this, s);
			synchronized (this) {
				_workers.put(s, lt);
			}
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }


  /**
   * adds a new client to the network. The difference between clients and
   * workers is that "clients" submit jobs to the network, but don't want to
   * "get" any job to do themselves. Workers may also submit jobs to the network
   * but are also available to run tasks themselves as well.
   * @param s Socket
   */
  private void addNewClientConnection(Socket s) {
    try {
      PDAsynchBTECListenerThread lt = new PDAsynchBTECListenerThread(this, s);
      lt.start();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }


  private static void usage() {
    System.err.println("usage: java -cp <classpath> parallel.distributed.PDAsynchBatchTaskExecutorSrv [workersport(7980)] [clientsport(7981)] [sendInitCmd?(false)] [other_srv_ipaddress,other_srv_port(null)]");
  }


  private static boolean contains(Vector clients, String cname) {
    if (clients==null) return false;
    for (int i=0; i<clients.size(); i++) {
      String ci = (String) clients.elementAt(i);
      if (ci!=null && ci.equals(cname)) return true;
    }
    return false;
  }


  /**
   * auxiliary inner class used for listening for incoming worker connections
   * and creating new PDAsynchBTEWListener objects to handle each connection.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2016</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class AsynchWThread extends Thread {
    int _port;

    private AsynchWThread(int port) {
      _port = port;
    }


    public void run() {
      try {
        ServerSocket ss = new ServerSocket(_port);
        System.out.println("AsynchSrv: Now Accepting Worker Connections");
        while (true) {
          try {
            Socket s = ss.accept();
            System.out.println("AsynchSrv: New Worker Added to the Network");
            addNewWorkerConnection(s);
            System.out.println("AsynchSrv: finished adding new worker connection to the _workers");
          }
          catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
  }


  /**
   * auxiliary inner class used for listening for incoming client connections
   * and creating new PDAsynchBTECListenerThread threads to handle each connection.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2016</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class AsynchCThread extends Thread {
    private int _port;

    private AsynchCThread(int port) {
      _port = port;
    }

    public void run() {
      try {
        ServerSocket ss = new ServerSocket(_port);
        System.out.println("AsynchSrv: Now Accepting Client Connections");
        while (true) {
          try {
            Socket s = ss.accept();
            System.out.println("AsynchSrv: New Client Added to the Network");
            addNewClientConnection(s);
            System.out.println("AsynchSrv: finished adding new client connection");
          }
          catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
  }


  /**
   * auxiliary inner class.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class PDAsynchBTECListenerThread extends Thread {
    private final Socket _s;
    private final ObjectInputStream _ois;
    private final ObjectOutputStream _oos;
    private final PDAsynchBatchTaskExecutorSrv _srv;
    private final boolean _isAvail = true;

    private PDAsynchBTECListenerThread(PDAsynchBatchTaskExecutorSrv srv, Socket s) throws IOException {
      _srv = srv;
      _s = s;
      _ois = new ObjectInputStream(_s.getInputStream());
      _oos = new ObjectOutputStream(_s.getOutputStream());
      _oos.flush();
    }


    public void run() {
			utils.Messenger mger = utils.Messenger.getInstance();
			RRObject r = null;
			if (_sendInitCmd) {
				if (getInitCmd()==null) {  // the first client to send an init-cmd wins.
					try {
						r = (RRObject) _ois.readObject();
						if (r instanceof PDAsynchInitCmd) {
							setInitCmd((PDAsynchInitCmd) r);
							r = null;
						}
					}
					catch (Exception e) {
						mger.msg("PDAsynchBTECListenerThread.run(): Client did not send an init cmd...", 1);
					}
				}
			}
      while (true) {
        try {
          mger.msg("PDAsynchBTECListenerThread.run(): waiting to read a TaskObjectsAsynchExecutionRequest...",2);
					TaskObjectsAsynchExecutionRequest obj;
          // 1. read from socket input
					if (r!=null) {
						obj = (TaskObjectsAsynchExecutionRequest) r;
						r = null;
					} else {
	          obj = (TaskObjectsAsynchExecutionRequest) _ois.readObject();  // obj is an TaskObjectsAsynchExecutionRequest
					}
          mger.msg("PDAsynchBTECListenerThread.run(): TaskObjectsAsynchExecutionRequest read",2);
          // 2. take appropriate action
          try {
            obj.runProtocol(_srv, _ois, _oos);
          }
          catch (PDAsynchBatchTaskExecutorException e) {  // give it a second chance
						mger.msg("PDAsynchBTECListenerThread.run(): calling obj.runProtocol() "+
							       "issued PDAsynchBatchTaskExecutorException, will try one more time.", 1);
						secondChance(obj);
          }
					catch (IOException e) {  // worker somehow failed, give srv one more shot, then notify client
						mger.msg("PDAsynchBTECListenerThread.run(): calling obj.runProtocol() "+
							       "issued IOException, will try one more time.", 1);
						secondChance(obj);
					}
        }
				catch (ClassCastException e) {
					mger.msg("PDAsynchBTECListenerThread.run(): object incorrectly cast...", 1);
					r=null;
				}
        catch (Exception e) {  // client closed connection
          //e.printStackTrace();
          try {
            _ois.close();
            _oos.close();
            _s.close();
          }
          catch (Exception e2) {
            e2.printStackTrace();
          }
          utils.Messenger.getInstance().msg("PDAsynchBatchTaskExecutorSrv: Client Network Connection Closed",0);
          return;  // bye bye
        }
      }
    }
		
		
		private void secondChance(TaskObjectsAsynchExecutionRequest obj) throws ClassNotFoundException, IOException {
			try {
				obj.runProtocol(_srv, _ois, _oos);
			}
			catch (PDAsynchBatchTaskExecutorException e2) {
				utils.Messenger.getInstance().msg("PDAsynchBTECListenerThread.run(): sending NoWorkerAvailableResponse() to client...",1);
				// e.printStackTrace();
				_oos.writeObject(new NoWorkerAvailableResponse(obj._tasks));
				_oos.flush();							
			}
			catch (IOException e2) {
				utils.Messenger.getInstance().msg("PDAsynchBTECListenerThread.run(): sending FailedReply to client...",1);
				// e.printStackTrace();
				_oos.writeObject(new FailedReply());
				_oos.flush();														
			}
		}
  }


  /**
   * auxiliary inner class. Several methods are synchronized to protect access 
	 * to the socket connection <CODE>_s</CODE> and its <CODE>_ois,_oos</CODE> 
	 * I/O streams to the worker on the other side of the socket.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class PDAsynchBTEWListener {
    private Socket _s;
    private ObjectInputStream _ois;
    private ObjectOutputStream _oos;
    private final PDAsynchBatchTaskExecutorSrv _srv;
    private boolean _isAvail = true;  // this field is also modified by other
		                                  // threads and needs synchronized access

    private PDAsynchBTEWListener(PDAsynchBatchTaskExecutorSrv srv, Socket s) throws IOException {
      _srv = srv;
      _s = s;
      _ois = new ObjectInputStream(_s.getInputStream());
      _oos = new ObjectOutputStream(_s.getOutputStream());
      _oos.flush();
			if (_sendInitCmd) {  // send to worker the init cmd
				while (getInitCmd()==null) {  // wait if necessary to get the init-cmd
					synchronized (_sync) {
						try {
							_sync.wait();
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					}
				}
				_oos.writeObject(_initCmd);
				_oos.flush();
			}
    }


    private synchronized void runObject(TaskObjectsAsynchExecutionRequest obj) throws IOException, PDAsynchBatchTaskExecutorException {
      Object res = null;
      try {
        setAvailability(false);
				_oos.writeObject(obj);
				_oos.flush();
				res = _ois.readObject();
        setAvailability(true);
      }
      catch (IOException e) {  // worker closed connection
        processException(e);
        throw e;
      }
      catch (ClassNotFoundException e) {
        processException(e);
        throw new IOException("stream has failed");
      }
      if (!(res instanceof OKReply)) {
				PDAsynchBatchTaskExecutorException e = 
					new PDAsynchBatchTaskExecutorException("worker failed to run tasks");
				processException(e);  // kick worker out
				throw e;
			}
    }


    private synchronized boolean getAvailability() {
      return _isAvail && _s!=null && _s.isClosed()==false;
    }
    private synchronized void setAvailability(boolean v) { _isAvail = v; }
		
		
		private synchronized int getNumAvailableThreads() {
			if (!getAvailability()) return -1;  // worker unavailable.
			PDAsynchBatchTaskExecutorWrkAvailabilityRequest r = new PDAsynchBatchTaskExecutorWrkAvailabilityRequest();
			int num_avail=0;
			try {
				_oos.writeObject(r);
				_oos.flush();
				OKReplyData reply = (OKReplyData) _ois.readObject();
				num_avail = ((Integer)reply.getData()).intValue();
			}
			catch (Exception e) {
				num_avail=-1;
				setAvailability(false);
				utils.Messenger.getInstance().msg("Socket has been closed?", 0);
        // try graceful exit
				try {
					_s.shutdownOutput();
					_s.close(); // Now we can close the Socket
				}
				catch (IOException e2) {
					// silently ignore
				}
			}
			return num_avail;
		}


    private synchronized boolean isConnectionLost() {
      return _s==null || _s.isClosed();
    }


    private void processException(Exception e) {
      // e.printStackTrace();
      try {
        _ois.close();
        _oos.close();
        _s.close();
      }
      catch (Exception e2) {
        // e2.printStackTrace();
      }
      finally {
	      synchronized (_srv) {
		      _workers.remove(_s);
			    utils.Messenger.getInstance().msg("PDAsynchBatchTaskExecutorSrv: Worker Network Connection Closed",0);
				}
        _ois = null;
        _oos = null;
        _s = null;
      }
    }	
	}
	
}

