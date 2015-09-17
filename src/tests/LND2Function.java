package tests;

import popt4jlib.*;
import java.util.*;

/**
 * class implements the so-called LND2 function (continuous, non-differentiable).
 * Its global minimum value for any number of dimensions is zero.
 * The function is a generalization of MXHILB function.
 * See Haarala (2004) and Brimberg et al (2013).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 *
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LND2Function implements FunctionIntf {
  /**
   * no-op no-arg public constructor
   */
  public LND2Function() {
    // no-op
  }


  /**
   * evaluate the LND2 function at the given point <CODE>x</CODE>.
   * @param arg Object must be a <CODE>double[]</CODE> or a
   * <CODE>popt4jlib.VectorIntf</CODE> object.
   * @param p HashMap unused.
   * @throws IllegalArgumentException if <CODE>arg</CODE> is not of the mentioned
   * types.
   * @return double
   */
  public double eval(Object arg, HashMap p) throws IllegalArgumentException {
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
   * invoke as <CODE>java -cp &lt;classpath&gt; tests.LND2Function &lt;x1&gt; ... &lt;xn&gt; </CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    int n = args.length;
    double[] x = new double[n];
    for (int i=0; i<n; i++) {
      x[i] = Double.parseDouble(args[i]);
    }
    LND2Function f = new LND2Function();
    double res=f.eval(x,null);
    System.out.println(res);
  }


  /**
   * specifies the function LND2 on double[] x.
   * @param x double[]
   * @return double
   */
  private double evalArray(double[] x) {
    double res = Double.NEGATIVE_INFINITY;
    for (int i=1; i<=x.length; i++) {
      double sum = 0.0;
      for (int j=0; j<x.length; j++) {
        sum += x[j] / (double)(i+j);
      }
      sum = Math.abs(sum);
      if (sum > res) res = sum;
    }
    return res;
  }
}

