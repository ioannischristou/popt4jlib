package parallel.distributed;

import java.io.Serializable;

/**
 * indicates a succesful grant for a distributed lock.
 * Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class LockResponse implements Serializable {
  private final static long serialVersionUID = 4474466566392221232L;

  public LockResponse() {
  }
}

