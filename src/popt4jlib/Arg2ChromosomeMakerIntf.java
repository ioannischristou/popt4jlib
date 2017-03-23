package popt4jlib;

import java.util.HashMap;
import java.io.Serializable;

/**
 * interface defining the methods that an object must support so as to be
 * able to convert a function argument (an object type that the
 * function being minimized accepts) into a chromosome object (notion used in
 * GA, SA, PS, EA meta-heuristics among others). The "inverse" operation is
 * provided by the <CODE>Chromosome2ArgMakerIntf</CODE>.
 * Extends the <CODE>java.io.Serializable</CODE> interface so that implementing 
 * objects can be transported across JVMs in distributed computation.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface Arg2ChromosomeMakerIntf extends Serializable {
  /**
   * converts an object belonging to the domain of the function being minimized
   * into a chromosome object that the meta-heuristics (GA etc.) manipulate.
   * @param chromosome Object
   * @param params HashMap
   * @throws OptimizerException
   * @return Object a chromosome object.
   */
  public Object getChromosome(Object chromosome, HashMap params)
      throws OptimizerException;  // FunctionArgument->Chromosome Map
}

