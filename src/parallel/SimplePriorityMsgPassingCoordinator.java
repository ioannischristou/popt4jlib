package parallel;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.Iterator;


/**
 * The SimplePriorityMsgPassingCoordinator class: yet another class that allows
 * sending and receiving (synchronous and asynchronous) data between threads.
 * It behaves as the SimpleFasterMsgPassingCoordinator, except that tasks are
 * assumed to have different priorities, and higher-priority tasks are received
 * before lower-priority ones. The class is used e.g. with the
 * <CODE>FasterParallelAsynchBatchPriorityTaskExecutor</CODE>.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class SimplePriorityMsgPassingCoordinator {
  private static int _maxSize=10000;  // may be modified only before main use
  private TreeSet _data;  // holds ComparableTaskObject objects
  private static SimplePriorityMsgPassingCoordinator _instance=null;
  private static HashMap _instances=new HashMap();  // map<String name, SPMPC instance>


  /**
   * private constructor in accordance with the Singleton Design Pattern
   */
  private SimplePriorityMsgPassingCoordinator() {
    _data = new TreeSet();
  }


  /**
   * return the default SimplePriorityMsgPassingCoordinator object.
   * @return SimpleFasterMsgPassingCoordinator
   */
  public synchronized static SimplePriorityMsgPassingCoordinator getInstance() {
    if (_instance==null) {
      _instance = new SimplePriorityMsgPassingCoordinator();
    }
    return _instance;
  }


  /**
   * return the unique SimplePriorityMsgPassingCoordinator object associated
   * with a given name.
   * @return SimplePriorityMsgPassingCoordinator
   */
  public synchronized static SimplePriorityMsgPassingCoordinator getInstance(String name) {
    SimplePriorityMsgPassingCoordinator instance = (SimplePriorityMsgPassingCoordinator) _instances.get(name);
    if (instance==null) {
      instance = new SimplePriorityMsgPassingCoordinator();
      _instances.put(name, instance);
    }
    return instance;
  }


  /**
   * return the _maxSize data member
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
	public synchronized static void setMaxSize(int num) throws ParallelException, IllegalArgumentException {
		if (_instance!=null || _instances.size()>0)
			throw new ParallelException(
							"SimplePriorityMsgPassingCoordinator.setMaxSize(num): call is only allowed "+
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
   * method -regardless of which thread that is. The method returns immediately.
   * @param data ComparableTaskObject
   * @throws ParallelException if there are more data than _maxSize in the queue
   */
  public synchronized void sendData(ComparableTaskObject data)
      throws ParallelException {
    if (_data.size()>=_maxSize)
      throw new ParallelException("SimplePriorityMsgPassingCoordinator queue is full");
    _data.add(data);
    notifyAll();
  }


  /**
   * stores the data object to be consumed by any thread that invokes recvData()
   * method -regardless of which thread that is. The method will wait if the
   * queue is full until another thread consumes a datum and allows this thread
   * to write to the _data buffer.
   * @param data Object
   */
  public synchronized void sendDataBlocking(ComparableTaskObject data) {
    while (_data.size()>= _maxSize) {
      try {
        wait();
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    _data.add(data);
    notifyAll();
  }


  /**
   * retrieves the first data Object that has been stored via a call (from
   * another thread) to <CODE>sendData[Blocking](data)</CODE>. If no such
   * data exists, the calling thread will wait until another thread stores an
   * appropriate datum. The implementation will always receive the first datum 
	 * available in the priority queue. 
	 * <p> Notice: this method should not be used in
	 * cases where <CODE>ThreadSpecificComparableTaskObject</CODE> object is sent, 
	 * since the invocation of this method by a thread, may well result in the 
	 * object being received without any concern about the calling thread's id.
   * @return Object
   */
  public synchronized Object recvData() {
    while (true) {
      if (_data.size()>0) {
        Iterator it = _data.iterator();
        ComparableTaskObject res = (ComparableTaskObject) it.next();
        it.remove();
        notifyAll();
        return res;
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
   * retrieves the first data Object that has been stored via a call (from
   * another thread) to <CODE>sendData[Blocking](data)</CODE> that either 
	 * happens to be a simple <CODE>ComparableTaskObject</CODE> or a 
	 * <CODE>ThreadSpecificComparableTaskObject</CODE> with a thread-id that 
	 * matches the argument passed in. If no such data exists, the calling thread 
	 * will wait until another thread stores an appropriate datum.
   * @return Object
   */
  public synchronized Object recvData(int myid) {
    while (true) {
      if (_data.size()>0) {
        Iterator it = _data.iterator();
				while (it.hasNext()) {
					ComparableTaskObject res = (ComparableTaskObject) it.next();
					if (res instanceof ThreadSpecificComparableTaskObject) {
						ThreadSpecificComparableTaskObject res2 = 
										(ThreadSpecificComparableTaskObject) res;
						int tid2runon = res2.getThreadIdToRunOn();
						if (tid2runon==myid ||  // ok, it's for me 
								tid2runon == Integer.MAX_VALUE) {  // still ok, res doesn't care who's gonna run it
							it.remove();
							notifyAll();
							return res2;				
						}
					} else {  // ok, sender doesn't care about which thread executes it.
						it.remove();
						notifyAll();
						return res;
					}
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
	 * synchronized method captures a snapshot of the <CODE>_data</CODE> queue for 
	 * debugging purposes only.
	 * @return String
	 */
	public synchronized String toString() {
		String data_string="";
		Iterator it = _data.iterator();
		while (it.hasNext()) {
			data_string += it.next().toString();
			if (it.hasNext()) data_string+= ",";
		}
		String res = "SimplePriorityMsgPassingCoordinator[_data.size()="+
						     _data.size()+" _data=["+
								 data_string+"]"+
						     "]";
		return res;
	}
}

