package popt4jlib.PS;

import popt4jlib.OptimizerException;
import utils.RndUtil;
import java.util.Hashtable;

/**
 * the standard implementation of NewVelocityMakerIntf for the case where
 * all objects are <CODE>double[]</CODE> objects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1StdVelocityMaker implements NewVelocityMakerIntf {
  /**
   * sole public constructor (no-op).
   */
  public DblArray1StdVelocityMaker() {
  }


  /**
   * implements the standard formula for velocity updates of a particle
   * (see wikipedia entry: Particle_swarm_optimization).
   * @param x Object of type double[]
   * @param v Object of type double[]
   * @param p Object of type double[]
   * @param g Object of type double[]
   * @param params Hashtable may contain the following entries:
   * <li> &lt;"dpso.fp", $value$&gt; optional, the parameter ö_p, default is 2.0.
   * <li> &lt;"dpso.fg", $value$&gt; optional, the parameter ö_g, default is 2.0.
   * <li> &lt;"dpso.w", $value$&gt; optional, the parameter ù, default is 0.6.
   * <li> &lt;"thread.id",$integer_value"&gt; mandatory, the (internal) id of the
   * thread invoking this method; this number is used so as to look-up the right
   * random-number generator associated with the current thread.
   * @throws OptimizerException if any of the parameters are null or not of type
   * <CODE>double[]</CODE> or not of the same length.
   * @return Object
   */
  public Object createNewVelocity(Object x, Object v, Object p, Object g,
                                  Hashtable params)
      throws OptimizerException {
    if (x==null || v==null || p==null || g==null || params==null)
      throw new OptimizerException("null args passed in");
    try {
      final double[] x2 = (double[]) x;
      final double[] v2 = (double[]) v;
      final double[] p2 = (double[]) p;
      final double[] g2 = (double[]) g;
      final int id = ((Integer) params.get("thread.id")).intValue();
      if (x2.length!=v2.length || x2.length!=p2.length || x2.length!=g2.length)
        throw new OptimizerException("arg arrays have different lengths");
      final int n = x2.length;
      double[] result = new double[x2.length];
      double rp = RndUtil.getInstance(id).getRandom().nextDouble();
      double rg = RndUtil.getInstance(id).getRandom().nextDouble();
      double fp = 2.0;
      Double fpD = (Double) params.get("dpso.fp");
      if (fpD!=null && fpD.doubleValue()>=0) fp = fpD.doubleValue();
      double fg = 2.0;
      Double fgD = (Double) params.get("dpso.fg");
      if (fgD!=null && fgD.doubleValue()>=0) fg = fgD.doubleValue();
      double w = 0.6;
      Double wD = (Double) params.get("dpso.w");
      if (wD!=null && wD.doubleValue()>=0) w = wD.doubleValue();
      for (int i=0; i<n; i++) {
        result[i] = w*v2[i] + fp*rp*(p2[i]-x2[i]) + fg*rg*(g2[i]-x2[i]);
      }
      return result;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("createNewVelocity(): failed");
    }
  }
}
