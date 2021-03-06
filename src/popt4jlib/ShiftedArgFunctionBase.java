package popt4jlib;

import java.util.*;

/**
 * The class is a wrapper class for FunctionIntf objects, that evaluates the
 * FunctionIntf object not on the actual VectorIntf or double[] argument x that
 * it is given, but rather on a shifted vector x-&delta;, where the shift &delta; 
 * is passed in the params hashtable as a pair 
 * &lt;"function.shiftarg", double[] delta&gt;.
 * However, if it is passed a non-vector of doubles argument, the eval() method
 * simply passes along the argument to the underlying function without shifting
 * it, since it does not know how to do the shifting.
 * <p>Notes:
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
public class ShiftedArgFunctionBase implements FunctionIntf {
  private FunctionIntf _f=null;

  /**
   *
   * @param f FunctionIntf
   */
  public ShiftedArgFunctionBase(FunctionIntf f) {
    _f = f;
  }


  /**
   * evaluates the function f passed in during construction of this object at 
	 * the point arg-delta where delta is a double[] found in the params under the
	 * key "function.shiftarg". If the arg is a VectorIntf object, the evaluation
	 * will be rather slow as there will be conversions from one data type to the
	 * other, and object creations.
   * @param arg Object must be double[] or VectorIntf
   * @param params HashMap
   * @return double
   */
  public double eval(Object arg, HashMap params) {
    double[] newarg=null;
    if (arg instanceof double[]) {
      newarg = (double[]) ((double[]) arg).clone();
    }
    else if (arg instanceof VectorIntf) {
      newarg = (double[]) ((VectorIntf) arg).getDblArray1().clone();
    }
    if (newarg!=null) {
      try {
        int n = newarg.length;
        double[] delta = (double[]) params.get("function.shiftarg");
        for (int i=0; i<n; i++) newarg[i] -= delta[i];
        if (arg instanceof VectorIntf) {
          double y = _f.eval(new DblArray1Vector(newarg), params);
					if (Double.isNaN(y)) 
						return Double.MAX_VALUE;  // itc-20190701: fix NaN value issue
					return y;
				}
        else {
					double y = _f.eval(newarg, params);
					if (Double.isNaN(y)) 
						return Double.MAX_VALUE;  // itc-20190701: fix NaN value issue
					return y;
				}
      }
      catch (ClassCastException e) {
        e.printStackTrace();  // no-op
      }
    }
    double y = _f.eval(arg, params);
		if (Double.isNaN(y)) return Double.MAX_VALUE;  // itc-20190701: fix NaN val
		return y;
  }

}

