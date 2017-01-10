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
	private HashMap _workerStatistics;  // map<PDAsynchBTEListener listener, Long num_tasks_accepted>
	private long _maxNumTasksPerWorker = 0;
  private int _workersPort = 7980;  // default port
  private int _clientsPort = 7981;  // default port
  private static final int _NUM_ATTEMPTS = 10;  // num attempts to iterate over
                                                // available worker connections
                                                // to try to find an idle one.
	private static final int _MAX_WIN_POLL_ATTEMPTS = 50;  // num workers to poll
	                                                       // after a free one has
	                                                       // been found
	private static final long _LAST_RESORT_SLEEP_TIME = 10;  // in msecs
  private static PDAsynchBatchTaskExecutorClt _otherKnownServer = null;
	// next variables indicate if an init-cmd should be sent to workers upon
	// connecting to the server
	private static PDAsynchInitCmd _initCmd = null;
	private final boolean _sendInitCmd;  // if made static, cannot be final
	private static final Object _sync = new Object();  // used for synchronization


  /**
   * sole constructor.
   * @param wport int the port workers (PDAsynchBatchTaskExecutorWrk) connect to.
   * @param cport int the port clients (PDAsynchBatchTaskExecutorClt) connect to.
	 * @param sendinitcmd boolean sets the appropriate flag
   */
  private PDAsynchBatchTaskExecutorSrv(int wport, int cport, boolean sendinitcmd) {
    _workers = new HashMap();
		_workerStatistics = new HashMap();
    _workersPort = wport;
    _clientsPort = cport;
		_sendInitCmd = sendinitcmd;
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
		boolean send_init_cmd = false;
    if (args.length>2) {
      if (args[2].toLowerCase().startsWith("t"))
        send_init_cmd = true;
      else send_init_cmd = false;
    }		
    PDAsynchBatchTaskExecutorSrv server = new PDAsynchBatchTaskExecutorSrv(wport, cport, send_init_cmd);
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
			_initCmd.applyOnServer();
			_sync.notifyAll();
			if (_otherKnownServer!=null) {  // send the command to the other server too
				try {
					_otherKnownServer.sendInitCmd(cmd);
				}
				catch (Exception e) {
					utils.Messenger.getInstance().msg("PDAsynchBatchTaskExecutorSrv.setInitCmd(): "+
						                                "failed to send command to other "+
						                                "known server, other server will "+
						                                "not be used", 0);
					try {
						PDAsynchBatchTaskExecutorClt.disconnect();
					}
					catch (IOException e2) {
						// ignore
					}
					_otherKnownServer=null;
				}
			}
		}
		utils.Messenger.getInstance().msg("PDAsynchBatchTaskExecutorSrv: init-cmd set.", 0);
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
	 * awaits until there is at least one worker registered (and initialized if 
	 * requested) in the network of this server's workers.
	 */
	synchronized void awaitWorkers() {
		while (_workers.size()==0) {
			try {
				wait();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
	
	
	/**
	 * get the current number of workers connected to this server.
	 * @return int
	 */
	private synchronized int getNumWorkers() {
		return _workers.size();
	}
	

	/**
	 * searches (up to <CODE>_NUM_ATTEMPTS</CODE> times) among all its worker
	 * connections, to find a connection to the worker with the most free threads.
	 * In each iteration, if the best worker connection reports at least one free
	 * thread, it sends the tasks to this connection. Otherwise, it tries to send
	 * the tasks to the connection to the other known server, if it has one such
	 * connection, and if the tasks were not submitted from this other server. If
	 * this isn't possible either, it sends the tasks to this known worker to
	 * be running (even though currently busy) that has gotten the most requests
	 * while free, assuming worker has capacity, ie its queue of tasks isn't full. 
	 * Otherwise, it throws <CODE>PDAsynchBatchTaskExecutorException</CODE>, which 
	 * propagates to client who reads a <CODE>NoWorkerAvailableResponse</CODE> 
	 * response object, and in turn throws 
	 * <CODE>PDAsynchBatchTaskExecutorNWAException</CODE> from the
	 * <CODE>PDAsynchBatchTaskExecutorClt.submitWork*()</CODE> methods.
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
    Set workers2rm = new HashSet();  // Set<Socket s> where s is key to PDAsynchBTEWListener tit
		Socket s = null;
    PDAsynchBTEWListener t = null;
		PDAsynchBTEWListener t_last_resort = null;
		utils.Messenger mger = utils.Messenger.getInstance();
		long tid = ((popt4jlib.IdentifiableIntf) Thread.currentThread()).getId();
		int wsz=getNumWorkers();
    mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): Thread-"+tid+
			       ": finding an available worker connection among "+wsz+" workers",2);
    // 1. find a worker (via Round-Robin)
    int count = 0;
    while (++count<_NUM_ATTEMPTS) {
      workers2rm.clear();
			int best = 0;
			//mger.msg("submitWork(): Thread-"+tid+": entering synchronized region", 2);
			synchronized (this) {  // must protect _workers and the iterator
				//mger.msg("submitWork(): Thread-"+tid+": got monitor, entering loop over _workers", 2);
	      Iterator sit = _workers.keySet().iterator();
				int winners=0;
			  while (sit.hasNext()) {
				  Socket sc = (Socket) sit.next();
					PDAsynchBTEWListener tit = (PDAsynchBTEWListener) _workers.get(sc);
	        int num_avail = tit.getNumAvailableThreads();
					if (num_avail>best) {
						best = num_avail;
						s = sc;
						t = tit;
					}
					else if (num_avail==0 && isFastest(tit)) {
						t_last_resort=tit; // record this worker even if busy, so as to send
						                   // tasks to this one if everything else fails
					}
					if (num_avail<0) {
						if (tit.isConnectionLost()) workers2rm.add(sc);  // used to be add(tit)
					}
					else if (best>0 && ++winners>=_MAX_WIN_POLL_ATTEMPTS) break;
					// after an available worker has been found, poll no more than _MAX_WIN_POLL_ATTEMPTS
				}
				if (t!=null) {  // remove (s,t) pair from _workers so that it's not 
					              // available to other client listeners until the tasks
					              // are submitted near the end of this method.
					_workers.remove(s);
					updateWorkerStatistics(t, tasks.length);
				}
	      // remove any "lost connections" worker listeners
		    Iterator it = workers2rm.iterator();
			  while (it.hasNext()) _workers.remove(it.next());
				if (workers2rm.size()>0) {  // the _maxNumTasksPerWorker value is no longer valid
					PDAsynchBTEWListener t2 = updateMaxNumTasksPerWorker();
					if (t_last_resort==null) t_last_resort = t2;
				}
			}  // end synchronized on this
			//mger.msg("submitWork(): Thread-"+tid+" done w/ synchronized region", 2);
			if (best>0) {
				count=_NUM_ATTEMPTS;  // found one
			}
    }
    if (t==null) {  // failed to find an available worker
      mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): Thread-"+tid+": no available workers...",2);
			if (_otherKnownServer!=null) {
        //mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): Thread-"+tid+": trying other known server",2);
        try {
          String clientipaddress_port = _otherKnownServer.getHostIPAddress() + "_" + _otherKnownServer.getPort();
          if (!contains(originating_clients,clientipaddress_port)) {
						// ok, try other server
						mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): Thread-"+tid+": "+
							       "forwarding tasks to: "+clientipaddress_port,2);
						originating_clients.addElement(clientipaddress_port);
						_otherKnownServer.submitWork(originating_clients, tasks);
						return;
					}
					// otherwise, this server will have to handle the tasks...
				}
        catch (Exception e) {  // other server failed, send to last resort worker
					mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): Thread-"+tid+
						       ": received exception e='"+e+
						       "' while trying to submit to other known server."+
						       " Will resort to fastest known worker if one exists.", 0);
        }
			}
			else if (t_last_resort!=null) {  // submit to last working, busy worker
				mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): Thread-"+tid+": resorting to fastest known working worker", 2);
				int count_down=_NUM_ATTEMPTS;
				while (--count_down>0) {
					synchronized (this) {
						if (t_last_resort.ensureCapacity(tasks.length)) {  // do the submission from the synchronized block
					    //mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): Thread-"+tid+": using a busy worker",1);
							// 2. submit tasks
							TaskObjectsAsynchExecutionRequest req = new TaskObjectsAsynchExecutionRequest(originating_clients, tasks);
							//mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): Thread-"+tid+": created the TaskObjectsAsynchExecutionRequest to send",1);
							submitWork(req, t_last_resort);
							//mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): Thread-"+tid+": finished running submitWork(req,ois,oos)",1);							
							return;
						}
					}
					// nope, sleep a bit
					try {
						Thread.sleep(_LAST_RESORT_SLEEP_TIME);
					}
					catch (InterruptedException e) {
						// no-op
					}
				}
				// no worker seems to be able to undertake work: 
				// send the tasks back to the client by throwing PDAsynchBatchTaskExecutorException
				throw new PDAsynchBatchTaskExecutorException("PDAsynchBTESrv.submitWork(): no worker seems to have capacity");
			}
			else  // failed completely
        throw new PDAsynchBatchTaskExecutorException("no available worker or known srv could undertake work");
    }
    //mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): Thread-"+tid+": found a worker",2);
    // 2. submit tasks, capacity is ensured
    TaskObjectsAsynchExecutionRequest req = new TaskObjectsAsynchExecutionRequest(originating_clients, tasks);
    //mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): Thread-"+tid+": created the TaskObjectsAsynchExecutionRequest to send",2);  
		try {
			submitWork(req, t);
		  mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(tasks): Thread-"+tid+": finished running submitWork(req,ois,oos)",2);
		}
		finally {
			synchronized (this) {
				_workers.put(s, t);  // put back the (s,t) pair
			}
		}
  }


  private void submitWork(TaskObjectsAsynchExecutionRequest req, PDAsynchBTEWListener t)
      throws IOException, PDAsynchBatchTaskExecutorException {
    //utils.Messenger mger = utils.Messenger.getInstance();
		//mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(req,t): sending request",2);
    t.runObject(req);
    //mger.msg("PDAsynchBatchTaskExecutorSrv.submitWork(req,t): response received",2);
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
				_workerStatistics.put(lt, new Long(0));
				notifyAll();  // notify waiting clients that there is a new worker in town
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
	 * @param id long
   */
  private void addNewClientConnection(Socket s, long id) {
    try {
      PDAsynchBTECListenerThread lt = new PDAsynchBTECListenerThread(this, s, id);
      lt.start();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
	
	
	/**
	 * update the statistics for the given worker, to reflect the fact that it
	 * has gotten this many tasks.
	 * @param t PDAsynchBTEWListener
	 * @param numtasks int
	 */
	private void updateWorkerStatistics(PDAsynchBTEWListener t, int numtasks) {
		Long nL = (Long) _workerStatistics.get(t);
		Long nL2 = new Long(nL.longValue()+numtasks);
		_workerStatistics.put(t, nL2);
		if (nL2.longValue()>_maxNumTasksPerWorker) {
			_maxNumTasksPerWorker = nL2.longValue();
		}
	}
	
	
	/**
	 * search through the entire hash-map for the worker with the most tasks, and
	 * return this worker listener, updating the related cache value 
	 * (<CODE>_maxNumTasksPerWorker</CODE>) as well.
	 * @return PDAsynchBTEWListener may be null
	 */
	private PDAsynchBTEWListener updateMaxNumTasksPerWorker() {
		_maxNumTasksPerWorker = 0;
		PDAsynchBTEWListener best = null;
		Iterator it = _workerStatistics.keySet().iterator();
		while (it.hasNext()) {
			PDAsynchBTEWListener l = (PDAsynchBTEWListener) it.next();
			Long vL = (Long) _workerStatistics.get(l);
			if (vL.longValue()>_maxNumTasksPerWorker) {
				best = l;
				_maxNumTasksPerWorker = vL.longValue();
			}
		}
		return best;
	}
	
	
	/**
	 * check whether the argument worker has accepted the maximum number of tasks
	 * among the currently known workers.
	 * @param t
	 * @return boolean true if it has gotten the most tasks so far
	 */
	private boolean isFastest(PDAsynchBTEWListener t) {
		long num_tasks_4_t = ((Long)_workerStatistics.get(t)).longValue();
		return (num_tasks_4_t >= _maxNumTasksPerWorker);
	}


  private static void usage() {
    System.err.println("usage: java -cp <classpath> "+
			                 "parallel.distributed.PDAsynchBatchTaskExecutorSrv "+
			                 "[workersport(7980)] [clientsport(7981)] "+
			                 "[sendInitCmd?(false)] "+
			                 "[other_srv_ipaddress,other_srv_port(null)]");
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


		/**
		 * opens a server-socket and listens for incoming connections; for each such
		 * connection, calls the method <CODE>addNewWorkerConnection(sock)</CODE> of
		 * the enclosing server.
		 */
    public void run() {
      try {
				utils.Messenger mger = utils.Messenger.getInstance();
        ServerSocket ss = new ServerSocket(_port);
        mger.msg("AsynchSrv: Now Accepting Worker Connections",1);
        while (true) {
          try {
            Socket s = ss.accept();
            mger.msg("AsynchSrv: New Worker Added to the Network",1);
            addNewWorkerConnection(s);
            mger.msg("AsynchSrv: finished adding new worker connection to the _workers",1);
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

		
		/**
		 * opens a server-socket and listens for incoming connections; for each such
		 * connection, calls the method <CODE>addNewClientConnection(sock,id)</CODE>
		 * with an increasing id counter.
		 */
    public void run() {
			long id=0;
      try {
				utils.Messenger mger = utils.Messenger.getInstance();
        ServerSocket ss = new ServerSocket(_port);
        mger.msg("AsynchSrv: Now Accepting Client Connections",1);
        while (true) {
          try {
            Socket s = ss.accept();
						++id;
            mger.msg("AsynchSrv: New Client (w/ id="+id+") Added to the Network",1);
            addNewClientConnection(s,id);
            mger.msg("AsynchSrv: finished adding new client (w/ id="+id+") connection",1);
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
  class PDAsynchBTECListenerThread extends Thread implements popt4jlib.IdentifiableIntf {
		private long _tid;  // thread identifier
    private final Socket _s;
    private final ObjectInputStream _ois;
    private final ObjectOutputStream _oos;
    private final PDAsynchBatchTaskExecutorSrv _srv;
    private final boolean _isAvail = true;

    private PDAsynchBTECListenerThread(PDAsynchBatchTaskExecutorSrv srv, Socket s, long id) throws IOException {
      _tid = id;
			_srv = srv;
      _s = s;
      _oos = new ObjectOutputStream(_s.getOutputStream());
      _oos.flush();
      _ois = new ObjectInputStream(_s.getInputStream());
    }


		/**
		 * return this thread's id as specified in its constructor.
		 * @return long
		 */
		public long getId() { return _tid; }
		
		
		/**
		 * the thread's main method, will initially wait for a PDAsynchInitCmd from
		 * the socket to the client, assuming no such cmd has yet arrived, and sets
		 * it as the initialization command for workers; then it
		 * will enter an infinite loop, waiting to read 
		 * <CODE>TaskObjectsAsynchExecutionRequest</CODE> objects, on which it
		 * calls their <CODE>runProtocol()</CODE> method.
		 */
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
					catch (IOException e) {  // worker somehow failed, give srv one more shot
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
   * auxiliary inner class. Several methods synchronize parts to protect access
	 * to the socket connection <CODE>_s</CODE> and its <CODE>_ois,_oos</CODE>
	 * I/O streams to the worker on the other side of the socket.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2016</p>
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
      _oos = new ObjectOutputStream(_s.getOutputStream());
      _oos.flush();
      _ois = new ObjectInputStream(_s.getInputStream());
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


    private void runObject(TaskObjectsAsynchExecutionRequest obj) throws IOException, PDAsynchBatchTaskExecutorException {
			utils.Messenger mger = utils.Messenger.getInstance();
			/*
			long tid = -1;
			if (Thread.currentThread() instanceof popt4jlib.IdentifiableIntf) {
				tid = ((popt4jlib.IdentifiableIntf) Thread.currentThread()).getId();
			}
			*/
      Object res = null;
      try {
				synchronized (this) {
					setAvailability(false);
					_oos.writeObject(obj);
					_oos.flush();
					//mger.msg("WListener.runObject(): Thread-"+tid+" waiting to read (OK) response from sending TOAER to Worker", 1);
					res = _ois.readObject();
					setAvailability(true);
				}
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
				mger.msg("runObject(): unexpected response from server: "+res, 0);
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


		private int getNumAvailableThreads() {
			utils.Messenger mger = utils.Messenger.getInstance();
			/*
			long tid = -1;
			if (Thread.currentThread() instanceof popt4jlib.IdentifiableIntf) {
				tid = ((popt4jlib.IdentifiableIntf) Thread.currentThread()).getId();
			}
			*/
			if (!getAvailability()) return -1;  // worker unavailable.
			PDAsynchBatchTaskExecutorWrkAvailabilityRequest r = new PDAsynchBatchTaskExecutorWrkAvailabilityRequest();
			int num_avail=0;
			try {
				synchronized (this) {
					_oos.writeObject(r);
					_oos.flush();
					//mger.msg("WListener.getNumAvailableThreads(): Thread-"+tid+" waiting to read OKReplyData from sending AvailabilityRequest to Worker", 1);
					OKReplyData reply = (OKReplyData) _ois.readObject();
					//mger.msg("WListener.getNumAvailableThreads(): Thread-"+tid+" got response", 1);
					num_avail = ((Integer)reply.getData()).intValue();
				}
			}
			catch (Exception e) {
				num_avail=-1;
				setAvailability(false);
				mger.msg("WListener.getNumAvailableThreads(): Socket has been closed? exception '"+e+"' caught", 0);
        // try graceful exit
				try {
					synchronized (this) {
						_s.shutdownOutput();
						_s.close(); // Now we can close the Socket
					}
				}
				catch (IOException e2) {
					// silently ignore
				}
			}
			return num_avail;
		}

		
		private boolean ensureCapacity(int size) {
			utils.Messenger mger = utils.Messenger.getInstance();
			/*
			long tid = -1;
			if (Thread.currentThread() instanceof popt4jlib.IdentifiableIntf) {
				tid = ((popt4jlib.IdentifiableIntf) Thread.currentThread()).getId();
			}
			*/
			if (!getAvailability()) return false;  // worker unavailable.
			PDAsynchBatchTaskExecutorWrkCapacityRequest r = new PDAsynchBatchTaskExecutorWrkCapacityRequest(size);
			int num_cap=0;
			try {
				synchronized (this) {
					_oos.writeObject(r);
					_oos.flush();
					//mger.msg("WListener.ensureCapacity(): Thread-"+tid+" waiting to read OKReplyData from sending CapacityRequest to Worker", 1);
					OKReplyData reply = (OKReplyData) _ois.readObject();
					//mger.msg("WListener.getNumAvailableThreads(): Thread-"+tid+" got response", 1);
					num_cap = ((Integer)reply.getData()).intValue();
				}
			}
			catch (Exception e) {
				num_cap=-1;
				setAvailability(false);
				mger.msg("PDAsynchBTEWListener.ensureCapacity(): Socket has been closed? closing connection", 0);
        // try graceful exit
				try {
					synchronized (this) {
						_s.shutdownOutput();
						_s.close(); // Now we can close the Socket
					}
				}
				catch (IOException e2) {
					// silently ignore
				}
			}
			return num_cap>0;
		}		
		

    private synchronized boolean isConnectionLost() {
      return _s==null || _s.isClosed();
    }


    private void processException(Exception e) {
      // e.printStackTrace();
			utils.Messenger.getInstance().msg("Srv.WListener.processException(): enter", 2);  // itc: HERE rm asap
      try {
				synchronized (this) {
					_ois.close();
					_oos.close();
					_s.close();
				}
      }
      catch (Exception e2) {
        // e2.printStackTrace();
      }
      finally {
				utils.Messenger.getInstance().msg(
					"Srv.WListener.processException(): about to get _srv monitor", 2);  // itc: HERE rm asap
	      synchronized (_srv) {
		      _workers.remove(_s);
			    utils.Messenger.getInstance().msg("Srv.PDAsynchBTEWListener.processException():"+
						                                " Worker Network Connection Closed and "+
						                                "Removed from server's _workers Connections",0);
				}
				synchronized (this) {
					_ois = null;
					_oos = null;
					_s = null;
				}
      }
    }
	}

}

