package popt4jlib.GA;

import popt4jlib.OptimizerException;
import utils.*;
import java.util.*;

/**
 * implements the "standard" mutation operator for chromosomes represented
 * as <CODE>double[]</CODE> objects, of varying length.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblVarArray1AlleleMutationOp implements MutationOpIntf {

  /**
   * sole public no-arg constructor (with no-op body).
   */
  public DblVarArray1AlleleMutationOp() {
  }


  /**
   * the operation mutates each chromosome argument (not a DGAIndividual, not a
   * Function() arg object) and stores them in a Pair object that is returned.
	 * Notice that the chromosome1 and chromosome2 arguments themselves ARE
	 * actually changed as a result of this call (NO new double[] objects are 
	 * created).
   * @param chromosome1 Object must be double[]
   * @param chromosome2 Object must be double[]
   * @param params Hashtable should contain the following key-value pairs:
	 * <ul>
   * <li> &lt;"dga.minallelevalue", $value$&gt; optional, the minimum value for
   * any allele in the chromosome. Default -Infinity.
   * <li> &lt;"dga.minallelevalue$i$", $value$&gt; optional, the minimum value for
   * the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}. If this value is less than the global value
   * specified by the "dga.minallelevalue" key, it is ignored.
   * <li> &lt;"dga.maxallelevalue", $value$&gt; optional, the maximum value for
   * any allele in the chromosome. Default +Infinity.
   * <li> &lt;"dga.maxallelevalue$i$", $value$&gt; optional, the maximum value for
   * the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}. If this value is greater than the global value
   * specified by the "dga.maxallelevalue" key, it is ignored.
   * <li> &lt;"thread.id",$integer_value"&gt; mandatory, the (internal) id of the
   * thread invoking this method; this number is used so as to look-up the right
   * random-number generator associated with the current thread.
	 * </ul>
   * @throws OptimizerException if any of the above params is incorrectly set
   * @return Pair Pair&lt;double[], double[]&gt;
   */
  public Pair doMutation(Object chromosome1, Object chromosome2, Hashtable params) throws OptimizerException {
    try {
      final int id = ( (Integer) params.get("thread.id")).intValue();
      double[] a1 = (double[]) chromosome1;
      double[] a2 = (double[]) chromosome2;
      double min = Double.MAX_VALUE;
      double max = Double.NEGATIVE_INFINITY;
      int n1 = a1.length;
      for (int i = 0; i < n1; i++) {
        if (max < a1[i]) max = a1[i];
        else if (min > a1[i]) min = a1[i];
      }
      int r1 = RndUtil.getInstance(id).getRandom().nextInt(n1);
      double rv = RndUtil.getInstance(id).getRandom().nextGaussian();
      a1[r1] += (max - min) * rv;
      // check if value exceeds any limits
      Double minargval = (Double) params.get("dga.minallelevalue");
      if (minargval != null && minargval.doubleValue() > a1[r1])
        a1[r1] = minargval.doubleValue();
      Double minargvalr1 = (Double) params.get("dga.minallelevalue"+r1);
      if (minargvalr1 != null && minargvalr1.doubleValue() > a1[r1])
        a1[r1] = minargvalr1.doubleValue();

      Double maxargval = (Double) params.get("dga.maxallelevalue");
      if (maxargval != null && maxargval.doubleValue() < a1[r1])
        a1[r1] = maxargval.doubleValue();
      Double maxargvalr1 = (Double) params.get("dga.maxallelevalue"+r1);
      if (maxargvalr1 != null && maxargvalr1.doubleValue() < a1[r1])
        a1[r1] = maxargvalr1.doubleValue();

      int n2 = a2.length;
      for (int i = 0; i < n2; i++) {
        if (max < a2[i]) max = a2[i];
        else if (min > a2[i]) min = a2[i];
      }
      int r2 = RndUtil.getInstance(id).getRandom().nextInt(n2);
      rv = RndUtil.getInstance(id).getRandom().nextGaussian();
      a2[r2] += (max - min) * rv;
      if (minargval != null && minargval.doubleValue() > a2[r2])
        a2[r2] = minargval.doubleValue();
      Double minargvalr2 = (Double) params.get("dga.minallelevalue"+r2);
      if (minargvalr2 != null && minargvalr2.doubleValue() > a2[r2])
        a2[r2] = minargvalr2.doubleValue();

      if (maxargval != null && maxargval.doubleValue() < a2[r2])
        a2[r2] = maxargval.doubleValue();
      Double maxargvalr2 = (Double) params.get("dga.maxallelevalue"+r2);
      if (maxargvalr2 != null && maxargvalr2.doubleValue() < a2[r2])
        a2[r2] = maxargvalr2.doubleValue();

      Pair p = new Pair(a1, a2);
      return p;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("doMutation(): failed");
    }
  }

}
