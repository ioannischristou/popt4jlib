package parallel;

import java.util.HashMap;
import java.util.Vector;

/**
 * A read-write lock mechanism implementing the notion of fairness of granting
 * requests, in that the first thread to execute the code a request, will get
 * their request serviced before the second one. Same as DMCoordinator class, 
 * but guarantees FIFO order of threads' execution (i.e. if a writer thread 
 * arrives before a reader, the writer will execute before the reader, and 
 * vice-versa).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FairDMCoordinator {

  // synchronizing variables
  private int _readers=0;
  private int _writerCalls=0;
  private Thread _writerThread=null;
  private HashMap _readerThreads=null;  // map<Thread t, Integer count>
  // the next two variables are used for "fair granting" of locks to avoid
  // starvation (i.e. to "ensure progress")
  private Vector _waitingOn;
  private Lock _lock;

  static private FairDMCoordinator _DMCoord=null;
  static private HashMap _coords = new HashMap();


  /**
   * provides, securely, the unique instance of DMCoordinator that is
   * used to coordinate readers and writers. The method needs to be
   * synchronized (at the class level, as it's a static method) so as
   * to avoid the possibility of two different client threads receiving
   * different DMCoordinator objects to coordinate on (which results in no
   * coordination).
   * @return DMCoordinator the DMCoordinator object to use for locking/unlocking
   * various readers/writers threads.
   */
  synchronized static public FairDMCoordinator getInstance() {
    if (_DMCoord==null) _DMCoord = new FairDMCoordinator();
    return _DMCoord;
  }


  /**
   * provides, securely, the unique instance of DMCoordinator named &lt;name&gt; 
   * that is used to coordinate readers and writers. The method needs to be
   * synchronized (at the class level, as it's a static method) so as
   * to avoid the possibility of two different client threads receiving
   * different DMCoordinator objects to coordinate on (which results in no
   * coordination).
   * @param name String the name of the DMCoordinator object to obtain
   * @return DMCoordinator the object that goes by that name (creates new object
   * if no object goes by that name)
   */
  synchronized static public FairDMCoordinator getInstance(String name) {
    FairDMCoordinator dmc = (FairDMCoordinator) _coords.get(name);
    if (dmc==null) {
      dmc = new FairDMCoordinator();
      _coords.put(name, dmc);
    }
    return dmc;
  }


  /**
   * obtain read access. The method will wait if there is another thread
   * waiting to obtain a write-lock. It is ok for a thread that already has
   * the read lock or the write lock to call this method, however, it must
   * eventually be followed by a releaseReadAccess() call.
   */
  public void getReadAccess() {
    _lock.getLock();
    Thread current = Thread.currentThread();
    Integer cI = (Integer) _readerThreads.get(current);
    if (_writerThread == current || cI != null) {  // already have the lock
      ++_readers;
      int c = 1;
      if (cI!=null) c = cI.intValue()+1;
      _readerThreads.put(Thread.currentThread(), new Integer(c));
      _lock.releaseLock();
      return;
    }
    if (mustWait(null)) {
      ReadWaitObject w = new ReadWaitObject(current.getName()); // name used for debugging only
      synchronized (w) {
        _waitingOn.add(w);
        _lock.releaseLock();
        while (!w.getIsDone()) {
          try {
            w.wait();  // wait on this object, until someone tells us we can go
                       // the wait here is guaranteed to be waken from another
                       // thread that will call (later) notify() on w -when this
                       // thread's turn comes, and thus is not unconditional.
                       // The while-loop is only needed to protect against
                       // "spurious wake-ups"!!!
          }
          catch (InterruptedException e) {
            current.interrupt(); // recommended action
          }
        }
        _lock.getLock();
        _waitingOn.remove(w);
      }
    }
    // ok, get the lock
    ++_readers;
    _readerThreads.put(Thread.currentThread(), new Integer(1));
    _lock.releaseLock();
    return;
  }


  /**
   * This method might throw and it does so in the event that a deadlock or
   * starvation could otherwise occur. This may happen if many readers try
   * simultaneously to upgrade their locks to Write Lock status.
   * So the method will allow a single reader to upgrade to Write Lock
   * but will always throw if a reader tries to upgrade to a Write Lock while
   * there is another reader.
   * Notice that a reader that upgrades to a write lock must still release its
   * read lock -after it releases its write lock. Also, a writer thread may
   * "get" the write lock it already owns as many times as it wishes, but it
   * must eventually release the write lock exactly as many times as it "got" it
   * @throws ParallelException if the current thread already is a reader but
   * there is at least another reader in the system.
   */
  public void getWriteAccess() throws ParallelException {
    _lock.getLock();
    Thread current = Thread.currentThread();
    if (_writerThread == current) {
      ++_writerCalls;
      _lock.releaseLock();
      return;  // we have the lock
    }
    boolean is_cur_a_reader = _readerThreads.containsKey(current);
    if (_readers>1 && is_cur_a_reader) {
      _lock.releaseLock();
      throw new ParallelException("Illegal Attempt to Upgrade Read Lock");
    }
    if (is_cur_a_reader) {
      // this is the only reader, and thus there can also be no writer,
      // so just upgrade to writer status
      _writerThread=current;
      ++_writerCalls;
      _lock.releaseLock();
      return;
    }
    // wait until there are no other writers nor readers
    if (_writerCalls>0 || _readers>0 || _waitingOn.size()>0) {
      WriteWaitObject w = new WriteWaitObject(current.getName());  // name is only used for testing
      synchronized (w) {
        _waitingOn.add(w);
        _lock.releaseLock();
        while (!w.getIsDone()) {
          try {
            w.wait(); // the wait here is guaranteed to be waken from another
            // thread that will call (later) notify() on w -when this
            // thread's turn comes, and thus is not unconditional.
            // The while-loop only protects against "spurious wakes"!
          }
          catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // recommended action
          }
        }
        _lock.getLock();
        _waitingOn.remove(0);  // w is always at position 0
      }
    }
    ++_writerCalls;
    _writerThread = current; // get the write lock
    _lock.releaseLock();
  }


  /**
   * releases the read lock the calling thread owns
   * @throws ParallelException if the calling thread doesn't have the lock
   */
  public void releaseReadAccess() throws ParallelException {
    _lock.getLock();
    Thread current = Thread.currentThread();
    Integer rcI = (Integer) _readerThreads.get(current);
    if (rcI==null) {
      _lock.releaseLock();
      throw new ParallelException("Thread not allowed to call FairDMCoordinator.releaseReadAccess()");
    }
    --_readers;
    int rcn = rcI.intValue()-1;
    if (rcn==0) {
      _readerThreads.remove(current);
    }
    else {
      _readerThreads.put(current, new Integer(rcn));
    }
    if (_readers==0 && _writerThread==null) {  // second test is needed
      // no readers nor writer in the system any more
      // notify all readers up to (but not) first writer
      // or the first writer
      int i=0;
      while (i<_waitingOn.size()) {
        if (i==0) {
          WaitObject w = (WaitObject) _waitingOn.elementAt(0);
          synchronized (w) {
            w.setDone();
            w.notify();
          }
          if (w instanceof WriteWaitObject) break;
        }
        else {  // not first time
          WaitObject w = (WaitObject) _waitingOn.elementAt(i);
          if (w instanceof ReadWaitObject) {
            //w = (WaitObject) _waitingOn.elementAt(i);
            synchronized (w) {
              w.setDone();
              w.notify();
            }
          }
          else break;
        }
        ++i;
      }
    }
    _lock.releaseLock();
  }


  /**
   * releases the write lock the calling thread owns
   * @throws ParallelException if the calling thread doesn't have the lock
   */
  public void releaseWriteAccess() throws ParallelException {
    _lock.getLock();
    if (_writerThread!=Thread.currentThread()) {
      _lock.releaseLock();
      throw new ParallelException(
      "Thread not allowed to call FairDMCoordinator.releaseWriteAccess()");
    }
    if (--_writerCalls==0) {
      _writerThread = null;
      // notify all readers waiting up to (but not) first writer
      // or first writer only (if no readers before it)
      int i = 0;
      while (i<_waitingOn.size()) {
        if (i==0) {
          WaitObject w = (WaitObject) _waitingOn.elementAt(0);
          boolean w_is_writer = w instanceof WriteWaitObject;
          if (w_is_writer && _readers>0) break;  // I am still a reader and
                                                 // first waiter is a writer
          synchronized (w) {
            w.setDone();
            w.notify();
          }
          if (w_is_writer) break;
        }
        else {  // not first time
          WaitObject w = (WaitObject) _waitingOn.elementAt(i);
          if (w instanceof ReadWaitObject) {
            synchronized (w) {
              w.setDone();
              w.notify();
            }
          }
          else break;
        }
        ++i;
      }
    }
    _lock.releaseLock();
  }


  /**
   * return the current number of read-lock calls that have been accepted (this
   * does not have to be the same as the number of different threads that have
   * the read lock on this DMCoordinator object).
   * @return int
   */
  public int getNumReaders() {
    int res = 0;
    _lock.getLock();
    res = _readers;
    _lock.releaseLock();
    return res;
  }


  /**
   * return the number of total different threads in the system that currently
   * have the read-lock.
   * @return int
   */
  public int getNumReaderThreads() {
    int res=0;
    _lock.getLock();
    res = _readerThreads.size();
    _lock.releaseLock();
    return res;
  }


  /**
   * return the current number of writers (can be more than one if the only
   * writer thread has called getWriteAcess() multiple times)
   * Notice that there can also be simultaneously many read-locks in the system
   * (but they have to all belong to the same thread)
   * @return int
   */
  public int getNumWriters() {
    int res=0;
    _lock.getLock();
    res = _writerCalls;
    _lock.releaseLock();
    return res;
  }


  /**
   * return true iff the current thread has a read-lock of this FairDMCoordinator
   * object.
   * @return boolean
   */
  public boolean currentThreadHasReadLock() {
    _lock.getLock();
    boolean res = (_readerThreads.get(Thread.currentThread())!=null);
    _lock.releaseLock();
    return res;
  }


  /**
   * return true iff the current thread has the write-lock of this FairDMCoordinator
   * object.
   * @return boolean
   */
  public boolean currentThreadHasWriteLock() {
    _lock.getLock();
    boolean res = (Thread.currentThread()==_writerThread);
    _lock.releaseLock();
    return res;
  }


  private FairDMCoordinator() {
    _readerThreads = new HashMap();
    _waitingOn = new Vector();
    _lock = new Lock();
  }


  private boolean mustWait(Object w) {
    if (_writerThread!=null) return true;
    int wsz = _waitingOn.size();
    for (int i=0; i<wsz; i++) {
      Object wi = _waitingOn.elementAt(i);
      if (wi instanceof WriteWaitObject) return true;
      else if (wi==w) return false;
    }
    return false;
  }

	
	/**
	 * auxiliary inner-class, not part of the public API.
	 */
	class WaitObject {
		String _name;
		boolean _isDone=false;

		public WaitObject(String n) {
			_name=n;
		}
		public void setDone() { _isDone = true; }
		public boolean getIsDone() { return _isDone; }
	}


	/**
	 * auxiliary inner-class, not part of the public API.
	 */
	class WriteWaitObject extends WaitObject {
		public WriteWaitObject(String n) {
			super(n);
		}
	}


	/**
	 * auxiliary inner-class, not part of the public API.
	 */
	class ReadWaitObject extends WaitObject {
		public ReadWaitObject(String n) {
			super(n);
		}
	}

}

