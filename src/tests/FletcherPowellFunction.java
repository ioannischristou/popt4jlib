package tests;

import popt4jlib.*;
import java.util.*;

/**
 *
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 *
 * This class implements the Fletcher-Powell Function in n-dimensions
 * f(x) = \sum_{i=1}^n (A_i - B_i)^2
 * A_i = \sum_{j=1}^n [a_{ij}sin(á_j)+b_{ij}cos(á_j)
 * B_i = \sum_{j=1}^n [a_{ij}sin(x_j)+b_{ij}cos(x_j)
 * The global minimum is at the values á_j \in [-ð, ð]
 * The parameters a_{ij} and b_{ij} are random integers in {-100,+100}.
 * Usually, n=30.
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FletcherPowellFunction implements FunctionIntf {
  /**
   * public no-arg constructor
   */
  public FletcherPowellFunction() {
  }


  /**
   * evaluates the Fletcher-Powell test-function at the point x=arg
   * @param arg Object must be a <CODE>double[]</CODE> or
   * <CODE>popt4jlib.VectorIntf</CODE> object.
   * @param p HashMap MUST contain the following pairs:
	 * <ul>
   * <li> &lt;"A", Vector[]&gt; where each element of the Vector[] is a Vector&lt;Double&gt;
   * <li> &lt;"B", Vector[]&gt; where each element of the Vector[] is a Vector&lt;Double&gt;
	 * </ul>
   * @return double
   * @throws IllegalArgumentException if arg or p do not comply with the above
   * specifications.
   */
  public double eval(Object arg, HashMap p) throws IllegalArgumentException {
    try {
      Vector[] A = (Vector[]) p.get("A"); // A[i] is Vector<Double> and represents the i-th row of A
      Vector[] B = (Vector[]) p.get("B"); // B[i] same as A[i].
      double[] alpha = (double[]) p.get("alpha"); // alpha is the position of the global minimum
      double[] x = null;
      if (arg instanceof VectorIntf) x = ( (VectorIntf) arg).getDblArray1();
      else x = (double[]) arg;
      final int n = x.length;
      double res = 0;
      for (int i = 0; i < n; i++) {
        double termi = aterm(A[i], B[i], alpha) - aterm(A[i], B[i], x);
        res += termi * termi;
      }
      return res;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Fletcher-Powell Function cannot be "+
                                         "evaluated at given point with given parameters");
    }
  }


  private static double aterm(Vector a, Vector b, double[] x) {
    final int n = x.length;
    double res = 0;
    for (int j=0; j<n; j++) {
      int aij = ((Integer) a.elementAt(j)).intValue();
      int bij = ((Integer) b.elementAt(j)).intValue();
      double t = aij*Math.sin(x[j])+bij*Math.cos(x[j]);
      res += t;
    }
    return res;
  }

}

