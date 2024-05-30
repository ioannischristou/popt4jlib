package popt4jlib;

import popt4jlib.GradientDescent.VecUtil;


/**
 * Implements the <CODE>ArgDistanceCalcIntf</CODE> for <CODE>double[]</CODE> 
 * objects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1DistCalc implements ArgDistanceCalcIntf {
	
	/** assuming d1 and d2 are <CODE>double[]</CODE> objects, returns the
	 * Euclidean norm of their distance.
	 * @param d1 Object  // double[]
	 * @param d2 Object  // double[]
	 * @return double the vectors' Euclidean distance
	 * @throws ClassCastException if any of the arguments are not of the desired
	 * type
	 */
	public double dist(Object d1, Object d2) {
		double[] a1 = (double[]) d1;
		double[] a2 = (double[]) d2;
		DblArray1Vector v1 = new DblArray1Vector(a1);
		DblArray1Vector v2 = new DblArray1Vector(a2);
		return VecUtil.getEuclideanDistance(v1, v2);
	}
	
}
