package popt4jlib;

import parallel.TaskObject;
import java.util.HashMap;
import java.io.Serializable;


/**
 * auxiliary class used when the user requests function evaluations to be
 * executed in separate threads in a thread-pool for a limited time so as to
 * avoid "locking-up" the minimization process by function evaluations that
 * take too long to complete (such evaluations are "lost" by the minimization
 * process even if they eventually complete). The class is also useful for 
 * wrapping up function evaluation requests to be executed in a remote JVM
 * (distributed). For more details, see the
 * <CODE>parallel.LimitedTimeTaskExecutor</CODE> class and the
 * <CODE>LimitedTimeEvalFunction</CODE> class in this package. The class and its
 * methods are of course thread-safe (as long as the underlying function object
 * that implements the FunctionIntf interface to be evaluated also happens to be
 * thread-safe).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FunctionEvaluationTask implements TaskObject {
  // private final static long serialVersionUID = -7654946164692599332L;
  private FunctionIntf _f;
  private Object _arg;
  private HashMap _params;
  private double _val=Double.MAX_VALUE;
  private boolean _isDone=false;

  /**
   * sole constructor. When the constructed object is to be used to submit a
	 * request for distributed computation to a remote JVM, all args passed in 
	 * this constructor must implement the <CODE>java.io.Serializable</CODE>
	 * interface.
   * @param f FunctionIntf
   * @param arg Object 
   * @param params HashMap
   */
  public FunctionEvaluationTask(FunctionIntf f, Object arg, HashMap params) {
    _f = f;
    _arg = arg;
    _params = new HashMap(params);  // keep FindBugs happy
  }


  /**
   * the main method calls the <CODE>f.eval(arg, params)</CODE> method of the
   * arument <CODE>f</CODE> passed in the constructor argument, with argument
   * the <CODE>arg</CODE> passed in and the parameters <CODE>params</CODE> all
   * passed in the constructor. After the function <CODE>f</CODE> is evaluated
   * the method <CODE>setObjValue(val)</CODE> with value the result of the
   * evaluation is executed, so that it may be gotten from subsequent call to
   * <CODE>getObjValue()</CODE>. Also, afterwards, <CODE>isDone()</CODE> returns
   * true.
   * @return this
   */
  public Serializable run() {
    double val = _f.eval(_arg, _params);
    synchronized (this) {
			setObjValue(val);
			setDone(true);
		}
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
	 * return a String representation of this object. Used for debugging.
	 * @return String
	 */
	public String toString() {
		String res = "FET[";
		res += _f.getClass().getName();
		res += "("+_arg+",(";
		// add params pairs
		java.util.Iterator it = _params.keySet().iterator();
		while (it.hasNext()) {
			String k = (String) it.next();
			Object v = _params.get(k);
			res += "<"+k+","+v+">";
			if (it.hasNext()) res += ",";
		}
		res += "))="+_val+(_isDone ? " (" : " (!")+"done)"+"]";
		return res;
	}

	
	/**
	 * return the objective function.
	 * @return FunctionIntf
	 */
	protected FunctionIntf getFunction() {
		return _f;
	}
	
	
	/**
	 * return the argument.
	 * @return Object
	 */
	protected Object getArg() {
		return _arg;
	}

	
	/**
	 * return the parameters with which this object was constructed.
	 * @return HashMap
	 */
	protected HashMap getParams() {
		return _params;
	}
	
	
  /**
   * set the objective value.
   * @param v double
   */
  protected void setObjValue(double v) { _val = v; }


  /**
   * indicate the evaluation is done (or the opposite).
   * @param v boolean
   */
  protected void setDone(boolean v) { _isDone = v; }

}

