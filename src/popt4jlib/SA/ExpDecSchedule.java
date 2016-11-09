package popt4jlib.SA;

import popt4jlib.OptimizerException;
import java.util.*;

/**
 * an implementation of the SAScheduleIntf that follows the so-called 
 * exponential-decrease schedule (for an application of this schedule, see
 * Gregoriou (ed.), "Handbook of Trading: Strategies for Navigating and Profiting in
 * Currency, Bond and Stock Markets", McGraw-Hill, 2010). 
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class ExpDecSchedule implements SAScheduleIntf {

  /**
   * sole constructor (no-op body)
   */
  public ExpDecSchedule() {

  }


  /**
   * returns the number y = T0 * exp(-K*t^(1/D)).
   * @param t int the current (outer-)iteration number
   * @param params HashMap may contain the following key-value pairs:
   * <ul>
	 * <li> &lt;"dsa.T0", $value$&gt; optional, default is 1000.0.
   * <li> &lt;"dsa.K", $value$&gt; optional, default is 20.0.
	 * <li> &lt;"dsa.D", $value$&gt; optional, default is 2.
	 * </ul>
   * @throws OptimizerException
   * @return double
   */
  public double getTemp(int t, HashMap params) throws OptimizerException {
    double t0 = 1000.0;
    double k = 20.0;
		double D = 2.0;
    try {
	    Double kD = (Double) params.get("dsa.K");
      if (kD != null) k = kD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
    try {
	    Double tD = (Double) params.get("dsa.T0");
      if (tD != null) t0 = tD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
    try {
	    Double DD = (Double) params.get("dsa.D");
      if (DD != null) D = DD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
    return t0*Math.exp(-k*Math.pow(t, 1.0/D));
  }

}
