package popt4jlib.SA;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * implements the operator for creating random chromosome objects that are of
 * type <CODE>double[]</CODE>.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1CMaker implements RandomChromosomeMakerIntf {
  /**
   * sole public constructor is empty.
   */
  public DblArray1CMaker() {
    // no-op
  }

  /**
   * creates fixed-length <CODE>double[]</CODE> objects, according to certain
   * parameters, specified in the params argument. The value for each element
   * in the array, is drawn from the uniform distribution restricted within the
   * boundaries of the element's range specified in the params key-value pairs.
   * @param params Hashtable must contain the following params:
   * <li> &lt"dsa.chromosomelength", $integer_value$&gt mandatory, the length
   * of the chromosome.
   * <li> &lt"dsa.minallelevalue", $value$&gt mandatory, the minimum value
   * for any allele in the chromosome.
   * <li> &lt"dsa.minallelevalue$i$", $value$&gt optional, the minimum value
   * for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}). If this value is less than the global value
   * specified by the "dsa.minallelevalue" key, it is ignored.
   * <li> &lt"dsa.maxallelevalue", $value$&gt mandatory, the maximum value
   * for any allele in the chromosome.
   * <li> &lt"dsa.maxallelevalue$i$", $value$&gt optional, the maximum value
   * for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}). If this value is greater than the global
   * value specified by the "dsa.maxallelevalue" key, it is ignored.
   * <li> &lt"thread.id",$integer_value"&gt mandatory, the (internal) id of the
   * thread invoking this method; this number is used so as to look-up the right
   * random-number generator associated with the current thread.
   * @throws OptimizerException
   * @return Object double[] of length specified in the params.
   */
  public Object createRandomChromosome(Hashtable params) throws OptimizerException {
    try {
      final int nmax = ( (Integer) params.get("dsa.chromosomelength")).intValue();
      final double maxalleleval=((Double) params.get("dsa.maxallelevalue")).doubleValue();
      final double minalleleval=((Double) params.get("dsa.minallelevalue")).doubleValue();
      final int id = ((Integer) params.get("thread.id")).intValue();
      double[] arr = new double[nmax];
      for (int i=0; i<nmax; i++) {
        // restore within bounds, if any
        double maxargvali = maxalleleval;
        Double MaviD = (Double) params.get("dsa.maxallelevalue"+i);
        if (MaviD!=null && MaviD.doubleValue()<maxalleleval)
          maxargvali = MaviD.doubleValue();
        double minargvali = minalleleval;
        Double maviD = (Double) params.get("dsa.minallelevalue"+i);
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

