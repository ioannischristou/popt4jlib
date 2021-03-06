package parallel.distributed;

import parallel.*;
import java.net.*;
import java.io.*;


/**
 * server class implementing distributed Message-Passing between inter-process
 * (and intra-process) threads. Threads must know their own id and that they
 * are unique with respect to any given coordinator name chosen. The difference
 * between this class and the <CODE>DMsgPassingCoordinatorSrv</CODE> is that
 * each socket connection is treated as a long-lasting one, and is the
 * responsibility of the clients to close the connection and release server
 * resources. Also, messages are active, in that clients may send DMsgIntf
 * objects that may be different from DSendMsg/DRecvMsg objects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DActiveMsgPassingCoordinatorLongLivedConnSrv {
  private int _port = 7895;  // default port
  private long _countConns=0;

  /**
   * constructor specifying the port the server will listen to, and
   * the max. number of threads in the thread-pool.
   * @param port int if &lt; 1024, the number 7895 is used.
   */
  DActiveMsgPassingCoordinatorLongLivedConnSrv(int port) {
    if (port >= 1024)
      _port = port;
  }


  /**
   * enters an infinite loop, listening for socket connections, and handling
   * the incoming sockets in new threads.
   * @throws IOException
   * @throws ParallelException
   */
  void run() throws IOException, ParallelException {
    ServerSocket ss = new ServerSocket(_port);
    while (true) {
      utils.Messenger.getInstance().msg("DActiveMsgPassingCoordinatorLongLivedConnSrv: waiting for socket connection on port "+_port,0);
      Socket s = ss.accept();
      ++_countConns;
      utils.Messenger.getInstance().msg("DActiveMsgPassingCoordinatorLongLivedConnSrv: Total "+
                                        _countConns+" socket connections arrived.", 1);
      handle(s, _countConns);
    }
  }


  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; parallel.distributed.DActiveMsgPassingCoordinatorLongLivedConnSrv [port(7895)]</CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    int port = -1;
    DActiveMsgPassingCoordinatorLongLivedConnSrv srv=null;
    if (args.length>0) {
      port = Integer.parseInt(args[0]);
    }
    srv = new DActiveMsgPassingCoordinatorLongLivedConnSrv(port);
    try {
      srv.run();
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }


  private void handle(Socket s, long connum) {
    DMPCLLCThread t = new DMPCLLCThread(s, connum);
    t.start();
  }


  /**
   * inner helper class
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class DMPCLLCThread extends Thread {
    private Socket _s;
    private long _conId;


    DMPCLLCThread(Socket s, long conid) {
      _s = s;
      _conId = conid;
    }


    public void run() {
      ObjectInputStream ois = null;
      ObjectOutputStream oos = null;
      try {
        oos = new ObjectOutputStream(_s.getOutputStream());
        oos.flush();
        ois = new ObjectInputStream(_s.getInputStream());
        boolean cont = true;
        while (cont) {
          try {
            DMsgIntf msg = (DMsgIntf) ois.readObject(); // read a msg (DMsgIntf object)
            msg.execute(oos);
          }
          catch (IOException e) {
            System.err.println("DMPCLLCThread.run(): socket connection closed, exiting");
            System.err.flush();
            cont = false;
          }
        }
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
      finally {
        try {
          if (oos != null) {
            oos.flush();
            oos.close();
          }
          if (ois != null) ois.close();
          _s.close();
        }
        catch (IOException e) {
          e.printStackTrace();  // shouldn't get here
        }
      }
    }
  }
}

