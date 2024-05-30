package popt4jlib;

import popt4jlib.GradientDescent.VecUtil;


/**
 * Implements the <CODE>ArgDistanceCalcIntf</CODE> for 
 * <CODE>VectorIntf</CODE> objects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class VectorIntfDistCalc implements ArgDistanceCalcIntf {
	
	/** assuming d1 and d2 are <CODE>VectorIntf</CODE> objects, returns the
	 * Euclidean norm of their distance.
	 * @param d1 Object  // VectorIntf
	 * @param d2 Object  // VectorIntf
	 * @return double the vectors' Euclidean distance
	 * @throws ClassCastException if any of the arguments are not of the desired
	 * type
	 */
	public double dist(Object d1, Object d2) {
		VectorIntf v1 = (VectorIntf) d1;
		VectorIntf v2 = (VectorIntf) d2;
		return VecUtil.getEuclideanDistance(v1, v2);
	}
	
}
