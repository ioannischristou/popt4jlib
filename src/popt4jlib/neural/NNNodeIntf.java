package popt4jlib.neural;

import java.util.HashMap;

/**
 * common interface for any Neural Network node (hidden or output).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface NNNodeIntf {
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
}

