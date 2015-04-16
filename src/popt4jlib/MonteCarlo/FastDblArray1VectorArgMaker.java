/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib.MonteCarlo;

import java.util.*;
import popt4jlib.DblArray1Vector;
import popt4jlib.RandomArgMakerClonableIntf;
import popt4jlib.OptimizerException;
import utils.RndUtil;


/**
 * creates random <CODE>DblArray1Vector</CODE> objects of fixed length, 
 * according to parameters passed in a <CODE>Hashtable</CODE> object.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FastDblArray1VectorArgMaker implements RandomArgMakerClonableIntf {
	private int _arglen;
	private double _minargval;
	private List _minargvali;
	private double _maxargval;
	private List _maxargvali;
	private Random _r;
	private DblArray1Vector _x;  // the current argument cache


	/**
	 * sole public constructor.
	 */
	public FastDblArray1VectorArgMaker() {
		// no-op
	}


	/**
	 * creates a new instance that can be used without the need for any
	 * synchronization when invoked, as different threads own different objects.
	 * @param p Hashtable must contain the following params:
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
   * @throws OptimizerException if any of the params above is incorrectly set
   * @return FastDblArray1VectorArgMaker new instance according to the params passed in
   */
  public RandomArgMakerClonableIntf newInstance(Hashtable params) throws OptimizerException {
    try {
      FastDblArray1VectorArgMaker clone = new FastDblArray1VectorArgMaker();
      clone._arglen = ( (Integer) params.get("mcs.arglength")).intValue();
      clone._minargvali = new ArrayList();
      clone._maxargvali = new ArrayList();
			clone._x = new DblArray1Vector(clone._arglen);
      for (int i=0; i<clone._arglen; i++) {
        Double minvali = (Double) params.get("mcs.minargvalue"+i);
        clone._minargvali.add(minvali);
        Double maxvali = (Double) params.get("mcs.maxargvalue"+i);
        clone._maxargvali.add(maxvali);
				if (_x!=null) clone._x.setCoord(i, _x.getCoord(i));
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
   * creates fixed-length <CODE>DblArray1Vector</CODE> objects, according to its
   * data member params.
   * @param params Hashtable unused
	 * @return Object DblArray1Vector
   */
  public Object createRandomArgument(Hashtable params) throws OptimizerException {
    try {
      DblArray1Vector arr = DblArray1Vector.newInstance(_arglen);
			if (_x==null) _x = new DblArray1Vector(_arglen);
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
				double vali = minargvali + _r.nextDouble()*(maxargvali-minargvali);
        arr.setCoord(i,vali);
				_x.setCoord(i, vali);  // update cache with current arg as well
      }
      return arr;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("createRandomArgument: failed");
    }
  }
	
	
	/**
	 * returns the current argument.
	 * @return Object DlbArray1Vector
	 */
	public Object getCurrentArgument() {
		return _x;
	}

}
