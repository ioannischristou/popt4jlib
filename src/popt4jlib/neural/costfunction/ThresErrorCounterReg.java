package popt4jlib.neural.costfunction;

import popt4jlib.FunctionIntf;
import java.util.HashMap;

/**
 * implements the regularized version of the ThresErrorCounter function, where 
 * besides the threshold-based counting of the errors, we add to the result the 
 * magnitude of the weights (multiplied by a &lambda; factor) as regularization 
 * component.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ThresErrorCounterReg extends ThresErrorCounter {
	
	private double _lambda = 1.0;
	
	
	/**
	 * single public no-arg constructor.
	 */
	public ThresErrorCounterReg() {
		super();
	}
	
	
	/**
	 * public constructor sets the regularization factor (may be overriden in the
	 * params of the <CODE>eval()</CODE> function call.)
	 * @param l double
	 */
	public ThresErrorCounterReg(double l) {
		this();
		_lambda = l;
	}
	
	
	/**
	 * computes and returns the number of components that deviate from zero too
	 * much, plus the L2-norm of the weights multiplied by a &lambda; factor that
	 * may be optionally included in the params.
	 * @param arg Object  // double[]
	 * @param params HashMap may contain a key-value pair of the form 
	 * &lt;"ffnn.errorcountthres",$val$&gt;. Default is 1.e-6. It must contain a
	 * key-value pair of the form &lt;"ffnn.weights", double[] weights&gt; that 
	 * are the weights. May also contain an optional &lt;"ffnn.lambda",Double&gt;
	 * pair that will override the current <CODE>_lambda</CODE> value.
	 * @return double
	 */
	public double eval(Object arg, HashMap params) {
		double errors_thres = super.eval(arg, params);
		// add the weight regularization term
		double lambda = _lambda;
		if (params.containsKey("ffnn.lambda")) {
			lambda = ((Double)params.get("ffnn.lambda")).doubleValue();
		}
		double[] ws = (double[])params.get("ffnn.weights");
		double ws_l2 = 0.0;
		for (int i=0; i<ws.length; i++) {
			ws_l2 += ws[i]*ws[i];
		}
		return errors_thres + lambda*Math.sqrt(ws_l2);
	}
}

