package parallel;

/**
 * Standard Lock mechanism for thread coordination. This lock mechanism is NOT
 * re-entrant, meaning that if a thread that has called <CODE>getLock()</CODE>
 * on an object lock, calls again <CODE>lock.getLock()</CODE> before first 
 * releasing the lock (via <CODE>lock.releaseLock()</CODE>), the thread will
 * hang there forever (unless some other thread that doesn't have the lock(!), 
 * actually calls <CODE>lock.releaseLock()</CODE>, which would be wrong in so 
 * many ways.)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Lock {
  private int _i=0;  // _i=0 means lock free, _i=1 means lock taken
  private int _waiting=0;  // used to give priority to waiting threads

  /**
   * public no-arg constructor
   */
  public Lock() { }
	
	
  /**
   * get the lock, wait if necessary for another thread to release it.
   */
  public synchronized void getLock() {
    if (_i==0 && _waiting>0) {  // don't steal the lock
      ++_waiting;
      try {
        wait();  // can be awaken by a "spurious wake-up" as well
                 // in which (rare) case, it will indeed steal the lock
				         // if it happens to re-acquire the monitor (2nd time)
				         // before the thread that waits in the while loop below
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // recommended behavior
      }
      --_waiting;
    }
    while (_i!=0) {
      ++_waiting;
      try {
        wait();
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // recommended behavior
      }
      --_waiting;
    }
    _i=1;
  }
	
	
	/**
	 * get the lock only if it is immediately available, and there are no other
	 * waiting threads. Method returns immediately.
	 * @return boolean true if the lock was obtained, false otherwise.
	 */
	public synchronized boolean getLockIfAvailable() {
		if (_i==0 && _waiting==0) {  // don't steal the lock
			_i=1;
			return true;
		}
		return false;
	}


  /**
   * releases the lock. No mechanism exists to verify that the thread calling
   * <CODE>releaseLock()</CODE> actually has the lock, and it is the
   * responsibility of the caller to never release a lock that it does not have.
   */
  public synchronized void releaseLock() {
    _i=0;
    notifyAll();
  }
}

