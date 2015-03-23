package tests;

import popt4jlib.*;
import java.util.*;

/**
 * This class implements the Trid Function Gradient in n-dimensions.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class TridFunctionGrad implements VecFunctionIntf {
  /**
   * public no-arg constructor
   */
  public TridFunctionGrad() {
  }


  /**
   * evaluate the TridFunction gradient at point x.
   * @param x VectorIntf
   * @param p Hashtable unused
   * @return VectorIntf
   * @throws IllegalArgumentException if x==null
   */
  public VectorIntf eval(VectorIntf x, Hashtable p) throws IllegalArgumentException {
    try {
      int n = x.getNumCoords();
      VectorIntf g = x.newInstance();  // x.newCopy();
      for (int i = 0; i < n; i++) {
        double xi = x.getCoord(i);
        double xim1 = i > 0 ? x.getCoord(i - 1) : 0.0;
        double xip1 = (i < n - 1) ? x.getCoord(i + 1) : 0.0;
        double val = 2 * (xi - 1) - (xim1 + xip1);
        g.setCoord(i, val);
      }
      return g;
    }
    catch (Exception e) {
      e.printStackTrace();  // can only be NullPointerException
      throw new IllegalArgumentException("cannot evaluate gradient at given x");
    }
  }


  /**
   * evaluate partial derivative at i.
   * @param x VectorIntf
   * @param p Hashtable
   * @param i int
   * @throws IllegalArgumentException
   * @return double
   */
  public double evalCoord(VectorIntf x, Hashtable p, int i) throws IllegalArgumentException {
    try {
      int n = x.getNumCoords();
      double xi = x.getCoord(i);
      double xim1 = i > 0 ? x.getCoord(i - 1) : 0.0;
      double xip1 = (i < n - 1) ? x.getCoord(i + 1) : 0.0;
      double val = 2 * (xi - 1) - (xim1 + xip1);
      return val;
    }
    catch (Exception e) {
      e.printStackTrace();  // can only be NullPointerException
      throw new IllegalArgumentException("cannot evaluate gradient at given x");
    }
  }
}

