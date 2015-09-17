package popt4jlib.LocalSearch;

import java.util.*;

/**
 * specifies filters for determining "trial" points in the neighborhood
 * of a solution arg, using a "guide" x.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface NeighborhoodFilterIntf {
  /**
   * method will return a <CODE>List</CODE> of argument objects to be "tried" so 
	 * as to "expand" or "shrink" the neighborhood defined around the solution arg. 
	 * The returned list will contain objects to be tried, based on the "guiding"
   * object x. For example, in a graph packing application, x might be a set of
   * integers to be removed from the current solution arg (that might also be a
   * set of integers), and then, the resulting list will contain all the new
   * integers that can be added to the reduced solution.
   * @param x Object
   * @param arg Object
   * @param params HashMap
   * @throws LocalSearchException
   * @return List
   */
  List filter(Object x, Object arg, HashMap params) throws LocalSearchException;
}

