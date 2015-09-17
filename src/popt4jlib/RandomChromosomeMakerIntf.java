package popt4jlib;

import java.util.*;

/**
 * interface class for constructing random chromosome objects used in meta-
 * heuristics such as GA,EA,PS,MC,SA etc.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface RandomChromosomeMakerIntf {

  /**
   * the sole method every RandomChromosomeMakerIntf implementation must provide.
   * @param params HashMap
   * @throws OptimizerException
   * @return Object
   */
  public Object createRandomChromosome(HashMap params) throws OptimizerException;
}

