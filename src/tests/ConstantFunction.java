package tests;

import popt4jlib.*;
import java.util.*;


/**
 * The constant function trivially returns the value of the key "const" in the 
 * params, or zero if it can't find the key.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ConstantFunction implements FunctionIntf {
  /**
   * public default no-arg constructor
   */
  public ConstantFunction() {
  }

  /**
   * return the value of key "const" in the params of the function.
   * The params map may contain the parameter:
   * &lt;"const", Double val&gt; which has the value val to be returned; else
	 * zero will be returned.
   * @param arg Object unused.
   * @param params HashMap
   * @return double
   */
  public double eval(Object arg, HashMap params) {
    try {
			if (params==null) return 0.0;
      Double aD = (Double) params.get("const");
      if (aD != null) return aD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
		return 0.0;
  }
}
