package popt4jlib.LocalSearch;

import java.util.*;

public interface IntSetNeighborhoodFilterIntf extends NeighborhoodFilterIntf {
  /**
   * returns a Vector<Integer> points to be "tried" to expand (or shrink) the
   * set arg.
   * @param x Integer
   * @param arg Set Set<Integer>
   * @param params Hashtable
   * @return Vector Vector<Integer>
   * @throws LocalSearchException
   */
  public Vector filter(Integer x, Set arg, Hashtable params) throws LocalSearchException;


  /**
   * returns the maximum number of ints that should be considered for addition
   * or subtraction in a particular move-maker.
   * @return int
   */
  public int getMaxCardinality4Search();
}

