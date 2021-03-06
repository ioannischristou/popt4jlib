package tests;

import popt4jlib.*;
import java.util.*;

/**
 * class implements the so-called LND1 function (continuous, non-differentiable).
 * Its global minimum value for any number of dimensions is zero. The function
 * is a "generalization" of MAXQ problem.
 * See Haarala (2004) and Brimberg et al (2013).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LND1Function implements FunctionIntf {
  /**
   * no-op no-arg public constructor.
   */
  public LND1Function() {
    // no-op
  }


  /**
   * evaluate the LND1 function at the given point <CODE>x</CODE>.
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
      final int n = x.getNumCoords();
      double res = x.getCoord(0)*x.getCoord(0);
      for (int i=1; i<n; i++) {
        double xi = x.getCoord(i);
        double xi2 = xi*xi;
        if (xi2>res) res = xi2;
      }
      return res;
    } else {
      try {
        double[] x = (double[]) arg;
        int n = x.length;
        double res = x[0]*x[0];
        for (int i=1; i<n; i++) {
          double xi2 = x[i]*x[i];
          if (xi2>res) res = xi2;
        }
        return res;
      }
      catch (Exception e) {
        e.printStackTrace();
        throw new IllegalArgumentException("function cannot be evaluated at the passed argument");
      }
    }
  }


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; tests.LND1Function &lt;x1&gt; ... &lt;xn&gt; </CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    int n = args.length;
    double[] x = new double[n];
    for (int i=0; i<n; i++) {
      x[i] = Double.parseDouble(args[i]);
    }
    LND1Function f = new LND1Function();
    double res=f.eval(x,null);
    System.out.println(res);
  }
}

