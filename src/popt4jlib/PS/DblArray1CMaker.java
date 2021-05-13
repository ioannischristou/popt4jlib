package popt4jlib.PS;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * creates random double[] objects of fixed length, according to parameters
 * passed in a <CODE>HashMap</CODE> object.
 * <p>Notes:
 * <ul>
 * <li>2021-05-13: fixed mispelling of key "dpso.[min|max]argval$i$" when 
 * looking for specific components' minimum and/or maximum allowed values.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.1
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
			Messenger mger = Messenger.getInstance();
      LightweightParams params = new LightweightParams(p);
      final int id = params.getInteger("thread.id").intValue();
			final Random rnd = RndUtil.getInstance(id).getRandom();
      final int nmax = params.getInteger("dpso.chromosomelength").intValue();
      double maxallelevalg=Double.MAX_VALUE;
      Double maxag = params.getDouble("dpso.maxallelevalue");
      if (maxag!=null) maxallelevalg = maxag.doubleValue();
      double minallelevalg=Double.NEGATIVE_INFINITY;
      Double minag = params.getDouble("dpso.minallelevalue");
      if (minag!=null) minallelevalg = minag.doubleValue();
      double[] arr = new double[nmax];
      for (int i=0; i<nmax; i++) {
        double minalleleval = minallelevalg;
        Double minai = params.getDouble("dpso.minallelevalue"+i);
        if (minai!=null && minai.doubleValue() > minallelevalg)
          minalleleval = minai.doubleValue();
        double maxalleleval = maxallelevalg;
        Double maxai = params.getDouble("dpso.maxallelevalue"+i);
        if (maxai!=null && maxai.doubleValue() < maxallelevalg)
          maxalleleval = maxai.doubleValue();
        arr[i] = minalleleval +
            rnd.nextDouble()*(maxalleleval-minalleleval);
      }
			// diagnostics
			if (mger.getDebugLvl()>=2) {
				String arrstr = "DblArray1CMaker.createRandomChromosome(): res=[";
				for (int i=0; i<arr.length; i++) {
					arrstr += arr[i];
					if (i<arr.length-1) arrstr += ",";
				}
				arrstr += "]";
				mger.msg(arrstr, 2);
			}
      return arr;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("createRandomChromosome: failed");
    }
  }
}

