package popt4jlib;

import java.util.*;

/**
 * The class is a wrapper class for FunctionIntf objects, that keeps track of
 * how many times a function has been evaluated, plus it forces threads asking
 * for function evaluation to execute sequentially.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ReentrantFunctionBase implements FunctionIntf {
  private long _evalCount=0;
  private FunctionIntf _f=null;

  /**
   * public constructor, setting up the evaluation count to zero, maintaining
   * a reference to the function passed in as the argument.
   * @param f FunctionIntf
   */
  public ReentrantFunctionBase(FunctionIntf f) {
    _evalCount=0;
    _f = f;
  }


  /**
   * force function to be reentrant as no two threads can enter the eval()
   * method simultaneously
   * @param arg Object
   * @param params HashMap
   * @return double
   */
  public synchronized double eval(Object arg, HashMap params) {
    ++_evalCount;
    return _f.eval(arg, params);
  }


  /**
   * get the total number of times the <CODE>eval(arg,params)</CODE> method was
   * called so far.
   * @return long
   */
  synchronized public long getEvalCount() { return _evalCount; }

}

