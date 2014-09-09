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
 * This class implements a Function() object that counts the number of times
 * the values in the argument change sign. Thus, the optimal argument would
 * be an argument such as [1 -1 1 -1 1 -1 ... 1]
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class AlternateFunction implements FunctionIntf {

  /**
   * public no-arg constructor
   */
  public AlternateFunction() {
  }


  /**
   * evaluates the AlternateFunction at the argument given in the first argument
   * @param arg Object Object may be either a <CODE>double[]</CODE> or a
   * <CODE>VectorIntf</CODE> object.
   * @param p Hashtable unused.
   * @return double
   */
  public double eval(Object arg, Hashtable p) {
    double[] x = (double[]) arg;
    double res = 1;
    int n = x.length;
    for (int i=1; i<n; i++) {
      if (x[i]*x[i-1] < 0) res += 1;
    }
    return 1.0/res;
  }
}

