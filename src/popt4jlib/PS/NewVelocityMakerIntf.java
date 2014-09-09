package popt4jlib.PS;

import popt4jlib.OptimizerException;
import java.util.Hashtable;

/**
 * specifies how to create new velocity objects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
interface NewVelocityMakerIntf {
  /**
   *
   * @param x Object the current individual's chromosome (i.e. particle position)
   * @param v Object the current individual's velocity
   * @param p Object the best position the individual was found in
   * @param g Object the best position found for the island of individuals (swarm)
   * @param params Hashtable
   * @throws OptimizerException
   * @return Object
   */
  public Object createNewVelocity(Object x, Object v,
                                  Object p, Object g,
                                  Hashtable params) throws OptimizerException;
}
