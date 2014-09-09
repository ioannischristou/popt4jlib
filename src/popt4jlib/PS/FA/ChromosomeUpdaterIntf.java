package popt4jlib.PS.FA;

import java.util.Hashtable;
import popt4jlib.OptimizerException;

/**
 * updates chromosome-i to move towards chromosome-j according to the params
 * found in params.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface ChromosomeUpdaterIntf {
  /**
   * creates a new chromosome (starting with chromosomei) that is somehow closer
   * to chromosomej.
   * @param chromosomei Object
   * @param chromosomej Object
   * @param params Hashtable
   * @throws OptimizerException
   * @return Object
   */
  public Object update(Object chromosomei, Object chromosomej, Hashtable params) throws OptimizerException;
  /**
   * optional operation, needed only if some kind of "annealing" of the a_t
   * parameters is to take place. Otherwise, may be left as a no-op operation.
   */
  public void incrementGeneration();
}

