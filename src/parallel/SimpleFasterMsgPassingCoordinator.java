package parallel;

import java.util.Hashtable;


/**
 * The SimpleFasterMsgPassingCoordinator class is yet another class that allows
 * sending and receiving (synchronous and asynchronous) data between threads.
 * It behaves as the more general MsgPassingCoordinator, except that senders
 * cannot choose which thread will receive the data they submit, and receivers
 * cannot choose from which thread they will receive data. 
 * However, this restriction can effectively
 * be bypassed by sending <CODE>ThreadSpecificTaskObject</CODE> objects (see the
 * documentation for that interface for the exact semantics of the interface 
 * method <CODE>getThreadIdToRunOn()</CODE>.)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class SimpleFasterMsgPassingCoordinator {
	/**
	 * compile-time constant, forces class to act as a synchronizer between 
	 * producers and consumers
	 */
	private static final int _maxSize=10000;
  /**
   * maintains RegisteredParcel objects to be exchanged between threads
   */
  private BoundedBufferArrayUnsynchronized _data;  // Vector<RegisteredParcel>
  private static SimpleFasterMsgPassingCoordinator _instance=null;
  private static Hashtable _instances=new Hashtable();  // map<String name, SFMPC instance>


  /**
   * private constructor in accordance with the Singleton Design Pattern
   */
  private SimpleFasterMsgPassingCoordinator() {
    _data = new BoundedBufferArrayUnsynchronized(_maxSize);
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
   * return the constant _maxSize data member
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
   * @param myid int the id of the thread calling this method
   * @param data Object
   * @throws ParallelException if there are more data than _maxSize in the queue
   */
  public synchronized void sendData(int myid, Object data)
      throws ParallelException {
    if (_data.size()>=_maxSize)
      throw new ParallelException("SimpleFasterMsgPassingCoordinator queue is full");
    RegisteredParcel p = new RegisteredParcel(myid, Integer.MAX_VALUE, data);
		// commented code below is unsafe: see RegisteredParcelPool class documentation
		// and the RegisteredParcelPoolFailTest class.
    // RegisteredParcel p = RegisteredParcel.newInstance(myid, Integer.MAX_VALUE, data);
    _data.addElement(p);
    notifyAll();
  }


  /**
   * stores the data object to be consumed by any thread that invokes recvData()
   * method -regardless of which thread that is. The method will wait if the
   * queue is full until another thread consumes a datum and allows this thread
   * to write to the _data buffer.
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
    RegisteredParcel p = new RegisteredParcel(myid, Integer.MAX_VALUE, data);
		// commented code below is unsafe: see RegisteredParcelPool class documentation
		// and the RegisteredParcelPoolFailTest class.
    // RegisteredParcel p = RegisteredParcel.newInstance(myid, Integer.MAX_VALUE, data);
    try {
      _data.addElement(p);
    }
    catch (IllegalStateException e) {  // cannot get here
      e.printStackTrace();
    }
    notifyAll();
  }


  /**
   * retrieves the first data Object that has been stored via a call (from
   * another thread) to sendData(threadId, data). If no such data exists, the 
	 * calling thread will wait until another thread stores an appropriate datum.
   * @param myid int 
   * @return Object
   */
  public synchronized Object recvData(int myid) {
    while (true) {
      Object res = null;
			final int dsz = _data.size();
      if (dsz>0) {
        try {
          RegisteredParcel p = (RegisteredParcel) _data.elementAt(0);  // pick the first datum
          res = p.getData();
					int i=0;
					boolean found=true;
					while (res instanceof ThreadSpecificTaskObject) {
						ThreadSpecificTaskObject tsto = (ThreadSpecificTaskObject) res;
						final int tstoid = tsto.getThreadIdToRunOn();
						if (tstoid!=myid && tstoid!=Integer.MAX_VALUE && (sameSign(tstoid,myid) || tstoid==-myid)) {
							found = false;
							if (i<dsz-1) {
								p = (RegisteredParcel) _data.elementAt(++i);
								res = p.getData();
								found = true;  // reset to true
							} else break;
						} else {
							found = true;
							break;
						}
					}
					if (found) {
						_data.remove(i);  // used to be _data.remove();
						notifyAll();
						// commented code below is part of an unsafe idiom: 
						// see RegisteredParcelPool class documentation
						// and the RegisteredParcelPoolFailTest class.
						// p.release();
						return res;
					}
        }
        catch (IndexOutOfBoundsException e) {  // cannot get here
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

	
	/**
   * the method has the same semantics as the recvData(myid) method except that
   * it returns immediately with null if no appropriate data exist at the time
   * of the call. 
   * @param myid int 
   * @return Object
	 */
  public synchronized Object recvDataIfAnyExist(int myid) {
    Object res = null;
    final int dsz = _data.size();
    if (dsz>0) {
	    try {
	      RegisteredParcel p = (RegisteredParcel) _data.elementAt(0);  // pick the first datum
        res = p.getData();
				int i=0;
				boolean found=true;
				while (res instanceof ThreadSpecificTaskObject) {
					ThreadSpecificTaskObject tsto = (ThreadSpecificTaskObject) res;
					final int tstoid = tsto.getThreadIdToRunOn();
					if (tstoid!=myid && tstoid!=Integer.MAX_VALUE  && (sameSign(tstoid,myid) || tstoid==-myid)) {
						found = false;
						if (i<dsz-1) {
							p = (RegisteredParcel) _data.elementAt(++i);
							res = p.getData();
							found = true;  // reset to true
						} else break;
					} else {
						found = true;
						break;
					}
				}
				if (found) {
					_data.remove(i);  // used to be _data.remove();
					notifyAll();
					// commented code below is part of an unsafe idiom: 
					// see RegisteredParcelPool class documentation
					// and the RegisteredParcelPoolFailTest class.
					// p.release();
					return res;
				}
      }
      catch (IndexOutOfBoundsException e) {
        // cannot get here
        e.printStackTrace();
      }
    }
		return null;
  }

	
	private static boolean sameSign(int x, int y) {
		return x>=0 ? y>=0 : y<=0;
	}

}

