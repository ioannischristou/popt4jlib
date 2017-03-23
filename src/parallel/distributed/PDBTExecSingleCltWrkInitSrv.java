package parallel.distributed;

import parallel.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * class extends the PDBatchTaskExecutorSrv class to allow a load-balancing 
 * distributed computing architecture, whereby a server object of this class can 
 * only have a single client connected to it, and no other known servers 
 * connected to it at all. Many workers of type <CODE>PDBTExecInitedWrk</CODE> 
 * (only) can be connected to this server. Each such worker, before starting its 
 * threads, will have to be initialized by executing an "initialization-command" 
 * object that the unique client attached to this server will submit to this 
 * server. The server will always be forwarding the initialization command to 
 * each worker upon connecting to it. 
 * Notice that in case workers connect to this server before the unique client 
 * connects to it, the workers will have to wait indefinitely until the client 
 * connects and sends its initialization command which will then be forwarded to 
 * all waiting workers and all new ones coming afterwards.
 * Notice also that this server achieves load-balancing among connected workers
 * by dividing equally the load of the multiple TaskObject objects submitted in 
 * each client request, among many <CODE>WrkSubmissionThread</CODE> threads 
 * (defined in RRObject.java; many more than the number of available workers) 
 * that simultaneously attempt to submit their load to the available workers; as 
 * faster workers finish first they become available for picking up the work of 
 * the waiting threads, and thus load balancing is achieved.
 * Computing Policies:
 * If a worker connection is lost during processing a batch of tasks, the batch
 * will be re-submitted once more to the next available worker, as soon as such
 * a worker becomes available. Similarly, if a worker fails to process a batch
 * of tasks and returns a <CODE>FailedReply</CODE> object back to this server,
 * the server will attempt one more time to re-submit the batch to another 
 * worker as soon as such a worker becomes available. In case a worker fails 
 * to process two different batches of jobs in sequence, the server drops its
 * connection from this "loser" worker. If the same batch of jobs fails to be 
 * executed by two different workers, the server sends back to the client a 
 * <CODE>FailedReply</CODE> to indicate job cannot be successfully completed.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBTExecSingleCltWrkInitSrv extends PDBatchTaskExecutorSrv { 
	private final static int _CHECK_PERIOD_MSECS=10000;  // 10 seconds
	private RRObject _initCmd=null;  // the init. cmd to be sent to all workers
	                                 // attached to this server before any other
	                                 // job.

	
  /**
   * sole public constructor.
   * @param wport int the port workers (PDBTExecInitedWrk) connect to.
   * @param cport int the port for the client (PDBTExecInitedClt) to connect to.
   */
  public PDBTExecSingleCltWrkInitSrv(int wport, int cport) {
		super(wport, cport);
  }


  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; 
	 * parallel.distributed.PDBTExecSingleCltWrkInitSrv 
	 * [workers_port(7890)] [client_port(7891)] </CODE>.
   * @param args String[]
   */
  public static void main(String[] args) {
    int wport = 7890;  // default port
    int cport = 7891;
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
    PDBTExecSingleCltWrkInitSrv server = new PDBTExecSingleCltWrkInitSrv(wport, 
			                                                                   cport);
    try {
      server.run();
    }
    catch (Exception e) {
      e.printStackTrace();
      System.err.println("Server exits due to exception.");
    }
  }


  protected void run() throws IOException {
    W2Thread workersListeningThread = new W2Thread(getWorkersPort());
    workersListeningThread.start();
    C2Thread clientListeningThread = new C2Thread(getClientsPort());
    clientListeningThread.start();
  }

	
  TaskObjectsExecutionResults submitWork(Vector originating_clients, 
		                                     TaskObject[] tasks) 
		throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
    Set workers2rm = new HashSet();  // Set<Socket s> for 
		                                 // (Socket s, PDBTEW2Listener t) pair
    PDBTEW2Listener t = null;
    utils.Messenger.getInstance().msg(
			"PDBTExecSingleCltWrkInitSrv.submitWork(clts,tasks): "+
			"finding an available worker connection among "+
			getNumWorkers()+" known workers",1);
    // 1. find a worker (via Round-Robin)
    synchronized (this) {
			boolean cont = true;
      while (cont) {
        workers2rm.clear();
        Iterator sit = getWorkers().keySet().iterator();
        while (sit.hasNext()) {
          Socket s = (Socket) sit.next();
          t = (PDBTEW2Listener) getWorkers().get(s);
          boolean is_avail = t.getAvailability() && !getWorking().contains(t);
          // the _working set is needed so that submitWork() is guaranteed not
          // to choose twice the same worker before the worker is done with the
          // first request.
          if (is_avail) {
            cont = false;  // break out of the top-level while-loop too
            getWorking().add(t);
            break;
          }
          else {
            if (t.isConnectionLost()) workers2rm.add(s);  // used to be add(t)
            t = null;  // reset to null
          }
        }
        // remove any "lost connections" worker listeners
        Iterator it = workers2rm.iterator();
        while (it.hasNext()) getWorkers().remove(it.next());
				if (t==null) {
					// before trying again to find an available worker, wait a while
					try {
						wait(_CHECK_PERIOD_MSECS);  // wait out the workers for some time
					}
					catch (InterruptedException e) {
						// e.printStackTrace();
						Thread.currentThread().interrupt();  // recommended
					}
				}
      }
    }  // synchronized (this)
    if (t==null) {  // failed to find an available thread
      utils.Messenger.getInstance().msg(
				"PDBTExecSingleCltWrkInitSrv.submitWork(clt,tasks): "+
				"no available threads...",1);
      throw new PDBatchTaskExecutorException(
				"no available worker or known srv to undertake work");
    }
    utils.Messenger.getInstance().msg(
			"PDBTExecSingleCltWrkInitSrv.submitWork(clt,tasks): "+
			"found an available worker",1);
    // 2. submit tasks and get back results
    TaskObjectsExecutionRequest req = 
			new TaskObjectsExecutionRequest(originating_clients, tasks);
    utils.Messenger.getInstance().msg(
			"PDBTExecSingleCltWrkInitSrv.submitWork(clt,tasks): "+
			"created the TaskObjectsExecutionRequest to send",1);
    RRObject res = submitWork(req, t);
    utils.Messenger.getInstance().msg(
			"PDBTExecSingleCltWrkInitSrv.submitWork(tasks): finished running "+
			"submitWork(req,ois,oos)",1);
    synchronized (this) {
      getWorking().remove(t);  // declare worker's availability again
			notifyAll();  // declare I'm done. 
			              // With only a single client connected, not of much use.
    }
    if (res instanceof TaskObjectsExecutionResults)
      return (TaskObjectsExecutionResults) res;
    else {
      throw new PDBatchTaskExecutorException(
				"PDBTExecSingleCltWrkInitSrv.submitWork(tasks): "+
				"worker failed to process tasks.");
    }
  }


	/**
	 * simply calls the method <CODE>t.runObject(req)</CODE> wrapped in some debug
	 * messages, and returns the result of the call.
	 * @param rq TaskObjectsExecutionRequest
	 * @param t PDBTEW2Listener
	 * @return TaskObjectsExecutionResults
	 * @throws IOException
	 * @throws PDBatchTaskExecutorException 
	 */
  private TaskObjectsExecutionResults submitWork(TaskObjectsExecutionRequest rq, 
		                                             PDBTEW2Listener t)
    throws IOException, PDBatchTaskExecutorException {
    utils.Messenger.getInstance().msg(
			"PDBTExecSingleCltWrkInitSrv.submitWork(req,t): sending request",1);
    TaskObjectsExecutionResults res = t.runObject(rq);
    utils.Messenger.getInstance().msg(
			"PDBTExecSingleCltWrkInitSrv.submitWork(req,t): response received",1);
    return res;
  }

	
	/**
	 * sets the initial command for all workers to execute, and notifies any 
	 * worker-threads waiting for such command to be sent to the workers.
	 * @param obj RRObject
	 */
	private synchronized void setInitCmd(RRObject obj) {
		_initCmd = obj;
		notifyAll();
	}


  /**
   * adds a new worker to the network.
   * @param s Socket
	 * @throws IOException
   */
  private synchronized void addNewWorkerConnection(Socket s) 
		throws IOException {
    PDBTEW2Listener lt = new PDBTEW2Listener(this, s);
		lt.init();
    getWorkers().put(s, lt);
  }


  /**
   * adds client to the network. The difference between clients and
   * workers is that "clients" submit jobs to the network, but don't want to
   * "get" any job to do themselves. Workers may also submit jobs to the network
   * but are also available to run tasks themselves as well.
   * @param s Socket
	 * @throws IOException
   */
  private synchronized void addNewClientConnection(Socket s) 
		throws IOException {
		PDBTEC2ListenerThread lt = new PDBTEC2ListenerThread(this, s);
    lt.start();
  }


  private static void usage() {
    System.err.println("usage: java -cp <classpath> "+
			                 "parallel.distributed.PDBTExecSingleCltWrkInitSrv "+
			                 "[workersport] [clientport]");
  }


  /**
   * auxiliary inner class used for listening for incoming worker connections
   * and creating new PDBTEWListener objects to handle each connection.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class W2Thread extends Thread {
    int _port;

    private W2Thread(int port) {
      _port = port;
    }


		/**
		 * creates a server socket listening on port specified in the object 
		 * constructor, and then enters an infinite loop waiting for incoming
		 * socket connection requests representing a worker process attempting to
		 * connect to this server, which it handles via the enclosing server's
		 * <CODE>addNewWorkerConnection(s)</CODE> method.
		 */
    public void run() {
			utils.Messenger mger = utils.Messenger.getInstance();
      try {
        ServerSocket ss = new ServerSocket(_port);
        mger.msg("Srv: Now Accepting Worker Connections",0);
        while (true) {
          try {
            Socket s = ss.accept();
            mger.msg("Srv: Incoming New Worker Connection to the Network",0);
						mger.msg("Srv: Thread may have to wait if an init_cmd has not yet "+
							       "arrived from the client",0);
            addNewWorkerConnection(s);
            mger.msg("Srv: finished adding new worker connection to the "+
							       "_workers",0);
          }
          catch (Exception e) {
						mger.msg("PDBTExecSingleCltWrkInitSrv.W2Thread.run(): "+
							       "An error (exception '"+e+
							       "') occured while adding new worker connection", 2);
            // e.printStackTrace();
          }
        }
      }
      catch (IOException e) {
        // e.printStackTrace();
				mger.msg("PDBTExecSingleCltWrkInitSrv.W2Thread.run(): "+
					       "Failed to create Server Socket, Server exiting.", 0);
				System.exit(-1);
      }
    }
  }


  /**
   * auxiliary inner class used for listening for incoming client connections
   * and creating new PDBTECListenerThread threads to handle each connection.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class C2Thread extends Thread {
    private int _port;

    private C2Thread(int port) {
      _port = port;
    }

		
		/**
		 * creates a server socket listening on the port specified in the parameter 
		 * of the constructor, and waits for a single incoming client connection
		 * which it handles by invoking the <CODE>addNewClientConnection(s)</CODE>
		 * method of the enclosing server, and then the thread exits.
		 */
    public void run() {
			utils.Messenger mger = utils.Messenger.getInstance();
      try {
        ServerSocket ss = new ServerSocket(_port);
        mger.msg("Srv: Now Accepting Single Client Connection",0);
        //while (true) {
          try {
            Socket s = ss.accept();
            mger.msg("Srv: Client Added to the Network",0);
            addNewClientConnection(s);
            mger.msg("Srv: finished adding client connection",0);
          }
          catch (Exception e) {
            // e.printStackTrace();
						mger.msg("Client Connection failed (exception: '"+e+
							       "'), exiting...",0);
						System.exit(-1);
          }
        //}
      }
      catch (IOException e) {
        // e.printStackTrace();
				mger.msg("PDBTExecSingleCltWrkInitSrv.C2Thread.run(): "+
					       "Failed to create Server Socket, Server exiting.", 0);
				System.exit(-1);
      }
    }
  }


  /**
   * auxiliary inner class. Not part of the public API.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011-2017</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class PDBTEC2ListenerThread extends Thread {
    private Socket _s;
    private ObjectInputStream _ois;
    private ObjectOutputStream _oos;
    private PDBTExecSingleCltWrkInitSrv _srv;
    private boolean _isAvail = true;

    private PDBTEC2ListenerThread(PDBTExecSingleCltWrkInitSrv srv, Socket s) 
      throws IOException {
      _srv = srv;
      _s = s;
      _oos = new ObjectOutputStream(_s.getOutputStream());
      _oos.flush();
      _ois = new ObjectInputStream(_s.getInputStream());
    }


		/**
		 * reads from the input stream the initialization command sent to it, sends
		 * back to the client an <CODE>OKReply</CODE> "ACK" msg, and then enters an
		 * infinite loop waiting to read from the input stream an 
		 * <CODE>RRObject</CODE> obj that should really be of type 
		 * <CODE>TaskObjectsExecutionRequest</CODE>, on which it executes its method
		 * <CODE>obj.runProtocol(_srv,_ois, _oos)</CODE>.
		 */
    public void run() {
			// first, read from socket the worker-initialization object that will
			// be broadcast for execution to every worker connecting to this server.
			utils.Messenger mger = utils.Messenger.getInstance();
			try {
				mger.msg(
					"PDBTEC2ListenerThread: waiting to read the init_cmd from client", 1);
				RRObject initCmd = (RRObject) _ois.readObject();
				mger.msg(
					"PDBTEC2ListenerThread: done reading the init_cmd from client", 1);
				setInitCmd(initCmd);
				mger.msg("PDBTEC2ListenerThread: done setting the init_cmd", 2);
				// send back to the clt an "ACK" message
				_oos.writeObject(new OKReply());  // no need for _oos.reset() here
				_oos.flush();
				mger.msg(
					"PDBTEC2ListenerThread: done sending OKReply through the socket", 1);
			}
			catch (Exception e) {  // client closed connection
        // e.printStackTrace();
        try {
          _ois.close();
          _oos.close();
          _s.close();
        }
        catch (Exception e2) {
          // e2.printStackTrace();
        }
        mger.msg("PDBTEC2ListenerThread: Client Network Connection Closed",0);
        System.exit(-1);
			}
      while (true) {
        try {
          mger.msg("PDBTEC2ListenerThread.run(): waiting to read an RRObject...",
						       2);
          // 1. read from socket input
          RRObject obj =  (RRObject) _ois.readObject();  
          // obj is a TaskObjectsExecutionRequest
          mger.msg("PDBTEC2ListenerThread.run(): RRObject read",2);
          // 2. take appropriate action
          try {
            obj.runProtocol(_srv, _ois, _oos);
          }
          catch (PDBatchTaskExecutorException e) {
						mger.msg("PDBTEC2ListenerThread.run(): calling obj.runProtocol() "+
							       "issued PDBatchTaskExecutorException, "+
							       "will try one more time.", 1);
						secondChance(obj);
          }
					catch (IOException e) {  // worker somehow failed, 
						                       // give srv one more shot, then notify client
						mger.msg("PDBTEC2ListenerThread.run(): calling obj.runProtocol() "+
							       "issued IOException, will try one more time.", 1);
						secondChance(obj);
					}
        }
        catch (Exception e) {  // client closed connection
          // e.printStackTrace();
          try {
            _ois.close();
            _oos.close();
            _s.close();
          }
          catch (Exception e2) {
            // e2.printStackTrace();
          }
          mger.msg("PDBTExecSingleCltWrkInitSrv: Client Network Connection "+
						       "Closed (exception '"+e+"' caught)",0);
          System.exit(-1);
        }
      }
    }
		
		
		private void secondChance(RRObject obj) throws ClassNotFoundException, 
			                                             IOException {
			try {
				obj.runProtocol(_srv, _ois, _oos);
			}
			catch (PDBatchTaskExecutorException e2) {
				utils.Messenger.getInstance().msg(
					"PDBTEC2ListenerThread.run(): sending NoWorkerAvailableResponse() "+
					"to client...",1);
				// e.printStackTrace();
				_oos.reset();  // force write of object anew
				_oos.writeObject(new NoWorkerAvailableResponse(
					                     ((TaskObjectsExecutionRequest) obj)._tasks));
				_oos.flush();							
			}
			catch (IOException e2) {
				utils.Messenger.getInstance().msg(
					"PDBTEC2ListenerThread.run(): sending FailedReply to client...",1);
				// e.printStackTrace();
				_oos.writeObject(new FailedReply());  // no need for _oos.reset() here
				_oos.flush();														
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
  class PDBTEW2Listener {
    private Socket _s;
    private ObjectInputStream _ois;
    private ObjectOutputStream _oos;
    private final PDBTExecSingleCltWrkInitSrv _srv;
    private boolean _isAvail = true;
		private boolean _OK2SendOOB = false;  // redundant initialization
		private boolean _isPrevRunSuccess=true;
		private TaskObject[] _prevFailedBatch=null;

    private PDBTEW2Listener(PDBTExecSingleCltWrkInitSrv srv, Socket s) throws IOException {
      _srv = srv;
      _s = s;
      _oos = new ObjectOutputStream(_s.getOutputStream());
      _oos.flush();
      _ois = new ObjectInputStream(_s.getInputStream());
    }

		
		/**
		 * no need to synchronize any block in this method, as it is already 
		 * synchronized on _srv since it's only called by the 
		 * <CODE>_srv.addNewWorkerConnection(s)</CODE> method.
		 * @throws IOException 
		 */
		private void init() throws IOException {
			// wait until the _initCmd is ready, and send it over the socket
			while (_initCmd==null) {
				try {
					utils.Messenger.getInstance().msg(
						"PDBTExecSingleCltWrkInitSrv.PDBTEW2Listener.init(): "+
						"W2Thread waiting on server to obtain init_cmd from client...", 1);
					_srv.wait();  // the thread calling this method is already 
					              // synchronized on _srv.
				}
				catch (InterruptedException e) {
					// e.printStackTrace();
					Thread.currentThread().interrupt();
				}
			}
			_oos.reset();  // force object to be written anew
			_oos.writeObject(_initCmd);
			_oos.flush();
			utils.Messenger.getInstance().msg(
						"PDBTExecSingleCltWrkInitSrv.PDBTEW2Listener.init(): done.", 1);
		}

		
    private TaskObjectsExecutionResults runObject(TaskObjectsExecutionRequest o) 
			throws IOException, PDBatchTaskExecutorException {
      Object res = null;
      try {
        setAvailability(false);
        _oos.reset();  // force writing object anew
        _oos.writeObject(o);
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
      if (res instanceof TaskObjectsExecutionResults) {
        _isPrevRunSuccess=true;
				return (TaskObjectsExecutionResults) res;
			}
      else {
				PDBatchTaskExecutorException e = 
					new PDBatchTaskExecutorException("worker failed to run tasks");
				if (_isPrevRunSuccess==false && !sameAsPrevFailedJob(o._tasks)) { 
					processException(e);  // twice a loser, kick worker out
				}
				_isPrevRunSuccess=false;
				_prevFailedBatch = o._tasks;
				throw e;
			}
    }


		/**
		 * there is an issue with this method: if it is called often enough, the 
		 * <CODE>_s.sendUrgentData(0);</CODE> method that it invokes, will force 
		 * the <CODE>_s</CODE> socket to close on the other end, at least on Windows 
		 * systems. This behavior is due to the fact that OOB data handling is 
		 * problematic since there are conflicting specifications in TCP. Therefore, 
		 * it is required that the method is not called with high frequency (see the 
		 * <CODE>PDBTExecSingleCltWrkInitSrv._CHECK_PERIOD_MSECS</CODE> flag in this 
		 * file.)
		 * @return true iff the worker is available to accept work according to all 
		 * evidence.
		 */
    private synchronized boolean getAvailability() {
      boolean res = _isAvail && _s!=null && _s.isClosed()==false;
      if (res && _OK2SendOOB) {  // work-around the OOB data issue  
        // last test using OOB sending of data
        try {
					_OK2SendOOB = false;  // indicates should not send OOB data until 
					                      // set to true
          _s.sendUrgentData(0);  // unfortunately, if this method is called 
					                       // often enough, it will cause the socket to 
					                       // ... close???
          res = true;
        }
        catch (IOException e) {
					// e.printStackTrace();
          utils.Messenger.getInstance().msg(
						"PDBTExecSingleCltWrkInitSrv.getAvailability(): "+
						"Socket has been closed",0);
          res = false;
          _isAvail = false;  // declare availability to false as well
          // try graceful exit
          try {
            _s.shutdownOutput();
            _s.close(); // Now we can close the Socket
          }
          catch (IOException e2) {
            // silently ignore
          }
        }
      }
      return res;
    }
    private synchronized void setAvailability(boolean v) { 
			_isAvail = v;
			// by commenting out line below, sending OOB data is disabled,
			// as it is not possible in Windows to reliably send repeated OOB data
			// and not have the client at some point throw SocketException (connection
			// reset by peer).			
			// if (v) _OK2SendOOB = true;
		}


    private boolean isConnectionLost() {
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
		      getWorkers().remove(_s);
			    utils.Messenger.getInstance().msg(
						"PDBTExecSingleCltWrkInitSrv: Worker Network Connection Closed",0);
				}
        _ois = null;
        _oos = null;
        _s = null;
      }
    }

		
		/**
		 * compare the input argument with the batch tasks from the previous failed
		 * batch job.
		 * @param tasks TaskObject[]
		 * @return true if the two batch jobs' tasks have the same byte array 
		 * representation task-for-task.
		 */
		private boolean sameAsPrevFailedJob(TaskObject[] tasks) {
			if (_prevFailedBatch==null) return false;
			if (_prevFailedBatch.length!=tasks.length) return false;
			for (int i=0; i<tasks.length; i++) {
				if (!sameBytes(tasks[i],_prevFailedBatch[i])) return false;
			}
			return true;
		}
		
		
		/**
		 * compare the byte-array representation of two TaskObjects.
		 * @param f TaskObject
		 * @param s TaskObject
		 * @return true if the two arguments have the same byte-array
		 */
		private boolean sameBytes(TaskObject f, TaskObject s) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = null;
			try {
				out = new ObjectOutputStream(bos);
				out.writeObject(f);
				byte[] fs = bos.toByteArray();
				out.writeObject(s);
				byte[] ss = bos.toByteArray();
				if (fs.length!=ss.length) return false;
				for (int i=0; i<fs.length; i++) {
					if (fs[i]!=ss[i]) return false;
				}
				return true;
			}
			catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			finally {
				if (out!=null) {
					try {
						out.close();
					}
					catch (IOException e) {
						// ignore
					}
					try {
						bos.close();
					}
					catch (IOException e) {
						// ignore
					}
				}
			}
		}
  }  // class PDBTExecSingleCltWrkInitSrv.PDBTEW2Listener 

}  // class PDBTExecSingleCltWrkInitSrv

