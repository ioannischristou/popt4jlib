package parallel.distributed;

import parallel.*;
import java.net.*;
import java.io.*;


/**
 * server class implementing distributed Message-Passing between inter-process
 * (and intra-process) threads. Threads must know their own id and that they
 * are unique with respect to any given coordinator name chosen.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DMsgPassingCoordinatorSrv {
  private int _port = 7894;  // default port
  private DynamicAsynchTaskExecutor _pool;
  private long _countConns=0;

  /**
   * constructor specifying the port the server will listen to, and
   * the maximum number of threads in the thread-pool.
   * @param port int if &lt; 1024, the number 7894 is used.
   * @param maxthreads int if &lt; 10000, the number 9999 is used.
   */
  DMsgPassingCoordinatorSrv(int port, int maxthreads) {
    if (port >= 1024)
      _port = port;
    try {
      if (maxthreads<10000)
        maxthreads = 9999;  // the max. number of threads
                            // cannot be restricted as it
                            // may introduce starvation locks.
      _pool = DynamicAsynchTaskExecutor.
								newDynamicAsynchTaskExecutor(2, maxthreads);
    }
    catch (ParallelException e) {
      e.printStackTrace();  // cannot get here
    }
  }


  /**
   * enters an infinite loop, listening for socket connections, and handling
   * the incoming sockets as new Runnable tasks (DMPCTask objects) that are
   * given to the associated thread-pool.
   * @throws IOException
   * @throws ParallelException
   */
  void run() throws IOException, ParallelException {
    ServerSocket ss = new ServerSocket(_port);
		utils.Messenger mger = utils.Messenger.getInstance();
    while (true) {
      mger.msg("DMsgPassingCoordinatorSrv: waiting for socket connection",0);
      Socket s = ss.accept();
      ++_countConns;
      mger.msg("Total "+_countConns+" socket connections arrived.",0);
      handle(s, _countConns);
    }
  }


  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; 
	 * parallel.distributed.DMsgPassingCoordinatorSrv [port(7894)] 
	 * [maxthreads(10000)]
	 * </CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    int port = -1;
    int maxthreads = 10000;
    DMsgPassingCoordinatorSrv srv=null;
    if (args.length>0) {
      port = Integer.parseInt(args[0]);
      if (args.length>1)
        maxthreads = Integer.parseInt(args[1]);
    }
    srv = new DMsgPassingCoordinatorSrv(port, maxthreads);
    try {
      srv.run();
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

	
	/**
	 * creates a task from the arguments and executes it in its thread-pool.
	 * @param s Socket
	 * @param connum long
	 * @throws IOException
	 * @throws ParallelException 
	 */
  private void handle(Socket s, long connum) throws IOException, 
		                                                ParallelException {
    DMPCTask t = new DMPCTask(s, connum);
    _pool.execute(t);
  }


  /**
   * inner helper class, not part of the public API.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class DMPCTask implements Runnable {
    private Socket _s;
    private long _conId;

    DMPCTask(Socket s, long conid) {
      _s = s;
      _conId = conid;
    }

    public void run() {
      utils.Messenger.getInstance().msg("DMPCTask with id="+
				                                _conId+" running...",1);
      ObjectInputStream ois = null;
      ObjectOutputStream oos = null;
      try {
        oos = new ObjectOutputStream(_s.getOutputStream());
        oos.flush();
        ois = new ObjectInputStream(_s.getInputStream());
        DMsgIntf msg = (DMsgIntf) ois.readObject();  // read msg (send or recv)
        msg.execute(oos);
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
      finally {
        try {
          if (oos != null) {
            oos.flush();
            oos.close();
          }
          if (ois != null) ois.close();
          _s.close();
        }
        catch (IOException e) {
          e.printStackTrace();  // shouldn't get here
        }
      }
    }
  }

}

