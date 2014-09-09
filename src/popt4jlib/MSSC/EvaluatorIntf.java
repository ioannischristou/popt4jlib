package popt4jlib.MSSC;

/**
 * interface for MSSC evaluations.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface EvaluatorIntf {
  /**
   * evaluates a particular Clusterer object's job (that must have already
   * occured prior to this call).
   * @param cl ClustererIntf
   * @throws ClustererException
   * @return double
   */
  public double eval(ClustererIntf cl) throws ClustererException;
}

