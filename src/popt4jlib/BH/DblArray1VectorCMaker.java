package popt4jlib.BH;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * creates random DblArray1Vector objects of fixed length, according to params
 * passed in a <CODE>HashMap</CODE> object for the Basin Hopping algorithm.
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
   * @param params HashMap must contain the following params:
	 * <ul>
   * <li> &lt;"[dgabh.]chromosomelength", $integer_value$&gt; mandatory, the 
	 * length of the chromosome.
   * <li> &lt;"[dgabh.]minallelevalue", $value$&gt; mandatory, the min value for
   * any allele in the chromosome.
   * <li> &lt;"[dgabh.]minallelevalue$i$", $value$&gt; optional, the min value 
	 * for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}). If this value is less than the global value
   * specified by the "[dgabh.]minallelevalue" key, it is ignored.
   * <li> &lt;"[dgabh.]maxallelevalue", $value$&gt; mandatory, the maximum value 
	 * for any allele in the chromosome.
   * <li> &lt;"[dgabh.]maxallelevalue$i$", $value$&gt; optional, the max value 
	 * for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}). If this is greater than the global value
   * specified by the "[dgabh.]maxallelevalue" key, it is ignored.
   * <li> &lt;"thread.id",$integer_value"&gt; mandatory, the (internal) id of 
	 * the thread invoking this method; this number is used so as to look-up the 
	 * right random-number generator associated with the current thread.
	 * </ul>
   * @throws OptimizerException if any of the params above are incorrectly set
   * @return Object // DblArray1Vector of length specified in the params.
   */
  public Object createRandomChromosome(HashMap params) 
		throws OptimizerException {
    try {
      final int id = ((Integer)params.get("thread.id")).intValue();
			LightweightParams p = new LightweightParams(params);
      final int nmax = p.getInteger("dgabh.chromosomelength").intValue();
      final double maxalleleval = 
				p.getDouble("dgabh.maxallelevalue").doubleValue();
      final double minalleleval = 
				p.getDouble("dgabh.minallelevalue").doubleValue();
      DblArray1Vector vec = DblArray1Vector.newInstance(nmax);
      for (int i=0; i<nmax; i++) {
        // restore within bounds, if any
        double maxargvali = maxalleleval;
        Double MaviD = p.getDouble("dgabh.maxallelevalue"+i);
        if (MaviD!=null && MaviD.doubleValue()<maxalleleval)
          maxargvali = MaviD.doubleValue();
        double minargvali = minalleleval;
        Double maviD = p.getDouble("dgabh.minallelevalue"+i);
        if (maviD!=null && maviD.doubleValue()>minalleleval)
          minargvali = maviD.doubleValue();
        double vali = minargvali +
                        RndUtil.getInstance(id).getRandom().nextDouble()*
					              (maxargvali-minargvali);
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
