package popt4jlib;

import java.util.*;

/**
 * The class is a wrapper class for FunctionIntf objects, that keeps track of
 * how many times a function has been evaluated.
 * <p>Notes:
 * <ul>
 * <li>2024-03-22: fixed a bug in the <CODE>eval()</CODE> method of the class
 * that would throw <CODE>NullPointerException</CODE> if the params argument was 
 * null.
 * <li>2019-07-01: modified <CODE>eval()</CODE> to return 
 * <CODE>Double.MAX_VALUE</CODE> when the underlying function returns NaN.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2019</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 2.0
 */
public class FunctionBase implements FunctionIntf {
  // private final static long serialVersionUID = -7654946164692599332L;
  private long _evalCount=0;
  private FunctionIntf _f=null;

  /**
   * public constructor resetting the number of evaluations made for the given
   * function f.
   * @param f FunctionIntf
   */
  public FunctionBase(FunctionIntf f) {
    _evalCount=0;
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
		synchronized (this) {
			if (params!=null) {
				Long max_countL = (Long) params.get("maxfuncevalslimit");
				if (max_countL!=null && max_countL.longValue() <= _evalCount)
					return Double.MAX_VALUE;  // stop the function evaluation process
			}
			++_evalCount;
		}
    double y = _f.eval(arg, params);
		if (Double.isNaN(y)) return Double.MAX_VALUE;  // itc-20190701: fix NaN vals
		return y;
  }


  /**
   * return the total number of times the <CODE>eval(x,params)</CODE> method
   * was called.
   * @return long
   */
  synchronized public long getEvalCount() { return _evalCount; }


  /**
   * increment the total evaluations count. Not synchronized, and must therefore
	 * always be invoked from within a synchronized block.
   */
  protected void incrCount() { ++_evalCount; }

	
	/**
	 * get the underlying function to be evaluated.
	 * @return FunctionIntf
	 */
	protected FunctionIntf getFunction() {
		return _f;
	}
}

