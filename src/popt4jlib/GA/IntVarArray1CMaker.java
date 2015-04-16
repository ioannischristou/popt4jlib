package popt4jlib.GA;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * creates random int[] objects of varying length, according to parameters
 * passed in a <CODE>Hashtable</CODE> object.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntVarArray1CMaker implements RandomChromosomeMakerIntf {
  /**
   * public no-arg constructor (no-op body)
   */
  public IntVarArray1CMaker() {
  }


  /**
   * creates variable-length <CODE>int[]</CODE> objects, according to certain
   * parameters, specified in the params argument. The value for each element
   * in the array, is drawn from the uniform distribution restricted within the
   * boundaries of the element's range specified in the params key-value pairs.
	 * The array's element values are computed using the uniform distribution in 
	 * the specified range of acceptable values.
   * @param params Hashtable must contain the following params:
	 * <ul>
   * <li> &lt;"dga.maxchromosomelength", $integer_value$&gt; mandatory, the max.
   * length of the chromosome.
   * <li> &lt;"dga.minallelevalue", $integer_value$&gt; mandatory, the minimum
   * value for any allele in the chromosome.
   * <li> &lt;"dga.minallelevalue$i$", $integer_value$&gt; optional, the minimum
   * value for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}. If this value is less than the global value
   * specified by the "dga.minallelevalue" key, it is ignored.
   * <li> &lt;"dga.maxallelevalue", $integer_value$&gt; mandatory, the maximum
   * value for any allele in the chromosome.
   * <li> &lt;"dga.maxallelevalue$i$", $integer_value$&gt; optional, the maximum
   * value for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}. If this value is greater than the global value
   * specified by the "dga.maxallelevalue" key, it is ignored.
   * <li> &lt;"thread.id",$integer_value"&gt; mandatory, the (internal) id of the
   * thread invoking this method; this number is used so as to look-up the right
   * random-number generator associated with the current thread.
	 * </ul>
   * @throws OptimizerException if any of the above params is incorrectly set
   * @return Object int[] of length specified in the params.
   */
  public Object createRandomChromosome(Hashtable params) throws OptimizerException {
    try {
      final int nmax = ( (Integer) params.get("dga.maxchromosomelength")).intValue();
      final int maxalleleval=((Integer) params.get("dga.maxallelevalue")).intValue();
      final int minalleleval=((Integer) params.get("dga.minallelevalue")).intValue();
      final int id = ((Integer) params.get("thread.id")).intValue();
      final int n = 1+RndUtil.getInstance(id).getRandom().nextInt(nmax);
      int[] arr = new int[n];
      for (int i=0; i<n; i++) {
        // ensure bounds, if any
        int maxargvali = maxalleleval;
        Integer MaviI = (Integer) params.get("dga.maxallelevalue"+i);
        if (MaviI!=null && MaviI.intValue()<maxalleleval)
          maxargvali = MaviI.intValue();
        int minargvali = minalleleval;
        Integer maviD = (Integer) params.get("dga.minallelevalue"+i);
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
