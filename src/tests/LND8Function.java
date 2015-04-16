package tests;

import popt4jlib.*;
import java.util.*;

/**
 * class implements the so-called LND8 function (continuous, non-differentiable).
 * Its global minimum value for n=10 is -6.5146, n=20 is -13.5831,
 * n=30 is -20.6535, n=40 is -27.7243, n=50 is -34.795.
 * The function is called "Chained Mifflin 2".
 * See Haarala (2004) and Brimberg et al (2013).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 *
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LND8Function implements FunctionIntf {
  /**
   * no-arg no-op public constructor
   */
  public LND8Function() {
    // no-op
  }


  /**
   * evaluate the LND8 function at the given point <CODE>x</CODE>.
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
   * invoke as <CODE>java -cp &lt;classpath&gt; tests.LND8Function &lt;x1&gt; ... &lt;xn&gt; </CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    int n = args.length;
    double[] x = new double[n];
    for (int i=0; i<n; i++) {
      x[i] = Double.parseDouble(args[i]);
    }
    LND8Function f = new LND8Function();
    double res=f.eval(x,null);
    System.out.println(res);
  }


  /**
   * specifies the function LND8 on double[] x.
   * @param x double[]
   * @return double
   */
  private double evalArray(double[] x) {
    double res = 0.0;
    for (int i=0; i<x.length-1; i++) {
      double xi = x[i], xip1 = x[i+1];
      res += -xi + 2.0*(xi*xi+xip1*xip1-1.0) + 1.75*Math.abs(xi*xi+xip1*xip1-1.0);
    }
    return res;
  }
}

