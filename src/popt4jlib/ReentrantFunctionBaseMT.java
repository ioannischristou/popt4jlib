package popt4jlib;

import java.util.*;


/**
 * The class is a wrapper class for FunctionIntf objects, that keeps track of
 * how many times a function has been evaluated, plus it forces threads asking
 * for function evaluation to execute using a different FunctionIntf object,
 * so that no calls from different threads execute on the same FunctionIntf
 * object (this way, if the eval() call modifies only NON-static data members
 * of the FunctionIntf object, there is no data race danger).
 * <p>Notice that the underlying FunctionIntf object must have a no-arg
 * constructor otherwise an exception will be thrown as no such constructor will
 * be found in order to populate the array of FunctionIntf objects that this
 * class' objects maintain.</p>
 * <p>Further Notes:
 * <ul>
 * <li>20190701: modified <CODE>eval()</CODE> to return 
 * <CODE>Double.MAX_VALUE</CODE> when the underlying function returns NaN.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2019</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ReentrantFunctionBaseMT implements FunctionIntf {
  private long _evalCount=0;
  private FunctionIntf[] _f=null;


  /**
   * public constructor creates nt new f objects (using reflection) and stores
   * them in an array. Threads with a (global) id in the set {0,...nt-1} calling
   * the eval(x,params) method of this object (with the params map containing an
   * entry &lt;"thread.id", Integer n&gt; so that <CODE>0 &le; n.intValue() &lt; nt </CODE>)
   * will run concurrently, but threads with different ids (specified in the
   * params map) will run sequentially.
   * @param f FunctionIntf
   * @param nt int the number does not have to be equal to the total number of
   * threads in the system, and can be anything greater than or equal to zero.
   */
  public ReentrantFunctionBaseMT(FunctionIntf f, int nt) {
    _evalCount=0;
    _f = new FunctionIntf[nt+1];
    Class fc = f.getClass();
    for (int i=0; i<=nt; i++) {
      try {
        _f[i] = (FunctionIntf) fc.newInstance();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }


  /**
   * force function to be reentrant, as long as no STATIC data members are
   * modified during evaluations, as no two threads can enter the eval(x,p)
   * method on the same object simultaneously.
   * @param arg Object
   * @param params HashMap
   * @return double
   */
  public double eval(Object arg, HashMap params) throws IllegalArgumentException {
    incrEvalCount();
    Integer id = (Integer) params.get("thread.id");
    if (id==null || id.intValue()==-1 || id.intValue()>=_f.length-1) {
      synchronized (this) {
        double v = _f[_f.length - 1].eval(arg, params);
				if (Double.isNaN(v)) return Double.MAX_VALUE;  // itc-20190701: fix NaN
        return v;
      }
    }
    double v = _f[id.intValue()].eval(arg, params);
		if (Double.isNaN(v)) return Double.MAX_VALUE;  // itc-20190701: fix NaN
		return v;
  }


  /**
   * return the total number of calls made to <CODE>eval(x,p)</CODE>
   * @return long
   */
  synchronized public long getEvalCount() { return _evalCount; }


  /**
   * increment the total evaluation count.
   */
  synchronized private void incrEvalCount() { ++_evalCount; }

}

