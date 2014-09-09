package popt4jlib.PS;

import popt4jlib.*;
import java.util.*;

/**
 * specifies how to create random velocity objects, given some parameters
 * included in a <CODE>Hashtable</CODE> object.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
interface RandomVelocityMakerIntf {
  /**
   * returns a velocity object, created using the parameters passed in as
   * argument.
   * @param params Hashtable
   * @throws OptimizerException
   * @return Object
   */
  public Object createRandomVelocity(Hashtable params) throws OptimizerException;
}

