package popt4jlib.neural;

import utils.Messenger;
import cern.jet.random.Normal;
import java.util.HashMap;


/**
 * GELU implements the Gaussian Error Linear Unit function used by Google, but
 * described in Hendrycks et al (2016), "Gaussian Error Linear Units (GELUs)". 
 * It is a smooth version of the classical ReLU. The class knows how to 
 * differentiate itself when gradient information is required via the 
 * <CODE>evalPartialDerivativeB()</CODE> methods that essentially implement
 * automatic differentiation for FeedForward Neural Networks. Can be used only 
 * as hidden layer node.
 * Notice the class computes &phi;(x) and &Phi;(x) methods via the CERN colt 
 * java numerical library, and in particular the jet sub-library.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class GELU extends BaseNNNode implements NNNodeIntf {
	
	private final static Messenger _mger = Messenger.getInstance();
	
	private Linear _linearUnit = new Linear();  // used to compute node input sum
	private Normal _norm = new Normal(0,1,null);  // should not need to be 
	                                              // ThreadLocal-ed
	
	
	/**
	 * public no-arg no-op constructor.
	 */
	public GELU() {
		// no-op.
	}
	
	
	/**
	 * Given arguments two vectors x and y, returns p&Phi;(p) where p=&lt;x,y&gt;.
	 * @param inputSignals double[]
	 * @param weights double[]
	 * @return double
	 */
	public double eval(double[] inputSignals, double[] weights) {
		if (isDropout()) return 0.0;
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[i];
		return prod*_norm.cdf(prod);
	}
	
	
	/**
	 * same as <CODE>eval(s,w)</CODE> method, but the 2nd argument is now assumed
	 * to hold all the network weights.
	 * @param inputSignals double[]
	 * @param weights double[]
	 * @param offset int
	 * @return double
	 */
	public double eval(double[] inputSignals, double[] weights, int offset) {
		if (isDropout()) return 0.0;
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[offset+i];
		return prod*_norm.cdf(prod);
	}

	
	/**
	 * Given arguments two vectors x and y, returns p&Phi;(p) where 
	 * p=&lt;x,y[0:x.len-1]&gt;+y[y.len-1].
	 * @param inputSignals double[]
	 * @param weights double[] includes as last element this node's bias term
	 * @return double
	 */
	public double evalB(double[] inputSignals, double[] weights) {
		if (isDropout()) {
			// cache values for speeding up auto-differentiation
			setLastInputsCache(inputSignals);
			setLastEvalCache(0.0);
			setLastDerivEvalCache2(0.0);  // since node is stuck at 0, derivative is 0
			return 0.0;
		}
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[i];
		prod += weights[inputSignals.length];  // bias term
		double result = prod*_norm.cdf(prod);
		// cache values for speeding up auto-differentiation
		setLastInputsCache(inputSignals);
		setLastEvalCache(result);
		return result;
	}

	
	/**
	 * same as <CODE>evalB(s,w)</CODE> method, but the 2nd argument is now assumed
	 * to hold all the network weights plus the nodes' biases.
	 * @param inputSignals double[]
	 * @param weights double[] includes nodes' biases
	 * @param offset int
	 * @return double
	 */
	public double evalB(double[] inputSignals, double[] weights, int offset) {
		if (isDropout()) {
			// cache values for speeding up auto-differentiation
			setLastInputsCache(inputSignals);
			setLastEvalCache(0.0);
			setLastDerivEvalCache2(0.0);
			return 0.0;
		}
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[offset+i];
		prod += weights[offset+inputSignals.length];  // bias term
		double result = prod*_norm.cdf(prod);
		// cache values for speeding up auto-differentiation
		setLastInputsCache(inputSignals);
		setLastEvalCache(result);
		setLastDerivEvalCache2(gPrime(prod));
		return result;
	}
	
	
	/**
	 * get this node's name.
	 * @return String "GELU"
	 */
	public String getNodeName() {
		return "GELU";
	}
	
	
	/**
	 * evaluates the partial derivative of this node (as a function of weights)
	 * with respect to the weight variable whose weight is given by the value of 
	 * the weights array in the given index. The derivative exists everywhere for 
	 * this "smoothed" version of ReLU.
	 * @param weights double[] all variables (including biases) array
	 * @param index int the index of the partial derivative to take
	 * @param inputSignals double[]
	 * @param true_lbl double
	 * @param p HashMap includes the train-data matrix and train-labels array.
	 * @return double
	 */
	public double evalPartialDerivativeB(double[] weights, int index, 
		                                   double[] inputSignals, double true_lbl,
																			 HashMap p) {
		if (isDropout()) {
			setLastDerivEvalCache(0.0);
			return 0.0;
		}
		// 0. see if the value is already computed before
		double cache = getLastDerivEvalCache();
		if (!Double.isNaN(cache)) return cache;
		// 1. if index is after input weights, derivative is zero
		if (index > _biasInd) {
			final double result = 0.0;
			if (_mger.getDebugLvl()>=5) {
				String wstr="[ ";
				for (int i=0; i<weights.length; i++) wstr += weights[i]+" ";
				wstr += "]";
				String isstr="[ ";
				for (int i=0; i<inputSignals.length; i++) isstr += inputSignals[i]+" ";
				isstr += "]";
				_mger.msg("GELU<"+_startWeightInd+
				 	        ">.evalPartialDerivativeB(weights="+wstr+
					        ", index="+index+
						      ", input_signals="+isstr+", lbl="+true_lbl+",p)="+result, 5);
			}
			setLastDerivEvalCache(result);
			return result;
		}
		// 2. if index is for bias, derivative is gPrime(node_input) 
		else if (index == _biasInd) {
			// compute this node's input-sum
			double[] last_inputs = getLastInputsCache();
			if (last_inputs!=null) {  // last_inputs exists in the cache!
				final int layerno = getNodeLayer();
				final NNNodeIntf[][] hidden_layers = _ffnn.getHiddenLayers();
				int pos = 0;
				int prev_layer_no = inputSignals.length;
				for (int i=0; i<layerno; i++) {
					pos += hidden_layers[i].length*(prev_layer_no+1);  // +1 for bias
					prev_layer_no = hidden_layers[i].length;
				}
				pos += getPositionInLayer()*(prev_layer_no+1);
				double node_input = _linearUnit.evalB(last_inputs,weights,pos);
				final double result = gPrime(node_input);
				setLastDerivEvalCache(result);
				return result;
			}
			// nope, node inputs must be computed from scratch
			double node_input = Double.NaN;
			final int num_inputs = inputSignals.length;  // get the #input_signals
			final NNNodeIntf[][] hidden_layers = _ffnn.getHiddenLayers();
			final int num_hidden_layers = hidden_layers.length;
			final OutputNNNodeIntf output_node = _ffnn.getOutputNode();
			// compute from layer-0 to this node
			int pos = 0;  // the position index in the vector weights
			// get inputs for the layer. Inputs are same for all nodes in a layer.
			double[] layer_i_inputs = new double[num_inputs];
			for (int i=0; i<num_inputs; i++) layer_i_inputs[i] = inputSignals[i];
			boolean found = false;
			for (int i=0; i<num_hidden_layers && !found; i++) {
				NNNodeIntf[] layeri = hidden_layers[i];
				double[] layeri_outputs = new double[layeri.length];
				for (int j=0; j<layeri.length; j++) {
					NNNodeIntf node_i_j = layeri[j];
					if (node_i_j == this) {  // this is the node we're looking for
						found = true;
						break;
					}
					layeri_outputs[j] = node_i_j.evalB(layer_i_inputs, weights, pos);
					pos += layer_i_inputs.length + 1;  // +1 is for the bias
				}
				if (!found)
					layer_i_inputs = layeri_outputs;  // set the inputs for next iteration
			}
			if (found) {
				node_input = _linearUnit.evalB(layer_i_inputs, weights, pos);
			}
			else if (output_node==this) {  // our node is the output node
				throw new IllegalStateException("GELU cannot be used as output node");
			}
			else {
				throw new IllegalStateException("couldn't find this node in FFNN");
			}
			final double result = gPrime(node_input);
			if (_mger.getDebugLvl()>=5) {
				String wstr="[ ";
				for (int i=0; i<weights.length; i++) wstr += weights[i]+" ";
				wstr += "]";
				String isstr="[ ";
				for (int i=0; i<inputSignals.length; i++) isstr += inputSignals[i]+" ";
				isstr += "]";
				_mger.msg("GELU<"+_startWeightInd+
					        ">.evalPartialDerivativeB(weights="+wstr+
					        ", index="+index+
						      ", input_signals="+isstr+", lbl="+true_lbl+",p)="+result, 5);
			}
			setLastInputsCache(layer_i_inputs);
			setLastDerivEvalCache(result);
			return result;
		}
		// 3. if index is for direct input weights, derivative is the derivative of
		//    the total node input multiplied by the corresponding signal applied to
		//    this weight
		else if (_startWeightInd <= index && index <= _endWeightInd) {
			double[] last_inputs = getLastInputsCache();
			if (last_inputs!=null) {
				final int layerno = getNodeLayer();
				final NNNodeIntf[][] hidden_layers = _ffnn.getHiddenLayers();
				int pos = 0;
				int prev_layer_no = inputSignals.length;
				for (int i=0; i<layerno; i++) {
					pos += hidden_layers[i].length*(prev_layer_no+1);  // +1 for bias
					prev_layer_no = hidden_layers[i].length;
				}
				pos += getPositionInLayer()*(prev_layer_no+1);
				double node_input=_linearUnit.evalB(last_inputs,weights,pos);
				double result = gPrime(node_input)*last_inputs[index-pos];
				setLastDerivEvalCache(result);
				return result;
			}
			// nope, not in cache, do the work
			final int num_inputs = inputSignals.length;  // get the #input_signals
			final NNNodeIntf[][] hidden_layers = _ffnn.getHiddenLayers();
			final int num_hidden_layers = hidden_layers.length;
			final OutputNNNodeIntf output_node = _ffnn.getOutputNode();
			// compute from layer-0 to this node
			int pos = 0;  // the position index in the vector weights
			// get inputs for the layer. Inputs are same for all nodes in a layer.
			double[] layer_i_inputs = new double[num_inputs];
			for (int i=0; i<num_inputs; i++) 
				layer_i_inputs[i] = inputSignals[i];
			for (int i=0; i<num_hidden_layers; i++) {
				NNNodeIntf[] layeri = hidden_layers[i];
				double[] layeri_outputs = new double[layeri.length];
				for (int j=0; j<layeri.length; j++) {
					NNNodeIntf node_i_j = layeri[j];
					if (node_i_j == this) {  // this is the node we're looking for
						double node_input=_linearUnit.evalB(layer_i_inputs,weights,pos);
						double result = gPrime(node_input)*layer_i_inputs[index-pos];
						if (_mger.getDebugLvl()>=5) {
							String wstr="[ ";
								for (int k=0; k<weights.length; k++) wstr += weights[k]+" ";
								wstr += "]";
								String isstr="[ ";
								for (int k=0; k<inputSignals.length; k++) 
									isstr += inputSignals[k]+" ";
								isstr += "]";
								_mger.msg("GELU<"+_startWeightInd+
					                ">.evalPartialDerivativeB(weights="+wstr+
					                ", index="+index+
						              ", input_signals="+isstr+", lbl="+true_lbl+",p)="+
									        result, 5);
						}
						setLastInputsCache(layer_i_inputs);
						setLastDerivEvalCache(result);
						return result;
					}
					layeri_outputs[j] = node_i_j.evalB(layer_i_inputs, weights, pos);
					pos += layer_i_inputs.length + 1;  // +1 is for the bias
				}
				layer_i_inputs = layeri_outputs;  // set the inputs for next iteration
			}	
			if (output_node==this) {  // our node is the output node
				throw new IllegalStateException("GELU cannot be used as output node");
			}
			throw new IllegalStateException("for weight-index="+index+" cannot find "+
				                              " node corresponding to it");
		}
		// 4. if index is for a weight connecting the previous layer that this node
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
				_mger.msg("GELU<"+_startWeightInd+
				          ">.evalPartialDerivativeB(weights="+wstr+
				          ", index="+index+
				          ", input_signals="+isstr+", lbl="+true_lbl+",p)=0", 5);
			}				
			setLastDerivEvalCache(0.0);
			return 0.0;
		}
		// 5. else index is for a previous signal weight, and derivative is the 
		//    sum of the weights to this node's inputs multiplied by the 
		//    partial derivative of each of the previous layer's nodes outputs 
		//    with respect to the weight represented by index, all this multiplied
		//    by the derivative of the total node_input
		else {
			int layer_of_node = getNodeLayer();
			if (layer_of_node==0) {
				throw new IllegalStateException("node="+this+" index="+index+
					                              " layer_of_node="+layer_of_node);
			}			
			double node_input = Double.NaN;
			double[] last_inputs = getLastInputsCache();
			if (last_inputs != null) {
				final int layerno = getNodeLayer();
				final NNNodeIntf[][] hidden_layers = _ffnn.getHiddenLayers();
				int pos = 0;
				int prev_layer_no = inputSignals.length;
				for (int i=0; i<layerno; i++) {
					pos += hidden_layers[i].length*(prev_layer_no+1);  // +1 for bias
					prev_layer_no = hidden_layers[i].length;
				}
				pos += getPositionInLayer()*(prev_layer_no+1);
				node_input=_linearUnit.evalB(last_inputs,weights,pos);
			}
			else {
				final int num_inputs = inputSignals.length;  // get the #input_signals
				final NNNodeIntf[][] hidden_layers = _ffnn.getHiddenLayers();
				final int num_hidden_layers = hidden_layers.length;
				final OutputNNNodeIntf output_node = _ffnn.getOutputNode();
				// compute from layer-0 to this node
				int pos = 0;  // the position index in the vector weights
				// get inputs for the layer. Inputs are same for all nodes in a layer.
				double[] layer_i_inputs = new double[num_inputs];
				for (int i=0; i<num_inputs; i++) layer_i_inputs[i] = inputSignals[i];
				boolean found = false;
				for (int i=0; i<num_hidden_layers && !found; i++) {
					NNNodeIntf[] layeri = hidden_layers[i];
					double[] layeri_outputs = new double[layeri.length];
					for (int j=0; j<layeri.length; j++) {
						NNNodeIntf node_i_j = layeri[j];
						if (node_i_j == this) {  // this is the node we're looking for
							found = true;
							break;
						}
						layeri_outputs[j] = node_i_j.evalB(layer_i_inputs, weights, pos);
						pos += layer_i_inputs.length + 1;  // +1 is for the bias
					}
					if (!found)
						layer_i_inputs = layeri_outputs;  // set inputs for next iteration
				}
				if (found) {
					setLastInputsCache(layer_i_inputs);
					node_input = _linearUnit.evalB(layer_i_inputs, weights, pos);
				}
				else if (output_node==this) {  
					throw new IllegalStateException("GELU cannot be used as output node");
				}
				else {
					throw new IllegalStateException("couldn't find this node in FFNN");
				}
			}
			double result = 0.0;			
			NNNodeIntf[] prev_layer = _ffnn.getHiddenLayers()[layer_of_node-1];
			for (int j=_startWeightInd; j<=_endWeightInd; j++) {
				int lj = j-_startWeightInd;
				result += weights[j] * 
				          prev_layer[lj].evalPartialDerivativeB(weights, index, 
									                                      inputSignals, 
																												true_lbl,
																											  p);
			}
			result *= gPrime(node_input);
			if (_mger.getDebugLvl()>=5) {
				String wstr="[ ";
				for (int k=0; k<weights.length; k++) wstr += weights[k]+" ";
				wstr += "]";
				String isstr="[ ";
				for (int k=0; k<inputSignals.length; k++) 
					isstr += inputSignals[k]+" ";
				isstr += "]";
				_mger.msg("GELU<"+_startWeightInd+
				          ">.evalPartialDerivativeB(weights="+wstr+
				          ", index="+index+
				          ", input_signals="+isstr+", lbl="+true_lbl+",p)="+
				          result, 5);
			}							
			setLastDerivEvalCache(result);
			return result;
		}
	}

	
	/**
	 * evaluates the partial derivative of this node (as a function of weights)
	 * with respect to the weight variable whose weight is given by the value of 
	 * the weights array in the given index, using the grad vector thread-local
	 * cache. 
	 * The derivative exists everywhere for this "smoothed" version of ReLU.
	 * @param weights double[] all variables (including biases) array
	 * @param index int the index of the partial derivative to take
	 * @param inputSignals double[]
	 * @param true_lbl double
	 * @return double
	 */
	public double evalPartialDerivativeB(double[] weights, int index, 
		                                   double[] inputSignals, double true_lbl) {
		if (isDropout()) {
			setLastDerivEvalCache2(0.0);
			setGradVectorCache(index, 0.0);
			return 0.0;
		}
		// 0. see if the value is already computed before
		double cache = getGradVectorCache()[index];
		if (!Double.isNaN(cache)) return cache;
		// 1. if index is after input weights, derivative is zero
		if (index > _biasInd) {
			final double result = 0.0;
			//setLastDerivEvalCache(result);
			setGradVectorCache(index, result);
			return result;
		}
		// 2. if index is for bias, derivative is gPrime(node_input) 
		else if (index == _biasInd) {
			// check if it's in the cache
			final double result2 = getLastDerivEvalCache2();
			if (!Double.isNaN(result2)) {
				setGradVectorCache(index, result2);
				return result2;
			}
			// compute this node's input-sum
			double[] last_inputs = getLastInputsCache();
			if (last_inputs!=null) {  // last_inputs exists in the cache!
				final int layerno = getNodeLayer();
				final NNNodeIntf[][] hidden_layers = _ffnn.getHiddenLayers();
				int pos = 0;
				int prev_layer_no = inputSignals.length;
				for (int i=0; i<layerno; i++) {
					pos += hidden_layers[i].length*(prev_layer_no+1);  // +1 for bias
					prev_layer_no = hidden_layers[i].length;
				}
				pos += getPositionInLayer()*(prev_layer_no+1);
				double node_input = _linearUnit.evalB(last_inputs,weights,pos);
				final double result = gPrime(node_input);
				//setLastDerivEvalCache(result);
				setLastDerivEvalCache2(result);
				setGradVectorCache(index, result);
				return result;
			}
			// nope, node inputs must be computed from scratch
			double node_input = Double.NaN;
			final int num_inputs = inputSignals.length;  // get the #input_signals
			final NNNodeIntf[][] hidden_layers = _ffnn.getHiddenLayers();
			final int num_hidden_layers = hidden_layers.length;
			final OutputNNNodeIntf output_node = _ffnn.getOutputNode();
			// compute from layer-0 to this node
			int pos = 0;  // the position index in the vector weights
			// get inputs for the layer. Inputs are same for all nodes in a layer.
			double[] layer_i_inputs = new double[num_inputs];
			for (int i=0; i<num_inputs; i++) layer_i_inputs[i] = inputSignals[i];
			boolean found = false;
			for (int i=0; i<num_hidden_layers && !found; i++) {
				NNNodeIntf[] layeri = hidden_layers[i];
				double[] layeri_outputs = new double[layeri.length];
				for (int j=0; j<layeri.length; j++) {
					NNNodeIntf node_i_j = layeri[j];
					if (node_i_j == this) {  // this is the node we're looking for
						found = true;
						break;
					}
					layeri_outputs[j] = node_i_j.evalB(layer_i_inputs, weights, pos);
					pos += layer_i_inputs.length + 1;  // +1 is for the bias
				}
				if (!found)
					layer_i_inputs = layeri_outputs;  // set the inputs for next iteration
			}
			if (found) {
				node_input = _linearUnit.evalB(layer_i_inputs, weights, pos);
			}
			else if (output_node==this) {  // our node is the output node
				throw new IllegalStateException("GELU cannot be used as output node");
			}
			else {
				throw new IllegalStateException("couldn't find this node in FFNN");
			}
			final double result = gPrime(node_input);
			setLastInputsCache(layer_i_inputs);
			//setLastDerivEvalCache(result);
			setLastDerivEvalCache2(result);
			setGradVectorCache(index, result);
			return result;
		}
		// 3. if index is for direct input weights, derivative is the derivative of
		//    the total node input multiplied by the corresponding signal applied to
		//    this weight
		else if (_startWeightInd <= index && index <= _endWeightInd) {
			double[] last_inputs = getLastInputsCache();
			if (last_inputs!=null) {
				final double deval2 = getLastDerivEvalCache2();
				if (!Double.isNaN(deval2)) {
					final int pos = getDirectInputWeightStartIndex();
					final double result = deval2*last_inputs[index-pos];
					setGradVectorCache(index, result);
					return result;
				}
				final int layerno = getNodeLayer();
				final NNNodeIntf[][] hidden_layers = _ffnn.getHiddenLayers();
				/*
				int pos = 0;
				int prev_layer_no = inputSignals.length;
				for (int i=0; i<layerno; i++) {
					pos += hidden_layers[i].length*(prev_layer_no+1);  // +1 for bias
					prev_layer_no = hidden_layers[i].length;
				}
				pos += getPositionInLayer()*(prev_layer_no+1);
				*/
				int pos = getDirectInputWeightStartIndex();
				double node_input=_linearUnit.evalB(last_inputs,weights,pos);
				double dres = gPrime(node_input);
				setLastDerivEvalCache2(dres);
				double result = dres*last_inputs[index-pos];
				//setLastDerivEvalCache(result);
				setGradVectorCache(index, result);
				return result;
			}
			// nope, not in cache, do the work
			final int num_inputs = inputSignals.length;  // get the #input_signals
			final NNNodeIntf[][] hidden_layers = _ffnn.getHiddenLayers();
			final int num_hidden_layers = hidden_layers.length;
			final OutputNNNodeIntf output_node = _ffnn.getOutputNode();
			// compute from layer-0 to this node
			int pos = 0;  // the position index in the vector weights
			// get inputs for the layer. Inputs are same for all nodes in a layer.
			double[] layer_i_inputs = new double[num_inputs];
			for (int i=0; i<num_inputs; i++) 
				layer_i_inputs[i] = inputSignals[i];
			for (int i=0; i<num_hidden_layers; i++) {
				NNNodeIntf[] layeri = hidden_layers[i];
				double[] layeri_outputs = new double[layeri.length];
				for (int j=0; j<layeri.length; j++) {
					NNNodeIntf node_i_j = layeri[j];
					if (node_i_j == this) {  // this is the node we're looking for
						double node_input=_linearUnit.evalB(layer_i_inputs,weights,pos);
						double deval = gPrime(node_input);
						setLastDerivEvalCache2(deval);
						double result = deval*layer_i_inputs[index-pos];
						setLastInputsCache(layer_i_inputs);
						//setLastDerivEvalCache(result);
						setGradVectorCache(index, result);
						return result;
					}
					layeri_outputs[j] = node_i_j.evalB(layer_i_inputs, weights, pos);
					pos += layer_i_inputs.length + 1;  // +1 is for the bias
				}
				layer_i_inputs = layeri_outputs;  // set the inputs for next iteration
			}	
			if (output_node==this) {  // our node is the output node
				throw new IllegalStateException("GELU cannot be used as output node");
			}
			throw new IllegalStateException("for weight-index="+index+" cannot find "+
				                              " node corresponding to it");
		}
		// 4. if index is for a weight connecting the previous layer that this node
		//    belongs to with another node of this layer (but not this node), 
		//    result is zero
		else if (!isWeightVariableAntecedent(index)) {
			//setLastDerivEvalCache(0.0);
			setGradVectorCache(index, 0.0);
			return 0.0;
		}
		// 5. else index is for a previous signal weight, and derivative is the 
		//    sum of the weights to this node's inputs multiplied by the 
		//    partial derivative of each of the previous layer's nodes outputs 
		//    with respect to the weight represented by index, all this multiplied
		//    by the derivative of the total node_input
		else {
			int layer_of_node = getNodeLayer();
			if (layer_of_node==0) {
				throw new IllegalStateException("node="+this+" index="+index+
					                              " layer_of_node="+layer_of_node);
			}
			double deval2 = getLastDerivEvalCache2();
			if (!Double.isNaN(deval2)) {  // no need to look up last_inputs etc.
				double result = 0.0;			
				NNNodeIntf[] prev_layer = _ffnn.getHiddenLayers()[layer_of_node-1];
				for (int j=_startWeightInd; j<=_endWeightInd; j++) {
					int lj = j-_startWeightInd;
					result += weights[j] * 
						        prev_layer[lj].evalPartialDerivativeB(weights, index, 
										                                      inputSignals, 
																													true_lbl);
				}
				result *= deval2;
				setGradVectorCache(index, result);
				return result;
			}
			// nope, do the work
			double node_input = Double.NaN;
			double[] last_inputs = getLastInputsCache();
			if (last_inputs != null) {
				/*
				final int layerno = getNodeLayer();
				final NNNodeIntf[][] hidden_layers = _ffnn.getHiddenLayers();
				int pos = 0;
				int prev_layer_no = inputSignals.length;
				for (int i=0; i<layerno; i++) {
					pos += hidden_layers[i].length*(prev_layer_no+1);  // +1 for bias
					prev_layer_no = hidden_layers[i].length;
				}
				pos += getPositionInLayer()*(prev_layer_no+1);
				*/
				int pos = getDirectInputWeightStartIndex();
				node_input=_linearUnit.evalB(last_inputs,weights,pos);
			}
			else {
				final int num_inputs = inputSignals.length;  // get the #input_signals
				final NNNodeIntf[][] hidden_layers = _ffnn.getHiddenLayers();
				final int num_hidden_layers = hidden_layers.length;
				final OutputNNNodeIntf output_node = _ffnn.getOutputNode();
				// compute from layer-0 to this node
				int pos = 0;  // the position index in the vector weights
				// get inputs for the layer. Inputs are same for all nodes in a layer.
				double[] layer_i_inputs = new double[num_inputs];
				for (int i=0; i<num_inputs; i++) layer_i_inputs[i] = inputSignals[i];
				boolean found = false;
				for (int i=0; i<num_hidden_layers && !found; i++) {
					NNNodeIntf[] layeri = hidden_layers[i];
					double[] layeri_outputs = new double[layeri.length];
					for (int j=0; j<layeri.length; j++) {
						NNNodeIntf node_i_j = layeri[j];
						if (node_i_j == this) {  // this is the node we're looking for
							found = true;
							break;
						}
						layeri_outputs[j] = node_i_j.evalB(layer_i_inputs, weights, pos);
						pos += layer_i_inputs.length + 1;  // +1 is for the bias
					}
					if (!found)
						layer_i_inputs = layeri_outputs;  // set inputs for next iteration
				}
				if (found) {
					setLastInputsCache(layer_i_inputs);
					node_input = _linearUnit.evalB(layer_i_inputs, weights, pos);
				}
				else if (output_node==this) {  
					throw new IllegalStateException("GELU cannot be used as output node");
				}
				else {
					throw new IllegalStateException("couldn't find this node in FFNN");
				}
			}
			double result = 0.0;			
			NNNodeIntf[] prev_layer = _ffnn.getHiddenLayers()[layer_of_node-1];
			for (int j=_startWeightInd; j<=_endWeightInd; j++) {
				int lj = j-_startWeightInd;
				result += weights[j] * 
				          prev_layer[lj].evalPartialDerivativeB(weights, index, 
									                                      inputSignals, 
																												true_lbl);
			}
			deval2 = gPrime(node_input);
			setLastDerivEvalCache2(deval2);
			result *= deval2;
			//setLastDerivEvalCache(result);
			setGradVectorCache(index, result);
			return result;
		}
	}
	
	
	/**
	 * the derivative of this node as a function of the total node input.
	 * @param x double the node input
	 * @return double 
	 */
	private double gPrime(double x) {
		return _norm.cdf(x) + x*_norm.pdf(x);
	}

}

