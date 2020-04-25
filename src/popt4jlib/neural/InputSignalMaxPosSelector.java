package popt4jlib.neural;


/**
 * class implements the arg-max selector function that can be used as output
 * node when the output layer should consist of more than one node. In this case
 * the "real" output layer is the last hidden layer in the network, and this 
 * class simply chooses as value for the network the node index with the largest
 * activation value. Obviously, weights of the last hidden layer towards this
 * output node are not used.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class InputSignalMaxPosSelector implements NNNodeIntf {
	
	
	/**
	 * public no-arg, no-op constructor.
	 */
	public InputSignalMaxPosSelector() {
		// no-op
	}
	
	
	/**
	 * computes and returns the index in inputSignals argument that has the 
	 * largest value.
	 * @param inputSignals double[]
	 * @param weights double[] unused
	 * @return double  // int in {0,1,...inputSignals.length-1}
	 */
	public double eval(double[] inputSignals, double[] weights) {
		double max = Double.NEGATIVE_INFINITY;
		int bind = -1;
		for (int i=0; i<inputSignals.length; i++) {
			if (inputSignals[i]>max) {
				max = inputSignals[i];
				bind = i;
			}
		}
		return bind;
	}

	
	/**
	 * same as <CODE>eval(s,w)</CODE> method, but the 2nd argument is now assumed
	 * to hold all the network weights.
	 * @param inputSignals double[]
	 * @param weights double[] unused
	 * @param offset int unused
	 * @return double  // int in {0,1,...inputSignals.length-1}
	 */
	public double eval(double[] inputSignals, double[] weights, int offset) {
		double max = Double.NEGATIVE_INFINITY;
		int bind = -1;
		for (int i=0; i<inputSignals.length; i++) {
			if (inputSignals[i]>max) {
				max = inputSignals[i];
				bind = i;
			}
		}
		return bind;
	}

}
