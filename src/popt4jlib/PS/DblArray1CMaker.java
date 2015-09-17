package popt4jlib.PS;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * creates random double[] objects of fixed length, according to parameters
 * passed in a <CODE>HashMap</CODE> object.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1CMaker implements RandomChromosomeMakerIntf {
  /**
   * sole public no-arg constructor (no-op).
   */
  public DblArray1CMaker() {
    // no-op
  }


  /**
   * creates fixed-length <CODE>double[]</CODE> objects, according to certain
   * parameters, specified in the params argument. The value for each element
   * in the array, is drawn from the uniform distribution restricted within the
   * boundaries of the element's range specified in the params key-value pairs.
   * @param p HashMap must contain the following params:
	 * <ul>
   * <li> &lt;"[dpso.]chromosomelength", $integer_value$&gt; mandatory, the length
   * of the chromosome.
   * <li> &lt;"[dpso.]minallelevalue", $value$&gt; mandatory, the minimum value
   * for any allele in the chromosome.
   * <li> &lt;"[dpso.]minallelevalue$i$", $value$&gt; optional, the minimum value
   * for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}). If this value is less than the global value
   * specified by the "[dpso.]minallelevalue" key, it is ignored.
   * <li> &lt;"[dpso.]maxallelevalue", $value$&gt; mandatory, the maximum value
   * for any allele in the chromosome.
   * <li> &lt;"[dpso.]maxallelevalue$i$", $value$&gt; optional, the maximum value
   * for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}). If this value is greater than the global
   * value specified by the "[dpso.]maxallelevalue" key, it is ignored.
   * <li> &lt;"thread.id",$integer_value"&gt; mandatory, the (internal) id of the
   * thread invoking this method; this number is used so as to look-up the right
   * random-number generator associated with the current thread.
	 * </ul>
   * @throws OptimizerException
   * @return Object double[] of length specified in the params.
   */
  public Object createRandomChromosome(HashMap p) throws OptimizerException {
    try {
      LightweightParams params = new LightweightParams(p);
      final int nmax = params.getInteger("dpso.chromosomelength").intValue();
      double maxallelevalg=Double.MAX_VALUE;
      Double maxag = params.getDouble("dpso.maxallelevalue");
      if (maxag!=null) maxallelevalg = maxag.doubleValue();
      double minallelevalg=Double.NEGATIVE_INFINITY;
      Double minag = params.getDouble("dpso.minallelevalue");
      if (minag!=null) minallelevalg = minag.doubleValue();
      final int id = params.getInteger("thread.id").intValue();
      double[] arr = new double[nmax];
      for (int i=0; i<nmax; i++) {
        double minalleleval = minallelevalg;
        Double minai = params.getDouble("dpso.minalleleval"+i);
        if (minai!=null && minai.doubleValue() > minallelevalg)
          minalleleval = minai.doubleValue();
        double maxalleleval = maxallelevalg;
        Double maxai = params.getDouble("dpso.maxalleleval"+i);
        if (maxai!=null && maxai.doubleValue() < maxallelevalg)
          maxalleleval = maxai.doubleValue();
        arr[i] = minalleleval +
            RndUtil.getInstance(id).getRandom().nextDouble()*(maxalleleval-minalleleval);
      }
      return arr;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("createRandomChromosome: failed");
    }
  }
}

