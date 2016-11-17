package popt4jlib.PS;

import popt4jlib.OptimizerException;
import java.util.HashMap;

/**
 * specifies how to create new velocity objects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface NewVelocityMakerIntf {
  /**
   *
   * @param x Object the current individual's chromosome (i.e. particle position)
   * @param v Object the current individual's velocity
   * @param p Object the best position the individual was found in
   * @param g Object the best position found for the island of individuals (swarm)
   * @param params HashMap
   * @throws OptimizerException
   * @return Object
   */
  public Object createNewVelocity(Object x, Object v,
                                  Object p, Object g,
                                  HashMap params) throws OptimizerException;
}
