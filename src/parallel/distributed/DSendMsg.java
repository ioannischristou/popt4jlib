package parallel.distributed;

import parallel.*;
import java.io.*;

/**
 * implements a wrapper object for a "sendData()" request from a (local/remote)
 * thread that is transmitted via the socket wire to the
 * <CODE>parallel.distributed.DMsgPassingCoordinatorSrv</CODE>.
 * Not for use as part of the public API (despite the "public" status).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DSendMsg implements DMsgIntf {
  private static final long serialVersionUID = -9037948377409143418L;
  private Integer _fromId;
  private Integer _toId=null;
  private String _coordName;
  private Serializable _data;


  /**
   * wraps a sendData(fromid) request -ie don't care who the receiver is.
   * @param fromid int
   * @param data Serializable data to send
   * @param coordName String the name of the coordinator to be used by the
   * server.
   */
  public DSendMsg(int fromid, Serializable data, String coordName) {
    _fromId = new Integer(fromid);
    _data = data;
    _coordName = coordName;
  }


  /**
   * wraps a sendData(fromid, tiid) request -ie only send data from this sender
   * who declared their tid is fromid, to a thread that will declare their tid
   * to be toid.
   * @param fromid int
   * @param toid int
   * @param data Serializable data to send
   * @param coordName String the name of the coordinator to be used by the
   * server.
   */
  public DSendMsg(int fromid, int toid, Serializable data, String coordName) {
    this(fromid, data, coordName);
    _toId = new Integer(toid);
  }


  /**
   * sends the data and sends back to the JVM's thread that connected to
   * the server via the same socket an <CODE>parallel.distributed.OKReply</CODE>
   * object.
   * @param oos ObjectOutputStream
   * @throws IOException
   * @throws ParallelException
   */
  public void execute(ObjectOutputStream oos) 
    throws IOException, ParallelException {
    MsgPassingCoordinator coord = MsgPassingCoordinator.getInstance(_coordName);
    try {
      if (_toId != null) coord.sendData(_fromId.intValue(), _toId.intValue(),
                                        _data);
      else coord.sendData(_fromId.intValue(), _data);
    }
    catch (ParallelException e) {
      oos.writeObject(new FailedReply());  // no need for oos.reset() first
      oos.flush();
      throw e;
    }
    oos.writeObject(new OKReply());  // no need for oos.reset() first
    oos.flush();
  }
}

