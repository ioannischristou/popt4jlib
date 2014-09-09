package popt4jlib.LocalSearch;

import java.util.*;

public interface NeighborhoodFilterIntf {
  /**
   * method will return a Vector of argument objects to be "tried" so as to
   * "expand" or "shrink" the neighborhood defined around the solution arg. The
   * returned Vector will contain objects to be tried, based on the "guiding"
   * object x. For example, in a graph packing application, x might be a set of
   * integers to be removed from the current solution arg (that might also be a
   * set of integers), and then, the resulting vector will contain all the new
   * integers that can be added to the reduced solution.
   * @param x Object
   * @param arg Object
   * @param params Hashtable
   * @throws LocalSearchException
   * @return Vector
   */
  Vector filter(Object x, Object arg, Hashtable params) throws LocalSearchException;
}

