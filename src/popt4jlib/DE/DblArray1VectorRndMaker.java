package popt4jlib.DE;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * the class generates random vectors in R^n as VectorIntf objects that may
 * obey certain bounding-box constraints.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1VectorRndMaker {
  private HashMap _params;

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
   * <br>The "local" constraints can only impose more strict constraints on the
   * variables, but cannot be used to "over-ride" a global constraint to make
   * the domain of the variable wider.</br>
   * @param params HashMap
   */
  public DblArray1VectorRndMaker(HashMap params) {
    _params = new HashMap(params);  // maintain a privately owned version of
                                      // passed params
  }


  /**
   * returns a VectorIntf object whose dimensionality is equal to the parameter
   * specified in the construction process by the key "dde.numdimensions". The
   * returned VectorIntf will obey the bounding-box constraints specified in the
   * construction process.
   * @throws OptimizerException
   * @return VectorIntf
   */
  public VectorIntf createNewRandomVector() throws OptimizerException {
    double mingval = Double.NEGATIVE_INFINITY;
    try {
      Double mingvD = (Double) _params.get("dde.minargval");
      if (mingvD != null) mingval = mingvD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
    double maxgval = Double.POSITIVE_INFINITY;
    try {
      Double maxgvD = (Double) _params.get("dde.maxargval");
      if (maxgvD != null) maxgval = maxgvD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
    if (maxgval < mingval)
      throw new OptimizerException("global min arg value > global max arg value");
    int n = 0;
    try {
      int tid = ((Integer) _params.get("thread.id")).intValue();
      n = ((Integer) _params.get("dde.numdimensions")).intValue();
      double[] arr = new double[n];
      for (int i=0; i<n; i++) {
        double minval = mingval;
        Double mvD = (Double) _params.get("dde.minargval"+i);
        if (mvD!=null && mvD.doubleValue()>minval) minval = mvD.doubleValue();
        double maxval = maxgval;
        Double MvD = (Double) _params.get("dde.maxargval"+i);
        if (MvD!=null && MvD.doubleValue()<maxval) maxval = MvD.doubleValue();
        if (minval>maxval)
          throw new OptimizerException("global min arg value > global max arg value");
        double factor = 100.0;
        if (maxval!=Double.POSITIVE_INFINITY && minval!=Double.NEGATIVE_INFINITY)
          factor = (maxval-minval)/10;
        arr[i] = RndUtil.getInstance(tid).getRandom().nextGaussian()*factor;
        if (arr[i] > maxval) arr[i] = maxval;
        else if (arr[i] < minval) arr[i] = minval;
        //arr[i] = minval +
        //         RndUtil.getInstance(tid).getRandom().nextDouble()*(maxval-minval);
      }
      return new DblArray1Vector(arr);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("createNewRandomVector() failed");
    }
  }
}

