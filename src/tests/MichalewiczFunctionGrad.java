package tests;

import popt4jlib.*;
import java.util.*;

/**
 * This class implements the Michalewicz Function Gradient in n-dimensions
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class MichalewiczFunctionGrad implements VecFunctionIntf {
  /**
   * public no-arg constructor
   */
  public MichalewiczFunctionGrad() {
  }


  /**
   * evaluates the gradient of the Michalewicz function at the point x.
   * @param x VectorIntf
   * @param p Hashtable must contain the following pair:
   * <li> <"m", Double val> the value of the "m" parameter in the definition
   * or the Michalewicz function.
   * @return VectorIntf
   * @throws IllegalArgumentException if the hashtable does not contain the
   * parameter "m", or if x is null.
   */
  public VectorIntf eval(VectorIntf x, Hashtable p) throws IllegalArgumentException {
    try {
      double m = ( (Double) p.get("m")).doubleValue();
      int n = x.getNumCoords();
      VectorIntf g = x.newCopy();
      for (int i = 0; i < n; i++) {
        double xi = x.getCoord(i);
        double vala = (i + 1) * xi * xi / Math.PI;
        double val0 = Math.sin(vala);
        double val1 = -Math.cos(xi) * Math.pow(val0, 2.0 * m);
        double val2 = -Math.sin(xi) * 2.0 * m * Math.pow(val0, (2 * m - 1)) *
            Math.cos(vala) * 2 * (i + 1) * xi / Math.PI;
        double val = val1 + val2;
        g.setCoord(i, val);
      }
      return g;
    }
    catch (Exception e) {
      // can only be a NullPointerException or a ClassCastException
      e.printStackTrace();
      throw new IllegalArgumentException("cannot evaluate gradient with passed arguments");
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
      double m = ( (Double) p.get("m")).doubleValue();
      double xi = x.getCoord(i);
      double vala = (i + 1) * xi * xi / Math.PI;
      double val0 = Math.sin(vala);
      double val1 = -Math.cos(xi) * Math.pow(val0, 2.0 * m);
      double val2 = -Math.sin(xi) * 2.0 * m * Math.pow(val0, (2 * m - 1)) *
          Math.cos(vala) * 2 * (i + 1) * xi / Math.PI;
      double val = val1 + val2;
      return val;
    }
    catch (Exception e) {
      // can only be a NullPointerException or a ClassCastException
      e.printStackTrace();
      throw new IllegalArgumentException("cannot evaluate gradient with passed arguments");
    }
  }
}

