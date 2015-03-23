package parallel;

import java.util.Hashtable;


/**
 * Exactly the same as <CODE>SimpleFasterMsgPassingCoordinator</CODE> except 
 * that its queue is unbounded, and therefore cannot throw 
 * <CODE>ParallelException</CODE> in cases where having a full queue would 
 * throw.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class UnboundedSimpleFasterMsgPassingCoordinator {
  /**
   * maintains RegisteredParcel objects to be exchanged between threads
   */
  private UnboundedBufferArrayUnsynchronized _data;
  private static UnboundedSimpleFasterMsgPassingCoordinator _instance=null;
  private static Hashtable _instances=new Hashtable();  // map<String name, USFMPC instance>


  /**
   * private constructor in accordance with the Singleton Design Pattern
   */
  private UnboundedSimpleFasterMsgPassingCoordinator() {
    _data = new UnboundedBufferArrayUnsynchronized(1024);  // arg. is init. size
  }


  /**
   * return the default UnboundedSimpleFasterMsgPassingCoordinator object.
   * @return UnboundedSimpleFasterMsgPassingCoordinator
   */
  public synchronized static UnboundedSimpleFasterMsgPassingCoordinator getInstance() {
    if (_instance==null) {
      _instance = new UnboundedSimpleFasterMsgPassingCoordinator();
    }
    return _instance;
  }


  /**
   * return the unique UnboundedSimpleFasterMsgPassingCoordinator object associated
   * with a given name.
   * @return UnboundedSimpleFasterMsgPassingCoordinator
   */
  public synchronized static UnboundedSimpleFasterMsgPassingCoordinator getInstance(String name) {
    UnboundedSimpleFasterMsgPassingCoordinator instance = 
						(UnboundedSimpleFasterMsgPassingCoordinator) _instances.get(name);
    if (instance==null) {
      instance = new UnboundedSimpleFasterMsgPassingCoordinator();
      _instances.put(name, instance);
    }
    return instance;
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
   * method -regardless of which thread that is. The method returns immediately.
   * @param myid int the id of the thread calling this method
   * @param data Object
   */
  public synchronized void sendData(int myid, Object data) {
    RegisteredParcel p = new RegisteredParcel(myid, Integer.MAX_VALUE, data);
    _data.addElement(p);
    notifyAll();
  }


  /**
   * retrieves the first data Object that has been stored via a call (from
   * another thread) to sendData(threadId, data). If no such
   * data exists, the calling thread will wait until another thread stores an
   * appropriate datum.
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
						// notifyAll();  // nobody waits for the queue to become "not-full" 
						// so no notifications are needed
						return res;
					}
				}
				catch (IndexOutOfBoundsException e) {  // never thrown
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
					// notifyAll();  // nobody waits for the queue to become "not-full" 
					// so no notifications are needed
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

