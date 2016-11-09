package tests;

import popt4jlib.*;
import java.util.*;

/**
 * This class implements the Rastrigin Function Gradient in n-dimensions.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */

public class RastriginFunctionGrad implements VecFunctionIntf {
  /**
   * public no-arg constructor
   */
  public RastriginFunctionGrad() {
  }


  /**
   * evaluates the Rastrigin function gradient at the specified point x.
   * @param x VectorIntf
   * @param p HashMap must contain the following parameters:
	 * <ul>
   * <li> &lt;"A", Double val&gt; the value of the parameter A in the defn of the
   * Rastrigin Function (<CODE>tests.RastriginFunction</CODE>)
   * <li> &lt;"w", Double val&gt; the value of the parameter w in the defn of the
   * Rastrigin Function (<CODE>tests.RastriginFunction</CODE>)
	 * </ul>
   * @return VectorIntf
   * @throws IllegalArgumentException if any of the arguments does not adhere
   * to the specifications.
   */
  public VectorIntf eval(VectorIntf x, HashMap p) throws IllegalArgumentException {
    try {
      int n = x.getNumCoords();
      VectorIntf g = x.newInstance();  // x.newCopy();
      double a = ( (Double) p.get("A")).doubleValue();
      double w = ( (Double) p.get("w")).doubleValue();
      for (int i = 0; i < n; i++) {
        double xi = x.getCoord(i);
        double val = 2 * xi + w * a * Math.sin(w * xi);
        g.setCoord(i, val);
      }
      return g;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("gradient cannot be evaluated with given arguments");
    }
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
    try {
      double a = ( (Double) p.get("A")).doubleValue();
      double w = ( (Double) p.get("w")).doubleValue();
      double xi = x.getCoord(i);
      double val = 2 * xi + w * a * Math.sin(w * xi);
      return val;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("gradient cannot be evaluated with given arguments");
    }
  }
}

