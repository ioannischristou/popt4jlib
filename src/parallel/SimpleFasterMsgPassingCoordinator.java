package parallel;

import java.util.Hashtable;


/**
 * The SimpleFasterMsgPassingCoordinator class is yet another class that allows
 * sending and receiving (synchronous and asynchronous) data between threads.
 * It behaves as the more general MsgPassingCoordinator, except that senders
 * cannot choose which thread will receive the data they submit, and receivers
 * cannot choose from which thread they will receive data.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class SimpleFasterMsgPassingCoordinator {
  private static final int _maxSize=10000;
  //private Vector _data;  // Vector<RegisteredParcel>
  /**
   * maintains RegisteredParcel objects to be exchanged between threads
   */
  private BoundedBufferArray _data;
  private static SimpleFasterMsgPassingCoordinator _instance=null;
  private static Hashtable _instances=new Hashtable();  // map<String name, SFMPC instance>


  /**
   * private constructor in accordance with the Singleton Design Pattern
   */
  private SimpleFasterMsgPassingCoordinator() {
    //_data = new Vector();
    _data = new BoundedBufferArray(_maxSize);
  }


  /**
   * return the default SimpleFasterMsgPassingCoordinator object.
   * @return SimpleFasterMsgPassingCoordinator
   */
  public synchronized static SimpleFasterMsgPassingCoordinator getInstance() {
    if (_instance==null) {
      _instance = new SimpleFasterMsgPassingCoordinator();
    }
    return _instance;
  }


  /**
   * return the unique SimpleFasterMsgPassingCoordinator object associated
   * with a given name.
   * @return SimpleFasterMsgPassingCoordinator
   */
  public synchronized static SimpleFasterMsgPassingCoordinator getInstance(String name) {
    SimpleFasterMsgPassingCoordinator instance = (SimpleFasterMsgPassingCoordinator) _instances.get(name);
    if (instance==null) {
      instance = new SimpleFasterMsgPassingCoordinator();
      _instances.put(name, instance);
    }
    return instance;
  }


  /**
   * return the _maxSize data member
   * @return int
   */
  public static int getMaxSize() { return _maxSize; }


  /**
   * get the current number of tasks in the queue awaiting processing.
   * @return int
   */
  public synchronized int getNumTasksInQueue() {
    return _data.size();
  }


  /**
   * stores the data object to be consumed by any thread that invokes recvData()
   * method -regardless of which thread that is. The method returns immediately.
   * Note: senders "create" RegisteredParcel objects, receivers "release" them.
   * @param myid int the id of the thread calling this method
   * @param data Object
   * @throws ParallelException if there are more data than _maxSize in the queue
   */
  public synchronized void sendData(int myid, Object data)
      throws ParallelException {
    if (_data.size()>=_maxSize)
      throw new ParallelException("SimpleFasterMsgPassingCoordinator queue is full");
    // Pair p = new Pair(null, data);
    // RegisteredParcel p = new RegisteredParcel(new Integer(myid), null, data);
    RegisteredParcel p = RegisteredParcel.newInstance(myid, Integer.MAX_VALUE, data);
    _data.addElement(p);
    notifyAll();
  }


  /**
   * stores the data object to be consumed by any thread that invokes recvData()
   * method -regardless of which thread that is. The method will wait if the
   * queue is full until another thread consumes a datum and allows this thread
   * to write to the _data buffer.
   * Note: senders "create" RegisteredParcel objects, receivers "release" them.
   * @param myid int the id of the thread calling this method
   * @param data Object
   */
  public synchronized void sendDataBlocking(int myid, Object data) {
    /*
    if (utils.Debug.debug(popt4jlib.Constants.MPC)>0) {
      utils.Messenger.getInstance().msg("MsgPassingCoordinator.sendData(): _data.size()="+_data.size(),2);
    }
    */
    while (_data.size()>= _maxSize) {
      try {
        wait();
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    // Pair p = new Pair(null, data);
    // RegisteredParcel p = new RegisteredParcel(new Integer(myid), null, data);
    RegisteredParcel p = RegisteredParcel.newInstance(myid, Integer.MAX_VALUE, data);
    try {
      _data.addElement(p);
    }
    catch (ParallelException e) {
      // cannot get here
      e.printStackTrace();
    }
    notifyAll();
  }


  /**
   * retrieves the first data Object that has been stored via a call (from
   * another thread) to sendData(threadId, data). If no such
   * data exists, the calling thread will wait until another thread stores an
   * appropriate datum.
   * Note: senders "create" RegisteredParcel objects, receivers "release" them.
   * @param myid int unused
   * @return Object
   */
  public synchronized Object recvData(int myid) {
    while (true) {
      Object res = null;
      if (_data.size()>0) {
        try {
          RegisteredParcel p = (RegisteredParcel) _data.elementAt(0);  // pick the first datum
          res = p.getData();
          _data.remove();
          notifyAll();
          p.release();
          return res;
        }
        catch (ParallelException e) {
          // cannot get here
          e.printStackTrace();
        }
      }
      // else:
      try {
        wait();
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // recommended action
      }
    }
  }

}

