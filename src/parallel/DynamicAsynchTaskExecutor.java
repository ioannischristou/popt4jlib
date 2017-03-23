package parallel;

import java.util.*;

/**
 * A faster class that implements thread-pooling to allow its users to execute
 * concurrently tasks implementing the Runnable interface. The run() method of
 * each task must clearly be thread-safe!, and also, after calling execute(task)
 * no thread (including the one in which the call originated) should manipulate
 * in any way the submitted task. Unfortunately, there is no mechanism in the
 * language to enforce this constraint; the user of the library has to enforce
 * this (mild) constraint in their code.
 * The class utilizes the (faster) Message-Passing mechanism implemented in the
 * <CODE>SimpleFasterMsgPassingCoordinator</CODE> class of this package.
 * The class itself is thread-safe meaning that there can exist multiple
 * <CODE>DynamicAsynchTaskExecutor</CODE> objects, and multiple
 * concurrent threads may call the public methods of the class on the same or
 * different objects as long as the constraints mentioned above are satisfied.
 * The thread-pool will grow in size when tasks are submitted and there are
 * no available threads to run them (up to a certain max capacity threshold).
 *
 * <p>Notice that the max. capacity on threads to be created entails a hidden
 * possibility for starvation: if up to capacity tasks are sent to this
 * executor, and will all be waiting for messages (or, are more generally
 * dependent upon) other tasks to be created and executed later from this same
 * executor, the application will lock: the latter tasks will never be executed
 * since this executor will be full, its threads executing tasks waiting for the
 * latter tasks that are waiting for the former ones to finish. In general,
 * such an application must know in advance the maximum number of tasks that
 * may be waiting concurrently for others, and specify a thread-capacity above
 * this maximum. 
 * Another option would be to use the <CODE>LimitedTimeTaskExecutor</CODE>
 * with no upper bound on the task execution time (the latter mentioned executor
 * does not pose any upper bound on the number of threads that may be created
 * in its thread-pool.)</p>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class DynamicAsynchTaskExecutor {
  private static int _nextId = 0;
  private int _id;  // DynamicAsynchTaskExecutor id
  private List _threads;  // used to be Vector
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
	 * @return DynamicAsynchTaskExecutor properly initialized
	 * @throws ParallelException 
	 */
	public static DynamicAsynchTaskExecutor newDynamicAsynchTaskExecutor(int numthreads, int maxthreads) throws ParallelException {
		DynamicAsynchTaskExecutor ex = new DynamicAsynchTaskExecutor(numthreads, maxthreads);
		ex.initialize();
		return ex;
	}
	
	
	/**
	 * replaces public constructor.
	 * @param numthreads int
	 * @param maxthreads int
	 * @param runoncurrent boolean
	 * @return DynamicAsynchTaskExecutor properly initialized
	 * @throws ParallelException 
	 */
	public static DynamicAsynchTaskExecutor newDynamicAsynchTaskExecutor(int numthreads, int maxthreads, 
					                                                             boolean runoncurrent) throws ParallelException {
		DynamicAsynchTaskExecutor ex = new DynamicAsynchTaskExecutor(numthreads, maxthreads, runoncurrent);
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
  private DynamicAsynchTaskExecutor(int numthreads, int maxthreads) throws ParallelException {
    if (numthreads<=0) throw new ParallelException("constructor arg must be > 0");
    if (maxthreads > SimpleFasterMsgPassingCoordinator.getMaxSize() ||
        numthreads > maxthreads)
      throw new ParallelException("cannot construct so many threads");
    _id = getNextObjId();
    _threads = new ArrayList(numthreads);  // numthreads arg. denotes capacity
    _maxNumThreads = maxthreads;
		/* itc 2015-15-01: moved to initialize() method
    for (int i=0; i<numthreads; i++) {
      Thread ti = new DATEThread(this, -(i+1));
      _threads.addElement(ti);
      ti.setDaemon(true);  // thread will end when main thread ends
      ti.start();
    }
    _isRunning = true;
    _sfmpc = SimpleFasterMsgPassingCoordinator.getInstance("DynamicAsynchTaskExecutor" + _id);
		*/
  }


  /**
   * private constructor, constructing a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool.
   * @param maxthreads int the max number of threads in the thread-pool.
   * @param runoncurrent boolean if false no task will run on current thread in
   * case the threads in the pool are full and no new thread can be created.
   * @throws ParallelException if numthreads &le; 0 or if numthreads&gt;maxthreads.
   */
  private DynamicAsynchTaskExecutor(int numthreads, int maxthreads,
                                   boolean runoncurrent)
      throws ParallelException {
    this(numthreads, maxthreads);
    _runOnCurrent = runoncurrent;
  }

	
	/**
	 * called exactly once, right after an object has been constructed.
	 */
	private void initialize() {
		final int numthreads = _threads.size();
    for (int i=0; i<numthreads; i++) {
      Thread ti = new DATEThread(this, -(i+1));
      _threads.add(ti);
      ti.setDaemon(true);  // thread will end when main thread ends
      ti.start();
    }
    _isRunning = true;
    _sfmpc = SimpleFasterMsgPassingCoordinator.getInstance("DynamicAsynchTaskExecutor" + _id);		
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
   * <CODE>DynamicAsynchTaskExecutor</CODE> was constructed via the
   * two-argument constructor, or the third argument in the three-argument
   * constructor was true, then the task executes in the current thread.
   * The call will also block in case the queue of tasks in the thread-pool
   * (implemented in <CODE>SimpleFasterMsgPassingCoordinator</CODE> class of
   * this package) is full (default=10000) in which case the current thread will
   * wait until threads in the thread-pool finish up their tasks so the queue
   * becomes less than full and can accept more tasks.
   * @param task Runnable
   * @throws ParallelException if the shutDown() method has been called prior
   * to this call or if there are no available threads to run the task, nor is
   * it possible to create a new thread (all threads up to capacity are already
   * busy) and this <CODE>DynamicAsynchTaskExecutor</CODE> object does not allow
   * running a task in the current thread.
   */
  public void execute(Runnable task) throws ParallelException {
    if (task == null)return;
    boolean run_on_current = false;
    synchronized (this) {
			if (!_isRunning) throw new ParallelException("thread-pool not running");
      ++_numTasksSubmitted;
      utils.Messenger.getInstance().msg("Current total #threads="+getNumThreads(),1);
      if (isOK2SubmitTask() || task instanceof DATEPoissonPill) {
        _sfmpc.sendDataBlocking(_id, task);
      }
      else {
        if (getNumThreads() < _maxNumThreads) { // create new thread
          Thread ti = new DATEThread(this, - (getNumThreads() + 1));
          _threads.add(ti);
          ti.setDaemon(true); // thread will end when main thread ends
          ti.start();
          _sfmpc.sendDataBlocking(_id, task);
        }
        else {  // no capacity remaining
          ++_numTasksHandled;  // for both outcomes of the if-then-else stmt.
          if (_runOnCurrent) run_on_current = true;
          else {  // throw exception
            throw new ParallelException("DynamicAsynchTaskExecutor.execute(): not enough capacity to submit task now");
          }
        }
      }
    }  // end synchronized block
    if (run_on_current) {
      utils.Messenger.getInstance().msg(
          "DynamicAsynchTaskExecutor.execute(task): running task on current thread",
          0);
      task.run(); // run on current thread
    }
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
      DATEPoissonPill p = new DATEPoissonPill();
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


  private synchronized static int getNextObjId() { return ++_nextId; }


  /**
   * nested helper class for DynamicAsynchTaskExecutor. Not part of the public
	 * API.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class DATEThread extends Thread {
    private DynamicAsynchTaskExecutor _e;
    private int _id;
    private boolean _isIdle=true;
    private SimpleFasterMsgPassingCoordinator _datetmpc;

    /**
     * public constructor. The id argument is a negative integer.
     * @param e DynamicAsynchTaskExecutor
     * @param id int
     */
    public DATEThread(DynamicAsynchTaskExecutor e, int id) {
      _e = e;
      _id = id;
    }


    /**
     * the run() method of the thread, loops continuously, waiting for a task
     * to arrive via the SimpleFasterMsgPassingCoordinator class and executes it.
     * Any exceptions the task throws are caught &amp; ignored. In case the data that
     * arrives is a PoissonPill, the thread exits its run() loop.
     */
    public void run() {
      final int fpbteid = _e.getObjId();
      _datetmpc = SimpleFasterMsgPassingCoordinator.getInstance("DynamicAsynchTaskExecutor"+fpbteid);
      boolean do_run = true;
      while (do_run) {
        Object data = _datetmpc.recvData(_id);
        setIdle(false);
        try {
          if (data instanceof DATEPoissonPill) {
            do_run = false; // done
            break;
          }
          else if (data instanceof Runnable) ( (Runnable) data).run();
          else throw new ParallelException("data object cannot be run");
        }
        catch (Exception e) {
          e.printStackTrace();  // task threw an exception, ignore and continue
        }
        incrNumTasksHandled();  // indicate task was "handled".
        setIdle(true);
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
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class DATEPoissonPill implements Runnable {
    public void run() {
      // no-op
    }
  }

}

