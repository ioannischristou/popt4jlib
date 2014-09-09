package parallel.distributed;

import java.io.*;

/**
 * indicates an "OK" response to a previous request.
 * Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class OKReply implements Serializable {
  private static final long serialVersionUID = 8184476063928530086L;

  public OKReply() {
  }

  public String toString() {
    return "OKReply";
  }

}

