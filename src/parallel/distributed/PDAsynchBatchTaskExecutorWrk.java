package parallel.distributed;

import java.net.*;
import java.io.*;
import java.util.*;
import parallel.*;


/**
 * class implements a worker process, that is capable of accepting (through
 * network sockets) arrays of <CODE>TaskObject</CODE>s and executes them using a
 * <CODE>parallel.FasterParallelAsynchBatchTaskExecutor</CODE>. The process
 * connects on a server (by default on localhost, hosting the
 * <CODE>parallel.distributed.PDAsynchBatchTaskExecutorSrv</CODE> process) on
 * the dedicated port for worker connections (default 7980) and starts listening
 * in for <CODE>parallel.distributed.TaskObjectsAsynchExecutionRequest</CODE>
 * requests which it then processes. No results are returned directly to the
 * server.
 * Notice that in the event of shutting down this worker via Ctrl-c,
 * the process will attempt to shut-down cleanly, by first declaring its closing
 * state, then finishing up all received asynch-tasks
 * in the executor's queue, and only after that, exiting. This process should 
 * result in the worker exiting gracefully and cleanly from the network of 
 * asynch-servers/workers.
 * Further, notice that it is possible that a task executing on a worker may
 * decide to disallow the worker from accepting further requests from the
 * server it is connected to, unless the requests have originated from this 
 * worker process itself; this is accomplished by the task calling
 * <CODE>PDAsynchBatchTaskExecutorWrk.setServerDisabled(true)</CODE>. This is
 * useful for example in <CODE>graph.packing.DBBNode*</CODE> where there is the
 * requirement for each worker process to impose a threshold on the total number
 * of nodes it may create. When this threshold is reached, the code calls above
 * method, and after this, worker only accepts tasks that originated from it.
 * In such cases however, it is necessary that either the init-cmd sent to this
 * worker forces the <CODE>PDAsynchBatchTaskExecutorClt</CODE> to be properly
 * initialized (call its <CODE>setHostPort()</CODE> method), or else that the
 * default port is used for the pdasynch-client.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2016-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class PDAsynchBatchTaskExecutorWrk {
  private static int _numthreads = 10;  // default number of threads of 
	                                      // FasterParallelAsynchBatchTaskExecutor
  private static String _host = "localhost";  // default host
  private static int _port = 7980;  // default worker port
  private static Socket _s = null;
	private static FasterParallelAsynchBatchTaskExecutor _executor = null;
	private static final Object _sync = new Object();  
	// _synch object needed during termination via Ctrl-c
	private static volatile boolean _isClosing = false;  // set during termination
	// being a volatile boolean variable, _isClosing needs no further 
	// synchronization for read/write accessing even in JDK1.4
	private static volatile boolean _srvReqsDenied = false;  // same comment 
	// as above for volatile applies; when this flag is on, worker always
	// sends back FailedReply responses to server issuing requests
	private static boolean _isDone = false;  // condition for termination
	private static boolean _runInitCmd = false;

	private final static boolean _GATHER_STATS = true;
	private static long _avgTimeBetweenJobsRecvd=0;
	private static long _lastTimeWhenJobRecvd=0;
	private static long _numJobsRecvd=0;
	private static long _numTasksRecvd=0;
	
	
  /**
   * no-arg constructor will assume host is localhost, workers' port is
   * 7980, the associated FasterParallelAsynchBatchTaskExecutor will have 10
	 * threads, won't expect an init-cmd first, and will have an associated 
	 * executor's queue size that is unbounded (according to the 
	 * <CODE>parallel.UnboundedSimpleFasterMsgPassingCoordinator</CODE> class).
   */
  private PDAsynchBatchTaskExecutorWrk() {
		// no-op
  }


  /**
   * constructor provides values for the numthreads, host/port parameters, and
	 * runinitcmd for this worker process.
   * @param numthreads int
   * @param host String
   * @param port int
	 * @param runinitcmd boolean
   */
  private PDAsynchBatchTaskExecutorWrk(int numthreads, String host, int port,
		                                   boolean runinitcmd) {
    _numthreads = numthreads;
    _host = host;
    _port = port;
		_runInitCmd = runinitcmd;
  }


  /**
   * auxiliary method called by main()
   * @throws IOException
   */
  private void run() throws IOException {
    _s=null;
    ObjectInputStream ois=null;
    ObjectOutputStream oos=null;
		utils.Messenger mger = utils.Messenger.getInstance();
    try {
      mger.msg("AsynchWrk: About to Connect to AsynchSrv at "+
				       "address(host,port)=("+_host+","+_port+")",0);
      _s = new Socket(_host, _port);
      mger.msg("AsynchWrk: socket created",1);
      oos = new ObjectOutputStream(_s.getOutputStream());
      oos.flush();
      ois = new ObjectInputStream(_s.getInputStream());
      mger.msg("AsynchWrk: Connected to AsynchSrv at address(host,port)=("+
				       _host+","+_port+")",0);
			RRObject r = null;
			if (_runInitCmd) {  // expect the first object to arrive from the socket
				                  // to be an init-cmd
				try {
					r = (RRObject) ois.readObject();
					if (r instanceof PDAsynchInitCmd) {
						r.runProtocol(null, null, null);
						mger.msg("AsynchWrk: executed the initialization command "+
							       "received from server", 1);
						r = null;
					}
				}
				catch (Exception e) {
					mger.msg("AsynchWrk: failed to receive or execute an init-cmd "+
						       "from server, will exit.", 1);
				}
			}
			// get the address string the client of the PDA network will be using
			// Notice that it is the responsibility of the application to have
			// called the method PDAsynchBatchTaskExecutorClt.setHostPort(host, port);
			// or else (in the absense of an init-cmd received and executed that calls
			// this method), that the default port specified in the client (7981) to
			// be the right one.
			final String _clt_originator_string = 
				PDAsynchBatchTaskExecutorClt.getInstance().getOriginatorString();
			// tasks should never execute on the current thread if the executor is
			// full, as this would force the main thread to become unresponsive to
			// incoming requests through the socket.
			synchronized (PDAsynchBatchTaskExecutorWrk.class) {
				_executor = FasterParallelAsynchBatchTaskExecutor.
					newFasterParallelAsynchBatchTaskExecutor(_numthreads, false);
			}
			final OKReply res = new OKReply();
      while (r==null) {  // if r!=null, initialization failed, thus exit...
        try {
          // get a request
          mger.msg("AsynchWrk: waiting to read an RRObject",2);
          RRObject req = (RRObject) ois.readObject();
					if (isClosing()) {
						mger.msg("AsynchWrk: in closing state, sending to Srv FailedReply "+
							       "to terminate Worker Connection", 0);
						FailedReply fr = new FailedReply();
						oos.writeObject(fr);  // oos.reset() not needed here
						oos.flush();
						break;
					}
					if (getServerRequestsDisabled()) {  // don't accept requests
						                                  // not originating from me
						mger.msg("AsynchWrk: worker currently does not process requests"+
							       " not originating from this worker",0);
						if (req instanceof PDAsynchBatchTaskExecutorWrkAvailabilityRequest){
							OKReplyData reply = new OKReplyData(new Integer(-1));
							oos.writeObject(reply);  // no need to calls oos.reset() first
							oos.flush();
						}
						else if (req instanceof 
							       PDAsynchBatchTaskExecutorWrkQueueSizeRequest){
							OKReplyData reply = 
								new OKReplyData(new Integer(Integer.MAX_VALUE));
							oos.writeObject(reply);  // no need to calls oos.reset() first
							oos.flush();
						}						
						else if (req instanceof TaskObjectsAsynchExecutionRequest) {
							TaskObjectsAsynchExecutionRequest toaer = 
								(TaskObjectsAsynchExecutionRequest) req;
							if (toaer._originatingClients.contains(_clt_originator_string)) {
								if (_GATHER_STATS) {
									synchronized (PDAsynchBatchTaskExecutorWrk.class) {
										long now = System.currentTimeMillis();
										long dur = _lastTimeWhenJobRecvd==0 ? 
											           0 : now-_lastTimeWhenJobRecvd;
										_lastTimeWhenJobRecvd=now;
										_avgTimeBetweenJobsRecvd = 
											(_avgTimeBetweenJobsRecvd*_numJobsRecvd+dur) / 
											++_numJobsRecvd;
										_numTasksRecvd += toaer._tasks.length;
									}
								}
								// ok, execute
								Vector tasks = new Vector();
								for (int i=0; i<toaer._tasks.length; i++)
									tasks.addElement(toaer._tasks[i]);
								//  process request
								_executor.executeBatch(tasks);
								mger.msg("AsynchWrk: TaskObjectsAsynchExecutionRequest "+
									       "successfully submitted to pool",2);
								oos.writeObject(res);  // no need to call oos.reset() here
								oos.flush();
								mger.msg("AsynchWrk: sent an OKReply response",2);
							}
							else {  // send back RequestIgnoredReply, which will not cause
								      // the server to drop the connection to this worker
								// no need for oos.reset()
								oos.writeObject(new RequestIgnoredReply());
								oos.flush();
								mger.msg("AsynchWrk: sent an RequestIgnoredReply response",2);
							}
						}
						else {
							throw new PDAsynchBatchTaskExecutorException(
								          "AsynchWrk: cannot parse request");
						}
						continue;
					}  // if worker is disabled
					if (req instanceof PDAsynchBatchTaskExecutorWrkAvailabilityRequest) {
						mger.msg(
							"AsynchWrk: got PDAsynchBatchTaskExecutorWrkAvailabilityRequest", 
							2);
						int num_busy = _executor.getNumBusyThreads();
						int num_avail = _numthreads - (num_busy>=0 ? num_busy:_numthreads);
						OKReplyData okdata = new OKReplyData(new Integer(num_avail));
						oos.writeObject(okdata);  // no need to call oos.reset() first here
						oos.flush();
						//int qsz = _executor.getNumTasksInQueue();
						//mger.msg("AsynchWrk: sent back number of free threads="+
						//         num_avail+" (extor queue size="+qsz+")", 2);
					}
					else if (req instanceof PDAsynchBatchTaskExecutorWrkQueueSizeRequest){
						mger.msg(
							"AsynchWrk: got PDAsynchBatchTaskExecutorWrkQueueSizeRequest", 
							2);
						int qsz = _executor.getNumTasksInQueue();
						OKReplyData okdata = new OKReplyData(new Integer(qsz));
						oos.writeObject(okdata);  // no need to call oos.reset() first here
						oos.flush();
					}
					else if (req instanceof TaskObjectsAsynchExecutionRequest) {
						mger.msg("AsynchWrk: got a TaskObjectsAsynchExecutionRequest",2);
						if (req!=null) {
							TaskObjectsAsynchExecutionRequest toaer = 
								(TaskObjectsAsynchExecutionRequest) req;
							if (_GATHER_STATS) {
								synchronized (PDAsynchBatchTaskExecutorWrk.class) {
									long now = System.currentTimeMillis();
									long dur = _lastTimeWhenJobRecvd==0 ? 
										           0 : now-_lastTimeWhenJobRecvd;
									_lastTimeWhenJobRecvd=now;
									_avgTimeBetweenJobsRecvd = 
										(_avgTimeBetweenJobsRecvd*_numJobsRecvd+dur) / 
										++_numJobsRecvd;
									_numTasksRecvd += toaer._tasks.length;
								}
							}
							Vector tasks = new Vector();
							for (int i=0; i<toaer._tasks.length; i++)
								tasks.addElement(toaer._tasks[i]);
							//  process request
							_executor.executeBatch(tasks);
							mger.msg("AsynchWrk: TaskObjectsAsynchExecutionRequest w/ size="+
								       tasks.size()+
								       " successfully submitted to pool",2);
							oos.writeObject(res);  // no need to call oos.reset() here
							oos.flush();
							mger.msg("AsynchWrk: sent an OKReply response",2);
						}
					}
					else {
						throw new PDAsynchBatchTaskExecutorException(
							          "AsynchWrk: cannot parse request");
					}
        }
        catch (SocketException e) {
          //e.printStackTrace();
          mger.msg("Socket Exception caught.Exiting.",0);
          break;
        }
        catch (IOException e2) {
          //e2.printStackTrace();
          mger.msg("I/O Exception caught.Exiting.",0);
          break;
        }
        catch (ClassCastException e3) {
          e3.printStackTrace();
          // don't disconnect
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
			mger.msg("AsynchWrk: finally, synchronizing on _sync", 0);
			synchronized (_sync) {
				if (!_isDone) {
					if (ois!=null) ois.close();
					if (oos!=null) oos.close();
					if (_s!=null) _s.close();
					if (_executor!=null) {
						mger.msg("AsynchWrk: shutting down executor...", 0);
						_executor.shutDownAndWait4Threads2Finish();
					}
					_isDone=true;
				}
			}
      mger.msg("AsynchWrk: Closed Connection to Srv at "+
				       "address(host,port)=("+_host+","+_port+")",0);
    }
  }

	
	/**
	 * hook method that allows the objects that this worker runs to ask for its 
	 * status.
	 * @return boolean
	 */
	public static boolean isClosing() {
		return _isClosing;
	}
	

	/**
	 * hook method that allows the objects that this worker runs to ask for its 
	 * status regarding accepting server requests.
	 * @return boolean
	 */
	public static boolean getServerRequestsDisabled() {
		return _srvReqsDenied;
	}

	/**
	 * hook method that allows the objects that this worker runs to set the status
	 * of the corresponding variable, and thus to disable or enable the worker in
	 * the running JVM to accept or deny server requests.
	 * @param flag boolean
	 */
	public static void setServerRequestsDisabled(boolean flag) {
		_srvReqsDenied = flag;
	}
	
	
	/**
	 * return the number of tasks in this worker's executor's queue. Should only
	 * be called by TaskObject's running in this worker's executor's threads.
	 * @return int
	 */
	public static int getNumTasksInQueue() {
		return _executor.getNumTasksInQueue();
	}
	
	
	/**
	 * return the number of tasks in this worker's executor's queue. Should only
	 * be called by TaskObject's running in this worker's executor's threads.
	 * @return int
	 */
	public static int getNumThreads() {
		return _executor.getNumThreads();
	}
	
	
	/**
	 * removes from executor all tasks currently after the given position, and 
	 * returns them to the caller.
	 * @param pos int
	 * @return TaskObject[] may be null if there weren't more than pos tasks in
	 * executor queue
	 */
	public static TaskObject[] getAllTasksAfterPos(int pos) {
		return _executor.popAllTasksAfterPos(pos);
	}
	
	
	/**
	 * allow code from a <CODE>TaskObject</CODE> executing in one of this worker's
	 * executor's threads, to submit a batch of tasks to this worker's executor.
	 * Should only be called by TaskObject's running in this worker's executor's 
	 * threads.
	 * @param tasks Collection  // Collection&lt;TaskObject&gt;
	 * @return boolean  // false iff the worker was in shutting-down state when
	 * the method was invoked
	 * @throws ParallelException if the method is executed after the worker has
	 * entered shut-down state (not very likely, but certainly possible)
	 */
	public static boolean executeBatch(Collection tasks) 
		throws ParallelException {
		if (!_isClosing) {
			_executor.executeBatch(tasks);
			return true;
		}
		else return false;
	}

	
  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; 
	 * parallel.distributed.PDAsynchBatchTaskExecutorWrk 
	 * [numthreads(10)] [host(localhost)] [port(7980)] [runInitCmd(false)] </CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    int numthreads = 10;
    String host = "localhost";
    int port = 7980;
		boolean runInitCmd = false;

    // register handle to close socket if we stop the program via ctrl-c
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
				System.err.println("Wrk: in shut-down hook.");
				_isClosing=true;
				synchronized (_sync) {
					if (!_isDone) {
						try {
							if (_executor!=null) {
								System.err.println("Wrk: gracefully shutting down executor...");
								// print all diagnostic messages
								utils.Messenger.getInstance().setDebugLevel(Integer.MAX_VALUE);  
								_executor.shutDownAndWait4Threads2Finish();
								System.err.println("Wrk: shut-down hook process completed.");
							}
							System.err.println("Wrk: shutting down socket connection to Srv");
							// itc: HERE shutting down output and closing _s may not be needed
							_s.shutdownOutput(); // Sends the 'FIN' on the network
							_s.close(); // Now we can close the Socket
							_isDone=true;
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
      }
    }
    );

    if (args.length>0) {
      try {
        numthreads = Integer.parseInt(args[0]);
        if (args.length>1) {
          host = args[1];
        }
        if (args.length>2) {
          port = Integer.parseInt(args[2]);
        }
        if (args.length>3) {
          // runInitCmd = Boolean.parseBoolean(args[3]);
          if (args[3].toLowerCase().startsWith("t"))
            runInitCmd = true;
          else runInitCmd = false;
        }
      }
      catch (Exception e) {
        usage();
        System.exit(-1);
      }
    }
		
    final PDAsynchBatchTaskExecutorWrk worker = 
			new PDAsynchBatchTaskExecutorWrk(numthreads, host, port, runInitCmd);
		// running the worker in a separate demon thread is needed, so that in the
		// event of a Ctrl-c the main thread will halt, but the worker thread will
		// keep running together with the _executor threads, and the shutdown-hook
		// thread as well, allowing for clean termination, assuming of course that
		// the tasks currently running in the executor don't need the connection to
		// the Srv.
		Thread rt = new Thread(new Runnable() {
			public void run() {
				try {
					worker.run();
				}
				catch (IOException e) {
			     utils.Messenger.getInstance().msg(
						 "AsynchWrk: exits due to IOException.",0);
				}
				catch (Exception e) {
					utils.Messenger.getInstance().msg("AsynchWrk: unexpected exception '"+
						                                e.toString()+
						                                "' caught...JVM will exit...", 0);
					e.printStackTrace();
				}
			}
		});
		rt.setDaemon(true);
		rt.start();
		
		// start also a debugging thread that will be printing frequently the load
		// and other statistics of this worker
		Thread st = new Thread(new Runnable() {
			public void run() {
				int cnt=0;
				while (++cnt>=0) {  // this will eventually stop when int overflows
					try {
						Thread.sleep(60000);  // sleep for 1 minute
						synchronized (PDAsynchBatchTaskExecutorWrk.class) {
							if (_executor!=null) {
								System.err.println("Wrk: #Threads-busy="+
									                 _executor.getNumBusyThreads()+
									                 " #TasksInQueue="+
									                 _executor.getNumTasksInQueue()+" #Threads="+
									                 _executor.getNumThreads());
								System.err.println("Wrk: AvgTimeBetweenJobsRecvd="+
									                 _avgTimeBetweenJobsRecvd+" #JobsRecvd="+
									                 _numJobsRecvd+" #TasksRecvd="+
									                 _numTasksRecvd);
							}
							else System.err.println("Wrk: _executor still null after "+cnt+
								                      " minutes (approximately)");
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
		st.setDaemon(true);  // stop when main stops
		st.start();
		
		try {
			rt.join();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
  }
	
	
  private static void usage() {
    System.err.println("usage: java -cp <classpath> "+
			                 "parallel.distributed.PDAsynchBatchTaskExecutorWrk "+
			                 "[numthreads(10)] [host(localhost)] [port(7980)] "+
			                 "[runInitCmd?(false)]");
  }

}

