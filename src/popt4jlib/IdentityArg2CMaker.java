package popt4jlib;

import java.util.Hashtable;

/**
 * class implements the identity Arg->Chromosome transformation. The class is
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
   * @param params Hashtable
   * @return Object
   */
  public Object getChromosome(Object c, Hashtable params) {
    return c;
  }
}
