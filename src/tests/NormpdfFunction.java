package tests;

import popt4jlib.*;
import java.util.*;


/**
 * The normpdf function defined as follows:
 * $$f(x) = (1/\sqrt{2ð})e^{-||x||^2/2}$$
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class NormpdfFunction implements FunctionIntf {
  /**
   * public default no-arg constructor
   */
  public NormpdfFunction() {
  }


  /**
   * return the value of the multi-variate normpdf function
   * (usually symbolized as ö(x)).
   * @param arg Object may be either a <CODE>double[]</CODE> or a
   * <CODE>VectorIntf</CODE> object.
   * @param params HashMap unused
   * @throws IllegalArgumentException if the arg is not of the expected type.
   * @return double
   */
  public double eval(Object arg, HashMap params) throws IllegalArgumentException {
    double[] x = null;
    if (arg instanceof VectorIntf) x= ((VectorIntf) arg).getDblArray1();
    else if (arg instanceof double[]) x = (double[]) arg;
    else throw new IllegalArgumentException("NormpdfFunction expects double[] or VectorIntf");
    final int n = x.length;
    double xn2 = 0;
    for (int i=0; i<n; i++) {
      xn2 += x[i]*x[i];
    }
    return Math.exp(-xn2/2.0)/Math.pow(2.0*Math.PI, (double) n/2.0);
  }
}

