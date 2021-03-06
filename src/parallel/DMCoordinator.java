package parallel;

import java.util.HashMap;
import java.io.Serializable;


/**
 * Class providing proper synchronization between
 * readers and writers. Many readers may concurrently execute, but
 * there can be only one writer thread at a time, and while a writer has
 * control, no reader thread may gain access (except the writer itself).
 * The clients of this class
 * when wishing to gain read access, simply call
 * DMCoordinator.getInstance([name]).getReadAccess()
 * and when they're done must call
 * DMCoordinator.getInstance([name]).releaseReadAccess()
 * and similarly for writers.
 * It is also possible for a reader to attempt to upgrade to a Write Lock
 * which will throw an exception however if there are more than 1 reader at
 * the time of the attempt.
 * It is the responsibility of the clients
 * to ensure that these methods must always be called in pairs (every getXXX
 * must be followed by a releaseXXX).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DMCoordinator implements Serializable {
  // the class is made serializable only so that other objects containing
  // references to it as data-members (e.g. Graph objects) can be transported
  // over sockets to other JVMs. In another JVM, they certainly do not
  // maintain any synchronization or mutual exclusion properties with the
  // parent JVM.
  // If a reference is transported when it is not in "clean" state (i.e. when
  // someone actually has a read or write lock), an exception will be thrown
  // as references to Threads cannot be serialized (one could ignore these
  // refs in serialization/deserialization process by declaring _writerThread
  // and _readerThreads as "transient", but then, it would be possible to
  // transfer a DMCoordinator object that is read and/or write - locked to
  // another JVM in an unlocked state, which would likely be wrong, so it's
  // better to cause an exception to be thrown in such a case).
  private final static long serialVersionUID = -5398202788281387109L;

  // synchronizing variables
  private int _readers=0;
  private int _writerCalls=0;
  private Thread _writerThread=null;
  private HashMap _readerThreads=null;  // map<Thread t, Integer count>
  // the next two variables are used for "fair granting" of locks to avoid
  // starvation (i.e. to "ensure progress")
  private int _writersWaiting=0;
  private int _readersWaiting=0;

  static private DMCoordinator _DMCoord=null;
  static private HashMap _coords = new HashMap();

  /**
   * the following constant should likely not be set at zero value
   * since that would make real the possibility of dead-locking in
   * a situation where a thread that has a read-lock, spawns and
   * waits for another thread that must also get the read-lock, but
   * in the mean-while, a third thread has requested the write-lock
   * in which case, the spawned thread, being "polite" will wait
   * behind the third thread for ever.
   */
  private static final long _PRINT_INTERVAL_MSECS = 1;  // compile-time const


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
  synchronized static public DMCoordinator getInstance() {
    if (_DMCoord==null) _DMCoord = new DMCoordinator();
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
  synchronized static public DMCoordinator getInstance(String name) {
    DMCoordinator dmc = (DMCoordinator) _coords.get(name);
    if (dmc==null) {
      dmc = new DMCoordinator();
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
  synchronized public void getReadAccess() {
    Thread current = Thread.currentThread();
    Integer cI = (Integer) _readerThreads.get(current);
    if (_writerThread == current || cI != null) {  // have lock already
      ++_readers;
      int c = 1;
      if (cI!=null) c = cI.intValue()+1;
      _readerThreads.put(Thread.currentThread(), new Integer(c));
      return;
    }
    // waiting for another writer-thread (that arrived earlier and is in wait
    // state) will only happen for whatever amount of time _PRINT_INTERVAL_MSECS
    // specifies; after that (or in the case of a "spurious wake-up"!!!),
    // the current thread will stop being "polite" and will proceed to see how
    // it can grab the read-lock.
    if (_writersWaiting>0) {  // don't steal the lock from a writer
      ++_readersWaiting;
      try  {
        if (_PRINT_INTERVAL_MSECS>1)
          utils.Messenger.getInstance().msg("DMCoordinator.getReadAccess(): waiting behind wait-writers "+
                                            "TIMESTAMP="+System.currentTimeMillis(),0);
        wait(_PRINT_INTERVAL_MSECS);  // a writer is waiting before us
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // recommended action
      }
      --_readersWaiting;
    }
    while (_writerThread!=null) {
      ++_readersWaiting;
      try  {
        if (_PRINT_INTERVAL_MSECS>1)
          utils.Messenger.getInstance().msg("DMCoordinator.getReadAccess(): waiting for writer to finish "+
                                            "TIMESTAMP="+System.currentTimeMillis(),0);
        wait(_PRINT_INTERVAL_MSECS);  // someone ELSE has a write lock
      }
      catch (InterruptedException e) {
        current.interrupt();  // recommended action
      }
      --_readersWaiting;
    }
    // sanity check
    if (_writerThread!=null || _writerCalls!=0) {
      //throw new ParallelException("SANITY TEST FAILED");
      utils.Messenger.getInstance().msg("DMCoordinator.getReadAccess(): SANITY TEST FAILED "+
                                        "TIMESTAMP="+System.currentTimeMillis(),0);
      // throw an exception without declaring it
      Integer x = null;
      System.err.println("throwing NullPointerException now to unwind computations...");
      System.err.println(x.intValue());
    }
    // ok, get the lock
    ++_readers;
    _readerThreads.put(Thread.currentThread(), new Integer(1));
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
  synchronized public void getWriteAccess() throws ParallelException {
    Thread current = Thread.currentThread();
    if (_writerThread == current) {
      ++_writerCalls;
      return;  // we have the lock
    }
    boolean is_cur_a_reader = _readerThreads.containsKey(current);
    if (_readers>1 && is_cur_a_reader)
      throw new ParallelException("Illegal Attempt to Upgrade Read Lock");
    if (is_cur_a_reader) {
      // this is the only reader, and thus there can also be no writer,
      // so just upgrade to writer status
      _writerThread=current;
      ++_writerCalls;
      return;
    }
    // waiting for another thread (that arrived earlier and is in wait
    // state) will only happen for whatever amount of time _PRINT_INTERVAL_MSECS
    // specifies; after that (or in the case of a "spurious wake-up"!!!),
    // the current thread will stop being "polite" and will proceed to see how
    // it can grab the write-lock.
    if (_readersWaiting>0 || _writersWaiting>0) {  // don't steal the lock
      ++_writersWaiting;
      try {
        if (_PRINT_INTERVAL_MSECS>1)
          utils.Messenger.getInstance().msg("DMCoordinator.getWriteAccess(): waiting behind other wait-[readers/writers]"+
                                            " TIMESTAMP="+System.currentTimeMillis(),0);
        wait(_PRINT_INTERVAL_MSECS);
      }
      catch (InterruptedException e) {
        current.interrupt();  // recommended action
      }
      --_writersWaiting;
    }
    // wait until there are no other writers nor readers
    while (_writerCalls>0 || _readers>0) {
      ++_writersWaiting;
      try {
        if (_PRINT_INTERVAL_MSECS>1)
          utils.Messenger.getInstance().msg("DMCoordinator.getWriteAccess(): waiting for other [readers/writer] to finish "+
                                            "TIMESTAMP="+System.currentTimeMillis(),0);
        wait(_PRINT_INTERVAL_MSECS);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // recommended action
      }
      --_writersWaiting;
    }
    // sanity check
    if (_writerThread!=null || _writerCalls!=0 || _readers>0) {
      throw new ParallelException("SANITY TEST FAILED");
    }
    // we're ok
    ++_writerCalls;
    _writerThread = current;  // get the write lock
  }


  /**
   * releases the read lock the calling thread owns
   * @throws ParallelException if the calling thread doesn't have the lock
   */
  synchronized public void releaseReadAccess() throws ParallelException {
    Thread current = Thread.currentThread();
    Integer rcI = (Integer) _readerThreads.get(current);
    if (rcI==null) {
      throw new ParallelException("Thread not allowed to call DMCoordinator.releaseReadAccess()");
    }
    --_readers;
    int rcn = rcI.intValue()-1;
    if (rcn==0) {
      _readerThreads.remove(current);
    }
    else {
      _readerThreads.put(current, new Integer(rcn));
    }
    if (_readers==0 && _writerThread==null) {
      notifyAll();  // no readers or writer in the system any more
    }
  }


  /**
   * releases the write lock the calling thread owns
   * @throws ParallelException if the calling thread doesn't have the lock
   */
  synchronized public void releaseWriteAccess() throws ParallelException {
    if (_writerThread!=Thread.currentThread()) {
      throw new ParallelException(
      "Thread not allowed to call DMCoordinator.releaseWriteAccess()");
    }
    if (--_writerCalls==0) {
      _writerThread = null;  // no writer in the system any more
      notifyAll();
    }
  }


  /**
   * return the current number of read-lock calls that have been accepted (this
   * does not have to be the same as the number of different threads that have
   * the read lock on this DMCoordinator object).
   * @return int
   */
  synchronized public int getNumReaders() {
    return _readers;
  }


  /**
   * return the number of total different threads in the system that currently
   * have the read-lock.
   * @return int
   */
  synchronized public int getNumReaderThreads() {
    return _readerThreads.size();
  }


  /**
   * return the current number of writers (can be more than one if the only
   * writer thread has called getWriteAcess() multiple times)
   * Notice that there can also be simultaneously many read-locks in the system
   * (but they have to all belong to the same thread)
   * @return int
   */
  synchronized public int getNumWriters() {
    return _writerCalls;
  }


  /**
   * return true iff the current thread has a read-lock of this DMCoordinator
   * object.
   * @return boolean
   */
  synchronized public boolean currentThreadHasReadLock() {
    return (_readerThreads.get(Thread.currentThread())!=null);
  }


  /**
   * return true iff the current thread has the write-lock of this DMCoordinator
   * object.
   * @return boolean
   */
  synchronized public boolean currentThreadHasWriteLock() {
    return (Thread.currentThread()==_writerThread);
  }


  private DMCoordinator() {
    _readerThreads = new HashMap();
  }
}

