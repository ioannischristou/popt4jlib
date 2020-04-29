package popt4jlib;

import java.util.HashMap;
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
   * @return PairObjDouble returning the arg-min together with the min value 
	 * found
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException;
	

	/**
	 * set this object's parameters needed for the optimization process. 
	 * For optimizers that implement the <CODE>LocalOptimizerIntf</CODE>, the 
	 * parameter set must always contain a starting point from which to start the
	 * local optimization process (with key that is usually "[X.]x0" where X is an 
	 * abbreviated name of the method employed).
	 * @param params HashMap
	 * @throws OptimizerException 
	 */
  public void setParams(HashMap params) throws OptimizerException;

}
