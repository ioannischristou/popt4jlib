package graph.partitioning;

import graph.*;
import java.util.HashMap;

public interface HPartitionerIntf {

  public void setObjectiveFunction(HObjFncIntf f);

  /**
   * the method call will partition the graph g's nodes into k blocks. The
   * resulting partition will be stored in an array int[] that will be
   * returned to the caller. Any other constraints etc. are passed in
   * the method via the properties argument.
   * k is the number of blocks to partition the graph into. the return array
   * has length g.getNumNodes() and the values of the array elems are in
   * [1,...,k]. The partitioner will attempt to optimize the objective function
   * set in the call setObjectiveFunction(f).
   * @param g HGraph
   * @param k int
   * @param properties HashMap
   * @return int[]
   */
  public int[] partition(HGraph g, int k, HashMap properties) throws PartitioningException;
}

