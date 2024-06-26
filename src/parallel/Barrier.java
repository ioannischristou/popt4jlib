package parallel;

import java.util.HashMap;


/**
 * A class implementing the Barrier concept in parallel/distributed computing.
 * Before thread groups wishing to coordinate on barrier points can use this
 * class, a "setup" must occur either via a call to Barrier.setNumThreads(n) or
 * (recommended) Barrier.setNumThreads(groupname, numthreads). Each of the n
 * threads to "synchronize" on the barrier point then calls
 * Barrier.getInstance([groupname]).barrier();
 * The threads wait at this point until n threads actually reach this point,
 * at which time, they resume execution.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Barrier {
  private static Barrier _instance;
  private static HashMap _instances = new HashMap();  // map<String name, Barrier b>
  private int _origNumThreads;
  private int _numThreads2Wait4;


  /**
   * method must be called before barrier() can be used. It should be invoked
   * only once for each group of threads which to coordinate with the barrier()
   * call.
   * @param n int
   * @throws ParallelException if it is called twice
   */
  public synchronized static void setNumThreads(int n) throws ParallelException {
    if (_instance!=null)
      throw new ParallelException("call to Barrier.setNumThreads(n) already occured once");
    _instance = new Barrier(n);
    _instance.setInstanceOrigNumThreads(n);  // introduced to keep FindBugs happy
  }


  /**
   * method has the same purpose as the <CODE>setNumThreads(n)</CODE> call, 
	 * except that it creates a separate Barrier instance that can be referenced 
	 * by the <CODE>getInstance(name)</CODE> call, so that several 
	 * <CODE>Barrier</CODE> objects can co-exist in the same program.
   * @param name String the name with which the Barrier object can be later got
   * @param n int the number of threads that will be coordinated on this Barrier
   * @throws ParallelException if there has been a prior call with the same name
   */
  public synchronized static void setNumThreads(String name, int n) throws ParallelException {
    if (_instances.get(name)!=null)
      throw new ParallelException("call to Barrier.setNumThreads("+name+", n) already occured");
    Barrier b = new Barrier(n);
    synchronized (b) {  // keep FindBugs happy
      b._origNumThreads = n;
    }
    _instances.put(name, b);
  }


  /**
   * returns the instance that was created by a prior call to 
	 * <CODE>setNumThreads(n)</CODE>.
   * @return Barrier null if the required call to set the number of threads has
	 * not yet occurred
   */
  public synchronized static Barrier getInstance() {
    return _instance;
  }


  /**
   * returns the instance that was created by a prior call to
   * <CODE>Barrier.setNumThreads(name,n)</CODE>.
   * @param name String
   * @return Barrier null if no instance by this name exists
   */
  public synchronized static Barrier getInstance(String name) {
    return (Barrier) _instances.get(name);
  }


  /**
   * remove the instance named after the argument passed in from the known
   * data-base of Barrier instances (maintained by this class). The caller
   * must ensure that no threads will call the barrier() method for the instance
   * to be deleted again, otherwise deadlocks may arise (some threads waiting
   * on the barrier, and the remaining threads will never be able to call the
   * barrier() method again unless they maintained a local reference to it).
   * @param name String
   * @throws ParallelException if there was no such <CODE>Barrier</CODE> 
	 * instance in the data-base of Barrier instances.
   */
  public synchronized static void removeInstance(String name) throws ParallelException {
    Barrier b = (Barrier) _instances.remove(name);
    if (b==null)
      throw new ParallelException("no Barrier w/ name "+name+" in _instances");
  }


  /**
   * main method of the class: it waits until _origNumThreads threads enter
   * this method, and it guarantees that if a thread enters this method again
   * before all threads have exited the previous barrier() call it will wait
   * first for all the other threads to exit and then will proceed.
   */
  public void barrier() {
    while (passBarrierFast()==false) {  // used to be passBarrier()
      // repeat: this is not busy-waiting behavior except for the case
      // when a thread has passed the barrier and is calling again
      // the barrier() method before all other threads exit the previous call
      /*
      try {
        Thread.currentThread().sleep(1);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      */
    }
  }


  /**
   * private constructor in accordance with the Singleton(s) Design Pattern.
   * @param n int
   */
  private Barrier(int n) {
    _numThreads2Wait4 = n;
  }


  private synchronized void setInstanceOrigNumThreads(int n) {
    _origNumThreads = n;
  }


  private synchronized boolean passBarrier() {
    if (_numThreads2Wait4 < 0) 
			return false;  // thread just ran through to the next barrier point before 
		                 // reseting
    --_numThreads2Wait4;
    while (_numThreads2Wait4>0) {
      try {
        wait();
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // recommended behavior
        // no-op
      }
    }
    if (--_numThreads2Wait4==-_origNumThreads) {
      // I am the last thread to pass this point, so reset
      _numThreads2Wait4 = _origNumThreads;  // reset
    }
    notifyAll();  // wake them all up
    return true;
  }
	

	/**
	 * the method is the same as <CODE>passBarrier()</CODE> except that it tries
	 * to avoid "spurious" notifications that could slow the system down.
	 * @return boolean true only when the barrier has passed
	 */
	private synchronized boolean passBarrierFast() {
    if (_numThreads2Wait4 < 0) 
			return false;  // thread just ran through to the next barrier point before 
		                 // reseting
    --_numThreads2Wait4;
    while (_numThreads2Wait4>0) {
      try {
        wait();
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // recommended behavior
        // no-op
      }
    }
    if (--_numThreads2Wait4==-_origNumThreads) {
      // I am the last thread to pass this point, so reset
      _numThreads2Wait4 = _origNumThreads;  // reset
    } else notify();  // there is still someone else to wake up
    return true;
	}

}

