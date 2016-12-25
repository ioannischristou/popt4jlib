package parallel;

import java.util.*;
import java.io.*;

/**
 * A faster class that implements thread-pooling to allow its users to execute
 * concurrently batch of tasks implementing the 
 * <CODE>ComparableTaskObject</CODE> interface (or the extending interface,
 * <CODE>ThreadSpecificComparableTaskObject</CODE> encapsulating the requirement
 * that the task be run on a thread with a particular thread-id).
 * The run() method of each task must clearly be thread-safe!, and also, after 
 * calling <CODE>executeBatch(tasks)</CODE>, no thread (including the one in 
 * which the call originated) should manipulate in any way the submitted tasks 
 * or their container (the Collection argument to the call). Unfortunately, 
 * there is no mechanism in the language to enforce this constraint; the user of 
 * the library has to enforce this (mild) constraint in their code.
 * The class utilizes the (faster) Message-Passing mechanism implemented in the
 * <CODE>SimplePriorityMsgPassingCoordinator</CODE> class of this package. The 
 * class itself is thread-safe meaning that there can exist multiple
 * <CODE>FasterParallelAsynchBatchPriorityTaskExecutor</CODE> objects, multiple
 * concurrent threads may call the public methods of the class on the same or
 * different objects as long as the constraints mentioned above are satisfied.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class FasterParallelAsynchBatchPriorityTaskExecutor {
  private static int _nextId = 0;
  private int _id;  // ParallelBatchTaskExecutor id
  private FPABPTEThread[] _threads;
  private boolean _isRunning;
  private boolean _runOnCurrent=true;
  private SimplePriorityMsgPassingCoordinator _spmpc;

	
  /**
   * public factory constructor, constructing a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool
	 * @return FasterParallelAsynchBatchPriorityTaskExecutor properly initialized
   * @throws ParallelException if numthreads &le; 0 or if too many threads are
   * asked to be created.
   */	
	public static FasterParallelAsynchBatchPriorityTaskExecutor newFasterParallelAsynchBatchPriorityTaskExecutor(int numthreads) throws ParallelException {
		FasterParallelAsynchBatchPriorityTaskExecutor ex = new FasterParallelAsynchBatchPriorityTaskExecutor(numthreads);
		ex.initialize();
		return ex;
	}

	
  /**
   * public factory constructor, constructing a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool
   * @param runoncurrent boolean if false no task will run on current thread in
   * case the threads in the pool are full.
	 * @return FasterParallelAsynchBatchPriorityTaskExecutor properly initialized
   * @throws ParallelException if numthreads &le; 0 or if too many threads are
   * asked to be created.
   */		
	public static FasterParallelAsynchBatchPriorityTaskExecutor newFasterParallelAsynchBatchPriorityTaskExecutor(int numthreads, boolean runoncurrent) throws ParallelException {
		FasterParallelAsynchBatchPriorityTaskExecutor ex = new FasterParallelAsynchBatchPriorityTaskExecutor(numthreads, runoncurrent);
		ex.initialize();
		return ex;
	}

	
  /**
   * private constructor, constructing a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool
   * @throws ParallelException if numthreads &le; 0 or if too many threads are
   * asked to be created.
   */
  private FasterParallelAsynchBatchPriorityTaskExecutor(int numthreads) throws ParallelException {
    if (numthreads<=0) throw new ParallelException("constructor arg must be > 0");
    if (numthreads > SimplePriorityMsgPassingCoordinator.getMaxSize()/2)
      throw new ParallelException("cannot construct so many threads");
    _id = getNextObjId();
    _threads = new FPABPTEThread[numthreads];
    /* itc: 2015-15-01: moved to initialize() method
		for (int i=0; i<numthreads; i++) {
      _threads[i] = new FPABPTEThread(this, -(i+1));
      _threads[i].setDaemon(true);  // thread will end when main thread ends
      _threads[i].start();
    }
    _isRunning = true;
    _spmpc = SimplePriorityMsgPassingCoordinator.getInstance("FasterParallelAsynchBatchPriorityTaskExecutor" + _id);
		*/
  }


  /**
   * private constructor, constructing a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool
   * @param runoncurrent boolean if false no task will run on current thread in
   * case the threads in the pool are full.
   * @throws ParallelException if numthreads &le; 0.
   */
  private FasterParallelAsynchBatchPriorityTaskExecutor(int numthreads,
                                                       boolean runoncurrent)
      throws ParallelException {
    this(numthreads);
    _runOnCurrent = runoncurrent;
  }

	
	/**
	 * called exactly once after this object is constructed.
	 */
	private void initialize() {
		final int numthreads = _threads.length;
		for (int i=0; i<numthreads; i++) {
      _threads[i] = new FPABPTEThread(-(i+1));
      _threads[i].setDaemon(true);  // thread will end when main thread ends
      _threads[i].start();
    }
    _isRunning = true;
    _spmpc = SimplePriorityMsgPassingCoordinator.getInstance("FasterParallelAsynchBatchPriorityTaskExecutor" + _id);		
	}
	

  /**
   * get the current number of tasks in the queue awaiting processing.
   * @return int
   */
  public int getNumTasksInQueue() {
    return _spmpc.getNumTasksInQueue();
  }


  /**
   * the main method of the class. Submits all tasks in the argument collection
   * (must be objects implementing the <CODE>ComparableTaskObject</CODE> interface in
   * package <CODE>parallel</CODE>)
   * for execution, and the thread-pool will ignore any exceptions any task may
   * throw when its run() method is invoked.
   * The call is asynchronous, so that the method does return immediately
   * without waiting for any task to complete; however, if all threads in the
   * thread-pool are busy and the
   * <CODE>FasterParallelAsynchBatchPriorityTaskExecutor</CODE> was constructed via the
   * single-argument constructor, or the second argument in the two-argument
   * constructor was true, then the next task executes in the current thread.
   * The call will also block in case the queue of tasks in the thread-pool
   * (implemented in <CODE>SimplePriorityMsgPassingCoordinator</CODE> class of
   * this package) is full (default=10000) in which case the current thread will
   * wait until threads in the thread-pool finish up their tasks so the queue
   * becomes less than full and can accept more tasks.
   *
   * Notice the possibility for locking when tasks sent to this executor are
   * dependent upon latter tasks to be submitted to the same executor, but the
   * executor becomes full (only possible because of the finite capacity of the
   * executor's thread-pool). The application must ensure this situation does
   * not happen (the executor cannot do anything to prevent this). See also the
   * discussion in <CODE>parallel.DynamicAsynchTaskExecutor</CODE>.
   *
   * A synchronous version is implemented in the ParallelBatchTaskExecutor class.
   * @param tasks Collection a Collection of ComparableTaskObject objects
   * @throws ParallelException if the shutDown() method has been called prior
   * to this call
   * @throws ParallelExceptionUnsubmittedTasks if this object does not allow
   * running tasks in the current thread and some tasks could not be sent to
   * the thread-pool due to a full <CODE>SimplePriorityMsgPassingCoordinator</CODE>
   * msg-queue; in this case the un-submitted tasks are returned inside the
   * exception object, along with any object in the <CODE>tasks</CODE> argument
	 * that does not implement the <CODE>ComparableTaskObject</CODE> interface (if 
	 * there is even a single such object, this exception will be thrown as well,
	 * even though all other <CODE>ComparableTaskObject</CODE> objects in the arg
	 * will be submitted normally for execution).
   */
  public void executeBatch(Collection tasks) throws ParallelException, ParallelExceptionUnsubmittedTasks {
    if (tasks == null)return;
    Iterator it = tasks.iterator();
    if (isRunning() == false)
      throw new ParallelException("thread-pool is not running");
    Vector unsubmitted_tasks = new Vector();  // tasks that couldn't be submitted
    Vector tasks_to_run = new Vector();  // tasks to run on same thread
    synchronized (this) {
      while (it.hasNext()) {
        Object t = null;
        ComparableTaskObject task = null;
        try {
          t = it.next();
          task = (ComparableTaskObject) t;
        }
        catch (ClassCastException e) {
          e.printStackTrace();
          unsubmitted_tasks.add(t);
          continue; // ignore task and continue
        }
        if ( (!_runOnCurrent && existsRoom()) || 
								existsIdleThread() || 
								task instanceof ComparablePoissonPill) {  // ok
          _spmpc.sendDataBlocking(task);
        }
        else {
          if (!_runOnCurrent) { // try to submit, if it fails add in Vector,
                                // then send back in exception object.
            try {
              _spmpc.sendData(task);
            }
            catch (ParallelException e) {
              unsubmitted_tasks.add(task);
            }
          }
          else {
            // task.run();
            tasks_to_run.add(task);
          }
        }
      }
    }
    if (tasks_to_run.size()>0) {
      for (int i=0; i<tasks_to_run.size(); i++)
        ((ComparableTaskObject) tasks_to_run.elementAt(i)).run();
    }
    if (unsubmitted_tasks.size()>0) {
      throw new ParallelExceptionUnsubmittedTasks(unsubmitted_tasks);
    }
  }


  /**
   * executes in one of the threads in the thread-pool of this executor the
   * task argument. The task will never be attempted to run on the current 
	 * thread (but it might eventually run on this thread, if this call is made
	 * from one of the executor's threads, and it's the same one eventually chosen
	 * to run the submitted task).
   * @param task ComparableTaskObject
   * @return boolean true iff the task was successfully submitted.
   * @throws ParallelException if the executor is not running
   */
  public boolean execute(ComparableTaskObject task) throws ParallelException {
    if (task == null)return false;
    if (isRunning() == false)
      throw new ParallelException("thread-pool is not running");
    boolean res = true;
    synchronized (this) {
      if ( (!_runOnCurrent && existsRoom()) || existsIdleThread()) { // ok
        _spmpc.sendDataBlocking(task);
      }
      else {
        try {
          _spmpc.sendData(task);
        }
        catch (ParallelException e) {
          res = false;
        }
      }
    }
    return res;
  }


  /**
   * shut-down all the threads in this executor's thread-pool. It is the
   * caller's responsibility to ensure that after this method has been called
   * no other thread (including the threads in the thread-pool) will call the
   * executeBatch() method. Use of condition-counters or similar (e.g. as in the
   * class <CODE>graph.AllMWCFinderBKMT</CODE>) may be necessary when tasks
   * given to the thread-pool for execution may call the
   * <CODE>executeBatch()</CODE> method recursively.
   * The executor cannot be used afterwards, and will throw ParallelException
   * if the method executeBatch() or shutDown() is called again.
   * @throws ParallelException
   * @throws ParallelExceptionUnsubmittedTasks
   */
  public synchronized void shutDown() throws ParallelException, ParallelExceptionUnsubmittedTasks {
    if (_isRunning==false)
      throw new ParallelException("shutDown() has been called already");
    final int numthreads = _threads.length;
    Vector pills = new Vector();
    for (int i=0; i<numthreads; i++) {
      ComparablePoissonPill p = new ComparablePoissonPill();
      pills.addElement(p);
    }
    executeBatch(pills);
    _isRunning = false;
    return;
  }


  /**
   * return true iff it has been shut-down.
   * @return boolean
   */
  public synchronized boolean isShutDown() {
    return _isRunning==false;
  }


  /**
   * return the number of threads in the thread-pool.
   * @return int
   */
  public int getNumThreads() {
    if (_threads!=null)
      return _threads.length;
    else return 0;
  }

	
	/**
	 * method exists for debugging purposes only.
	 * @return String 
	 */
	public String getDataQueueAsString() {
		return _spmpc.toString();
	}

	
  int getObjId() { return _id; }


  /**
   * if true, then a task can be "sent" (submitted) to the thread-pool for
   * processing without causing any waiting on the executeBatch() method.
   * @return boolean
   */
  private synchronized boolean existsRoom() {
    return getNumTasksInQueue() <
           SimplePriorityMsgPassingCoordinator.getMaxSize()-2*_threads.length;
  }


  /**
   * if true there currently (but without guarantee for how long in the presence
   * of multiple threads invoking the executeBatch() method of this object)
   * exists at least one idle thread in the thread-pool.
   * @return boolean
   */
  private synchronized boolean existsIdleThread() {
    for (int i=0; i<_threads.length; i++) {
      if (_threads[i].isIdle()) return true;
    }
    return false;
  }


  private synchronized boolean isRunning() { return _isRunning; }


  private synchronized static int getNextObjId() { return ++_nextId; }

	
	/**
	 * helper inner class for FasterParallelAsynchBatchPriorityTaskExecutor, not 
	 * part of the public API.
	 * <p>Title: popt4jlib</p>
	 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
	 * <p>Copyright: Copyright (c) 2011</p>
	 * <p>Company: </p>
	 * @author Ioannis T. Christou
	 * @version 1.0
	 */
	class FPABPTEThread extends Thread implements popt4jlib.IdentifiableIntf {
		private int _id;
		private boolean _isIdle=true;

		/**
		 * public constructor.
		 * @param id int a negative integer.
		 */
		public FPABPTEThread(int id) {
			_id = id;
		}


		/**
		 * the run() method of the thread, loops continuously, waiting for a task
		 * to arrive via the <CODE>SimplePriorityMsgPassingCoordinator</CODE> class 
		 * and executes it.
		 * Any exceptions the task throws are caught &amp; ignored. In case the data that
		 * arrives is a <CODE>ComparablePoissonPill</CODE>, the thread exits its 
		 * <CODE>run()</CODE> loop.
		 */
		public void run() {
			final int fpbteid = getObjId(); // _e.getObjId();
			final SimplePriorityMsgPassingCoordinator fpabptetmpc =
					SimplePriorityMsgPassingCoordinator.getInstance("FasterParallelAsynchBatchPriorityTaskExecutor"+fpbteid);
			boolean do_run = true;
			while (do_run) {
				Object data = fpabptetmpc.recvData(_id);  // used to be recvData();
				setIdle(false);
				try {
					if (data instanceof ComparableTaskObject) ( (ComparableTaskObject) data).run();
					else if (data instanceof ComparablePoissonPill) {
						do_run = false; // done
						break;
					}
					else throw new ParallelException("data object cannot be run");
				}
				catch (Exception e) {
					e.printStackTrace();  // task threw an exception, ignore and continue
				}
				setIdle(true);
			}
		}

		
		// IdentifiableIntf method below
		/**
		 * This implementation returns the id this object gets in construction time.
		 * Therefore, if there exist more than one 
		 * <CODE>FasterParallelAsynchBatchPriorityTaskExecutor</CODE> objects in a JVM
		 * there will be more than one <CODE>FPABPTEThread</CODE> objects with the 
		 * same id. However, if the id is always only used with the same executor,
		 * there is no problem as there will never be two threads created by the same
		 * executor, both (threads) having the same id.
		 * @return long _id
		 */
		public long getId() { return _id; }

		synchronized void setIdle(boolean v) { _isIdle = v; }
		synchronized boolean isIdle() { return _isIdle; }
	}


	/**
	 * auxiliary class.
	 * <p>Title: popt4jlib</p>
	 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
	 * <p>Copyright: Copyright (c) 2011</p>
	 * <p>Company: </p>
	 * @author Ioannis T. Christou
	 * @version 1.0
	 */
	class ComparablePoissonPill implements ComparableTaskObject {
		public Serializable run() {
			return null;
		}
		public boolean isDone() {
			return true;
		}
		public void copyFrom(TaskObject t) throws IllegalArgumentException {
			throw new IllegalArgumentException("unsupported");
		}
		public int compareTo(Object other) {
			return 0;
		}
	}
	
}

