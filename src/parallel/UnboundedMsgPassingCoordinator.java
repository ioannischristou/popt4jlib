package parallel;

import java.util.HashMap;
import java.util.List;
import utils.PairObjDouble;

/**
 * Exactly the same as <CODE>MsgPassingCoordinator</CODE> except 
 * that its queue is unbounded, and therefore cannot throw 
 * <CODE>ParallelException</CODE> in cases where having a full queue would 
 * throw. The class is useful when having a (short enough) bounded queue may 
 * result in the process hanging (due to starvation), and therefore, very large
 * buffers would otherwise need to be allocated up front (see for example the 
 * <CODE>popt4jlib.GA.DGA</CODE> class, where in order to have determinism in 
 * the order in which incumbents are set in the main class, the msg-passing
 * mechanism is used).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class UnboundedMsgPassingCoordinator {

  private UnboundedBufferArrayUnsynchronized _data;
	private boolean _selectiveReceiveOn;  // used to block receiver threads from
	                                      // reading data when a selective receive
	                                      // operation is under way
  private static UnboundedMsgPassingCoordinator _instance=null;
  private static HashMap _instances=new HashMap();  // map<String name, 
	                                                  //     UnboundedMPC inst>


  /**
   * private constructor in accordance with the Singleton Design Pattern
   */
  private UnboundedMsgPassingCoordinator() {
    _data = new UnboundedBufferArrayUnsynchronized(1024);  // init. size
		_selectiveReceiveOn=false;  // not needed, as default
  }


  /**
   * return the default UnboundedMsgPassingCoordinator object.
   * @return UnboundedMsgPassingCoordinator
   */
  public synchronized static UnboundedMsgPassingCoordinator getInstance() {
    if (_instance==null) {
      _instance = new UnboundedMsgPassingCoordinator();
    }
    return _instance;
  }


  /**
   * return the unique UnboundedMsgPassingCoordinator object associated
   * with a given name.
   * @return UnboundedMsgPassingCoordinator
   */
  public synchronized static 
	    UnboundedMsgPassingCoordinator getInstance(String name) {
    UnboundedMsgPassingCoordinator instance = 
			(UnboundedMsgPassingCoordinator) _instances.get(name);
    if (instance==null) {
      instance = new UnboundedMsgPassingCoordinator();
      _instances.put(name, instance);
    }
    return instance;
  }


  /**
   * return the _maxSize data member
   * @return int
   */
  public synchronized int getCurrentMaxSize() { 
		return _data.getCurrentMaxSize(); 
	}

	
  /**
   * get the current number of tasks in the queue awaiting processing.
   * @return int
   */
  public synchronized int getNumTasksInQueue() {
    return _data.size();
  }


  /**
   * stores the data object to be consumed by any thread that invokes 
	 * <CODE>recvData()</CODE> method -regardless of which thread that is- as long 
	 * as the thread is not interested in who the sender is, or as long as it's 
	 * interested in msgs from thread with id myid. The method returns immediately.
   * @param myid int the id of the thread calling this method
   * @param data Object
   */
  public synchronized void sendData(int myid, Object data) {
    RegisteredParcel p = new RegisteredParcel(myid, Integer.MAX_VALUE, data);
    _data.addElement(p);
    notifyAll();
  }


  /**
   * stores the data object to be consumed only by the first thread that invokes
   * the method <CODE>recvData(threadId)</CODE>- as long as the thread is not 
	 * interested in who the sender is, or as long as it's interested in msgs from 
	 * thread with id myid.
   * The method returns immediately.
   * @param myid int my thread's id
   * @param threadId int the id of the thread that is the recipient
   * @param data Object
	 * @throws ParallelException if myid==threadId
   */
  public synchronized void sendData(int myid, int threadId, Object data)
		throws ParallelException {
    if (myid==threadId) throw new ParallelException("cannot send to self");
    RegisteredParcel p = new RegisteredParcel(myid, threadId, data);
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
          // toid==null || toid.intValue()==myid
          res = p.getData();
          _data.remove(i);
          // notifyAll();
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
   * the method has the same semantics as the <CODE>recvData(tid)</CODE> method 
	 * except that it returns immediately with null if no appropriate data exist 
	 * at the time of the call. There is one exception: this implementation will 
	 * invoke <CODE>wait()</CODE> first if a selective receive operation is 
	 * currently under way by another thread (without checking if any data exist 
	 * or not), and will be notified when the selective receive finishes.
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
        // id==null || id.intValue()==myid
        res = p.getData();
        _data.remove(i);
        // notifyAll();
        break;
      }
    }
    return res;
  }


  /**
   * retrieves the first data Object that has been stored via a call to
   * <CODE>sendData(fromid, data)</CODE> or 
	 * <CODE>sendData(fromid, threadId, data)</CODE>. In other words, we wait for 
	 * a msg from a particular thread. If no such data exists, the calling thread 
	 * will wait until the right thread stores an appropriate datum.
	 * This implementation will invoke <CODE>wait()</CODE> first if a selective 
	 * receive operation is currently under way by another thread, and will
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
      utils.Messenger.getInstance().msg("MsgPassingCoordinator.recvData(): _data.size()="+_data.size(),2);
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
          // fId.intValue()==fromid && (toid==null || toid.intValue()==myid)
          res = p.getData();
          _data.remove(i);
          // notifyAll();
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
   * the method has the same semantics as the <CODE>recvData(tid)</CODE> method 
	 * except that it returns immediately with null if no appropriate data exist 
	 * at the time of the call. There is one exception: this implementation will 
	 * invoke <CODE>wait()</CODE> first if a selective receive operation is 
	 * currently under way by another thread (without checking if any data exist 
	 * or not), and will be notified when the selective receive finishes.
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
        // fId.intValue()==fromid && (id==null || id.intValue()==myid)
        res = p.getData();
        _data.remove(i);
        // notifyAll();
        break;
      }
    }
    return res;
  }


	/**
	 * perform essentially a selective-receive (interruptible) blocking operation.
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
						// notifyAll();
						return new PairObjDouble(res, fId);
					}
				}
				// 3. oops, not done, go on and wait for some data, 
				// unless we're interrupted
				wait();
			}
		}
		finally {
			_selectiveReceiveOn = false;
			notifyAll();
		}
	}

}
