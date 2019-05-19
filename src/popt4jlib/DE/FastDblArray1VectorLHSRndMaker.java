package popt4jlib.DE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import popt4jlib.DblArray1Vector;
import popt4jlib.OptimizerException;
import popt4jlib.VectorIntf;
import utils.Pair;
import utils.RndUtil;

/**
 * the class generates random vectors in R^n as VectorIntf objects that 
 * obey certain bounding-box constraints, utilizing the Latin Hypercube Sampling
 * technique (LHS).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2019</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FastDblArray1VectorLHSRndMaker implements VectorIntfRndMakerIntf {
	private int _arglen;
	private int _M;
	private double _minargval;
	private List _minargvali;
	private double _maxargval;
	private List _maxargvali;
	private Random _r;

	private List _intervals; // List<Pair<Double left, Double right>[]> 
	private int[] _order;
	
	volatile static int _cnt = 0;
	
	
  /**
   * constructor.
   * The parameters that must be passed in (the HashMap arg) are as follows:
	 * <ul>
   * <li> &lt;"dde.numdimensions", Integer nd&gt; mandatory, num. of dimensions
	 * <li> &lt;"dde.popsize", Integer m&gt; mandatory, number of intervals to
	 * partition each dimension's domain
   * <li> &lt;"dde.minargval", Double v&gt; optional, the min. value that any
   * component of the returned vector may assume
   * <li> &lt;"dde.maxargval", Double v&gt; optional, the max. value that any
   * component of the returned vector may assume
   * <li> &lt;"dde.minargval"+$i$, Double v&gt; optional, the min. value that 
	 * the i-th component of the returned vector may assume 
	 * (i={0,1,...nd.intValue()-1})
   * <li> &lt;"dde.maxargval"+$i$, Double v&gt; optional, the max. value that 
	 * the i-th component of the returned vector may assume 
	 * (i={0,1,...nd.intValue()-1})
   * </ul>
   * <p>The "local" constraints can only impose more strict constraints on the
   * variables, but cannot be used to "over-ride" a global constraint to make
   * the domain of the variable wider.</p>
   * @param params HashMap
   */
  public FastDblArray1VectorLHSRndMaker(HashMap params) {
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
      throw new IllegalArgumentException(
				          "global min arg value > global max arg value");
    try {
		  int tid = ((Integer) params.get("thread.id")).intValue();
      _r = RndUtil.getInstance(tid).getRandom();
			_arglen = ((Integer) params.get("dde.numdimensions")).intValue();
			_M = ((Integer) params.get("dde.popsize")).intValue();
		}
		catch (ClassCastException e) {
			throw new IllegalArgumentException(
				          "thread.id or dde.numdimensions or dde.numpoints wrong");
		}
		_minargvali = new ArrayList();
		_maxargvali = new ArrayList();
		for (int i=0; i<_arglen; i++) {
			Double maviD = (Double) params.get("dde.minargval"+i);
			if (maviD==null || Double.compare(maviD.doubleValue(),_minargval)<0)
				_minargvali.add(new Double(_minargval));
			else _minargvali.add(maviD);
			Double MaviD = (Double) params.get("dde.maxargval"+i);
			if (MaviD==null || Double.compare(MaviD.doubleValue(),_maxargval)>0)
				_maxargvali.add(new Double(_maxargval));
			else _maxargvali.add(MaviD);
		}
		
		// specify the M intervals in each dimension, and also the way each of the 
		// M points is to be constructed.
		_intervals = new ArrayList(_arglen);
		for (int i=0; i<_arglen; i++) {
			Pair[] intvlsi = new Pair[_M];
			double l = ((Double)_minargvali.get(i)).doubleValue();
			double len = (((Double)_maxargvali.get(i)).doubleValue() - l)/_M;
			for (int j=0; j<_M; j++) {
				Double left = new Double(l);
				Double right = l+len;
				intvlsi[j] = new Pair(left, right);
				l = right.doubleValue();
			}
			_intervals.add(intvlsi);
		}
		_order = new int[_M];
		for (int i=0; i<_M; i++) _order[i] = i;
  }
	
	
  /**
   * returns a VectorIntf object whose dimensionality is equal to the parameter
   * specified in the construction process by the key "dde.numdimensions". The
   * returned VectorIntf will obey the bounding-box constraints specified in the
   * construction process, and will be constructed according to the Latin 
	 * Hypercube Sampling technique.
	 * Note: it is not worth it to use pooling mechanisms in this class, as this
	 * class and this method in particular is only used in the initialization 
	 * phase of the DE algorithm (population initialization).
	 * @param r Random
   * @throws OptimizerException
   * @return VectorIntf
   */
  public VectorIntf createNewRandomVector(Random r) throws OptimizerException {
    int n = _arglen;
    try {
      double[] arr = new double[n];
      for (int d=0; d<n; d++) {
				int j = _order[(_cnt+d) % _M];
				double minval = 
					((Double) ((Pair[])_intervals.get(d))[j].getFirst()).doubleValue();
				double maxval = 
					((Double) ((Pair[])_intervals.get(d))[j].getSecond()).doubleValue();
        arr[d] = minval + r.nextDouble()*(maxval-minval);
      }
			synchronized (FastDblArray1VectorLHSRndMaker.class) {
				++_cnt;
			}
      return new DblArray1Vector(arr);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("createNewRandomVector() failed");
    }
  }

	
  /**
	 * calls the <CODE>createNewRandomVector(r)</CODE> method, with argument the
	 * member <CODE>_r</CODE>.
   * @throws OptimizerException
   * @return VectorIntf
   */
  public VectorIntf createNewRandomVector() throws OptimizerException {
		return createNewRandomVector(_r);
	}
	
}

