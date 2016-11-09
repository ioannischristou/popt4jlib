package popt4jlib.SA;

import popt4jlib.OptimizerException;
import java.util.*;

/**
 * an implementation of the SAScheduleIntf that follows so-called 
 * Cauchy-Annealing schedule. 
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class CauchySchedule implements SAScheduleIntf {

  /**
   * sole constructor (no-op body)
   */
  public CauchySchedule() {

  }


  /**
   * returns the number y = T0/t.
   * @param t int the current (outer-)iteration number
   * @param params HashMap may contain the following key-value pairs:
   * <ul>
	 * <li> &lt;"dsa.T0", $value$&gt; optional, default is 1000.0.
	 * </ul>
   * @throws OptimizerException
   * @return double
   */
  public double getTemp(int t, HashMap params) throws OptimizerException {
    double t0 = 1000.0;
    try {
	    Double tD = (Double) params.get("dsa.T0");
      if (tD != null) t0 = tD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
    return t0/(double)t;
  }

}
