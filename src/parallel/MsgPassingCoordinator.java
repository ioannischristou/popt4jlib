package parallel;

import utils.PairObjDouble;
import java.util.List;
import java.util.HashMap;


/**
 * The MsgPassingCoordinator class is yet another class that allows
 * sending and receiving (synchronous and asynchronous) data between threads.
 * Threads can call the method sendData(thread-id, data) to asynchronously
 * store some data in a queue that only the thread with thread-id can retrieve
 * (in a FIFO fashion only). The data Objects that are stored are not copies
 * of the original objects, but rather references to them and so should not
 * be changed in any way after they are stored via a call to sendData... or
 * more appropriately, the sender thread should make sure it sends to the
 * receiver thread a copy of the data it intends to send. Unfortunately, the
 * latter strategy cannot be effectively enforced as Object.clone() is a
 * protected method, and it cannot be called on the parameter passed in the
 * send method from this code. Also, if we require that the objects to be sent
 * implement some interface (say, Clonable, as opposed to the Java built-in
 * (empty) Cloneable interface), then the objects that we are most likely
 * interested in sending/receiving, which are Java built-in data types including
 * arrays, will not be passable per se...
 * Notice that the ids used in the method signatures do not have to be actually
 * tied to some kind of "thread-id" associated with the calling threads, but
 * are instead simple designations for who can read a datum and who can send a
 * datum to where.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class MsgPassingCoordinator {
	/**
	 * the maximum size of the queue that holds messages for this coordinator.
	 * Can be set only prior to any normal use by a call to the static method
	 * <CODE>setMaxSize(num)</CODE>. Default is 10000.
	 */
  private static int _maxSize=10000;
  /**
   * maintains the RegisteredParcel objects to be exchanged between threads
   */
  private BoundedBufferArrayUnsynchronized _data;  // used to be 
	                                                 // Vector<RegisteredParcel>
	private boolean _selectiveReceiveOn;  // used to block receiver threads from
	                                      // reading data when a selective receive
	                                      // operation is under way
  private static MsgPassingCoordinator _instance=null;
  private static HashMap _instances=new HashMap();  // map<String name, 
	                                                  //     MPC instance>
	
	private final static utils.Messenger _mger = utils.Messenger.getInstance();

	
  /**
   * private constructor in accordance with the Singleton Design Pattern
   */
  private MsgPassingCoordinator() {
    _data = new BoundedBufferArrayUnsynchronized(_maxSize);
		_selectiveReceiveOn=false;  // not needed, as default
  }


  /**
   * return the default MsgPassingCoordinator object.
   * @return MsgPassingCoordinator
   */
  public synchronized static MsgPassingCoordinator getInstance() {
    if (_instance==null) {
      _instance = new MsgPassingCoordinator();
    }
    return _instance;
  }


  /**
   * return the unique MsgPassingCoordinator object associated with given name.
   * @return MsgPassingCoordinator
   */
  public synchronized static MsgPassingCoordinator getInstance(String name) {
    MsgPassingCoordinator instance = 
			(MsgPassingCoordinator) _instances.get(name);
    if (instance==null) {
      instance = new MsgPassingCoordinator();
      _instances.put(name, instance);
    }
    return instance;
  }


  /**
   * return the _maxSize data member.
   * @return int
   */
  public synchronized static int getMaxSize() { return _maxSize; }

	
	/**
	 * sets the size of the queue of messages. Must be called only prior to any 
	 * other call to the <CODE>getInstance()</CODE> methods.
	 * @param num int the new maximum size of the queue.
	 * @throws ParallelException if a call to the <CODE>getInstance()</CODE> 
	 * methods occurred before this call.
	 * @throws IllegalArgumentException if the argument is &le; 0. 
	 */
	public synchronized static void setMaxSize(int num) 
		throws ParallelException, IllegalArgumentException {
		if (_instance!=null || _instances.size()>0)
			throw new ParallelException(
							"MsgPassingCoordinator.setMaxSize(num): call is only allowed "+
							"before any call to getInstance([]) methods on this class");
		if (num<=0) throw new IllegalArgumentException("argument must be > 0");
		_maxSize = num;
	}
	

  /**
   * get the current number of tasks in the queue awaiting processing.
   * @return int
   */
  public synchronized int getNumTasksInQueue() {
    return _data.size();
  }


  /**
   * stores the data object to be consumed by any thread that invokes recvData()
   * method -regardless of which thread that is- as long as the thread is not
   * interested in who the sender is, or as long as it's interested in msgs from
   * thread with id myid. The method returns immediately.
   * @param myid int the id of the thread calling this method
   * @param data Object
   * @throws ParallelException if there are more data than _maxSize in the queue
   */
  public synchronized void sendData(int myid, Object data)
    throws ParallelException {
    if (_data.size()>=_maxSize)
      throw new ParallelException("MsgPassingCoordinator queue is full");
    // Pair p = new Pair(null, data);
    RegisteredParcel p = new RegisteredParcel(myid, Integer.MAX_VALUE, data);
		// idiom below cannot be safely used. See RegisteredParcelPool doc.
    // RegisteredParcel p = RegisteredParcel.newInstance(myid,Integer.MAX_VALUE, 
		//                                                   data);
    _data.addElement(p);
    notifyAll();
  }


  /**
   * stores the data object to be consumed by any thread that invokes recvData()
   * method -regardless of which thread that is- as long as the thread is not
   * interested in who the sender is, or as long as it's interested in msgs from
   * thread with id myid. The method will wait if the queue is full until
   * another thread consumes a datum and allows this thread to write to the
   * _data buffer.
   * @param myid int the id of the thread calling this method
   * @param data Object
   */
  public synchronized void sendDataBlocking(int myid, Object data) {
    /*
    if (utils.Debug.debug(popt4jlib.Constants.MPC)>0) {
      utils.Messenger.getInstance().msg(
		    "MsgPassingCoordinator.sendDataBlocking(): _data.size()="+
		    _data.size(),2);
    }
    */
    while (_data.size()>= _maxSize) {
      try {
				_mger.msg(
					"WARNING: MsgPassingCoordinator.sendDataBlocking(myid,data): "+
					"data queue is full.",0);
        wait();
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // no interruptions allowed, 
				                                     // so e is not re-thrown
      }
    }
    // Pair p = new Pair(null, data);
    RegisteredParcel p = new RegisteredParcel(myid, Integer.MAX_VALUE, data);
		// idiom below cannot be safely used. See RegisteredParcelPool documentation.
    // RegisteredParcel p = RegisteredParcel.newInstance(myid, Integer.MAX_VALUE, data);
    _data.addElement(p);
    notifyAll();
  }


  /**
   * stores the data object to be consumed only by the first thread that invokes
   * the method recvData(threadId)- as long as the thread is not interested in
   * who the sender is, or as long as it's interested in msgs from thread with
   * id myid.
   * The method returns immediately.
   * @param myid int my thread's id
   * @param threadId int the id of the thread that is the recipient
   * @param data Object
   * @throws ParallelException if there are more data than _maxSize in the queue
   * or if the thread tries to send smth to itself
   */
  public synchronized void sendData(int myid, int threadId, Object data)
    throws ParallelException {
    if (myid==threadId) throw new ParallelException("cannot send to self");
    if (_data.size()>=_maxSize)
      throw new ParallelException("MsgPassingCoordinator queue is full");
    // Pair p = new Pair(new Integer(threadId), data);
    RegisteredParcel p = new RegisteredParcel(myid, threadId, data);
		// idiom below cannot be safely used. See RegisteredParcelPool doc.
    // RegisteredParcel p = RegisteredParcel.newInstance(myid, threadId, data);
    _data.addElement(p);
    notifyAll();
  }


  /**
   * stores the data object to be consumed only by the first thread that invokes
   * the method recvData(threadId)- as long as the thread is not interested in
   * who the sender is, or as long as it's interested in msgs from thread with
   * id myid.
   * The method will wait if the queue is full until another thread consumes a
   * datum and allows this thread to write to the _data buffer.
   * @param threadId int
   * @param data Object
   * @throws ParallelException if the thread tries to send data to itself
   */
  public synchronized void sendDataBlocking(int myid, int threadId, Object data)
    throws ParallelException {
    if (myid==threadId) throw new ParallelException("cannot send to self");
    while (_data.size()>= _maxSize) {
      try {
				_mger.msg(
					"WARNING: MsgPassingCoordinator.sendDataBlocking(myid,tid,data): "+
					"data queue is full.",0);
        wait();
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // no interruptions allowed, 
				                                     // so e is not re-thrown
      }
    }
    // Pair p = new Pair(new Integer(threadId), data);
    RegisteredParcel p = new RegisteredParcel(myid, threadId, data);
		// idiom below cannot be safely used. See RegisteredParcelPool doc.
    //RegisteredParcel p = RegisteredParcel.newInstance(myid, threadId, data);
    _data.addElement(p);
    notifyAll();
  }


  /**
   * retrieves the first data Object that has been stored via a call (from
   * another thread) to sendData(data) or sendData(threadId, data). If no such
   * data exists, the calling thread will wait until another thread stores an
   * appropriate datum. This implementation will invoke wait() if a selective
	 * receive operation is currently under way by another thread, and will
	 * be notified when the selective receive finishes.
   * @param myid int
   * @return Object
   */
  public synchronized Object recvData(int myid) {
		while (_selectiveReceiveOn) {
			try {
				wait();
			}
			catch (InterruptedException e) {  // do not allow interruptions!
				Thread.currentThread().interrupt();  // recommended, but of no use
			}
		}
    while (true) {
      Object res = null;
      for (int i=0; i<_data.size(); i++) {
        RegisteredParcel p = (RegisteredParcel) _data.elementAt(i);
        // Integer toid = p.getToId();
        int toid = p.getToId();
        if (toid==Integer.MAX_VALUE || toid==myid) { 
          res = p.getData();
					_data.remove(i);
          notifyAll();
					// idiom below cannot be safely used. See RegisteredParcelPool doc.
          // p.release();
          return res;
        }
      }
      try {
        wait();
      }
      catch (InterruptedException e) {  // no interruptions allowed
        Thread.currentThread().interrupt();  // recommended action
      }
    }
  }


  /**
   * the method has the same semantics as the recvData(tid) method except that
   * it returns immediately with null if no appropriate data exist at the time
   * of the call. There is one exception: this implementation will invoke wait()
	 * first if a selective receive operation is currently under way by another
	 * thread (without checking if any data exist or not), and will
	 * be notified when the selective receive finishes.
   * @param myid int the threadId
   * @return Object
   */
  public synchronized Object recvDataIfAnyExist(int myid) {
		while (_selectiveReceiveOn) {
			try {
				wait();
			}
			catch (InterruptedException e) {  // do not allow interruptions!
				Thread.currentThread().interrupt();  // recommended, but of no use
			}
		}
    Object res = null;
    for (int i=0; i<_data.size(); i++) {
      RegisteredParcel p = (RegisteredParcel) _data.elementAt(i);
      // Integer id = p.getToId();
      int id = p.getToId();
      if (id==Integer.MAX_VALUE || id==myid) { 
        res = p.getData();
        _data.remove(i);
        notifyAll();
				// idiom below cannot be safely used. See RegisteredParcelPool doc.
        // p.release();
        break;
      }
    }
    return res;
  }


  /**
   * retrieves the first data Object that has been stored via a call to
   * sendData(fromid, data) or sendData(fromid, threadId, data). In other words,
   * we wait for a msg from a particular thread.
   * If no such data exists, the calling thread will wait until the right thread
   * stores an appropriate datum.
	 * This implementation will invoke wait() first if a selective receive
	 * operation is currently under way by another thread, and will
	 * be notified when the selective receive finishes.
   * @param myid int
   * @param fromid int the sender's id
   * @return Object
   * @throws ParallelException if myid==fromid
   */
  public synchronized Object recvData(int myid, int fromid)
      throws ParallelException {
    if (myid==fromid) throw new ParallelException("cannot receive from self");
    /*
    if (utils.Debug.debug(popt4jlib.Constants.MPC)>0) {
      utils.Messenger.getInstance().msg(
		    "MsgPassingCoordinator.recvData(): _data.size()="+_data.size(),2);
    }
    */
		while (_selectiveReceiveOn) {
			try {
				wait();
			}
			catch (InterruptedException e) {  // do not allow interruptions!
				Thread.currentThread().interrupt();  // recommended, but of no use
			}
		}
    while (true) {
      Object res = null;
      for (int i=0; i<_data.size(); i++) {
        RegisteredParcel p = (RegisteredParcel) _data.elementAt(i);
        // Integer toid = (Integer) p.getToId();
        int toid = p.getToId();
        // Integer fId = (Integer) p.getFromId();
        int fId = p.getFromId();
        if (fId==fromid && (toid==Integer.MAX_VALUE || toid==myid)) {  
          res = p.getData();
          _data.remove(i);
          notifyAll();
					// idiom below cannot be safely used. See RegisteredParcelPool doc.
          // p.release();
          return res;
        }
      }
      try {
        wait();
      }
      catch (InterruptedException e) {  // don't allow interruptions
        Thread.currentThread().interrupt();  // recommended action
      }
    }
  }
	
	
  /**
   * retrieves the first data Object that has been stored via a call to
   * sendData(fromid, data) or sendData(fromid, threadId, data), unless the 
	 * sender specified as their id a negative number, in which case the threadId
	 * value is ignored. In other words, we wait for a msg from a particular 
	 * thread (fromid) unless myid is negative, and also the sender of the object
	 * sent this negative number. This feature is only used to implement the 
	 * <CODE>parallel.distributed.PDBatchTaskExecutor.
	 *         runTaskOnAllThreads(TaskObject task)</CODE>
	 * method that requires guarantee that a task will be executed on all threads
	 * in the executor's thread-pool.
   * If no such data exists, the calling thread will wait until the right thread
   * stores an appropriate datum.
	 * This implementation will invoke wait() first if a selective receive
	 * operation is currently under way by another thread, and will
	 * be notified when the selective receive finishes.
   * @param myid int
   * @param fromid int the sender's id
   * @return Object
   * @throws ParallelException if myid==fromid
   */
  public synchronized Object recvDataIgnoringFromIdOnNegativeMyId(int myid, 
		                                                              int fromid)
      throws ParallelException {
    if (myid==fromid) throw new ParallelException("cannot receive from self");
    /*
    if (utils.Debug.debug(popt4jlib.Constants.MPC)>0) {
      utils.Messenger.getInstance().msg(
		    "MsgPassingCoordinator.recvData(): _data.size()="+_data.size(),2);
    }
    */
		while (_selectiveReceiveOn) {
			try {
				wait();
			}
			catch (InterruptedException e) {  // do not allow interruptions!
				Thread.currentThread().interrupt();  // recommended, but of no use
			}
		}
    while (true) {
      Object res = null;
      for (int i=0; i<_data.size(); i++) {
        RegisteredParcel p = (RegisteredParcel) _data.elementAt(i);
        // Integer toid = (Integer) p.getToId();
        int toid = p.getToId();
        // Integer fId = (Integer) p.getFromId();
        int fId = p.getFromId();
        if ((fId==fromid && (toid==Integer.MAX_VALUE || toid==myid)) ||
					  (toid<0 && myid==toid)) {  // 2nd case: when toid<0, ignore fromid.  
          res = p.getData();
          _data.remove(i);
          notifyAll();
					// idiom below cannot be safely used. See RegisteredParcelPool doc.
          // p.release();
          return res;
        }
      }
      try {
        wait();
      }
      catch (InterruptedException e) {  // don't allow interruptions
        Thread.currentThread().interrupt();  // recommended action
      }
    }
  }
	
	
  /**
   * the method has the same semantics as the recvData(tid) method except that
   * it returns immediately with null if no appropriate data exist at the time
   * of the call. There is one exception: this implementation will invoke wait()
	 * first if a selective receive operation is currently under way by another
	 * thread (without checking if any data exist or not), and will
	 * be notified when the selective receive finishes.
   * @param myid int
   * @param fromid int the msg must be from this sender
   * @return Object
   * @throws ParallelException if myid==fromid
   */
  public synchronized Object recvDataIfAnyExist(int myid, int fromid)
      throws ParallelException {
    if (myid==fromid) throw new ParallelException("cannot receive from self");
		while (_selectiveReceiveOn) {
			try {
				wait();
			}
			catch (InterruptedException e) {  // do not allow interruptions!
				Thread.currentThread().interrupt();  // recommended, but of no use
			}
		}
    Object res = null;
    for (int i=0; i<_data.size(); i++) {
      RegisteredParcel p = (RegisteredParcel) _data.elementAt(i);
      // Integer id = p.getToId();
      int id = p.getToId();
      // Integer fId = p.getFromId();
      int fId = p.getFromId();
      if (fId==fromid && (id==Integer.MAX_VALUE || id==myid)) {
        res = p.getData();
        _data.remove(i);
        notifyAll();
				// idiom below cannot be safely used. See RegisteredParcelPool doc.
        // p.release();
        break;
      }
    }
    return res;
  }


	/**
	 * perform an essentially selective-receive (interruptible) blocking operation.
	 * @param myid int
	 * @param fromids List // List&lt;Integer&gt;
	 * @return PairObjDouble a Pair object whose "object" part is the received
	 * data and the "Double" part is the integer id of the fromids list on which
	 * the data was received.
	 * @throws ParallelException if the first arg is contained in the second arg.
	 * @throws InterruptedException if the operation is interrupted by some other
	 * thread calling interrupt() on the thread executing the selRecvData() while
	 * it's on wait-mode.
	 * @throws IllegalArgumentException if fromids is null or empty
	 */
	public synchronized PairObjDouble selRecvData(int myid, List fromids)
			throws ParallelException, InterruptedException, IllegalArgumentException {
		try {
			_selectiveReceiveOn = true;
			// 1. check args, and if myid is contained in the fromids list,
			// in which case, throw a ParallelException
			if (fromids==null || fromids.size()==0)
				throw new IllegalArgumentException("fromids list is null or empty");
			if (fromids.contains(new Integer(myid)))
				throw new ParallelException("cannot receive from self");
			// 2. now check the data
	    while (true) {
		    Object res = null;
			  for (int i=0; i<_data.size(); i++) {
				  RegisteredParcel p = (RegisteredParcel) _data.elementAt(i);
					// Integer toid = (Integer) p.getToId();
					int toid = p.getToId();
					// Integer fId = (Integer) p.getFromId();
					int fId = p.getFromId();
					if (fromids.contains(new Integer(fId)) && 
						  (toid==Integer.MAX_VALUE || toid==myid)) {
						// found it
						res = p.getData();
						_data.remove(i);
						notifyAll();
						// idiom below cannot be safely used. See RegisteredParcelPool doc.
						// p.release();
						return new PairObjDouble(res, fId);
					}
				}
				// 3. oops, not done, go on and wait for some data, unless interrupted
				wait();
			}
		}
		finally {
			_selectiveReceiveOn = false;
			notifyAll();
		}
	}
}

