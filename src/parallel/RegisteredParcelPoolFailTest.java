/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel;

import java.util.HashMap;


/**
 * Tests the RegisteredParcel-pooling mechanism to show-case that in general
 * it is broken in the presence of multiple msg-passing coordinators and 
 * multiple thread sending/receiving msgs among many of them. The compile-time
 * sanity tests constants of the RegisteredParcelPool class must be set to 
 * true for this test to work.
 * Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RegisteredParcelPoolFailTest {
	/**
	 * creates n &ge; 1 SFMPCOld coordinators, and m &ge; n threads. The
	 * main thread then sends data to all the others through the "respective" 
	 * coordinator. Each of the other m threads simply receive that data.
	 * If n=1 then there should be no exception raised. If n &gt; 1 there should
	 * be NullPointerExceptions thrown.
	 * @param args [num_coords(3)] [num_threads(3)] [maxiters(1000)]
	 */
	public static void main(String[] args) {
		int n = 3;
		try {
			n = Integer.parseInt(args[0]);
		}
		catch (Exception e) {
			System.err.println("using n="+n+" coords");
		}
		if (n==1) RegisteredParcelPool.setIgnoreReleaseSanityTest(true);
		int m = 3;
		try {
			m = Integer.parseInt(args[1]);
		}
		catch (Exception e) {
			System.err.println("using m="+m+" threads");
		}
		if (m<n) {
			System.err.println("m<n is an error.");
			System.exit(-1);
		}
		int maxiters = 1000;
		try {
			maxiters = Integer.parseInt(args[2]);
		}
		catch (Exception e) {
			System.err.println(" will run for maxiters="+maxiters+" iterations");
		}
		SFMPCOld[] coords = new SFMPCOld[n];
		RPFailTestThread[] threads = new RPFailTestThread[m];
		for (int i=0; i<n; i++) {
			coords[i] = SFMPCOld.getInstance("SFMPCOld"+i);
			if (i<m) {
				threads[i] = new RPFailTestThread(i, coords[i], maxiters);
				threads[i].start();
			}
		}
		for (int i=n; i<m; i++) {
			threads[i] = new RPFailTestThread(i, coords[(i-n) % n], maxiters);
			threads[i].start();
		}
		// send data to everyone
		for (int i=1; i<=maxiters; i++) {
			for (int j=0; j<m; j++) {
				try {
					coords[j % n].sendData(-1, new Integer(i));
				}
				catch (ParallelException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			boolean ok=false;
			int i=0;
			for (; i<m && !ok; i++) {
				threads[i].join();
				if (threads[i].isDone()==false) {
					System.err.println("Thread-"+threads[i]._id+" joins but is not done.");
					if (n>1) ok=true;
					else break;  // thread failed but n==1: test fails
				}
			}
			if (i==m && n==1) ok = true;  // all threads ran
			if (!ok) System.err.println("test failed.");
			else System.out.println("Test succeeded.");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("Test succeeded.");
		}
		return;
	}
	
	static class RPFailTestThread extends Thread {
		private SFMPCOld _coord;
		private int _id;
		private int _maxnum;
		private boolean _isDone;  // init. to false
		
		RPFailTestThread(int id, SFMPCOld coord, int maxnum) {
			_coord = coord;
			_id = id;
			_maxnum = maxnum;
		}
		
		public void run() {
			while (true) {
				Integer m = (Integer) _coord.recvData(_id);
				if (m.intValue()>=_maxnum) break;
			}
			//synchronized (this) {
				_isDone = true;
			//}
		}
		
		public boolean isDone() {  // no need for synchronization
			return _isDone;
		}
	}
}


class SFMPCOld {
  private static final int _maxSize=10000;
  //private Vector _data;  // Vector<RegisteredParcel>
  /**
   * maintains RegisteredParcel objects to be exchanged between threads
   */
  private BoundedBufferArrayUnsynchronized _data;
  private static SFMPCOld _instance=null;
  private static HashMap _instances=new HashMap();  // map<String name, SFMPC instance>


  /**
   * private constructor in accordance with the Singleton Design Pattern
   */
  private SFMPCOld() {
    _data = new BoundedBufferArrayUnsynchronized(_maxSize);
  }


  /**
   * return the default SFMPCOld object.
   * @return SFMPCOld
   */
  public synchronized static SFMPCOld getInstance() {
    if (_instance==null) {
      _instance = new SFMPCOld();
    }
    return _instance;
  }

	
  /**
   * return the unique SFMPCOld object associated
   * with a given name.
   * @return SFMPCOld
   */
  public synchronized static SFMPCOld getInstance(String name) {
    SFMPCOld instance = (SFMPCOld) _instances.get(name);
    if (instance==null) {
      instance = new SFMPCOld();
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
      throw new ParallelException("SFMPCOld queue is full");
    // RegisteredParcel p = new RegisteredParcel(myid, Integer.MAX_VALUE, data);
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
    // RegisteredParcel p = new RegisteredParcel(myid, Integer.MAX_VALUE, data);
    RegisteredParcel p = RegisteredParcel.newInstance(myid, Integer.MAX_VALUE, data);
    try {
      _data.addElement(p);
    }
    catch (IllegalStateException e) {
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
						if (tstoid!=myid && tstoid!=Integer.MAX_VALUE) {
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
						p.release();
						return res;
					}
        }
        catch (IndexOutOfBoundsException e) {
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

	
	/**
   * the method has the same semantics as the recvData(myid) method except that
   * it returns immediately with null if no appropriate data exist at the time
   * of the call. 
   * Note: senders "create" RegisteredParcel objects, receivers "release" them.
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
					if (tstoid!=myid && tstoid!=Integer.MAX_VALUE) {
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
					p.release();
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

}

