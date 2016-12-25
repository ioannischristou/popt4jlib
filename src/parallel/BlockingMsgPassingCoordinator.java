package parallel;

import java.util.Vector;
import java.util.HashMap;


/**
 * The BlockingMsgPassingCoordinator class is yet another Singleton class that
 * implements the rendevouz mechanism in parallel programming.
 * Threads can call the method sendData(myid, t-id, data) to store some data in
 * a queue that only the thread with thread-id &quot;t-id&quot; can retrieve (in 
 * a FIFO fashion only: threads receive the messages sent to them in the same
 * order they entered the queue), and waits until the datum is received by the 
 * receiver thread. The data Objects that are stored are not copies
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
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class BlockingMsgPassingCoordinator {
  /**
	 * the maximum size of the <CODE>_data</CODE> array.
	 */
	private static final int _maxSize=10000;
	/**
	 * the bounded array holding <CODE>RegisteredParcel</CODE> objects that
	 * represent the messages passed around together with the data they contain.
	 */
  private BoundedBufferArrayUnsynchronized _data; 
  private static BlockingMsgPassingCoordinator _instance=null;
  private static HashMap _instances=new HashMap();  // map<String name, BMPC instance>


  /**
   * private constructor, in accordance with the Singleton Design Pattern
   */
  private BlockingMsgPassingCoordinator() {
    _data = new BoundedBufferArrayUnsynchronized(_maxSize);
  }


  /**
   * return the unique BlockingMsgPassingCoordinator object that can exist in
   * a JVM.
   * @return BlockingMsgPassingCoordinator
   */
  public synchronized static BlockingMsgPassingCoordinator getInstance() {
    if (_instance==null) {
      _instance = new BlockingMsgPassingCoordinator();
    }
    return _instance;
  }


  /**
   * return the unique BlockingMsgPassingCoordinator object associated
   * with a given name.
   * @return BlockingMsgPassingCoordinator
   */
  public synchronized static BlockingMsgPassingCoordinator getInstance(String name) {
    BlockingMsgPassingCoordinator instance = (BlockingMsgPassingCoordinator)
        _instances.get(name);
    if (instance==null) {
      instance = new BlockingMsgPassingCoordinator();
      _instances.put(name, instance);
    }
    return instance;
  }


  /**
   * stores the data object to be consumed by any thread that invokes recvData()
   * method -regardless of which thread that is. The method waits until the
   * data is retrieved.
   * @param myid int the sender's id
   * @param data Object
   * @throws ParallelException if there are more data than _maxSize in the queue
   * at the time when the method is invoked. A necessary (but not sufficient)
   * condition for this is that the total number of threads in the system
   * sending/receiving messages is more than _maxSize.
   */
  public synchronized void sendData(int myid, Object data) throws ParallelException {
    if (_data.size()>=_maxSize)
      throw new ParallelException("MsgPassingCoordinator queue is full");
    // Pair p = new Pair(null, data);
    RegisteredParcel p = new RegisteredParcel(myid, Integer.MAX_VALUE, data);
		// idiom below cannot be safely used. See RegisteredParcelPool documentation.
		// RegisteredParcel p = RegisteredParcel.newInstance(myid, Integer.MAX_VALUE, data);
    _data.addElement(p);
    notifyAll();
    // wait until item is retrieved
    while (!isConsumed(p)) {
      try {
        wait();
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // recommended action
      }
    }
  }


  /**
   * stores the data object to be consumed only by the first thread that invokes
   * the method recvData(threadId). The method will wait until the data is
   * received.
   * @param myid int
   * @param threadId int
   * @param data Object
   * @throws ParallelException if there are more data than _maxSize in the queue
   * at the time when the method is invoked, or if myid==threadId
   */
  public synchronized void sendData(int myid, int threadId, Object data)
      throws ParallelException {
    if (myid==threadId)
      throw new ParallelException("sender and receiver cannot be the same");
    if (_data.size()>=_maxSize)
      throw new ParallelException("BlockingMsgPassingCoordinator queue is full");
    // Pair p = new Pair(new Integer(threadId), data);
    RegisteredParcel p = new RegisteredParcel(myid, threadId, data);
		// idiom below cannot be safely used. See RegisteredParcelPool documentation.
    // RegisteredParcel p = RegisteredParcel.newInstance(myid, threadId, data);
    _data.addElement(p);
    notifyAll();
    // wait until item is retrieved
    while (!isConsumed(p)) {
      try {
        wait();
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // recommended action
      }
    }
  }


  /**
   * retrieves the first data Object that has been stored via a call (from
   * another thread) to sendData(sendid,data) or sendData(sendid,recvId,data).
   * If no such data exists the calling thread will wait until the right thread
   * stores an appropriate datum.
   * @param myId int
   * @return Object
   */
  public synchronized Object recvData(int myId) {
    while (true) {
      Object res = null;
      for (int i=0; i<_data.size(); i++) {
        RegisteredParcel p = (RegisteredParcel) _data.elementAt(i);
        // Integer id = (Integer) p.getToId();
        int id = p.getToId();
        if (id==Integer.MAX_VALUE || id==myId) {  // id==null || id.intValue()==myId
          res = p.getData();
          _data.remove(i);
          notifyAll();
					// idiom below cannot be safely used. See RegisteredParcelPool documentation.
          // p.release();  // release the object
          return res;
        }
      }
      try {
        wait();
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // recommended action
      }
    }
  }


  /**
   * same as recvData(myid) but will only retrieve a datum from the specified
   * sender thread (whose id is specified in the 2nd argument).
   * @param myId int
   * @param fromId int
   * @return Object
   * @throws ParallelException if myId==fromId
   */
  public synchronized Object recvData(int myId, int fromId)
      throws ParallelException {
    if (myId==fromId) throw new ParallelException("cannot receive from self");
    while (true) {
      Object res = null;
      for (int i=0; i<_data.size(); i++) {
        RegisteredParcel p = (RegisteredParcel) _data.elementAt(i);
        // Integer id = p.getToId();
        int id = p.getToId();
        // Integer fId = p.getFromId();
        int fId = p.getFromId();
        if (fId==fromId && (id==Integer.MAX_VALUE || id==myId)) {  // fId.intValue()==fromId && (id==null || id.intValue()==myId)
          res = p.getData();
          _data.remove(i);
          notifyAll();
					// idiom below cannot be safely used. See RegisteredParcelPool documentation.
          // p.release();  // release the object
          return res;
        }
      }
      try {
        wait();
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // recommended action
      }
    }
  }


  private boolean isConsumed(RegisteredParcel p) {
    final int datasz = _data.size();
    for (int i=datasz-1; i>=0; i--) {
      if (_data.elementAt(i)==p) return false;
    }
    return true;
  }

}

