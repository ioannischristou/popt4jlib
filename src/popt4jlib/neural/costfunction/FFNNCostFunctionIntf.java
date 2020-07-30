package popt4jlib.neural.costfunction;

import popt4jlib.FunctionIntf;
import popt4jlib.neural.FFNN4TrainB;
import java.util.HashMap;


/**
 * all FFNN cost functions must implement this interface so that their gradient
 * can be "auto"-computed.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface FFNNCostFunctionIntf extends FunctionIntf {

	/**
	 * evaluates the partial derivative of the entire network's cost function (as 
	 * a function of its weights) with respect to the weight variable whose weight 
	 * is given by the value of the weights array in the given index.
	 * @param weights double[] all variables (including biases) array
	 * @param index int the index of the partial derivative to take
	 * @param input_signals double[] the input signals for the network (an 
	 * instance of the training data)
	 * @param true_lbl double the true label corresponding to the input_signals
	 * vector
	 * @param p HashMap includes the train-data matrix and train-labels array.
	 * @return double
	 */	
	public double evalPartialDerivativeB(double[] weights, int index, 
		                                   double[] input_signals, double true_lbl, 
																			 HashMap p);
	
	
	/**
	 * same as 
	 * <CODE>
	 * evalPartialDerivativeB(weights, index, input_signals, true_lbl, p)
	 * </CODE>
	 * but uses the grad-vector caches instead of the last deriv cache.
	 * @param weights double[] all variables (including biases) array
	 * @param index int the index of the partial derivative to take
	 * @param input_signals double[] the input signals for the network (an
	 * instance of the training data)
	 * @param true_lbl double the true label corresponding to the input_signals
	 * vector
	 * @return double 
	 */
	public double evalPartialDerivativeB(double[] weights, int index,
		                                   double[] input_signals, double true_lbl);
	
	
	/**
	 * computes the partial derivative of this function on an array of values that
	 * is the derivative of this function on a set of training instances and 
	 * labels.
	 * @param d_per_instance double[] must have been computed from calls to 
	 * <CODE>evalPartialDerivativeB(wgts,ind,train_inst,train_lbl,p)</CODE>.
	 * @return double
	 */	
	public double evalPartialDerivativeB(double[] d_per_instance);

	
	/**
	 * get the ffnn to which this cost function is a member of.
	 * @return FFNN4TrainB
	 */
	public FFNN4TrainB getFFNN();
	
	
	/**
	 * sets the ffnn in which this cost function is a member of.
	 * @param ffnn FFNN4TrainB
	 */
	public void setFFNN(FFNN4TrainB ffnn);
}
