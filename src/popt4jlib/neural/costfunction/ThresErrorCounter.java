package popt4jlib.neural.costfunction;

import popt4jlib.FunctionIntf;
import java.util.HashMap;

/**
 * implements an error counter where as error in a vector component counts any
 * quantity that is larger than a threshold in absolute value.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ThresErrorCounter implements FunctionIntf {
	
	/**
	 * single public no-arg constructor is a no-op.
	 */
	public ThresErrorCounter() {
		// no-op
	}
	
	
	/**
	 * computes and returns the number of components that deviate from zero too
	 * much.
	 * @param arg Object  // double[]
	 * @param params HashMap may contain a key-value pair of the form 
	 * &lt;"ffnn.errorcountthres",$val$&gt;. Default is 1.e-6.
	 * @return double
	 */
	public double eval(Object arg, HashMap params) {
		double[] x = (double[])arg;
		double thres = 1.e-6;
		if (params!=null && params.containsKey("ffnn.errorcountthres")) {
			thres = ((Double)params.get("ffnn.errorcountthres")).doubleValue();
		}
		double result = 0.0;
		for (int i=0; i<x.length; i++) 
			if (Double.compare(Math.abs(x[i]),thres)>0) result += 1.0;
		return result;
	}
}

