package parallel.distributed;

import java.util.StringTokenizer;


/**
 * tests the DLock class methods. Must have started the 
 * <CODE>DAccumulatorSrv</CODE> and <CODE>DLockSrv</CODE> servers before 
 * running this class. To fully test DLock[Srv], several JVMs should be started
 * running this same class (with any -different- number of threads each).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DLockTest {
	
  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; parallel.DLockTest [num_threads] [host_lockport_unlockport]</CODE>
   * The program will spawn 100 threads by default, and each thread will lock once.
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      long now = System.currentTimeMillis();
      int n = 100;
      if (args.length > 0)
        n = Integer.parseInt(args[0]);
			DLock lock;
			String host = "localhost";
			int lockport = 7892;
			int unlockport = 7893;
			if (args.length>1) {
				StringTokenizer st = new StringTokenizer(args[1],"_");
				host = st.nextToken();
				lockport = Integer.parseInt(st.nextToken());
				unlockport = Integer.parseInt(st.nextToken());
			}
			lock = DLock.getInstance(host,lockport,unlockport);
      Thread[] _threads = new Thread[n];
      for (int i = 0; i < n; i++) {
        _threads[i] = new DLockThread(i, lock);
        _threads[i].start();
        try {
          Thread.sleep(20);
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      // wait for all threads to finish
      for (int i = 0; i < n; i++) {
        try {
          _threads[i].join();
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      long dur = System.currentTimeMillis() - now;
      System.out.println("total duration (msecs): " + dur);
			// don't try to shut-down the DAccumulatorSrv or the DLockSrv as this 
			// process should not be the only one running for the locks.
    }
    catch (java.io.IOException e3) {
      e3.printStackTrace();
    }
  }

	
	/**
	 * auxiliary inner class for the DLockTest class.
	 * <p>Title: popt4jlib</p>
	 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
	 * <p>Copyright: Copyright (c) 2011</p>
	 * <p>Company: </p>
	 * @author Ioannis T. Christou
	 * @version 1.0
	 */
	static class DLockThread extends Thread {
		private int _i;
		DLock _lock;
		public DLockThread(int i, DLock lock) {
			super("T"+i);
			_i = i;
			_lock = lock;
		}

		public void run() {
			try {
				//System.out.println("Thread-" + _i + " getting lock");
				_lock.getLock();
				System.out.println("Thread-" + _i + " got lock");
				double sum = DAccumulatorClt.getSumNumber();  // must add up to zero
				//System.out.println("Thread-" + _i + " accumulated sum=" + sum + "( expected zero)");
				if (sum>=1 || sum<=-1) {
					System.err.println("ERROR with expected sum occurred. Exiting to block...");
					return;  // does not release the lock, so as to block
				}
				DAccumulatorClt.addNumber(1);  // send 1 to the d-accumulator srv
				sum = DAccumulatorClt.getSumNumber();  // expected is 1
				if (sum>=2 || sum<=0) {
					System.err.println("ERROR with expected sum occurred. Exiting to block...");
					return;  // does not release the lock, so as to block
				}				
				Thread.sleep((long) (1000/(_i+1)));
				DAccumulatorClt.addNumber(-1);  // send -1 to the d-accumulator srv
				sum = DAccumulatorClt.getSumNumber();  // must add up to zero
				//System.out.println("Thread-" + _i + " accumulated sum=" + sum + "( expected zero)");
				if (sum>=1 || sum<=-1) {
					System.err.println("ERROR with expected sum occurred. Exiting to block...");
					return;  // does not release the lock, so as to block
				}
				System.out.println("Thread-" + _i + " releasing lock");
				_lock.releaseLock();
				//System.out.println("Thread-" + _i + " released lock");
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}

