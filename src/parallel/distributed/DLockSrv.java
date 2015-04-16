package parallel.distributed;

import parallel.*;
import java.io.*;
import java.net.*;


/**
 * class implements a server for distributed locking functionality. Multiple
 * JVMs can connect to this server and ask for the lock corresponding to this
 * server. Each client may as well be multi-threaded.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DLockSrv {
  private int _port=7892;
  private long _lockCount=0;
  private DynamicAsynchTaskExecutor _pool;


  /**
   * no-arg constructor.
   */
  public DLockSrv() {
    try {
      _pool = DynamicAsynchTaskExecutor.newDynamicAsynchTaskExecutor(1, 1000);
    }
    catch (ParallelException e) {
      e.printStackTrace();  // cannot happen
    }
  }


  /**
   * constructor with port number specification.
   * @param port int the port number to listen to
   */
  public DLockSrv(int port) {
    this();
    _port = port;
  }


  /**
   * runs the server.
   * @throws IOException
   * @throws ParallelException
   */
  public void run() throws IOException, ParallelException {
    ServerSocket ss = new ServerSocket(_port);
    Lock lock = new FIFOLock();
    utils.Messenger.getInstance().msg("DLockSrv server started...",0);
    while (true) {
      Socket s = ss.accept();
      DLockClient dlc = new DLockClient(s, lock);
      _pool.execute(dlc);
    }
  }


  synchronized void incrLockCount() {
    ++_lockCount;
  }


  synchronized long getLockCount() { return _lockCount; }


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; [portnumber(7892)]</CODE>.
   * @param args String[]
   */
  public static void main(String[] args) {
    DLockSrv srv = null;
    if (args.length>0)
      srv = new DLockSrv(Integer.parseInt(args[0]));
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
   * inner class, used to handle incoming connections from other JVMs.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class DLockClient implements Runnable {
    private Socket _s;
    private Lock _lock;

    public DLockClient(Socket s, Lock l) {
      _s = s;
      _lock = l;
    }


    public void run() {
      ObjectInputStream ois=null;
      ObjectOutputStream oos=null;
      try {
        oos = new ObjectOutputStream(_s.getOutputStream());
        oos.flush();
        ois = new ObjectInputStream(_s.getInputStream());
        Object inobj = ois.readObject();
        if (inobj instanceof LockRequest) {
          _lock.getLock();
          incrLockCount();
          oos.writeObject(new LockResponse());
          oos.flush();
          utils.Messenger.getInstance().msg("Total lock requests so far: "+getLockCount()+
                                            " current pool size="+_pool.getNumThreads(),1);
        }
        else if (inobj instanceof UnlockRequest) {
          _lock.releaseLock();
          oos.writeObject(new UnlockResponse());
          oos.flush();
        }
        else {
          utils.Messenger.getInstance().msg("unexpected object received",0);
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

