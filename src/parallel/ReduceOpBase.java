package parallel;

import java.util.*;


/**
 * A class implementing the Reduce concept in parallel/distributed computing.
 * Before thread groups wishing to participate in reducing ops can use this
 * class, a "setup" must occur either via a call to ReduceOpBase.addThread(t)
 * or (recommended) ReduceOpBase.addThread(groupname, t). Each of the added
 * threads to execute a reducing operation then calls
 * <CODE>result = ReduceOpBase.getInstance([groupname]).reduce(mydata, op);</CODE>.
 * The threads wait at this point until all threads added actually reach this
 * point, at which time, they execute the reduction operation and return the 
 * data to all calling threads. Once threads start calling
 * the <CODE>reduce(data,op)</CODE> method it is forbidden for other threads to 
 * call <CODE>addThread([name,]t)</CODE> method (behavior becomes undefined).
 * If a thread wishes to leave, it may do so by calling
 * <CODE>ReduceOpBase.removeCurrentThread([groupname])</CODE>
 * The op in <CODE>reduce(data,op)</CODE> operation can differ for each thread,
 * meaning that one thread can participate in the reduction doing a minimization
 * operation, while another can simultaneously be doing a maximization operation.
 * If a thread other than the ones originally entering the reduce-op (via setup)
 * calls the <CODE>reduce(data,op)</CODE> methods then a ParallelException will 
 * be thrown.
 * Notice that this implementation is modeled after the ComplexBarrier class in
 * this package.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ReduceOpBase {
  private static ReduceOpBase _instance;
  private static Hashtable _instances = new Hashtable();  // map<String name, ReduceOpBase b>
  private int _numThreads2Wait4=0;
  private Hashtable _threadsData = new Hashtable();  // map<Thread t, Object data>

	
  /**
   * method must be called before reduce(data) can be used. It should be invoked
   * only once for each group of threads which to execute the reduce(data)
   * call.
   * @param t Thread
   */
  public synchronized static void addThread(Thread t) {
    if (_instance==null) _instance = new ReduceOpBase();
    _instance.placeThread(t);  // introduced to keep FindBugs happy
  }


  /**
   * method has the same purpose as the addThread(t) call, except that it
   * creates a separate ReduceOpBase instance that can be referenced by the
   * getInstance(name) call, so that several reduce-op objects can co-exist in 
	 * the same program.
   * @param name String the name with which the ReduceOpBase object can be later got
   * @param t Thread a thread that will be coordinated on this object
   */
  public synchronized static void addThread(String name, Thread t) {
    ReduceOpBase b=(ReduceOpBase) _instances.get(name);
    if (b==null) b = new ReduceOpBase();
    synchronized (b) {  // keep FindBugs happy
      b.placeThread(t);
    }
    _instances.put(name, b);
  }


  /**
   * removes the current thread from having to synchronize on the default reducer.
   * @throws ParallelException
   */
  public synchronized static void removeCurrentThread() throws ParallelException {
    if (_instance==null) throw new ParallelException("no instance to remove thread from");
    synchronized (_instance) {
      Thread current = Thread.currentThread();
      if (_instance._threadsData.containsKey(current)==false)
        throw new ParallelException("thread not originally participant in this reduce object");
      _instance.removeThread(current);
    }
  }


  /**
   * removes the current thread from having to participate in the reduce with
   * the given name.
   * @param name String
   * @throws ParallelException if the reduce group name does not exist or if the
   * thread was not a participant in this barrier.
   */
  public synchronized static void removeCurrentThread(String name) throws ParallelException {
    ReduceOpBase instance = (ReduceOpBase) _instances.get(name);
    if (instance==null)
      throw new ParallelException("no instance w/ name="+name+" to remove thread from");
    synchronized (instance) {
      Thread current = Thread.currentThread();
      if (instance._threadsData.containsKey(current)==false)
        throw new ParallelException("thread not originally participant in this reduce object");
      instance.removeThread(current);
    }
  }


  /**
   * returns the instance that was created by a prior call to addThread(t)
   * @return ReduceOpBase
   */
  public synchronized static ReduceOpBase getInstance() {
    return _instance;
  }


  /**
   * returns the instance that was created by a prior call to
   * ReduceOpBase.addThread(name,t)
   * @param name String
   * @return ReduceOpBase
   */
  public synchronized static ReduceOpBase getInstance(String name) {
    return (ReduceOpBase) _instances.get(name);
  }


  /**
   * remove the instance named after the argument passed in from the known
   * data-base of reduce instances (maintained by this class). The caller
   * must ensure that no threads will call the reduce(o) method for the instance
   * to be deleted again, otherwise deadlocks may arise (some threads waiting
   * on the reduce, and the remaining threads will never be able to call the
   * reduce(data) method again).
   * @param name String
   * @throws ParallelException if there was no such ReduceOpBase instance in the
   * data-base of reduce instances.
   */
  public synchronized static void removeInstance(String name) throws ParallelException {
    ReduceOpBase b = (ReduceOpBase) _instances.remove(name);
    if (b==null)
      throw new ParallelException("no ReduceOp w/ name "+name+" in _instances");
  }


  /**
   * main method of the class. It waits until all participating threads enter
   * this method, and it guarantees that if a thread enters this method again
   * before all threads have exited the previous reduce() call it will wait
   * first for all the other threads to exit and then will proceed.
   * @param data Object - the data of this thread to participate in the reduce
	 * @param reduceOp ReduceOperator - the operator to apply to the data
	 * @throws ParallelException if current thread is not registered via addThread
	 * @return Object - the result of the reduce operation
   */
  public Object reduce(Object data, ReduceOperator op) throws ParallelException {
    synchronized (this) {
      if (_threadsData.containsKey(Thread.currentThread()) == false)
        throw new ParallelException("current thread cannot call reduce()");
    }
		// first barrier
    while (passBarrier()==false) {
      // repeat: this is not busy-waiting behavior except for the case
      // when a thread has passed the barrier and is calling again
      // the barrier() method before all other threads exit the previous call
      Thread.yield();
    }
		// ok, put data in
		synchronized (this) {
			_threadsData.put(Thread.currentThread(), data);
		}
		// second barrier to ensure everyone has put data in
    while (passBarrier()==false) {
      // repeat: this is not busy-waiting behavior except for the case
      // when a thread has passed the barrier and is calling again
      // the barrier() method before all other threads exit the previous call
      Thread.yield();
    }
		// finally, do the reduction operation (if any), and return the result
		Object result = op==null ? null : op.reduce(_threadsData);
		return result;
  }


  /**
   * private constructor in accordance with the Singleton(s) Design Pattern.
   */
  private ReduceOpBase() {
    // _numThreads2Wait4 = n;
  }


  private synchronized void placeThread(Thread t) {
    _threadsData.put(t,new Object());
    ++_numThreads2Wait4;
  }


  private synchronized void removeThread(Thread t) {
    _threadsData.remove(t);
    if (_numThreads2Wait4>0)  // some threads may have entered barrier, all have left last barrier() call
      --_numThreads2Wait4;
    else if (_numThreads2Wait4<0)
      ++_numThreads2Wait4;  // there are threads still in the previous barrier() call
                            // current thread has already left previous barrier() call
                            // and is now in removeThread()
    if (_numThreads2Wait4==0)  // this would happen only if all other threads
                               // have entered barrier() at this time
      notify();  // all other threads were waiting for me
  }


  private synchronized boolean passBarrier() {
    if (_numThreads2Wait4 < 0) return false;  // thread just ran through to the next barrier point before reseting
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
    if (--_numThreads2Wait4==-_threadsData.size()) {
      // I am the last thread to pass this point, so reset
      _numThreads2Wait4 = _threadsData.size();  // reset
    }
    notifyAll();  // wake them all up
    return true;
  }

}

