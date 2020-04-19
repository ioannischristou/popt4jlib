package popt4jlib.neural;


/**
 * ReLU implements the x_{+} function known in ANN literature as RectiLinear
 * Activation Unit.
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

}
