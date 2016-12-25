package parallel.distributed;

import java.net.*;
import java.io.*;
import java.util.*;
import parallel.FasterParallelAsynchBatchTaskExecutor;
import parallel.SimpleFasterMsgPassingCoordinator;


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
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class PDAsynchBatchTaskExecutorWrk {
  private static int _numthreads = 10;  // default number of threads of FasterParallelAsynchBatchTaskExecutor
  private static String _host = "localhost";  // default host
  private static int _port = 7980;  // default worker port
  private static Socket _s = null;
	private static FasterParallelAsynchBatchTaskExecutor _executor = null;
	private static final Object _sync = new Object();  // synch object needed during termination via Ctrl-c
	private static volatile boolean _isClosing = false;  // set during termination
	// being a volatile boolean variable, _isClosing needs no further 
	// synchronization for read/write accessing even in JDK1.4
	private static boolean _isDone = false;  // condition for termination
	private static boolean _runInitCmd = false;
	private static int _queueSize = SimpleFasterMsgPassingCoordinator.getMaxSize();

  /**
   * no-arg constructor will assume host is localhost, workers' port is
   * 7980, the associated FasterParallelAsynchBatchTaskExecutor will have 10
	 * threads, won't expect an init-cmd first, and will have an associated 
	 * executor's queue size equal to 10000 (default for the 
	 * <CODE>SimpleFasterMsgPassingCoordinator</CODE> class).
   */
  private PDAsynchBatchTaskExecutorWrk() {
		// no-op
  }


  /**
   * constructor provides values for the numthreads, host/port parameters, 
	 * runinitcmd and queue-size for this worker.
   * @param numthreads int
   * @param host String
   * @param port int
	 * @param runinitcmd boolean
	 * @param queuesize int
   */
  private PDAsynchBatchTaskExecutorWrk(int numthreads, String host, int port,
		                                   boolean runinitcmd, int queuesize) {
    _numthreads = numthreads;
    _host = host;
    _port = port;
		_runInitCmd = runinitcmd;
		_queueSize = queuesize;
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
      mger.msg("AsynchWrk: About to Connect to AsynchSrv at address(host,port)=("+_host+","+_port+")",0);
      _s = new Socket(_host, _port);
      mger.msg("AsynchWrk: socket created",1);
      oos = new ObjectOutputStream(_s.getOutputStream());
      oos.flush();
      ois = new ObjectInputStream(_s.getInputStream());
      mger.msg("AsynchWrk: Connected to AsynchSrv at address(host,port)=("+_host+","+_port+")",0);
			RRObject r = null;
			if (_runInitCmd) {  // expect the first object to arrive from the socket
				                  // to be an init-cmd
				try {
					r = (RRObject) ois.readObject();
					if (r instanceof PDAsynchInitCmd) {
						r.runProtocol(null, null, null);
						mger.msg("AsynchWrk: executed the initialization command received from server", 1);
						r = null;
					}
				}
				catch (Exception e) {
					mger.msg("AsynchWrk: failed to receive or execute an init-cmd from server, will exit.", 1);
				}
			}
			// tasks should never execute on the current thread if the executor is
			// full, as this would force the main thread to become unresponsive to
			// incoming requests through the socket.
			
      _executor = FasterParallelAsynchBatchTaskExecutor.
				newFasterParallelAsynchBatchTaskExecutor(_numthreads,false,_queueSize);
			OKReply res = new OKReply();
      while (r==null) {  // if r!=null, initialization failed, thus exit...
        try {
          // get a request
          mger.msg("AsynchWrk: waiting to read an RRObject",2);
          RRObject req = (RRObject) ois.readObject();
					if (isClosing()) {
						mger.msg("AsynchWrk: in closing state, sending to Srv FailedReply to terminate Worker Connection", 0);
						FailedReply fr = new FailedReply();
						oos.writeObject(fr);
						oos.flush();
						break;
					}
					if (req instanceof PDAsynchBatchTaskExecutorWrkAvailabilityRequest) {
						mger.msg("AsynchWrk: got a PDAsynchBatchTaskExecutorWrkAvailabilityRequest", 2);
						int num_avail = _numthreads - _executor.getNumBusyThreads();
						OKReplyData okdata = new OKReplyData(new Integer(num_avail));
						oos.writeObject(okdata);
						oos.flush();
						//int qsz = _executor.getNumTasksInQueue();
						//mger.msg("AsynchWrk: sent back number of free threads="+num_avail+" (extor queue size="+qsz+")", 2);
					}
					else if (req instanceof PDAsynchBatchTaskExecutorWrkCapacityRequest) {
						mger.msg("AsynchWrk: got a PDAsynchBatchTaskExecutorWrkCapacityRequest", 2);
						int size = ((PDAsynchBatchTaskExecutorWrkCapacityRequest) req).getSize();
						boolean cap = _executor.isBatchSubmissionOK(size);
						OKReplyData reply = new OKReplyData(cap ? new Integer(1) : new Integer(0));
						oos.writeObject(reply);
						oos.flush();
						//int qsz = _executor.getNumTasksInQueue();
						//mger.msg("AsynchWrk: sent back capacity="+cap+" (extor queue size="+qsz+")", 2);
					}
					else if (req instanceof TaskObjectsAsynchExecutionRequest) {
						mger.msg("AsynchWrk: got a TaskObjectsAsynchExecutionRequest",2);
						if (req!=null) {
							TaskObjectsAsynchExecutionRequest toaer = (TaskObjectsAsynchExecutionRequest) req;
							Vector tasks = new Vector();
							for (int i=0; i<toaer._tasks.length; i++)
								tasks.addElement(toaer._tasks[i]);
							//  process request
							_executor.executeBatch(tasks);
							mger.msg("AsynchWrk: TaskObjectsAsynchExecutionRequest successfully submitted to pool",2);
							oos.writeObject(res);
							oos.flush();
							mger.msg("AsynchWrk: sent an OKReply response",2);
						}
					}
					else {
						throw new PDAsynchBatchTaskExecutorException("AsynchWrk: cannot parse request");
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
			synchronized (_sync) {
				if (!_isDone) {
					if (ois!=null) ois.close();
					if (oos!=null) oos.close();
					if (_s!=null) _s.close();
					if (_executor!=null) {
						try {
							_executor.shutDownAndWait4Threads2Finish();
						}
						catch (parallel.ParallelExceptionUnsubmittedTasks e2) {
							e2.printStackTrace();
						}
					}
					_isDone=true;
				}
			}
      mger.msg("AsynchWrk: Closed Connection to Srv at address(host,port)=("+_host+","+_port+")",0);
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
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; parallel.distributed.PDAsynchBatchTaskExecutorWrk [numthreads(10)] [host(localhost)] [port(7980)] [runInitCmd(false)] [maxqueuesize(10000)]</CODE>
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
				System.err.println("Wrk: in shut-sown hook.");
				_isClosing=true;
				synchronized (_sync) {
					if (!_isDone) {
						try {
							if (_executor!=null) {
								System.err.println("Wrk: gracefully shutting down executor...");
								utils.Messenger.getInstance().setDebugLevel(Integer.MAX_VALUE);  // print all diagnostic messages
								_executor.shutDownAndWait4Threads2Finish();
								System.err.println("Wrk: shut-down hook process completed.");
							}
							System.err.println("Wrk: shutting down socket connection to server.");
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
					if (args.length>4) {
						_queueSize = Integer.parseInt(args[4]);
					}
        }
      }
      catch (Exception e) {
        usage();
        System.exit(-1);
      }
    }

    final PDAsynchBatchTaskExecutorWrk worker = new PDAsynchBatchTaskExecutorWrk(numthreads, host, port, runInitCmd, _queueSize);
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
			     utils.Messenger.getInstance().msg("AsynchWrk: exits due to IOException.",0);
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
		try {
			rt.join();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
  }
	
	
  private static void usage() {
    System.err.println("usage: java -cp <classpath> parallel.distributed.PDAsynchBatchTaskExecutorWrk [numthreads(10)] [host(localhost)] [port(7980)] [runInitCmd?(false)] [maxqueuesize(10000)]");
  }

}

