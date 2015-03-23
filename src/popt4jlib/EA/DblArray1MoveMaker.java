package popt4jlib.EA;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * class implements the NewChromosomeMakerIntf for chromosomes that are
 * <CODE>double[]</CODE> objects of fixed length.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1MoveMaker implements NewChromosomeMakerIntf {

  /**
   * no-arg no-op constructor
   */
  public DblArray1MoveMaker() {
    // no-op
  }


  /**
   * create and return a <CODE>double[]</CODE> object that will represent a
   * random "move" from the current chromosome argument passed in as first
   * argument. The parameters passed in the second argument are as follows:
	 * <ul>
   * <li> &lt"dea.movesigma", Double v&gt optional, the &delta value to use as &sigma when
   * determining the distance to move in each dimension. Default is 1.0.
   * <li> &lt"dea.movesigma$i$", Double v&gt optional, the &delta value to use as &sigma when
   * determining the distance to move in the i-th dimension. Default is null.
   * <li> &lt"dea.minallelevalue", Double v&gt optional, the min. value that any
   * component of the returned vector may assume.
   * <li> &lt"dea.maxallelevalue", Double v&gt optional, the max. value that any
   * component of the returned vector may assume.
   * <li> &lt"dea.minallelevalue"+$i$, Double v&gt optional, the min. value that the
   * i-th comp. of the returned vector may assume (i={0,1,...nd.intValue()-1})
   * <li> &lt"dea.maxallelevalue"+$i$, Double v&gt optional, the max. value that the
   * i-th comp. of the returned vector may assume (i={0,1,...nd.intValue()-1})
   * </ul>
	 * <br>The "local" constraints can only impose more strict constraints on the
   * variables, but cannot be used to "over-ride" a global constraint to make
   * the domain of the variable wider.</br>
	 * <p>Notice that the &sigma values are used in determining the variance of 
	 * the Gaussian distribution from which the random distance measures are
	 * computed along each dimension.</p>
   * @param chromosome Object must be a double[].
   * @param params Hashtable see discussion above
   * @throws OptimizerException
   * @return Object a double[]
   */
  public Object createNewChromosome(Object chromosome, Hashtable params) throws OptimizerException {
    double[] arg = (double[]) chromosome;  // chromosome is a double[] array
    double[] res = new double[arg.length];
    double delta = 1;
    Double dD = (Double) params.get("dea.movesigma");
    if (dD!=null && dD.doubleValue()>0) delta = dD.doubleValue();
    double maxv = ((Double) params.get("dea.maxallelevalue")).doubleValue();
    double minv = ((Double) params.get("dea.minallelevalue")).doubleValue();
    int tid = ((Integer) params.get("thread.id")).intValue();
    for (int i=0; i<arg.length; i++) {
      double rd = RndUtil.getInstance(tid).getRandom().nextGaussian();
      // check for specific ó_i
      Double si = (Double) params.get("dea.movesigma"+i);
      double s = delta;
      if (si!=null && si.doubleValue()>0) s = si.doubleValue();
      res[i] = arg[i] + rd*s;
      // restore within bounds, if any
      double maxargvali = maxv;
      Double MaviD = (Double) params.get("dea.maxallelevalue"+i);
      if (MaviD!=null && MaviD.doubleValue()<maxv)
        maxargvali = MaviD.doubleValue();
      double minargvali = minv;
      Double maviD = (Double) params.get("dea.minallelevalue"+i);
      if (maviD!=null && maviD.doubleValue()>minv)
        minargvali = maviD.doubleValue();

      if (res[i]<minargvali) res[i]=minargvali;
      else if (res[i]>maxargvali) res[i]=maxargvali;
    }
    return res;
  }
}

