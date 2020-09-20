package popt4jlib.neural.costfunction;

import popt4jlib.neural.FFNN4TrainB;
import java.util.HashMap;

/**
 * measures the accuracy of classification where as error in a vector component 
 * counts any quantity that is (not NaN and) larger than 1/2 in absolute value.
 * Notice that the 1/2 value threshold comes from the fact that when deciding 
 * the label that corresponds to the output of the network (in categorical 
 * classification), the output is simply rounded to the nearest integer (for 
 * example an output of 4.32 becomes rounded to 4.0; if the true label is 4, the
 * error is 0.32, else it's always (in absolute value) greater than 1/2.
 * Since we are always solving a minimization problem, we actually return 
 * 100 - accuracy.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class CategoricalAccuracy implements FFNNCostFunctionIntf {
	
	private FFNN4TrainB _ffnn = null;
	
	/**
	 * single public no-arg constructor is a no-op.
	 */
	public CategoricalAccuracy() {
		// no-op
	}
	
	
	/**
	 * always throws UnsupportedOperationException.
	 * @param x double
	 * @param num_insts int
	 * @return double
	 * @throws UnsupportedOperationException
	 */
	public double evalDerivative(double x, int num_insts) {
		throw new UnsupportedOperationException("CategoricalAccuracy cannot "+
			                                      "evaluate derivative of FFNN");		
	}
	
	
	/**
	 * computes and returns the number of components that deviate from zero by 
	 * more than 1/2 divided by the total number of non NaN components, multiplied
	 * by 100 (the percentage error which is 100 - accuracy).
	 * @param arg Object  // double[]
	 * @param params HashMap unused
	 * @return double
	 */
	public double eval(Object arg, HashMap params) {
		double[] x = (double[])arg;
		int errors = 0;
		int cnt = 0;
		for (int i=0; i<x.length; i++) {
			if (!Double.isNaN(x[i])) {
				++cnt;
				if (Double.compare(Math.abs(x[i]), 0.5)>0) ++errors;
			}
		}
		return (100.0*errors)/(double)cnt;
	}
	
	
	/**
	 * returns the network this function is set be part of.
	 * @return FFNN4TrainB
	 */
	public FFNN4TrainB getFFNN() {
		return _ffnn;
	}
	
	
	/**
	 * set the network this function is part of.
	 * @param f FFNN4TrainB
	 */
	public void setFFNN(FFNN4TrainB f) {
		_ffnn = f;
	}
	
	
	/**
	 * throws <CODE>UnsupportedOperationException</CODE>, since categorical 
	 * accuracy is not a differentiable function.
	 * @param weights double[] all variables (including biases) array
	 * @param index int the index of the partial derivative to take
	 * @param input_signals double[] the input signals for the network (an 
	 * instance of the training data)
	 * @param true_lbl double the true label corresponding to the input_signals
	 * vector
	 * @param p HashMap 
	 * @return double
	 * @throws UnsupportedOperationException always
	 */	
	public double evalPartialDerivativeB(double[] weights, int index, 
		                                   double[] input_signals, double true_lbl, 
																			 HashMap p) {
		throw new UnsupportedOperationException("CategoricalAccuracy cannot "+
			                                      "evaluate derivative of FFNN");
	}
	
	/**
	 * throws <CODE>UnsupportedOperationException</CODE>, since categorical 
	 * accuracy is not a differentiable function.
	 * @param weights double[] all variables (including biases) array
	 * @param index int the index of the partial derivative to take
	 * @param input_signals double[] the input signals for the network (an
	 * instance of the training data)
	 * @param true_lbl double the true label corresponding to the input_signals
	 * vector
	 * @param num_insts int the number of training instances
	 * @return double 
	 * @throws UnsupportedOperationException always
	 */
	public double evalPartialDerivativeB(double[] weights, int index,
		                                   double[] input_signals, double true_lbl,
	                                     int num_insts){
		throw new UnsupportedOperationException("CategoricalAccuracy cannot "+
			                                      "evaluate derivative of FFNN");		
	}
	
	
	/**
	 * throws <CODE>UnsupportedOperationException</CODE>, since categorical 
	 * accuracy is not a differentiable function.
	 * @param errors double[]
	 * @return double
	 * @throws UnsupportedOperationException always
	 */
	public double evalPartialDerivativeB(double[] errors) {
		throw new UnsupportedOperationException("CategoricalAccuracy cannot "+
			                                      "evaluate derivative of FFNN");		
	}

}

