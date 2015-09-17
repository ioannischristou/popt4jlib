package popt4jlib;

import java.util.*;

/**
 * interface class for constructing random arguments for a function.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface RandomArgMakerIntf {
  /**
   * the sole method every RandomArgMakerIntf implementation must provide.
   * @param params HashMap
   * @throws OptimizerException
   * @return Object
   */
  public Object createRandomArgument(HashMap params) throws OptimizerException;
}
