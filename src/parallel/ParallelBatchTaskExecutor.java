package parallel;

import java.util.*;

/**
 * A class that implements thread-pooling to allow its users to execute
 * concurrently a batch of tasks implementing the TaskObject, or more
 * simply, the Runnable interface. The run() method of each task must clearly
 * be thread-safe!, and also, after calling executeBatch(tasks), no thread
 * should be able to manipulate in any way the submitted tasks or their
 * container (the Collection argument to the call). Unfortunately, there is no
 * mechanism in the java programming language to enforce this constraint; the
 * user of the library has to enforce this (mild) constraint in their code.
 * The class utilizes the Message-Passing mechanism implemented in the
 * MsgPassingCoordinator class of this package. The class itself is thread-safe
 * meaning that there can exist multiple ParallelBatchTaskExecutor objects, and
 * multiple concurrent threads may call the public methods of the class on the
 * same or different objects as long as the above mentioned constraints are
 * satisfied. Also, notice that due to the synchronized nature of the
 * executeBatch() method, despite the fact that the tasks in the bath execute
 * concurrently, two concurrent calls of the executeBatch() method from two
 * different threads will execute serially. For this reason, there is no need
 * for this executor to implement dynamic thread management (such as starting
 * more threads upon higher loads, or modifying threads' priorities etc. as is
 * done in the LimitedTimeTaskExecutor class).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class ParallelBatchTaskExecutor {
  private static int _nextId = 0;
  private int _id;  // ParallelBatchTaskExecutor id
  private int _curId=0;
  private PBTEThread[] _threads;
  private boolean _isRunning;
  private int _batchSize;
  private MsgPassingCoordinator _mpcFwd=null;
  private MsgPassingCoordinator _mpcBak=null;

	
  /**
   * public factory constructor, constructing a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool
	 * @return ParallelBatchTaskExecutor properly initialized
   * @throws ParallelException if numthreads &lte; 0.
   */	
	public static ParallelBatchTaskExecutor 
				newParallelBatchTaskExecutor(int numthreads) throws ParallelException {
		ParallelBatchTaskExecutor ex = new ParallelBatchTaskExecutor(numthreads);
		ex.initialize();
		return ex;
	}

	
  /**
   * public factory constructor, constructing a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool
   * @param bsize int the batch size to be submitted each time to the threadpool
	 * @return ParallelBatchTaskExecutor properly initialized
   * @throws ParallelException if numthreads &lte; 0.
   */	
	public static ParallelBatchTaskExecutor 
				newParallelBatchTaskExecutor(int numthreads, int bsize) 
								throws ParallelException {
		ParallelBatchTaskExecutor ex = new ParallelBatchTaskExecutor(numthreads, bsize);
		ex.initialize();
		return ex;
	}

	
  /**
   * private constructor, constructing a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool
   * @throws ParallelException if numthreads &lte; 0.
   */
  private ParallelBatchTaskExecutor(int numthreads) throws ParallelException {
    if (numthreads<=0) throw new ParallelException("constructor arg must be > 0");
    _id = getNextObjId();
    _threads = new PBTEThread[numthreads];
		/* itc 2015-01-15: moved code below to initialize() method
    for (int i=0; i<numthreads; i++) {
      _threads[i] = new PBTEThread(this);
      _threads[i].setDaemon(true);  // thread will end when main thread ends
      _threads[i].start();
    }
    _isRunning = true;
    _batchSize = MsgPassingCoordinator.getMaxSize()/2;
    // get refs to MsgPassingCoordinator's
    _mpcFwd = MsgPassingCoordinator.getInstance("ParallelBatchTaskExecutor"+_id);
    _mpcBak = MsgPassingCoordinator.getInstance("ParallelBatchTaskExecutor_ack"+_id); 
		*/
  }


  /**
   * private constructor, allowing to reduce the size of the batches submitted
   * to the thread pool. If the 2nd (bsize) argument is greater than the default
   * batch size, namely MsgPassingCoordinator.getMaxSize()/2, it is simply
   * ignored.
   * @param numthreads int the number of threads in the thread-pool
   * @param bsize int the batch size to be submitted each time to the threadpool
   * @throws ParallelException if numthreads &lte; 0
   */
  private ParallelBatchTaskExecutor(int numthreads, int bsize)
      throws ParallelException {
    this(numthreads);
    if (bsize < _batchSize) _batchSize = bsize;
  }

	
	/**
	 * called exactly once right after this object is constructed.
	 */
	private void initialize() {
    _isRunning = true;
    _batchSize = MsgPassingCoordinator.getMaxSize()/2;
    // get refs to MsgPassingCoordinator's
    _mpcFwd = MsgPassingCoordinator.getInstance("ParallelBatchTaskExecutor"+_id);
    _mpcBak = MsgPassingCoordinator.getInstance("ParallelBatchTaskExecutor_ack"+_id); 		
		final int numthreads = _threads.length;
    for (int i=0; i<numthreads; i++) {
      _threads[i] = new PBTEThread(this);
      _threads[i].setDaemon(true);  // thread will end when main thread ends
      _threads[i].start();
    }
	}
	

  /**
   * the main method of the class. Executes all tasks in the argument collection
   * (must be objects implementing the TaskObject interface in package parallel
   * or the native Runnable interface), ignoring any exceptions the tasks may
   * throw when their run() method is invoked. The tasks are executed in batches
   * of up to MsgPassingCoordinator.getMaxSize()/2. The call is blocking, so
   * that the method does not return until all tasks have been executed
   * (successfully or not). Asynchronous versions are implemented in the
   * ParallelAsynchBatchTaskExecutor and FasterParallelAsynchBatchTaskExecutor
   * classes.
   * @param tasks Collection
   * @throws ParallelException if the shutDown() method has been called prior
   * to this call
   * @return int the number of successfully executed tasks.
   */
  public synchronized int executeBatch(Collection tasks) throws ParallelException {
    if (tasks==null) return 0;
    if (_isRunning==false)
      throw new ParallelException("thread-pool is not running");
    int runtasks = 0;
    int failedtasks = 0;
    // submit the tasks in batches
    int batch_counter = 0;
    Iterator it = tasks.iterator();
    while (it.hasNext()) {
      Set ids = new HashSet();
      while (it.hasNext()) {
        Object task = it.next();
        int id = getNextId();
        ids.add(new Integer(id));
        //MsgPassingCoordinator.getInstance("ParallelBatchTaskExecutor"+_id).sendDataBlocking(id, task);
        _mpcFwd.sendDataBlocking(id, task);
        if (++batch_counter==_batchSize) {
          batch_counter = 0;
          break;
        }
      }
      // now wait for each submitted task to finish
      Iterator itback = ids.iterator();
      while (itback.hasNext()) {
        int id = ( (Integer) itback.next()).intValue();
        // acknowledgement to be received on the "back-channel"
        Object task =
            // MsgPassingCoordinator.getInstance("ParallelBatchTaskExecutor_ack"+_id).recvData(-1, id);
            _mpcBak.recvData(-1, id);
        if (task instanceof TaskObject && ( (TaskObject) task).isDone() == false)
          ++failedtasks;
        else ++runtasks;
      }
    }
    return runtasks;
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
      //MsgPassingCoordinator.getInstance("ParallelBatchTaskExecutor"+_id).
      _mpcFwd.sendDataBlocking(getNextId(), new PoissonPill());
    }
    _isRunning = false;
    return;
  }


  public synchronized void shutDownOld() throws ParallelException {
    if (_isRunning==false)
      throw new ParallelException("shutDownXXX() has been called already");
    final int numthreads = _threads.length;
    Vector pills = new Vector();
    for (int i=0; i<numthreads; i++) {
      OldPill p = new OldPill();
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
  private synchronized static int getNextObjId() { return ++_nextId; }
  private synchronized int getNextId() { return ++_curId; }

}


class PBTEThread extends Thread {
  private static Hashtable _ids = new Hashtable();  // map<PBTE e, Integer curId>
  private ParallelBatchTaskExecutor _e;
  private boolean _doRun=true;

  public PBTEThread(ParallelBatchTaskExecutor e) {
    synchronized (PBTEThread.class) {
      if (_ids.get(e)==null)
        _ids.put(e, new Integer(0));
    }
    _e = e;
  }


  public void run() {
    final int pbteid = _e.getObjId();
    final MsgPassingCoordinator _mpcFwd = MsgPassingCoordinator.getInstance("ParallelBatchTaskExecutor"+pbteid);
    final MsgPassingCoordinator _mpcBak = MsgPassingCoordinator.getInstance("ParallelBatchTaskExecutor_ack"+pbteid);
    while (isRunning()) {
      // get the "sender" id
      int id = getNextId(_e);
      try {
        // threads all use the same receiver id
        // Object data = MsgPassingCoordinator.getInstance("ParallelBatchTaskExecutor"+pbteid).
        Object data = _mpcFwd.recvData(0, id);
        try {
          if (data instanceof TaskObject) ( (TaskObject) data).run();
          else if (data instanceof Runnable) ( (Runnable) data).run();
          else if (data instanceof PoissonPill) break;  // done
          else if (data instanceof OldPill) stopRunning();  // done
          else throw new ParallelException("data object cannot be run");
        }
        catch (Exception e) {
          e.printStackTrace();  // task threw an exception, ignore and continue
        }
        // send back the data object as acknowledgement on the "back-channel"
        // any results may be written in the data object itself
        // MsgPassingCoordinator.getInstance("ParallelBatchTaskExecutor_ack"+pbteid).
        _mpcBak.sendDataBlocking(id, -1, data);
      }
      catch (ParallelException e) {
        e.printStackTrace();  // no-op
      }
    }
  }


  synchronized void stopRunning() { _doRun=false; }


  private synchronized boolean isRunning() { return _doRun; }


  private static synchronized int getNextId(ParallelBatchTaskExecutor e) {
    // this method must be the same as the one in ParallelBatchTaskExecutor
    // return ++_curId;
    Integer curId = (Integer) _ids.get(e);
    int nextid = curId.intValue()+1;
    _ids.put(e, new Integer(nextid));
    return nextid;
  }

}


class PoissonPill {
  // denotes end of computations for receiving thread
}


// for testing old shutdown() method only
class OldPill {
  // denots end of computations for receiving thread
}

