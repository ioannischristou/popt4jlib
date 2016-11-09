package parallel.distributed;

import java.net.*;
import java.io.*;
import java.util.*;
import parallel.FasterParallelAsynchBatchTaskExecutor;


/**
 * class implements a worker process, that is capable of accepting (through
 * network sockets) arrays of TaskObjects and executes them using a
 * <CODE>parallel.FasterParallelAsynchBatchTaskExecutor</CODE>. The process 
 * connects on a server (by default on localhost, hosting the
 * <CODE>parallel.distributed.PDAsynchBatchTaskExecutorSrv</CODE> process) on
 * the dedicated port for worker connections (default 7980) and starts listening
 * in for <CODE>parallel.distributed.TaskObjectsAsynchExecutionRequest</CODE> 
 * requests which it then processes. No results are returned directly to the 
 * server. 
 * Notice that in the event of shutting down this worker via Ctrl-c,
 * the process will attempt to shut-down cleanly, by first disconnecting its 
 * socket connection to the server, then finishing up all received asynch-tasks
 * in the executor's queue, and only after that, exiting. Assuming that tasks
 * do not need the (Wrk-Srv) connection, this process will result in the worker
 * exiting gracefully and cleanly from the network of asynch-servers/workers.
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
	private static boolean _isDone = false;  // condition for termination
	private static boolean _runInitCmd = false;
	
  /**
   * no-arg constructor will assume host is localhost, workers' port is
   * 7980, the associated FasterParallelAsynchBatchTaskExecutor will have 10 
	 * threads, and won't expect an init-cmd first.
   */
  private PDAsynchBatchTaskExecutorWrk() {
  }


  /**
   * constructor provides values for the numthreads, host/port parameters for
   * this worker.
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
      _executor = FasterParallelAsynchBatchTaskExecutor.newFasterParallelAsynchBatchTaskExecutor(_numthreads);
			OKReply res = new OKReply();
      while (r==null) {  // if r!=null, initialization failed, thus exit...
        try {
          // get a request
          mger.msg("AsynchWrk: waiting to read an RRObject",2);
          RRObject req = (RRObject) ois.readObject();
					if (req instanceof PDAsynchBatchTaskExecutorWrkAvailabilityRequest) {
						mger.msg("AsynchWrk: got a PDAsynchBatchTaskExecutorWrkAvailabilityRequest", 2);
						int num_avail = _numthreads - _executor.getNumBusyThreads();
						OKReplyData okdata = new OKReplyData(new Integer(num_avail));
						oos.writeObject(okdata);
						oos.flush();
						mger.msg("AsynchWrk: sent back number of free threads="+num_avail, 2);
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
							mger.msg("AsynchWrk: TaskObjectsAsynchExecutionRequest successfully submitted to pool.",2);
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
						catch (parallel.ParallelException e2) {
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
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; parallel.distributed.PDAsynchBatchTaskExecutorWrk [numthreads(10)] [host(localhost)] [port(7980)] [runInitCmd(false)]</CODE>
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
				synchronized (_sync) {
					if (!_isDone) {
						try {
							System.err.println("Wrk: shutting down socket connection to server.");
							_s.shutdownOutput(); // Sends the 'FIN' on the network
							_s.close(); // Now we can close the Socket
							if (_executor!=null) {
								System.err.println("Wrk: gracefully shutting down executor.");
								_executor.shutDownAndWait4Threads2Finish();
								System.err.println("Wrk: shut-down hook process completed.");
							}
							_isDone=true;
						}
						catch (Exception e) {
							// silently ignore
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
					runInitCmd = Boolean.parseBoolean(args[3]);
				}
      }
      catch (Exception e) {
        usage();
        System.exit(-1);
      }
    }
		
    final PDAsynchBatchTaskExecutorWrk worker = new PDAsynchBatchTaskExecutorWrk(numthreads, host, port, runInitCmd);
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
			     utils.Messenger.getInstance().msg("AsynchWrk exits due to IOException.",0);						
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
    System.err.println("usage: java -cp <classpath> parallel.distributed.PDAsynchBatchTaskExecutorWrk [numthreads(10)] [host(localhost)] [port(7980)] [runInitCmd?(false)]");
  }
	
}

