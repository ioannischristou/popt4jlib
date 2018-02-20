package popt4jlib.DE;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * the class generates uniform random vectors in R^n as VectorIntf objects that 
 * obey certain bounding-box constraints.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FastDblArray1VectorURndMaker {
	private int _arglen;
	private double _minargval;
	private List _minargvali;
	private double _maxargval;
	private List _maxargvali;
	private Random _r;
	

  /**
   * constructor.
   * The parameters that must be passed in (the HashMap arg) are as follows:
	 * <ul>
   * <li> &lt;"dde.numdimensions", Integer nd&gt; mandatory, the number of dimensions
   * <li> &lt;"dde.minargval", Double v&gt; optional, the min. value that any
   * component of the returned vector may assume
   * <li> &lt;"dde.maxargval", Double v&gt; optional, the max. value that any
   * component of the returned vector may assume
   * <li> &lt;"dde.minargval"+$i$, Double v&gt; optional, the min. value that the i-th
   * component of the returned vector may assume (i={0,1,...nd.intValue()-1})
   * <li> &lt;"dde.maxargval"+$i$, Double v&gt; optional, the max. value that the i-th
   * component of the returned vector may assume (i={0,1,...nd.intValue()-1})
   * </ul>
   * <p>The "local" constraints can only impose more strict constraints on the
   * variables, but cannot be used to "over-ride" a global constraint to make
   * the domain of the variable wider.</p>
   * @param params HashMap
   */
  public FastDblArray1VectorURndMaker(HashMap params) throws IllegalArgumentException {
    _minargval = Double.NEGATIVE_INFINITY;
    try {
      Double mingvD = (Double) params.get("dde.minargval");
      if (mingvD != null) _minargval = mingvD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
    _maxargval = Double.POSITIVE_INFINITY;
    try {
      Double maxgvD = (Double) params.get("dde.maxargval");
      if (maxgvD != null) _maxargval = maxgvD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
    if (_maxargval < _minargval)
      throw new IllegalArgumentException("global min arg value > global max arg value");
    try {
		  int tid = ((Integer) params.get("thread.id")).intValue();
      _r = RndUtil.getInstance(tid).getRandom();
			_arglen = ((Integer) params.get("dde.numdimensions")).intValue();
		}
		catch (ClassCastException e) {
			throw new IllegalArgumentException("thread.id or dde.numdimensions wrong");
		}
		_minargvali = new ArrayList();
		_maxargvali = new ArrayList();
		for (int i=0; i<_arglen; i++) {
			Double maviD = (Double) params.get("dde.minargval"+i);
			_minargvali.add(maviD);
			Double MaviD = (Double) params.get("dde.maxargval"+i);
			_maxargvali.add(MaviD);
		}
  }


  /**
   * returns a VectorIntf object whose dimensionality is equal to the parameter
   * specified in the construction process by the key "dde.numdimensions". The
   * returned VectorIntf will obey the bounding-box constraints specified in the
   * construction process.
	 * Note: it is not worth it to use pooling mechanisms in this class, as this
	 * class and this method in particular is only used in the initialization 
	 * phase of the DE algorithm (population initialization).
   * @throws OptimizerException
   * @return VectorIntf
   */
  public VectorIntf createNewRandomVector() throws OptimizerException {
    int n = _arglen;
    try {
      double[] arr = new double[n];
      for (int i=0; i<n; i++) {
        double minval = _minargval;
        Double mvD = (Double) _minargvali.get(i);
        if (mvD!=null && mvD.doubleValue()>minval) minval = mvD.doubleValue();
        double maxval = _maxargval;
        Double MvD = (Double) _maxargvali.get(i);
        if (MvD!=null && MvD.doubleValue()<maxval) maxval = MvD.doubleValue();
        if (minval>maxval)
          throw new OptimizerException("global min arg value > global max arg value");
        arr[i] = minval + _r.nextDouble()*(maxval-minval);
      }
      return new DblArray1Vector(arr);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("createNewRandomVector() failed");
    }
  }

	
  /**
   * returns a VectorIntf object whose dimensionality is equal to the parameter
   * specified in the construction process by the key "dde.numdimensions". The
   * returned VectorIntf will obey the bounding-box constraints specified in the
   * construction process.
	 * Note: it is not worth it to use pooling mechanisms in this class, as this
	 * class and this method in particular is only used in the initialization 
	 * phase of the DE algorithm (population initialization).
	 * @param r Random the random number generator to use to generate the new
	 * random vector
   * @throws OptimizerException
   * @return VectorIntf
   */
  public VectorIntf createNewRandomVector(Random r) throws OptimizerException {
    int n = _arglen;
    try {
      double[] arr = new double[n];
      for (int i=0; i<n; i++) {
        double minval = _minargval;
        Double mvD = (Double) _minargvali.get(i);
        if (mvD!=null && mvD.doubleValue()>minval) minval = mvD.doubleValue();
        double maxval = _maxargval;
        Double MvD = (Double) _maxargvali.get(i);
        if (MvD!=null && MvD.doubleValue()<maxval) maxval = MvD.doubleValue();
        if (minval>maxval)
          throw new OptimizerException("global min arg value > global max arg value");
        arr[i] = minval + r.nextDouble()*(maxval-minval);
      }
      return new DblArray1Vector(arr);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("createNewRandomVector() failed");
    }
  }

}

