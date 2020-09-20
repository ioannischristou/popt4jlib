package popt4jlib.neural.costfunction;

import popt4jlib.neural.OutputNNNodeIntf;
import popt4jlib.neural.FFNN4TrainB;
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
	
	/**
	 * evaluates the partial derivative of the entire network's cost function (as 
	 * a function of its weights) with respect to the weight variable whose weight 
	 * is given by the value of the weights array in the given index.
	 * @param weights double[] all variables (including biases) array
	 * @param index int the index of the partial derivative to take
	 * @param input_signals double[] the input signals for the network (an 
	 * instance of the training data)
	 * @param true_lbl double the true label corresponding to the input_signals
	 * vector
	 * @param p HashMap 
	 * @return double
	 */	
	public double evalPartialDerivativeB(double[] weights, int index, 
		                                   double[] input_signals, double true_lbl, 
																			 HashMap p) {
		// evaluate the output node of this network on the (x,y) training pair
		// compute the output node's derivative, and double the product of the two
		double lambda = 1.0;
		if (p.containsKey("ffnn.lambda")) {
			lambda = ((Double)p.get("ffnn.lambda")).doubleValue();
		}
		final FFNN4TrainB ffnn = getFFNN();
		OutputNNNodeIntf outn = ffnn.getOutputNode();
		// the hash-map p must contain &lt;"hiddenws$i$", double[][]&gt; as well as 
		// &lt;"outputws",double[]&gt; pairs.
		if (!p.containsKey("hiddenws0") || !p.containsKey("outputws")) {
			for (int i=0; i<ffnn.getNumHiddenLayers(); i++) {
				double[][] wgtsi = ffnn.getLayerWeightsWithBias(i, weights, 
					                                              input_signals.length);
				p.put("hiddenws"+i, wgtsi);
			}
			double[] outwgts = ffnn.getOutputWeightsWithBias(weights);
			p.put("outputws", outwgts);
		}
		//double err = ffnn.evalNetworkOnInputData(input_signals, p)-true_lbl;
		double err = ffnn.evalNetworkOutputOnTrainingData(weights, 
			                                                input_signals, true_lbl, 
								  																		p) - true_lbl;
		double g_index = outn.evalPartialDerivativeB(weights, index, 
			                                           input_signals, true_lbl, p);
		// add to the derivative the value lambda*weights[index]/sqrt(sum(weights^2)
		double sumw2 = 0.0;
		for (int i=0; i<weights.length; i++) {
			sumw2 += weights[i]*weights[i];
		}
		final double res = 2.0*err*g_index + lambda*weights[index]/Math.sqrt(sumw2);
		if (_mger.getDebugLvl()>=3) {
			String wstr="[ ";
			for (int i=0; i<weights.length; i++) wstr += weights[i]+" ";
			wstr += "]";
			String isstr="[ ";
			for (int i=0; i<input_signals.length; i++) isstr += input_signals[i]+" ";
			isstr += "]";
			_mger.msg("L2NormReg.evalPartialDerivativeB(weights="+wstr+
			 	        ", index="+index+
					      ", input_signals="+isstr+", lbl="+true_lbl+",p)="+res, 3);
		}
		return res;
	}
	
	
	/**
	 * always throws exception, as this method is not supported by this class. The
	 * reason for not being supported is that the &lambda; term that multiplies
	 * the L2-norm of the weights cannot be accessed here since we eliminated the
	 * parameters hash-map from the argument list in the signature of this method.
	 * @param weights double[] all variables (including biases) array
	 * @param index int the index of the partial derivative to take
	 * @param input_signals double[] the input signals for the network (an
	 * instance of the training data)
	 * @param true_lbl double the true label corresponding to the input_signals
	 * vector
	 * @param num_insts int unused
	 * @throws UnsupportedOperationException always
	 * @return double 
	 */
	public double evalPartialDerivativeB(double[] weights, int index,
		                                   double[] input_signals, double true_lbl,
																			 int num_insts){
		throw new UnsupportedOperationException("method not supported");
	}

	
	/**
	 * computes the partial derivative of this function on an array of values that
	 * is the derivative of this function on a set of training instances and 
	 * labels.
	 * @param d_per_instance double[] must have been computed from calls to 
	 * <CODE>evalPartialDerivativeB(wgts,ind,train_inst,train_lbl,p)</CODE>.
	 * @return double
	 */
	public double evalPartialDerivativeB(double[] d_per_instance) {
		double sum = 0.0;
		for (int i=0; i<d_per_instance.length; i++) {
			if (Double.isNaN(d_per_instance[i])) continue;
			sum += d_per_instance[i];
		}
		return sum;
	}

}
