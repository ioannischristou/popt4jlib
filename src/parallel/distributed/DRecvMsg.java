package parallel.distributed;

import parallel.*;
import java.io.*;

/**
 * implements a wrapper object for a "recvData()" request from a (local/remote)
 * thread that is transmitted via the socket wire to the
 * <CODE>parallel.distributed.DMsgPassingCoordinatorSrv</CODE>.
 * Not for use as part of the public API (despite the "public" status).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DRecvMsg implements DMsgIntf {
  private static final long serialVersionUID = 6033014013165329991L;
  private Integer _fromId=null;
  private Integer _myId;
  private String _coordName;


  /**
   * wraps a recvData(myid) request -i.e. don't care who the sender is.
   * @param myid int id of the thread requesting to receive some data.
   * @param coordName String name of the coordinator to be used by the server.
   */
  public DRecvMsg(int myid, String coordName) {
    _myId = new Integer(myid);
    _coordName = coordName;
  }


  /**
   * wraps a recvData(myid, fromid) request -i.e. only accept data from a sender
   * who declared their tid is fromid.
   * @param myid int id of the thread requesting to receive data.
   * @param fromid int declared id of the thread from which the thread with
   * declared id=myid is interested in receiving data only.
   * @param coordName String name of the coordinator to be used by the server.
   */
  public DRecvMsg(int myid, int fromid, String coordName) {
    this(myid, coordName);
    _fromId = new Integer(fromid);
  }


  /**
   * reads the data and sends them back to the JVM's thread that connected to
   * the server, via the same socket.
   * @param oos ObjectOutputStream
   * @throws ParallelException
   * @throws IOException
   */
  public void execute(ObjectOutputStream oos) throws ParallelException, IOException {
    MsgPassingCoordinator coord = MsgPassingCoordinator.getInstance(_coordName);
    Serializable data = null;
    try {
      if (_fromId != null) data = (Serializable) coord.recvData(_myId.intValue(), _fromId.intValue());
      else data = (Serializable) coord.recvData(_myId.intValue());
			oos.reset();  // force object to be written anew
      oos.writeObject(data);
      oos.flush();
    }
    catch (ParallelException e) {  // indicate failure to remote client
      oos.writeObject(new FailedReply());  // no need for oos.reset() first
      oos.flush();
      throw e;
    }
  }
}

