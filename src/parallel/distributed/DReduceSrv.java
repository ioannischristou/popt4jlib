package parallel.distributed;

/**
 * class implements a server for distributed reduce functionality. Multiple
 * JVMs can connect to this server and ask for reduce ops corresponding to this
 * server. Each client may as well be multi-threaded.
 * Notice that this implementation is modeled after the <CODE>DBarrierSrv</CODE>
 * class in this package.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DReduceSrv {
  private static int _port = 7901;


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; parallel.distributed.DReduceSrv [port(7901)]</CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length>0) {
      int port = Integer.parseInt(args[0]);
      if (port > 1024) _port = port;
      else {
        usage();
        System.exit(-1);
      }
    }
    DActiveMsgPassingCoordinatorLongLivedConnSrv coord = new DActiveMsgPassingCoordinatorLongLivedConnSrv(_port);
    try {
      utils.Messenger.getInstance().msg("DReduceSrv.main(): running DMsgPassingCoordinatorLongLivedConnSrv",0);
      coord.run();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void usage() {
    System.err.println("usage: java -cp <classpath> parallel.distributed.DReduceSrv [port(7901, must be > 1024)]");
  }
}

