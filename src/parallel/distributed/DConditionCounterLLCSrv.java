package parallel.distributed;

/**
 * Same functionality as <CODE>DConditionCounter[Clt|Srv]</CODE> classes, but
 * use long-lived connections, for clients that call frequently incr/decr ops.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DConditionCounterLLCSrv {
  private static int _port = 7899;


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; parallel.distributed.DConditionCounterLLCSrv [port(7899)]</CODE>
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
      utils.Messenger.getInstance().msg("DConditionCounterLLCSrv.main(): running DActiveMsgPassingCoordinatorLongLivedConnSrv",0);
      coord.run();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void usage() {
    System.err.println("usage: java -cp <classpath> parallel.distributed.DConditionCounterLLCSrv [port(7899, must be > 1024)]");
  }
}

