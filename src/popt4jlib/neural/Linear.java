package popt4jlib.neural;


/**
 * Linear implements the linear activation function, and can be used as hidden
 * layer node or as output node.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Linear implements OutputNNNodeIntf {
	
	/**
	 * public no-arg no-op constructor.
	 */
	public Linear() {
		// no-op.
	}
	
	
	/**
	 * Given arguments two vectors x and y, returns the inner product &lt;x,y&gt;.
	 * @param inputSignals double[]
	 * @param weights double[]
	 * @return double
	 */
	public double eval(double[] inputSignals, double[] weights) {
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[i];
		return prod;
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
		return prod;
	}

	
	/**
	 * Given arguments two vectors x and y, returns 
	 * &lt;x,y[0:x.len-1]&gt;+y[y.len-1].
	 * @param inputSignals double[]
	 * @param weights double[] includes as last element the bias term
	 * @return double
	 */
	public double evalB(double[] inputSignals, double[] weights) {
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[i];
		prod += weights[weights.length-1];  // bias term
		return prod;
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
		return prod;
	}

	
	/**
	 * called when the node is used as output node, simply calls
	 * <CODE>eval(inputSignals,weights,offset)</CODE>.
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
	 * called when the node is used as output node, simply calls
	 * <CODE>evalB(inputSignals,weights,offset)</CODE>.
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
	 * @return String "Linear"
	 */
	public String getNodeName() {
		return "Linear";
	}

}

