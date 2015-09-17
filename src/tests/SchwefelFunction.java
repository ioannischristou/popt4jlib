package tests;

import popt4jlib.*;
import java.util.*;

/**
 * This class implements the Schwefel Function in n-dimensions.
 * The function is defines as follows:
 * $$ f(x) = - \sum_{i=1}^n [-x_i sin(\sqrt{|x_i|}] $$.
 * The function assumes its global minimum at x=420.9687e where e is the unit
 * vector in n dimensions, and its minimum value is -418.9829n
 * Bound constraints are: x \in [-500, +500]^n
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class SchwefelFunction implements FunctionIntf {
  /**
   * public no-arg constructor
   */
  public SchwefelFunction() {
  }


  /**
   * evaluates the Schwefel function at x=arg.
   * @param arg Object must be either <CODE>double[]</CODE> or
   * <CODE>popt4jlib.VectorIntf</CODE>
   * @param p HashMap unused
   * @return double
   * @throws IllegalArgumentException if the argument does not adhere to the
   * specification
   */
  public double eval(Object arg, HashMap p) throws IllegalArgumentException {
    try {
      if (arg instanceof VectorIntf) {
        VectorIntf x = (VectorIntf) arg;
        int n = x.getNumCoords();
        double res = 0;
        for (int i = 0; i < n; i++) {
          double xi = x.getCoord(i);
          res -= xi * Math.sin(Math.sqrt(Math.abs(xi)));
        }
        return res;
      }
      else {
        double[] x = (double[]) arg;
        int n = x.length;
        double res = 0;
        for (int i = 0; i < n; i++) {
          res -= x[i] * Math.sin(Math.sqrt(Math.abs(x[i])));
        }
        return res;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("cannot evaluate function with given argument");
    }
  }


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; tests.SchwefelFunction &lt;x_1&gt; ... &lt;x_n&gt;</CODE>
   * where the parameters are the values of the components of the vector point
   * at which the Schwefel function is to be evaluated.
   * @param args String[]
   */
  public static void main(String[] args) {
    int n = args.length;
    double[] x = new double[n];
    HashMap p = null;
    for (int i=0; i<n; i++) {
      x[i] = Double.parseDouble(args[i]);
    }
    SchwefelFunction f = new SchwefelFunction();
    double res=f.eval(x,p);
    System.out.println(res);
  }
}

