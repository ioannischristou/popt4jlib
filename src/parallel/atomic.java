package parallel;

import java.util.*;


/**
 * A class implementing the <atomic> critical section protocol in Java.
 * Users of this class wishing to ensure that certain pieces of code execute
 * atomically, must enclose the critical section in the calls
 *
 * <li> <CODE>atomic.start(i)</CODE>
 * <li> <CODE> &ltcritical section code&gt </CODE>
 * <li> <CODE>atomic.end(i)</CODE>
 *
 * where <CODE>i >= 0</CODE>.
 * However, the class semantics are such so that it is possible for another
 * thread to release the lock that another thread has acquired. If this
 * possibility is unacceptable, then the class Semaphore can be used
 * instead, which ensures that no release call can occur if the calling
 * thread does not have a permit.
 *
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class atomic {

  private static Hashtable _h = new Hashtable();  // map<Long i, Lock a>

  /**
   * get the lock for critical section i.
   * @param i long
   */
  public static void start(long i) {
    Lock ai = getLock(i);
    ai.getLock();
  }

  /**
   * release the lock of critical section i
   * @param i long
   */
  public static void end(long i) {
    Lock ai = getLock(i);
    ai.releaseLock();
  }


  private static synchronized Lock getLock(long i) {
    Long ii = new Long(i);
    Lock a = (Lock) _h.get(ii);
    if (a==null) {
      a = new Lock();
      _h.put(ii, a);
    }
    return a;
  }
}

