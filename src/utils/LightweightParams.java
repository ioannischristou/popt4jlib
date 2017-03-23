package utils;

import java.util.*;


/**
 * helper class that allows searching parameters hierarchically by name.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LightweightParams extends Params {

  /**
   * public constructor. Does NOT make a copy of the HashMap.
   * @param p HashMap
   */
  public LightweightParams(HashMap p) {
    super(p,true);
  }

}
