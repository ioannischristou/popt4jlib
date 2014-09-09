package parallel;

import java.util.Vector;

/**
 * class that ensures First-In-First-Served lock acquisition among threads.
 * Again there is no mechanism to check whether a thread calling
 * <CODE>releaseLock()</CODE> actually had the lock prior to the call.
 * Uses the specific notification Design Pattern by Tom Cargill.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FIFOLock extends Lock {
  private Vector _waitingOn;
  private boolean _isFree;

  /**
   * public no-arg constructor
   */
  public FIFOLock() {
    super();
    _waitingOn = new Vector();
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
      WObject w = new WObject();  // object on which the current thread will wait
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
      WObject w = (WObject) _waitingOn.elementAt(0);  // notify the oldest waiting thread
      synchronized (w) {
        w.setDone();
        w.notify();
      }
    }
    super.releaseLock();
  }
}


class WObject {
  boolean _isDone=false;

  public WObject() {
    // no-op
  }
  public void setDone() { _isDone = true; }
  public boolean getIsDone() { return _isDone; }
}

