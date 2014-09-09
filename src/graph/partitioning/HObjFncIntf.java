package graph.partitioning;

import graph.*;

/**
 * defines the methods to be used by any class that will implement an objective
 * function measure of a partition of a hyper-graph.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface HObjFncIntf {
  /**
   * returns the overall value of the partitioning defined in the 2nd arg.
   * @param g HGraph
   * @param partition int[]
   * @return double
   */
  double value(HGraph g, int[] partition);


  /**
   * returns the values of each partition of the partitioning defined in the
   * 2nd argument.
   * @param g HGraph
   * @param partition int[]
   * @return double[]
   */
  double[] values(HGraph g, int[] partition);
}
