package popt4jlib.neural;

import java.io.Serializable;

/**
 * common interface for a Neural Network node at any hidden layer.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface NNNodeIntf extends Serializable {
	/**
	 * method assumes that the input weights are available as a 1-D array of the 
	 * same dimension as the input signals to the node.
	 * @param inputSignals double[]
	 * @param inputWeights double[]
	 * @return double
	 */
	public double eval(double[] inputSignals, double[] inputWeights);
	
	
	/**
	 * method assumes that the weights are stored in a single, large 1-D array,
	 * and therefore, the third argument provides the index in the array at which
	 * the weights for this particular node are stored.
	 * @param inputSignals double[]
	 * @param inputWeights double[] assumed to contain all weights in a network
	 * @param inputWeightsStartOffset int the index in the inputWeights that the
	 * weights for this node are stored.
	 * @return double
	 */
	public double eval(double[] inputSignals, double[] inputWeights, 
		                 int inputWeightsStartOffset);

	
	/**
	 * method assumes that connection weights as well as node biases are all 
	 * stored in a single, large 1-D array, and therefore, the third argument 
	 * provides the index in the array at which the weights for this particular 
	 * node (connection weights plus node bias immediately after them) are stored.
	 * @param inputSignals double[]
	 * @param inputWeights double[] assumed to contain all weights in a network 
	 * including the biases for each node
	 * @param inputWeightsStartOffset int the index in the inputWeights that the
	 * weights for this node are stored. Right after the weights the bias variable
	 * for the node follows
	 * @return double
	 */
	public double evalB(double[] inputSignals, double[] inputWeights, 
		                 int inputWeightsStartOffset);
	
	
	/**
	 * works when biases are included.
	 * @param inputSignals double[]
	 * @param inputWgts double[]
	 * @return double
	 */
	public double evalB(double[] inputSignals, double[] inputWgts);
	
	
	/**
	 * returns this node's name (usually just the type of the node, eg "ReLU").
	 * @return String
	 */
	public String getNodeName();
	
}

