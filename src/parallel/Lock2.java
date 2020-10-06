package parallel;

/**
 * Standard Lock mechanism for thread coordination. This lock mechanism is NOT
 * re-entrant, meaning that if a thread that has called <CODE>getLock()</CODE>
 * on an object lock, calls again <CODE>lock.getLock()</CODE> before first 
 * releasing the lock (via <CODE>lock.releaseLock()</CODE>), the thread will
 * hang there forever (unless some other thread that doesn't have the lock(!), 
 * actually calls <CODE>lock.releaseLock()</CODE>, which would be wrong in so 
 * many ways.) A special mechanism is used to prevent threads coming after 
 * waiting threads during the split second that the lock is free to acquire it.
 * The mechanism safe-guards against spurious wakes (which the <CODE>Lock</CODE>
 * class doesn't) but it only prevents just-arriving threads from stealing from 
 * threads that are actually waiting on the while-loop for the lock to be 
 * released; it doesn't force FCFS order. For FCFS ordering, use the 
 * <CODE>FIFOLock</CODE> class. Notice that under heavily congested situations
 * it can be up to 10x slower than the standard Lock class, so it might not be
 * worth using.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Lock2 {
  private int _i=0;  // _i=0 means lock free, _i=1 means lock taken
  private int _waiting1=0;  // used to give priority to waiting threads
	private int _waiting2=0;  // same as above

	
  /**
   * public no-arg constructor
   */
  public Lock2() { }
	
	
  /**
   * get the lock, wait if necessary for another thread to release it.
   */
  public synchronized void getLock() {
    while (_i==0 && _waiting2>0) {  // don't steal the lock
      ++_waiting1;
      try {
        wait();  
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // recommended behavior
      }
      --_waiting1;
    }
    while (_i!=0) {
      ++_waiting2;
      try {
        wait();
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // recommended behavior
      }
      --_waiting2;
    }
    _i=1;
  }
	
	
	/**
	 * get the lock only if it is immediately available, and there are no other
	 * waiting threads. Method returns immediately.
	 * @return boolean true if the lock was obtained, false otherwise.
	 */
	public synchronized boolean getLockIfAvailable() {
		if (_i==0 && _waiting1+_waiting2==0) {  // don't steal the lock
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

