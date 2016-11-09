package popt4jlib.MSSC;

import popt4jlib.VectorIntf;
import popt4jlib.GradientDescent.VecUtil;
import java.util.*;

/**
 * implements the classical MSSC criterion for a given clustering.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class KMeansSqrEvaluator implements EvaluatorIntf {

  /**
   * sole constructor (no-op body).
   */
  public KMeansSqrEvaluator() {
  }


  /**
   * return the sum of the distances of each document from the cluster center
   * it belongs.
   * @param cl Clusterer
   * @return double
   * @throws ClustererException
   */
  public double eval(ClustererIntf cl) throws ClustererException {
    double ret = 0.0;
    List centers = cl.getCurrentCenters();
    List docs = cl.getCurrentVectors();
    int[] asgnms = cl.getClusteringIndices();
    if (asgnms==null) {
      // no clustering has occured yet, or failed
      return Double.NaN;
    }
    final int n = docs.size();
    // final int k = centers.size();
    for (int i=0; i<n; i++) {
      VectorIntf di = (VectorIntf) docs.get(i);
      VectorIntf ci = (VectorIntf) centers.get(asgnms[i]);
      //ret += _m.dist(di, ci);
      double dist2 = VecUtil.getEuclideanDistance(di,ci);
      dist2 *= dist2;  // square it
      ret += dist2;
    }
    return ret;
  }


  /**
   * evaluates the Sum-of-Square-Errors of each VectorIntf in the docs argument
   * from the collection's center.
   * @param docs Vector
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
      double dist2 = VecUtil.getEuclideanDistance(doc_i, center);  // VecUtil.norm2(VecUtil.subtract(doc_i,center));
      dist2 *= dist2;  // square it
      ret += dist2;
    }
    return ret;
  }
}

