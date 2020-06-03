package popt4jlib.neural;


/**
 * HardThres implements the hard-threshold function in ANNs can be used both as
 * hidden layer node and output node.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class HardThres implements OutputNNNodeIntf {
	
	private double _theta = 0.0;
	
	
	/**
	 * public no-arg, no-op constructor leaves &theta; parameter to zero.
	 */
	public HardThres() {
		// no-op
	}
	
	
	/**
	 * public constructor sets the &theta; parameter of this node.
	 * @param theta double
	 */
	public HardThres(double theta) {
		_theta = theta;
	}
	
	
	/**
	 * Given arguments two vectors x and y, returns 1 if &lt;x,y&gt; &gt; &theta;
	 * and zero otherwise.
	 * @param inputSignals double[]
	 * @param weights double[]
	 * @return double
	 */
	public double eval(double[] inputSignals, double[] weights) {
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[i];
		return Double.compare(prod, _theta) > 0 ? 1 : 0.0;
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
		return Double.compare(prod, _theta) > 0 ? 1 : 0.0;
	}

	
	/**
	 * same as <CODE>eval(s,w)</CODE> method, but the 2nd argument includes as 
	 * last element the bias of the node.
	 * @param inputSignals double[]
	 * @param weights double[] includes bias term as last element
	 * @return double
	 */
	public double evalB(double[] inputSignals, double[] weights) {
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[i];
		prod += weights[weights.length-1];  // bias term
		return Double.compare(prod, _theta) > 0 ? 1 : 0.0;
	}


	/**
	 * same as <CODE>evalB(s,w)</CODE> method, but the 2nd argument is now assumed
	 * to hold all the network weights, and includes extra elements holding the 
	 * bias of each node.
	 * @param inputSignals double[]
	 * @param weights double[] includes nodes' bias terms
	 * @param offset int
	 * @return double
	 */
	public double evalB(double[] inputSignals, double[] weights, int offset) {
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[offset+i];
		prod += weights[offset + inputSignals.length];  // bias term
		return Double.compare(prod, _theta) > 0 ? 1 : 0.0;
	}


	/**
	 * called when the node is used as output node.
	 * @param inputSignals double[]
	 * @param weights double[]
	 * @param offset int
	 * @param true_label double unused
	 * @return double
	 */
	public double eval(double[] inputSignals, double[] weights, int offset, 
		                 double true_label) {
		return eval(inputSignals, weights, offset);
	}
	

	/**
	 * called when the node is used as output node.
	 * @param inputSignals double[]
	 * @param weights double[]
	 * @param offset int
	 * @param true_label double unused
	 * @return double
	 */
	public double evalB(double[] inputSignals, double[] weights, int offset,
		                  double true_label) {
		return evalB(inputSignals, weights, offset);
	}

	
	/**
	 * get this node's name.
	 * @return String "HardThres(_theta)"
	 */
	public String getNodeName() {
		return "HardThres("+_theta+")";
	}

}

