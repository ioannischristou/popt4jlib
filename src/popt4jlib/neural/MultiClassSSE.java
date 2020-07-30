package popt4jlib.neural;

import utils.Messenger;
import java.util.HashMap;


/**
 * class implements the max input signal position selector when acting as an 
 * input training instance evaluator. However, during FFNN training, the output
 * it computes is the sum of the square errors of each of its input signals
 * which must equal the number of classes in the training set. These input 
 * signals must be in the range [0,1] (thus the <CODE>Sigmoid</CODE> or 
 * <CODE>TanH01</CODE> classes are best suited for this last hidden layer).
 * Obviously weights of the last hidden layer towards this output node are not 
 * used. This class should be used together with the 
 * <CODE>popt4jlib.neural.costfunction.L1Norm</CODE> (or alternatively, the
 * <CODE>popt4jlib.neural.costfunction.MAE</CODE>) cost functions, as it already
 * computes as error the sum of square errors of each of the (sigmoid?) nodes in
 * the previous layer.
 * Notice when used as hidden node (not output), this class is discontinuous 
 * (thus NOT differentiable) wherever the max value is attained by more than one 
 * argument. In all other cases the partial derivative is zero. This implies 
 * that the class should only be used as output node!
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class MultiClassSSE extends BaseNNNode implements OutputNNNodeIntf {
	
	private final static Messenger _mger = Messenger.getInstance();
	
	private Linear _linearUnit = new Linear();  // used to compute node input sum

	
	/**
	 * public no-arg, no-op constructor.
	 */
	public MultiClassSSE() {
		// no-op
	}
	
	
	/**
	 * computes and returns the index in inputSignals argument that has the 
	 * largest value.
	 * @param inputSignals double[]
	 * @param weights double[] unused
	 * @return double  // int in {0,1,...inputSignals.length-1}
	 */
	public double eval(double[] inputSignals, double[] weights) {
		if (weights.length!=inputSignals.length) 
			throw new IllegalArgumentException("weights and inputSignals lengths "+
				                                 "don't match");
		double max = Double.NEGATIVE_INFINITY;
		int bind = -1;
		for (int i=0; i<inputSignals.length; i++) {
			if (Double.compare(inputSignals[i], max)>0) {
				max = inputSignals[i];
				bind = i;
			}
		}
		return bind;
	}

	
	/**
	 * same as <CODE>eval(s,w)</CODE> method, but the 2nd argument is now assumed
	 * to hold all the network weights.
	 * @param inputSignals double[]
	 * @param weights double[] unused
	 * @param offset int unused
	 * @return double  // int in {0,1,...inputSignals.length-1}
	 */
	public double eval(double[] inputSignals, double[] weights, int offset) {
		if (weights.length<offset+inputSignals.length) 
			throw new IllegalArgumentException("weights length is incorrect");
		double max = Double.NEGATIVE_INFINITY;
		int bind = -1;
		for (int i=0; i<inputSignals.length; i++) {
			if (Double.compare(inputSignals[i], max)>0) {
				max = inputSignals[i];
				bind = i;
			}
		}
		return bind;
	}

		
	/**
	 * same as <CODE>eval(s,w)</CODE> method, but the 2nd argument is now assumed
	 * to hold the bias of the node as well (goes unused).
	 * @param inputSignals double[]
	 * @param weights double[] unused; bias also has no role here
	 * @return double  // int in {0,1,...inputSignals.length-1}
	 */
	public double evalB(double[] inputSignals, double[] weights) {
		if (weights.length!=inputSignals.length+1) 
			throw new IllegalArgumentException("(biased)weights and inputSignals "+
				                                 "lengths don't match");
		double max = Double.NEGATIVE_INFINITY;
		int bind = -1;
		for (int i=0; i<inputSignals.length; i++) {
			if (Double.compare(inputSignals[i],max)>0) {
				max = inputSignals[i];
				bind = i;
			}
		}
		return bind;
	}	
	
	
	/**
	 * same as <CODE>evalB(s,w)</CODE> method, but the 2nd argument is now assumed
	 * to hold as last element the bias term for this node (still goes unused).
	 * @param inputSignals double[]
	 * @param weights double[] unused; bias also has no role here
	 * @param offset int unused
	 * @return double  // int in {0,1,...inputSignals.length-1}
	 */
	public double evalB(double[] inputSignals, double[] weights, int offset) {
		if (weights.length<offset+inputSignals.length+1) 
			throw new IllegalArgumentException("(biased)weights length is incorrect");
		double max = Double.NEGATIVE_INFINITY;
		int bind = -1;
		for (int i=0; i<inputSignals.length; i++) {
			if (Double.compare(inputSignals[i],max)>0) {
				max = inputSignals[i];
				bind = i;
			}
		}
		return bind;
	}
	
	
	/**
	 * called when the node is used as output node during training.
	 * @param inputSignals double[]
	 * @param weights double[] unused
	 * @param offset int unused
	 * @param true_label double  // int really
	 * @return double
	 */
	public double eval(double[] inputSignals, double[] weights, int offset, 
		                 double true_label) {
		double sse = 0.0;
		final int true_class = (int) true_label;
		for (int i=0; i<inputSignals.length; i++) {
			double last_layer_out_i = inputSignals[i];
			double expected_i = i == true_class ? 1.0 : 0.0;
			double err_i = last_layer_out_i - expected_i;
			sse += err_i*err_i;
		}
		final double result = sse+true_label;  // add true_label so that the error 
		                                       // in cost function becomes just sse
		// cache result for speeding up auto-differentiation
		setLastInputsCache(inputSignals);
		setLastEvalCache(result);
		return result;
	}
	

	/**
	 * called when the node is used as output node during training.
	 * @param inputSignals double[]
	 * @param weights double[] unused
	 * @param offset int unused
	 * @param true_label double  // int really
	 * @return double
	 */
	public double evalB(double[] inputSignals, double[] weights, int offset,
		                  double true_label) {
		return eval(inputSignals, weights, offset, true_label);
	}
	
	
	/**
	 * get this node's name.
	 * @return String "MultiClassSSE"
	 */
	public String getNodeName() {
		return "MultiClassSSE";
	}


	/**
	 * evaluates the partial derivative of this node (as a function of weights)
	 * with respect to the weight variable whose weight is given by the value of 
	 * the weights array in the given index. The derivative for this node exists
	 * everywhere when it is used as the output node.
	 * @param weights double[] all variables (including biases) array
	 * @param index int the index of the partial derivative to take
	 * @param inputSignals double[]
	 * @param true_lbl double
	 * @param p HashMap includes the train-data matrix and train-labels array
	 * @return double
	 * @throws IllegalStateException if this node is not the output node of the 
	 * network it belongs to
	 */
	public double evalPartialDerivativeB(double[] weights, int index, 
		                                   double[] inputSignals, double true_lbl,
																			 HashMap p) {
		if (_ffnn.getOutputNode()!=this) {
			throw new IllegalStateException("MultiClassSSE node used as hidden node");
		}
		// 0. see if the value is already computed before
		double cache = getLastDerivEvalCache();
		if (!Double.isNaN(cache)) {
			return cache;
		}
		// 1. if index is after input weights, throw exception!
		if (index > _biasInd) {
			throw new IllegalArgumentException("MultiClassSSE node is output but "+
				                                 "index="+index+" > _bias="+_biasInd);
		}
		// 2. if index is for direct input weights (or bias) derivative is zero
		else if (_startWeightInd <= index && index <= _biasInd) {
			final double result = 0.0;
			if (_mger.getDebugLvl()>=5) {
				String wstr="[ ";
				for (int i=0; i<weights.length; i++) wstr += weights[i]+" ";
				wstr += "]";
				String isstr="[ ";
				for (int i=0; i<inputSignals.length; i++) isstr += inputSignals[i]+" ";
				isstr += "]";
				_mger.msg("MultiClassSSE<"+_startWeightInd+
				 	        ">.evalPartialDerivativeB(weights="+wstr+
					        ", index="+index+
						      ", input_signals="+isstr+", lbl="+true_lbl+",p)="+result, 5);
			}
			setLastDerivEvalCache(result);
			return result;
		}
		// 3. if index is for a weight connecting the previous layer that this node
		//    belongs to with another node of this layer (but not this node), 
		//    result is zero
		else if (!isWeightVariableAntecedent(index)) {
			if (_mger.getDebugLvl()>=5) {
				String wstr="[ ";
				for (int k=0; k<weights.length; k++) wstr += weights[k]+" ";
				wstr += "]";
				String isstr="[ ";
				for (int k=0; k<inputSignals.length; k++) 
					isstr += inputSignals[k]+" ";
				isstr += "]";
				_mger.msg("MultiClassSSE<"+_startWeightInd+
				          ">.evalPartialDerivativeB(weights="+wstr+
				          ", index="+index+
				          ", input_signals="+isstr+", lbl="+true_lbl+",p)=0", 5);
			}				
			setLastDerivEvalCache(0.0);
			return 0.0;
		}
		// 4. else index is for a previous signal weight, and derivative is the 
		//    sum of the errors si-ei of each input signal to this node minus the
		//    "correct" value for that signal multiplied by the partial derivative
		//    of the input signal; all that multiplied by 2
		else {
			int layer_of_node = _ffnn.getNumHiddenLayers();  // it's the output node
			double[] last_inputs = getLastInputsCache();
			if (last_inputs == null) {  // nope, not in cache, must work from scratch
				final int num_inputs = inputSignals.length;  // get the #input_signals
				final NNNodeIntf[][] hidden_layers = _ffnn.getHiddenLayers();
				final int num_hidden_layers = hidden_layers.length;
				// compute from layer-0 to this node
				int pos = 0;  // the position index in the vector weights
				// get inputs for the layer. Inputs are same for all nodes in a layer.
				double[] layer_i_inputs = new double[num_inputs];
				for (int i=0; i<num_inputs; i++) layer_i_inputs[i] = inputSignals[i];
				for (int i=0; i<num_hidden_layers; i++) {
					NNNodeIntf[] layeri = hidden_layers[i];
					double[] layeri_outputs = new double[layeri.length];
					for (int j=0; j<layeri.length; j++) {
						NNNodeIntf node_i_j = layeri[j];
						layeri_outputs[j] = node_i_j.evalB(layer_i_inputs, weights, pos);
						pos += layer_i_inputs.length + 1;  // +1 is for the bias
					}
					layer_i_inputs = layeri_outputs;  // set inputs for next iteration
				}
				last_inputs = layer_i_inputs;
				setLastInputsCache(last_inputs);
			}
			double result = 0.0;			
			NNNodeIntf[] prev_layer = _ffnn.getHiddenLayers()[layer_of_node-1];
			for (int j=0; j<last_inputs.length; j++) {
				double expected_j = j == (int)true_lbl ? 1.0 : 0.0;
				double errj = last_inputs[j] - expected_j;
				result += errj * 
				          prev_layer[j].evalPartialDerivativeB(weights, index, 
									                                     inputSignals, 
																											 true_lbl,
																											 p);
			}
			result += result;  // 2*result
			if (_mger.getDebugLvl()>=5) {
				String wstr="[ ";
				for (int k=0; k<weights.length; k++) wstr += weights[k]+" ";
				wstr += "]";
				String isstr="[ ";
				for (int k=0; k<inputSignals.length; k++) 
					isstr += inputSignals[k]+" ";
				isstr += "]";
				_mger.msg("MultiClassSSE<"+_startWeightInd+
				          ">.evalPartialDerivativeB(weights="+wstr+
				          ", index="+index+
				          ", input_signals="+isstr+", lbl="+true_lbl+",p)="+
				          result, 0);
				String listr="[ ";
				for (int k=0; k<last_inputs.length; k++) listr += last_inputs[k]+" ";
				listr += "]";
				_mger.msg("last_inputs="+listr,0);
			}
			setLastDerivEvalCache(result);
			return result;
		}
	}

	
	/**
	 * evaluates the partial derivative of this node (as a function of weights)
	 * with respect to the weight variable whose weight is given by the value of 
	 * the weights array in the given index, using the grad vector thread local 
	 * cache. The derivative for this node exists everywhere when it is used as 
	 * the output node.
	 * @param weights double[] all variables (including biases) array
	 * @param index int the index of the partial derivative to take
	 * @param inputSignals double[]
	 * @param true_lbl double
	 * @return double
	 * @throws IllegalStateException if this node is not the output node of the 
	 * network it belongs to
	 */
	public double evalPartialDerivativeB(double[] weights, int index, 
		                                   double[] inputSignals, double true_lbl) {
		if (_ffnn.getOutputNode()!=this) {
			throw new IllegalStateException("MultiClassSSE node used as hidden node");
		}
		// 0. see if the value is already computed before
		//double cache = getLastDerivEvalCache();
		double cache = getGradVectorCache()[index];
		if (!Double.isNaN(cache)) {
			return cache;
		}
		// 1. if index is after input weights, throw exception!
		if (index > _biasInd) {
			throw new IllegalArgumentException("MultiClassSSE node is output but "+
				                                 "index="+index+" > _bias="+_biasInd);
		}
		// 2. if index is for direct input weights (or bias) derivative is zero
		else if (_startWeightInd <= index && index <= _biasInd) {
			final double result = 0.0;
			//setLastDerivEvalCache(result);
			setGradVectorCache(index, result);
			return result;
		}
		// 3. if index is for a weight connecting the previous layer that this node
		//    belongs to with another node of this layer (but not this node), 
		//    result is zero
		else if (!isWeightVariableAntecedent(index)) {
			//setLastDerivEvalCache(0.0);
			setGradVectorCache(index, 0.0);
			return 0.0;
		}
		// 4. else index is for a previous signal weight, and derivative is the 
		//    sum of the errors si-ei of each input signal to this node minus the
		//    "correct" value for that signal multiplied by the partial derivative
		//    of the input signal; all that multiplied by 2
		else {
			int layer_of_node = _ffnn.getNumHiddenLayers();  // it's the output node
			double[] last_inputs = getLastInputsCache();
			if (last_inputs == null) {  // nope, not in cache, must work from scratch
				final int num_inputs = inputSignals.length;  // get the #input_signals
				final NNNodeIntf[][] hidden_layers = _ffnn.getHiddenLayers();
				final int num_hidden_layers = hidden_layers.length;
				// compute from layer-0 to this node
				int pos = 0;  // the position index in the vector weights
				// get inputs for the layer. Inputs are same for all nodes in a layer.
				double[] layer_i_inputs = new double[num_inputs];
				for (int i=0; i<num_inputs; i++) layer_i_inputs[i] = inputSignals[i];
				for (int i=0; i<num_hidden_layers; i++) {
					NNNodeIntf[] layeri = hidden_layers[i];
					double[] layeri_outputs = new double[layeri.length];
					for (int j=0; j<layeri.length; j++) {
						NNNodeIntf node_i_j = layeri[j];
						layeri_outputs[j] = node_i_j.evalB(layer_i_inputs, weights, pos);
						pos += layer_i_inputs.length + 1;  // +1 is for the bias
					}
					layer_i_inputs = layeri_outputs;  // set inputs for next iteration
				}
				last_inputs = layer_i_inputs;
				setLastInputsCache(last_inputs);
			}
			double result = 0.0;			
			NNNodeIntf[] prev_layer = _ffnn.getHiddenLayers()[layer_of_node-1];
			for (int j=0; j<last_inputs.length; j++) {
				double expected_j = j == (int)true_lbl ? 1.0 : 0.0;
				double errj = last_inputs[j] - expected_j;
				result += errj * 
				          prev_layer[j].evalPartialDerivativeB(weights, index, 
									                                     inputSignals, 
																											 true_lbl);
			}
			result += result;  // 2*result
			//setLastDerivEvalCache(result);
			setGradVectorCache(index, result);
			return result;
		}
	}

}
