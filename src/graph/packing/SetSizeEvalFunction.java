package graph.packing;

import popt4jlib.FunctionIntf;
import java.util.*;

/**
 * Evaluates the size of the set-argument and returns its negative.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class SetSizeEvalFunction implements FunctionIntf {

  /**
   * sole public constructor
   */
  public SetSizeEvalFunction() {
    // no-op
  }


  /**
   * return minus the size of the set-valued argument
   * @param arg Object must be a Set
   * @param params Hashtable
   * @throws IllegalArgumentException if arg is not a Set.
   * @return double
   */
  public double eval(Object arg, Hashtable params) throws IllegalArgumentException {
    try {
      Set s = (Set) arg;
      return -s.size();
    }
    catch (ClassCastException e) {
      throw new IllegalArgumentException("arg must be a Set<Integer>");
    }
  }
}

