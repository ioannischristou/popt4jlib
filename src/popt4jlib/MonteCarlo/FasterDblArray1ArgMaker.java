package popt4jlib.MonteCarlo;

import java.util.*;
import popt4jlib.RandomArgMakerClonableIntf;
import popt4jlib.OptimizerException;
import utils.RndUtil;


/**
 * creates random <CODE>double[]</CODE> objects of fixed length, according to 
 * parameters passed in a <CODE>HashMap</CODE> object. It should be faster 
 * than the <CODE>FastDblArray1ArgMaker</CODE> class, because it doesn't create 
 * new double[] each time the <CODE>createRandomArgument(p)</CODE> method is 
 * invoked, but instead re-uses a private (cache) data member. Being completely
 * unsynchronized, the methods of an object of this class must only be called
 * from the same thread.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FasterDblArray1ArgMaker implements RandomArgMakerClonableIntf {
	private int _arglen;
	private double _minargval;
	private List _minargvali;
	private double _maxargval;
	private List _maxargvali;
	private Random _r;
	private double[] _x;  // the cache (i.e. pool of 1 object)


	/**
	 * sole public constructor.
	 */
	public FasterDblArray1ArgMaker() {
		// no-op
	}


	/**
	 * creates a new instance that can be used without the need for any
	 * synchronization when invoked, as different threads own different objects.
	 * Notice that the <CODE>_x</CODE> (current-arg) data member, if not null, 
	 * is also deep-copied into the new instance that is returned.
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
   * @throws OptimizerException if incorrect arguments were passed in the 
	 * params argument
   * @return FasterDblArray1ArgMaker new instance according to the params passed in
   */
  public RandomArgMakerClonableIntf newInstance(HashMap params) throws OptimizerException {
    try {
      FasterDblArray1ArgMaker clone = new FasterDblArray1ArgMaker();
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
			clone._x = new double[clone._arglen];
			if (_x!=null) {  // do a deep-copy cloning of the cache 
				for (int i=0; i<_arglen; i++) {
					clone._x[i] = _x[i];
				}
			}
      return clone;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("incorrect args");
    }
  }


  /**
   * creates fixed-length <CODE>double[]</CODE> objects, according to its
   * data member params (not the ones passed in as argument in the call).
   * @param params HashMap unused
	 * @return Object double[]
	 * @throws OptimizerException if an error occurs computing the next random
	 * argument
   */
  public Object createRandomArgument(HashMap params) throws OptimizerException {
    try {
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
        _x[i] = minargvali + _r.nextDouble()*(maxargvali-minargvali);
      }
      return _x;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("createRandomArgument: failed");
    }
  }
	
	
	/**
	 * returns the last random <CODE>double[]</CODE> array created by a call to 
	 * the <CODE>createRandomArgument(p)</CODE> method.
	 * @return Object double[]
	 */
	public Object getCurrentArgument() {
		return _x;
	}

}
