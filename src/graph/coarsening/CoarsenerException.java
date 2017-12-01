package graph.coarsening;

/**
 * Exception class specific to <CODE>graph.coarsening</CODE> package classes.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class CoarsenerException extends Exception{
  /**
	 * single constructor, outputs in the stderr the message passed as input.
	 * @param msg String
	 */
	public CoarsenerException(String msg) {
    System.err.println(msg);
    System.err.flush();
  }
}
