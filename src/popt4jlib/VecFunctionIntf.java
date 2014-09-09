package popt4jlib;

import java.util.Hashtable;

/**
 * definition of a vector function mapping vectors in R^n to vectors in R^m
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface VecFunctionIntf {
  /**
   * the single method a VecFunctionIntf object must implement.
   * @param arg VectorIntf
   * @param params Hashtable
   * @throws IllegalArgumentException if the arg is null or not in the domain
   * of the function.
   * @return VectorIntf
   */
  public VectorIntf eval(VectorIntf arg, Hashtable params) throws IllegalArgumentException;


  /**
   * evaluate only one coordinate of the vector function.
   * @param arg VectorIntf
   * @param params Hashtable
   * @param coord int
   * @throws IllegalArgumentException if the arg is null or not in the domain
   * of the function, or if the coord is not in the range of the dimensions of
   * the function.
   * @return double
   */
  public double evalCoord(VectorIntf arg, Hashtable params, int coord) throws IllegalArgumentException;

}
