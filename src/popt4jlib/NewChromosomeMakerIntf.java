package popt4jlib;

import java.util.*;

/**
 * allows the creation of a new Object that will play the role of a chromosome
 * in algorithms such as GA,EA,PS, etc. using a starting point
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface NewChromosomeMakerIntf {
  /**
   * the main method: given an existing chromosome and some parameters,
   * construct a new chromosome object.
   * @param chromosome Object
   * @param params HashMap
   * @throws OptimizerException
   * @return Object
   */
  public Object createNewChromosome(Object chromosome, HashMap params) throws OptimizerException;
}

