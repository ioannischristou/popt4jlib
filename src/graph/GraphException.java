package graph;

/**
 * Class represents  exceptions in graph-related methods in the graph package.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class GraphException extends Exception {
	/**
	 * sole public constructor.
	 * @param msg String
	 */
  public GraphException(String msg) {
    System.err.println(msg);
    System.err.flush();
  }
}
