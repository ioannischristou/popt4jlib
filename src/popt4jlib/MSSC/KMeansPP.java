package popt4jlib.MSSC;

import popt4jlib.*;
import popt4jlib.GradientDescent.VecUtil;
import utils.RndUtil;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;


/**
 * implements the KMeans++ method for seeds initialization for the K-Means
 * algorithm. This implementation can be used to take into accounts any weights 
 * associated with the data points in the dataset, by calling the 
 * <CODE>getInitialCenters(k, weights)</CODE> method. This method was initially
 * developed to work as the "reclustering" phase of the K-Means|| algorithm.
 * <p>Notes:
 * <ul>
 * <li>2018-11-13: added getInitialCenters(k,weights) method to support KMeans||
 * final reclustering.
 * <li>2018-11-01: modified the run-time type of getInitialCenters(k) method to
 * return an ArrayList so as to avoid Vector whose methods are synchronized. 
 * Also, now implements the ClustererInitIntf, which is also implemented by
 * class KMeansCC in this package -implementing the K-Means|| algorithm.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2024</p>
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
      throw new IllegalArgumentException("KMeansPP.<init>: vectors arg "+
				                                 "must have at least one vector");
    _vectors = vectors;
		_minDistCache = new double[_vectors.size()];
		for (int i=0; i<_vectors.size(); i++) 
			_minDistCache[i] = Double.MAX_VALUE;  // init. cache
  }

	
	/**
	 * the main method of the class calls <CODE>getInitialCenters(k,null)</CODE>.
	 * @param k int
	 * @return List  // List&lt;VectorIntf&gt;
	 */
	public List getInitialCenters(int k) {
		return getInitialCenters(k,null);
	}
	

  /**
   * the main implementation method of the class.
   * @param k int
	 * @param weights double[] optional, may be null; if not, then data are not
	 * sampled uniformly but according to their weights, that must be positive.
   * @throws IllegalArgumentException if the centers k, is higher than the 
	 * total number of points to be clustered, or if weights is not null but its
	 * length doesn't match the <CODE>_vectors</CODE> data. Unchecked.
   * @return List // List&lt;VectorIntf&gt;
   */
  public List getInitialCenters(int k, double[] weights) {
    final int n = _vectors.size();
    if (k>n)
      throw new IllegalArgumentException("KMeansPP.getInitialCenters("+k+",wts"+
				                                 "): more centers than data points "+
				                                 "requested.");
		if (weights!=null && weights.length!=n) 
			throw new IllegalArgumentException("KMeansPP.getInitialCenters(k,w): "+
				                                 "w.length doesn't match _vectors");
		//ok, go to work
    List centers = new ArrayList(k);  // reserve k seats in this list
		Random rr = RndUtil.getInstance().getRandom();
    // 1. choose uniformly at random the first data-point
    int n0 = -1;
		double w_tot = 1.0;
		if (weights==null) n0 = rr.nextInt(n);
		else {
			w_tot = 0.0;
			for (int i=0; i<n; i++) {
				w_tot += weights[i];
			}
			double rv = rr.nextDouble();
			double c_s = 0.0;
			for (int i=0; i<n; i++) {
				c_s += weights[i]/w_tot;
				if (c_s >= rv) {
					n0 = i;
					break;
				}
			}
		}
    centers.add(((VectorIntf) _vectors.get(n0)).newInstance());
    // 2. choose a center x from the remaining data points according to a
    // probability distribution that's proportional to w(x)*D(x)^2 (the further,
    // the better) until we have k centers.
    double[] dists = new double[n];
    for (int i=2; i<=k; i++) {
			// update _minDistCache
			VectorIntf lc = (VectorIntf) centers.get(centers.size()-1);
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
				if (weights!=null) dists[j] *= weights[j];
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
      centers.add(((VectorIntf) _vectors.get(c)).newInstance());
    }
    return centers;
  }

}

