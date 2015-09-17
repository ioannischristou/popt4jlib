package popt4jlib;

import java.util.*;

/**
 * allows the definition of the neighborhood of a point in a search space that
 * is needed by Local-Search algorithms in combinatorial optimization problems.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface AllChromosomeMakerIntf {
  /**
   * the main method: given an existing chromosome and some parameters,
   * construct ALL new chromosome objects within the neighborhood of this
   * chromosome.
   * @param chromosome Object
   * @param params HashMap
   * @throws OptimizerException
   * @return Vector // Vector&lt;Object chromosome&gt;
   */
  public Vector createAllChromosomes(Object chromosome, HashMap params) throws OptimizerException;
}

