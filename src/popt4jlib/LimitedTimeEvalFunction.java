package popt4jlib;

import parallel.*;
import java.util.*;

/**
 * The class is a wrapper class for FunctionIntf objects, that forces the
 * function to execute in a different thread than the calling thread, allowing
 * only up to a certain amount of time for execution.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LimitedTimeEvalFunction implements FunctionIntf {
  private long _evalCount=0;
  private long _successfullEvalCount=0;
  private LimitedTimeTaskExecutor _executor=null;
  private FunctionIntf _f=null;


  /**
   * public constructor, setting up a <CODE>LimitedTimeTaskExecutor</CODE>
   * object that allows running tasks for up to millis2wait milliseconds in
   * different threads in the executor's thread-pool.
   * @param f FunctionIntf
   * @param millis2wait long
   */
  public LimitedTimeEvalFunction(FunctionIntf f, long millis2wait) {
    _executor = new LimitedTimeTaskExecutor(millis2wait);
    _f = f;
  }


  /**
   * the function evaluation method of the class: creates a new
   * <CODE>FunctionEvaluationTask</CODE> object and submits it to the
   * <CODE>LimitedTimeTaskExecutor</CODE> object associated with this object for
   * execution. This method is guaranteed to return in no more than millis2wait
   * milliseconds (the second argument passed in the object's constructor). If
   * the evaluation of the underlying function takes more time than the maximum
   * allowed time, then this method will return <CODE>Double.MAX_VALUE</CODE>
   * @param arg Object the argument of the function to be evaluated.
   * @param params HashMap the parameters of the function to be evaluated.
   * @return double the function value or <CODE>Double.MAX_VALUE</CODE> if the
   * evaluation takes too long.
   */
  public double eval(Object arg, HashMap params) {
    incrCount();
    FunctionEvaluationTask t = new FunctionEvaluationTask(_f, arg, params);
    if (_executor.execute(t)) incrSucEvalCount();
    return t.getObjValue();
  }


  /**
   * return the total number of calls made to <CODE>eval(arg,params)</CODE>
   * @return long
   */
  synchronized public long getEvalCount() { return _evalCount; }


  /**
   * return the total number of successful (on-time) evaluations
   * @return long
   */
  synchronized public long getSucEvalCount() { return _successfullEvalCount; }


  /**
   * auxiliary method
   */
  synchronized private void incrCount() { ++_evalCount; }


  /**
   * auxiliary method
   */
  synchronized private void incrSucEvalCount() { ++_successfullEvalCount; }

}

