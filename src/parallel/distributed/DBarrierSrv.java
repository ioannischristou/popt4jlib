package parallel.distributed;

/**
 * class implements a server for distributed barrier functionality. Multiple
 * JVMs can connect to this server and ask for the barrier corresponding to this
 * server. Each client may as well be multi-threaded.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DBarrierSrv {
  private static int _port = 7896;


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; parallel.distributed.DBarrierSrv [port(7896)]</CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length>0) {
      int port = Integer.parseInt(args[1]);
      if (port > 1024) _port = port;
      else {
        usage();
        System.exit(-1);
      }
    }
    DActiveMsgPassingCoordinatorLongLivedConnSrv coord = new DActiveMsgPassingCoordinatorLongLivedConnSrv(_port);
    try {
      utils.Messenger.getInstance().msg("DBarrierSrv.main(): running DMsgPassingCoordinatorLongLivedConnSrv",0);
      coord.run();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void usage() {
    System.err.println("usage: java -cp <classpath> parallel.distributed.DBarrierSrv [port(7896, must be > 1024)]");
  }
}

