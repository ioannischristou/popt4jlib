package popt4jlib.neural;


/**
 * TanH01 implements the tanh function, scaled and shifted so as to produce 
 * values in [0,1] as activation function of a node. Can only be used as hidden
 * layer node.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class TanH01 extends BaseNNNode implements NNNodeIntf {
	
	/**
	 * public no-arg no-op constructor.
	 */
	public TanH01() {
		// no-op.
	}
	
	
	/**
	 * Given arguments two vectors x and y, returns (tanh(&lt;x,y&gt;)+1)/2.
	 * @param inputSignals double[]
	 * @param weights double[]
	 * @return double
	 */
	public double eval(double[] inputSignals, double[] weights) {
		if (isDropout()) return 0.0;
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[i];
		return (Math.tanh(prod)+1)/2;
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
		return (Math.tanh(prod)+1)/2;
	}


	/**
	 * Given arguments two vectors x and y, returns 
	 * (tanh(&lt;x,y[0:x.len-1]&gt;+y[y.len-1])+1)/2.
	 * @param inputSignals double[]
	 * @param weights double[] includes a bias term
	 * @return double
	 */
	public double evalB(double[] inputSignals, double[] weights) {
		if (isDropout()) return 0.0;
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[i];
		prod += weights[inputSignals.length];  // bias term
		return (Math.tanh(prod)+1)/2;
	}

	
	/**
	 * same as <CODE>evalB(s,w)</CODE> method, but the 2nd argument is now assumed
	 * to hold all the network weights including node biases
	 * @param inputSignals double[]
	 * @param weights double[] includes nodes' biases
	 * @param offset int
	 * @return double
	 */
	public double evalB(double[] inputSignals, double[] weights, int offset) {
		if (isDropout()) return 0.0;
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[offset+i];
		prod += weights[offset+inputSignals.length];  // bias term
		return (Math.tanh(prod)+1)/2;
	}

	
	/**
	 * get this node's name.
	 * @return String "TanH01"
	 */
	public String getNodeName() {
		return "TanH01";
	}

}

