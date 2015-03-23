package popt4jlib.MSSC;

import popt4jlib.*;
import popt4jlib.GradientDescent.VecUtil;
import utils.RndUtil;
import java.util.*;


/**
 * implements the KMeans++ method for seeds initialization for the K-Means
 * algorithm.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class KMeansPP {
  private List _vectors;
	private double[] _minDistCache;  // _minDistCache[i] is the current min. dist
	                                 // of _vectors[i] from the current centers


  /**
   * public constructor.
   * @param vectors List List<VectorIntf> objects.
   * @throws IllegalArgumentException if arg is empty or null.
   */
  public KMeansPP(List vectors) throws IllegalArgumentException {
    if (vectors==null || vectors.size()==0)
      throw new IllegalArgumentException("KMeansPP.<init>: vectors arg must have at least one vector");
    _vectors = vectors;
		_minDistCache = new double[_vectors.size()];
		for (int i=0; i<_vectors.size(); i++) _minDistCache[i] = Double.MAX_VALUE;  // init. cache
  }


  /**
   * the main method of the class.
   * @param k int
   * @throws IllegalArgumentException
   * @return Vector Vector<VectorIntf> 
   */
  public List getInitialCenters(int k) throws IllegalArgumentException {
    final int n = _vectors.size();
    if (k>n)
      throw new IllegalArgumentException("KMeansPP.getInitialCenters("+k+"): more centers than data points requested.");
    Vector centers = new Vector(k);  // reserve k seats in this vector
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
	 * @param centers List<VectorIntf>
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

