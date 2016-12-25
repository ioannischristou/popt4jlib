package parallel;

/**
 * test-driver for <CODE>MsgPassingCoordinator</CODE> class (send/recvData()
 * methods).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class MsgPassingTest {
  static private int _numThreads = 10;
	
	/**
	 * get the number of threads used in this test.
	 * @return int
	 */
  public static int getNumThreads() { return _numThreads; }

	
	/**
	 * single no-op constructor.
	 */
  public MsgPassingTest() {
  }

	
	/**
	 * invoke as:
	 * <CODE>java -cp &lt;classpath&gt; parallel.MsgPassingTest</CODE>.
	 * @param args 
	 */
  public static void main(String[] args) {
    MPThread t0 = new MPThread(0);
    t0.start();
    for (int i=1; i<_numThreads; i++) {
      MPThread ti = new MPThread(i);
      ti.start();
    }
    try {
      MsgPassingCoordinator.getInstance().sendData( -1, 0, new Object());
    }
    catch (Exception e) { e.printStackTrace(); }
  }

	
	/**
	 * auxiliary inner-class, not part of the public API.
	 */
	static class MPThread extends Thread {
		private int _id;

		public MPThread(int i) {
			_id = i;
		}

		public void run() {
			final int numthreads = MsgPassingTest.getNumThreads();
			int sendTo = _id+1;
			if (sendTo>=numthreads) sendTo = 0;
			try {
				for (int i = 0; i < 10; i++) {
					MsgPassingCoordinator.getInstance().recvData(_id);
					System.out.println("Thread-" + _id + " doing iter " + i);
					MsgPassingCoordinator.getInstance().sendData(_id, sendTo, new Object());
				}
			}
			catch (Exception e) { e.printStackTrace(); }
		}
	}
	
}


