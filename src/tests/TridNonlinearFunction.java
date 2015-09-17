package tests;

import popt4jlib.*;
import java.util.*;

/**
 * This class implements a variation of the TRID Function (convex quadratic)
 * in n-dimensions that is highly nonlinear. It presents many optimization
 * algorithms and packages with serious problems. As an example, both the
 * MADS algorithm implemented in the NOMAD package, and standard CG-type or
 * Newton-type algorithms implemented in the Shuan-Shu library fail to find the
 * global optimum which is at least -250.000,00 for n=100.
 * The function is defined as follows:
 * $$ f(x) = \sum_{i=1}^n (x_i-1)^2 - \sum_{i=2, i%2=0}^n [x_i cos(x_{i-1})]
 *    - \sum_{i=2, i%2=1}^n [x_i sin(x_{i-1}] $$.
 * Bound constraints are: x \in [-n^2, +n^2]^n
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class TridNonlinearFunction implements FunctionIntf {
  /**
   * public no-arg constructor
   */
  public TridNonlinearFunction() {
  }


  /**
   * evaluates the TridNonlinear function at x=arg.
   * @param arg Object must be <CODE>double[]</CODE> or
   * <CODE>popt4jlib.VectorIntf</CODE>
   * @param p HashMap unused
   * @return double
   * @throws IllegalArgumentException if arg does not adhere to the specification
   */
  public double eval(Object arg, HashMap p) throws IllegalArgumentException {
    try {
      if (arg instanceof VectorIntf) {
        VectorIntf x = (VectorIntf) arg;
        final int n = x.getNumCoords();
        double t1 = 0;
        double t2 = 0;
        for (int i = 0; i < n; i++) {
          double xi = x.getCoord(i);
          t1 += (xi - 1) * (xi - 1);
          if (i > 0) {
            double xim1 = 0.0;
            if (i % 2 == 0) xim1 = Math.sin(x.getCoord(i - 1));
            else xim1 = Math.cos(x.getCoord(i - 1));
            t2 += xi * n * xim1;
          }
        }
        return t1 - t2;
      }
      else {
        double[] x = (double[]) arg;
        final int n = x.length;
        double t1 = 0;
        double t2 = 0;
        for (int i = 0; i < n; i++) {
          double xi = x[i];
          t1 += (xi - 1) * (xi - 1);
          if (i > 0) {
            double xim1 = 0.0;
            if (i % 2 == 0) xim1 = Math.sin(x[i - 1]);
            else xim1 = Math.cos(x[i - 1]);
            t2 += xi * n * xim1;
          }
        }
        return t1 - t2;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("cannot evaluate function with given arg");
    }
  }


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; tests.TridNonlinearFunction &lt;x_1&gt; ... &lt;x_n&gt;</CODE>
   * where the parameters are the values of the components of the vector point
   * at which the function will be evaluated.
   * @param args String[]
   */
  public static void main(String[] args) {
    int n = args.length;
    double[] x = new double[n];
    for (int i=0; i<n; i++) {
      x[i] = Double.parseDouble(args[i]);
    }
    TridNonlinearFunction f = new TridNonlinearFunction();
    double res=f.eval(x,null);
    System.out.println(res);
  }

}

