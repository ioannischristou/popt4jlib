package popt4jlib.MonteCarlo;

import java.util.*;
import popt4jlib.RandomArgMakerClonableIntf;
import popt4jlib.OptimizerException;
import utils.RndUtil;


/**
 * creates random <CODE>double[]</CODE> objects of fixed length, according to 
 * parameters passed in a <CODE>HashMap</CODE> object. Being completely
 * unsynchronized, this class's design relies on the fact that no two threads
 * will ever call the same object's methods.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FastDblArray1ArgMaker implements RandomArgMakerClonableIntf {
	private int _arglen;
	private double _minargval;
	private List _minargvali;
	private double _maxargval;
	private List _maxargvali;
	private Random _r;
	private double[] _lastArg;


	/**
	 * sole public constructor.
	 */
	public FastDblArray1ArgMaker() {
		// no-op
	}


	/**
	 * creates a new instance that can be used without the need for any
	 * synchronization when invoked, as different threads own different objects.
	 * Notice that the <CODE>_lastArg</CODE> data member, if not null, is also 
	 * deep-copied into the new instance that is returned.
	 * @param params HashMap must contain the following params:
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
   * @return FastDblArray1ArgMaker new instance according to the params passed in
   */
  public RandomArgMakerClonableIntf newInstance(HashMap params) throws OptimizerException {
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
			clone._lastArg = new double[clone._arglen];
			if (_lastArg!=null) {
				for (int i=0; i<_arglen; i++) clone._lastArg[i] = _lastArg[i];
			}
      return clone;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("incorrect args");
    }
  }


  /**
   * creates fixed-length <CODE>double[]</CODE> objects, according to the params
	 * passed in the invocation <CODE>newInstance(params);</CODE> that created
	 * this object. As a side-effect, the created double[] random vector is also
	 * stored in the <CODE>_lastArg</CODE> data member, so it can be retrieved by
	 * a call to <CODE>getCurrentArgument()</CODE>. 
   * @param params HashMap unused
	 * @return Object double[].
   */
  public Object createRandomArgument(HashMap params) throws OptimizerException {
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
			_lastArg = arr;  // keep last arg created
      return arr;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("createRandomArgument: failed");
    }
  }
	
	
	/**
	 * returns the argument held in the <CODE>_lastArg</CODE> data member, where
	 * the last argument created by <CODE>createRandomArgument(unused)</CODE> is
	 * stored.
	 * @return Object double[] 
	 */
	public Object getCurrentArgument() {
		return _lastArg;
	}

}

