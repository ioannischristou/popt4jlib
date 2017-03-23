package popt4jlib;

import java.util.HashMap;
import java.io.Serializable;

/**
 * interface class for constructing random chromosome objects used in meta-
 * heuristics such as GA,EA,PS,MC,SA,BH etc.
 * Extends the <CODE>java.io.Serializable</CODE> interface so that implementing 
 * objects can be transported across JVMs in distributed computation.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface RandomChromosomeMakerIntf extends Serializable {

  /**
   * sole method every RandomChromosomeMakerIntf implementation must provide.
	 * Each time the method is called, must return a new object (must not re-use
	 * previously created arrays etc.)
   * @param params HashMap
   * @throws OptimizerException
   * @return Object
   */
  public Object createRandomChromosome(HashMap params) 
		throws OptimizerException;
}

