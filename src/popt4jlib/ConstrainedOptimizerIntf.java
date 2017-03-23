package popt4jlib;

import utils.*;

/**
 * the interface every optimizer (for constrained optimization) must implement.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface ConstrainedOptimizerIntf extends OptimizerIntf {

  /**
   * the unique method every optimizer for constrained optimization must
   * implement.
   * @param f FunctionIntf
	 * @param c VecFunctionIntf the inequality constraints of the problem: each
	 * component of this function must be greater than or equal to zero
	 * @param e VecFunctionIntf the equality constraints of the problem: each
	 * component of this function must be equal to zero
   * @throws OptimizerException if the process fails
   * @return PairObjDouble returning the arg-min together with the min value 
	 * found
   */
  public PairObjDouble minimize(FunctionIntf f, 
		                            VecFunctionIntf c, 
																VecFunctionIntf e) 
		throws OptimizerException;
}
