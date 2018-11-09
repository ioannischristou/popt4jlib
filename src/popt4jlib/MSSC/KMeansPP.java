package popt4jlib.MSSC;

import popt4jlib.*;
import popt4jlib.GradientDescent.VecUtil;
import utils.RndUtil;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;


/**
 * implements the KMeans++ method for seeds initialization for the K-Means
 * algorithm.
 * <p>Notes:
 * <ul>
 * <li>2018-11-01: modified the run-time type of getInitialCenters(k) method to
 * return an ArrayList so as to avoid Vector whose methods are synchronized. 
 * Also, now implements the ClustererInitIntf, which is also implemented by
 * class KMeansCC in this package -implementing the K-Means|| algorithm.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.1
 */
public class KMeansPP implements ClustererInitIntf {
  private List _vectors;
	private double[] _minDistCache;  // _minDistCache[i] is the current min. dist
	                                 // of _vectors[i] from the current centers


  /**
   * public constructor.
   * @param vectors List // List&lt;VectorIntf&gt; objects.
   * @throws IllegalArgumentException if arg is empty or null.
   */
  public KMeansPP(List vectors) {
    if (vectors==null || vectors.size()==0)
      throw new IllegalArgumentException("KMeansPP.<init>: vectors arg must have at least one vector");
    _vectors = vectors;
		_minDistCache = new double[_vectors.size()];
		for (int i=0; i<_vectors.size(); i++) _minDistCache[i] = Double.MAX_VALUE;  // init. cache
  }


  /**
   * the main method of the class.
   * @param k int
   * @throws IllegalArgumentException if the centers k, is higher than the 
	 * total number of points to be clustered
   * @return List // List&lt;VectorIntf&gt;
   */
  public List getInitialCenters(int k) {
    final int n = _vectors.size();
    if (k>n)
      throw new IllegalArgumentException("KMeansPP.getInitialCenters("+k+"): more centers than data points requested.");
    List centers = new ArrayList(k);  // reserve k seats in this list
		Random rr = RndUtil.getInstance().getRandom();
    // 1. choose uniformly at random the first data-point
    int n0 = rr.nextInt(n);
    centers.add(((VectorIntf) _vectors.get(n0)).newInstance());  // used to be newCopy();
    // 2. choose a center x from the remaining data points according to a
    // probability distribution that is proportional to D(x)^2 (the further,
    // the better) until we have k centers.
    double[] dists = new double[n];
    for (int i=2; i<=k; i++) {
			// update _minDistCache
			VectorIntf lc = (VectorIntf) centers.get(centers.size()-1);
			double mind = Double.MAX_VALUE;
			for (int j=0; j<n; j++) {
				VectorIntf xj = (VectorIntf) _vectors.get(j);
				double djc = VecUtil.getEuclideanDistance(xj, lc);
				if (djc < _minDistCache[j]) _minDistCache[j] = djc;
			}
      // compute D(x) for each x
      double total = 0.0;
      for (int j=0; j<n; j++) {
        // VectorIntf xj = (VectorIntf) _vectors.get(j);
        // _dists[j] = getMinDist(xj, centers);
				dists[j] = _minDistCache[j];
        total += dists[j]*dists[j];
      }
      // draw a point x at random with D(x)^2 probability
      double[] pie = new double[n];
      pie[0]=dists[0]*dists[0]/total;
      for (int j=1; j<n; j++) pie[j] = pie[j-1]+dists[j]*dists[j]/total;
      double r = rr.nextDouble();
      int c = 0;
      for (int j=0; j<n; j++) {
        if (pie[j]>r && dists[j]>1.e-12) {  // comparison const needs revision
          c = j;
          break;
        }
      }
      centers.add(((VectorIntf) _vectors.get(c)).newInstance());  // used to be newCopy();
    }
    return centers;
  }


	/**
	 * Not used any more.
	 * @param x VectorIntf
	 * @param centers List&lt;VectorIntf&gt;
	 * @return double
	 */
  private double getMinDist(VectorIntf x, List centers) {
    int k = centers.size();
    double mind = Double.MAX_VALUE;
    for (int i=0; i<k; i++) {
      double di = VecUtil.getEuclideanDistance(x, (VectorIntf) centers.get(i));
      if (di<mind) mind = di;
    }
    return mind;
  }
}

