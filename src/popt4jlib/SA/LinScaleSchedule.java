package popt4jlib.SA;

import popt4jlib.OptimizerException;
import java.util.*;

/**
 * an implementation of the SAScheduleIntf that is based on a step-function
 * linear cooling schedule (the temperature remains the same for a fixed
 * number of iterations, and then drops linearly to the next value.)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LinScaleSchedule implements SAScheduleIntf {

  /**
   * sole constructor (no-op body)
   */
  public LinScaleSchedule() {

  }


  /**
   * returns the number y = t0 / L^2 where L = 1 + [K*t/t0] where [x] is the
   * floor of x.
   * @param t int
   * @param params HashMap may contain the following key-value pairs:
   * <li> &lt;"dsa.T0", $value$&gt; optional, default is 1000.0.
   * <li> &lt;"dsa.K", $value$&gt; optional, default is 20.0.
   * @throws OptimizerException
   * @return double
   */
  public double getTemp(int t, HashMap params) throws OptimizerException {
    double t0 = 1000.0;
    Double tD = (Double) params.get("dsa.T0");
    double k = 20.0;
    Double kD = (Double) params.get("dsa.K");
    try {
      if (kD != null) k = kD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
    try {
      if (tD != null) t0 = tD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
    double lvl = 1.0 + Math.floor(k*t/t0);
    return t0/(lvl*lvl);
  }

}
