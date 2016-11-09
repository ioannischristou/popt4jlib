package popt4jlib;

import utils.*;

/**
 * the interface every optimizer (for unconstrained optimization) must implement.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface OptimizerIntf {

  /**
   * the unique method every optimizer for unconstrained optimization must
   * implement.
   * @param f FunctionIntf
   * @throws OptimizerException
   * @return PairObjDouble
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException;
}
