package popt4jlib.PS;

import popt4jlib.OptimizerException;
import java.util.HashMap;

/**
 * class specifies the operator for adding velocity objects to chromosome
 * objects, with the help of some parameters specified in a
 * <CODE>HashMap</CODE>. When both objects are arrays of same length, clearly
 * the operator amounts to creating a new array whose elements are the sum of
 * the corresponding elements of the two arrays; in other cases, the "addition"
 * operation would be more complex, and the corresponding class will implement
 * the specification.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
interface ChromosomeVelocityAdderIntf {
  /**
   * the operator that specifies how a velocity object is to be added to a
   * "chromosome" object representing a position in the solution space.
   * @param chromosome Object
   * @param velocity Object
   * @param params HashMap
   * @throws OptimizerException
   * @return Object
   */
  public Object addVelocity2Chromosome(Object chromosome, Object velocity, HashMap params) throws OptimizerException;
}

