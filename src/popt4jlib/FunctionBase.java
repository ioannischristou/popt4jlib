package popt4jlib;

import java.util.*;

/**
 * The class is a wrapper class for FunctionIntf objects, that keeps track of
 * how many times a function has been evaluated.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FunctionBase implements FunctionIntf {
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
   * @return double
   */
  public double eval(Object arg, HashMap params) {
    Long max_countL = (Long) params.get("maxfuncevalslimit");
    if (max_countL!=null && max_countL.longValue() <= getEvalCount())
      return Double.MAX_VALUE;  // stop the function evaluation process
    incrCount();
    return _f.eval(arg, params);
  }


  /**
   * return the total number of times the <CODE>eval(x,params)</CODE> method
   * was called.
   * @return long
   */
  synchronized public long getEvalCount() { return _evalCount; }


  /**
   * increment the total evaluations count.
   */
  synchronized private void incrCount() { ++_evalCount; }

}

