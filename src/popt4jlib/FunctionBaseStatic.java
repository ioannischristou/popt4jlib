package popt4jlib;

import java.util.*;

/**
 * The class is a wrapper class for FunctionIntf objects, that keeps track of
 * how many times a function has been evaluated. The class works as the class
 * <CODE>FunctionBase</CODE>, only the counter and related methods are all 
 * static, counting ALL evaluations of any function that has been wrapped in 
 * this wrapper (regardless of function type).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2024</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FunctionBaseStatic implements FunctionIntf {
  // private final static long serialVersionUID = -7654946164692599332L;
  private static long _evalCount=0;
  private FunctionIntf _f=null;

  /**
   * public constructor resetting the number of evaluations made for the given
   * function f.
   * @param f FunctionIntf
   */
  public FunctionBaseStatic(FunctionIntf f) {
    _f = f;
  }


  /**
   * increments the evaluation count and calls the <CODE>eval(arg,params)</CODE>
   * method of the function passed in the constructor.
   * @param arg Object
   * @param params HashMap
   * @return double notice however that the evaluation will always return 
	 * <CODE>Double.MAX_VALUE</CODE> if this method has been invoked as many 
	 * times (or more) as specified in the value for the key "maxfuncevalslimit"
	 * in the params argument; if no such key exists in the params, then the 
	 * evaluations will never stop.
   */
  public double eval(Object arg, HashMap params) {
		if (params!=null) {
			Long max_countL = (Long) params.get("maxfuncevalslimit");
			if (max_countL!=null && max_countL.longValue() <= getEvalCount())
				return Double.MAX_VALUE;  // stop the function evaluation process
		}
		incrCount();
    double y = _f.eval(arg, params);
		if (Double.isNaN(y)) return Double.MAX_VALUE;
		return y;
  }


  /**
   * return the total number of times the <CODE>eval(x,params)</CODE> method
   * was called.
   * @return long
   */
  synchronized static public long getEvalCount() { return _evalCount; }


  /**
   * increment the total evaluations count.
   */
  protected synchronized static void incrCount() { ++_evalCount; }

	
	/**
	 * get the underlying function to be evaluated.
	 * @return FunctionIntf
	 */
	protected FunctionIntf getFunction() {
		return _f;
	}
}

