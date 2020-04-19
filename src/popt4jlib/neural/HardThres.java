package popt4jlib.neural;


/**
 * HardThres implements the hard-threshold function in ANNs.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class HardThres implements NNNodeIntf {
	
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

}
