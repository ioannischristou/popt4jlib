package parallel.distributed;


/**
 * tests the DLock class methods.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DLockTest {

  /**
   * invoke as <CODE>java -cp &ltclasspath&gt parallel.DLockTest [num_threads] </CODE>
   * The program will spawn 100 threads by default, and each thread will lock once.
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      long now = System.currentTimeMillis();
      int n = 100;
      if (args.length > 0)
        n = Integer.parseInt(args[0]);
      DLock lock = DLock.getInstance();
      Thread[] _threads = new Thread[n];
      for (int i = 0; i < n; i++) {
        _threads[i] = new DLockThread(i, lock);
        _threads[i].start();
        try {
          Thread.sleep(20);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
      // wait for all threads to finish
      for (int i = 0; i < n; i++) {
        try {
          _threads[i].join();
        }
        catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      long dur = System.currentTimeMillis() - now;
      System.out.println("total duration (msecs): " + dur);
    }
    catch (java.io.IOException e3) {
      e3.printStackTrace();
    }
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
class DLockThread extends Thread {
  private int _i;
  DLock _lock;
  public DLockThread(int i, DLock lock) {
    super("T"+i);
    _i = i;
    _lock = lock;
  }

  public void run() {
    try {
      System.out.println("Thread-" + _i + " getting lock");
      _lock.getLock();
      System.out.println("Thread-" + _i + " got lock");
      Thread.sleep((long) (1000/(_i+1)));
      System.out.println("Thread-" + _i + " releasing lock");
      _lock.releaseLock();
      System.out.println("Thread-" + _i + " released lock");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}

