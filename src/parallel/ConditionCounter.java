package parallel;

/**
 * implements a "condition" counter, so that when the object's counter reaches
 * a specified number, it releases a notification for any waiting thread to
 * resume execution. The class is useful in combination with the
 * <CODE>parallel.ParallelAsynchBatchTaskExecutor</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ConditionCounter {
  private long _count=0;
  private long _target=0;
  private boolean _isSatisfied=false;

  /**
   * public no-arg constructor, sets the object's taget count to zero.
   */
  public ConditionCounter() {
  }

  /**
   * specifies the target for the counter to reach (exactly).
   * @param t long
   */
  public ConditionCounter(long t) {
    _target = t;
  }


  /**
   * increments object's counter by 1, and issues a notification if the counter
   * after the update equals the target.
   */
  public synchronized void increment() {
    if (++_count == _target) {
      _isSatisfied = true;
      notifyAll();
    } else _isSatisfied = false;
  }


  /**
   * decrements object's counter by 1, and issues a notification if the counter
   * after the update equals the target.
   */
  public synchronized void decrement() {
    if (--_count == _target) {
      _isSatisfied = true;
      notifyAll();
    } else _isSatisfied = false;
  }


  /**
   * increments object's counter by c, and issues a notification if the counter
   * after the update equals the target.
   * @param c long
   */
  public synchronized void add(long c) {
    _count += c;
    if (_count == _target) {
      _isSatisfied = true;
      notifyAll();
    } else _isSatisfied = false;
  }


  /**
   * decrements object's counter by c, and issues a notification if the counter
   * after the update equals the target.
   * @param c long
   */
  public synchronized void subtract(long c) {
    _count -= c;
    if (_count == _target) {
      _isSatisfied = true;
      notifyAll();
    } else _isSatisfied = false;
  }


  /**
   * causes the current thread to wait until a notification is issued by another
   * thread invoking one of the other methods of this object that issue
   * notifications.
   */
  public synchronized void await() {
    while (_isSatisfied==false) {
      try {
        wait();
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }


  /**
   * resets this object's counter to zero, and sets the object's
   * <CODE>_isSatisfied</CODE> flag to false, for use in another "countdown"
   * process.
   */
  public synchronized void reset() {
    _count = 0;
    _isSatisfied = false;
  }
}

