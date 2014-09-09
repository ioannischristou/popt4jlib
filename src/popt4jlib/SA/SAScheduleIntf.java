package popt4jlib.SA;

import popt4jlib.OptimizerException;
import java.util.*;

/**
 * defines the contract for a temperature cooling schedule -primarily depending
 * on the outer-most iteration number of the SA process, but also possibly on
 * other parameters and/or variable values that may be passed via the Hashtable
 * second argument of the main method of the interface.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
interface SAScheduleIntf {

  /**
   * main method of the interface, returning the temperature value given an
   * iteration number.
   * @param gen int the outer-most iteration number in the SA process.
   * @param props Hashtable any other parameters
   * @throws OptimizerException
   * @return double
   */
  public double getTemp(int gen, Hashtable props) throws OptimizerException;
}

