package parallel;

import java.util.HashMap;


/**
 * OrderedBarrier guarantees that if threads are synchronized via this class, 
 * any tasks given as args to the <CODE>orderedBarrier(task)</CODE> call, will 
 * execute in the order with which the threads executing them were first 
 * registered with the barrier (via the <CODE>addThread(thread)</CODE> call). 
 * Any such tasks must not be manipulated by other threads after they have been 
 * submitted for execution by the <CODE>orderedBarrier(task)</CODE> call.
 * Behavior is undefined if after some threads have started using the
 * orderedBarrier() call, another thread calls the addThread() method, unless a
 * call to reset() is preceded at a time when all threads have finished waiting
 * at their last barrier point.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class OrderedBarrier {
  private static OrderedBarrier _instance;
  private static HashMap _instances = new HashMap();  // map<String name, Barrier b>
  private HashMap _threads;  // map<Thread id, Integer priority>
  private int _numThreads2Wait4=0;
  private boolean _onHold[];


  /**
   * method must be called before orderedBarrier() can be used. It should be
   * invoked only once by each thread which wishes to coordinate with the
   * orderedBarrier() call.
   * @param t Thread a thread that wishes to be coordinated in the order in
   * which it registers with the OrderedBarrier class
   */
  public synchronized static void addThread(Thread t) {
    if (_instance==null)
      _instance = new OrderedBarrier();
    _instance.incrNumThreads2Wait4(t);
  }


  /**
   * method must be called before orderedBarrier() can be used. It should be
   * invoked only once by each thread which wishes to coordinate with the
   * orderedBarrier() call with the particular group provided by the barriername.
   * @param t Thread a thread that wishes to be coordinated in the order in
   * which it registers with the OrderedBarrier class
   */
  public synchronized static void addThread(Thread t, String barriername) {
    OrderedBarrier instance = (OrderedBarrier) _instances.get(barriername);
    if (instance==null) instance = new OrderedBarrier();
    instance.incrNumThreads2Wait4(t);
    _instances.put(barriername, instance);
  }


  /**
   * return the unique OrderedBarrier instance that can exist in a JVM.
   * @return OrderedBarrier
   */
  public synchronized static OrderedBarrier getInstance() {
    return _instance;
  }


  /**
   * returns the instance that was created by a prior call to
   * OrderedBarrier.addThread(t,name)
   * @param name String
   * @return Barrier
   */
  public synchronized static OrderedBarrier getInstance(String name) {
    return (OrderedBarrier) _instances.get(name);
  }


  /**
   * remove the instance named after the argument passed in from the known
   * data-base of Barrier instances (maintained by this class). The caller
   * must ensure that no threads will call the barrier() method for the instance
   * to be deleted again, otherwise deadlocks may arise (some threads waiting
   * on the barrier, and the remaining threads will never be able to call the
   * barrier() method again).
   * @param name String
   * @throws ParallelException if there was no such Barrier instance in the
   * data-base of Barrier instances.
   */
  public synchronized static void removeInstance(String name) throws ParallelException {
    OrderedBarrier b = (OrderedBarrier) _instances.remove(name);
    if (b==null)
      throw new ParallelException("no OrderedBarrier w/ name "+name+" in _instances");
  }


  /**
   * the main method to call when a barrier point must be erected.
   * Each thread that must be co-ordinated, calls this method.
   * Once all threads that have initially called the addThread() method
   * reach this point, the task objects passed to the call are executed in
   * the order with which the threads called the addThread() method.
   * @param task TaskObject the task to execute in order
   * @throws ParallelException if the thread calling the orderedBarrier(task)
   * method has not been "registered" before with a call to addThread(thread)
   * to this OrderedBarrier object.
   */
  public void orderedBarrier(TaskObject task) throws ParallelException {
    synchronized (this) {
      if (_threads.get(Thread.currentThread())==null)
        throw new ParallelException("Thread calling orderedBarrier(t) "+
                                    "is not registered with this OrderedBarrier "+
                                    "object");
      if (_onHold == null) {
        _onHold = new boolean[_threads.size()];
        // _numThreads2Wait4 = _threads.size();
        for (int i = 0; i < _numThreads2Wait4; i++) _onHold[i] = false;
      }
    }
    while (passOrderedBarrier(task)==false) {
			Thread.yield();
      /*
      try {
        Thread.currentThread().sleep(10);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // recommended action
        // no-op
      }
      */
    }
  }


  /**
   * the method resets the OrderedBarrier object.
   * If any threads are still waiting on the barrier, the method throws
   * ParallelException. The method's intent is to clean-up a used-up
   * OrderedBarrier object so that it can be re-used later on (same or other
   * threads calling first the addThread() method and then calling the
   * orderedBarrier() method).
   * @throws ParallelException
   */
  public synchronized void reset() throws ParallelException {
    if (_numThreads2Wait4!=_threads.size()) {
      throw new ParallelException("OrderedBarrier cannot be reset at this point");
    }
    _threads = new HashMap();
    _numThreads2Wait4 = 0;
    _onHold = null;
  }


  /**
   * the method implementing the logic behind the OrderedBarrier concept.
   * It ensures that all participating threads have called the barrier(t) method
   * before it starts executing each thread's task t in ordered sequence.
   * @param t TaskObject
   * @return boolean
   */
  private synchronized boolean passOrderedBarrier(TaskObject t) {
    // check if everybody is out, then return true
    if (_numThreads2Wait4<0) {
      return false;  // thread tried to enter loop before having all others exit the previous one
    }
    // set _onHold[i]
    Integer idI = (Integer) _threads.get(Thread.currentThread());
    int id = idI.intValue();
    _onHold[id] = true;
    // ensure all threads enter the barrier
    --_numThreads2Wait4;
    while (_numThreads2Wait4>0) {
      try {
        wait();
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // recommended action
      }
    }
    // at this point all except me are waiting in the wait() above
    boolean hold = true;
    while (hold) {
      boolean notyet = false;  // notyet is false if all threads before me have exited loop
      for (int i=0; i<id && !notyet; i++) {
        if (_onHold[i]) notyet = true;
      }
      if (notyet==false) {
        _onHold[id] = false;
        hold = false;  // it's my time to exit
        break;
      }
      else notifyAll();  // it's not my time to exit yet, but I have to tell others to wake up
      try {
        wait();
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // recommended action
        // no-op
      }
    }
    if (--_numThreads2Wait4==-_threads.size()) {
      // I am the last thread to pass this point, so reset
      _numThreads2Wait4 = _threads.size();  // reset
    }
    notifyAll();
    if (t!=null) t.run();  // guaranteed that the tasks will execute completely
                           // in start-order of the threads, unless they are
                           // threads themselves in which case execution completion
                           // cannot be guaranteed
                           // this is because of the synchronized method.
    return true;
  }


  /**
   * private constructor, in accordance with the Singleton Design Pattern
   */
  private OrderedBarrier() {
    _threads = new HashMap();
  }


  /**
   * introduced to keep FindBugs happy
   */
  private synchronized void incrNumThreads2Wait4(Thread t) {
    _threads.put(t, new Integer(_numThreads2Wait4++));
  }
}

