package popt4jlib.LocalSearch;

import java.util.*;

/**
 * extension of <CODE>NeighborhoodFilterIntf</CODE> class to work specifically
 * with Set&ltInteger&gt arg solutions and Integers as "guides".
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface IntSetNeighborhoodFilterIntf extends NeighborhoodFilterIntf {
  /**
   * returns a List&ltInteger&gt points to be "tried" to expand (or shrink) the
   * set arg.
   * @param x Integer
   * @param arg Set // Set&ltInteger&gt
   * @param params Hashtable
   * @return List // List&ltInteger&gt
   * @throws LocalSearchException
   */
  public List filter(Integer x, Set arg, Hashtable params) throws LocalSearchException;


  /**
   * returns the maximum number of ints that should be considered for addition
   * or subtraction in a particular move-maker.
   * @return int
   */
  public int getMaxCardinality4Search();
}

