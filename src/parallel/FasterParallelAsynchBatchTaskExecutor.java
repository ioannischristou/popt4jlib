package parallel;

import java.util.*;
import popt4jlib.IdentifiableIntf;

/**
 * A faster class that implements thread-pooling to allow its users to execute
 * concurrently a batch of tasks implementing the TaskObject, or more
 * simply, the Runnable interface. The run() method of each task must clearly
 * be thread-safe!, and also, after calling executeBatch(tasks), no thread
 * (including the one in which the call originated) should manipulate in any
 * way the submitted tasks or their container (the Collection argument to the
 * call). Unfortunately, there is no mechanism in the language to enforce this
 * constraint; the user of the library has to enforce this (mild) constraint in
 * their code.
 * The class utilizes the (faster) Message-Passing mechanism implemented in 
 * <CODE>UnboundedSimpleFasterMsgPassingCoordinator</CODE> in same package. 
 * The class itself is thread-safe meaning that there can exist multiple
 * <CODE>FasterParallelAsynchBatchTaskExecutor</CODE> objects, and multiple
 * concurrent threads may call the public methods of the class on the same or
 * different objects as long as the constraints mentioned above are satisfied.
 * <p>Notes:
 * <ul>
 * <li>2023-09-08: added functionality to allow the executor to cancel any 
 * currently executing <CODE>CancelableTaskObject</CODE>s.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 2.0 switched from SimpleFasterMsgPassingCoordinator to 
 * UnboundedSimpleFasterMsgPassingCoordinator
 */
public final class FasterParallelAsynchBatchTaskExecutor {
  private static int _nextId = 0;
  private int _id;  // ParallelBatchTaskExecutor id
  private FPABTEThread[] _threads;
  private boolean _isRunning;
  private final boolean _runOnCurrent;
  private UnboundedSimpleFasterMsgPassingCoordinator _usfmpc;
	private boolean _countingBusyThreadsOpUnderway;  // init to false
	private boolean _popOpUnderway;  // init to false
	private int _executeBatchInProgress=0;  // number of method invocations
	

  /**
   * public factory constructor constructs a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool
	 * @return FasterParallelAsynchBatchTaskExecutor properly initialized
   * @throws ParallelException if numthreads &le; 0 or if too many threads are
   * asked to be created.
   */	
	public static FasterParallelAsynchBatchTaskExecutor 
			newFasterParallelAsynchBatchTaskExecutor(int numthreads) 
		throws ParallelException {
		FasterParallelAsynchBatchTaskExecutor ex = 
			new FasterParallelAsynchBatchTaskExecutor(numthreads);
		ex.initialize();
		return ex;
	}

				
  /**
   * public factory constructor constructs a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool
   * @param runoncurrent boolean if false no task will run on current thread in
   * case the threads in the pool are full
	 * @return FasterParallelAsynchBatchTaskExecutor properly initialized
   * @throws ParallelException if numthreads &le; 0 or if too many threads are
   * asked to be created.
   */					
	public static FasterParallelAsynchBatchTaskExecutor 
			newFasterParallelAsynchBatchTaskExecutor(int numthreads, 
				                                       boolean runoncurrent) 
		throws ParallelException {
		FasterParallelAsynchBatchTaskExecutor ex = 
			new FasterParallelAsynchBatchTaskExecutor(numthreads, runoncurrent);
		ex.initialize();
		return ex;
	}
	
	
  /**
   * private constructor, constructing a thread-pool of numthreads threads. 
	 * Allows tasks to execute on current thread.
   * @param numthreads int the number of threads in the thread-pool
   * @throws ParallelException if numthreads &le; 0 
   */
  private FasterParallelAsynchBatchTaskExecutor(int numthreads) 
		throws ParallelException {
    if (numthreads<=0) throw new ParallelException("ctor arg must be > 0");
    _id = getNextObjId();
    _threads = new FPABTEThread[numthreads];
		_runOnCurrent = true;
  }


  /**
   * private constructor, constructing a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool
   * @param runoncurrent boolean if false no task will run on current thread in
   * case the threads in the pool are full.
   * @throws ParallelException if numthreads &le; 0.
   */
  private FasterParallelAsynchBatchTaskExecutor(int numthreads,
                                               boolean runoncurrent)
      throws ParallelException {
    if (numthreads<=0) throw new ParallelException("ctor arg must be > 0");
    _id = getNextObjId();
    _threads = new FPABTEThread[numthreads];
    _runOnCurrent = runoncurrent;
  }

	
	/**
	 * called exactly once right after this object is constructed.
	 */
	private void initialize() {
		final int numthreads = _threads.length;
    for (int i=0; i<numthreads; i++) {
      _threads[i] = new FPABTEThread(-(i+1));
      _threads[i].setDaemon(true);  // thread will end when main thread ends
      _threads[i].start();
    }
    _isRunning = true;
    _usfmpc = UnboundedSimpleFasterMsgPassingCoordinator.
			         getInstance("FasterParallelAsynchBatchTaskExecutor"+_id); 
	}
	

  /**
   * get the current number of tasks in the queue awaiting processing.
   * @return int
   */
  public int getNumTasksInQueue() {
    return _usfmpc.getNumTasksInQueue();
  }


  /**
   * the main method of the class. Submits all tasks in the argument collection
   * (must be objects implementing the <CODE>TaskObject</CODE> interface in
   * package <CODE>parallel</CODE> or the native <CODE>Runnable</CODE> 
	 * interface) for execution, and the thread-pool will ignore any exceptions 
	 * any task may throw when its run() method is invoked.
   * The call is asynchronous, so that the method does return immediately
   * without waiting for any task to complete; however, if all threads in the
   * thread-pool are busy and the
   * <CODE>FasterParallelAsynchBatchTaskExecutor</CODE> was constructed via the
   * single-argument constructor, or the second argument in the two-argument
   * constructor was true, then the next task executes in the current thread.
   *
   * Notice the possibility for locking when tasks sent to this executor are
   * dependent upon latter tasks to be submitted to the same executor, but the
   * executor becomes full (only possible because of the finite capacity of the
   * executor's thread-pool). The application must ensure this situation does
   * not happen (the executor cannot do anything to prevent this). See also the
   * discussion in <CODE>parallel.DynamicAsynchTaskExecutor</CODE>.
   *
   * A synchronous version is implemented in class ParallelBatchTaskExecutor.
   * @param tasks Collection
   * @throws ParallelException if the shutDown() method has been called prior
   * to this call
   */
  public void executeBatch(Collection tasks) 
		throws ParallelException {
    if (tasks == null) return;
    Iterator it = tasks.iterator();
    synchronized (this) {
			if (_isRunning == false)
				throw new ParallelException("thread-pool is not running");
			while (_popOpUnderway) {  // don't submit yet
				try{
					wait();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			++_executeBatchInProgress;
		}
		try {
			while (it.hasNext()) {
				Object task = it.next();
				if (!_runOnCurrent || existsIdleThread() || 
						task instanceof PoissonPill) {  // ok
					_usfmpc.sendData(_id, task);  
				}
				else {
					if (!_runOnCurrent) {  // submit to thread-pool
						_usfmpc.sendData(_id, task);
					}
					else {  // run on current-thread
						if (task instanceof TaskObject) ( (TaskObject) task).run();
						else if (task instanceof Runnable) ( (Runnable) task).run();
					}
				}
			}
		}
		finally {
			synchronized (this) {
				--_executeBatchInProgress;
				if (_executeBatchInProgress==0)
					notifyAll();
			}
		}
  }
	
	
	/**
	 * get an estimate of the number of busy threads in the executor. The estimate
	 * is not completely accurate unless the FPABTEThread compile-time constant 
	 * <CODE>_DO_CORRECT_COUNTING_BUSY_THREADS</CODE> is set to true, which 
	 * however incurs a penalty overhead on the performance of this executor.
	 * @return int the number of currently busy threads; the number is reversed if
	 * the executor has invoked the <CODE>shutDown()</CODE> method.
	 */
	public synchronized int getNumBusyThreads() {
		//if (!_isRunning) return 0;
		_countingBusyThreadsOpUnderway=true;
		int count = 0;
		for (int i=0; i<_threads.length; i++) {
			if (!_threads[i].isIdle()) ++count;
		}
		_countingBusyThreadsOpUnderway=false;
		return _isRunning ? count : -count;
	}
	
	
	/**
	 * calls the <CODE>cancelCurrentTaskObject()</CODE> on any 
	 * <CODE>CancelableTaskObject</CODE> that might be running on any thread of 
	 * this executor's thread-pool at the time this method is invoked. If a thread
	 * is running anything else (or is idle) nothing happens (on this thread).
	 */
	public synchronized void cancelAnyCurrentCancelableTaskObjects() {
		if (!_isRunning) return;
		for (int i=0; i<_threads.length; i++) {
			_threads[i].cancelCurrentTaskObject();
		}
	}
	
	
	/**
	 * remove and return all tasks in this executor's queue after position pos,
	 * regardless of sender and/or receiver intended addresses.
	 * @param pos int
	 * @return TaskObject[] will be null if there are less than pos tasks in the
	 * queue at the time of the call
	 */
	public synchronized TaskObject[] popAllTasksAfterPos(int pos) {
		if (!_isRunning) return null;
		try {
			while (_executeBatchInProgress>0) {
				try {
					wait();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			_popOpUnderway = true;
			final int num_tasks = _usfmpc.getNumTasksInQueue();
			if (num_tasks<pos) return null;
			List tasks = new ArrayList();
			while (_usfmpc.getNumTasksInQueue()>=pos) {
				TaskObject obj = (TaskObject) _usfmpc.recvLastDataSent();
				tasks.add(obj);
			}
			TaskObject[] result = new TaskObject[tasks.size()];
			int j=tasks.size()-1;
			for (int i=0; i<tasks.size(); i++) {
				result[j--] = (TaskObject) tasks.get(i);
			}
			return result;
		}
		finally {
			_popOpUnderway = false;
			notifyAll();
		}
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
   */
  public synchronized void shutDown() throws ParallelException {
    if (_isRunning==false)
      throw new ParallelException("shutDown() has been called already");
		_isRunning=false;  // stop other threads from submitting into this pool
		while (_executeBatchInProgress>0) {  // wait until current submissions done
			try {
				wait();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		// now, go submit your poisonous pills, you murderer...
    final int numthreads = _threads.length;
    Vector pills = new Vector();
    for (int i=0; i<numthreads; i++) {
      PoissonPill p = new PoissonPill();
      pills.addElement(p);
    }
		_isRunning=true;  // needed for the executeBatch(pills) to work
    executeBatch(pills);
    _isRunning = false;
    return;
  }
	
	
	/**
	 * invokes the <CODE>shutDown()</CODE> method and waits until all threads have
	 * finished their execution.
	 */
	public void shutDownAndWait4Threads2Finish() {
		utils.Messenger mger = utils.Messenger.getInstance();
		try {
			shutDown();
		}
		catch (ParallelException e) {
			mger.msg("FPABTExecutor.shutDownAndWait4Threads2Finish(): "+
				       "shutDown() already invoked", 1);
		}
		mger.msg("FPABTExecutor: waiting for the pool threads to join", 1);
		for (int i=0; i<_threads.length; i++) {
			try {
				_threads[i].join();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		mger.msg("FPABTExecutor.shutDownAndWait4Threads2Finish(): Done.",1);
	}


  /**
   * return the number of threads in the thread-pool.
   * @return int
   */
  public synchronized int getNumThreads() {
    return _threads.length;
  }
	
	
  int getObjId() { return _id; }

	
	synchronized boolean countingBusyThreadsOpUnderway() {
		return _countingBusyThreadsOpUnderway; 
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
	 * inner helper class for FasterParallelAsynchBatchTaskExecutor. Not part of 
	 * the public API.
	 * <p>Title: popt4jlib</p>
	 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
	 * <p>Copyright: Copyright (c) 2011</p>
	 * <p>Company: </p>
	 * @author Ioannis T. Christou
	 * @version 1.0
	 */
	class FPABTEThread extends Thread implements IdentifiableIntf {
		private static final boolean _DO_CORRECT_COUNTING_BUSY_THREADS = false;
		private int _id;
		private boolean _isIdle=true;
		
		private Object _currentObj=null;  // currently executing object
		

		/**
		 * public constructor. The id argument is a negative integer.
		 * @param id int
		 */
		public FPABTEThread(int id) {
			_id = id;
		}


		/**
		 * implements the <CODE>getId()</CODE> method of the IdentifiableIntf.
		 * @return long  // int in reality
		 */
		public long getId() {
			return _id;
		}


		/**
		 * the run() method of the thread, loops continuously, waiting for a task
		 * to arrive via the <CODE>UnboundedSimpleFasterMsgPassingCoordinator</CODE> 
		 * class and runs it. Any exceptions the task throws are caught &amp; 
		 * ignored. In case the data that arrives is a PoissonPill, the thread exits 
		 * its run() loop.
		 */
		public void run() {
			final int fpbteid = getObjId();
			final UnboundedSimpleFasterMsgPassingCoordinator sfmpc =
					UnboundedSimpleFasterMsgPassingCoordinator.
						getInstance("FasterParallelAsynchBatchTaskExecutor"+fpbteid);
			boolean do_run = true;
			while (do_run) {
				Object data = sfmpc.recvData(_id);
				setIdle(false);
				try {
					if (data instanceof CancelableTaskObject) {
						setCurrentObj((CancelableTaskObject) data);
						( (TaskObject) data).run();
					}
					else if (data instanceof Runnable) ( (Runnable) data).run();
					else if (data instanceof PoissonPill) {
						do_run = false; // done
						setIdle(true);  // declare thread will not be running any more
						break;
					}
					else throw new ParallelException("data object cannot be run");
					setCurrentObj(null);
				}
				catch (Exception e) {
					setCurrentObj(null);
					e.printStackTrace();  // task threw an exception, ignore and continue
				}
				if (_DO_CORRECT_COUNTING_BUSY_THREADS) {
					while (countingBusyThreadsOpUnderway()) {
						// loop never executes: countingBusyThreadsOpUnderway() is 
						// synchronized so the thread will wait to get the lock, which 
						// the getNumBusyThreads() method will release only after it's
						// checked all threads, and reset its _countingBusyThreadsOpUnderway
						// field.
						// no-op
						// sanity check
						throw new Error("_countingBusyThreadsOpUnderway==true???");   
					}
				}
				setIdle(true);
			}
		}
		
		
		/**
		 * sets the <CODE>_currentObj</CODE> data member to the 
		 * <CODE>TaskObject</CODE> that is about to start executing, so that in 
		 * case it is a <CODE>CancelableTaskObject</CODE>, it can be canceled by 
		 * a call to <CODE>cancelCurrentTaskObject</CODE>.
		 * @param data TaskObject
		 */
		synchronized void setCurrentObj(TaskObject data) {
			_currentObj = data;
		}
		
		
		/**
		 * the method invokes the <CODE>cancel()</CODE> method on the 
		 * <CODE>_currentObj</CODE> iff it is a cancelable task object. The cancel
		 * method hopefully leads to the early finish of the execution of the run
		 * method of the currently running object.
		 */
		synchronized void cancelCurrentTaskObject() {
			if (_currentObj!=null && _currentObj instanceof CancelableTaskObject) {
				((CancelableTaskObject) _currentObj).cancel();
			}
		}

		synchronized void setIdle(boolean v) { _isIdle = v; }
		synchronized boolean isIdle() { return _isIdle; }
	}

	
	/**
	 * auxiliary inner-class, not part of the public API.
	 */
	class PoissonPill {
		// denotes end of computations for receiving thread
	}

}
