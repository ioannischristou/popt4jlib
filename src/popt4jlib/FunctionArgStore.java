package popt4jlib;

import java.util.HashMap;
import java.util.HashSet;

/**
 * The class is a wrapper class for FunctionIntf objects, extending FunctionBase
 * in that every argument it is invoked with, is stored in a set; thus any 
 * evaluation requests for arguments that have been seen before are honored 
 * while any other request returns <CODE>Double.MAX_VALUE</CODE> once the limit
 * on the allowed number of function evaluations is hit.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FunctionArgStore extends FunctionBase {
	private HashSet _args;
	
	/**
	 * sole constructor.
	 * @param f FunctionIntf the function to wrap.
	 */
	public FunctionArgStore(FunctionIntf f) {
		super(f);
		_args = new HashSet();
	}
	

  /**
   * acts as the <CODE>eval(arg,params)</CODE> method of its parent class, with
	 * the exception that if the argument arg has been seen before, the underlying
	 * function is always evaluated, regardless of any limits set.
   * @param arg Object
   * @param params HashMap
   * @return double 
   */
  public double eval(Object arg, HashMap params) {
    Long max_countL = (Long) params.get("maxfuncevalslimit");
		synchronized (this) {
			if (max_countL!=null && max_countL.longValue() <= getEvalCount()) {
				if (!_args.contains(arg))
					return Double.MAX_VALUE;  // stop the function evaluation process
				else return getFunction().eval(arg, params);
			}
			incrCount();
			_args.add(arg);
		}
    return getFunction().eval(arg, params);
  }
}

