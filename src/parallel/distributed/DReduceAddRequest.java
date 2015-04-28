package parallel.distributed;

import parallel.*;
import java.io.*;

/**
 * implements a DMsgIntf object that asks a DReduceSrv to add the corresponding
 * thread to its pool of participants in the DReducer. Not for use as part of
 * the normal public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DReduceAddRequest implements DMsgIntf {
  //private final static long serialVersionUID = -5265604222839083451L;
  private String _bname;


  /**
   * constructor.
   * @param bname String name of the reducer in the receiving DReduceSrv.
   */
  public DReduceAddRequest(String bname) {
    _bname = bname;
  }


  /**
   * adds the originating thread (via sockets) to the DReducer object named in
   * the constructor.
   * @param oos ObjectOutputStream
   * @throws ParallelException
   * @throws IOException
   */
  public void execute(ObjectOutputStream oos) throws ParallelException, IOException {
    ReduceOpBase.addThread(_bname, Thread.currentThread());
    // ok, handling thread on server added
    oos.writeObject(new OKReply());
    oos.flush();
  }

  public String toString() {
    return "DReduceAddRequest("+_bname+")";
  }

}

