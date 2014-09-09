package analysis;

/**
 * exception class for the analysis package.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntegrationException extends Exception {
  private String _msg;

  /**
   * sole public constructor. Writes the argument in the stderr stream.
   * @param msg String
   */
  public IntegrationException(String msg) {
    _msg = msg;
    System.err.println(msg);
  }
}

