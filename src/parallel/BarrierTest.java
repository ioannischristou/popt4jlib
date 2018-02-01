package parallel;

/**
 * test-driver for the <CODE>Barrier</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class BarrierTest {
	
	/**
	 * single public constructor.
	 */
  public BarrierTest() {
  }

	
	/**
	 * invoke as:
	 * <CODE>java -cp &lt;classpath&gt; parallel.BarrierTest</CODE>.
	 * @param args 
	 */
  public static void main(String[] args) {
    try {
			long start = System.currentTimeMillis();
      BThread arr[] = new BThread[10];
      Barrier.setNumThreads(arr.length);
      for (int i = 0; i < arr.length; i++) {
        arr[i] = new BThread(i);
        arr[i].start();
      }
			for (int i = 0; i < arr.length; i++) {
				arr[i].join();
			}
			long dur = System.currentTimeMillis()-start;
			System.out.println("done in "+dur+" msecs");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
	
	/**
	 * auxiliary inner class, not part of the public API.
	 */
	static class BThread extends Thread {
		private int _id;
		public BThread(int id) { _id = id; }
		public void run() {
			Barrier b = Barrier.getInstance();
			for (int i=0; i<1000000; i++) {
				//System.out.println("t-id="+_id+" i="+i);
				b.barrier();
			}
		}
	}

}

