package popt4jlib.neural.costfunction;

import popt4jlib.FunctionIntf;
import java.util.HashMap;

/**
 * implements the Mean Absolute Error (MAE) function.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class MAE implements FunctionIntf {
	
	/**
	 * single public no-arg constructor is a no-op.
	 */
	public MAE() {
		// no-op
	}
	
	
	/**
	 * computes and returns the value (|x_1|+|x_2|+...+|x_n|)/m where m is the 
	 * number of non-NaN components of x.
	 * @param arg Object  // double[]
	 * @param params HashMap unused
	 * @return double
	 */
	public double eval(Object arg, HashMap params) {
		double[] x = (double[])arg;
		double result = 0.0;
		int len=0;
		for (int i=0; i<x.length; i++) {
			if (Double.isNaN(x[i])) continue;
			result += (x[i] > 0.0 ? x[i] : -x[i]);
			++len;
		}
		return result/len;
	}
}
