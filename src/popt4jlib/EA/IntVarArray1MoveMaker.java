package popt4jlib.EA;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * class implements the NewChromosomeMakerIntf for chromosomes that are
 * <CODE>int[]</CODE> objects of variable length.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntVarArray1MoveMaker implements NewChromosomeMakerIntf {

  /**
   * public no-arg no-op constructor
   */
  public IntVarArray1MoveMaker() {
  }


  /**
   * create and return a <CODE>int[]</CODE> object that will represent a
   * random "move" from the current chromosome argument passed in as first
   * argument. The parameters passed in the second argument are as follows:
	 * <ul>
   * <li> &lt;"dea.movesigma", Double v&gt; optional, the &delta; value to use as &sigma; when
   * determining the distance to move in each dimension. Default is 1.0.
   * <li> &lt;"dea.movesigma$i$", Double v&gt; optional, the &delta; value to use as &sigma; when
   * determining the distance to move in the i-th dimension. Default is null.
   * <li> &lt;"dea.minallelevalue", Integer v&gt; optional, the min. value that any
   * component of the returned vector may assume.
   * <li> &lt;"dea.maxallelevalue", Integer v&gt; optional, the max. value that any
   * component of the returned vector may assume.
   * <li> &lt;"dea.minallelevalue"+$i$, Integer v&gt; optional, the min value that the
   * i-th comp. of the returned vector may assume (i={0,1,...nd.intValue()-1})
   * <li> &lt;"dea.maxallelevalue"+$i$, Integer v&gt; optional, the max value that the
   * i-th comp. of the returned vector may assume (i={0,1,...nd.intValue()-1})
	 * </ul>
   * <p>The "local" constraints can only impose more strict constraints on the
   * variables, but cannot be used to "over-ride" a global constraint to make
   * the domain of the variable wider.</p>
	 * <p>Notice that the &sigma; values are used in determining the variance of 
	 * the Gaussian distribution from which the random distance measures are
	 * computed along each dimension. Double computations are finally rounded
	 * into the nearest integer value when setting the values of the array that is
	 * returned.</p>
   * @param chromosome Object must be an int[]
   * @param params HashMap
   * @throws OptimizerException if any of the parameters are not passed in
   * properly
   * @return Object int[]
   */
  public Object createNewChromosome(Object chromosome, HashMap params) throws OptimizerException {
    try {
      int[] arg = (int[]) chromosome; // chromosome is a var int[] array
      int[] res = new int[arg.length];
      double delta = 1;
      Double dD = (Double) params.get("dea.movesigma");
      if (dD != null && dD.doubleValue() > 0) delta = dD.doubleValue();
      int maxv = ( (Integer) params.get("dea.maxallelevalue")).intValue();
      int minv = ( (Integer) params.get("dea.minallelevalue")).intValue();
      int tid = ( (Integer) params.get("thread.id")).intValue();
      for (int i = 0; i < arg.length; i++) {
        double rd = RndUtil.getInstance(tid).getRandom().nextGaussian();
        // check for specific �_i
        Double si = (Double) params.get("dea.movesigma" + i);
        double s = delta;
        if (si != null && si.doubleValue() > 0) s = si.doubleValue();
        res[i] = arg[i] + (int) Math.round(rd * s);
        // restore within bounds, if any
        int maxargvali = maxv;
        Integer MaviD = (Integer) params.get("dea.maxallelevalue" + i);
        if (MaviD != null && MaviD.intValue() < maxv)
          maxargvali = MaviD.intValue();
        int minargvali = minv;
        Integer maviD = (Integer) params.get("dea.minallelevalue" + i);
        if (maviD != null && maviD.intValue() > minv)
          minargvali = maviD.intValue();

        if (res[i] < minargvali) res[i] = minargvali;
        else if (res[i] > maxargvali) res[i] = maxargvali;
      }
      return res;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("createNewChromosome() failed");
    }
  }
}

