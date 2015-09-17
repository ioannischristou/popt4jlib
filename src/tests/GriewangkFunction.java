package tests;

import popt4jlib.*;
import java.util.*;

/**
 * Implements the Griwangk test-function for unconstrained continuous
 * optimization in n-dimensions. The test-function is defined as:
 * $$f(x)=||x||^2 / 4000 - \prod_{i=1}^n cos(x_i / \sqrt{i}) + 1$$
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 *
 * The function assumes its global minimum at x=0 and its minimum value is 0.
 * Bound constraints are (usually): x \in [-500, +500]^n
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class GriewangkFunction implements FunctionIntf {
  /**
   * public no-arg constructor
   */
  public GriewangkFunction() {
  }


  /**
   * evaluates the Griewangk function at point x=arg
   * @param arg Object must be <CODE>double[]</CODE> or a
   * <CODE>popt4jlib.VectorIntf</CODE> object.
   * @param p HashMap not used
   * @return double
   * @throws IllegalArgumentException if <CODE>arg</CODE> does
   * not adhere to the above specifications.
   */
  public double eval(Object arg, HashMap p) throws IllegalArgumentException {
    try {
      if (arg instanceof VectorIntf) {
        VectorIntf x = (VectorIntf) arg;
        int n = x.getNumCoords();
        double sum = 0;
        double prod = 1.0;
        for (int i = 0; i < n; i++) {
          double xi = x.getCoord(i);
          sum += xi * xi;
          prod *= Math.cos(xi / Math.sqrt(i + 1));
        }
        return sum / 4000.0 - prod + 1;
      }
      else {
        double[] x = (double[]) arg;
        int n = x.length;
        double sum = 0;
        double prod = 1.0;
        for (int i = 0; i < n; i++) {
          sum += x[i] * x[i];
          prod *= Math.cos(x[i] / Math.sqrt(i + 1));
        }
        return sum / 4000.0 - prod + 1;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("argument type incorrect");
    }
  }


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; tests.GriewangkFunction &lt;x_1&gt; ... &lt;x_n&gt;</CODE>
   * to print out the value of the Griewangk function at the point given by
   * the arguments.
   * @param args String[]
   */
  public static void main(String[] args) {
    int n = args.length;
    double[] x = new double[n];
    HashMap p = null;
    for (int i=0; i<n; i++) {
      x[i] = Double.parseDouble(args[i]);
    }
    GriewangkFunction f = new GriewangkFunction();
    double res=f.eval(x,p);
    System.out.println(res);
  }
}

