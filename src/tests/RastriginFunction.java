package tests;

import popt4jlib.*;
import java.util.*;

/**
 * This class implements the Rastrigin Function in n-dimensions. The Rastrigin
 * function is defined as follows:
 * $$ f(x)= An + \sum_{i=1}^n [x_i^2 - Acos(w x_i)] $$
 * By default, A=10, w=2�.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RastriginFunction implements FunctionIntf {
  /**
   * public no-arg constructor
   */
  public RastriginFunction() {
  }


  /**
   * evaluate the Rastrigin function at x=arg, for the values A and w given
   * in the p HashMap.
   * @param arg Object must be <CODE>double[]</CODE> or <CODE>popt4jlib.VectorIntf</CODE>.
   * @param p HashMap must contain the following two parameters:
	 * <ul>
   * <li> &lt;"A", Double val&gt; the value for the "A" parameter
   * <li> &lt;"w", Double val&gt; the value for the "w" parameter
	 * </ul>
   * @return double
   * @throws IllegalArgumentException if the arguments above do not adhere to
   * their specification (or if any is null)
   */
  public double eval(Object arg, HashMap p) throws IllegalArgumentException {
    try {
			double a = 10.0;
      Double aI = (Double) p.get("A");
			if (aI!=null) a = aI.doubleValue();
      double w = 2*Math.PI;
			Double wI = (Double) p.get("w");
			if (wI!=null) w = wI.doubleValue();
      if (arg instanceof VectorIntf) {
        VectorIntf x = (VectorIntf) arg;
        int n = x.getNumCoords();
        double res = n * a;
        for (int i = 0; i < n; i++) {
          res += (x.getCoord(i) * x.getCoord(i) - a * Math.cos(w * x.getCoord(i)));
        }
        return res;
      }
      else {
        double[] x = (double[]) arg;
        int n = x.length;
        double res = n * a;
        for (int i = 0; i < n; i++) {
          res += (x[i] * x[i] - a * Math.cos(w * x[i]));
        }
        return res;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("function cannot be evaluated with given arguments");
    }
  }


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; tests.RastriginFunction &lt;A&gt; &lt;w&gt; &lt;x_1&gt; ... &lt;x_n&gt;</CODE>
   * where the arguments x_1 ... x_n are the values of the components of the
   * vector point at which the function is to be evaluated.
   * @param args String[]
   */
  public static void main(String[] args) {
    int n = args.length;
    double[] x = new double[n-2];
    double a = Double.parseDouble(args[0]);
    double w = Double.parseDouble(args[1]);
    HashMap p = new HashMap();
    p.put("A",new Double(a));
    p.put("w",new Double(w));
    for (int i=2; i<n; i++) {
      x[i-2] = Double.parseDouble(args[i]);
    }
    RastriginFunction f = new RastriginFunction();
    double res=f.eval(x,p);
    System.out.println(res);
  }
}

