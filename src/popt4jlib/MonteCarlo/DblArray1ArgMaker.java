package popt4jlib.MonteCarlo;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * creates random double[] objects of fixed length, according to parameters
 * passed in a <CODE>Hashtable</CODE> object.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1ArgMaker implements RandomArgMakerIntf {

  /**
   * public sole constructor (no-op body).
   */
  public DblArray1ArgMaker() {
  }


  /**
   * creates fixed-length <CODE>double[]</CODE> objects, according to certain
   * parameters, specified in the params argument. The value for each element
   * in the array, is drawn from the uniform distribution restricted within the
   * boundaries of the element's range specified in the params key-value pairs.
   * @param params Hashtable must contain the following params:
	 * <ul>
   * <li> &lt;"mcs.arglength", $integer_value$&gt; mandatory, the length
   * of the argument.
   * <li> &lt;"mcs.minargvalue", $value$&gt; mandatory, the minimum value for
   * any arg. element.
   * <li> &lt;"mcs.minargvalue$i$", $value$&gt; optional, the minimum value for
   * the i-th element in the argument ($i$ must be in the range
   * {0,...,arg_length-1}. If this value is less than the global value
   * specified by the "mcs.minargvalue" key, it is ignored.
   * <li> &lt;"mcs.maxargvalue", $value$&gt; mandatory, the maximum value for
   * any element in the argument.
   * <li> &lt;"mcs.maxargvalue$i$", $value$&gt; optional, the maximum value for
   * the i-th element in the argument ($i$ must be in the range
   * {0,...,arg_length-1}. If this value is greater than the global value
   * specified by the "mcs.maxargvalue" key, it is ignored.
   * <li> &lt;"thread.id",$integer_value"&gt; mandatory, the (internal) id of the
   * thread invoking this method; this number is used so as to look-up the right
   * random-number generator associated with the current thread.
	 * </ul>
   * @throws OptimizerException if any of the params above are incorrectly set
   * @return Object double[] of length specified in the params
   */
  public Object createRandomArgument(Hashtable params) throws OptimizerException {
    try {
      final int nmax = ( (Integer) params.get("mcs.arglength")).intValue();
      final double maxalleleval=((Double) params.get("mcs.maxargvalue")).doubleValue();
      final double minalleleval=((Double) params.get("mcs.minargvalue")).doubleValue();
      final int id = ((Integer) params.get("thread.id")).intValue();
      double[] arr = new double[nmax];
      for (int i=0; i<nmax; i++) {
        // restore within bounds, if any
        double maxargvali = maxalleleval;
        Double MaviD = (Double) params.get("mcs.maxargvalue"+i);
        if (MaviD!=null && MaviD.doubleValue()<maxalleleval)
          maxargvali = MaviD.doubleValue();
        double minargvali = minalleleval;
        Double maviD = (Double) params.get("mcs.minargvalue"+i);
        if (maviD!=null && maviD.doubleValue()>minalleleval)
          minargvali = maviD.doubleValue();

        arr[i] = minargvali +
            RndUtil.getInstance(id).getRandom().nextDouble()*(maxargvali-minargvali);
      }
      return arr;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("createRandomArgument: failed");
    }
  }
}
