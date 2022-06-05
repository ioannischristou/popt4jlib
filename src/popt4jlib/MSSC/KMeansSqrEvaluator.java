package popt4jlib.MSSC;

import popt4jlib.VectorIntf;
import popt4jlib.GradientDescent.VecUtil;
import java.util.*;


/**
 * implements the classical MSSC criterion for a given clustering.
 * <p>Notice that this implementation works with weighted clustering as well.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2022</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.1
 */
public class KMeansSqrEvaluator implements EvaluatorIntf {

  /**
   * sole constructor (no-op body).
   */
  public KMeansSqrEvaluator() {
  }


  /**
   * return the sum of the squared distances of each document from the cluster 
	 * center it belongs. In case the data points have associated weights, each
	 * distance from the center is multiplied by the associated data weight (note
	 * that weights must be positive). If weights are present, the clusterer must
	 * have in its parameters an entry either for the key "gmeansmt.weights" or
	 * else an entry for the simpler key "weights", in which case, the weights
	 * are not actually taken into account when executing the two K-Means steps.
   * @param cl Clusterer
   * @return double
   * @throws ClustererException
   */
  public double eval(ClustererIntf cl) throws ClustererException {
    double ret = 0.0;
    List docs = cl.getCurrentVectors();
    int[] asgnms = cl.getClusteringIndices();
    if (asgnms==null) {
      // no clustering has occured yet, or failed
			System.err.println("KMeansSqrEvaluator: clusterer returns null asgns...");
      return Double.NaN;
    }
    List centers = cl.getCurrentCenters();
		final int k = centers.size();
		final HashMap params = cl.getParams();
		double[] weights = (double[]) params.get("gmeansmt.weights");
		// nope, check out the simpler "weights" key
		if (weights==null)	{
			weights = (double[]) params.get("weights");
			if (weights!=null) 
				centers = GMeansMTClusterer.getCenters(docs, weights, asgnms, k);
		}
    final int n = docs.size();
    // final int k = centers.size();
		/*
		double[] sumw = new double[centers.size()];  // init. to zeros
		if (weights!=null) {
			for (int i=0; i<n; i++) sumw[asgnms[i]] += weights[i];
		}
		*/
    for (int i=0; i<n; i++) {
      VectorIntf di = (VectorIntf) docs.get(i);
      VectorIntf ci = (VectorIntf) centers.get(asgnms[i]);
      //ret += _m.dist(di, ci);
      double dist2 = VecUtil.getEuclideanDistance(di,ci);
      dist2 *= dist2;  // square it
			if (weights!=null) dist2 *= weights[i];  // used to multiply by 
			                                         // weights[i]/sumw[asgnms[i]];
      ret += dist2;
    }
    return ret;
  }


  /**
   * evaluates the Sum-of-Square-Errors of each VectorIntf in the docs argument
   * from the collection's center. Note that this implementation does not take
	 * into account any weights the data points might have.
   * @param docs Vector  // Vector&lt;VectorIntf&gt;
   * @throws ClustererException
   * @return double
   */
  public static double evalCluster(Vector docs) throws ClustererException {
    double ret=0.0;
    VectorIntf center = VecUtil.getCenter(docs);
    final int docs_size = docs.size();
    for (int i=0; i<docs_size; i++) {
      VectorIntf doc_i = (VectorIntf) docs.elementAt(i);
      //ret += _m.dist(doc_i, center);
      //ret += VecUtil.norm2(VecUtil.subtract(doc_i,center));
      double dist2 = VecUtil.getEuclideanDistance(doc_i, center);  
      // used to be: VecUtil.norm2(VecUtil.subtract(doc_i,center));
      dist2 *= dist2;  // square it
      ret += dist2;
    }
    return ret;
  }
}

