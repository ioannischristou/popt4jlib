package tests;

import popt4jlib.*;
import java.util.*;

/**
 * class implements the so-called Drop-Wave multi-dimensional function used in
 * NLP method testing (mostly in meta-heuristic literature). The function is
 * defined as follows: $$f(x) = -(1+cos(12\sqrt{||x||^2})/(2+||x||^2/2)$$.
 * Notice that this definition extends the drop-wave function to many dimensions
 * (whereas the original definition is in 2 dimensions only).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 *
 * This class implements the DropWave Function in n-dimensions (usually, n=2).
 * The argument boundaries are [-5.12, 5.12]^n
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DropWaveFunction implements FunctionIntf {
  public DropWaveFunction() {
  }

  /**
   * evaluate the drop-wave function at the given point <CODE>x</CODE>.
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
      final int n = x.getNumCoords();
      double sqs = 0.0;
      for (int i=0; i<n; i++) sqs += x.getCoord(i)*x.getCoord(i);
      double res = -(1+Math.cos(12*Math.sqrt(sqs)))/(2+sqs/2.0);
      return res;
    } else {
      try {
        double[] x = (double[]) arg;
        int n = x.length;
        double sqs = 0.0;
        for (int i = 0; i < n; i++) sqs += x[i] * x[i];
        double res = -(1 + Math.cos(12 * Math.sqrt(sqs))) / (2 + sqs / 2.0);
        return res;
      }
      catch (Exception e) {
        e.printStackTrace();
        throw new IllegalArgumentException("function cannot be evaluated at the passed argument");
      }
    }
  }


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; tests.DropWaveFunction &lt;x1&gt; ... &lt;xn&gt; </CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    int n = args.length;
    double[] x = new double[n];
    for (int i=0; i<n; i++) {
      x[i] = Double.parseDouble(args[i]);
    }
    DropWaveFunction f = new DropWaveFunction();
    double res=f.eval(x,null);
    System.out.println(res);
  }
}

