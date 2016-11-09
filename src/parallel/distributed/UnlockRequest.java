package parallel.distributed;

import java.io.Serializable;

/**
 * represents a request for releasing a distributed lock. Not part of the
 * public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class UnlockRequest implements Serializable {
  private final static long serialVersionUID = -2593042151290658868L;

  public UnlockRequest() {
  }
}

