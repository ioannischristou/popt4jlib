package parallel;

import java.util.*;

/**
 * A class that implements thread-pooling to allow its users to execute
 * concurrently a batch of tasks implementing the <CODE>TaskObject</CODE>, or 
 * more simply, the <CODE>Runnable</CODE> interface. The <CODE>run()</CODE> 
 * method of each task must clearly be thread-safe!, and also, after calling 
 * <CODE>executeBatch(tasks)</CODE>, no thread should be able to manipulate in 
 * any way the submitted tasks or their container (the <CODE>Collection</CODE> 
 * argument to the call). Unfortunately, there is no mechanism in the java 
 * programming language to enforce this constraint; the user of the library has 
 * to enforce this (mild) constraint in their code.
 * The class utilizes the Message-Passing mechanism implemented in the
 * <CODE>MsgPassingCoordinator</CODE> class of this package. The class itself is 
 * thread-safe meaning that there can exist multiple 
 * <CODE>ParallelBatchTaskExecutor</CODE> objects, and multiple concurrent 
 * threads may call the public methods of the class on the same or different 
 * objects as long as the above mentioned constraints are satisfied. Also, 
 * notice that due to the synchronized nature of the
 * <CODE>executeBatch()</CODE> method, despite the fact that the tasks in the 
 * batch execute concurrently, two concurrent calls of the 
 * <CODE>executeBatch()</CODE> method from two different threads will execute 
 * serially. For this reason, there is no need for this executor to implement 
 * dynamic thread management (such as starting more threads upon higher loads, 
 * or modifying threads' priorities etc. as is done in the 
 * <CODE>LimitedTimeTaskExecutor</CODE> class).
 * <p>Notes:
 * <ul>
 * <li> 2020-06-07: modified the methods <CODE>getXXXId()</CODE> to work 
 * correctly even if the tasks submitted are more than 
 * <CODE>Integer.MAX_VALUE</CODE> (which is not such a large number after all).
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.1
 */
public final class ParallelBatchTaskExecutor {
  private static int _nextId = 0;
  private int _id;  // ParallelBatchTaskExecutor id
  private int _curId=0;
  private PBTEThread[] _threads;
  private boolean _isRunning;
  private int _batchSize = MsgPassingCoordinator.getMaxSize()/2;
  private MsgPassingCoordinator _mpcFwd=null;  // send msgs to thread-pool
  private MsgPassingCoordinator _mpcBak=null;  // recv msgs from thread-pool

	
  /**
   * public factory constructor, constructing a thread-pool of numthreads 
	 * threads.
   * @param numthreads int the number of threads in the thread-pool
	 * @return ParallelBatchTaskExecutor properly initialized
   * @throws ParallelException if numthreads &le; 0.
   */	
	public static ParallelBatchTaskExecutor 
				newParallelBatchTaskExecutor(int numthreads) throws ParallelException {
		ParallelBatchTaskExecutor ex = new ParallelBatchTaskExecutor(numthreads);
		ex.initialize();
		return ex;
	}

	
  /**
   * public factory constructor, constructing a thread-pool of numthreads 
	 * threads.
   * @param numthreads int the number of threads in the thread-pool
   * @param bsize int the batch size to be submitted each time to the threadpool
	 * @return ParallelBatchTaskExecutor properly initialized
   * @throws ParallelException if numthreads &le; 0.
   */	
	public static ParallelBatchTaskExecutor 
				newParallelBatchTaskExecutor(int numthreads, int bsize) 
								throws ParallelException {
		ParallelBatchTaskExecutor ex = new ParallelBatchTaskExecutor(numthreads, 
			                                                           bsize);
		ex.initialize();
		return ex;
	}

	
  /**
   * private constructor, constructing a thread-pool of numthreads threads.
   * @param numthreads int the number of threads in the thread-pool
   * @throws ParallelException if numthreads &le; 0.
   */
  private ParallelBatchTaskExecutor(int numthreads) throws ParallelException {
    if (numthreads<=0) 
			throw new ParallelException("constructor arg must be > 0");
    _id = getNextObjId();
    _threads = new PBTEThread[numthreads];
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
    //_batchSize = MsgPassingCoordinator.getMaxSize()/2; 
		// setting the _batchSize above, would undo any explicit specification done
		// by the 2-argument newParallelBatchTaskExecutor() method.
    // get refs to MsgPassingCoordinator's
    _mpcFwd = MsgPassingCoordinator.getInstance("ParallelBatchTaskExecutor"+
			                                          _id);
    _mpcBak = MsgPassingCoordinator.getInstance("ParallelBatchTaskExecutor_ack"+
			                                          _id); 		
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
  public synchronized int executeBatch(Collection tasks) 
		throws ParallelException {
    if (tasks==null) return 0;
    if (_isRunning==false)
      throw new ParallelException("thread-pool is not running");
    int runtasks = 0;
		int failedtasks=0;
    // submit the tasks in batches
    int batch_counter = 0;
    Iterator it = tasks.iterator();
    while (it.hasNext()) {
      Set ids = new HashSet();
      while (it.hasNext()) {
        Object task = it.next();
        int id = getNextId();
        ids.add(new Integer(id));
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
        Object task = _mpcBak.recvData(-1, id);
        if (task instanceof TaskObject && ((TaskObject) task).isDone() == false)
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
  public synchronized int getNumThreads() {
    if (_threads!=null)
      return _threads.length;
    else return 0;
  }


  int getObjId() { return _id; }
  private synchronized static int getNextObjId() { 
		if (++_nextId<0) _nextId = 0;  // overflow occurred, reset to zero
		return _nextId;
		// return ++_nextId; 
	}
  private synchronized int getNextId() {
		if (++_curId < 0) _curId = 0;  // overflow occurred, reset to zero
		return _curId;
		// return ++_curId; 
	}


	/**
	 * auxiliary inner-class, not part of the public API.
	 */
	class PoissonPill {
		// denotes end of computations for receiving thread
	}


	/**
	 * auxiliary inner-class for testing old shutdown() method only; not part of 
	 * the public API.
	 */
	class OldPill {
		// denotes end of computations for receiving thread
	}

}


/**
 * auxiliary class implementing the threads of the thread-pool of the class
 * <CODE>ParallelBatchTaskExecutor</CODE> class, not part of the public API.
 */
final class PBTEThread extends Thread {
	private static HashMap _ids = new HashMap();  // map<PBTE e, Integer curId>
	private ParallelBatchTaskExecutor _e;
	private boolean _doRun=true;

	
	/**
	 * sole constructor.
	 * @param e ParallelBatchTaskExecutor
	 */
	public PBTEThread(ParallelBatchTaskExecutor e) {
		synchronized (PBTEThread.class) {
			if (_ids.get(e)==null)
				_ids.put(e, new Integer(0));
		}
		_e = e;
	}


	/**
	 * while the thread is running, it waits to receive data from the "forward"
	 * channel "ParallelBatchTaskExecutor$pbteid$" where $pbteid$ is the unique id
	 * of the executor object that spawned this thread, and if the data received 
	 * is Runnable or TaskObject, it calls their <CODE>run()</CODE> method, else
	 * if the data is a <CODE>ParallelBatchTaskExecutor.PoissonPill</CODE>, the 
	 * method returns, and the thread halts. Any other kind of data, the thread
	 * simply prints a message that it can't execute the object received, and 
	 * continues with the loop, which sends the data object to the "back" channel
	 * "ParallelBatchTaskExecutor_ack$pbteid$".
	 */
	public void run() {
		final int pbteid = _e.getObjId();
		final MsgPassingCoordinator _mpcFwd = 
			MsgPassingCoordinator.getInstance("ParallelBatchTaskExecutor"+pbteid);
		final MsgPassingCoordinator _mpcBak = 
			MsgPassingCoordinator.getInstance("ParallelBatchTaskExecutor_ack"+pbteid);
		while (isRunning()) {
			// get the "sender" id: notice that it is not sufficient for each thread
			// to simply increment a local id variable, as this would cause every 
			// thread to wait to receive the same message sent, and starvation would
			// ensue: the first thread to receive the message would proceed, but all
			// the rest would get stuck waiting for a message that will never arrive.
			int id = getNextId(_e);
			try {
				// threads all use the same receiver id
				Object data = _mpcFwd.recvData(0, id);
				try {
					if (data instanceof TaskObject) ( (TaskObject) data).run();
					else if (data instanceof Runnable) ( (Runnable) data).run();
					else if (data instanceof ParallelBatchTaskExecutor.PoissonPill) 
						break;  // done
					else if (data instanceof ParallelBatchTaskExecutor.OldPill) 
						stopRunning();  // done
					else throw new ParallelException("data object cannot be run");
				}
				catch (Exception e) {
					e.printStackTrace();  // task threw an exception, ignore and continue
				}
				// send back the data object as acknowledgement on the "back-channel"
				// any results may be written in the data object itself
				_mpcBak.sendDataBlocking(id, -1, data);
			}
			catch (ParallelException e) {
				e.printStackTrace();  // no-op
			}
		}
	}

	
	synchronized void stopRunning() { _doRun=false; }

	
	private synchronized boolean isRunning() { return _doRun; }

	
	/**
	 * implements a synchronized counter for each executor e, starting from 0, and
	 * incrementing by 1 each time it is called.
	 * @param e ParallelBatchTaskExecutor
	 * @return int
	 */
	private static synchronized int getNextId(ParallelBatchTaskExecutor e) {
		// this method must start, and then increment numbers in the same way the
		// ParallelBatchTaskExecutor.getNextId() method does. 
		Integer curId = (Integer) _ids.get(e);
		int nextid = curId.intValue()+1;
		if (nextid<0) nextid = 0;  // overflow occurred, reset to zero 
		_ids.put(e, new Integer(nextid));
		return nextid;
	}
	
}
