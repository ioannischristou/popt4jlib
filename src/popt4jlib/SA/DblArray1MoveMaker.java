package popt4jlib.SA;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * specifies how to make a move from one position to the next, when "positions"
 * are <CODE>double[]</CODE> objects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1MoveMaker implements NewChromosomeMakerIntf {
  /**
   * sole public constructor is empty.
   */
  public DblArray1MoveMaker() {
    // no-op
  }


  /**
   * specifies according to the standard Simulated Annealing update formula,
   * the next solution based on the current one, given in the first argument.
   * See wikipedia entry: Simulated_annealing.
   * Double values in the alleles of the new position, are projected to the
   * box-constrained integer feasible region (whenever box-constraints exist).
   * @param chromosome Object double[]
   * @param params HashMap must contain the following pairs:
	 * <ul>
   * <li> &lt;"dsa.minallelevalue", $value$&gt; mandatory, the minimum value
   * for any allele in the chromosome.
   * <li> &lt;"dsa.minallelevalue$i$", $value$&gt; optional, the minimum value
   * for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}). If this value is less than the global value
   * specified by the "dsa.minallelevalue" key, it is ignored.
   * <li> &lt;"dsa.maxallelevalue", $value$&gt; mandatory, the maximum value
   * for any allele in the chromosome.
   * <li> &lt;"dsa.maxallelevalue$i$", $value$&gt; optional, the maximum value
   * for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}). If this value is greater than the global
   * value specified by the "dsa.maxallelevalue" key, it is ignored.
   * <li> &lt;"thread.id",$integer_value"&gt; mandatory, the (internal) id of the
   * thread invoking this method; this number is used so as to look-up the right
   * random-number generator associated with the current thread.
   * <li> &lt;"dsa.movedelta",$value"&gt; mandatory, the value of the ä parameter.
	 * </ul>
   * @throws OptimizerException
   * @return Object double[]
   */
  public Object createNewChromosome(Object chromosome, HashMap params) throws OptimizerException {
    double[] arg = (double[]) chromosome;  // chromosome is a double[] array
    double[] res = new double[arg.length];
    double delta = ((Double) params.get("dsa.movedelta")).doubleValue();
    double maxv = ((Double) params.get("dsa.maxallelevalue")).doubleValue();
    double minv = ((Double) params.get("dsa.minallelevalue")).doubleValue();
    int tid = ((Integer) params.get("thread.id")).intValue();
    for (int i=0; i<arg.length; i++) {
      double rd = RndUtil.getInstance(tid).getRandom().nextDouble();
      res[i] = arg[i] + (2*rd-1)*delta;
      // restore within bounds, if any
      double maxargvali = maxv;
      Double MaviD = (Double) params.get("dsa.maxallelevalue"+i);
      if (MaviD!=null && MaviD.doubleValue()<maxv)
        maxargvali = MaviD.doubleValue();
      double minargvali = minv;
      Double maviD = (Double) params.get("dsa.minallelevalue"+i);
      if (maviD!=null && maviD.doubleValue()>minv)
        minargvali = maviD.doubleValue();
      if (res[i]<minargvali) res[i]=minargvali;
      else if (res[i]>maxargvali) res[i]=maxargvali;
    }
    return res;
  }
}

