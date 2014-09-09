package parallel;

import java.util.*;

/**
 * Class implementing the classical notion of Semaphores as "permits".
 * A thread of computation must acquire the Semaphore before proceeding.
 * Once it is done, it releases the Semaphore.
 * This implementation, allows each Semaphore to "give out" multiple permits
 * before it forces a calling thread to wait for the prior release of one of
 * the handed-out permits.
 * It also disallows threads from "releasing" a permit they don't have.
 * The same thread may call "acquire()" as many times as it pleases without
 * incrementing the number of permits handed out. However, it must call the
 * same number of times the "release()" method in order for the permit it
 * holds to be released.
 * Note: A Semaphore with only one permit is equivalent to the atomic class
 * in this package, except that is is impossible for a thread that does not
 * have a permit to release one.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Semaphore {
  private Hashtable _permitThreads = new Hashtable();  // map<Thread t, Integer numpermits>
  private Set _freePermits = new TreeSet();  // Set<Integer permitId>
  private Hashtable _takenPermits = new Hashtable();  // map<Thread t, Integer permitId>
  private int _maxPermits = 1;
  private int _numWaiting = 0;  // used to prevent new threads "stealing"
                                // the semaphore from already waiting ones
                                // that were just notified.

  /**
   * Public Constructor. Constructs a semaphore that will hand out permits to
   * n different threads before forcing another thread to wait for a permit to
   * be released.
   * @param n int the number of maximum permits to concurrently exist for this
   * Semaphore object.
   */
  public Semaphore(int n) {
    _maxPermits = n;
    _numWaiting = 0;
    for (int i=0; i<n; i++) {
      _freePermits.add(new Integer(i));
    }
  }


  /**
   * acquire a permit. The thread calling this method will wait if there exist
   * other threads that have acquired all the concurrently allowed permits and
   * have not released them yet using the release() method. If the calling
   * thread already has a permit, the method returns immediately, however the
   * same thread must call multiple times the release() method (exactly as many
   * as there were calls to acquire()) for the permit to be released.
   * @return int the permit id just(?) acquired
   */
  public synchronized int acquire() {
    Thread current = Thread.currentThread();
    Integer np = (Integer) _permitThreads.get(current);
    if (np!=null) {  // thread already has the permit
      int np1 = np.intValue()+1;
      _permitThreads.put(current, new Integer(np1));
      return ((Integer) _takenPermits.get(current)).intValue();
    }
    // wait for permit to be released
    if (_permitThreads.size() <= _maxPermits-1 && _numWaiting>0) {
      // don't steal the permit: enter waiting state as well
      _numWaiting++;
      try {
        wait();  // in case of a "spurious wakeup"!!!, the thread will simply
                 // go greedily to the while loop (forgetting its nice manners)
      }
      catch (InterruptedException e) {
        current.interrupt();
      }
      _numWaiting--;
    }
    while (_permitThreads.size()==_maxPermits) {
      _numWaiting++;
      try {
        wait();
      }
      catch (InterruptedException e) {
        current.interrupt();
      }
      _numWaiting--;
    }
    // now get the permit and return
    _permitThreads.put(current, new Integer(1));
    int pid = ((Integer) _freePermits.iterator().next()).intValue();
    _takenPermits.put(current, new Integer(pid));
    _freePermits.remove(new Integer(pid));
    return pid;
  }


  /**
   * release the permit the calling thread owns. If the calling thread had called
   * acquire() n times, this method must be called exactly n times before the
   * permit is released.
   * @throws ParallelException if the calling thread does not have a permit on
   * this Semaphore object.
   */
  public synchronized void release() throws ParallelException {
    Thread current = Thread.currentThread();
    Integer np = (Integer) _permitThreads.get(current);
    if (np==null)
      throw new ParallelException("Thread calling release() "+
                                  "does not have a permit on this Semaphore");
    int npm1 = np.intValue()-1;
    if (npm1==0) {
      _permitThreads.remove(current);
      Integer pid = (Integer) _takenPermits.remove(current);
      _freePermits.add(pid);
      notify();  // wake up just one waiting thread
      return;
    }
    else {
      _permitThreads.put(current, new Integer(npm1));
      return;
    }
  }
}

