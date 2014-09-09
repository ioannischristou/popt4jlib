package popt4jlib;

import parallel.TaskObject;
import java.util.Hashtable;
import java.io.Serializable;


/**
 * auxiliary class used when the user requests function evaluations to be
 * executed in separate threads in a thread-pool for a limited time so as to
 * avoid "locking-up" the minimization process by function evaluations that
 * take too long to complete (such evaluations are "lost" by the minimization
 * process even if they eventually complete). For more details, see the
 * <CODE>LimitedTimeTaskExecutor</CODE> class in the parallel package, and the
 * <CODE>LimitedTimeEvalFunction</CODE> class in this package. The class and its
 * methods are of course thread-safe (as long as the underlying function object
 * that implements the FunctionIntf interface to be evaluated also happens to be
 * thread-safe).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FunctionEvaluationTask implements TaskObject {
  private final static long serialVersionUID = -7654946164692599332L;
  private FunctionIntf _f;
  private Object _arg;
  private Hashtable _params;
  private double _val=Double.MAX_VALUE;
  private boolean _isDone=false;

  /**
   * constructor
   * @param f FunctionIntf
   * @param arg Object
   * @param params Hashtable
   */
  public FunctionEvaluationTask(FunctionIntf f, Object arg, Hashtable params) {
    _f = f;
    _arg = arg;
    _params = new Hashtable(params);  // keep FindBugs happy
    // interestingly, FindBugs doesn't complain about the previous asgnmnt...
  }


  /**
   * the main method calls the <CODE>f.eval(arg, params)</CODE> method of the
   * arument <CODE>f</CODE> passed in the constructor argument, with argument
   * the <CODE>arg</CODE> passed in and the parameters <CODE>params</CODE> all
   * passed in the constructor. After the function <CODE>f</CODE> is evaluated
   * the method <CODE>setObjValue(val)</CODE> with value the result of the
   * evaluation is executed, so that it may be gotten from subsequent call to
   * <CODE>getObjValue()</CODE>. Also, afterwards, <CODE>isDone()<CODE> returns
   * true.
   * @return this
   */
  public Serializable run() {
    double val = _f.eval(_arg, _params);
    setObjValue(val);
    setDone(true);
    return this;
  }


  /**
   * return true iff the <CODE>run()</CODE> method has run to its completion.
   * @return boolean
   */
  public synchronized boolean isDone() { return _isDone; }


  /**
   * return the value of the evaluation of the function.
   * @return double
   */
  public synchronized double getObjValue() { return _val; }


  /**
   * copy state from the input argument.
   * @param t TaskObject
   * @throws IllegalArgumentException
   */
  public synchronized void copyFrom(TaskObject t) throws IllegalArgumentException {
    if (t instanceof FunctionEvaluationTask == false)
      throw new IllegalArgumentException("TaskObject argument is not a FunctionEvaluationTask.");
    FunctionEvaluationTask t2 = (FunctionEvaluationTask) t;
    _arg = t2._arg;
    _isDone = t2._isDone;
    _params = t2._params;
    _f = t2._f;
    _val = t2._val;
  }


  /**
   * set the objective value.
   * @param v double
   */
  synchronized void setObjValue(double v) { _val = v; }


  /**
   * indicate the evaluation is done (or the opposite).
   * @param v boolean
   */
  synchronized void setDone(boolean v) { _isDone = v; }

}

