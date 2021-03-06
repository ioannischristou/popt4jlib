package popt4jlib.EA;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * the class creates int[] objects obeying possible bounding box constraints
 * set-forth in the parameters passed in.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntVarArray1CMaker implements RandomChromosomeMakerIntf {

  /**
   * no-arg no-op constructor.
   */
  public IntVarArray1CMaker() {
  }


  /**
   * creates and returns a <CODE>int[]</CODE> object with length up to the
   * (Integer) value mapped to the key "dea.maxchromosomelength" in the
   * <CODE>params</CODE> argument passed in. It will also obey the following
   * bounding box constraints:
	 * <ul>
   * <li> &lt;"dea.minallelevalue", Integer v&gt; optional, the min. value that any
   * component of the returned vector may assume
   * <li> &lt;"dea.maxallelevalue", Integer v&gt; optional, the max. value that any
   * component of the returned vector may assume
   * <li> &lt;"dea.minallelevalue"+$i$, Integer v&gt; optional, the min value that the
   * i-th comp. of the returned vector may assume (i={0,1,...nd.intValue()-1})
   * <li> &lt;"dea.maxallelevalue"+$i$, Integer v&gt; optional, the max value that the
   * i-th comp. of the returned vector may assume (i={0,1,...nd.intValue()-1})
   * </ul>
	 * <p>The "local" constraints can only impose more strict constraints on the
   * variables, but cannot be used to "over-ride" a global constraint to make
   * the domain of the variable wider.</p>
   * @param params HashMap see discussion above
   * @throws OptimizerException
   * @return Object int[]
   */
  public Object createRandomChromosome(HashMap params) throws OptimizerException {
    try {
      final int nmax = ( (Integer) params.get("dea.maxchromosomelength")).intValue();
      final int maxalleleval=((Integer) params.get("dea.maxallelevalue")).intValue();
      final int minalleleval=((Integer) params.get("dea.minallelevalue")).intValue();
      final int id = ((Integer) params.get("thread.id")).intValue();
      final int n = 1+RndUtil.getInstance(id).getRandom().nextInt(nmax);
      int[] arr = new int[n];
      for (int i=0; i<n; i++) {
        // ensure bounds, if any
        int maxargvali = maxalleleval;
        Integer MaviI = (Integer) params.get("dea.maxallelevalue"+i);
        if (MaviI!=null && MaviI.intValue()<maxalleleval)
          maxargvali = MaviI.intValue();
        int minargvali = minalleleval;
        Integer maviD = (Integer) params.get("dea.minallelevalue"+i);
        if (maviD!=null && maviD.intValue()>minalleleval)
          minargvali = maviD.intValue();

        arr[i] = minargvali +
            RndUtil.getInstance(id).getRandom().nextInt(maxargvali-minargvali+1);
      }
      return arr;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("createRandomChromosome: failed");
    }
  }
}

