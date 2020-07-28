package popt4jlib.neural;

import popt4jlib.VecFunctionIntf;
import popt4jlib.VectorIntf;
import popt4jlib.DblArray1Vector;
import popt4jlib.DblArray1VectorAccess;

import java.util.HashMap;
import java.util.Locale;
import java.text.NumberFormat;


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
		// clear "hiddenws$i$ and "outputws" from p
		HashMap p2 = new HashMap(p);
		final int num_hidden_layers = _ffnn.getNumHiddenLayers();
		for (int i=0; i<num_hidden_layers; i++) p2.remove("hiddenws"+i);
		p2.remove("outputws");
		DblArray1Vector g = new DblArray1Vector(_ffnn.getTotalNumWeights());
		for (int i=0; i<g.getNumCoords(); i++) {
			final double gi = evalCoordNoClearParams(x, p2, i);
			// final double gi = evalCoord(x, p2, i);
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
			return _ffnn.evalPartialDerivativeB(arr, coord, p, true);
		}
		return _ffnn.evalPartialDerivativeB(x.getDblArray1(), coord, p, true);
	}

	
	/**
	 * same as <CODE>evalCoord()</CODE> but does NOT clear the "hiddenws$i$" and
	 * "outputws" of the parameters passed to the 
	 * <CODE>evalPartialDerivativeB()</CODE> method of the FFNN4TrainB object 
	 * associated with this auto-differentiator object.
	 * @param x VectorIntf
	 * @param p HashMap
	 * @param coord int
	 * @return double
	 */
	private double evalCoordNoClearParams(VectorIntf x, HashMap p, int coord) {
		if (x instanceof DblArray1Vector) {  // don't create new double[]
			double[] arr = DblArray1VectorAccess.get_x((DblArray1Vector)x);
			return _ffnn.evalPartialDerivativeB(arr, coord, p, false);
		}
		return _ffnn.evalPartialDerivativeB(x.getDblArray1(), coord, p, false);
	}

	
	/**
	 * return a string representing the vector passed as first argument, according
	 * to the layers and structure of the neural network this gradient evaluator
	 * is for.
	 * @param g VectorIntf
	 * @param p HashMap must contain value for the key "ffnn.traindata".
	 * @return String
	 */
	public String toString(VectorIntf g, HashMap p) {
		NumberFormat df = NumberFormat.getInstance(Locale.US);
		df.setGroupingUsed(false);
		df.setMaximumFractionDigits(8);
		String dstr = "[\n";
		double[][] all_train_data = (double[][]) p.get("ffnn.traindata");
		int input_layer_length = all_train_data[0].length;
		final NNNodeIntf[][] hlayers = _ffnn.getHiddenLayers();
		int pos = 0;
		for (int i=0; i<hlayers.length; i++) {
			dstr += " {\n";
			final int layer_i_nodes = hlayers[i].length;
			for (int j=0; j<layer_i_nodes; j++) {
				dstr += "  <";
				for (int k=0; k<input_layer_length; k++) { 
					String g_pos = df.format(g.getCoord(pos++));
					dstr += g_pos + ",";
				} 
				dstr += " " + df.format(g.getCoord(pos++));
				dstr += ">\n";
			}
			dstr += " }\n";
			input_layer_length = hlayers[i].length;
		}
		// finally, the output node
		dstr += " (";
		for (int k=pos; k<g.getNumCoords()-1; k++) 
			dstr += df.format(g.getCoord(k))+",";
		dstr += " ";
		dstr += df.format(g.getCoord(g.getNumCoords()-1));
		dstr += ")\n]";
		return dstr;
	}
}
