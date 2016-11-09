package parallel.distributed;

import parallel.Lock;
import parallel.ParallelException;
import java.io.*;
import java.net.*;
import java.util.*;


/**
 * implements a distributed lock, valid across JVMs and across multiple threads
 * in each JVM. Within a JVM, only a single lock can exist for each 
 * &lt;host,lockport(,unlockport)&gt; pair.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DLock {
  private static HashMap _locks = new HashMap();  // map<String host_lockport_unlockport, DLock lock>
  private String _host = "localhost";
  private int _lockport = 7892;
	private int _unlockport = 7893;
  private Lock _localLock;


  /**
   * get the unique DLock object associated with the default 
	 * &lt;host,lockport,unlockport&gt; pair, &lt;localhost,7892,7893&gt;.
   * @return DLock
   */
  public static synchronized DLock getInstance() throws IOException {
    InetAddress ia = InetAddress.getLocalHost();
    String local_host_addr = ia.getHostAddress();
    String hostport = local_host_addr+"_7892_7893";
    DLock dl = (DLock) _locks.get(hostport);
    if (dl==null) {
      dl = new DLock(local_host_addr, 7892, 7893);
      _locks.put(hostport, dl);
    }
    return dl;
  }


  /**
   * get the unique DLock object associated with the specific 
	 * &lt;host,lockport,unlockport&gt; pair.
   * @param host String
   * @param lockport int
	 * @param unlockport int
   * @return DLock
   * @throws IOException
   */
  public static synchronized DLock getInstance(String host, int lockport, int unlockport) throws IOException {
    InetAddress ia = InetAddress.getByName(host);
    String host_addr = ia.getHostAddress();
    String hostports = host_addr+"_"+lockport+"_"+unlockport;
    DLock dl = (DLock) _locks.get(hostports);
    if (dl==null) {
      dl = new DLock(host_addr, lockport, unlockport);
      _locks.put(hostports, dl);
    }
    return dl;
  }


  /**
   * get a valid lock for all threads in this JVM, and all others that will
	 * execute this method that are connected on the same lock server/ports.
   * @throws ParallelException
   */
  public void getLock() throws ParallelException {
    // order of locking (1. , 2.) is very important
    // 1. synch among threads in this JVM
    _localLock.getLock();
    // 2. synch among all participating JVMs
    Socket s = null;
    ObjectInputStream ois = null;
    ObjectOutputStream oos = null;
    try {
      s = new Socket(_host, _lockport);
      LockRequest lr = new LockRequest();
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.flush();
      oos.writeObject(lr);
      oos.flush();
      ois = new ObjectInputStream(s.getInputStream());
      Object res = ois.readObject();
      if (res instanceof LockResponse == false)  // may have been a FailedReply
        throw new ParallelException("unexpected response from LockServer");
      // done
    }
    catch (Exception e) {
      e.printStackTrace();
      _localLock.releaseLock();
      throw new ParallelException("network failure?");
    }
    finally {
      try {
        if (ois != null) ois.close();
        if (oos != null) oos.close();
        if (s != null) s.close();
      }
      catch (IOException e) {
        e.printStackTrace(); // ignore
      }
    }
  }

	
  /**
   * get a valid lock for all threads in this JVM, and all others that will
	 * execute this method that are connected on the same lock server/ports, but
	 * ONLY IF the lock can be immediately obtained. Method returns immediately as
	 * it sends the request to the lock-server in the unlockport which is for 
	 * non-blocking requests.
   * @throws ParallelException
   */
  public boolean getLockIfAvailable() throws ParallelException {
    // order of locking (1. , 2.) is very important
    // 1. synch among threads in this JVM
    boolean got_local_lock = _localLock.getLockIfAvailable();
		if (got_local_lock) {
			// 2. synch among all participating JVMs
			Socket s = null;
			ObjectInputStream ois = null;
			ObjectOutputStream oos = null;
			try {
				s = new Socket(_host, _unlockport);  // send the request to the unlock-port!
				LockIfAvailableRequest lr = new LockIfAvailableRequest();
				oos = new ObjectOutputStream(s.getOutputStream());
				oos.flush();
				oos.writeObject(lr);
				oos.flush();
				ois = new ObjectInputStream(s.getInputStream());
				Object res = ois.readObject();
				if (res instanceof LockResponse) {
					return true;
				}
				else if (res instanceof LockNotAvailableNowResponse) {
					_localLock.releaseLock();
					return false;
				}
				else {  // cannot happen: even if DLockSrv's _client failed, 
					      // it will still send LockNotAvailableNowResponse
					throw new ParallelException("unexpected response from LockServer");
				}
				// done
			}
			catch (Exception e) {
				e.printStackTrace();
				_localLock.releaseLock();
				throw new ParallelException("network failure?");
			}
			finally {
				try {
					if (ois != null) ois.close();
					if (oos != null) oos.close();
					if (s != null) s.close();
				}
				catch (IOException e) {
					e.printStackTrace(); // ignore
				}
			}
		} else return false;
  }


  /**
   * release the lock across all JVMs asking for a lock for the particular
   * &lt;host,port&gt; pair that this lock is associated with.
   * @throws ParallelException
   */
  public void releaseLock() throws ParallelException {
    // order of unlocking (1. , 2.) is very important
    // 1. notify server
    Socket s = null;
    ObjectInputStream ois = null;
    ObjectOutputStream oos = null;
    try {
      s = new Socket(_host, _unlockport);
      UnlockRequest lr = new UnlockRequest();
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.flush();
      oos.writeObject(lr);
      oos.flush();
      ois = new ObjectInputStream(s.getInputStream());
      Object res = ois.readObject();
      if (res instanceof UnlockResponse == false) {
        // don't release lock locally either
        throw new ParallelException("unexpected response from LockServer");
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ParallelException("network failure?");
      // local lock not released either
    }
    finally {
      try {
        if (ois != null) ois.close();
        if (oos != null) oos.close();
        if (s != null) s.close();
      }
      catch (IOException e) {
        e.printStackTrace(); // ignore
      }
    }
    // 2. notify other threads in this JVM
    _localLock.releaseLock();
  }


  /**
   * no-arg constructor assumes host is localhost, and lock/unlockport is 
	 * 7892/7893.
   */
  private DLock() {
    _localLock = new Lock();
  }


  /**
   * constructor allows client to specify (host,lockport,unlockport) pair for 
	 * distributed-lock server location.
   * @param host String
   * @param lockport int
	 * @param unlockport int
	 * 
   */
  private DLock(String host, int lockport, int unlockport) {
    this();
    _host = host;
    _lockport = lockport;
		_unlockport = unlockport;
  }

}

