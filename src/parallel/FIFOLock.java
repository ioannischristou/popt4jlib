package parallel;

import java.util.List;
import java.util.ArrayList;

/**
 * class that ensures First-In-First-Served lock acquisition among threads.
 * Again there is no mechanism to check whether a thread calling
 * <CODE>releaseLock()</CODE> actually had the lock prior to the call. Again the
 * lock mechanism is NOT reentrant.
 * Uses the specific notification Design Pattern by Tom Cargill.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FIFOLock extends Lock {
  private List _waitingOn;  // itc 20150427: used to be Vector
  private boolean _isFree;

  /**
   * public no-arg constructor
   */
  public FIFOLock() {
    super();
    _waitingOn = new ArrayList();
    _isFree=true;
  }


  /**
   * gets the lock.
   */
  public void getLock() {
    super.getLock();
    if (_isFree && _waitingOn.size()==0) {
      _isFree=false;
      super.releaseLock();
    } else {
      WObject w = new WObject();  // object on which current thread will wait
      synchronized (w) {
        _waitingOn.add(w);
        super.releaseLock();
        while (!w.getIsDone()) {
          try {
            w.wait(); // the wait here is guaranteed to be waken from another
            // thread that will call (later) notify() on w -when this
            // thread's turn comes, and thus is not unconditional.
            // The while-loop protects against "spurious wake-ups".
          }
          catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
        super.getLock();
      }
      _waitingOn.remove(0);  // w is always at position 0
      _isFree=false;
      super.releaseLock();
    }
  }


  /**
   * releases the lock.
   */
  public void releaseLock() {
    super.getLock();
    _isFree = true;
    if (_waitingOn.size()>0) {
      WObject w = (WObject) _waitingOn.get(0);  // notify oldest waiting thread
      synchronized (w) {
        w.setDone();
        w.notify();
      }
    }
    super.releaseLock();
  }

	
	/**
	 * gets the lock if it is immediately available and there are no other waiting
	 * threads to get it. Method returns immediately.
	 * @return boolean true if thread got the lock, false otherwise.
	 */
	public boolean getLockIfAvailable() {
		if (super.getLockIfAvailable()) {
			if (_isFree && _waitingOn.size()==0) {
				_isFree = false;
				super.releaseLock();
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * private helper inner class. Not part of the public API.
	 */
	class WObject {
		boolean _isDone=false;

		public WObject() {
			// no-op
		}
		public void setDone() { _isDone = true; }
		public boolean getIsDone() { return _isDone; }
	}

}

