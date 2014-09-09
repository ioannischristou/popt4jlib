package parallel.distributed;

import java.io.*;
import parallel.*;

/**
 * DMsgIntf objects are meant to transport over Socket wires sendData() and/or
 * recvData() requests, as those are defined in the
 * <CODE>parallel.MsgPassingCoordinator</CODE> class, which is in fact the class
 * used to "transfer" message data between the threads in the
 * <CODE>parallel.distributed.DMsgPassingCoordinatorSrv</CODE> class.
 * Not part of the public API (despite the public status).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface DMsgIntf extends Serializable {
  /**
   * if the object is a DSendMsg calls the <CODE>sendData()</CODE> method on an
   * appropriate <CODE>parallel.MsgPassingCoordinator</CODE> and once the data
   * are safely sent, it sends back to the other end of the connecting socket an
   * <CODE>parallel.distributed.OKReply</CODE> object,
   * else if it's a DRecvMsg, calls the <CODE>recvData()</CODE> and once data
   * are received, sends them to the other end of the socket.
   * @param oos ObjectOutputStream
   * @throws ParallelException
   * @throws IOException
   */
  public void execute(ObjectOutputStream oos) throws ParallelException, IOException;
}

