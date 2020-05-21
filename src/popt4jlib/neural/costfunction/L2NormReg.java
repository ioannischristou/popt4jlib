package popt4jlib.neural.costfunction;

import java.util.HashMap;


/**
 * implements the regularized version of the L2-norm function, where besides the
 * L2-norm of the errors, we add to the result the magnitude of the weights as a
 * regularization component.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class L2NormReg extends L2Norm {
	
	/**
	 * single public no-arg constructor is a no-op.
	 */
	public L2NormReg() {
		super();
	}
	
	
	/**
	 * computes and returns ||x||_2 + &lambda; \times; ||w||_2.
	 * @param arg Object  // double[]
	 * @param params HashMap must contain the weights as <CODE>double[]</CODE> for
	 * key "ffnn.weights", and optionally the &lambda; regularizer value as value
	 * for the key "ffnn.lambda" (default is 1.0)
	 * @return double
	 */
	public double eval(Object arg, HashMap params) {
		final double errors_l2 = super.eval(arg, params);
		double[] ws = (double[]) params.get("ffnn.weights");
		double lambda = 1.0;
		if (params.containsKey("ffnn.lambda")) {
			lambda = ((Double)params.get("ffnn.lambda")).doubleValue();
		}
		double ws_l2 = 0.0;
		for (int i=0; i<ws.length; i++) {
			ws_l2 += ws[i]*ws[i];
		}
		return errors_l2 + lambda*Math.sqrt(ws_l2);
	}
}
