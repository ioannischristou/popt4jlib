package parallel.distributed;

import parallel.*;
import java.util.*;
import java.io.Serializable;


/**
 * Class implements thread-pooling to allow its users to execute
 * concurrently a batch of tasks implementing the <CODE>TaskObject</CODE> 
 * interface. The <CODE>run()</CODE> method of each task must clearly
 * be thread-safe!, must return non-null value under normal execution, and also, 
 * after calling <CODE>executeBatch(tasks)</CODE>, no thread
 * should be able to manipulate in any way the submitted tasks or their
 * container (the Collection argument to the call). Unfortunately, there is no
 * mechanism in the java programming language to enforce this constraint; the
 * user of the library has to enforce this (mild) constraint in their code.
 * The class utilizes the Message-Passing mechanism implemented in the
 * MsgPassingCoordinator class of this package. The class itself is
 * thread-safe meaning that there can exist multiple PDBatchTaskExecutor objects
 * &amp; multiple concurrent threads may call the public methods of the class on 
 * the same or different objects as long as the above mentioned constraints are
 * satisfied. Also, notice that due to the synchronized nature of the
 * executeBatch() method, despite the fact that the tasks in the batch execute
 * concurrently, two concurrent calls of the executeBatch() method from two
 * different threads will execute serially. For this reason, there is no need
 * for this executor to implement dynamic thread management (such as starting
 * more threads upon higher loads, or modifying threads' priorities etc. as is
 * done in the <CODE>parallel.LimitedTimeTaskExecutor</CODE> class).
 * The class is meant to be used in conjunction with
 * PDBatchTaskExecutor[Srv/Clt/Wrk] classes that will allow parallel/distributed
 * execution of <CODE>TaskObject</CODE> objects in remote JVMs running under
 * parallel (multi-core) machines.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class PDBatchTaskExecutor {
  private static int _nextId = 0;
  private int _id;  // PDBatchTaskExecutor id
  private int _curId=0;
  private PDBTEThread[] _threads;
  private boolean _isRunning;
  private int _batchSize;
  private FairDMCoordinator _rwLocker=null;  // used 2 query if executor is idle
	                                           // running or number of its threads
	                                           // concurrently with executeBatch()
	                                           // call
  private boolean _isIdle;

  private MsgPassingCoordinator _mpc_for;
  private MsgPassingCoordinator _mpc_bak;

	
  /**
   * public factory constructor, constructs a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool
	 * @return PDBatchTaskExecutor properly initialized
   * @throws ParallelException if numthreads &le; 0.
   */	
	public static PDBatchTaskExecutor 
				newPDBatchTaskExecutor(int numthreads) throws ParallelException {
		PDBatchTaskExecutor ex = new PDBatchTaskExecutor(numthreads);
		ex.initialize();
		return ex;
	}

	
  /**
   * public factory constructor, constructs a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool
   * @param bsize int the batch size to be submitted each time to the threadpool
	 * @return PDBatchTaskExecutor properly initialized
   * @throws ParallelException if numthreads &le; 0.
   */	
	public static PDBatchTaskExecutor 
				newPDBatchTaskExecutor(int numthreads, int bsize) 
								throws ParallelException {
		PDBatchTaskExecutor ex = new PDBatchTaskExecutor(numthreads, bsize);
		ex.initialize();
		return ex;
	}

	
  /**
   * private constructor, constructing a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool
   * @throws ParallelException if numthreads &le; 0.
   */
  private PDBatchTaskExecutor(int numthreads) throws ParallelException {
    if (numthreads<=0) throw new ParallelException("ctor arg must be > 0");
    _id = getNextObjId();
    _threads = new PDBTEThread[numthreads];
  }


  /**
   * private constructor, allowing to reduce the size of the batches submitted
   * to the thread pool. If the 2nd (bsize) argument is greater than the default
   * batch size, namely MsgPassingCoordinator.getMaxSize()/2, it is simply
   * ignored.
   * @param numthreads int the number of threads in the thread-pool
   * @param bsize int the batch size to be submitted each time to the threadpool
   * @throws ParallelException if numthreads &le; 0
   */
  private PDBatchTaskExecutor(int numthreads, int bsize)
      throws ParallelException {
    this(numthreads);
    if (bsize < _batchSize) _batchSize = bsize;
  }

	
	/**
	 * called exactly once right after this object is constructed.
	 */
	private void initialize() {
    _isRunning = true;
    _isIdle = true;
    _batchSize = MsgPassingCoordinator.getMaxSize()/2;
    _rwLocker = FairDMCoordinator.getInstance("PDBatchTaskExecutor"+_id);
    _mpc_for =
        MsgPassingCoordinator.getInstance("PDBatchTaskExecutor"+_id);
    _mpc_bak =
        MsgPassingCoordinator.getInstance("PDBatchTaskExecutor_ack"+_id);		
		final int numthreads = _threads.length;
    for (int i=0; i<numthreads; i++) {
      _threads[i] = new PDBTEThread(this,-(i+1));  // thread-id=-(i+1)
      _threads[i].setDaemon(true);  // thread will end when main thread ends
      _threads[i].start();
    }
	}

	
  /**
   * the main method of the class. Executes all tasks in the argument collection
   * (must be objects implementing the TaskObject interface in package parallel)
   * ignoring any exceptions the tasks may throw when their run() method is
   * invoked. The tasks are executed in batches of up to
   * <CODE>MsgPassingCoordinator.getMaxSize()/2</CODE>. The call is blocking, so
   * that the method does not return until all tasks have been executed
   * (successfully or not). Asynchronous versions are implemented in the
   * ParallelAsynchBatchTaskExecutor and FasterParallelAsynchBatchTaskExecutor
   * classes.
   * @param tasks Collection // Collection&lt;TaskObject&gt;. If the collection 
	 * contains an object that does not implement the TaskObject interface, object
   * will be added in the results vector in the corresponding order without any
   * processing done (even if it implements the Runnable interface or defines a
   * run() method).
   * @throws ParallelException if the shutDown() method has been called prior
   * to this call
   * @return Vector // Vector&lt;Serializable&gt; the successfully executed task 
	 * results. If a task threw an exception then the TaskObject itself is 
	 * returned instead of the expected result.
   * If tasks were null, then it also returns null.
   */
  public synchronized Vector executeBatch(Collection tasks) 
		throws ParallelException {
    if (tasks==null) return null;
    if (_isRunning==false)
      throw new ParallelException("thread-pool is not running");
    try {
      _rwLocker.getWriteAccess();
      _isIdle = false;
      _rwLocker.releaseWriteAccess();
    }
    catch (Exception e) {  // can never get here
      e.printStackTrace();
    }
    Vector results = new Vector();
    // submit the tasks in batches
    int batch_counter = 0;
    Iterator it = tasks.iterator();
    while (it.hasNext()) {
      Set ids = new TreeSet();  // order tasks in order of id they get
      while (it.hasNext()) {
        Object task = it.next();
        int id = getNextId();
        ids.add(new Integer(id));
        _mpc_for.sendDataBlocking(id, task);
        if (++batch_counter==_batchSize) {
          batch_counter = 0;
          break;
        }
      }
      // now wait for each submitted task to finish
      Iterator itback = ids.iterator();
      while (itback.hasNext()) {
        int id = ( (Integer) itback.next()).intValue();
        // result to be received on the "back-channel"
        Object result =
            _mpc_bak.recvData(-1, id);
        if (result != null)
          results.add(result);
        else results.add(new FailedExecutionResult());
      }
    }
    // declare availability
    try {
      _rwLocker.getWriteAccess();
      _isIdle = true;
      _rwLocker.releaseWriteAccess();
    }
    catch (Exception e) {  // can never get here
      e.printStackTrace();
    }
    return results;
  }
	

	/**
	 * executes the task passed as parameter on each thread in this executor's
	 * thread-pool. The threads will execute the task concurrently, so if 
	 * sequential execution is needed, the task must be properly synchronized.
	 * Useful when a special-command (such as a request for all threads to load or 
	 * update some data) must be issued to the executor. As the 
	 * <CODE>executeBatch(tasks)</CODE> method, this method executes synchronously 
	 * and atomically. Notice that a special trick is utilized to achieve this
	 * functionality: the threads in this executor all have negative ids (starting
	 * from -1); when this method submits the task to the thread-pool via the 
	 * associated forward <CODE>MsgPassingCoordinator</CODE> queue, it sends the 
	 * same task via a call to the 
	 * <CODE>sendDataBlocking(nexttaskid, threadid, task)</CODE> method of the 
	 * forward msg-passing coordinator for threadid=-1...-#threads_in_pool. The 
	 * fact that the threadid is a negative number causes the threads receiving
	 * tasks to only be interested in matching their id (negative number) with the
	 * threadid designated in the call mentioned above, and thus guarantee that 
	 * each of the threads in the thread-pool will execute the task submitted to 
	 * it. Notice that it is not possible to try to sequentially execute this task 
	 * on the various threads in the pool.
	 * @param task TaskObject 
	 * @throws ParallelException
	 */
	public synchronized void executeTaskOnAllThreads(TaskObject task) 
		throws ParallelException {
		if (task==null) return;
    if (_isRunning==false)
      throw new ParallelException("thread-pool is not running");
    try {
      _rwLocker.getWriteAccess();
      _isIdle = false;
      _rwLocker.releaseWriteAccess();
    }
    catch (Exception e) {  // can never get here
      e.printStackTrace();
    }
		// ask task to execute on each thread.
		// Notice that trying to execute both the sendDataBlocking, and the 
		// recvData on the back-channel in the same iteration of a single for-loop
		// below is not possible (will not work correctly as there will be in 
		// general problems on the id waited on on the back-channel in the 
		// same-loop case).
		TreeSet ids = new TreeSet();
		for (int i=0; i<_threads.length; i++) {
			int id = getNextId();
			ids.add(new Integer(id));
			//System.err.println("PDBTExecutor: sending task with myid="+id+
			//                   " threadId="+(-i-1));
			_mpc_for.sendDataBlocking(id, -(i+1), task);
			//System.err.println("PDBTExecutor: task sent successfully. "+
			//                   "Now waiting to recvData with myid=-1 fromId="+id);
		}
		// get results back and ignore them
		Iterator it = ids.iterator();
		while (it.hasNext()) {
			int id = ((Integer)it.next()).intValue();
			_mpc_bak.recvData(-1, id);
			//System.err.println("PDBTExecutor: result with id="+id+" received.");
		}
		
    // declare availability
    try {
      _rwLocker.getWriteAccess();
      _isIdle = true;
      _rwLocker.releaseWriteAccess();
    }
    catch (Exception e) {  // can never get here
      e.printStackTrace();
    }
	}
	

  /**
   * shut-down all the threads in this executor's thread-pool.
   * The executor cannot be used afterwards, and will throw ParallelException
   * if the method executeBatch() or shutDown() is called again.
   * @throws ParallelException
   */
  public synchronized void shutDown() throws ParallelException {
    if (_isRunning==false)
      throw new ParallelException("shutDown() has been called already");
    final int numthreads = _threads.length;
    for (int i=0; i<numthreads; i++) {
      _threads[i].stopRunning();
      MsgPassingCoordinator.getInstance("PDBatchTaskExecutor"+_id).
                              sendDataBlocking(getNextId(), new PoissonPill());
    }
		try {
			_rwLocker.getWriteAccess();
			_isRunning = false;
			_rwLocker.releaseWriteAccess();
		}
		catch (ParallelException e) {  // cannot happen
			e.printStackTrace();
		}
    return;
  }


  /**
   * returns true iff shutDown() has not yet been called on this object.
   * @return boolean
   */
  public boolean isLive() {
    try {
      _rwLocker.getReadAccess();
      return _isRunning;
    }
    finally {
			try {
				_rwLocker.releaseReadAccess();
			}
			catch (ParallelException e) {  // cannot happen
				e.printStackTrace();
			}
    }
  }


  /**
   * return the number of threads in the thread-pool.
   * @return int
   */
  public int getNumThreads() {
    try {
      _rwLocker.getReadAccess();
      return _threads.length;
    }
    finally {
			try {
				_rwLocker.releaseReadAccess();
			}
			catch (ParallelException e) {  // cannot happen
				e.printStackTrace();
			}
    }
  }


  /**
   * report if at this particular moment, the executor is idle or if its thread
   * pool is currently running some tasks.
   * @return boolean
   */
  public boolean isIdle() {
    try {
      _rwLocker.getReadAccess();
      return _isIdle;
    }
    finally {
			try {
				_rwLocker.releaseReadAccess();
			}
			catch (ParallelException e) {
				e.printStackTrace();  // cannot happen
			}
    }
  }


  int getObjId() { return _id; }
  private synchronized static int getNextObjId() { return ++_nextId; }
  private synchronized int getNextId() { return ++_curId; }


	/**
	 * auxiliary class for PDBatchTaskExecutor, implementing the threads in the
	 * executor's thread-pool. Not part of the public API.
	 * <p>Title: popt4jlib</p>
	 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
	 * <p>Copyright: Copyright (c) 2011</p>
	 * <p>Company: </p>
	 * @author Ioannis T. Christou
	 * @version 1.0
	 */
	static class PDBTEThread extends Thread {
		private static HashMap _ids = new HashMap();  // map<PDBTE e, Integer curId>
		private PDBatchTaskExecutor _e;  // unnecessary now
		private boolean _doRun=true;
		private int _id;

		/**
		 * sole constructor.
		 * @param e PDBatchTaskExecutor was needed before class became nested.
		 * @param id int the id of the thread, starts in {0,...threadpool_size-1}.
		 */
		PDBTEThread(PDBatchTaskExecutor e, int id) {
			synchronized (PDBTEThread.class) {
				if (_ids.get(e)==null)
					_ids.put(e, new Integer(0));
			}
			_e = e;
			_id = id;
		}


		/**
		 * waits for <CODE>TaskObject</CODE> objects to execute from the related
		 * "PDBatchTaskExecutor$id$ message-passing queue, executes them, and sends
		 * the same task object on the "back-channel" as acknowledgement of 
		 * execution. If it encounters a <CODE>PoissonPill</CODE> object, the method
		 * returns.
		 */
		public void run() {
			final int pdbteid = _e.getObjId();
			final MsgPassingCoordinator mpc_fwd =
					MsgPassingCoordinator.getInstance("PDBatchTaskExecutor"+pdbteid);
			final MsgPassingCoordinator mpc_bak =
					MsgPassingCoordinator.getInstance("PDBatchTaskExecutor_ack"+pdbteid);
			while (isRunning()) {
				// get the "sender" id
				int id = getNextId(_e);
				try {
					// threads all use the same receiver id
					// Object data = mpc_fwd.recvData(0, id);
					// itc20170629: threads now use their own receiver id to support
					// the new method executeTaskOnAllThreads
					Object data = mpc_fwd.recvDataIgnoringFromIdOnNegativeMyId(_id, id);
					try {
						if (data instanceof TaskObject) {
							Serializable result = ( (TaskObject) data).run();
							// Results are expected in the returned object
							mpc_bak.sendDataBlocking(id, -1, result);
						}
						else if (data instanceof PoissonPill) break;  // done
						else throw new ParallelException("data object cannot be run");
					}
					catch (Exception e) {
						e.printStackTrace();  // task threw an exception, ignore & continue
						// send back data object as acknowledgement on the "back-channel"
						mpc_bak.sendDataBlocking(id, -1, data);
					}
				}
				catch (ParallelException e) {
					e.printStackTrace();  // no-op
				}
			}
		}


		synchronized void stopRunning() { _doRun=false; }


		private synchronized boolean isRunning() { return _doRun; }


		private static synchronized int getNextId(PDBatchTaskExecutor e) {
			// this method must be the same as the one in PDBatchTaskExecutor
			// return ++_curId;
			Integer curId = (Integer) _ids.get(e);
			int nextid = curId.intValue()+1;
			_ids.put(e, new Integer(nextid));
			return nextid;
		}

	}

	
	/**
	 * empty inner-class represents command for ending the threads in the 
	 * thread-pool. Not part of the public API.
	 */
	class PoissonPill {
		// denotes end of computations for receiving thread
	}


	/**
	 * empty inner-class represents response is an indication of a failed 
	 * execution. Not part of the public API.
	 */
	class FailedExecutionResult implements Serializable {
		// private static final long serialVersionUID = 5361446529275021760L;
		// denotes a failed execution of a task
	}

}
