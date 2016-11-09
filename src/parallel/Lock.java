package parallel;

/**
 * Standard Lock mechanism for thread coordination.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Lock {
  private int _i=0;
  private int _waiting=0;  // used to give priority to waiting threads

  /**
   * public no-arg constructor
   */
  public Lock() { }


  /**
   * gets the lock, waiting if necessary
   */
  public synchronized void getLock() {
    if (_i==0 && _waiting>0) {  // don't steal the lock
      ++_waiting;
      try {
        wait();  // can be awaken by a "spurious wake-up" as well
                 // in which case, it will then act "greedily" and
                 // go directly to the while-loop
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
	 * get the lock only if it immediately available, with no waiting needed. 
	 * Method returns immediately.
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

