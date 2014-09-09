package tests;

import popt4jlib.*;
import java.util.*;


/**
 * The Ackley test function for real function optimization. The function's args
 * are usually constrained in the interval [-32, +32]^n.
 * The Ackley Function in LaTeX is defined as follows:
 * $$f(x) = -ae^{-b\sqrt{\sum_{i=1}^n {{x_i}^2}/n}}-e^{\sum_{i=1}^n {cos(cx_i}/n}}
 *   + a + e $$
 * with default values for a=20, b=0.2, and c=2ð.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class AckleyFunction implements FunctionIntf {
  /**
   * public default no-arg constructor
   */
  public AckleyFunction() {
  }

  /**
   * return the value of the Ackley function.
   * The params map may contain following parameters:
   * &lt"a", Double val&gt the value of the a parameter
   * &lt"b", Double val&gt the value of the b parameter
   * &lt"c", Double val&gt the value of the c parameter
   * @param arg Object may be either a <CODE>double[]</CODE> or a
   * <CODE>VectorIntf</CODE> object.
   * @param params Hashtable
   * @throws IllegalArgumentException if the arg is not of the expected type.
   * @return double
   */
  public double eval(Object arg, Hashtable params) throws IllegalArgumentException {
    double[] x = null;
    if (arg instanceof VectorIntf) x= ((VectorIntf) arg).getDblArray1();
    else if (arg instanceof double[]) x = (double[]) arg;
    else throw new IllegalArgumentException("AckleyFunction expects double[] or VectorIntf");
    double res = 0.0;
    double a = 20.0;
    try {
      Double aD = (Double) params.get("a");
      if (aD != null) a = aD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
    double b = 0.2;
    try {
      Double bD = (Double) params.get("b");
      if (bD != null) b = bD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
    double c = 2.0*Math.PI;
    try {
      Double cD = (Double) params.get("c");
      if (cD != null) c = cD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
    final int n = x.length;
    double t1 = 0.0;
    double t2 = 0.0;
    for (int i=0; i<n; i++) {
      t1 += x[i]*x[i];
      t2 += Math.cos(c*x[i]);
    }
    res = -a*Math.exp(-b*Math.sqrt(t1/n)) -Math.exp(t2/n) + a + Math.E;
    return res;
  }
}
