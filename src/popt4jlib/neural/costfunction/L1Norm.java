package popt4jlib.neural.costfunction;

import popt4jlib.neural.FFNN4TrainB;
import popt4jlib.neural.OutputNNNodeIntf;
import popt4jlib.neural.BaseNNNode;
import utils.Messenger;
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
public class L1Norm implements FFNNCostFunctionIntf {
	private final static Messenger _mger = Messenger.getInstance();
	
	private FFNN4TrainB _ffnn;

	
	/**
	 * single public no-arg constructor is a no-op.
	 */
	public L1Norm() {
		// no-op
	}
	

	/**
	 * computes the value 1 or -1 depending on the sign of the argument.
	 * @param x double
	 * @param num_insts int unused
	 * @return double
	 */
	public double evalDerivative(double x, int num_insts) {
		return Double.compare(x, 0.0) > 0 ? 1 : -1;
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
		for (int i=0; i<x.length; i++) {
			if (Double.isNaN(x[i])) {
				continue;
			}
			result += (x[i] > 0.0 ? x[i] : -x[i]);
		}
		return result;
	}

	
	/**
	 * returns the network this function is set be part of.
	 * @return FFNN4TrainB
	 */
	public FFNN4TrainB getFFNN() {
		return _ffnn;
	}
	
	
	/**
	 * set the network this function is part of.
	 * @param f FFNN4TrainB
	 */
	public void setFFNN(FFNN4TrainB f) {
		_ffnn = f;
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
		OutputNNNodeIntf outn = _ffnn.getOutputNode();
		// the hash-map p must contain &lt;"hiddenws$i$", double[][]&gt; as well as 
		// &lt;"outputws",double[]&gt; pairs.
		if (!p.containsKey("hiddenws0") || !p.containsKey("outputws")) {
			for (int i=0; i<_ffnn.getNumHiddenLayers(); i++) {
				double[][] wgtsi = _ffnn.getLayerWeightsWithBias(i, weights, 
					                                               input_signals.length);
				p.put("hiddenws"+i, wgtsi);
			}
			double[] outwgts = _ffnn.getOutputWeightsWithBias(weights);
			p.put("outputws", outwgts);
		}
		//final double err=_ffnn.evalNetworkOnInputData(input_signals, p)-true_lbl;
		double err = _ffnn.evalNetworkOutputOnTrainingData(weights, 
			                                                 input_signals, true_lbl, 
																											 p) - true_lbl;
		final double g_index = outn.evalPartialDerivativeB(weights, index, 
			                                                 input_signals, true_lbl, 
																											 p);
		final double res = Double.compare(err, 0.0) > 0 ? g_index : -g_index;
		if (_mger.getDebugLvl()>=3) {
			String wstr="[ ";
			for (int i=0; i<weights.length; i++) wstr += weights[i]+" ";
			wstr += "]";
			String isstr="[ ";
			for (int i=0; i<input_signals.length; i++) isstr += input_signals[i]+" ";
			isstr += "]";
			_mger.msg("L1Norm.evalPartialDerivativeB(weights="+wstr+
			 	        ", index="+index+
					      ", input_signals="+isstr+", lbl="+true_lbl+",p)="+res, 3);
		}
		return res;
	}
	
	
	/**
	 * same as 
	 * <CODE>
	 * evalPartialDerivativeB(weights, index, input_signals, true_lbl, p)
	 * </CODE>
	 * but uses the grad-vector thread-local caches instead of the last deriv 
	 * cache. Also uses the thread-local last-eval cache.
	 * @param weights double[] all variables (including biases) array
	 * @param index int the index of the partial derivative to take
	 * @param input_signals double[] the input signals for the network (an
	 * instance of the training data)
	 * @param true_lbl double the true label corresponding to the input_signals
	 * vector
	 * @param num_insts int the number of training instances unused
	 * @return double 
	 */
	public double evalPartialDerivativeB(double[] weights, int index,
		                                   double[] input_signals, double true_lbl,
																			 int num_insts){
		// evaluate the output node of this network on the (x,y) training pair
		// compute the output node's derivative, and double the product of the two
		OutputNNNodeIntf outn = _ffnn.getOutputNode();
		BaseNNNode boutn = (BaseNNNode) outn;
		final double cache = boutn.getLastEvalCache();
		double ffnn_eval = Double.isNaN(cache)==false ? 
		  cache : 
		  _ffnn.evalNetworkOutputOnTrainingData(weights, input_signals, true_lbl, 
																					  null);  
		double err = ffnn_eval - true_lbl;
		final double g_index = outn.evalPartialDerivativeB(weights, index, 
			                                                 input_signals, true_lbl);
		final double res = Double.compare(err, 0.0) > 0 ? g_index : -g_index;
		if (_mger.getDebugLvl()>=3) {
			String wstr="[ ";
			for (int i=0; i<weights.length; i++) wstr += weights[i]+" ";
			wstr += "]";
			String isstr="[ ";
			for (int i=0; i<input_signals.length; i++) isstr += input_signals[i]+" ";
			isstr += "]";
			_mger.msg("L1Norm.evalPartialDerivativeB(weights="+wstr+
			 	        ", index="+index+
					      ", input_signals="+isstr+", lbl="+true_lbl+")="+res, 3);
		}
		return res;		
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
			if (Double.isNaN(d_per_instance[i])) {
				continue;
			}
			sum += d_per_instance[i];
		}
		return sum;
	}
}
