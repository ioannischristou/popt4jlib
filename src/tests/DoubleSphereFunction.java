package tests;

import popt4jlib.*;
import java.util.*;


/**
 * The Lunacek et al Double-Sphere test function for real function optimization. 
 * The function's args are usually constrained in the interval [-5, +5]^n.
 * The Double-Sphere Function in LaTeX is defined as follows:
 * $$f(x) = \min\{\sum_{i=1}^n (x_i-\miu_1)^2, nd+s\sum_{i=1}^n(x_i-\miu_2)^2$$
 * with default values for d=1, s=0.7, ì_1=2.5, and ì_2=((ì_1^2-d)/s)^0.5.
 * Notice that when both nd,s &ge; 0, the global optimum is 0, located at 
 * x=\miu_1 e, where e is the unit vector in all dimensions, whereas otherwise, 
 * assuming s &gt; 0, the global optimum is located at x=\miu_2, assuming of 
 * course that both \miu_i are within the domain of definition of the function.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DoubleSphereFunction implements FunctionIntf {
	
  /**
   * public default no-arg constructor
   */
  public DoubleSphereFunction() {
		
  }

	
  /**
   * return the value of the Double-Sphere function.
   * The params map may contain following parameters:
   * <ul>
	 * <li>&lt;"m1", Double val&gt; the value of the &mu;_1 parameter, default is
	 * 2.5
   * <li>&lt;"s", Double val&gt; the value of the s parameter, default is 0.7
   * <li>&lt;"d", Double val&gt; the value of the d parameter, default is 1.0
	 * <li>&lt;"m2", Double val&gt; the value of the &mu;_2 parameter, default is
	 * -((&mu;_1^2-d)/s)^0.5
	 * </ul>
   * @param arg Object may be either a <CODE>double[]</CODE> or a
   * <CODE>VectorIntf</CODE> object.
   * @param params HashMap
   * @throws IllegalArgumentException if the arg is not of the expected type.
   * @return double
   */
  public double eval(Object arg, HashMap params) 
		  throws IllegalArgumentException {
    double[] x = null;
    if (arg instanceof VectorIntf) x= ((VectorIntf) arg).getDblArray1();
    else if (arg instanceof double[]) x = (double[]) arg;
    else throw new IllegalArgumentException("AckleyFunction expects double[] or VectorIntf");
    double res = 0.0;
    double m1 = 2.5;
    try {
      Double m1D = (Double) params.get("m1");
      if (m1D != null) m1 = m1D.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
    double d = 1.0;
    try {
      Double dD = (Double) params.get("d");
      if (dD != null) d = dD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
    double s = 0.7;
    try {
      Double sD = (Double) params.get("s");
      if (sD != null) s = sD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
		double m2 = -Math.sqrt((m1*m1-d)/s);
		try {
			Double m2D = (Double) params.get("m2");
			if (m2D!=null) m2 = m2D.doubleValue();
		}
    catch (ClassCastException e) {
      e.printStackTrace();
    }
		
    final int n = x.length;
		double t1 = 0;
		double t2 = 0;
		for (int i=0; i<n; i++) {
			t1 += (x[i]-m1)*(x[i]-m1);
			t2 += (x[i]-m2)*(x[i]-m2);
		}
		t2 *= s;
		t2 += n*d;
    return Math.min(t1, t2);
  }
}
