package popt4jlib.neural;

import popt4jlib.*;
import java.util.HashMap;
import utils.DataMgr;


/**
 * FFNN implements a standard feef-forward ANN, with arbitrary inputs, hidden
 * layers, and a single output node. Any network the class represents can 
 * accept an arbitrary number of input signals, and it is the weights that are
 * passed in the params argument of the <CODE>eval()</CODE> method that 
 * determine the number of the input signals expected by this ANN.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FFNN implements FunctionIntf {
	/**
	 * the ANN is represented as a 2-D matrix of NNNodeIntf objects. Each column
	 * in this matrix represents a hidden layer. The output layer is NOT 
	 * represented in this matrix.
	 */
	private NNNodeIntf[][] _hiddenLayers;
	/**
	 * field stores the single output node of the ANN.
	 */
	private OutputNNNodeIntf _outputNode;
	
	
	/**
	 * single public constructor.
	 * @param hiddenLayers Object[]  // NNNodeIntf[][]
	 * @param output 
	 */
	public FFNN(Object[] hiddenLayers, OutputNNNodeIntf output) {
		_outputNode = output;
		final int num_layers = hiddenLayers.length;
		_hiddenLayers = new NNNodeIntf[num_layers][];
		for (int i=0; i<_hiddenLayers.length; i++) {
			Object[] hli = (Object[]) hiddenLayers[i];
			_hiddenLayers[i] = new NNNodeIntf[hli.length];
			for (int j=0; j<hli.length; j++) 
				_hiddenLayers[i][j] = (NNNodeIntf) hli[j];
		}
	}
	
	
	/**
	 * allow sub-classes access to the <CODE>_hiddenLayers</CODE> field.
	 * @return NNNodeIntf[][]  // _hiddenLayers
	 */
	protected NNNodeIntf[][] getHiddenLayers() {
		return _hiddenLayers;
	}
	
	
	/**
	 * allow access to the <CODE>_outputNode</CODE> field.
	 * @return NNNodeIntf  // _outputNode
	 */
	public OutputNNNodeIntf getOutputNode() {
		return _outputNode;
	}
	
	
	/**
	 * given an array of weights for the entire network (all_weights) and the 
	 * total number of input signals for the network, compute the weights 2-D 
	 * array for a given layer (starting from zero).
	 * @param layerIndex int
	 * @param all_weights double[]
	 * @param numInputSignals int
	 * @return double[][] the weights for the layer
	 */
	public double[][] getLayerWeights(int layerIndex, double[] all_weights,
		                                int numInputSignals) {
		final int num_nodes_in_layer = _hiddenLayers[layerIndex].length;
		double[][] weights = new double[num_nodes_in_layer][];
		int num_signals_4_layer = layerIndex > 0 ? 
			                          _hiddenLayers[layerIndex-1].length :
			                          numInputSignals;
		int start_pos = 0;
		int prev_num_nodes = numInputSignals;
		for (int i=0; i<layerIndex; i++) {
			start_pos += _hiddenLayers[i].length * prev_num_nodes;
			prev_num_nodes = _hiddenLayers[i].length;
		}
		// start_pos now is the index in all_weights where we must start retrieving 
		// values for our result
		for (int i=0; i<num_nodes_in_layer; i++) {
			weights[i] = new double[num_signals_4_layer];
			for (int j=0; j<num_signals_4_layer; j++) 
				weights[i][j] = all_weights[start_pos++];
		}
		return weights;
	}
	
	
	/**
	 * similar to <CODE>getLayerWeights()</CODE> but for the output node.
	 * @param all_weights double[]
	 * @return double[] the weights for the output node
	 */
	public double[] getOutputWeights(double[] all_weights) {
		final int num_signals_4_out = _hiddenLayers[_hiddenLayers.length-1].length;
		double[] weights = new double[num_signals_4_out];
		final int stop_pos = all_weights.length - num_signals_4_out;
		for (int i=all_weights.length-1; i>=stop_pos; --i) {
			weights[weights.length-(all_weights.length-i)] = all_weights[i];
		}
		return weights;		
	}

	
	/**
	 * given an array of weights for the entire network (all_weights) and the 
	 * total number of input signals for the network, compute the weights 2-D 
	 * array for a given layer (starting from zero), where the all_weights array
	 * includes biases for each node in the network.
	 * @param layerIndex int
	 * @param all_weights double[]
	 * @param numInputSignals int
	 * @return double[][] the weights for the layer including biases for nodes
	 */
	public double[][] getLayerWeightsWithBias(int layerIndex, 
		                                        double[] all_weights,
		                                        int numInputSignals) {
		final int num_nodes_in_layer = _hiddenLayers[layerIndex].length;
		double[][] weights = new double[num_nodes_in_layer][];
		int num_signals_4_layer = 
			layerIndex > 0 ? 
			  _hiddenLayers[layerIndex-1].length + 1 :  // +1 is bias term
			  numInputSignals + 1;  // +1 is bias term
		int start_pos = 0;
		int prev_num_nodes = numInputSignals;
		for (int i=0; i<layerIndex; i++) {
			start_pos += _hiddenLayers[i].length * (prev_num_nodes+1);
			prev_num_nodes = _hiddenLayers[i].length;
		}
		// start_pos now is the index in all_weights where we must start retrieving 
		// values for our result
		for (int i=0; i<num_nodes_in_layer; i++) {
			weights[i] = new double[num_signals_4_layer];
			for (int j=0; j<num_signals_4_layer; j++) 
				weights[i][j] = all_weights[start_pos++];
		}
		return weights;
	}
	
	
	/**
	 * similar to <CODE>getLayerWeightsWithBias()</CODE> but for the output node.
	 * @param all_weights double[]
	 * @return double[] the weights for the output node
	 */
	public double[] getOutputWeightsWithBias(double[] all_weights) {
		final int num_signals_4_out = 
			_hiddenLayers[_hiddenLayers.length-1].length+1;  // +1 is bias term
		double[] weights = new double[num_signals_4_out];
		final int stop_pos = all_weights.length - num_signals_4_out;
		for (int i=all_weights.length-1; i>=stop_pos; --i) {
			weights[weights.length-(all_weights.length-i)] = all_weights[i];
		}
		return weights;		
	}

	
	/**
	 * get the number of hidden layers.
	 * @return int
	 */
	public int getNumHiddenLayers() {
		return _hiddenLayers.length;
	}
	
	
	/**
	 * inputs are input signals. Weights are stored in the params map as 
	 * <CODE>double[][]</CODE> matrices for each of the hidden layers, with 
	 * corresponding key name "hiddenws$i$" where i starts from 0. Each matrix 
	 * has as many rows as there are nodes in the corresponding layer, and its
	 * columns are as many as there are nodes in the previous layer!
	 * The output node weights are stored as a <CODE>double[]</CODE> with key 
	 * "outputws".
	 * @param inputs Object // double[]
	 * @param params HashMap  // may include boolean value for key "includeBiases"
	 * @return double
	 */
	public double eval(Object inputs, HashMap params) {
		double[] x = (double[]) inputs;
		final boolean biases = 
			params.containsKey("includeBiases") ?
			  ((Boolean) params.get("includeBiases")).booleanValue() :
			  false;
		final int numLayers = _hiddenLayers.length;
		double[][][] weights = new double[numLayers][][];
		for (int i=0; i<numLayers; i++) {
			weights[i] = (double[][]) params.get("hiddenws"+i);
		}
		double[] outputws = (double[]) params.get("outputws");
		// now do the computation, from input layer to the final hidden layer
		double[] layer_outputs=null;
		for (int i=0; i<numLayers; i++) {
			layer_outputs = new double[weights[i].length];
			for (int j=0; j<layer_outputs.length; j++) {
				NNNodeIntf nodeij = _hiddenLayers[i][j];
				layer_outputs[j] = biases==false ? 
					                   nodeij.eval(x, weights[i][j]) :
					                   nodeij.evalB(x,weights[i][j]);              
			}
			x = layer_outputs;
		}
		// finally, compute the output layer output!
		double ret = biases==false ? 
			             _outputNode.eval(layer_outputs, outputws) :
			             _outputNode.evalB(layer_outputs, outputws);
		return ret;
	}
	
	
	/**
	 * test the output of the FFNN function. Invoke as:
	 * <CODE>
	 * java -cp &lt;classpath&gt; popt4jlib.neural.FFNN &lt;props_file&gt;
	 * </CODE>
	 * It first "reads" an FFNN from appropriate props, and then applies some 
	 * input feature vectors to it to test the final output values.
	 * 
	 * An example of the contents of a valid test file is the following:
	 * <PRE>
	 * # the following defines an ANN and weights on the nn's connections
	 * # the ANN looks like this:
	 * # INP L0   L1   OUT
	 * # x1
	 * # x2  N00  N10  y
	 * # x3  N01  N11
	 * # x4       N12
	 * class,n00,popt4jlib.neural.ReLU
	 * class,n01,popt4jlib.neural.ReLU
	 * class,n10,popt4jlib.neural.ReLU
	 * class,n11,popt4jlib.neural.ReLU
	 * class,n12,popt4jlib.neural.ReLU
	 * class,outputlayer,popt4jlib.neural.HardThres,0.0
	 * array,layer1_arr,popt4jlib.neural.NNNodeIntf,n00,n01
	 * array,layer2_arr,popt4jlib.neural.NNNodeIntf,n10,n11,n12
	 * array,hiddenlayers,[Ljava.lang.Object;,layer1_arr,layer2_arr
	 * matrix,hiddenws0,/Users/itc/projects/popt4jlib/testdata/hiddenws0.dat
	 * # contents of hiddenws0.dat are:
	 * # 2 4
	 * # 1,1 2,1 3,0 4,0
	 * # 1,0 2,0 3,1 4,1
	 * matrix,hiddenws1,/Users/itc/projects/popt4jlib/testdata/hiddenws1.dat
	 * # contents of hiddenws1.dat are:
	 * # 3 2 
	 * # 1,11 2,12
	 * # 1,13 2,14
	 * # 1,15 2,16
	 * array,outputws,double,31.0,32.0,33.0
	 * </PRE>
	 * 
	 * @param args String[] requires a single parameter, the name of the params
	 * file defining the network and the inputs.
	 */
	public static void main(String[] args) {
		try {
			String paramsfile = args[0];
			HashMap props = DataMgr.readPropsFromFile(paramsfile);
			Object[] hidden_layers = (Object[]) props.get("hiddenlayers");
			// figure out num_features from hiddenws0 matrix
			double[][] hiddenws_mat = (double[][]) props.get("hiddenws0");
			final int num_features = hiddenws_mat.length;
			OutputNNNodeIntf output_layer = 
				(OutputNNNodeIntf) props.get("outputlayer");
			FFNN net = new FFNN(hidden_layers, output_layer);
			double[] inputs = new double[num_features];
			// let inputs stay at zero first
			double out = net.eval(inputs, props);
			System.out.println("net evaluates to "+out);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
}
