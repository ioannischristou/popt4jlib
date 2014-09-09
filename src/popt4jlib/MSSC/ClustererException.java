package popt4jlib.MSSC;

/**
 * Defines a checked exception that may be thrown from clustering algorithms.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ClustererException extends Exception {

  /**
   * public constructor writes the msg in stderr.
   * @param msg String
   */
  public ClustererException(String msg) {
    System.err.println(msg);
    System.err.flush();
  }
}

