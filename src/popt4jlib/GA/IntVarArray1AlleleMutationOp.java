package popt4jlib.GA;

import popt4jlib.OptimizerException;
import utils.*;
import java.util.*;

/**
 * Class implements the "standard" mutation operator for chromosomes represented
 * as <CODE>int[]</CODE> objects, of varying length.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntVarArray1AlleleMutationOp implements MutationOpIntf {

  /**
   * public constructor (no-op body).
   */
  public IntVarArray1AlleleMutationOp() {
  }


  /**
   * the operation mutates each chromosome argument (not a DGAIndividual, not a
   * Function() arg object) and stores them in a Pair object that is returned.
	 * Notice that the two chromosome arguments ARE actually changed as a result
	 * of this call (NO new int[]'s are created). The mutations occur according to
	 * Gaussian processes but obey the range of values specified in params.
   * @param chromosome1 Object int[]
   * @param chromosome2 Object int[]
   * @param params HashMap must contain the following:
	 * <ul>
   * <li> &lt;"dga.minallelevalue", $integer_value$&gt; optional, the minimum
   * value for any allele in the chromosome. Default -Infinity.
   * <li> &lt;"dga.minallelevalue$i$", $integer_value$&gt; optional, the minimum
   * value for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}. If this value is less than the global value
   * specified by the "dga.minallelevalue" key, it is ignored.
   * <li> &lt;"dga.maxallelevalue", $integer_value$&gt; optional, the maximum
   * value for any allele in the chromosome. Default +Infinity.
   * <li> &lt;"dga.maxallelevalue$i$", $integer_value$&gt; optional, the maximum
   * value for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}. If this value is greater than the global value
   * specified by the "dga.maxallelevalue" key, it is ignored.
   * <li> &lt;"thread.id",$integer_value"&gt; mandatory, the (internal) id of the
   * thread invoking this method; this number is used so as to look-up the right
   * random-number generator associated with the current thread.
	 * </ul>
   * @throws OptimizerException if any of the above params is incorrectly set
   * @return Pair containing the two <CODE>int[]</CODE> arguments: chromosome1 
	 * and chromosome2
   */
  public Pair doMutation(Object chromosome1, Object chromosome2, HashMap params) throws OptimizerException {
    try {
      final int id = ( (Integer) params.get("thread.id")).intValue();
      int[] a1 = (int[]) chromosome1;
      int[] a2 = (int[]) chromosome2;
      int min = Integer.MAX_VALUE;
      int max = Integer.MIN_VALUE;
      int n1 = a1.length;
      for (int i = 0; i < n1; i++) {
        if (max < a1[i]) max = a1[i];
        else if (min > a1[i]) min = a1[i];
      }
      int r1 = RndUtil.getInstance(id).getRandom().nextInt(n1);
      double rv = RndUtil.getInstance(id).getRandom().nextGaussian();
      a1[r1] += Math.floor((max - min) * rv);
      // check if value exceeds any limits
      Integer minargval = (Integer) params.get("dga.minallelevalue");
      if (minargval != null && minargval.intValue() > a1[r1])
        a1[r1] = minargval.intValue();
      Integer maxargval = (Integer) params.get("dga.maxallelevalue");
      if (maxargval != null && maxargval.intValue() < a1[r1])
        a1[r1] = maxargval.intValue();
      Integer minargvalr1 = (Integer) params.get("dga.minallelevalue"+r1);
      if (minargvalr1 != null && minargvalr1.intValue() > a1[r1])
        a1[r1] = minargvalr1.intValue();
      Integer maxargvalr1 = (Integer) params.get("dga.maxallelevalue"+r1);
      if (maxargvalr1 != null && maxargvalr1.intValue() < a1[r1])
        a1[r1] = maxargvalr1.intValue();

      int n2 = a2.length;
      for (int i = 0; i < n2; i++) {
        if (max < a2[i]) max = a2[i];
        else if (min > a2[i]) min = a2[i];
      }
      int r2 = RndUtil.getInstance(id).getRandom().nextInt(n2);
      rv = RndUtil.getInstance(id).getRandom().nextGaussian();
      a2[r2] += Math.floor((max - min) * rv);
      if (minargval != null && minargval.intValue() > a2[r2])
        a2[r2] = minargval.intValue();
      Integer minargvalr2 = (Integer) params.get("dga.minallelevalue"+r2);
      if (minargvalr2 != null && minargvalr2.intValue() > a2[r2])
        a2[r2] = minargvalr2.intValue();
      if (maxargval != null && maxargval.intValue() < a2[r2])
        a2[r2] = maxargval.intValue();
      Integer maxargvalr2 = (Integer) params.get("dga.maxallelevalue"+r2);
      if (maxargvalr2 != null && maxargvalr2.intValue() < a2[r2])
        a2[r2] = maxargvalr2.intValue();


      Pair p = new Pair(a1, a2);
      return p;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("doMutation(): failed");
    }
  }

}
