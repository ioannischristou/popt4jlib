package popt4jlib.neural;

import java.util.HashMap;
import utils.Messenger;


/**
 * Sigmoid implements the 1/(1+exp(-ax)) function as activation function of a 
 * node. The class knows how to differentiate itself when gradient information 
 * is required via the <CODE>evalPartialDerivativeB()</CODE> method that 
 * essentially implements automatic differentiation for FeedForward Neural 
 * Networks. Can only be used as hidden layer node.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Sigmoid extends BaseNNNode implements NNNodeIntf {
	
	private final static Messenger _mger = Messenger.getInstance();
	
	private Linear _linearUnit = new Linear();  // used to compute node input sum
	
	private double _a = 5.0;  // reasonable values for a in (1,10)
	

	/**
	 * sole public constructor.
	 * @param a double should be &gt; 0.
	 */
	public Sigmoid(double a) {
		_a = a;
	}
	
	
	/**
	 * Given arguments two vectors x and y, return 1 / (1 + exp(-a(&lt;x,y&gt;))).
	 * @param inputSignals double[]
	 * @param weights double[]
	 * @return double
	 */
	public double eval(double[] inputSignals, double[] weights) {
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[i];
		return 1.0 / (1.0 + Math.exp(-_a*prod));
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
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[offset+i];
		return 1.0 / (1.0 + Math.exp(-_a*prod));
	}

	
	/**
	 * Given arguments two vectors x and y, return 
	 * 1 / (1 + exp(-a(&lt;x,y[0:x.len-1]&gt;+y[x.len]))). 
	 * @param inputSignals double[]
	 * @param weights double[] includes bias term as last element
	 * @return double
	 */
	public double evalB(double[] inputSignals, double[] weights) {
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[i];
		prod += weights[inputSignals.length];
		double result = 1.0 / (1.0 + Math.exp(-_a*prod));
		// cache result for speeding up auto-differentiation
		setLastInputsCache(inputSignals);
		setLastEvalCache(result);
		return result;
	}

	
	/**
	 * same as <CODE>evalB(s,w)</CODE> method, but the 2nd argument is now assumed
	 * to hold all the network weights including biases.
	 * @param inputSignals double[]
	 * @param weights double[] includes bias terms for each node
	 * @param offset int
	 * @return double
	 */
	public double evalB(double[] inputSignals, double[] weights, int offset) {
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[offset+i];
		prod += weights[offset+inputSignals.length];  // bias term
		double result = 1.0 / (1.0 + Math.exp(-_a*prod));
		// cache result for speeding up auto-differentiation
		setLastInputsCache(inputSignals);
		setLastEvalCache(result);
		return result;
	}

	
	/**
	 * get this node's name.
	 * @return String "Sigmoid(_a)"
	 */
	public String getNodeName() {
		return "Sigmoid("+_a+")";
	}
	

	/**
	 * evaluates the partial derivative of this node (as a function of weights)
	 * with respect to the weight variable whose weight is given by the value of 
	 * the weights array in the given index. THe derivative for this node exists
	 * everywhere (the sigmoid function is an analytic function).
	 * @param weights double[] all variables (including biases) array
	 * @param index int the index of the partial derivative to take
	 * @param inputSignals double[]
	 * @param true_lbl double
	 * @param p HashMap includes the train-data matrix and train-labels array
	 * @return double
	 */
	public double evalPartialDerivativeB(double[] weights, int index, 
		                                   double[] inputSignals, double true_lbl,
																			 HashMap p) {
		// 0. see if the value is already computed before
		double cache = getLastDerivEvalCache();
		if (!Double.isNaN(cache)) {
			return cache;
		}
		// 1. if index is after input weights, derivative is zero
		if (index > _biasInd) {
			final double result = 0.0;
			if (_mger.getDebugLvl()>=2) {
				String wstr="[ ";
				for (int i=0; i<weights.length; i++) wstr += weights[i]+" ";
				wstr += "]";
				String isstr="[ ";
				for (int i=0; i<inputSignals.length; i++) isstr += inputSignals[i]+" ";
				isstr += "]";
				_mger.msg("Sigmoid<"+_startWeightInd+
				 	        ">.evalPartialDerivativeB(weights="+wstr+
					        ", index="+index+
						      ", input_signals="+isstr+", lbl="+true_lbl+",p)="+result, 2);
			}
			setLastDerivEvalCache(result);
			return result;
		}
		// 2. if index is for bias, derivative is sPrime(nodeinput)
		else if (index == _biasInd) {
			// compute this node's input-sum
			double node_input = Double.NaN;
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
				node_input = _linearUnit.evalB(last_inputs,weights,pos);	
			}
			else {  // nope, not in the cache, work from scratch
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
					node_input = _linearUnit.evalB(layer_i_inputs, weights, pos);
				}
				else if (output_node==this) {  // our node is the output node
					node_input = _linearUnit.evalB(layer_i_inputs, weights, pos);
				}
				else {
					throw new IllegalStateException("couldn't find this node in FFNN");
				}
			}
			final double result = sPrime(node_input);
			if (_mger.getDebugLvl()>=2) {
				String wstr="[ ";
				for (int i=0; i<weights.length; i++) wstr += weights[i]+" ";
				wstr += "]";
				String isstr="[ ";
				for (int i=0; i<inputSignals.length; i++) isstr += inputSignals[i]+" ";
				isstr += "]";
				_mger.msg("Sigmoid<"+_startWeightInd+
				 	        ">.evalPartialDerivativeB(weights="+wstr+
					        ", index="+index+
						      ", input_signals="+isstr+", lbl="+true_lbl+",p)="+result, 2);
			}
			setLastDerivEvalCache(result);
			return result;
		}
		// 3. if index is for direct input weights, derivative is the input-signal
		//    applied to this weight multiplied by sPrime(node_input)
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
				double result = sPrime(node_input)*last_inputs[index-pos]; 
				setLastDerivEvalCache(result);
				return result;
			}
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
						double node_input = 
							_linearUnit.evalB(layer_i_inputs, weights, pos);
						double result = sPrime(node_input)*layer_i_inputs[index-pos]; 
						if (_mger.getDebugLvl()>=2) {
							String wstr="[ ";
								for (int k=0; k<weights.length; k++) wstr += weights[k]+" ";
								wstr += "]";
								String isstr="[ ";
								for (int k=0; k<inputSignals.length; k++) 
									isstr += inputSignals[k]+" ";
								isstr += "]";
								_mger.msg("Sigmoid<"+_startWeightInd+
					                ">.evalPartialDerivativeB(weights="+wstr+
					                ", index="+index+
						              ", input_signals="+isstr+", lbl="+true_lbl+",p)="+
									        result, 2);
						}
						setLastInputsCache(layer_i_inputs);  // itc: HERE is this correct?
						setLastDerivEvalCache(result);
						return result;
					}
					layeri_outputs[j] = node_i_j.evalB(layer_i_inputs, weights, pos);
					pos += layer_i_inputs.length + 1;  // +1 is for the bias
				}
				layer_i_inputs = layeri_outputs;  // set the inputs for next iteration
			}	
			if (output_node==this) {  // our node is the output node
  			double node_input = _linearUnit.evalB(layer_i_inputs, weights, pos);
				double result = sPrime(node_input)*layer_i_inputs[index-pos];
				if (_mger.getDebugLvl()>=2) {
					String wstr="[ ";
					for (int k=0; k<weights.length; k++) wstr += weights[k]+" ";
					wstr += "]";
					String isstr="[ ";
					for (int k=0; k<inputSignals.length; k++) 
						isstr += inputSignals[k]+" ";
					isstr += "]";
					_mger.msg("Sigmoid<"+_startWeightInd+
					          ">.evalPartialDerivativeB(weights="+wstr+
					          ", index="+index+
					          ", input_signals="+isstr+", lbl="+true_lbl+",p)="+
					          result, 2);
				}				
				setLastInputsCache(layer_i_inputs);  // itc: HERE is this correct?
				setLastDerivEvalCache(result);
				return result;
			}
			throw new IllegalStateException("for weight-index="+index+" cannot find "+
				                              " node corresponding to it");
		}
		// 4. if index is for a weight connecting the previous layer that this node
		//    belongs to with another node of this layer (but not this node), 
		//    result is zero
		else if (!isWeightVariableAntecedent(index)) {
			if (_mger.getDebugLvl()>=2) {
				String wstr="[ ";
				for (int k=0; k<weights.length; k++) wstr += weights[k]+" ";
				wstr += "]";
				String isstr="[ ";
				for (int k=0; k<inputSignals.length; k++) 
					isstr += inputSignals[k]+" ";
				isstr += "]";
				_mger.msg("Sigmoid<"+_startWeightInd+
				          ">.evalPartialDerivativeB(weights="+wstr+
				          ", index="+index+
				          ", input_signals="+isstr+", lbl="+true_lbl+",p)=0", 2);
			}				
			setLastDerivEvalCache(0.0);
			return 0.0;
		}
		// 5. else index is for a previous signal weight, and derivative is the 
		//    sum of the weights to this node's inputs multiplied by the 
		//    partial derivative of each of the previous layer's nodes outputs 
		//    with respect to the weight represented by index, all this multiplied
		//    by sPrime(node_input)
		else {
			/*
			int layer_of_node = _ffnn.getOutputNode()==this ? 
				                    _ffnn.getNumHiddenLayers() :
				                    getHiddenNodeLayer();
			*/
			int layer_of_node = getNodeLayer();
			if (layer_of_node==0) {
				throw new IllegalStateException("node="+this+" index="+index+
					                              " layer_of_node="+layer_of_node);
			}			
			double node_input = Double.NaN;
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
				node_input = _linearUnit.evalB(last_inputs,weights,pos);	
			}
			else {  // nope, not in cache, must work from scratch
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
					node_input = _linearUnit.evalB(layer_i_inputs, weights, pos);
				}
				else if (output_node==this) {  // our node is the output node
					node_input = _linearUnit.evalB(layer_i_inputs, weights, pos);
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
			result *= sPrime(node_input);
			if (_mger.getDebugLvl()>=2) {
				String wstr="[ ";
				for (int k=0; k<weights.length; k++) wstr += weights[k]+" ";
				wstr += "]";
				String isstr="[ ";
				for (int k=0; k<inputSignals.length; k++) 
					isstr += inputSignals[k]+" ";
				isstr += "]";
				_mger.msg("Sigmoid<"+_startWeightInd+
				          ">.evalPartialDerivativeB(weights="+wstr+
				          ", index="+index+
				          ", input_signals="+isstr+", lbl="+true_lbl+",p)="+
				          result, 2);
			}							
			setLastDerivEvalCache(result);
			return result;
		}
	}

	
	/**
	 * the derivative of the sigmoid function as a function of one variable.
	 * @param x double
	 * @return double
	 */
	private double sPrime(double x) {
		double eax = Math.exp(-_a*x);
		double oneplusexp2 = 1.0 + eax;
		oneplusexp2 *= oneplusexp2;
		return _a*eax / oneplusexp2; 
	}

}

