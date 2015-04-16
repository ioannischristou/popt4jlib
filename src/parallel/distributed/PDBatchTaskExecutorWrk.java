package parallel.distributed;

import java.net.*;
import java.io.*;
import java.util.*;


/**
 * class implements a worker process, that is capable of accepting (through
 * network sockets) arrays of TaskObjects and executes them using a
 * <CODE>parallel.distributed.PDBatchTaskExecutor</CODE>. The process connects
 * on a server (hosting the
 * <CODE>parallel.distributed.PDBatchTaskExecutorSrv</CODE> process) on the
 * dedicated port for worker connections (default 7890) and starts listening in
 * for <CODE>parallel.distributed.TaskObjectsExecutionRequest</CODE> requests,
 * which it then processes and returns the results wrapped in a
 * <CODE>parallel.distributed.TaskObjectsExecutionResults</CODE> object via the
 * connecting socket to the server.
 *
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBatchTaskExecutorWrk {
  private int _numthreads = 10;  // default number of threads of PDBatchTaskExecutor
  private String _host = "localhost";  // default host
  private int _port = 7890;  // default worker port
  static Socket _s = null;


  /**
   * public no-arg constructor will assume host is localhost, workers' port is
   * 7890, and the associated PDBatchTaskExecutor will have 10 threads.
   */
  public PDBatchTaskExecutorWrk() {
  }


  /**
   * constructor provides values for the numthreads, host/port parameters for
   * this worker.
   * @param numthreads int
   * @param host String
   * @param port int
   */
  public PDBatchTaskExecutorWrk(int numthreads, String host, int port) {
    _numthreads = numthreads;
    _host = host;
    _port = port;
  }


  /**
   * auxiliary method called by main()
   * @throws IOException
   */
  private void run() throws IOException {
    _s=null;
    ObjectInputStream ois=null;
    ObjectOutputStream oos=null;
    PDBatchTaskExecutor executor=null;
    try {
      System.out.println("Wrk: About to Connect to Srv at address(host,port)=("+_host+","+_port+")");
      System.out.flush();
      _s = new Socket(_host, _port);
      utils.Messenger.getInstance().msg("Wrk: socket created",1);
      oos = new ObjectOutputStream(_s.getOutputStream());
      oos.flush();
      ois = new ObjectInputStream(_s.getInputStream());
      System.out.println("Wrk: Connected to Srv at address(host,port)=("+_host+","+_port+")");
      System.out.flush();
      executor = PDBatchTaskExecutor.newPDBatchTaskExecutor(_numthreads);
      while (true) {
        try {
          // get a request
          utils.Messenger.getInstance().msg("Wrk: waiting to read a TaskObjectsExecutionRequest",2);
          TaskObjectsExecutionRequest req = (TaskObjectsExecutionRequest) ois.readObject();
          utils.Messenger.getInstance().msg("Wrk: got a TaskObjectsExecutionRequest",2);
          if (req!=null) {
            Vector tasks = new Vector();
            for (int i=0; i<req._tasks.length; i++)
              tasks.addElement(req._tasks[i]);
            //  process the request and get back results
            Vector results = executor.executeBatch(tasks);
            utils.Messenger.getInstance().msg("Wrk: finished processing the TaskObjectsExecutionRequest",2);
            Object[] arr = new Object[results.size()];
            for (int i=0; i<results.size(); i++) {
              arr[i] = results.elementAt(i);
            }
            TaskObjectsExecutionResults res = new TaskObjectsExecutionResults(arr);
            oos.writeObject(res);
            oos.flush();
            utils.Messenger.getInstance().msg("Wrk: sent a TaskObjectsExecutionResults response",2);
          }
        }
        catch (SocketException e) {
          //e.printStackTrace();
          utils.Messenger.getInstance().msg("Socket Exception caught.Exiting.",0);
          break;
        }
        catch (IOException e2) {
          //e2.printStackTrace();
          utils.Messenger.getInstance().msg("I/O Exception caught.Exiting.",0);
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
      utils.Messenger.getInstance().msg("Wrk: Closed Connection to Srv at address(host,port)=("+_host+","+_port+")",0);
    }
  }


  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; parallel.distributed.PDBatchTaskExecutorWrk [numthreads(10)] [host(localhost)] [port(7890)]</CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    int numthreads = 10;
    String host = "localhost";
    int port = 7890;

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
      }
      catch (Exception e) {
        usage();
        System.exit(-1);
      }
    }
    PDBatchTaskExecutorWrk worker = new PDBatchTaskExecutorWrk(numthreads, host, port);
    try {
      worker.run();
    }
    catch (IOException e) {
      //e.printStackTrace();
      utils.Messenger.getInstance().msg("Wrk exits due to IOException.",0);
    }
  }


  private static void usage() {
    System.err.println("usage: java -cp <classpath> parallel.distributed.PDBatchTaskExecutorWrk [numthreads(10)] [host(localhost)] [port(7890)]");
  }

}

