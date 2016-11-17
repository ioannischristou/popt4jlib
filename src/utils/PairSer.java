package utils;

import java.io.Serializable;

/**
 * serializable utility class holding pairs of objects. Useful when transporting
 * Pair objects. Of course, both objects in the pair must be serializable too.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PairSer extends Pair implements Serializable {
	// private static final long serialVersionUID = -1L;

  /**
   * public constructor.
   * @param first Object
   * @param second Object
   */
  public PairSer(Object first, Object second) {
    super(first, second);
  }

}

