package popt4jlib;

import java.util.HashMap;

/**
 * class implements the identity Arg-&gt;;Chromosome transformation. The class is
 * simply an auxiliary class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IdentityArg2CMaker implements Arg2ChromosomeMakerIntf {
  /**
   * public no-arg no-op constructor
   */
  public IdentityArg2CMaker() {
  }


  /**
   * returns the argument passed in.
   * @param c Object
   * @param params HashMap
   * @return Object
   */
  public Object getChromosome(Object c, HashMap params) {
    return c;
  }
}
