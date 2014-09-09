package popt4jlib.SA;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * implements the operator for creating variable-length random chromosome objects
 * that are of type <CODE>int[]</CODE>.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntVarArray1CMaker implements RandomChromosomeMakerIntf {
  /**
   * sole public constructor is empty.
   */
  public IntVarArray1CMaker() {
    // no-op
  }


  /**
   * specifies how to create variable-length random chromosomes that are int[]
   * arrays.
   * @param params Hashtable must contain the following pairs:
   * <li> &lt"dsa.maxchromosomelength", $integer_value$&gt mandatory, the max.
   * length the chromosome int[] may take.
   * <li> &lt"dsa.minallelevalue", $integer_value$&gt mandatory, the minimum
   * value for any allele in the chromosome.
   * <li> &lt"dsa.minallelevalue$i$", $integer_value$&gt optional, the minimum
   * value for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,maxchromosome_length-1}). If this value is less than the global val.
   * specified by the "dsa.minallelevalue" key, it is ignored.
   * <li> &lt"dsa.maxallelevalue", $integer_value$&gt mandatory, the max. value
   * for any allele in the chromosome.
   * <li> &lt"dsa.maxallelevalue$i$", $integer_value$&gt optional, the maximum
   * value for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,maxchromosome_length-1}). If this value is greater than the global
   * value specified by the "dsa.maxallelevalue" key, it is ignored.
   * <li> &lt"thread.id",$integer_value"&gt mandatory, the (internal) id of the
   * thread invoking this method; this number is used so as to look-up the right
   * random-number generator associated with the current thread.
   * @throws OptimizerException
   * @return Object int[] of length up to dsa.maxchromosomelength, chosen
   * randomly from the uniform distribution in {1,n}.
   */
  public Object createRandomChromosome(Hashtable params) throws OptimizerException {
    try {
      final int nmax = ( (Integer) params.get("dsa.maxchromosomelength")).intValue();
      final int maxalleleval=((Integer) params.get("dsa.maxallelevalue")).intValue();
      final int minalleleval=((Integer) params.get("dsa.minallelevalue")).intValue();
      final int id = ((Integer) params.get("thread.id")).intValue();
      final int n = 1+RndUtil.getInstance(id).getRandom().nextInt(nmax);
      int[] arr = new int[n];
      for (int i=0; i<n; i++) {
        // ensure bounds, if any
        int maxargvali = maxalleleval;
        Integer MaviI = (Integer) params.get("dsa.maxallelevalue"+i);
        if (MaviI!=null && MaviI.intValue()<maxalleleval)
          maxargvali = MaviI.intValue();
        int minargvali = minalleleval;
        Integer maviD = (Integer) params.get("dsa.minallelevalue"+i);
        if (maviD!=null && maviD.intValue()>minalleleval)
          minargvali = maviD.intValue();

        arr[i] = minargvali +
            RndUtil.getInstance(id).getRandom().nextInt(maxargvali-minargvali+1);
      }
      return arr;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("createRandomChromosome: failed");
    }
  }
}

