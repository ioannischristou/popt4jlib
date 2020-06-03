package popt4jlib.neural;

/**
 * common interface for a Neural Network node that can be used as hidden or 
 * output layer node.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface OutputNNNodeIntf extends NNNodeIntf {
	
	/**
	 * same as <CODE>NNNodeIntf.eval(inputs,weights,offset)</CODE> but accepts a
	 * 4th argument holding the real class label (as a number).
	 * @param inputSignals double[]
	 * @param weights double[] array holding all the network weights
	 * @param offset int position where the weights for this node start
	 * @param true_lbl double the real label for the input data described in the 
	 * vector inputSignals
	 * @return double
	 */
	public double eval(double[] inputSignals, double[] weights, int offset, 
		                 double true_lbl);
	
	
	/**
	 * same as <CODE>NNNodeIntf.evalB(inputs,weights,offset)</CODE> but accepts a
	 * 4th argument holding the real class label (as a number).
	 * @param inputSignals double[]
	 * @param weights double[] array holding all the network weights PLUS node
	 * biases
	 * @param offset int position where the weights for this node start
	 * @param true_lbl double the real label for the input data described in the 
	 * vector inputSignals
	 * @return double
	 */
	public double evalB(double[] inputSignals, double[] weights, int offset, 
		                  double true_lbl);
	
}
