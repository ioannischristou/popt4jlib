package popt4jlib.LocalSearch;

import java.util.*;

/**
 * extension of <CODE>NeighborhoodFilterIntf</CODE> class to work specifically
 * with Set&lt;Integer&gt; arg solutions and Integers as "guides".
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface IntSetNeighborhoodFilterIntf extends NeighborhoodFilterIntf {
  /**
   * returns a List&lt;Integer&gt; points to be "tried" to expand (or shrink) the
   * set arg.
   * @param x Integer
   * @param arg Set // Set&lt;Integer&gt;
   * @param params HashMap
   * @return List // List&lt;Integer&gt;
   * @throws LocalSearchException
   */
  public List filter(Integer x, Set arg, HashMap params) throws LocalSearchException;


  /**
   * returns the maximum number of ints that should be considered for addition
   * or subtraction in a particular move-maker.
   * @return int
   */
  public int getMaxCardinality4Search();
}

