package popt4jlib;

/**
 * Exception class used by most optimization algorithms in the popt4jlib package
 * to indicate abnormal execution of some task.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class OptimizerException extends Exception {
  public OptimizerException(String msg) {
    System.err.println(msg);
    System.err.flush();
  }
}
