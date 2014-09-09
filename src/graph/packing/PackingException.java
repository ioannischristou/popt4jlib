package graph.packing;

/**
 * Class used to indicate exceptions in the packing package!.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PackingException extends Exception {
  /**
   * constructs a PackingException object that prints its message in the stderr.
   * @param msg String
   */
  public PackingException(String msg) {
    System.err.println(msg);
    System.err.flush();
  }
}

