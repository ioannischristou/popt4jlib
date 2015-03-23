package tests;

import popt4jlib.*;
import java.util.*;

/**
 * This class implements the (weighted) ||x||^2 Function Gradient in n-dimensions.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Norm2FunctionGrad implements VecFunctionIntf {
  /**
   * public no-arg constructor
   */
  public Norm2FunctionGrad() {
  }


  /**
   * evaluates the gradient of the function (Ax)'x at the given point x.
   * @param x VectorIntf
   * @param p Hashtable if not null, may contain pairs of the following form:
   * <li> <"a"$i$, Double a> indicating the value of the element A[$i$+1][$i$+1]
   * (if non-existent, it is assumed to be 1)
   * @return VectorIntf
   * @throws IllegalArgumentException if the above specification for the arguments
   * is not adhered to
   */
  public VectorIntf eval(VectorIntf x, Hashtable p) throws IllegalArgumentException {
    try {
      int n = x.getNumCoords();
      VectorIntf g = x.newInstance();  // x.newCopy();
      for (int i = 0; i < n; i++) {
        double val = 2.0*x.getCoord(i);
        if (p!=null) {
          Double aiD = (Double) p.get("a" + i);
          if (aiD != null) val *= aiD.doubleValue();
        }
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
   * @param p Hashtable
   * @param i int
   * @throws IllegalArgumentException
   * @return double
   */
  public double evalCoord(VectorIntf x, Hashtable p, int i) throws IllegalArgumentException {
    try {
      double val = 2.0*x.getCoord(i);
      if (p!=null) {
        Double aiD = (Double) p.get("a" + i);
        if (aiD != null) val *= aiD.doubleValue();
      }
      return val;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("gradient cannot be evaluated with given arguments");
    }
  }
}

