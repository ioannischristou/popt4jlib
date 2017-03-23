package parallel;

import java.util.*;
import popt4jlib.IdentifiableIntf;

/**
 * Exactly the same as <CODE>DynamicAsynchTaskExecutor2</CODE> class, only that
 * the thread-pool's threads' hot local queue is also unbounded (as is the cold
 * local queue), as is the global shared queue, so that all associated queues
 * are unbounded, and therefore do not act as "synchronizers" between the 
 * executor threads (see Brian Goetz et al "Java Concurrency in Practice").
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class DynamicAsynchTaskExecutor3 {
  private static int _nextId = 0;
  private int _id;  // DynamicAsynchTaskExecutor3 id
  private List _threads;  // used to be Vector
	private int _initNumThreads;
  private int _maxNumThreads;
  private boolean _isRunning;
  private boolean _runOnCurrent=true;
  private long _numTasksHandled=0;
  private long _numTasksSubmitted=0;
  private UnboundedSimpleFasterMsgPassingCoordinator _sfmpc;

	
	/**
	 * replaces public constructor.
	 * @param numthreads int
	 * @param maxthreads int
	 * @return DynamicAsynchTaskExecutor3 properly initialized
	 * @throws ParallelException 
	 */
	public static DynamicAsynchTaskExecutor3 newDynamicAsynchTaskExecutor3(int numthreads, int maxthreads) throws ParallelException {
		DynamicAsynchTaskExecutor3 ex = new DynamicAsynchTaskExecutor3(numthreads, maxthreads);
		ex.initialize();
		return ex;
	}
	
	
	/**
	 * replaces public constructor.
	 * @param numthreads int
	 * @param maxthreads int
	 * @param runoncurrent boolean
	 * @return DynamicAsynchTaskExecutor3 properly initialized
	 * @throws ParallelException 
	 */
	public static DynamicAsynchTaskExecutor3 newDynamicAsynchTaskExecutor3(int numthreads, int maxthreads, 
					                                                             boolean runoncurrent) throws ParallelException {
		DynamicAsynchTaskExecutor3 ex = new DynamicAsynchTaskExecutor3(numthreads, maxthreads, runoncurrent);
		ex.initialize();
		return ex;
	}	
	

  /**
   * private constructor, constructing a thread-pool of numthreads threads.
   * @param numthreads int the number of initial threads in the thread-pool
   * @param maxthreads int the max. number of threads in the thread-pool
   * @throws ParallelException if numthreads &le; 0 or 
	 * if maxthreads &lt; numthreads.
   */
  private DynamicAsynchTaskExecutor3(int numthreads, int maxthreads) throws ParallelException {
    if (numthreads<=0) throw new ParallelException("constructor arg must be > 0");
    if (numthreads > maxthreads)
      throw new ParallelException("maxthreads must be >= numthreads");
    _id = getNextObjId();
    _threads = new ArrayList(numthreads);  // numthreads arg. denotes capacity
		_initNumThreads = numthreads;
    _maxNumThreads = maxthreads;
  }


  /**
   * private constructor, constructing a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool.
   * @param maxthreads int the max number of threads in the thread-pool.
   * @param runoncurrent boolean if false no task will run on current thread in
   * case the threads in the pool are full and no new thread can be created.
   * @throws ParallelException if numthreads &le; 0 or if numthreads &gt; maxthreads.
   */
  private DynamicAsynchTaskExecutor3(int numthreads, int maxthreads,
                                   boolean runoncurrent)
      throws ParallelException {
    this(numthreads, maxthreads);
    _runOnCurrent = runoncurrent;
  }

	
	/**
	 * called exactly once, right after an object has been constructed.
	 */
	private void initialize() {
		final int numthreads = _initNumThreads;
    for (int i=0; i<numthreads; i++) {
      Thread ti = new DATEThread3(this, -(i+1));
      _threads.add(ti);
      ti.setDaemon(true);  // thread will end when main thread ends
      ti.start();
    }
    _isRunning = true;
    _sfmpc = UnboundedSimpleFasterMsgPassingCoordinator.getInstance("DynamicAsynchTaskExecutor3." + _id);		
	}
	

  /**
   * get the current number of tasks in the queue awaiting processing.
   * @return int
   */
  public int getNumTasksInQueue() {
    return _sfmpc.getNumTasksInQueue();
  }


  /**
   * the main method of the class. Submits a <CODE>Runnable</CODE> interface
   * for execution, and the thread-pool will ignore any exceptions any task may
   * throw when its run() method is invoked.
   * The call is asynchronous, so that the method does return immediately
   * without waiting for any task to complete; however, if all threads in the
   * thread-pool are busy and there is no room for a new thread and the
   * <CODE>DynamicAsynchTaskExecutor3</CODE> was constructed via the
   * two-argument constructor, or the third argument in the three-argument
   * constructor was true, then the task executes in the current thread.
   * @param task Runnable
   * @throws ParallelException if the shutDown() method has been called prior to 
	 * this call 
   */
  public void execute(Runnable task) throws ParallelException {
    if (task == null)return;
    boolean run_on_current = false;
    synchronized (this) {
			if (!_isRunning) throw new ParallelException("thread-pool not running");
      ++_numTasksSubmitted;
      //utils.Messenger.getInstance().msg("Current total #threads="+getNumThreads(),1);
      if (isOK2SubmitTask() || task instanceof DATEPoissonPill3) {
        _sfmpc.sendData(_id, task);
      }
      else {
        if (_threads.size() < _maxNumThreads) { // create new thread
          Thread ti = new DATEThread3(this, - (_threads.size() + 1));
          _threads.add(ti);
          ti.setDaemon(true); // thread will end when main thread ends
          ti.start();
					utils.Messenger.getInstance().msg("Current total #threads="+_threads.size(),1);
          _sfmpc.sendData(_id, task);
        }
        else {  // no capacity remaining
          ++_numTasksHandled;  // for both outcomes of the if-then-else stmt.
          if (_runOnCurrent && isOK2RunOnCurrentThread(task)) run_on_current = true;
          else {
						_sfmpc.sendData(_id, task);
          }
        }
      }
    }  // end synchronized block
    if (run_on_current) {
      /*
			utils.Messenger.getInstance().msg(
          "DynamicAsynchTaskExecutor3.execute(task): running task on current thread",
          0);
			*/
      task.run(); // run on current thread
    }
  }

	
	/**
	 * forces the argument to be pushed to the current thread's "hot" local queue 
	 * of tasks, a fast unsynchronized operation.
	 * @param tsto
	 * @throws ParallelException if argument does not designate it should run from
	 * the current thread's id.
	 * @throws ClassCastException if current thread is not a 
	 * <CODE>DATEThread3</CODE> object.
	 */
	public void submitToSameThread(ThreadSpecificTaskObject tsto) 
					throws ParallelException, ClassCastException {
		DATEThread3 cur_thread = (DATEThread3) Thread.currentThread();
		if (cur_thread._id!=tsto.getThreadIdToRunOn())
			throw new ParallelException("method invocation not valid from current thread");
		cur_thread._hotLocalQueue.addElement(tsto);
	}

	
	/**
	 * forces the argument to be pushed to the current thread's cold local queue 
	 * of tasks, a fast unsynchronized operation.
	 * @param tsto Runnable
	 * @throws ClassCastException if current thread is not a 
	 * <CODE>DATEThread3</CODE> object.
	 */
	public void resubmitToSameThread(Runnable tsto) 
					throws ClassCastException {
		DATEThread3 cur_thread = (DATEThread3) Thread.currentThread();
		cur_thread._coldLocalQueue.addElement(tsto);
	}
	
	
	/**
	 * returns true iff the hot local queue is not full according to its current
	 * array size (so that it does not need resizing).
	 * @return boolean 
	 * @throws ClassCastException if current thread is not a 
	 * <CODE>DATEThread3</CODE> object.
	 */
	public boolean hotLocalQueueCurrentlyHasCapacity() throws ClassCastException {
		DATEThread3 cur_thread = (DATEThread3) Thread.currentThread();
		return cur_thread._hotLocalQueue.size()<cur_thread._hotLocalQueue.getCurrentMaxSize();
	}
	
	
	/**
	 * returns the size of the hot local queue.
	 * @return int 
	 * @throws ClassCastException if current thread is not a 
	 * <CODE>DATEThread3</CODE> object.
	 */
	public int hotLocalQueueSize() throws ClassCastException {
		DATEThread3 cur_thread = (DATEThread3) Thread.currentThread();
		return cur_thread._hotLocalQueue.size();
	}
	
	
	/**
	 * returns the size of the cold local queue.
	 * @return int 
	 * @throws ClassCastException if current thread is not a 
	 * <CODE>DATEThread3</CODE> object.
	 */
	public int coldLocalQueueSize() throws ClassCastException {
		DATEThread3 cur_thread = (DATEThread3) Thread.currentThread();
		return cur_thread._coldLocalQueue.size();
	}

	
	/**
	 * returns the integer id of the <CODE>DATEThread3</CODE> thread that is the
	 * current thread.
	 * @return int
	 * @throws ClassCastException if the current thread is not a DATEThread3 
	 * thread
	 */
	public int getCurrentThreadId() throws ClassCastException {
		DATEThread3 cur_thread = (DATEThread3) Thread.currentThread();
		return cur_thread._id;
	}
	
	
  /**
   * shut-down all the threads in this executor's thread-pool. It is the
   * caller's responsibility to ensure that after this method has been called
   * no other thread (including the threads in the thread-pool) will call the
   * execute() method. Use of condition-counters or similar may be
   * necessary when tasks given to the thread-pool for execution may call the
   * <CODE>execute()</CODE> method recursively.
   * The executor cannot be used afterwards, and will throw ParallelException
   * if the method execute() or shutDown() is called again.
   * @throws ParallelException
   */
  public synchronized void shutDown() throws ParallelException {
    if (_isRunning==false)
      throw new ParallelException("shutDown() has been called already");
    final int numthreads = _threads.size();
    for (int i=0; i<numthreads; i++) {
      DATEPoissonPill3 p = new DATEPoissonPill3();
      execute(p);
    }
    _isRunning = false;
    return;
  }
	
	
	/**
	 * calls <CODE>shutDown()</CODE> and waits for each thread in the pool to 
	 * finish execution. Obviously, must be called only once.
	 * @throws ParallelException
	 * @throws InterruptedException 
	 */
	public void shutDownAndWait4Threads() throws ParallelException, InterruptedException {
		// the method is unsyncrhonized as it should be: the shutDown() method is 
		// properly synchronized and sets _isRunning to false. Afterwards, any other
		// thread trying to call execute(task) will see _isRunning is false and 
		// throw ParallelException. The joining of the pool-threads cannot be called
		// while holding this executor's monitor lock, as it is possible that some
		// pool-thread may be still in the process of having received another task
		// from the global queue and will need to call the synchronized executor
		// method incrNumTasksHandled() and stall there for ever.
		shutDown();
    final int numthreads = _threads.size();
    for (int i=0; i<numthreads; i++) {
			DATEThread3 ti = (DATEThread3) _threads.get(i);
			ti.join();
    }		
	} 


  /**
   * return the number of threads in the thread-pool.
   * @return int
   */
  public synchronized int getNumThreads() {
    if (_threads!=null)
      return _threads.size();
    else return 0;
  }


  /**
   * invoked only by the threads in the thread-pool to indicate finishing
   * (normal or abnormal) of a task execution.
   */
  private synchronized void incrNumTasksHandled() {
    ++_numTasksHandled;
  }


  private int getObjId() { return _id; }


  /**
   * compute the difference between total tasks submitted to the associated
   * <CODE>UnboundedSimpleFasterMsgPassingCoordinator</CODE> object, and the 
	 * total tasks run by the threads in the thread-pool, and return ok if this 
	 * number is less than the total current number of threads.
	 * Only called from within the synchronized block of the 
	 * <CODE>execute(task)</CODE> method.
   * @return boolean
   */
  private boolean isOK2SubmitTask() {
    return _numTasksSubmitted - _numTasksHandled < _threads.size();
  }

	
	private boolean isOK2RunOnCurrentThread(Runnable task) {
		if (task instanceof ThreadSpecificTaskObject) {
			int tid = ((ThreadSpecificTaskObject) task).getThreadIdToRunOn();
			if (Thread.currentThread() instanceof IdentifiableIntf) {
				int cur_thread_id = (int) ((IdentifiableIntf) Thread.currentThread()).getId();
				return tid==cur_thread_id;
			} else return false;
		}
		return true;  // task doesn't care about which thread it runs on
	}
	

  private synchronized static int getNextObjId() { return ++_nextId; }


  /**
   * nested helper class for DynamicAsynchTaskExecutor3.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2015</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class DATEThread3 extends Thread implements IdentifiableIntf {
    private static final boolean _SEND_REQUEST_4_WORK_WHEN_IDLE=true;  // compile-time constant
		private DynamicAsynchTaskExecutor3 _e;
    private int _id;
    private boolean _isIdle=true;
    private UnboundedSimpleFasterMsgPassingCoordinator _datetmpc;
		private UnboundedBufferArrayUnsynchronized _hotLocalQueue;
		private UnboundedBufferArrayUnsynchronized _coldLocalQueue;  // used to be ArrayDeque<Runnable>

		
    /**
     * public constructor. The id argument is a negative integer.
     * @param e DynamicAsynchTaskExecutor3
     * @param id int
     */
    public DATEThread3(DynamicAsynchTaskExecutor3 e, int id) {
      _e = e;
      _id = id;
    }

		
		/**
		 * implements the <CODE>IdentifiableIntf</CODE> method.
		 * @return _id long the (int) id given to it by the main class object.
		 */
		public long getId() {
			return _id;
		}
		

    /**
     * the run() method of the thread, loops continuously, looking for a task
     * in the hot local queue (unsynchronized op); if there is at least one, 
		 * picks up the first one and executes it; else looks for a task in the 
		 * global shared queue (synchronized op/non-blocking); if it cannot find one, 
		 * it checks its cold local queue (unsynchronized op), and if there is none, 
		 * then goes waiting for a task from the global queue 
		 * (synchronized op/blocking).
     * Any exceptions the task throws are caught &amp; ignored. If the data that
     * arrives is a PoissonPill3, the thread exits its run() loop, without any
		 * regard to the tasks in its local queue.
     */
    public void run() {
			// thread-local statistics gathering variables
			long num_hotlocal_tasks_encountered = 0;
			long num_coldlocal_tasks_encountered = 0;
			long num_global_tasks_encountered = 0;
			long num_workrequests_issued = 0;
			// end statistics block
      final int fpbteid = _e.getObjId();
      _datetmpc = UnboundedSimpleFasterMsgPassingCoordinator.getInstance("DynamicAsynchTaskExecutor3."+fpbteid);
			_hotLocalQueue = new UnboundedBufferArrayUnsynchronized(1000);  // arg. is init. size
			_coldLocalQueue = new UnboundedBufferArrayUnsynchronized(2000);  // faster than ArrayList or ArrayDeque 
      boolean do_run = true;
			boolean recvd_from_local;
      while (do_run) {
				recvd_from_local=false;
				// 1. check the hot local queue
				Object data = null;
				if (_hotLocalQueue.size()>0) {
					recvd_from_local = true;
					try {
						data = _hotLocalQueue.remove();  // FIFO order processing
						++num_hotlocal_tasks_encountered;
					}
					catch (IllegalStateException e) {
						e.printStackTrace();  // cannot happen
					}
				} else {  // 2. check the global shared queue
					data = _datetmpc.recvDataIfAnyExist(_id);  // try global queue
					if (data == null) {  // 3. go to the cold queue
						if (_coldLocalQueue.size()>0) {
							recvd_from_local = true;
							data = _coldLocalQueue.remove();  // FIFO order processing
							++num_coldlocal_tasks_encountered;
						} else ++num_global_tasks_encountered;
					}
				}
				// 4. OK, conditionally let others know I'm "idle" and then 
				// block waiting on global queue
				if (data==null) { 
					if (_SEND_REQUEST_4_WORK_WHEN_IDLE) {
						IdleThreadWorkRequest req = new IdleThreadWorkRequest(_id);
						_datetmpc.sendData(_id, req);
						++num_workrequests_issued;
					}
					data = _datetmpc.recvData(_id);
					++num_global_tasks_encountered;
				}
		
        setIdle(false);
        try {
          if (data instanceof DATEPoissonPill3) {
            do_run = false; // done
            break;
          }
          else if (data instanceof Runnable) ( (Runnable) data).run();
          else throw new ParallelException("data object cannot be run");
        }
        catch (Exception e) {
          e.printStackTrace();  // task threw an exception, ignore and continue
        }
        if (!recvd_from_local) incrNumTasksHandled();  // indicate task was "handled".
        setIdle(true);
      }
			{  // print statistics block
				String stats = "DATEThread3-id="+_id+" STATS:\n";
				stats += " num_hotlocal_tasks_encountered="+num_hotlocal_tasks_encountered;
				stats += " num_coldlocal_tasks_encountered="+num_coldlocal_tasks_encountered;
				stats += " num_global_tasks_encountered="+num_global_tasks_encountered;
				long total = num_hotlocal_tasks_encountered +
								     num_coldlocal_tasks_encountered + 
										 num_global_tasks_encountered;
				stats += " (total="+total+")";
				stats += " num_workrequests_issued="+num_workrequests_issued;
				utils.Messenger.getInstance().msg(stats, 1);
			}
    }

		
    synchronized void setIdle(boolean v) { _isIdle = v; }
    synchronized boolean isIdle() { return _isIdle; }
  }


  /**
   * nested helper class indicates shut-down of thread-pool. Not part of the 
	 * public API.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2015</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class DATEPoissonPill3 implements Runnable {
    public void run() {
      // no-op
    }
  }
	
	
  /**
   * nested helper class indicates some thread found no work to do and went 
	 * blocking waiting for some data on the global pool. Not part of the public
	 * API.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2015</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
	class IdleThreadWorkRequest implements ThreadSpecificTaskObject {
		private int _threadid;
		
		/**
		 * sole constructor.
		 * @param tid int
		 */
		public IdleThreadWorkRequest(int tid) {
			_threadid = -tid;
		}
		
		
		/**
		 * returns the argument passed in the constructor negated.
		 * @return int 
		 */
		public int getThreadIdToRunOn() {
			return _threadid;
		}
		
		
		/**
		 * picks a task from the current thread's cold queue and sends it to the
		 * global queue as long as current thread does have work to do
		 */
		public void run() {
			DATEThread3 cur_thread = (DATEThread3) Thread.currentThread();
			UnboundedBufferArrayUnsynchronized coldlocal = cur_thread._coldLocalQueue;
			UnboundedBufferArrayUnsynchronized hotlocal = cur_thread._hotLocalQueue;
			if (coldlocal.size()>1 || (coldlocal.size()>0 && hotlocal.size()>0)) {
				Runnable r = (Runnable) coldlocal.remove();
				cur_thread._datetmpc.sendData(cur_thread._id, r);
			} else {  // send back to global queue original request? currently no-op
				// cur_thread._datetmpc.sendData(cur_thread._id, this);
			}
		}
	}

}

