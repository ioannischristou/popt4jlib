package tests;

import popt4jlib.*;
import java.util.*;

/**
 * class implements the so-called LND10 function (continuous, non-differentiable).
 * Its global minimum value is always zero.
 * The function is called "Chained crescent II".
 * See Haarala (2004) and Brimberg et al (2013).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 *
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LND10Function implements FunctionIntf {
  /**
   * no-arg no-op public constructor
   */
  public LND10Function() {
    // no-op
  }


  /**
   * evaluate the LND10 function at the given point <CODE>x</CODE>.
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
   * invoke as <CODE>java -cp &ltclasspath&gt tests.LND10Function &ltx1&gt ... &ltxn&gt </CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    int n = args.length;
    double[] x = new double[n];
    for (int i=0; i<n; i++) {
      x[i] = Double.parseDouble(args[i]);
    }
    LND10Function f = new LND10Function();
    double res=f.eval(x,null);
    System.out.println(res);
  }


  /**
   * specifies the function LND10 on double[] x.
   * @param x double[]
   * @return double
   */
  private double evalArray(double[] x) {
    double res = 0.0;
    double res1, res2;
    for (int i=0; i<x.length-1; i++) {
      double xi = x[i], xip1 = x[i+1];
      res1 = xi*xi + (xip1-1.0)*(xip1-1.0) + xip1 - 1.0;
      res2 = -xi*xi - (xip1-1.0)*(xip1-1.0) + xip1 + 1.0;
      res += Math.max(res1, res2);
    }
    return res;
  }
}

