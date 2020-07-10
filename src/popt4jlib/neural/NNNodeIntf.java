package popt4jlib.neural;

import java.io.Serializable;
import java.util.HashMap;

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

	
	/**
	 * set the network this node belongs to.
	 * @param ffnn FFNN4TrainB
	 */
	public void setFFNN4TrainB(FFNN4TrainB ffnn);
	
	
	/**
	 * get the network this node belongs to.
	 * @return FFNN4TrainB
	 */
	public FFNN4TrainB getFFNN4TrainB();

	
	/**
	 * sets the total number of variables (weights plus biases) for the FFNN that
	 * this node participates in.
	 * @param num_weights int
	 */
	public void setTotalNumWeights(int num_weights);
	
	
	/**
	 * sets the range of indices in the weights vector variable that are fed into
	 * this node. These are the weights that are directly input to this node and 
	 * includes the bias variable weight for this node.
	 * @param start int inclusive
	 * @param end int inclusive
	 */
	public void setWeightRange(int start, int end);
	
	
	/**
	 * adds the indices of weight variables that are input to nodes in previous
	 * layers that eventually connect to this one.
	 * @param start int inclusive
	 * @param end int inclusive
	 */
	public void addPreviousWeightsRange(int start, int end);
	
	
	/**
	 * gets the index of the first weight variable connected directly as  
	 * input to this node.
	 * @return int
	 */
	public int getDirectInputWeightStartIndex();

	
	/**
	 * gets the index of the last weight variable connected directly as  
	 * input to this node.
	 * @return int
	 */
	public int getDirectInputWeightEndIndex();
	
	
	/**
	 * returns true if and only if the index represented a connection weight that
	 * connects to a node that is eventually connected to this node.
	 * @param index int
	 * @return boolean
	 */
	public boolean isWeightVariableAntecedent(int index);
	
	
	/**
	 * evaluates the partial derivative of this node (as a function of weights)
	 * with respect to the weight variable whose weight is given by the value of 
	 * the weights array in the given index.
	 * @param weights double[] all variables (including biases) array
	 * @param index int the index of the partial derivative to take
	 * @param input_signals double[] the input signals for the network (an 
	 * instance of the training data)
	 * @param true_lbl double the true label corresponding to the input_signals
	 * vector
	 * @param p HashMap includes the train-data matrix and train-labels array.
	 * @return double
	 */	
	public double evalPartialDerivativeB(double[] weights, int index, 
		                                   double[] input_signals, double true_lbl, 
																			 HashMap p);
	
}

