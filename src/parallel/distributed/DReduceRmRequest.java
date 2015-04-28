package parallel.distributed;

import parallel.*;
import java.io.*;

/**
 * class implementing a DReducer Remove Request for the associated thread that
 * called the relevant <CODE>DReducer.removeCurrentThread()</CODE> method. Not
 * for use as part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DReduceRmRequest implements DMsgIntf {
  //private final static long serialVersionUID = -2489843289973313981L;
  private String _bname;

  /**
   * public constructor.
   * @param bname String
   */
  public DReduceRmRequest(String bname) {
    _bname = bname;
  }


  public void execute(ObjectOutputStream oos) throws ParallelException, IOException {
    try {
      ReduceOpBase.removeCurrentThread(_bname);
      oos.writeObject(new OKReply());
      oos.flush();
    }
    catch (ParallelException e) {
      oos.writeObject(new FailedReply());
      oos.flush();
      throw e;
    }
  }

  public String toString() {
    return "DReduceRmRequest(_bName="+_bname+")";
  }
}

