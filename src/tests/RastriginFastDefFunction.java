package tests;

import popt4jlib.*;
import java.util.*;

/**
 * This class implements the Rastrigin Function in n-dimensions with default 
 * parameters A=10, w=2ð.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RastriginFastDefFunction implements FunctionIntf {
  /**
   * public no-arg constructor
   */
  public RastriginFastDefFunction() {
  }


  /**
   * evaluate the Rastrigin function at x=arg (with default parameters A and w).
   * @param arg Object must be <CODE>double[]</CODE> or <CODE>popt4jlib.VectorIntf</CODE>.
   * @param p HashMap unused.
   * @return double
   * @throws IllegalArgumentException if the first argument does not adhere to
   * their specification or is null.
   */
  public double eval(Object arg, HashMap p) throws IllegalArgumentException {
    try {
			double a = 10.0;
      double w = 2*Math.PI;
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
   * invoke as <CODE>java -cp &lt;classpath&gt; tests.RastriginFunction &lt;x_1&gt; ... &lt;x_n&gt;</CODE>
   * where the arguments x_1 ... x_n are the values of the components of the
   * vector point at which the function is to be evaluated.
   * @param args String[]
   */
  public static void main(String[] args) {
    int n = args.length;
    double[] x = new double[n];
    double a = 10.0;
    double w = 2.0*Math.PI;
    for (int i=0; i<n; i++) {
      x[i] = Double.parseDouble(args[i]);
    }
    RastriginFastDefFunction f = new RastriginFastDefFunction();
    double res=f.eval(x,null);
    System.out.println(res);
  }
}

