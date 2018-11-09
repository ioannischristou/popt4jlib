package popt4jlib.GradientDescent;

import popt4jlib.*;
import java.util.*;

/**
 * Utility class for Vector computations.
 * <p>Note:
 * <ul>
 * <li>2018-11-09: modified <CODE>innerProduct(x,y)</CODE> to work with
 * sparse data faster. Also modified all methods not to declare the (unchecked)
 * exceptions they may throw when passed illegal arguments.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.1
 */
public class VecUtil {

  /**
   * public no-arg constructor.
   */
  public VecUtil() {
    // no-op
  }


  /**
   * compute the inner product of two VectorIntf objects.
   * @param x VectorIntf
   * @param y VectorIntf
   * @throws IllegalArgumentException if any arg is null or if the dimensions
   * of the two args don't match. Not checked though.
   * @return double
   */
  public static double innerProduct(VectorIntf x, VectorIntf y) {
    if (x==null) throw new IllegalArgumentException("1st arg is null");
    if (y==null) throw new IllegalArgumentException("2nd arg is null");
    final int n = x.getNumCoords();
    if (n!=y.getNumCoords())
      throw new IllegalArgumentException("dimensions don't match");
    double res=0.0;
		// test if x or y are sparse vectors 
		if (x instanceof DblArray1SparseVector) {
			DblArray1SparseVector xs = (DblArray1SparseVector) x;
			if (Double.compare(xs.getDefaultValue(),0.0)==0) {  // else makes no sense
				final int xsnz = xs.getNumNonZeros();
				for (int i=0; i<xsnz; i++) {
					int xipos = xs.getIthNonZeroPos(i);
					double xi = xs.getIthNonZeroVal(i);
					res += xi*y.getCoord(xipos);
				}
				return res;
			}
		}
		if (y instanceof DblArray1SparseVector) {
			DblArray1SparseVector ys = (DblArray1SparseVector) y;
			if (Double.compare(ys.getDefaultValue(),0.0)==0) {  // else makes no sense
				final int ysnz = ys.getNumNonZeros();
				for (int i=0; i<ysnz; i++) {
					int yipos = ys.getIthNonZeroPos(i);
					double yi = ys.getIthNonZeroVal(i);
					res += yi*x.getCoord(yipos);
				}
				return res;
			}
		}
		// revert to full inner product computation of vectors in R^n.
    for (int i=0; i<n; i++) {
      res += x.getCoord(i)*y.getCoord(i);
    }
    return res;
  }


  /**
   * return a new (unmanaged) VectorIntf object that is the sum of the two 
	 * arguments.
   * @param x VectorIntf
   * @param y VectorIntf
   * @throws IllegalArgumentException if any arg is null or if the dimensions
   * of the two args don't match. Not checked though.
   * @return VectorIntf of the same run-time type (class) as x
   */
  public static VectorIntf add(VectorIntf x, VectorIntf y) {
    if (x==null) throw new IllegalArgumentException("1st arg is null");
    if (y==null) throw new IllegalArgumentException("2nd arg is null");
    final int n = x.getNumCoords();
    if (n!=y.getNumCoords())
      throw new IllegalArgumentException("dimensions don't match");
    VectorIntf z = x.newInstance();  // x.newCopy();
    try {
      for (int i = 0; i < n; i++) {
        z.setCoord(i, x.getCoord(i) + y.getCoord(i));
      }
    }
    catch (parallel.ParallelException e) {
      e.printStackTrace();  // can never get here
    }
    return z;
  }


  /**
   * return a new (unmanaged) VectorIntf object that is the difference (x-y) of 
	 * the two arguments.
   * @param x VectorIntf
   * @param y VectorIntf
   * @throws IllegalArgumentException if any arg is null or if the dimensions
   * of the two args don't match. Not checked though.
   * @return VectorIntf of the same run-time type (class) as x
   */
  public static VectorIntf subtract(VectorIntf x, VectorIntf y) {
    if (x==null) throw new IllegalArgumentException("1st arg is null");
    if (y==null) throw new IllegalArgumentException("2nd arg is null");
    final int n = x.getNumCoords();
    if (n!=y.getNumCoords())
      throw new IllegalArgumentException("dimensions don't match");
    VectorIntf z = x.newInstance();  // x.newCopy();
    for (int i=0; i<n; i++) {
      try {
        z.setCoord(i, x.getCoord(i) - y.getCoord(i));
      }
      catch (parallel.ParallelException e) {  // can never get here
        e.printStackTrace();
      }
    }
    return z;
  }

	
  /**
   * return a new (unmanaged) VectorIntf object that is the component-wise 
	 * product of the two arguments.
   * @param x VectorIntf
   * @param y VectorIntf
   * @throws IllegalArgumentException if any arg is null or if the dimensions
   * of the two args don't match. Not checked though.
   * @return VectorIntf of the same run-time type (class) as x
   */
  public static VectorIntf componentProduct(VectorIntf x, VectorIntf y) {
    if (x==null) throw new IllegalArgumentException("1st arg is null");
    if (y==null) throw new IllegalArgumentException("2nd arg is null");
    final int n = x.getNumCoords();
    if (n!=y.getNumCoords())
      throw new IllegalArgumentException("dimensions don't match");
    VectorIntf z = x.newInstance();  // x.newCopy();
    try {
      for (int i = 0; i < n; i++) {
        z.setCoord(i, x.getCoord(i) * y.getCoord(i));
      }
    }
    catch (parallel.ParallelException e) {
      e.printStackTrace();  // can never get here
    }
    return z;
  }


  /**
   * return the k-th norm of the vector x.
   * @param x VectorIntf
   * @param k int
   * @throws IllegalArgumentException if x==null or if k&le;0. But, not checked.
   * @return double
   */
  public static double norm(VectorIntf x, int k) {
    if (x==null) throw new IllegalArgumentException("x is null");
    if (k<=0) throw new IllegalArgumentException("k<=0");
    if (k==2) return norm2(x);  // faster computation
    if (x instanceof DblArray1SparseVector) {  // short-cut for sparse vectors
      return ((DblArray1SparseVector) x).norm(k);
    }
    int n = x.getNumCoords();
    double res = 0.0;
    for (int i=0; i<n; i++) {
      double absxi = Math.abs(x.getCoord(i));
      res += Math.pow(absxi, k);
    }
    res = Math.pow(res, 1.0/(double) k);
    return res;
  }


  /**
   * short-cut for norm(x,2). Faster too.
   * @param x VectorIntf
   * @throws IllegalArgumentException if x==null. But, not checked.
   * @return double
   */
  public static double norm2(VectorIntf x) {
    if (x==null) throw new IllegalArgumentException("x is null");
    if (x instanceof DblArray1SparseVector) {  // short-cut for sparse vectors
      return ((DblArray1SparseVector) x).norm2();
    }
    final int n = x.getNumCoords();
    double res2=0.0;
    for (int i=0; i<n; i++) {
      double xi = x.getCoord(i);
      res2 += (xi * xi);
    }
    return Math.sqrt(res2);
  }


  /**
   * computes the infinity norm of x.
   * @param x VectorIntf
   * @throws IllegalArgumentException if x==null. But, not checked.
   * @return double
   */
  public static double normInfinity(VectorIntf x) {
    if (x==null) throw new IllegalArgumentException("x is null");
    if (x instanceof DblArray1SparseVector) {  // short-cut for sparse vectors
      return ((DblArray1SparseVector) x).normInfinity();
    }
    int n = x.getNumCoords();
    double res = 0.0;
    for (int i=0; i<n; i++) {
      final double absxi = Math.abs(x.getCoord(i));
      if (absxi>res) res = absxi;
    }
    return res;
  }


  /**
   * computes the zero-norm of x (#non-zero components of x)
   * @param x VectorIntf
   * @throws IllegalArgumentException if x==null. But, not checked.
   * @return int
   */
  public static int zeroNorm(VectorIntf x) {
    if (x==null) throw new IllegalArgumentException("x is null");
    if (x instanceof SparseVectorIntf) {  // short-cut for sparse vectors
      SparseVectorIntf y = (SparseVectorIntf) x;
			if (Double.compare(y.getDefaultValue(),0.0)==0) return y.getNumNonZeros();
    }
    final int n = x.getNumCoords();
    int res=0;
    for (int i=0; i<n; i++)
      if (Double.compare(x.getCoord(i),0.0)!=0) ++res;
    return res;
  }


  /**
   * checks if the two argument vectors have all their components equal. Faster
	 * only if both arguments are <CODE>DblArray1SparseVector</CODE>.
   * @param x VectorIntf
   * @param y VectorIntf
   * @throws IllegalArgumentException if x &amp; y are both null. Not checked.
   * @return boolean true iff all the components of the vectors x and y are
   * equal (so that for each component i, the comparison 
	 * <CODE>Double.compare(xi,yi)==0</CODE> returns true).
   * Returns false immediately if the two vectors have different lengths
   */
  public static boolean equal(VectorIntf x, VectorIntf y) {
    if (x==null && y==null) 
			throw new IllegalArgumentException("both args null");
    if (x==null || y==null) return false;
    if (x.getNumCoords()!=y.getNumCoords()) return false;
    // short-cut for specific sparse vectors
    if (x instanceof DblArray1SparseVector && 
			  y instanceof DblArray1SparseVector)
      return ((DblArray1SparseVector) x).equals((DblArray1SparseVector) y);
    for (int i=0; i<x.getNumCoords(); i++) {
      if (Double.compare(x.getCoord(i),y.getCoord(i))!=0) return false;
    }
    return true;
  }


  /**
   * returns the center of a collection of VectorIntf objects.
   * @param vectors Collection
   * @throws IllegalArgumentException if the collection is null, empty or if it
   * contains vectors of varying dimensions. Not checked though.
   * @return VectorIntf
   */
  public static VectorIntf getCenter(Collection vectors) {
    if (vectors==null || vectors.size()==0)
      throw new IllegalArgumentException("null argument or empty set");
    Iterator it = vectors.iterator();
    VectorIntf res = null;
    int n = 0;
    double[] arr = null;
    while (it.hasNext()) {
      VectorIntf x = (VectorIntf) it.next();
      n = x.getNumCoords();
      if (arr==null) arr = new double[n];
      else if (arr.length!=n)
        throw new IllegalArgumentException("vectors in collection "+
					                                 "have different length");
      for (int i=0; i<n; i++) arr[i] += x.getCoord(i);
    }
    for (int i=0; i<arr.length; i++) arr[i] /= ((double) vectors.size());
    res = new DblArray1Vector(arr);
    return res;
  }


  /**
   * returns the Euclidean distance between two VectorIntf objects.
   * @param x VectorIntf
   * @param y VectorIntf
   * @throws IllegalArgumentException if any arg is null or if the two args
   * dimensions differ. Exception is not checked.
   * @return double
   */
  public static double getEuclideanDistance(VectorIntf x, VectorIntf y) {
    if (x==null || y==null) 
			throw new IllegalArgumentException("at least one arg null");
    if (x.getNumCoords()!=y.getNumCoords()) 
			throw new IllegalArgumentException("args of different dimensions");
    double dist = 0.0;
    final int n = x.getNumCoords();
    for (int i=0; i<n; i++) {
      double xi = x.getCoord(i);
      double yi = y.getCoord(i);
      dist += (xi-yi)*(xi-yi);
    }
    return Math.sqrt(dist);
  }


  /**
   * return the cosine-similarity between x and y (ie the cos(x/\y))
   * used in Information Retrieval. This similarity measure is not a "distance"
   * measure, as the more "similar" two vectors are, the greater the returned
   * value of this method.
   * @param x VectorIntf
   * @param y VectorIntf
   * @throws IllegalArgumentException if x or y are null or if one of the two is
   * the zero vector, or if the two vectors' dimensions don't agree. Not checked
	 * though.
   * @return double
   */
  public static double getCosineSimilarityDistance(VectorIntf x, VectorIntf y) {
    if (x==null || y==null) 
			throw new IllegalArgumentException("at least one arg null");
    if (x.getNumCoords()!=y.getNumCoords()) 
			throw new IllegalArgumentException("args of different dimensions");
    double x_norm = norm2(x);
    double y_norm = norm2(y);
    if (Double.compare(x_norm,0.0)==0 || Double.compare(y_norm,0.0)==0)
      throw new IllegalArgumentException("x or y are zero");
    double ip;
    if (x instanceof DblArray1SparseVector) {
      ip = ((DblArray1SparseVector) x).innerProduct(y);
      return ip/(x_norm*y_norm);  // should be -ip/...
    }
    if (y instanceof DblArray1SparseVector) {
      ip = ((DblArray1SparseVector) y).innerProduct(x);
      return ip/(x_norm*y_norm);  // should be -ip/...
    }
    // else
    ip = innerProduct(x,y);
    return ip/(x_norm*y_norm);  // should be -ip/...
  }


  /**
   * return the Jaccard distance measure between two vectors. Also known as the
   * Tanimoto distance measure (used in Information Retrieval, 
	 * Recommender Systems etc.)
   * @param x VectorIntf
   * @param y VectorIntf
   * @throws IllegalArgumentException if any argument is null or if dimensions 
	 * don't match. Not checked though.
   * @return double
   */
  public static double getJaccardDistance(VectorIntf x, VectorIntf y) {
    if (x==null || y==null) 
			throw new IllegalArgumentException("at least one arg null");
    if (x.getNumCoords()!=y.getNumCoords()) 
			throw new IllegalArgumentException("args of different dimensions");
    double ip = innerProduct(x,y);
    double x_norm = norm2(x);
    double y_norm = norm2(y);
    if (Double.compare(x_norm,0.0)==0 && Double.compare(y_norm,0.0)==0)
      throw new IllegalArgumentException("x and y are both zero");
    return 1.0 - ip / (x_norm + y_norm - ip);
  }

}

