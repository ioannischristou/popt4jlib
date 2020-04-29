package popt4jlib.neural.costfunction;

import popt4jlib.FunctionIntf;
import java.util.HashMap;

/**
 * implements the L1-norm function.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class L1Norm implements FunctionIntf {
	
	/**
	 * single public no-arg constructor is a no-op.
	 */
	public L1Norm() {
		// no-op
	}
	
	
	/**
	 * computes and returns ||x||_1.
	 * @param arg Object  // double[]
	 * @param params HashMap unused
	 * @return double
	 */
	public double eval(Object arg, HashMap params) {
		double[] x = (double[])arg;
		double result = 0.0;
		for (int i=0; i<x.length; i++) result += (x[i] > 0.0 ? x[i] : -x[i]);
		return result;
	}
}
