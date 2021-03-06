package parallel.distributed;

import java.net.*;
import java.io.*;
import java.util.*;


/**
 * class implements a worker process, that is capable of accepting (through
 * network sockets) arrays of TaskObjects and executes them using a
 * <CODE>parallel.distributed.PDBatchTaskExecutor</CODE>. The process connects
 * on a server (hosting the
 * <CODE>parallel.distributed.PDBTExec[SingleCltWrk]Init[~ed]Srv</CODE> process) 
 * on the dedicated port for worker connections (default 7890) and first listens 
 * for an initialization command sent as an <CODE>RRObject</CODE> object, which 
 * runs by calling its <CODE>runProtocol(null,null,null)</CODE> method, meaning 
 * that during execution, this method must not engage in any communication with 
 * the server object (and will not send any response back); then it starts 
 * listening for <CODE>parallel.distributed.TaskObjectsExecutionRequest</CODE> 
 * requests, which it then processes and returns the results wrapped in a
 * <CODE>parallel.distributed.TaskObjectsExecutionResults</CODE> object via the
 * connecting socket to the server. One exception to the above is when the 
 * initialization command (received first) for the workers is a
 * <CODE>OKReplyRequestedPDBTExecWrkInitCmd</CODE> in which case, the worker
 * will submit an <CODE>OKReply</CODE> to the server upon execution of the cmd.
 * Also, the worker may receive from the server command objects of type
 * <CODE>PDBTExecCmd</CODE>. In this case, it executes the method
 * <CODE>runProtocol(null,null,null)</CODE> on the thread that listens on the 
 * socket connected to the server. If the command happens to be of type
 * <CODE>PDBTExecOnAllThreadsCmd</CODE>, then the worker thread listening on
 * the socket connected to the server, instead calls the 
 * <CODE>PDBatchTaskExecutor.executeTaskOnAllThreads(cmd)</CODE> method on the
 * associated PDBatchTaskExecutor with it. In both cases, after the command
 * executes, an <CODE>OKReply</CODE> is sent back to the server that issued the
 * commands.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBTExecInitedWrk {
  private int _numthreads = 10;  // default #threads of PDBatchTaskExecutor
  private String _host = "localhost";  // default host
  private int _port = 7890;  // default worker port
  static Socket _s = null;


  /**
   * public no-arg constructor will assume host is localhost, workers' port is
   * 7890, and the associated PDBatchTaskExecutor will have 10 threads.
   */
  public PDBTExecInitedWrk() {
  }


  /**
   * constructor provides values for the numthreads, host/port parameters for
   * this worker.
   * @param numthreads int
   * @param host String
   * @param port int
   */
  public PDBTExecInitedWrk(int numthreads, String host, int port) {
    _numthreads = numthreads;
    _host = host;
    _port = port;
  }


  /**
   * auxiliary method called by main().
   * @throws IOException
   */
  private void run() throws IOException {
    _s=null;
    ObjectInputStream ois=null;
    ObjectOutputStream oos=null;
    PDBatchTaskExecutor executor=null;
		utils.Messenger mger = utils.Messenger.getInstance();
    try {
      mger.msg("Wrk: About to Connect to Srv at address(host,port)=("+
				       _host+","+_port+")",0);
      _s = new Socket(_host, _port);
      mger.msg("Wrk: socket created",0);
			mger.msg("Wrk: if not first worker to connect to server, "+
				       "will have to wait until client sends init_cmd to server...", 2);
      oos = new ObjectOutputStream(_s.getOutputStream());
      oos.flush();
      ois = new ObjectInputStream(_s.getInputStream());
      mger.msg("Wrk: Connected to Srv at address(host,port)=("+
				       _host+","+_port+")",0);
			// first, read and execute the initialization command
			mger.msg("Wrk: Waiting to read initialization command...",0);
			RRObject init_cmd = (RRObject) ois.readObject();
			init_cmd.runProtocol(null, null, null);
			mger.msg("Wrk: Executed the initialization command received",0);
			if (init_cmd instanceof OKReplyRequestedPDBTExecWrkInitCmd) {
				oos.writeObject(new OKReply());
				oos.flush();
			}
			// next, continue as usual
      executor = PDBatchTaskExecutor.newPDBatchTaskExecutor(_numthreads);
      while (true) {
        try {
          // get a request
          mger.msg("Wrk: waiting to read a TaskObjectsExecutionRequest "+
						       "or PDBTExecCmd",2);
					RRObject rr = (RRObject) ois.readObject();
					if (rr instanceof PDBTExecCmd) {  // execute on this thread
						mger.msg("Wrk: got a PDBTExecCmd",2);
						if (rr instanceof PDBTExecOnAllThreadsCmd) {
							mger.msg("Wrk: PDBTExecCmd is a PDBTExecOnAllThreadsCmd, "+
								       "executing on all threads", 2);
							executor.executeTaskOnAllThreads((PDBTExecOnAllThreadsCmd) rr);
						} else {
							mger.msg("Wrk: executing PDBTExecCmd on main Wrk thread", 2);
							rr.runProtocol(null, null, null);
						}
						mger.msg("Wrk: sending OKReply as response to PDBTExecCmd to Srv", 2);
						oos.writeObject(new OKReply());  // no need for oos.reset() here
            mger.msg("Wrk: finished processing the PDBTExecCmd",2);
						continue;
					}
          TaskObjectsExecutionRequest req = (TaskObjectsExecutionRequest) rr;
          mger.msg("Wrk: got a TaskObjectsExecutionRequest",2);
          if (req!=null) {
            Vector tasks = new Vector();
            for (int i=0; i<req._tasks.length; i++) {
              tasks.addElement(req._tasks[i]);
						}
            //  process the request and get back results
            Vector results = executor.executeBatch(tasks);
            mger.msg("Wrk: finished processing the TaskObjectsExecutionRequest",
							       2);
            Object[] arr = new Object[results.size()];
            for (int i=0; i<results.size(); i++) {
              arr[i] = results.get(i);
            }
            TaskObjectsExecutionResults res = 
							new TaskObjectsExecutionResults(arr);
						oos.reset();  // force object to be written anew
            oos.writeObject(res);
            oos.flush();
            mger.msg("Wrk: sent a TaskObjectsExecutionResults response",2);
          }
        }
        catch (SocketException e) {
          //e.printStackTrace();
          mger.msg("Socket Exception caught.Exiting.",0);
          break;
        }
        catch (IOException e2) {
          e2.printStackTrace();  // itc: HERE rm asap
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
      // e.printStackTrace();
			mger.msg("PDBTExecInitedWrk.run(): Exception '"+e+
				       "' was thrown. Will close connection, shutdown executor, "+
				       "and exit. ", 0);
    }
    finally {
      if (ois!=null) ois.close();
      if (oos!=null) oos.close();
      if (_s!=null) _s.close();
      if (executor!=null) {
        try {
          executor.shutDown();
        }
        catch (parallel.ParallelException e2) {
          e2.printStackTrace();
        }
      }
      mger.msg("Wrk: Closed Connection to Srv at address(host,port)=("+
				       _host+","+_port+")",0);
    }
  }


  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; parallel.distributed.PDBTExecInitedWrk 
	 * [numthreads(10)] [host(localhost)] [port(7890)] [dbglvl(0)]</CODE>.
   * @param args String[]
   */
  public static void main(String[] args) {
    int numthreads = 10;
    String host = "localhost";
    int port = 7890;
		int lvl = 0;
    // register handle to close socket if we stop the program via ctrl-c
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        try {
          _s.shutdownOutput(); // Sends the 'FIN' on the network
          _s.close(); // Now we can close the Socket
        }
        catch (IOException e) {
          // silently ignore
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
					lvl = Integer.parseInt(args[3]);
				}
      }
      catch (Exception e) {
        usage();
        System.exit(-1);
      }
    }
		utils.Messenger.getInstance().setDebugLevel(lvl);
    PDBTExecInitedWrk worker = new PDBTExecInitedWrk(numthreads, host, port);
    try {
      worker.run();
    }
    catch (IOException e) {
      //e.printStackTrace();
      utils.Messenger.getInstance().msg("Wrk exits due to IOException.",0);
    }
  }


  private static void usage() {
    System.err.println("usage: java -cp <classpath> "+
			                 "parallel.distributed.PDBTExecInitedWrk "+
			                 "[numthreads(10)] [host(localhost)] "+
			                 "[port(7890)] [dbglvl(0)]");
  }

}

