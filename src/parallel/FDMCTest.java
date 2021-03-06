package parallel;


/**
 * tests the FairDMCoordinator class methods.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FDMCTest {

  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; parallel.FDMCTest</CODE>.
   * The program will spawn 1000 threads, 25% of whom will require write access
   * and the rest read access. A writer thread will sleep for less than 100 ms
   * whereas a reader thread will sleep for less than 50 ms. After its sleep
   * time every thread will exit. The purpose is to ensure threads are given
   * access in a FIFO order (at least the relation between readers and writers).
   * @param args String[]
   */
  public static void main(String[] args) {
    for (int i=0; i<1000; i++) {
      FDMCThread ti = new FDMCThread(i);
      ti.start();
      try {
        Thread.sleep(20);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

	
	/**
	 * auxiliary inner-class not part of the public API.
	 */
	static class FDMCThread extends Thread {
		private int _i;
		public FDMCThread(int i) {
			super("T"+i);
			_i = i;
		}

		public void run() {
			try {
				boolean is_writer = _i % 4 == 0 || _i % 5 == 0;
				if (is_writer) {
					System.out.println("Thread-" + _i + " getting write access");
					FairDMCoordinator.getInstance().getWriteAccess();
					System.out.println("Thread-" + _i + " got write access");
					Thread.sleep((long) (Math.random() * 100));
					FairDMCoordinator.getInstance().releaseWriteAccess();
					System.out.println("Thread-" + _i + " released write access");
				}
				else {
					System.out.println("Thread-" + _i + " getting read access");
					FairDMCoordinator.getInstance().getReadAccess();
					System.out.println("Thread-" + _i + " got read access");
					Thread.sleep((long) (Math.random() * 50));
					FairDMCoordinator.getInstance().releaseReadAccess();
					System.out.println("Thread-" + _i + " released read access");
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}

