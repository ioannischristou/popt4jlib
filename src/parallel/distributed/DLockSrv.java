package parallel.distributed;

import parallel.*;
import java.io.*;
import java.net.*;
import java.util.*;


/**
 * class implements a server for distributed locking functionality. Multiple
 * JVMs can connect to this server and ask for the lock corresponding to this
 * server. Each client may as well be multi-threaded.
 * Each <CODE>DLockSrv</CODE> may serve up to 10000 concurrent clients asking 
 * for the lock (assuming the O/S can handle this many sockets and threads).
 * If more clients need to get such a global distributed lock, multiple lock
 * servers may connect as clients to a centralized server (forming a star 
 * topology). Clients of the distributed global lock may then connect to any of
 * the participating lock servers in the star topology, and in this way millions
 * of clients could be reliably acquiring/releasing a single global lock. In 
 * fact, any connected tree that has no cycle in it, will work (if on the other
 * hand there exists a cycle in the servers topology, it is guaranteed that all
 * lock requests will instantly hang.)
 * As the server uses the <CODE>parallel.FIFOLock</CODE> class to implement its
 * lock mechanism, there is no way to check whether a thread calling
 * <CODE>releaseLock()</CODE> actually had obtained the lock prior to the call.
 * It is the responsibility of the calling code to ensure no thread tries to 
 * release the distributed lock without having obtained it first.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class DLockSrv {
  private int _lockPort=7892;  // the port for sending lock requests
	private int _unlockPort=7893;  // separate port for sending unlock requests, 
	                               // so that even in the face of many locks
	                               // an unlock request will always be executed.
	                               // LockIfAvailable requests which are non-
	                               // blocking, are also sent to this port!
  private long _lockCount=0;
  private DynamicAsynchTaskExecutor _pool;
	private DLock _client;  // if not null, points to the center of a star 
	                        // network of DLockSrv objects.


  /**
   * no-arg constructor.
   */
  private DLockSrv() {
    try {
      _pool = DynamicAsynchTaskExecutor.newDynamicAsynchTaskExecutor(1, 10000);
    }
    catch (ParallelException e) {
      e.printStackTrace();  // cannot happen
    }
  }


  /**
   * constructor with port number specification.
   * @param lockport int the port number to listen to for lock-requests
	 * @param unlockport int the port number to listen to for unlock-requests and
	 * lock-if-available (non-blocking) requests
	 * @throws IllegalArgumentException if lockport is same as unlockport
   */
  private DLockSrv(int lockport, int unlockport) {
    this();
		if (_lockPort==_unlockPort) throw new IllegalArgumentException("lockport same as unlockport");
    _lockPort = lockport;
		_unlockPort = unlockport;
  }
	

	/**
	 * constructor also specifies the host/ports address of the center of a star
	 * network of DLockSrv servers, of which this one is part of (not the center).
	 * @param lockport int 
	 * @param unlockport int
	 * @param other_host String
	 * @param other_lockport int
	 * @param other_unlockport int
	 * @throws IOException if the other server specified is not running.
	 */
	private DLockSrv(int lockport, int unlockport, 
		              String other_host, int other_lockport, int other_unlockport) 
		throws IOException {
		this(lockport,unlockport);
		_client = DLock.getInstance(other_host, other_lockport, other_unlockport);
	}


  /**
   * runs the server.
   * @throws IOException
   * @throws ParallelException
   */
  private void run() throws IOException, ParallelException {
    final Lock lock = new FIFOLock();
		Thread unlockThread = new Thread(new Runnable() {
			public void run() {
				try {
					ServerSocket ss = new ServerSocket(_unlockPort);
					while (true) {
						Socket s = ss.accept();
						DUnlockClient dulc = new DUnlockClient(s,lock);  // also responsible for lock-if-available requests
						dulc.run();  // execute in same thread
					}
				}
				catch (Exception e) {
					System.err.println("DLockSrv.run(): couldn't start the socket-server for unlock requests. Exiting");
					System.exit(-1);
				}
			}
		});
		unlockThread.start();
    ServerSocket ss = new ServerSocket(_lockPort);
    utils.Messenger.getInstance().msg("DLockSrv server started...",0);
    while (true) {
      Socket s = ss.accept();
      DLockClient dlc = new DLockClient(s, lock);
      _pool.execute(dlc);  // if enough clients ask for the lock, this method
			                     // will eventually block, and since it won't then be
			                     // be possible to serve the unlock-request that will
			                     // eventually arrrive, we'll face starvation; this is 
			                     // why a separate unlock port is needed to send the
			                     // unlock requests.
    }
  }


  private synchronized void incrLockCount() {
    ++_lockCount;
  }


  private synchronized long getLockCount() { return _lockCount; }


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; [lockportnumber(7892)] [unlockportnumber(7893)] 
	 * [&lt;otherhost&gt;_&lt;otherlockport&gt;_&lt;otherunlockport&gt;]</CODE>.
   * @param args String[]
   */
  public static void main(String[] args) {
    DLockSrv srv = null;
    if (args.length>2) {
			int lockport = Integer.parseInt(args[0]);
			int unlockport = Integer.parseInt(args[1]);
			String hp = args[2];
			StringTokenizer st = new StringTokenizer(hp,"_");
			String other_host = st.nextToken();
			int other_lockport = Integer.parseInt(st.nextToken());
			int other_unlockport = Integer.parseInt(st.nextToken());
			try {
				srv = new DLockSrv(lockport, unlockport, 
					                 other_host, other_lockport, other_unlockport);
			}
			catch (IOException e) {
				System.err.println("The DLockSrv in location <host>_<lockport>_<unlockport>:"+args[2]+
					                 " must be running before starting this server");
				e.printStackTrace();
				System.exit(-1);
			}
		}
		else if (args.length>0)
      srv = new DLockSrv(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
    else srv = new DLockSrv();
    try {
      srv.run();
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }


  /**
   * inner class, used to handle incoming connections from other JVMs. Not part
	 * of the public API.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class DLockClient implements Runnable {
    private Socket _s;  // socket to client of this DLockSrv
    private Lock _lock;

    private DLockClient(Socket s, Lock l) {
      _s = s;
      _lock = l;
    }


    public void run() {
      ObjectInputStream ois=null;
      ObjectOutputStream oos=null;
			utils.Messenger mger = utils.Messenger.getInstance();
      try {
        oos = new ObjectOutputStream(_s.getOutputStream());
        oos.flush();
        ois = new ObjectInputStream(_s.getInputStream());
        Object inobj = ois.readObject();
				boolean got_lock = false;
        if (inobj instanceof LockRequest) {
          _lock.getLock();
					got_lock = true;
					if (_client!=null) {
						// the lock on _lock above guarantees the _client.getLock() executes
						// atomically
						try {
							_client.getLock();
						}
						catch (ParallelException e) {
							mger.msg("failed to contact and get _client lock", 1);
							_lock.releaseLock();
							got_lock = false;
						}
					}
          incrLockCount();
					try {
						if (got_lock) oos.writeObject(new LockResponse());  // no need for reset
						else oos.writeObject(new FailedReply());
						oos.flush();
					}
					catch (IOException e) {  // client closed connection; release the lock
						if (got_lock) {
							mger.msg("cannot contact client, releasing obtained lock",1);
							if (_client!=null) {
								while (true) {
									try {
										_client.releaseLock();
										break;
									}
									catch (ParallelException e2) {
										// essentially, this DLockSrv is disconnected: cannot contact
										// client, cannot contact _client either, but has the lock
										// from the _client central server... hence the infinite loop
										// waiting to re-connect again
										mger.msg("cannot contact central _client server to release lock", 1);
										try {
											Thread.sleep(100);
										}
										catch (InterruptedException e3) {
											Thread.currentThread().interrupt();
										}
									} 
								}
							}
							_lock.releaseLock();
						}
					}
          mger.msg("Total lock requests so far: "+getLockCount()+
                   " current pool size="+_pool.getNumThreads(),1);
        }
        else {
          mger.msg("unexpected object received",0);
          return;
        }
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      catch (ClassNotFoundException e2) {
        e2.printStackTrace();
      }
      finally {
        try {
          if (ois!=null) ois.close();
          if (oos!=null) oos.close();
          _s.close();
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

  }

	
  /**
   * inner class, used to handle incoming connections from other JVMs. Not part
	 * of the public API.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class DUnlockClient implements Runnable {
    private Socket _s;  // socket to client of this DLockSrv
    private Lock _lock;

		
    private DUnlockClient(Socket s, Lock l) {
      _s = s;
      _lock = l;
    }


    public void run() {
			utils.Messenger mger = utils.Messenger.getInstance();
      ObjectInputStream ois=null;
      ObjectOutputStream oos=null;
      try {
        oos = new ObjectOutputStream(_s.getOutputStream());
        oos.flush();
        ois = new ObjectInputStream(_s.getInputStream());
        Object inobj = ois.readObject();
        if (inobj instanceof UnlockRequest) {
					// first, unlock the central server
					if (_client!=null) {
						// we assume here that the client that has connected to this local
						// server and issues the unlock request, already has the global lock
						// and therefore the _client.releaseLock() also executes atomically
						// the infinite loop is needed as the _client must be somehow 
						// eventually notified of the lock release
						while (true) {
							try {
								_client.releaseLock();
								break;
							}
							catch (ParallelException e) {
								mger.msg("cannot contact _client central server to release lock...", 1);
								try {
									Thread.sleep(100);
								}
								catch (InterruptedException e3) {
									Thread.currentThread().interrupt();
								}
							}
						}
					}
					// then unlock this server
          _lock.releaseLock();
          oos.writeObject(new UnlockResponse());  // no need for reset
          oos.flush();
        }
				else if (inobj instanceof LockIfAvailableRequest) {
          boolean got_local_lock = _lock.getLockIfAvailable();
					boolean got_lock = got_local_lock;
					if (_client!=null && got_lock) {
						// the lock on _lock above guarantees the _client.getLockIf() runs
						// atomically
						try {
							got_lock = _client.getLockIfAvailable();
						}
						catch (ParallelException e) {
							mger.msg("DLockSrv couldn't get its _client 's lock", 1);
							got_lock = false;
						}
					}
          incrLockCount();
					try {
						if (got_lock)
							oos.writeObject(new LockResponse());  // no need for reset
						else oos.writeObject(new LockNotAvailableNowResponse());  // same
						oos.flush();
					}
					catch (IOException e) {  // client closed connection; release the lock
						mger.msg("cannot contact client, releasing obtained lock",1);
						if (_client!=null && got_lock) {
							// infinite loop needed to ensure central _client server eventually
							// releases the lock
							while (true) {
								try {
									_client.releaseLock();
									break;
								}
								catch (ParallelException e2) {
									mger.msg("cannot contact _client central server to release lock...", 1);
									try {
										Thread.sleep(100);
									}
									catch (InterruptedException e3) {
										Thread.currentThread().interrupt();
									}								
								}
							}
						}
						if (got_local_lock) _lock.releaseLock();
					}
          mger.msg("Total lock requests so far: "+getLockCount()+
                   " current pool size="+_pool.getNumThreads(),1);
        }
        else {
          mger.msg("unexpected object received",0);
          return;
        }
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      catch (ClassNotFoundException e2) {
        e2.printStackTrace();
      }
      finally {
        try {
          if (ois!=null) ois.close();
          if (oos!=null) oos.close();
          _s.close();
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

  }

}

