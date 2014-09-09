package tests;

import popt4jlib.*;
import java.util.*;


/**
 * The Weierstrass test function for real function optimization. The function's
 * arguments are usually constrained in the interval [-1/2, +1/2]^n. Its
 * global optimum is zero, and the argmin is also zero. The function (inspired
 * from the nowhere differentiable Weirstrass function) is defined as follows:
 * $$ f(x) = \sum_{i=1}^n \sum{k=0}^k_{max} a^k cos(2ðb^k(x_i+0.5))
 *           - n \sum_{k=0}^k_{max} a^k cos(ðb^k) $$ where a is in [0,1] and
 * b is a positive odd  integer. Usually, k_{max}=20, a=0.5 and b=3.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class WeierstrassFunction implements FunctionIntf {
  /**
   * public no-arg constructor
   */
  public WeierstrassFunction() {
  }


  /**
   * evaluates the function at x=arg.
   * @param arg Object must be a <CODE>double[]</CODE> or
   * <CODE>popt4jlib.VectorIntf</CODE> object.
   * @param params Hashtable if not-null may contain the following pairs:
   * <li> <"a", Double val> the value for the a parameter, default is 0.5
   * <li> <"b", Double val> the value of the b parameter, default is 3
   * <li> <"kmax", Integer val> the value of the k_{max} parameter, default is 20
   * @throws IllegalArgumentException if arg does not conform to the specification
   * @return double
   */
  public double eval(Object arg, Hashtable params) throws IllegalArgumentException {
    double[] x = null;
    if (arg instanceof VectorIntf) x= ((VectorIntf) arg).getDblArray1();
    else if (arg instanceof double[]) x = (double[]) arg;
    else throw new IllegalArgumentException("WeierstrassFunction expects double[] or VectorIntf");
    double res = 0.0;
    double a = 0.5;
    double b = 3.0;
    int kmax = 20;
    if (params!=null) {
      try {
        Double aD = (Double) params.get("a");
        if (aD != null) a = aD.doubleValue();
      }
      catch (ClassCastException e) {
        e.printStackTrace();
      }
      try {
        Double bD = (Double) params.get("b");
        if (bD != null) b = bD.doubleValue();
      }
      catch (ClassCastException e) {
        e.printStackTrace();
      }
      try {
        Integer kI = (Integer) params.get("kmax");
        if (kI != null) kmax = kI.intValue();
      }
      catch (ClassCastException e) {
        e.printStackTrace();
      }
    }
    final int n = x.length;
    double t2 = 0.0;
    for (int k=0; k<=kmax; k++) {
      double ak = Math.pow(a, (double) k);
      double bk = Math.pow(b, (double) k);
      t2 += ak*Math.cos(Math.PI*bk);
    }
    double t1 = 0.0;
    for (int i=0; i<n; i++) {
      for (int k=0; k<=kmax; k++) {
        double ak = Math.pow(a, (double) k);
        double bk = Math.pow(b, (double) k);
        t1 += ak*Math.cos(2*Math.PI*bk*(x[i]+0.5));
      }
    }
    res = t1 - n*t2;
    return res;
  }
}

