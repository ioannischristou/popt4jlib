package popt4jlib;

import java.util.*;

/**
 * class implements the identity Chromosome-&gt;;Arg transformation. The class is
 * simply an auxiliary class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IdentityC2ArgMaker implements Chromosome2ArgMakerIntf {
  /**
   * public no-arg no-op constructor
   */
  public IdentityC2ArgMaker() {
  }


  /**
   * returns the argument passed in.
   * @param c Object
   * @param params Hashtable
   * @return Object
   */
  public Object getArg(Object c, Hashtable params) {
    return c;
  }
}
