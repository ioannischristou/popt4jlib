package popt4jlib.neural;


/**
 * Sigmoid implements the 1/(1+exp(-ax)) function as activation function of a 
 * node.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Sigmoid implements NNNodeIntf {
	
	private double _a = 5.0;  // reasonable values for a in (1,10)
	

	/**
	 * sole public constructor.
	 * @param a double
	 */
	public Sigmoid(double a) {
		_a = a;
	}
	
	
	/**
	 * Given arguments two vectors x and y, return 1 / (1 + exp(-a(&lt;x,y&gt;))).
	 * @param inputSignals double[]
	 * @param weights double[]
	 * @return double
	 */
	public double eval(double[] inputSignals, double[] weights) {
		double prod = 0.0;
		for (int i=0; i<inputSignals.length; i++)
			prod += inputSignals[i]*weights[i];
		return 1.0 / (1.0 + Math.exp(-_a*prod));
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
		return 1.0 / (1.0 + Math.exp(-_a*prod));
	}

}
