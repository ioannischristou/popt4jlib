package parallel;

import java.util.*;

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
 * The class utilizes the (faster) Message-Passing mechanism implemented in the
 * SimpleFasterMsgPassingCoordinator class of this package. The class itself is
 * thread-safe meaning that there can exist multiple
 * <CODE>FasterParallelAsynchBatchTaskExecutor</CODE> objects, and multiple
 * concurrent threads may call the public methods of the class on the same or
 * different objects as long as the constraints mentioned above are satisfied.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class FasterParallelAsynchBatchTaskExecutor {
  private static int _nextId = 0;
  private int _id;  // ParallelBatchTaskExecutor id
  private FPABTEThread[] _threads;
  private boolean _isRunning;
  private boolean _runOnCurrent=true;
  private SimpleFasterMsgPassingCoordinator _sfmpc;

  /**
   * public constructor, constructing a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool
   * @throws ParallelException if numthreads <= 0 or if too many threads are
   * asked to be created.
   */
  public FasterParallelAsynchBatchTaskExecutor(int numthreads) throws ParallelException {
    if (numthreads<=0) throw new ParallelException("constructor arg must be > 0");
    if (numthreads > SimpleFasterMsgPassingCoordinator.getMaxSize()/2)
      throw new ParallelException("cannot construct so many threads");
    _id = getNextObjId();
    _threads = new FPABTEThread[numthreads];
    for (int i=0; i<numthreads; i++) {
      _threads[i] = new FPABTEThread(this, -(i+1));
      _threads[i].setDaemon(true);  // thread will end when main thread ends
      _threads[i].start();
    }
    _isRunning = true;
    _sfmpc = SimpleFasterMsgPassingCoordinator.getInstance("FasterParallelAsynchBatchTaskExecutor"+_id);
  }


  /**
   * public constructor, constructing a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool
   * @param runoncurrent boolean if false no task will run on current thread in
   * case the threads in the pool are full.
   * @throws ParallelException if numthreads <= 0.
   */
  public FasterParallelAsynchBatchTaskExecutor(int numthreads,
                                               boolean runoncurrent)
      throws ParallelException {
    this(numthreads);
    _runOnCurrent = runoncurrent;
  }


  /**
   * get the current number of tasks in the queue awaiting processing.
   * @return int
   */
  public int getNumTasksInQueue() {
    return _sfmpc.getNumTasksInQueue();
  }


  /**
   * the main method of the class. Submits all tasks in the argument collection
   * (must be objects implementing the <CODE>TaskObject</CODE> interface in
   * package <CODE>parallel</CODE> or the native <CODE>Runnable</CODE> interface)
   * for execution, and the thread-pool will ignore any exceptions any task may
   * throw when its run() method is invoked.
   * The call is asynchronous, so that the method does return immediately
   * without waiting for any task to complete; however, if all threads in the
   * thread-pool are busy and the
   * <CODE>FasterParallelAsynchBatchTaskExecutor</CODE> was constructed via the
   * single-argument constructor, or the second argument in the two-argument
   * constructor was true, then the next task executes in the current thread.
   * The call will also block in case the queue of tasks in the thread-pool
   * (implemented in <CODE>SimpleFasterMsgPassingCoordinator</CODE> class of
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
   * @param tasks Collection
   * @throws ParallelException if the shutDown() method has been called prior
   * to this call
   * @throws ParallelExceptionUnSubmittedTasks if this object does not allow
   * running tasks in the current thread and some tasks could not be sent to
   * the thread-pool due to a full <CODE>SimpleFasterMsgPassingCoordinator</CODE>
   * msg-queue; in this case the unsubmitted tasks are returned inside the
   * exception object.
   */
  public void executeBatch(Collection tasks) throws ParallelException, ParallelExceptionUnsubmittedTasks {
    if (tasks == null)return;
    Vector unsubmitted_tasks = new Vector();  // tasks that couldn't be submitted
    Iterator it = tasks.iterator();
    if (isRunning() == false)
      throw new ParallelException("thread-pool is not running");
    while (it.hasNext()) {
      Object task = it.next();
      if ( (!_runOnCurrent && existsRoom()) || existsIdleThread()) {  // ok
        _sfmpc.sendDataBlocking(_id, task);
      }
      else {
        if (!_runOnCurrent) {  // try to submit, if it fails add in Vector,
                               // then send back in exception object.
          try {
            _sfmpc.sendData(_id, task);
          }
          catch (ParallelException e) {
            unsubmitted_tasks.add(task);
          }
        }
        else {
          if (task instanceof TaskObject) ( (TaskObject) task).run();
          else if (task instanceof Runnable) ( (Runnable) task).run();
        }
      }
    }
    if (unsubmitted_tasks.size()>0)
      throw new ParallelExceptionUnsubmittedTasks(unsubmitted_tasks);
  }


  /**
   * shut-down all the threads in this executor's thread-pool. It is the
   * caller's responsibility to ensure that after this method has been called
   * no other thread (including the threads in the thread-pool) won't call the
   * executeBatch() method. Use of condition-counters or similar (e.g. as in the
   * class <CODE>graph.AllMWCFinderBKMT</CODE>) may be necessary when tasks
   * given to the thread-pool for execution may call the
   * <CODE>executeBatch()</CODE> method recursively.
   * The executor cannot be used afterwards, and will throw ParallelException
   * if the method executeBatch() or shutDown() is called again.
   * @throws ParallelException
   * @throws InterruptedException
   * @throws ParallelExceptionUnsubmittedTasks
   */
  public synchronized void shutDown() throws ParallelException, ParallelExceptionUnsubmittedTasks {
    if (_isRunning==false)
      throw new ParallelException("shutDown() has been called already");
    final int numthreads = _threads.length;
    Vector pills = new Vector();
    for (int i=0; i<numthreads; i++) {
      PoissonPill p = new PoissonPill();
      pills.addElement(p);
    }
    executeBatch(pills);
    _isRunning = false;
    return;
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


  int getObjId() { return _id; }


  /**
   * if true, then a task can be "sent" (submittted) to the thread-pool for
   * processing without causing any waiting on the executeBatch() method.
   * @return boolean
   */
  private synchronized boolean existsRoom() {
    return getNumTasksInQueue() <
           SimpleFasterMsgPassingCoordinator.getMaxSize()-2*_threads.length;
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
}


/**
 * helper class for FasterParallelAsynchBatchTaskExecutor.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class FPABTEThread extends Thread {
  private FasterParallelAsynchBatchTaskExecutor _e;
  private int _id;
  private boolean _isIdle=true;

  /**
   * public constructor. The id argument is a negative integer.
   * @param e ParallelAsynchBatchTaskExecutor
   * @param id int
   */
  public FPABTEThread(FasterParallelAsynchBatchTaskExecutor e, int id) {
    _e = e;
    _id = id;
  }


  /**
   * the run() method of the thread, loops continuously, waiting for a task
   * to arrive via the SimpleFasterMsgPassingCoordinator class and executes it.
   * Any exceptions the task throws are caught & ignored. In case the data that
   * arrives is a PoissonPill, the thread exits its run() loop.
   */
  public void run() {
    final int fpbteid = _e.getObjId();
    final SimpleFasterMsgPassingCoordinator sfmpc =
        SimpleFasterMsgPassingCoordinator.getInstance("FasterParallelAsynchBatchTaskExecutor"+fpbteid);
    boolean do_run = true;
    while (do_run) {
      Object data = sfmpc.recvData(_id);
      setIdle(false);
      try {
        if (data instanceof TaskObject) ( (TaskObject) data).run();
        else if (data instanceof Runnable) ( (Runnable) data).run();
        else if (data instanceof PoissonPill) {
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

  synchronized void setIdle(boolean v) { _isIdle = v; }
  synchronized boolean isIdle() { return _isIdle; }
}

