package parallel.distributed;

import parallel.*;
import java.util.*;
import java.io.Serializable;


/**
 * Class implements thread-pooling to allow its users to execute
 * concurrently a batch of tasks implementing the <CODE>TaskObject</CODE> 
 * interface. The <CODE>run()</CODE> method of each task must clearly
 * be thread-safe!, and also, after calling executeBatch(tasks), no thread
 * should be able to manipulate in any way the submitted tasks or their
 * container (the Collection argument to the call). Unfortunately, there is no
 * mechanism in the java programming language to enforce this constraint; the
 * user of the library has to enforce this (mild) constraint in their code.
 * The class utilizes the Message-Passing mechanism implemented in the
 * MsgPassingCoordinator class of this package. The class itself is
 * thread-safe meaning that there can exist multiple PDBatchTaskExecutor objects
 * & multiple concurrent threads may call the public methods of the class on the
 * same or different objects as long as the above mentioned constraints are
 * satisfied. Also, notice that due to the synchronized nature of the
 * executeBatch() method, despite the fact that the tasks in the batch execute
 * concurrently, two concurrent calls of the executeBatch() method from two
 * different threads will execute serially. For this reason, there is no need
 * for this executor to implement dynamic thread management (such as starting
 * more threads upon higher loads, or modifying threads' priorities etc. as is
 * done in the LimitedTimeTaskExecutor class).
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
  private FairDMCoordinator _rwLocker=null;  // used to query if executor is idle
  private boolean _isIdle;

  private MsgPassingCoordinator _mpc_for;
  private MsgPassingCoordinator _mpc_bak;

	
  /**
   * public factory constructor, constructing a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool
	 * @return PDBatchTaskExecutor properly initialized
   * @throws ParallelException if numthreads &lte; 0.
   */	
	public static PDBatchTaskExecutor 
				newPDBatchTaskExecutor(int numthreads) throws ParallelException {
		PDBatchTaskExecutor ex = new PDBatchTaskExecutor(numthreads);
		ex.initialize();
		return ex;
	}

	
  /**
   * public factory constructor, constructing a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool
   * @param bsize int the batch size to be submitted each time to the threadpool
	 * @return PDBatchTaskExecutor properly initialized
   * @throws ParallelException if numthreads &lte; 0.
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
   * @throws ParallelException if numthreads &lte; 0.
   */
  private PDBatchTaskExecutor(int numthreads) throws ParallelException {
    if (numthreads<=0) throw new ParallelException("constructor arg must be > 0");
    _id = getNextObjId();
    _threads = new PDBTEThread[numthreads];
		/* itc: 2015-01-15 moved below code to initialize() method
    for (int i=0; i<numthreads; i++) {
      _threads[i] = new PDBTEThread(this);
      _threads[i].setDaemon(true);  // thread will end when main thread ends
      _threads[i].start();
    }
    _isRunning = true;
    _isIdle = true;
    _batchSize = MsgPassingCoordinator.getMaxSize()/2;
    _rwLocker = FairDMCoordinator.getInstance("PDBatchTaskExecutor"+_id);
    _mpc_for =
        MsgPassingCoordinator.getInstance("PDBatchTaskExecutor"+_id);
    _mpc_bak =
        MsgPassingCoordinator.getInstance("PDBatchTaskExecutor_ack"+_id);
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
      _threads[i] = new PDBTEThread(this);
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
   * @param tasks Collection Collection<TaskObject>. If the collection contains
   * an object that does not implement the TaskObject interface, the object
   * will be added in the results vector in the corresponding order without any
   * processing done (even if it implements the Runnable interface or defines a
   * run() method).
   * @throws ParallelException if the shutDown() method has been called prior
   * to this call
   * @return Vector Vector<Serializable> the successfully executed task results.
   * If a task threw an exception then the TaskObject itself is returned instead
   * of the expected result.
   * If tasks were null, then it also returns null.
   */
  public synchronized Vector executeBatch(Collection tasks) throws ParallelException {
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
    if (_threads!=null)
      return _threads.length;
    else return 0;
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

}


/**
 * auxiliary class for PDBatchTaskExecutor, implementing the threads in the
 * executor's thread-pool.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class PDBTEThread extends Thread {
  private static Hashtable _ids = new Hashtable();  // map<PDBTE e, Integer curId>
  private PDBatchTaskExecutor _e;
  private boolean _doRun=true;

  PDBTEThread(PDBatchTaskExecutor e) {
    synchronized (PDBTEThread.class) {
      if (_ids.get(e)==null)
        _ids.put(e, new Integer(0));
    }
    _e = e;
  }


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
        Object data = mpc_fwd.recvData(0, id);
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
          e.printStackTrace();  // task threw an exception, ignore and continue
          // send back the data object as acknowledgement on the "back-channel"
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


class PoissonPill {
  // denotes end of computations for receiving thread
}


class FailedExecutionResult implements Serializable {
  private static final long serialVersionUID = 5361446529275021760L;
  // denotes a failed execution of a task
}

