package popt4jlib.SA;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * specifies how to make a move from one position to the next, when "positions"
 * are <CODE>int[]</CODE> objects of varying length.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntVarArray1MoveMaker implements NewChromosomeMakerIntf {
  /**
   * sole public constructor is empty.
   */
  public IntVarArray1MoveMaker() {
    // no-op
  }


  /**
   * specifies according to the standard Simulated Annealing update formula,
   * the next solution based on the current one, given in the first argument.
   * See wikipedia entry: Simulated_annealing.
   * Double values in the alleles of the new position, are rounded off to the
   * nearest integer, and then projected to the box-constrained integer feasible
   * region (if any such box-constraints exist).
   * @param chromosome Object int[]
   * @param params Hashtable must contain the following pairs:
   * <li> &lt"dsa.minallelevalue", $integer_value$&gt mandatory, the min. value
   * for any allele in the chromosome.
   * <li> &lt"dsa.minallelevalue$i$", $integer_value$&gt optional, the min.
   * value for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}). If this value is less than the global value
   * specified by the "dsa.minallelevalue" key, it is ignored.
   * <li> &lt"dsa.maxallelevalue", $integer_value$&gt mandatory, the max. value
   * for any allele in the chromosome.
   * <li> &lt"dsa.maxallelevalue$i$", $integer_value$&gt optional, the max.
   * value for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}). If this value is greater than the global
   * value specified by the "dsa.maxallelevalue" key, it is ignored.
   * <li> &lt"thread.id",$integer_value"&gt mandatory, the (internal) id of the
   * thread invoking this method; this number is used so as to look-up the right
   * random-number generator associated with the current thread.
   * <li> &lt"dsa.movedelta",$value"&gt mandatory, the value of the ä parameter.
   * @throws OptimizerException
   * @return Object int[]
   */
  public Object createNewChromosome(Object chromosome, Hashtable params) throws OptimizerException {
    int[] arg = (int[]) chromosome;  // chromosome is a int[] array of var. length
    int[] res = new int[arg.length];
    double delta = ((Double) params.get("dsa.movedelta")).doubleValue();
    int maxv = ((Integer) params.get("dsa.maxallelevalue")).intValue();
    int minv = ((Integer) params.get("dsa.minallelevalue")).intValue();
    int tid = ((Integer) params.get("thread.id")).intValue();
    for (int i=0; i<arg.length; i++) {
      // ensure bounds, if any
      int maxargvali = maxv;
      Integer MaviI = (Integer) params.get("dsa.maxallelevalue"+i);
      if (MaviI!=null && MaviI.intValue()<maxv)
        maxargvali = MaviI.intValue();
      int minargvali = minv;
      Integer maviD = (Integer) params.get("dsa.minallelevalue"+i);
      if (maviD!=null && maviD.intValue()>minv)
        minargvali = maviD.intValue();

      double rd = RndUtil.getInstance(tid).getRandom().nextDouble();
      res[i] = arg[i] + (int) Math.round((2*rd-1)*delta);
      if (res[i]<minargvali) res[i]=minargvali;
      else if (res[i]>maxargvali) res[i]=maxargvali;
    }
    return res;
  }
}

