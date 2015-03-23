package popt4jlib.MSSC;

import popt4jlib.VectorIntf;
import popt4jlib.GradientDescent.VecUtil;
import java.util.*;

/**
 * implements the Sum-Of-Intracluster-Variances criterion for a given clustering.
 * This is not the standard Sum-of-Square-Errors criterion used in algorithms 
 * for MSSC such as K-Means (the SSE criterion is implemented in the 
 * <CODE>KMeansSqrEvaluator</CODE> class.)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class SumOfVarianceEvaluator implements EvaluatorIntf {

  /**
   * sole constructor (no-op body).
   */
  public SumOfVarianceEvaluator() {
  }


  /**
   * return the sum of the distances of each document from the cluster center
   * it belongs.
   * @param cl Clusterer
   * @return double or NaN if no assignments exist in the input argument
   * @throws ClustererException
   */
  public double eval(ClustererIntf cl) throws ClustererException {
    double ret = 0.0;
    Vector centers = cl.getCurrentCenters();
    Vector docs = cl.getCurrentVectors();
    int[] asgnms = cl.getClusteringIndices();
    if (asgnms==null) {
      // no clustering has occured yet, or failed
      return Double.NaN;
    }
    final int n = docs.size();
    final int k = centers.size();
		ArrayList[] dists = new ArrayList[k];
		for (int i=0; i<k; i++) {
			dists[i] = new ArrayList();
		}
    for (int i=0; i<n; i++) {
      VectorIntf di = (VectorIntf) docs.elementAt(i);
      VectorIntf ci = (VectorIntf) centers.elementAt(asgnms[i]);
      double dist = VecUtil.getEuclideanDistance(di,ci);
      dists[asgnms[i]].add(new Double(dist));
    }
		for (int i=0; i<k; i++) {
			double dimean = 0.0;
			ArrayList di = dists[i];
			for (int j=0; j<di.size(); j++) {
				dimean += ((Double) di.get(j)).doubleValue();
			}
			dimean /= di.size();
			double vi = 0.0;
			for (int j=0; j<di.size(); j++) {
				double dij = ((Double) di.get(j)).doubleValue();
				vi += (dij-dimean)*(dij-dimean);
			}
			vi /= ((double)di.size()-1.0);
			ret += vi;
		}
    return ret;
  }


  /**
   * evaluates the variance of the distances of each VectorIntf in the docs argument
   * from the collection's center.
   * @param docs Vector
   * @throws ClustererException
   * @return double
   */
  public static double evalCluster(Vector docs) throws ClustererException {
    double ret=0.0;
    VectorIntf center = VecUtil.getCenter(docs);
    final int docs_size = docs.size();
		double dists[] = new double[docs_size];
    double dmean = 0.0;
		for (int i=0; i<docs_size; i++) {
      VectorIntf doc_i = (VectorIntf) docs.elementAt(i);
      double dist = VecUtil.getEuclideanDistance(doc_i, center);
			dists[i] = dist;
			dmean += dist;
    }
		dmean /= (double) docs_size;
		for (int i=0; i<docs_size; i++) {
			ret += (dists[i]-dmean)*(dists[i]-dmean);
		}
		ret /= ((double)(docs_size)-1.0);
    return ret;
  }
}

