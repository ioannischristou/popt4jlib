package popt4jlib.MSSC;

import popt4jlib.VectorIntf;
import java.util.*;


/**
 * implements the classical MSSC criterion for a given clustering BUT with 
 * normalized attribute values (dimensions) in [0,1], as the default in WEKA
 * SimpleKMeansClusterer. Mostly used for comparison purposes, because running
 * the WEKA clusterer can be very slow for large K values.
 * <p>Notice that this implementation does NOT work with weighted clustering
 * (which is when points in the dataset have positive weights associated).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class KMeansNormSqrEvaluator implements EvaluatorIntf {
	private double[] _minAttrVal;  // double[m] where m=dim(v) for each v in docs
	private double[] _maxAttrVal;  // same as above
	private int _maxDimDev;  // should be zero
	private int _maxDim = 0;
	private int _minDim = Integer.MAX_VALUE;
	
  /**
   * sole constructor computes the min and max of each dimension, as well as
	 * the max deviation in number of dimensions among the vectors.
	 * @param docs List  // List&lt;VectorIntf&gt;
	 * @throws IllegalArgumentException if docs is null or empty or if the 
	 * vectors in docs don't all have the same number of dimensions
   */
  public KMeansNormSqrEvaluator(List docs) {
		if (docs==null || docs.size()==0) 
			throw new IllegalArgumentException("null or empty input parameter");
		_minAttrVal = new double[((VectorIntf)docs.get(0)).getNumCoords()];
		_maxAttrVal = new double[_minAttrVal.length];
		final int dsz = docs.size();
		final int dim = _minAttrVal.length;
		for (int m=0; m<dim; m++) {
			_minAttrVal[m] = Double.POSITIVE_INFINITY;
			_maxAttrVal[m] = Double.NEGATIVE_INFINITY;
			for (int i=0; i<dsz; i++) {
				VectorIntf vi = (VectorIntf) docs.get(i);
				if (_maxDim < vi.getNumCoords()) _maxDim = vi.getNumCoords();
				if (_minDim > vi.getNumCoords()) _minDim = vi.getNumCoords();
				final double vali = vi.getCoord(m);
				if (Double.compare(_minAttrVal[m],vali)>0) _minAttrVal[m] = vali;
				if (Double.compare(_maxAttrVal[m],vali)<0) _maxAttrVal[m] = vali;
			}
		}
		_maxDimDev = _maxDim - _minDim;
		if (_maxDimDev!=0) 
			throw new IllegalArgumentException("maxDim="+_maxDim+" minDim="+_minDim);
  }


  /**
   * return the sum of the normalized squared distances of each document from 
	 * the cluster center it belongs.
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
			System.err.println("KMeansNormSqrEvaluator: clusterer has null asgns");
      return Double.NaN;
    }
    final int n = docs.size();
    // final int k = centers.size();
		final int m = ((VectorIntf)docs.get(0)).getNumCoords();
    for (int i=0; i<n; i++) {
      VectorIntf di = (VectorIntf) docs.get(i);
      VectorIntf ci = (VectorIntf) centers.get(asgnms[i]);
      //ret += _m.dist(di, ci);
      double dist2 = 0.0;
			for (int j=0; j<m; j++) {
				double denomj = _maxAttrVal[j] - _minAttrVal[j];
				if (Double.compare(denomj, 0.0)==0) denomj=1.0;
				double dj = (di.getCoord(j)-ci.getCoord(j)) / denomj;
				dist2 += (dj*dj);
			}
      ret += dist2;
    }
    return ret;
  }

}

