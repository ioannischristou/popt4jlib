package parallel.distributed;

import java.io.*;

/**
 * indicates to remote client a failure in a previous request.
 * Not for use as part of the public API (despite the "public" status).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FailedReply implements Serializable {
  private static final long serialVersionUID = 6980423120040002874L;
  public FailedReply() {
  }


  public String toString() {
    return "FailedReply";
  }

}

