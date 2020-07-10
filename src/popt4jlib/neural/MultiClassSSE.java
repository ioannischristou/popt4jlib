package popt4jlib.neural;


/**
 * class implements the max input signal position selector when acting as an 
 * input training instance evaluator. However, during FFNN training, the output
 * it computes is the sum of the square errors of each of its input signals
 * which must equal the number of classes in the training set. These input 
 * signals must be in the range [0,1] (thus the <CODE>Sigmoid</CODE> or 
 * <CODE>TanH01</CODE> classes are best suited for this last hidden layer).
 * Obviously weights of the last hidden layer towards this output node are not 
 * used. This class should be used together with the 
 * <CODE>popt4jlib.neural.costfunction.L1Norm</CODE> (or alternatively, the
 * <CODE>popt4jlib.neural.costfunction.MAE</CODE>) cost functions, as it already
 * computes as error the sum of square errors of each of the (sigmoid?) nodes in
 * the previous layer.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class MultiClassSSE extends BaseNNNode implements OutputNNNodeIntf {
		
	/**
	 * public no-arg, no-op constructor.
	 */
	public MultiClassSSE() {
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
		if (weights.length!=inputSignals.length) 
			throw new IllegalArgumentException("weights and inputSignals lengths "+
				                                 "don't match");
		double max = Double.NEGATIVE_INFINITY;
		int bind = -1;
		for (int i=0; i<inputSignals.length; i++) {
			if (Double.compare(inputSignals[i], max)>0) {
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
		if (weights.length<offset+inputSignals.length) 
			throw new IllegalArgumentException("weights length is incorrect");
		double max = Double.NEGATIVE_INFINITY;
		int bind = -1;
		for (int i=0; i<inputSignals.length; i++) {
			if (Double.compare(inputSignals[i], max)>0) {
				max = inputSignals[i];
				bind = i;
			}
		}
		return bind;
	}

		
	/**
	 * same as <CODE>eval(s,w)</CODE> method, but the 2nd argument is now assumed
	 * to hold the bias of the node as well (goes unused).
	 * @param inputSignals double[]
	 * @param weights double[] unused; bias also has no role here
	 * @return double  // int in {0,1,...inputSignals.length-1}
	 */
	public double evalB(double[] inputSignals, double[] weights) {
		if (weights.length!=inputSignals.length+1) 
			throw new IllegalArgumentException("(biased)weights and inputSignals "+
				                                 "lengths don't match");
		double max = Double.NEGATIVE_INFINITY;
		int bind = -1;
		for (int i=0; i<inputSignals.length; i++) {
			if (Double.compare(inputSignals[i],max)>0) {
				max = inputSignals[i];
				bind = i;
			}
		}
		return bind;
	}	
	
	
	/**
	 * same as <CODE>evalB(s,w)</CODE> method, but the 2nd argument is now assumed
	 * to hold as last element the bias term for this node (still goes unused).
	 * @param inputSignals double[]
	 * @param weights double[] unused; bias also has no role here
	 * @param offset int unused
	 * @return double  // int in {0,1,...inputSignals.length-1}
	 */
	public double evalB(double[] inputSignals, double[] weights, int offset) {
		if (weights.length<offset+inputSignals.length+1) 
			throw new IllegalArgumentException("(biased)weights length is incorrect");
		double max = Double.NEGATIVE_INFINITY;
		int bind = -1;
		for (int i=0; i<inputSignals.length; i++) {
			if (Double.compare(inputSignals[i],max)>0) {
				max = inputSignals[i];
				bind = i;
			}
		}
		return bind;
	}
	
	
	/**
	 * called when the node is used as output node during training.
	 * @param inputSignals double[]
	 * @param weights double[] unused
	 * @param offset int unused
	 * @param true_label double  // int really
	 * @return double
	 */
	public double eval(double[] inputSignals, double[] weights, int offset, 
		                 double true_label) {
		double sse = 0.0;
		final int true_class = (int) true_label;
		for (int i=0; i<inputSignals.length; i++) {
			double last_layer_out_i = inputSignals[i];
			double expected_i = i == true_class ? 1.0 : 0.0;
			double err_i = last_layer_out_i - expected_i;
			sse += err_i*err_i;
		}
		return sse+true_label;  // add true_label so that the error in the cost
		                        // function becomes just sse
	}
	

	/**
	 * called when the node is used as output node during training.
	 * @param inputSignals double[]
	 * @param weights double[] unused
	 * @param offset int unused
	 * @param true_label double  // int really
	 * @return double
	 */
	public double evalB(double[] inputSignals, double[] weights, int offset,
		                  double true_label) {
		return eval(inputSignals, weights, offset, true_label);
	}
	
	
	/**
	 * get this node's name.
	 * @return String "InputSignalMaxPosSelector"
	 */
	public String getNodeName() {
		return "MultiClassSSE";
	}

}

