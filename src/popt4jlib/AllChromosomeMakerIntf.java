package popt4jlib;

import java.util.*;
import java.io.*;

/**
 * allows the definition of the neighborhood of a point in a search space that
 * is needed by Local-Search algorithms in combinatorial optimization problems.
 * Extends the <CODE>java.io.Serializable</CODE> interface so that implementing 
 * objects can be transported across JVMs in distributed computation.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface AllChromosomeMakerIntf extends Serializable {
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

