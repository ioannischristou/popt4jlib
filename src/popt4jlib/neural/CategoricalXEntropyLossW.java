package popt4jlib.neural;

import utils.Messenger;


/**
 * same as the <CODE>CategoricalXEntropyLoss</CODE> class, but when used as the 
 * output node, divides the value of the input of the node corresponding to the 
 * true label by the sum of the input signals of all its inputs.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class CategoricalXEntropyLossW implements OutputNNNodeIntf {
	
	
	/**
	 * (sole) public constructor is a no-op.
	 */
	public CategoricalXEntropyLossW() {
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
		if (weights.length<offset+inputSignals.length) 
			throw new IllegalArgumentException("weights length is incorrect");
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
			if (inputSignals[i]>max) {
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
			if (inputSignals[i]>max) {
				max = inputSignals[i];
				bind = i;
			}
		}
		return bind;
	}

	
	/**
	 * called when the node is used as output node, and returns the cross-entropy
	 * of the given input data instance, for the network with the given weights
	 * PLUS the true label value.
	 * @param inputSignals double[]
	 * @param weights double[] unused
	 * @param offset int unused
	 * @param true_label double value in {0,...,inputSignals.length-1}
	 * @return double
	 */
	public double eval(double[] inputSignals, double[] weights, int offset, 
		                 double true_label) {
		final int real_lbl = (int) true_label;
		double sum = 0.0;
		for (int i=0; i<inputSignals.length; i++) sum += inputSignals[i];
		if (Double.compare(Math.abs(sum),1.e-18)<0) 
			sum = 1.e-18;  // avoid division by zero
		double arg = inputSignals[real_lbl]/sum;
		// code below to avoid infinities and the like
		if (Double.compare(arg, 0.0)==0) arg = 1.e-300;
		double xent = -Math.log(arg);
		if (Double.isInfinite(xent) || Double.isNaN(xent)) {
			Messenger mger = Messenger.getInstance();
			String data = "CategoricalXEntropyLossW.eval():";
			data += " inputSignals=[ ";
			for (int j=0; j<inputSignals.length; j++) data += inputSignals[j]+" ";
			data += "], weights=[DONT_CARE], offset=DONT_CARE, true_label=";
			data += true_label;
			data += " SUM(inputSignals)="+sum;
			mger.msg(data, 0);
			throw new Error("eval() results in NaN or Infinity");
		}
		return xent + true_label;  // the reason for adding the true label is that
		                           // the cost function works on the error values:
		                           // valt-true_label, and we want the error to be
		                           // exactly xent
	}
	

	/**
	 * called when the node is used as output node is exactly the same as 
	 * <CODE>eval(inputSignals,weights,offset,true_label)</CODE>.
	 * @param inputSignals double[]
	 * @param weights double[] unused
	 * @param offset int unused
	 * @param true_label double value in {0,...inputSignals.length-1}
	 * @return double
	 */
	public double evalB(double[] inputSignals, double[] weights, int offset,
		                  double true_label) {
		return eval(inputSignals, weights, offset, true_label);
	}

	
	/**
	 * get this node's name.
	 * @return String "CategoricalXEntropyLoss"
	 */
	public String getNodeName() {
		return "CategoricalXEntropyLoss";
	}

}

