package popt4jlib;

import java.util.HashMap;

/**
 * interface defining a function mapping objects of some domain (not necessarily
 * R^n) to R.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface FunctionIntf {
  /**
   * the method mapping the arg passed in to a number in R. Any parameters are
   * passed in the params HashMap.
   * @param arg Object
   * @param params HashMap
   * @throws IllegalArgumentException if the arg is illegal for the function
   * @return double
   */
  public double eval(Object arg, HashMap params) throws IllegalArgumentException;
}
