package popt4jlib.neural;

import java.util.HashMap;


/**
 * class implements the Categorical Cross-Entropy loss function, that can only
 * be used as the output layer node of multi-class classification problems, 
 * where the previous layer has 1 node for each class and these last hidden 
 * layer nodes output values in [0,1] (for example, Sigmoid or TanH01).
 * Notice that the standard <CODE>FFNN.eval[B]()</CODE> methods that do not 
 * require knowledge of the true label are implemented with the same logic as 
 * that of the <CODE>InputSignalMaxPosSelector</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class CategoricalXEntropyLoss extends BaseNNNode 
                                     implements OutputNNNodeIntf {
	
	
	/**
	 * (sole) public constructor is a no-op.
	 */
	public CategoricalXEntropyLoss() {
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
			if (inputSignals[i]>max) {
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
			if (inputSignals[i]>max) {
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
			if (inputSignals[i]>max) {
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
			if (inputSignals[i]>max) {
				max = inputSignals[i];
				bind = i;
			}
		}
		return bind;
	}

	
	/**
	 * called when the node is used as output node, and returns the cross-entropy
	 * of the given input data instance, for the network with the given weights
	 * PLUS the true label value.
	 * @param inputSignals double[]
	 * @param weights double[] unused
	 * @param offset int unused
	 * @param true_label double value in {0,...,inputSignals.length-1}
	 * @return double
	 */
	public double eval(double[] inputSignals, double[] weights, int offset, 
		                 double true_label) {
		final int real_lbl = (int) true_label;
		// code below to avoid infinities and the like
		double arg = inputSignals[real_lbl];
		if (Double.compare(arg, 0.0)==0) arg = 1.e-300;
		double xent = -Math.log(arg);
		return xent + true_label;  // the reason for adding the true label is that
		                           // the cost function works on the error values:
		                           // valt-true_label, and we want the error to be
		                           // exactly xent
	}
	

	/**
	 * called when the node is used as output node is exactly the same as 
	 * <CODE>eval(inputSignals,weights,offset,true_label)</CODE>.
	 * @param inputSignals double[]
	 * @param weights double[] unused
	 * @param offset int unused
	 * @param true_label double value in {0,...inputSignals.length-1}
	 * @return double
	 */
	public double evalB(double[] inputSignals, double[] weights, int offset,
		                  double true_label) {
		return eval(inputSignals, weights, offset, true_label);
	}

	
	/**
	 * get this node's name.
	 * @return String "CategoricalXEntropyLoss"
	 */
	public String getNodeName() {
		return "CategoricalXEntropyLoss";
	}

	
	/**
	 * evaluates the partial derivative of this node (as a function of weights)
	 * with respect to the weight variable whose weight is given by the value of 
	 * the weights array in the given index.
	 * @param weights double[] all variables (including biases) array
	 * @param index int the index of the partial derivative to take
	 * @param inputSignals double[] the training instance 
	 * @param true_lbl double the training label
	 * @param p HashMap includes the train-data matrix and train-labels array.
	 * @return double
	 */
	public double evalPartialDerivativeB(double[] weights, int index, 
		                                   double[] inputSignals, double true_lbl,
																			 HashMap p) {
		// weights attached directly to this output node are irrelevant
		if (index >= _startWeightInd) return 0.0; 
		// else index is for a previous signal weight, and derivative is 
		// (-1 / this_node_inputSignals[true_lbl]) times the derivative of the 
		// previous layer node corresponding to the true_lbl
		else {
			final int num_inputs = inputSignals.length;  // get the #input_signals
			final NNNodeIntf[][] hidden_layers = _ffnn.getHiddenLayers();
			final int num_hidden_layers = hidden_layers.length;
			// compute from layer-0 to last layer
			int pos = 0;  // the position index in the vector weights
			// get inputs for the layer. Inputs are same for all nodes in a layer.
			double[] last_inputs = getLastInputsCache();
			if (last_inputs==null) {
				double[] layer_i_inputs = new double[num_inputs];
				for (int i=0; i<num_inputs; i++) layer_i_inputs[i] = inputSignals[i];
				for (int i=0; i<num_hidden_layers; i++) {
					NNNodeIntf[] layeri = hidden_layers[i];
					double[] layeri_outputs = new double[layeri.length];
					for (int j=0; j<layeri.length; j++) {
						NNNodeIntf node_i_j = layeri[j];
						layeri_outputs[j] = node_i_j.evalB(layer_i_inputs, weights, pos);
						pos += layer_i_inputs.length+1;  // +1 is for the bias
					}
					layer_i_inputs = layeri_outputs;  // set the inputs for next iteration
				}
				last_inputs = layer_i_inputs;
				setLastInputsCache(last_inputs);
			}
			double result = -1.0 / last_inputs[(int)true_lbl];
			// finally, multiply by the derivative of the true_lbl-th node in the 
			// last layer:
			final int ll = num_hidden_layers-1;
			NNNodeIntf tln = _ffnn.getHiddenLayers()[ll][(int)true_lbl];
			double last_der = 
				tln.evalPartialDerivativeB(weights, index, inputSignals, true_lbl, p);
			result *= last_der;
			return result;
		}
	}

	
	/**
	 * evaluates the partial derivative of this node (as a function of weights)
	 * with respect to the weight variable whose weight is given by the value of 
	 * the weights array in the given index.
	 * @param weights double[] all variables (including biases) array
	 * @param index int the index of the partial derivative to take
	 * @param inputSignals double[] the training instance 
	 * @param true_lbl double the training label
	 * @return double
	 */
	public double evalPartialDerivativeB(double[] weights, int index, 
		                                   double[] inputSignals, double true_lbl) {
		// weights attached directly to this output node are irrelevant
		if (index >= _startWeightInd) return 0.0; 
		// else index is for a previous signal weight, and derivative is 
		// (-1 / this_node_inputSignals[true_lbl]) times the derivative of the 
		// previous layer node corresponding to the true_lbl
		else {
			final NNNodeIntf[][] hidden_layers = _ffnn.getHiddenLayers();
			final int num_hidden_layers = hidden_layers.length;
			double[] layer_i_inputs = getLastInputsCache();
			if (layer_i_inputs==null) {  // do the work
				final int num_inputs = inputSignals.length;  // get the #input_signals
				// compute from layer-0 to last layer
				int pos = 0;  // the position index in the vector weights
				// get inputs for the layer. Inputs are same for all nodes in a layer.
				layer_i_inputs = new double[num_inputs];
				for (int i=0; i<num_inputs; i++) 
					layer_i_inputs[i] = inputSignals[i];
				for (int i=0; i<num_hidden_layers; i++) {
					NNNodeIntf[] layeri = hidden_layers[i];
					double[] layeri_outputs = new double[layeri.length];
					for (int j=0; j<layeri.length; j++) {
						NNNodeIntf node_i_j = layeri[j];
						layeri_outputs[j] = node_i_j.evalB(layer_i_inputs, weights, pos);
						pos += layer_i_inputs.length+1;  // +1 is for the bias
					}
					layer_i_inputs = layeri_outputs;  // set the inputs for next iteration
				}
				setLastInputsCache(layer_i_inputs);
			}
			double result = -1.0 / layer_i_inputs[(int)true_lbl];
			// finally, multiply by the derivative of the true_lbl-th node in the 
			// last layer:
			final int ll = num_hidden_layers-1;
			NNNodeIntf tln = _ffnn.getHiddenLayers()[ll][(int)true_lbl];
			double last_der = 
				tln.evalPartialDerivativeB(weights, index, inputSignals, true_lbl);
			
			result *= last_der;
			return result;
		}
	}

}

