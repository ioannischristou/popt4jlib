package parallel;

/**
 * generic exception class for the parallel package.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ParallelException extends Exception {
	/**
	 * public constructor prints msg to the <CODE>System.err</CODE> stream.
	 * @param msg String
	 */
  public ParallelException(String msg) {
    System.err.println(msg);
    System.err.flush();
  }
}
