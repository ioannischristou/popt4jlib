package parallel;

import java.io.Serializable;

/**
 * defines a "no-op" TaskObject.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class NoOpTask implements TaskObject {
	// private final static long serialVersionUID = -5265604222839083451L;
	
  /**
   * returns null immediately.
   * @return null
   */
  public Serializable run() {
		return null;
	}


  /**
   * return always true.
   * @return boolean
   */
  public boolean isDone() {
		return true;
	}


  /**
   * always throws UnsupportedOperationException.
   * @param other TaskObject not used.
   * @throws UnsupportedOperationException
   */
  public void copyFrom(TaskObject other) {
		throw new UnsupportedOperationException("not supported");
	}
}
