package parallel.distributed;

/**
 * class implements a server for distributed condition-counter functionality.
 * Multiple JVMs can connect to this server. Each client may as well be 
 * multi-threaded.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DConditionCounterSrv {
  private static int _port = 7899;
	private static int _maxthreads = 10000;


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; parallel.distributed.DConditionCounterSrv [port(7899)] [maxthreads(10000)]</CODE>
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
		if (args.length>1) {
			int maxthreads = Integer.parseInt(args[1]);
			if (maxthreads > 10000) _maxthreads = maxthreads;
			else {
				usage();
				System.exit(-1);
			}
		}
    DMsgPassingCoordinatorSrv coord = new DMsgPassingCoordinatorSrv(_port, _maxthreads);
    try {
      utils.Messenger.getInstance().msg("DConditionCounterSrv.main(): running DMsgPassingCoordinatorSrv",0);
      coord.run();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void usage() {
    System.err.println("usage: java -cp <classpath> parallel.distributed.DConditionCounterSrv [port(7899, must be > 1024)] [maxthreads(10000, must be >= 10000)]");
  }
}

