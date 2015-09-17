package tests;

import popt4jlib.*;
import java.util.*;

/**
 * class implements the so-called LND3 function (continuous, non-differentiable).
 * Its global minimum value for n dimensions is -(n-1)*Math.sqrt(2)
 * The function is called "Chained LQ" function.
 * See Haarala (2004) and Brimberg et al (2013).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 *
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LND3Function implements FunctionIntf {
  /**
   * no-arg no-op public constructor
   */
  public LND3Function() {
    // no-op
  }


  /**
   * evaluate the LND3 function at the given point <CODE>x</CODE>.
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
   * invoke as <CODE>java -cp &lt;classpath&gt; tests.LND3Function &lt;x1&gt; ... &lt;xn&gt; </CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    int n = args.length;
    double[] x = new double[n];
    for (int i=0; i<n; i++) {
      x[i] = Double.parseDouble(args[i]);
    }
    LND3Function f = new LND3Function();
    double res=f.eval(x,null);
    System.out.println(res);
  }


  /**
   * specifies the function LND3 on double[] x.
   * @param x double[]
   * @return double
   */
  private double evalArray(double[] x) {
    double res = 0.0;
    for (int i=0; i<x.length-1; i++) {
      res += Math.max(-x[i]-x[i+1], -x[i]-x[i+1]+(x[i]*x[i]+x[i+1]*x[i+1]-1));
    }
    return res;
  }
}

