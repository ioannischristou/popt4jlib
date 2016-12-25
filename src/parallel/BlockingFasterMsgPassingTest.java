package parallel;

/**
 * test-driver for the <CODE>BlockingFasterMsgPassingCoordinator</CODE> class.
 * Implements same test as class <CODE>BlockingMsgPassingTest</CODE> class, only
 * using the faster class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class BlockingFasterMsgPassingTest {
  static private int _numThreads = 1000;
  public static int getNumThreads() { return _numThreads; }

	/**
	 * single constructor.
	 */
  public BlockingFasterMsgPassingTest() {
  }

	
	/**
	 * invoke as:
	 * <CODE>java -cp &lt;classpath&gt; parallel.BlockingFasterMsgPassingTest</CODE>.
	 * @param args 
	 */
  public static void main(String[] args) {
    final long start = System.currentTimeMillis();
    for (int i=0; i<_numThreads; i++) {
      MPThread3 ti = new MPThread3(i);
      ti.start();
    }
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        System.out.println("tot_time (msecs)="+(System.currentTimeMillis()-start));
        System.out.flush();
      }
    }
    );

  }

	
	/**
	 * auxiliary inner-class, not part of the public API.
	 */
	static class MPThread3 extends Thread {
		private int _id;

		public MPThread3(int i) {
			_id = i;
		}

		public void run() {
			final int numthreads = BlockingFasterMsgPassingTest.getNumThreads();
			int sendTo = _id+1;
			if (sendTo>=numthreads) sendTo = 0;
			try {
				for (int i = 0; i < 10; i++) {
					if (i>0 || _id>0) {
						BlockingFasterMsgPassingCoordinator.getInstance().recvData(_id);
					}
					System.out.println("Thread-" + _id + " doing iter " + i);
					if (i<9 || _id<numthreads-1) BlockingFasterMsgPassingCoordinator.getInstance().sendData(_id, sendTo, new Object());
				}
			}
			catch (Exception e) { e.printStackTrace(); }
		}
	}

}

