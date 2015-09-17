package parallel.distributed;

import parallel.*;
import java.net.*;
import java.io.*;

/**
 * class implements a distributed read-write lock object across many threads
 * living in many (remote) JVMs. The following constraint is imposed on DRWLock
 * objects use: each of its methods can only be called from the
 * same thread that created this DRWLock object (otherwise an exception is
 * thrown). As a consequence, every thread that wishes to obtain a read/write
 * lock, must create such an object, and then start invoking its methods.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DRWLock {
  private String _host = "localhost";
  private int _port = 7897;
  private String _coordname = "DRWLock_"+_host+"_"+_port;  // coordname and rwlock name are the same
  private DActiveMsgPassingCoordinatorLongLivedConnClt _coordclt=null;
  private Thread _originatingThread = null;


  /**
   * no-arg constructor assumes the following defaults:
	 * <ul>
   * <li> host="localhost"
   * <li> port=7897
   * <li> rwlock/coord name = "DRWLock_localhost_7897"
	 * </ul>
   * @throws UnknownHostException
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws ParallelException
   */
  public DRWLock() throws UnknownHostException, IOException, ClassNotFoundException, ParallelException {
    _coordclt = new DActiveMsgPassingCoordinatorLongLivedConnClt(_host, _port, _coordname);
    _originatingThread = Thread.currentThread();
  }


  /**
   * constructor specifies
   * the IP address specified by first two argument parameters with the name
   * specified in the third parameter, so that later invocations of the
   * methods of this object will synchronize the current
   * thread with all other threads in all JVMs having constructed RWLock
   * objects connected to the same server-port, and same rwlock name.
   * @param host String
   * @param port int
   * @param coordname String
   * @throws UnknownHostException
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws ParallelException
   */
  public DRWLock(String host, int port, String coordname)
      throws UnknownHostException, IOException, ClassNotFoundException,
             ParallelException {
    // 0. initialization
    _host = host;
    _port = port;
    _coordname = coordname;
    _originatingThread = Thread.currentThread();
		// 1. long-lived connection active msg-passing client construction
    _coordclt = new DActiveMsgPassingCoordinatorLongLivedConnClt(_host, _port, _coordname);
  }


  /**
   * obtain a global read-lock corresponding to the coordinator name specified 
	 * in the constructor of this object.
   * @throws IOException if a network error occurs
   * @throws ClassNotFoundException if the server JVM is not in synch with
   * this JVM's classes.
   * @throws ParallelException if this DBarrier object was constructed by a
   * different thread than the one calling this method.
   */
  public void getReadAccess() throws IOException, ClassNotFoundException, ParallelException {
    // 0. check if thread is ok
    if (Thread.currentThread()!=_originatingThread) {
      throw new ParallelException("getReadAccess(): method called from thread different than the one originating the object");
    }
    // 1. send a get read lock request: when the method terminates, we're done
    _coordclt.sendData(-1, new DRLockGetRequest(_coordname));  // lock and coord names are the same
  }


  /**
   * release the read-lock held by this thread.
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws ParallelException if calling thread is not the same as creation
   * thread.
   */
  public void releaseReadAccess() throws IOException, ClassNotFoundException, ParallelException {
    // 0. check if thread is ok
    if (Thread.currentThread()!=_originatingThread) {
      throw new ParallelException("releaseReadAccess(): method called from thread different than the one originating the object");
    }
    // 1. send a DRLockReleaseRequest: when the method terminates, we're ok
    _coordclt.sendData(-1, new DRLockReleaseRequest(_coordname));  // lock and coord names are the same
  }

	
  /**
   * obtain a global write-lock corresponding to the coordinator name specified 
	 * in the constructor of this object.
   * @throws IOException if a network error occurs
   * @throws ClassNotFoundException if the server JVM is not in synch with
   * this JVM's classes.
   * @throws ParallelException if this DBarrier object was constructed by a
   * different thread than the one calling this method.
   */
  public void getWriteAccess() throws IOException, ClassNotFoundException, ParallelException {
    // 0. check if thread is ok
    if (Thread.currentThread()!=_originatingThread) {
      throw new ParallelException("getWriteAccess(): method called from thread different than the one originating the object");
    }
    // 1. send a get read lock request: when the method terminates, we're done
    _coordclt.sendData(-1, new DWLockGetRequest(_coordname));  // lock and coord names are the same
  }


  /**
   * release the read-lock held by this thread.
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws ParallelException if calling thread is not the same as creation
   * thread.
   */
  public void releaseWriteAccess() throws IOException, ClassNotFoundException, ParallelException {
    // 0. check if thread is ok
    if (Thread.currentThread()!=_originatingThread) {
      throw new ParallelException("releaseReadAccess(): method called from thread different than the one originating the object");
    }
    // 1. send a DRLockReleaseRequest: when the method terminates, we're ok
    _coordclt.sendData(-1, new DWLockReleaseRequest(_coordname));  // lock and coord names are the same
  }
	
	
	/**
	 * invoke this method when the thread has no further use for this r/w lock.
	 * It is the user's responsibility to ensure the thread does not hold neither
	 * a read nor a write lock on this object. If the method is invoked while 
	 * a lock is still held, then the lock will never be released again.
	 * @throws IOException 
	 */
	public void done() throws ParallelException, IOException {
    // 0. check if thread is ok
    if (Thread.currentThread()!=_originatingThread) {
      throw new ParallelException("releaseReadAccess(): method called from thread different than the one originating the object");
    }
		// 1. release the client connection
		_coordclt.closeConnection();
		_coordclt = null;  // safety precaution.
	}

}

