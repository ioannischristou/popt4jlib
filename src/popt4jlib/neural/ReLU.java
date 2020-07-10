package popt4jlib.neural;

import java.util.HashMap;
import utils.Messenger;


/**
 * ReLU implements the x_{+} function known in ANN literature as RectiLinear
 * Activation Unit. Can be used only as hidden layer node.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ReLU extends BaseNNNode implements NNNodeIntf {
	
	/**
	 * public no-arg no-op constructor.
	 */
	public ReLU() {
		// no-op.
	}
	
	
	/**
	 * Given arguments two vectors x and y, returns max(&lt;x,y&gt;,0).
	 * @param inputSignals double[]
	 * @param weights double[]
	 * @return double
	 */
	public double eval(double[] inputSignals, double[] weights) {
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[i];
		return Math.max(prod, 0.0);
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
		return Math.max(prod, 0.0);
	}

	
	/**
	 * Given arguments two vectors x and y, returns 
	 * max(&lt;x,y[0:x.len-1]&gt;+y[y.len-1],0).
	 * @param inputSignals double[]
	 * @param weights double[] includes as last element this node's bias term
	 * @return double
	 */
	public double evalB(double[] inputSignals, double[] weights) {
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[i];
		prod += weights[inputSignals.length];  // bias term
		return Math.max(prod, 0.0);
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
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[offset+i];
		prod += weights[offset+inputSignals.length];  // bias term
		return Math.max(prod, 0.0);
	}
	
	
	/**
	 * get this node's name.
	 * @return String "ReLU"
	 */
	public String getNodeName() {
		return "ReLU";
	}
	
	
	/**
	 * evaluates the partial derivative of this node (as a function of weights)
	 * with respect to the weight variable whose weight is given by the value of 
	 * the weights array in the given index. The derivative does not exist at zero
	 * for this node as the left and right derivative differ; we set it to zero
	 * there as well.
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
		Messenger mger = Messenger.getInstance();
		// 0. see if the value is already computed before
		DerivEvalData data = 
			new DerivEvalData(weights, inputSignals, true_lbl, index);
		final HashMap nodecache = getDerivEvalCache();
		if (nodecache.containsKey(data)) {
			final double result = ((Double) nodecache.get(data)).doubleValue();
			if (nodecache.size()>_MAX_ALLOWED_CACHE_SIZE) {
				removeOldData(_NUM_DATA_2_RM_FROM_CACHE);
			}
			return result;
		}
		// 1. if index is after input weights, derivative is zero
		if (index > _biasInd) {
			final double result = 0.0;
			if (mger.getDebugLvl()>=2) {
				String wstr="[ ";
				for (int i=0; i<weights.length; i++) wstr += weights[i]+" ";
				wstr += "]";
				String isstr="[ ";
				for (int i=0; i<inputSignals.length; i++) isstr += inputSignals[i]+" ";
				isstr += "]";
				mger.msg("ReLU<"+_startWeightInd+
					       ">.evalPartialDerivativeB(weights="+wstr+
					       ", index="+index+
						     ", input_signals="+isstr+", lbl="+true_lbl+",p)="+result, 2);
			}
			nodecache.put(data, new Double(result));
			return result;
		}
		// 2. if index is for bias, derivative is one if node inputs sum positive,
		//    zero else
		else if (index == _biasInd) {
			// compute this node's input-sum
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
				node_input = (new Linear()).evalB(layer_i_inputs, weights, pos);
			}
			else if (output_node==this) {  // our node is the output node
				node_input = (new Linear()).evalB(layer_i_inputs, weights, pos);
			}
			else {
				throw new IllegalStateException("couldn't find this node in FFNN");
			}
			final double result = Double.compare(node_input,0) > 0 ? 1.0 : 0.0;
			if (mger.getDebugLvl()>=2) {
				String wstr="[ ";
				for (int i=0; i<weights.length; i++) wstr += weights[i]+" ";
				wstr += "]";
				String isstr="[ ";
				for (int i=0; i<inputSignals.length; i++) isstr += inputSignals[i]+" ";
				isstr += "]";
				mger.msg("ReLU<"+_startWeightInd+
					       ">.evalPartialDerivativeB(weights="+wstr+
					       ", index="+index+
						     ", input_signals="+isstr+", lbl="+true_lbl+",p)="+result, 2);
			}
			nodecache.put(data, new Double(result));
			return result;
		}
		// 3. if index is for direct input weights, derivative is the input-signal
		//    applied to this weight, as long as the node_input is positive, else
		//    it's zero.
		else if (_startWeightInd <= index && index <= _endWeightInd) {
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
						double node_input=(new Linear()).evalB(layer_i_inputs,weights,pos);
						double result = Double.compare(node_input, 0) > 0 ?
							                layer_i_inputs[index-pos] : 0.0;
						if (mger.getDebugLvl()>=2) {
							String wstr="[ ";
								for (int k=0; k<weights.length; k++) wstr += weights[k]+" ";
								wstr += "]";
								String isstr="[ ";
								for (int k=0; k<inputSignals.length; k++) 
									isstr += inputSignals[k]+" ";
								isstr += "]";
								mger.msg("ReLU<"+_startWeightInd+
					               ">.evalPartialDerivativeB(weights="+wstr+
					               ", index="+index+
						             ", input_signals="+isstr+", lbl="+true_lbl+",p)="+
									       result, 2);
						}
						nodecache.put(data, new Double(result));
						return result;
					}
					layeri_outputs[j] = node_i_j.evalB(layer_i_inputs, weights, pos);
					pos += layer_i_inputs.length + 1;  // +1 is for the bias
				}
				layer_i_inputs = layeri_outputs;  // set the inputs for next iteration
			}	
			if (output_node==this) {  // our node is the output node
  			double node_input = (new Linear()).evalB(layer_i_inputs, weights, pos);
				double result = Double.compare(node_input, 0) > 0 ?
				                  layer_i_inputs[index-pos] : 0.0;
				if (mger.getDebugLvl()>=2) {
					String wstr="[ ";
					for (int k=0; k<weights.length; k++) wstr += weights[k]+" ";
					wstr += "]";
					String isstr="[ ";
					for (int k=0; k<inputSignals.length; k++) 
						isstr += inputSignals[k]+" ";
					isstr += "]";
					mger.msg("ReLU<"+_startWeightInd+
					         ">.evalPartialDerivativeB(weights="+wstr+
					         ", index="+index+
					         ", input_signals="+isstr+", lbl="+true_lbl+",p)="+
					         result, 2);
				}				
				nodecache.put(data, new Double(result));
				return result;
			}
			throw new IllegalStateException("for weight-index="+index+" cannot find "+
				                              " node corresponding to it");
		}
		// 4. if index is for a weight connecting the previous layer that this node
		//    belongs to with another node of this layer (but not this node), 
		//    result is zero
		else if (!isWeightVariableAntecedent(index)) {
			if (mger.getDebugLvl()>=2) {
				String wstr="[ ";
				for (int k=0; k<weights.length; k++) wstr += weights[k]+" ";
				wstr += "]";
				String isstr="[ ";
				for (int k=0; k<inputSignals.length; k++) 
					isstr += inputSignals[k]+" ";
				isstr += "]";
				mger.msg("ReLU<"+_startWeightInd+
				         ">.evalPartialDerivativeB(weights="+wstr+
				         ", index="+index+
				         ", input_signals="+isstr+", lbl="+true_lbl+",p)=0", 2);
			}				
			nodecache.put(data, new Double(0.0));
			return 0.0;
		}
		// 5. else index is for a previous signal weight, and derivative is the 
		//    sum of the weights to this node's inputs multiplied by the 
		//    partial derivative of each of the previous layer's nodes outputs 
		//    with respect to the weight represented by index, as long as the 
		//    node input sum is positive, else the derivative is zero.
		else {
			int layer_of_node = _ffnn.getOutputNode()==this ? 
				                    _ffnn.getNumHiddenLayers() :
				                    getHiddenNodeLayer();
			if (layer_of_node==0) {
				throw new IllegalStateException("node="+this+" index="+index+
					                              " layer_of_node="+layer_of_node);
			}			
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
				node_input = (new Linear()).evalB(layer_i_inputs, weights, pos);
			}
			else if (output_node==this) {  // our node is the output node
				node_input = (new Linear()).evalB(layer_i_inputs, weights, pos);
			}
			else {
				throw new IllegalStateException("couldn't find this node in FFNN");
			}
			double result = 0.0;			
			if (Double.compare(node_input, 0.0) > 0) {
				NNNodeIntf[] prev_layer = _ffnn.getHiddenLayers()[layer_of_node-1];
				for (int j=_startWeightInd; j<=_endWeightInd; j++) {
					int lj = j-_startWeightInd;
					result += weights[j] * 
					          prev_layer[lj].evalPartialDerivativeB(weights, index, 
										                                      inputSignals, 
																													true_lbl,
																												  p);
				}
			}
			if (mger.getDebugLvl()>=2) {
				String wstr="[ ";
				for (int k=0; k<weights.length; k++) wstr += weights[k]+" ";
				wstr += "]";
				String isstr="[ ";
				for (int k=0; k<inputSignals.length; k++) 
					isstr += inputSignals[k]+" ";
				isstr += "]";
				mger.msg("ReLU<"+_startWeightInd+
				         ">.evalPartialDerivativeB(weights="+wstr+
				         ", index="+index+
				         ", input_signals="+isstr+", lbl="+true_lbl+",p)="+
				         result, 2);
			}							
			nodecache.put(data, new Double(result));
			return result;
		}
	}

}

