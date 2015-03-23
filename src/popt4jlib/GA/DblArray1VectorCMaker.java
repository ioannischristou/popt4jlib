package popt4jlib.GA;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * creates random DblArray1Vector objects of fixed length, according to params
 * passed in a <CODE>Hashtable</CODE> object.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1VectorCMaker implements RandomChromosomeMakerIntf {

  /**
   * sole public constructor (no-op body)
   */
  public DblArray1VectorCMaker() {
  }


  /**
   * creates fixed-length <CODE>DblArray1Vector</CODE> objects, based to certain
   * parameters, specified in the params argument. The value for each element
   * in the array, is drawn from the uniform distribution restricted within the
   * boundaries of the element's range specified in the params key-value pairs.
   * @param params Hashtable must contain the following params:
	 * <ul>
   * <li> &lt"dga.chromosomelength", $integer_value$&gt mandatory, the length
   * of the chromosome.
   * <li> &lt"dga.minallelevalue", $value$&gt mandatory, the minimum value for
   * any allele in the chromosome.
   * <li> &lt"dga.minallelevalue$i$", $value$&gt optional, the minimum value for
   * the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}). If this value is less than the global value
   * specified by the "dga.minallelevalue" key, it is ignored.
   * <li> &lt"dga.maxallelevalue", $value$&gt mandatory, the maximum value for
   * any allele in the chromosome.
   * <li> &lt"dga.maxallelevalue$i$", $value$&gt optional, the maximum value for
   * the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}). If this value is greater than the global value
   * specified by the "dga.maxallelevalue" key, it is ignored.
   * <li> &lt"thread.id",$integer_value"&gt mandatory, the (internal) id of the
   * thread invoking this method; this number is used so as to look-up the right
   * random-number generator associated with the current thread.
	 * </ul>
   * @throws OptimizerException if any of the params above are incorrectly set
   * @return Object DblArray1Vector of length specified in the params.
   */
  public Object createRandomChromosome(Hashtable params) throws OptimizerException {
    try {
      final int nmax = ( (Integer) params.get("dga.chromosomelength")).intValue();
      final double maxalleleval=((Double) params.get("dga.maxallelevalue")).doubleValue();
      final double minalleleval=((Double) params.get("dga.minallelevalue")).doubleValue();
      final int id = ((Integer) params.get("thread.id")).intValue();
      DblArray1Vector vec = DblArray1Vector.newInstance(nmax);
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
        double vali = minargvali +
            RndUtil.getInstance(id).getRandom().nextDouble()*(maxargvali-minargvali);
				vec.setCoord(i, vali);
      }
      return vec;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("createRandomChromosome: failed");
    }
  }
}
