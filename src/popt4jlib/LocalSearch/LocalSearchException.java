package popt4jlib.LocalSearch;

/**
 * encapsulates exceptions that a Local-Search algorithm may throw.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LocalSearchException extends Exception {
  private String _msg;

  /**
   * public constructor writes the msg in stderr.
   * @param msg String
   */
  public LocalSearchException(String msg) {
    _msg = msg;
    System.err.println(msg);
  }
}

