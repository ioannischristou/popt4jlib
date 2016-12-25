package parallel;

/**
 * test-driver class for <CODE>BlockingMsgPassingCoordinator</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0

 */
public class BlockingMsgPassingTest {
  static private int _numThreads = 1000;
  public static int getNumThreads() { return _numThreads; }

	/**
	 * sole constructor.
	 */
  public BlockingMsgPassingTest() {
  }

	/**
	 * invoke as:
	 * <CODE>java -cp &lt;classpath&gt; parallel.BlockingMsgPassingTest</CODE>.
	 * @param args 
	 */
  public static void main(String[] args) {
    final long start = System.currentTimeMillis();
    for (int i=0; i<_numThreads; i++) {
      MPThread2 ti = new MPThread2(i);
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
	static class MPThread2 extends Thread {
		private int _id;

		public MPThread2(int i) {
			_id = i;
		}

		public void run() {
			final int numthreads = BlockingMsgPassingTest.getNumThreads();
			int sendTo = _id+1;
			if (sendTo>=numthreads) sendTo = 0;
			try {
				for (int i = 0; i < 10; i++) {
					if (i>0 || _id>0) BlockingMsgPassingCoordinator.getInstance().recvData(_id);
					System.out.println("Thread-" + _id + " doing iter " + i);
					if (i<9 || _id<numthreads-1) BlockingMsgPassingCoordinator.getInstance().sendData(_id, sendTo, new Object());
				}
			}
			catch (Exception e) { e.printStackTrace(); }
		}
	}

}


