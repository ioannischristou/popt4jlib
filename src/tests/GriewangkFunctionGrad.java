package tests;

import popt4jlib.*;
import java.util.*;

/**
 * This class implements the Griewangk Function Gradient in n-dimensions.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 *
 * @author Ioannis T. Christou
 * @version 1.0
 */

public class GriewangkFunctionGrad implements VecFunctionIntf {
  /**
   * public no-arg constructor
   */
  public GriewangkFunctionGrad() {
  }


  /**
   * evaluates the Griewangk gradient at the given point x.
   * @param x VectorIntf
   * @param p HashMap unused
   * @return VectorIntf
   * @throws IllegalArgumentException if x is null
   */
  public VectorIntf eval(VectorIntf x, HashMap p) throws IllegalArgumentException {
    if (x==null) throw new IllegalArgumentException("null argument");
    int n = x.getNumCoords();
    VectorIntf g = x.newInstance();  // x.newCopy();
    for (int i=0; i<n; i++) {
      double xi = x.getCoord(i);
      double val = xi/2000.0;
      double sqrtip1 = Math.sqrt(i+1);
      double prod = 1.0/sqrtip1;
      for (int j=0; j<n; j++) {
        if (j==i) prod *= Math.sin(xi/sqrtip1);
        else prod *= Math.cos(x.getCoord(j)/Math.sqrt(j+1));
      }
      val += prod;
      try {
        g.setCoord(i, val);
      }
      catch (parallel.ParallelException e) {  // can never get here
        e.printStackTrace();
      }
    }
    return g;
  }


  /**
   * evaluate partial derivative at i.
   * @param x VectorIntf
   * @param p HashMap
   * @param i int
   * @throws IllegalArgumentException
   * @return double
   */
  public double evalCoord(VectorIntf x, HashMap p, int i) throws IllegalArgumentException {
    if (x==null) throw new IllegalArgumentException("null argument");
    int n = x.getNumCoords();
    if (i<0 || i>=n) throw new IllegalArgumentException("coord out of range");
    double xi = x.getCoord(i);
    double val = xi/2000.0;
    double sqrtip1 = Math.sqrt(i+1);
    double prod = 1.0/sqrtip1;
    for (int j=0; j<n; j++) {
      if (j==i) prod *= Math.sin(xi/sqrtip1);
      else prod *= Math.cos(x.getCoord(j)/Math.sqrt(j+1));
    }
    val += prod;
    return val;
  }
}

