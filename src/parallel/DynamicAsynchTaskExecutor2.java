package parallel;

import java.util.*;
import popt4jlib.IdentifiableIntf;

/**
 * an enhancement over <CODE>DynamicAsynchTaskExecutor</CODE> in that it
 * augments its threads in its thread-pool with two local queues so that they
 * first look for tasks in their "hot" local queue, then if no task is in the
 * global queue either available for execution, they finally go into the "cold"
 * queue. These two queues are meant to reduce contention in the global queue,
 * and also to help the implementation of fork-join parallel programming.
 * Tasks (that need to implement the <CODE>ThreadSpecificTaskObject</CODE> 
 * interface) may go to the local queue by invoking the special methods
 * <CODE>[re]submitToSameThread(ThreadSpecificTaskObject o)</CODE>. In any case,
 * such tasks always execute in the specified thread.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class DynamicAsynchTaskExecutor2 {
  private static int _nextId = 0;
  private int _id;  // DynamicAsynchTaskExecutor2 id
  private List _threads;  // used to be Vector
  private int _initNumThreads;
  private int _maxNumThreads;
  private boolean _isRunning;
  private boolean _runOnCurrent=true;
  private long _numTasksHandled=0;
  private long _numTasksSubmitted=0;
  private SimpleFasterMsgPassingCoordinator _sfmpc;


	/**
	 * replaces public constructor.
	 * @param numthreads int
	 * @param maxthreads int
	 * @return DynamicAsynchTaskExecutor2 properly initialized
	 * @throws ParallelException
	 */
	public static DynamicAsynchTaskExecutor2 newDynamicAsynchTaskExecutor2(int numthreads, int maxthreads) throws ParallelException {
		DynamicAsynchTaskExecutor2 ex = new DynamicAsynchTaskExecutor2(numthreads, maxthreads);
		ex.initialize();
		return ex;
	}


	/**
	 * replaces public constructor.
	 * @param numthreads int
	 * @param maxthreads int
	 * @param runoncurrent boolean
	 * @return DynamicAsynchTaskExecutor2 properly initialized
	 * @throws ParallelException
	 */
	public static DynamicAsynchTaskExecutor2 newDynamicAsynchTaskExecutor2(int numthreads, int maxthreads,
					                                                             boolean runoncurrent) throws ParallelException {
		DynamicAsynchTaskExecutor2 ex = new DynamicAsynchTaskExecutor2(numthreads, maxthreads, runoncurrent);
		ex.initialize();
		return ex;
	}


  /**
   * private constructor, constructing a thread-pool of numthreads threads.
   * @param numthreads int the number of initial threads in the thread-pool
   * @param maxthreads int the max. number of threads in the thread-pool
   * @throws ParallelException if numthreads &le; 0 or if too many threads are
   * asked to be created or if maxthreads &lt; numthreads.
   */
  private DynamicAsynchTaskExecutor2(int numthreads, int maxthreads) throws ParallelException {
    if (numthreads<=0) throw new ParallelException("constructor arg must be > 0");
    if (maxthreads > SimpleFasterMsgPassingCoordinator.getMaxSize() ||
        numthreads > maxthreads)
      throw new ParallelException("cannot construct so many threads");
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
   * @throws ParallelException if numthreads &le; 0 or if numthreads&gt;maxthreads.
   */
  private DynamicAsynchTaskExecutor2(int numthreads, int maxthreads,
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
      Thread ti = new DATEThread2(this, -(i+1));
      _threads.add(ti);
      ti.setDaemon(true);  // thread will end when main thread ends
      ti.start();
    }
    _isRunning = true;
    _sfmpc = SimpleFasterMsgPassingCoordinator.getInstance("DynamicAsynchTaskExecutor2." + _id);
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
   * <CODE>DynamicAsynchTaskExecutor2</CODE> was constructed via the
   * two-argument constructor, or the third argument in the three-argument
   * constructor was true, then the task executes in the current thread.
   * The call will also block in case the queue of tasks in the thread-pool
   * (implemented in <CODE>SimpleFasterMsgPassingCoordinator</CODE> class of
   * this package) is full (default=10000) in which case the current thread will
   * wait until threads in the thread-pool finish up their tasks so the queue
   * becomes less than full and can accept more tasks.
   * @param task Runnable
   * @throws ParallelException if
	 * (a) the shutDown() method has been called prior to this call OR
	 * (b) there are no available threads to run the task, AND
	 * it's not possible to create a new thread (all threads up to capacity are
	 * already busy) AND
	 * this <CODE>DynamicAsynchTaskExecutor2</CODE> object does not allow
   * running a task directly in the current thread, AND
	 * the underlying global queue of tasks is full.
   */
  public void execute(Runnable task) throws ParallelException {
    if (task == null)return;
    if (isRunning() == false)
      throw new ParallelException("thread-pool is not running");
    boolean run_on_current = false;
    synchronized (this) {
      ++_numTasksSubmitted;
      //utils.Messenger.getInstance().msg("Current total #threads="+getNumThreads(),1);
      if (isOK2SubmitTask() || task instanceof DATEPoissonPill2) {
        _sfmpc.sendDataBlocking(_id, task);
      }
      else {
        if (getNumThreads() < _maxNumThreads) { // create new thread
          Thread ti = new DATEThread2(this, - (getNumThreads() + 1));
          _threads.add(ti);
          ti.setDaemon(true); // thread will end when main thread ends
          ti.start();
					utils.Messenger.getInstance().msg("Current total #threads="+getNumThreads(),1);
          _sfmpc.sendDataBlocking(_id, task);
        }
        else {  // no capacity remaining
          ++_numTasksHandled;  // for both outcomes of the if-then-else stmt.
          if (_runOnCurrent) run_on_current = true;
          else {
						try {
							_sfmpc.sendData(_id, task);
						}
						catch (ParallelException e) {  // ok, even queue is full, so throw
							// e.printStackTrace();
							throw new ParallelException("DynamicAsynchTaskExecutor2.execute(): not enough capacity to submit task now");
						}
          }
        }
      }
    }  // end synchronized block
    if (run_on_current) {
      /*
			utils.Messenger.getInstance().msg(
          "DynamicAsynchTaskExecutor2.execute(task): running task on current thread",
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
	 * <CODE>DATEThread2</CODE> object.
	 * @throws IllegalStateException if the current thread's local queue is full
	 */
	public void submitToSameThread(ThreadSpecificTaskObject tsto)
					throws ParallelException, ClassCastException, IllegalStateException {
		DATEThread2 cur_thread = (DATEThread2) Thread.currentThread();
		if (cur_thread._id!=tsto.getThreadIdToRunOn())
			throw new ParallelException("method invocation not valid from current thread");
		cur_thread._hotLocalQueue.addElement(tsto);
	}


	/**
	 * forces the argument to be pushed to the current thread's cold local queue
	 * of tasks, a fast unsynchronized operation.
	 * @param tsto Runnable
	 * @throws ClassCastException if current thread is not a
	 * <CODE>DATEThread2</CODE> object.
	 */
	public void resubmitToSameThread(Runnable tsto)
					throws ClassCastException {
		DATEThread2 cur_thread = (DATEThread2) Thread.currentThread();
		cur_thread._coldLocalQueue.addElement(tsto);  // used to be offer(tsto);
	}


	/**
	 * returns true iff the hot local queue is not full.
	 * @return boolean
	 * @throws ClassCastException if current thread is not a
	 * <CODE>DATEThread2</CODE> object.
	 */
	public boolean hotLocalQueueHasCapacity() throws ClassCastException {
		DATEThread2 cur_thread = (DATEThread2) Thread.currentThread();
		return cur_thread._hotLocalQueue.size()<cur_thread._hotLocalQueue.getMaxSize();
	}


	/**
	 * returns the size of the hot local queue.
	 * @return int
	 * @throws ClassCastException if current thread is not a
	 * <CODE>DATEThread2</CODE> object.
	 */
	public int hotLocalQueueSize() throws ClassCastException {
		DATEThread2 cur_thread = (DATEThread2) Thread.currentThread();
		return cur_thread._hotLocalQueue.size();
	}


	/**
	 * returns the size of the cold local queue.
	 * @return int
	 * @throws ClassCastException if current thread is not a
	 * <CODE>DATEThread2</CODE> object.
	 */
	public int coldLocalQueueSize() throws ClassCastException {
		DATEThread2 cur_thread = (DATEThread2) Thread.currentThread();
		return cur_thread._coldLocalQueue.size();
	}


  /**
   * shut-down all the threads in this executor's thread-pool. It is the
   * caller's responsibility to ensure that after this method has been called
   * no other thread (including the threads in the thread-pool) won't call the
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
      DATEPoissonPill2 p = new DATEPoissonPill2();
      execute(p);
    }
    _isRunning = false;
    return;
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
   * <CODE>SimpleFasterMsgPassingCoordinator</CODE> object, and the total tasks
   * run by the threads in the thread-pool, and return ok if this number is less
   * than the total current number of threads.
   * @return boolean
   */
  private synchronized boolean isOK2SubmitTask() {
    return _numTasksSubmitted - _numTasksHandled < _threads.size();
  }

  private synchronized boolean isRunning() { return _isRunning; }


  private synchronized static int getNextObjId() { return ++_nextId; }


  /**
   * nested helper class for DynamicAsynchTaskExecutor2.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class DATEThread2 extends Thread implements IdentifiableIntf {
    private DynamicAsynchTaskExecutor2 _e;
    private int _id;
    private boolean _isIdle=true;
    private SimpleFasterMsgPassingCoordinator _datetmpc;
		private BoundedBufferArrayUnsynchronized _hotLocalQueue;
		private UnboundedBufferArrayUnsynchronized _coldLocalQueue;  // serves as ArrayDeque<Runnable>


    /**
     * public constructor. The id argument is a negative integer.
     * @param e DynamicAsynchTaskExecutor2
     * @param id int
     */
    public DATEThread2(DynamicAsynchTaskExecutor2 e, int id) {
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
     * in the hot local queue; if there is, executes it;
		 * else looks for a task in the global shared queue; if it cannot find one,
		 * it checks its old local queue, and if there is none, then
		 * goes waiting for a task from the global queue.
     * Any exceptions the task throws are caught &amp; ignored. If the data that
     * arrives is a PoissonPill2, the thread exits its run() loop, without any
		 * regard to the tasks in its local queue.
     */
    public void run() {
      final int fpbteid = _e.getObjId();
      _datetmpc = SimpleFasterMsgPassingCoordinator.getInstance("DynamicAsynchTaskExecutor2."+fpbteid);
			_hotLocalQueue = new BoundedBufferArrayUnsynchronized(1000*SimpleFasterMsgPassingCoordinator.getMaxSize());
			// _coldLocalQueue = new ArrayDeque();  // claimed faster than ArrayList
      _coldLocalQueue = new UnboundedBufferArrayUnsynchronized(SimpleFasterMsgPassingCoordinator.getMaxSize());
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
					}
					catch (IllegalStateException e) {
						e.printStackTrace();  // cannot happen
					}
				} else {  // 2. check the global shared queue
					data = _datetmpc.recvDataIfAnyExist(_id);  // try global queue
					if (data == null) {  // 3. go to the cold queue
						if (_coldLocalQueue.size()>0) {
							recvd_from_local = true;
							data = _coldLocalQueue.remove();  // used to be poll();  // FIFO order processing
						}
					}
				}
				// 4. OK, now block waiting on global queue
				if (data==null)
					data = _datetmpc.recvData(_id);

        setIdle(false);
        try {
          if (data instanceof DATEPoissonPill2) {
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
    }

    synchronized void setIdle(boolean v) { _isIdle = v; }
    synchronized boolean isIdle() { return _isIdle; }
  }


  /**
   * nested helper class indicates shut-down of thread-pool
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class DATEPoissonPill2 implements Runnable {
    public void run() {
      // no-op
    }
  }

}

