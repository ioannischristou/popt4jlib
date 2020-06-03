package popt4jlib.neural;


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
public class ReLU implements NNNodeIntf {
	
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

}

