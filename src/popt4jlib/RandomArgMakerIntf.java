package popt4jlib;

import java.util.HashMap;
import java.io.Serializable;

/**
 * interface class for constructing random arguments for a function.
 * Extends the <CODE>java.io.Serializable</CODE> interface so that implementing 
 * objects can be transported across JVMs in distributed computation.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface RandomArgMakerIntf extends Serializable {
  /**
   * the sole method every RandomArgMakerIntf implementation must provide.
   * @param params HashMap
   * @throws OptimizerException
   * @return Object
   */
  public Object createRandomArgument(HashMap params) throws OptimizerException;
}
