/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib.MonteCarlo;

import java.util.*;
import popt4jlib.RandomArgMakerClonableIntf;
import popt4jlib.OptimizerException;
import utils.RndUtil;


/**
 *
 * @author itc
 */
public class FastDblArray1ArgMaker implements RandomArgMakerClonableIntf {
	private int _arglen;
	private double _minargval;
	private List _minargvali;
	private double _maxargval;
	private List _maxargvali;
	private Random _r;


	public FastDblArray1ArgMaker() {
		// no-op
	}


	/**
	 * creates a new instance that can be used without the need for any
	 * synchronization when invoked, as different threads own different objects.
	 * @param p Hashtable must contain the following params:
   * <li> &lt"mcs.arglength", $integer_value$&gt mandatory, the length
   * of the argument.
   * <li> &lt"mcs.minargvalue", $value$&gt mandatory, the minimum value for
   * any arg. element.
   * <li> &lt"mcs.minargvalue$i$", $value$&gt optional, the minimum value for
   * the i-th element in the argument ($i$ must be in the range
   * {0,...,arg_length-1}. If this value is less than the global value
   * specified by the "mcs.minargvalue" key, it is ignored.
   * <li> &lt"mcs.maxargvalue", $value$&gt mandatory, the maximum value for
   * any element in the argument.
   * <li> &lt"mcs.maxargvalue$i$", $value$&gt optional, the maximum value for
   * the i-th element in the argument ($i$ must be in the range
   * {0,...,arg_length-1}. If this value is greater than the global value
   * specified by the "mcs.maxargvalue" key, it is ignored.
   * <li> &lt"thread.id",$integer_value"&gt mandatory, the (internal) id of the
   * thread invoking this method; this number is used so as to look-up the right
   * random-number generator associated with the current thread.
   * @throws OptimizerException
   * @return FastDblArray1ArgMaker new instance according to the params passed in
   */
  public RandomArgMakerClonableIntf newInstance(Hashtable params) throws OptimizerException {
    try {
      FastDblArray1ArgMaker clone = new FastDblArray1ArgMaker();
      clone._arglen = ( (Integer) params.get("mcs.arglength")).intValue();
      clone._minargvali = new ArrayList();
      clone._maxargvali = new ArrayList();
      for (int i=0; i<clone._arglen; i++) {
        Double minvali = (Double) params.get("mcs.minargvalue"+i);
        clone._minargvali.add(minvali);
        Double maxvali = (Double) params.get("mcs.maxargvalue"+i);
        clone._maxargvali.add(maxvali);
      }
      clone._maxargval = ((Double) params.get("mcs.maxargvalue")).doubleValue();
      clone._minargval = ((Double) params.get("mcs.minargvalue")).doubleValue();
      final int id = ((Integer) params.get("thread.id")).intValue();
      clone._r = RndUtil.getInstance(id).getRandom();
      return clone;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("incorrect args");
    }
  }


  /**
   * creates fixed-length <CODE>double[]</CODE> objects, according to its
   * data member params.
   * @param params Hashtable unused
	 * @return Object double[].
   */
  public Object createRandomArgument(Hashtable params) throws OptimizerException {
    try {
      double[] arr = new double[_arglen];
      for (int i=0; i<_arglen; i++) {
        // restore within bounds, if any
        double maxargvali = _maxargval;
        Double MaviD = (Double) _maxargvali.get(i);
        if (MaviD!=null && MaviD.doubleValue()<_maxargval)
          maxargvali = MaviD.doubleValue();
        double minargvali = _minargval;
        Double maviD = (Double) _minargvali.get(i);
        if (maviD!=null && maviD.doubleValue()>_minargval)
          minargvali = maviD.doubleValue();
        arr[i] = minargvali + _r.nextDouble()*(maxargvali-minargvali);
      }
      return arr;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("createRandomArgument: failed");
    }
  }

}
