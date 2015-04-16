package tests;

import popt4jlib.*;
import java.util.*;

/**
 * class implements the so-called LND5 function (continuous, non-differentiable).
 * Its global minimum value for n dimensions is fmin = 2(n-1)
 * The function is called "Chained CB3 II" function.
 * See Haarala (2004) and Brimberg et al (2013).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 *
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LND5Function implements FunctionIntf {
  /**
   * no-arg no-op public constructor
   */
  public LND5Function() {
    // no-op
  }


  /**
   * evaluate the LND5 function at the given point <CODE>x</CODE>.
   * @param arg Object must be a <CODE>double[]</CODE> or a
   * <CODE>popt4jlib.VectorIntf</CODE> object.
   * @param p Hashtable unused.
   * @throws IllegalArgumentException if <CODE>arg</CODE> is not of the mentioned
   * types.
   * @return double
   */
  public double eval(Object arg, Hashtable p) throws IllegalArgumentException {
    if (arg instanceof VectorIntf) {
      VectorIntf x = (VectorIntf) arg;
      return evalArray(x.getDblArray1());
    } else {
      try {
        return evalArray((double[]) arg);
      }
      catch (Exception e) {
        e.printStackTrace();
        throw new IllegalArgumentException("function cannot be evaluated at the passed argument");
      }
    }
  }


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; tests.LND5Function &lt;x1&gt; ... &lt;xn&gt; </CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    int n = args.length;
    double[] x = new double[n];
    for (int i=0; i<n; i++) {
      x[i] = Double.parseDouble(args[i]);
    }
    LND5Function f = new LND5Function();
    double res=f.eval(x,null);
    System.out.println(res);
  }


  /**
   * specifies the function LND5 on double[] x.
   * @param x double[]
   * @return double
   */
  private double evalArray(double[] x) {
    double sum1 = 0.0, sum2 = 0.0, sum3 = 0.0;
    for (int i=0; i<x.length-1; i++) {
      double xi = x[i];
      double xi2 = xi*xi;
      double xip1 = x[i+1];
      sum1 += xi2*xi2+xip1;
      sum2 += (2.0-xi)*(2.0-xi)+(2.0-xip1)*(2.0-xip1);
      sum3 += 2.0*Math.exp(-xi+xip1);
    }
    return Math.max(Math.max(sum1, sum2), sum3);
  }
}

