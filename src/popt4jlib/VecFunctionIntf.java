package popt4jlib;

import java.util.HashMap;
import java.io.Serializable;

/**
 * definition of a vector function mapping vectors in R^n to vectors in R^m.
 * Extends the <CODE>java.io.Serializable</CODE> interface so that implementing 
 * objects can be transported across JVMs in distributed computation.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface VecFunctionIntf extends Serializable {
  /**
   * the single method a VecFunctionIntf object must implement.
   * @param arg VectorIntf
   * @param params HashMap
   * @throws IllegalArgumentException if the arg is null or not in the domain
   * of the function.
   * @return VectorIntf
   */
  public VectorIntf eval(VectorIntf arg, HashMap params) 
    throws IllegalArgumentException;


  /**
   * evaluate only one coordinate (component) of the vector function.
   * @param arg VectorIntf
   * @param params HashMap
   * @param coord int must be in the range {0,1...<CODE>getNumCoords()</CODE>-1}
   * @throws IllegalArgumentException if the arg is null or not in the domain
   * of the function, or if the coord is not in the range of the dimensions of
   * the function.
   * @return double
   */
  public double evalCoord(VectorIntf arg, HashMap params, int coord) 
    throws IllegalArgumentException;

}
