package tests;

import popt4jlib.*;
import java.util.*;

/**
 * This class implements the Langermann Function in n-dimensions. The function
 * is defined as follows:
 * $$f(x) = \sum_{i=1}^m [c_i * exp(-||x-A_{(i)}||^2/ð) * cos(ð*||x-A_{(i)}||^2)]$$
 * (usually m=5)
 * The argument boundaries are [0, 10]^n
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 *
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LangermannFunction implements FunctionIntf {
  /**
   * public no-arg constructor
   */
  public LangermannFunction() {
  }


  /**
   * evaluates the Langermann function at x=arg
   * @param arg Object must be <CODE>double[]</CODE> or <CODE>popt4jlib.VectorIntf</CODE>
   * @param p HashMap must contain the following pairs:
   * <li> <"A", Vector[]> where each element of the Vector[] is a Vector<Double>
   * <li> <"c", Vector<Double> >
   * @return double
   */
  public double eval(Object arg, HashMap p) {
    Vector[] A = (Vector[]) p.get("A");  // A[i] is Vector<Double> i=0...A.length-1
    Vector c = (Vector) p.get("c");
    double[] x=null;
    if (arg instanceof VectorIntf) {
      x = ((VectorIntf) arg).getDblArray1();
    } else x = (double[]) arg;
    //final int n = x.length;
    final int m = A.length;
    double res=0;
    for (int i=0; i<m; i++) {
      double dist2 = dist2(x,A[i]);
      double ci = ((Double) c.elementAt(i)).doubleValue();
      res += ci*Math.exp(-dist2/Math.PI)*Math.cos(Math.PI*dist2);
    }
    return res;
  }


  private static double dist2(double[] x, Vector a) {
    final int n = x.length;
    double res = 0;
    for (int i=0; i<n; i++) {
      double diffi = x[i] - ((Double) a.elementAt(i)).doubleValue();
      res += diffi*diffi;
    }
    return res;
  }
}

