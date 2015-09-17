package popt4jlib;

import java.util.*;

/**
 * the inverse map of the <CODE>Arg2ChromosomeMakerIntf</CODE>. Provides method
 * for converting a chromosome object (notion used in the GA,EA,SA,PS etc. meta-
 * heuristics) into a function argument object (an object belonging to the
 * domain of the function being minimized)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface Chromosome2ArgMakerIntf {
  /**
   * converts a chromosome object (used in the computations of GA etc. meta-
   * heuristics) into an object belonging to the functions' domain.
   * @param chromosome Object
   * @param params HashMap
   * @throws OptimizerException
   * @return Object
   */
  public Object getArg(Object chromosome, HashMap params) throws OptimizerException;  // Chromosome->FunctionArgument Map
}
