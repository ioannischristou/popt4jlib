package tests;

import popt4jlib.*;
import java.util.*;

/**
 * class implements the so-called LND6 function (continuous, non-differentiable).
 * Its global minimum value for any number of dimensions is always zero.
 * The function is called "Number of active faces" function.
 * See Haarala (2004) and Brimberg et al (2013).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 *
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LND6Function implements FunctionIntf {
  /**
   * no-arg no-op public constructor
   */
  public LND6Function() {
    // no-op
  }


  /**
   * evaluate the LND6 function at the given point <CODE>x</CODE>.
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
   * invoke as <CODE>java -cp &ltclasspath&gt tests.LND6Function &ltx1&gt ... &ltxn&gt </CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    int n = args.length;
    double[] x = new double[n];
    for (int i=0; i<n; i++) {
      x[i] = Double.parseDouble(args[i]);
    }
    LND6Function f = new LND6Function();
    double res=f.eval(x,null);
    System.out.println(res);
  }


  /**
   * specifies the function LND6 on double[] x.
   * @param x double[]
   * @return double
   */
  private double evalArray(double[] x) {
    double res = Double.NEGATIVE_INFINITY;
    for (int i=0; i<x.length; i++) {
      double gxi = Math.log(Math.abs(x[i])+1);
      if (gxi > res) res = gxi;
    }
    double sum=0.0;
    for (int i=0; i<x.length; i++) {
      sum += x[i];
    }
    sum = Math.log(Math.abs(sum)+1);
    if (sum>res) res = sum;
    return res;
  }
}

