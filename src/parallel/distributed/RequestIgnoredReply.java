package parallel.distributed;

import java.io.*;

/**
 * indicates that the request to which this is the response, was ignored.
 * Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RequestIgnoredReply implements Serializable {
  //private static final long serialVersionUID = ...L;

  public RequestIgnoredReply() {
  }

  public String toString() {
    return "RequestIgnoredReply";
  }

}

