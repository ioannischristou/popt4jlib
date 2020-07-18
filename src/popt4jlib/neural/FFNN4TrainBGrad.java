package popt4jlib.neural;

import popt4jlib.VecFunctionIntf;
import popt4jlib.VectorIntf;
import popt4jlib.DblArray1Vector;
import popt4jlib.DblArray1VectorAccess;

import java.util.HashMap;


/**
 * FFNN4TrainBGrad implements the gradient function of an FFNN4TrainB feed
 * forward neural network function. It uses the auto-differentiation 
 * functionality built-into the <CODE>FFNN4TrainB</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FFNN4TrainBGrad implements VecFunctionIntf {
	private FFNN4TrainB _ffnn;
	
	
	/**
	 * public constructor assumes that the network has been fully initialized with
	 * appropriate call to its <CODE>finalizeInitialization(n)</CODE> method 
	 * before invoking this constructor.
	 * @param ffnn FFNN4TrainB
	 */
	public FFNN4TrainBGrad(FFNN4TrainB ffnn) {
		_ffnn = ffnn;
	}
	
	
	/**
	 * public constructor initializes the network by calling the network's 
	 * <CODE>finalizeInitialization(n)</CODE> method with argument the second
	 * argument passed to this constructor.
	 * @param ffnn FFNN4TrainB
	 * @param num_input_signals int the number needed for the initialization of
	 * the network.
	 */
	public FFNN4TrainBGrad(FFNN4TrainB ffnn, int num_input_signals) {
		_ffnn = ffnn;
		_ffnn.finalizeInitialization(num_input_signals);
	}
	
	
	/**
	 * computes the gradient of the feed-forward neural network associated with
	 * this object at the specified weights given by x. The computation of the 
	 * partial derivatives is exact (does not rely on GradApproximator) due to the
	 * auto-differentiation capabilities of the <CODE>FFNN4TrainB</CODE> class.
	 * @param x VectorIntf the weights variables of the FFNN
	 * @param p HashMap the parameters that must contain the traindata and train
	 * labels
	 * @return VectorIntf the gradient of the network at the given weights 
	 */
	public VectorIntf eval(VectorIntf x, HashMap p) {
		DblArray1Vector g = new DblArray1Vector(_ffnn.getTotalNumWeights());
		for (int i=0; i<g.getNumCoords(); i++) {
			final double gi = evalCoord(x, p, i);
			g.setCoord(i, gi);
		}
		return g;
	}
	
	
	/**
	 * computes the partial derivative of the feed-forward neural network (with
	 * biases) on the weights given by x with respect to the coordinate coord.
	 * @param x VectorIntf the weights at which the evaluation of the partial 
	 * derivative takes place
	 * @param p HashMap must contain keys "ffnn.traindata" and "ffnn.trainlabels"
	 * @param coord int must be in {0,...#weights-1}
	 * @return double
	 */
	public double evalCoord(VectorIntf x, HashMap p, int coord) {
		if (x instanceof DblArray1Vector) {  // don't create new double[]
			double[] arr = DblArray1VectorAccess.get_x((DblArray1Vector)x);
			return _ffnn.evalPartialDerivativeB(arr, coord, p);
		}
		return _ffnn.evalPartialDerivativeB(x.getDblArray1(), coord, p);
	}
	
}
