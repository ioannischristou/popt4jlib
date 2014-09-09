package tests;

import popt4jlib.*;
import java.util.*;

/**
 * This class implements the TRID Function (convex quadratic) in n-dimensions
 * The function assumes its global minimum at x=[n 2(n-1) ... i(n+1-i)...n] and
 * its minimum value is -n(n+4)(n-1)/6
 * Bound constraints are: x \in [-n^2, +n^2]^n
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class TridFunction implements FunctionIntf {
  /**
   * public no-arg constructor
   */
  public TridFunction() {
  }


  /**
   * evaluates the Trid function at x=arg.
   * @param arg Object must be a <CODE>double[]</CODE> or <CODE>popt4jlib.VectorIntf</CODE>
   * object.
   * @param p Hashtable unused
   * @return double
   * @throws IllegalArgumentException if arg does not adhere to specification
   */
  public double eval(Object arg, Hashtable p) throws IllegalArgumentException {
    try {
      if (arg instanceof VectorIntf) {
        VectorIntf x = (VectorIntf) arg;
        final int n = x.getNumCoords();
        double t1 = 0;
        double t2 = 0;
        for (int i = 0; i < n; i++) {
          double xi = x.getCoord(i);
          t1 += (xi - 1) * (xi - 1);
          if (i > 0) t2 += xi * x.getCoord(i - 1);
        }
        return t1 - t2;
      }
      else {
        double[] x = (double[]) arg;
        final int n = x.length;
        double t1 = 0;
        double t2 = 0;
        for (int i = 0; i < n; i++) {
          double xi = x[i];
          t1 += (xi - 1) * (xi - 1);
          if (i > 0) t2 += xi * x[i - 1];
        }
        return t1 - t2;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("cannot evaluate TridFunction with given arg");
    }
  }
}

