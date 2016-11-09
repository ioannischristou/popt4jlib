package popt4jlib;

import java.util.HashMap;
import java.io.Serializable;

/**
 * interface defining a function mapping objects of some domain (not necessarily
 * R^n) to R. The interface is also asked to implement Serializable so that 
 * function objects can be transported over sockets and executed in remote JVMs.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface FunctionIntf extends Serializable {
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
