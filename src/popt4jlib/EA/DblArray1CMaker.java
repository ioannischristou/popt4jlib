package popt4jlib.EA;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * the class creates double[] objects obeying possible bounding box constraints
 * set-forth in the parameters passed in.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1CMaker implements RandomChromosomeMakerIntf {

  /**
   * no-arg no-op constructor.
   */
  public DblArray1CMaker() {
  }


  /**
   * creates and returns a <CODE>double[]</CODE> object with length equal to the
   * (Integer) value mapped to the key "dea.chromosomelength" in the
   * <CODE>params</CODE> argument passed in. It will and also obey the following
   * bounding box constraints:
	 * <ul>
   * <li> &lt;"dea.minallelevalue", Double v&gt; optional, the min. value that any
   * component of the returned vector may assume
   * <li> &lt;"dea.maxallelevalue", Double v&gt; optional, the max. value that any
   * component of the returned vector may assume
   * <li> &lt;"dea.minallelevalue"+$i$, Double v&gt; optional, the min. value that the
   * i-th comp. of the returned vector may assume (i={0,1,...nd.intValue()-1})
   * <li> &lt;"dea.maxallelevalue"+$i$, Double v&gt; optional, the max. value that the
   * i-th comp. of the returned vector may assume (i={0,1,...nd.intValue()-1})
	 * </ul>
   * <p>The "local" constraints can only impose more strict constraints on the
   * variables, but cannot be used to "over-ride" a global constraint to make
   * the domain of the variable wider.</p>
   * @param params HashMap
   * @throws OptimizerException if the mandatory parameters are not passed in
   * @return Object double[]
   */
  public Object createRandomChromosome(HashMap params) throws OptimizerException {
    try {
      final int nmax = ( (Integer) params.get("dea.chromosomelength")).intValue();
      final double maxalleleval=((Double) params.get("dea.maxallelevalue")).doubleValue();
      final double minalleleval=((Double) params.get("dea.minallelevalue")).doubleValue();
      final int id = ((Integer) params.get("thread.id")).intValue();
      double[] arr = new double[nmax];
      for (int i=0; i<nmax; i++) {
        // ensure bounds, if any
        double maxargvali = maxalleleval;
        Double MaviD = (Double) params.get("dea.maxallelevalue"+i);
        if (MaviD!=null && MaviD.doubleValue()<maxalleleval)
          maxargvali = MaviD.doubleValue();
        double minargvali = minalleleval;
        Double maviD = (Double) params.get("dea.minallelevalue"+i);
        if (maviD!=null && maviD.doubleValue()>minalleleval)
          minargvali = maviD.doubleValue();
        arr[i] = minargvali +
            RndUtil.getInstance(id).getRandom().nextDouble()*(maxargvali-minargvali);
      }
      return arr;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("createRandomChromosome: failed");
    }
  }
}
