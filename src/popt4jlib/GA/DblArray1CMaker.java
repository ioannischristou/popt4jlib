package popt4jlib.GA;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * creates random <CODE>double[]</CODE> objects of fixed length, according to 
 * parameters passed in a <CODE>HashMap</CODE> object.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1CMaker implements RandomChromosomeMakerIntf {

  /**
   * sole public constructor (no-op body)
   */
  public DblArray1CMaker() {
  }


  /**
   * creates fixed-length <CODE>double[]</CODE> objects, according to certain
   * parameters, specified in the params argument. The value for each element
   * in the array, is drawn from the uniform distribution restricted within the
   * boundaries of the element's range specified in the params key-value pairs.
   * @param params HashMap must contain the following params:
	 * <ul>
   * <li> &lt;"dga.chromosomelength", $integer_value$&gt; mandatory, the length
   * of the chromosome.
   * <li> &lt;"dga.minallelevalue", $value$&gt; mandatory, the minimum value for
   * any allele in the chromosome.
   * <li> &lt;"dga.minallelevalue$i$", $value$&gt; optional, the minimum value for
   * the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}). If this value is less than the global value
   * specified by the "dga.minallelevalue" key, it is ignored.
   * <li> &lt;"dga.maxallelevalue", $value$&gt; mandatory, the maximum value for
   * any allele in the chromosome.
   * <li> &lt;"dga.maxallelevalue$i$", $value$&gt; optional, the maximum value for
   * the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}). If this value is greater than the global value
   * specified by the "dga.maxallelevalue" key, it is ignored.
   * <li> &lt;"thread.id",$integer_value"&gt; mandatory, the (internal) id of the
   * thread invoking this method; this number is used so as to look-up the right
   * random-number generator associated with the current thread.
	 * </ul>
   * @throws OptimizerException if any of the above conditions doesn't hold
   * @return Object double[] of length specified in the params
   */
  public Object createRandomChromosome(HashMap params) throws OptimizerException {
    try {
      final int nmax = ( (Integer) params.get("dga.chromosomelength")).intValue();
      final double maxalleleval=((Double) params.get("dga.maxallelevalue")).doubleValue();
      final double minalleleval=((Double) params.get("dga.minallelevalue")).doubleValue();
      final int id = ((Integer) params.get("thread.id")).intValue();
      double[] arr = new double[nmax];
      for (int i=0; i<nmax; i++) {
        // restore within bounds, if any
        double maxargvali = maxalleleval;
        Double MaviD = (Double) params.get("dga.maxallelevalue"+i);
        if (MaviD!=null && MaviD.doubleValue()<maxalleleval)
          maxargvali = MaviD.doubleValue();
        double minargvali = minalleleval;
        Double maviD = (Double) params.get("dga.minallelevalue"+i);
        if (maviD!=null && maviD.doubleValue()>minalleleval)
          minargvali = maviD.doubleValue();

        arr[i] = minargvali +
            RndUtil.getInstance(id).getRandom().nextDouble()*(maxargvali-minargvali);
      }
      return arr;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("createRandomChromosome: failed");
    }
  }
}
