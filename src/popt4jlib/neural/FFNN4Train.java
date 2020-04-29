package popt4jlib.neural;

import popt4jlib.*;
import popt4jlib.neural.costfunction.L2Norm;
import parallel.TaskObject;
import parallel.distributed.PDBatchTaskExecutor;
import utils.DataMgr;
import utils.RndUtil;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Random;
import java.io.Serializable;


/**
 * FFNN4Train extends FFNN that implements a standard feef-forward ANN, with 
 * arbitrary inputs, hidden layers, and a single output node. The difference 
 * lies in that this class's <CODE>eval()</CODE> method takes as input the 
 * actual object of training, ie the weights of the ANN, and returns the error
 * function value (measured as the return value of the cost function passed in 
 * the constructor as extra 3rd argument) of the ANN on a -labeled- dataset 
 * passed in the params map of the <CODE>eval()</CODE> method.
 * Notice: this is a serial implementation by default, but also allows for 
 * shared-memory parallel evaluation of a weighted NN (training vectors are 
 * evaluated in parallel) if in the <CODE>eval()</CODE> method the params 
 * object passed in contains a key-value pair of the form
 * &lt;"ffnn.pdbtexecutor",parallel.distributed.PDBatchTaskExecutor&gt;.
 * Alternatively, if the 4-arg constructor was used to create an object of this
 * class and the 4th argument is greater than 1, then again, a shared-memory
 * parallel evaluation of the function on the training inputs will occur with as
 * many threads as the 3rd constructor argument specified.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FFNN4Train extends FFNN {
	
	private final static int _numInputsPerTask = 32;  // used in shared-memory
	                                                  // parallel evaluation

	private FunctionIntf _costFunc = null;

	private PDBatchTaskExecutor _extor = null;  // set by the 4-arg constructor
	private static volatile int _numObjs4 = 0;  // used to check 4 FFNN4Train objs
	
	
	/**
	 * 3-arg public constructor for serial training set evaluation (unless the 
	 * "right" params are passed to the <CODE>eval()</CODE> method.)
	 * @param hiddenlayers Object[]  // NodeIntf[][]
	 * @param outputnode NNNodeIntf
	 * @param f FunctionIntf the cost (error) function to measure the "cost" of 
	 * the network as a function of its errors on the training set; if null, the 
	 * L2 norm is used by default (essentially corresponding to a regression 
	 * problem).
	 */
	public FFNN4Train(Object[] hiddenlayers, NNNodeIntf outputnode, 
		                FunctionIntf f) {
		super(hiddenlayers, outputnode);
		if (f==null) f = new L2Norm();
		_costFunc = f;
		// ++_numObjs;  // deliberately not counting such objects
	}

	
	/**
	 * 4-arg public constructor allows for shared-memory parallel training set 
	 * evaluation.
	 * @param hiddenlayers Object[]  // NodeIntf[][]
	 * @param outputnode NNNodeIntf
	 * @param f FunctionIntf the cost (error) function to measure the "cost" of 
	 * the network as a function of its errors on the training set; if null, the 
	 * L2 norm is used by default (essentially corresponding to a regression 
	 * problem).
	 * @param numthreads int if &gt; 1, parallel training set evaluation occurs.
	 */
	public FFNN4Train(Object[] hiddenlayers, NNNodeIntf outputnode, 
		                FunctionIntf f, int numthreads) {
		super(hiddenlayers, outputnode);
		if (f==null) f = new L2Norm();
		_costFunc = f;
		if (++_numObjs4 > 1) {
			System.err.println("2nd FFNN4Train being constructed via 4-arg ctor???");
			System.exit(-1);
		}
		if (numthreads>1) {
			try {
				_extor = PDBatchTaskExecutor.newPDBatchTaskExecutor(numthreads);
			}
			catch (Exception e) {  // cannot happen
				e.printStackTrace();  // ignore
				_extor = null;
			}
		}
	}
	
	
	/**
	 * allows clients to call the base-class <CODE>eval()</CODE> method.
	 * @param arg Object  // double[] inputSignal
	 * @param params HashMap  // must contain &lt;"hiddenws$i$", double[][]&gt;
	 * as well as &lt;"outputws",double[]&gt; pairs.
	 * @return double
	 */
	public double evalNetworkOnInputData(Object arg, HashMap params) {
		return super.eval(arg, params);
	}
	
	
	/**
	 * evaluates this feed-forward neural network with single output on a given
	 * training dataset, with given training labels, according to the cost 
	 * function held in data member <CODE>_costFunction</CODE> where the cost 
	 * function accepts as inputs the errors of the network on the training data.
	 * The network can also be trained with random mini-batches if the key-value
	 * pair &lt;"ffnn.randombatchsize",$num$&gt; exists in the parameters hashmap
	 * passed in as second argument to the method call; in such a case, only a 
	 * random sample from the training set of the given size will be used to
	 * evaluate the network, which of course will significantly speed-up training.
	 * On the other hand, this will also make the call to this function with the
	 * same arguments non-deterministic as same weights and same parameters will
	 * be evaluated on a different training sample. Further, when only a strict
	 * subset of the entire training set is evaluated, the resulting errors array
	 * although it has length equal to the entire training set size, it will 
	 * contain zeros for those instances that were not evaluated, and in the case
	 * of parallel execution, the errors will not in general correspond to the 
	 * positions in the training set that they were created. This should not 
	 * create any problem for cost function evaluation, because no cost function 
	 * cares about where an error has occurred (see the classes in package 
	 * <CODE>popt4jlib.neural.costfunction</CODE>).
	 * @param x Object  // double[] the weights of the network:
	 * The weights are stored consecutively, starting with the weights of the 
	 * first node of the first layer, then continuing with the weights of the 
	 * second node of the first layer, ... until finally we get the weights of the
	 * connections to the output node. The FFNN class network architecture is that
	 * all nodes in one layer connect to all nodes in the next layer and only 
	 * those. Notice that x may be instead a <CODE>DblArray1Vector</CODE> object,
	 * in which case we obtain access to the underlying <CODE>double[]</CODE> via
	 * the <CODE>popt4jlib.DblArray1VectorAccess.get_x()</CODE> static method.
	 * @param params HashMap must contain the dataset as well as the labels for
	 * each input vector. The dataset is maintained as a 2-D double array
	 * <CODE>double[][]</CODE> with key "ffnn.traindata" where each row represents 
	 * one input vector, together with the expected outputs maintained in a 1-D 
	 * double array <CODE>double[]</CODE> stored for key "ffnn.trainlabels".  
	 * @return double
	 */
	public double eval(Object x, HashMap params) {
		double result = 0.0;
		// w is the weights of the network connections
		final double[] w = (x instanceof double[]) ?   
			                   (double[]) x :
			                   DblArray1VectorAccess.get_x((DblArray1Vector) x);
		final double[][] train_vectors = (double[][]) params.get("ffnn.traindata");
		final double[] train_labels = (double[]) params.get("ffnn.trainlabels");
		final int batchsize = 
			params.containsKey("ffnn.randombatchsize") ?
				((Integer)params.get("ffnn.randombatchsize")).intValue() : -1;
		final double prob_enter = batchsize > 0 ? 
				                        ((double) batchsize)/train_vectors.length : 1.0;
		final int num_inputs = train_vectors[0].length;  // get the #input_signals
		final NNNodeIntf[][] hidden_layers = getHiddenLayers();
		final int num_hidden_layers = hidden_layers.length;
		final NNNodeIntf output_node = getOutputNode();
		double[] errors = new double[train_vectors.length];
			
		Integer tidI = (Integer) params.get("thread.id");
		Random rnd = null;
		if (tidI!=null) rnd = RndUtil.getInstance(tidI.intValue()).getRandom();
		else rnd = RndUtil.getInstance().getRandom();
			
		// check if there is a PDBatchTaskExecutor to use
		PDBatchTaskExecutor extor = 
			(PDBatchTaskExecutor) params.get("ffnn.pdbtexecutor");
		if (extor==null) {  // see if we have built-in parallel!
			extor = _extor;
		}
		if (extor!=null) {  // create task-objects to do the work for each t
			List tasks = 
				new ArrayList((int) Math.ceil(((double)train_vectors.length) /
					                            _numInputsPerTask));  
      // List<FFNNMultiEvalTask>
			int cnt = 0;
			double[][] input_vectors = new double[_numInputsPerTask][];
			double[] input_labels = new double[_numInputsPerTask];
			for (int t=0; t<train_vectors.length; t++) {
				// enter with certain probability
				if (rnd.nextDouble()>prob_enter) {  // this train-inst doesn't get in
					continue;
				} 
				input_vectors[cnt] = train_vectors[t];
				input_labels[cnt++] = train_labels[t];
				if (cnt==_numInputsPerTask) {  // reached limit, create task and add to
					                             // tasks
					FFNNMultiEvalTask met = 
						new FFNNMultiEvalTask(input_vectors, input_labels, w,
							                    hidden_layers, output_node);
					tasks.add(met);
					cnt=0;
					input_vectors = new double[_numInputsPerTask][];
					input_labels = new double[_numInputsPerTask];
				}
			}
			if (cnt > 0) {  // there is a remainder
				FFNNMultiEvalTask met = 
					new FFNNMultiEvalTask(input_vectors, input_labels, w, 
						                    hidden_layers, output_node);
				tasks.add(met);
			}
			try {
				Vector err_res = extor.executeBatch(tasks);
				// every element of the err_res vector is a double[]
				int pos = 0;
				for (int i=0; i<err_res.size(); i++) {
					double[] erri = (double[]) err_res.get(i);
					for (int j=0; j<erri.length; j++) errors[pos++] = erri[j];
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			// finally, apply the cost function on the errors array, adding to the 
			// params the current weights as "ffnn.weights".
			HashMap params2 = new HashMap(params);
			params2.put("ffnn.weights", x);
			result = _costFunc.eval(errors, params2);
			return result;			
		}
		// nope, no parallel executor, do things serially.
		for (int t=0; t<train_vectors.length; t++) {
			// enter with certain probability
			if (rnd.nextDouble()>prob_enter) {  // don't evaluate this training inst.
				continue;
			} 			
			double[] inputs_t = train_vectors[t];
			// compute from layer-0 to final hidden layer the node activations
			int pos = 0;  // the position index in the vector w
			// get the inputs for the layer. Inputs are same for all nodes in a layer.
			double[] layer_i_inputs = new double[num_inputs];
			for (int i=0; i<num_inputs; i++) 
				layer_i_inputs[i] = inputs_t[i];  // must NOT modify inputs_t
			for (int i=0; i<num_hidden_layers; i++) {
				NNNodeIntf[] layeri = hidden_layers[i];
				double[] layeri_outputs = new double[layeri.length];
				for (int j=0; j<layeri.length; j++) {
					NNNodeIntf node_i_j = layeri[j];
					layeri_outputs[j] = node_i_j.eval(layer_i_inputs, w, pos);
					pos += layer_i_inputs.length;
				}
				layer_i_inputs = layeri_outputs;  // set the inputs for next iteration
			}
			double valt = output_node.eval(layer_i_inputs, w, pos);
			errors[t] = (valt-train_labels[t]);
		}
		// finally, apply the cost function on the errors array, adding to the 
		// params the current weights as "ffnn.weights".
		HashMap params2 = new HashMap(params);
		params2.put("ffnn.weights", x);
		result = _costFunc.eval(errors, params2);
		return result;
	}
	
	
	/**
	 * invoke as:
	 * <CODE>
	 * java -cp &lt;classpath&gt; popt4jlib.neural.FFNN4Train 
	 * &lt;params_file&gt; [num_threads(1)]
	 * </CODE>.
	 * <PRE>
	 * The params file is a file that should contain at least the "hiddenlayers"
	 * "outputlayer", "ffnn.traindata" and "ffnn.trainlabels" params; for example:
	 * # the following defines an ANN and weights on the nn's connections
	 * # the ANN looks like this:
	 * # INP L0   L1   OUT
	 * # x1
	 * # x2  N00  N10  y
	 * # x3  N01  N11
	 * # x4       N12
	 * class,n00,popt4jlib.neural.ReLU
	 * class,n01,popt4jlib.neural.ReLU
	 * class,n10,popt4jlib.neural.ReLU
	 * class,n11,popt4jlib.neural.ReLU
	 * class,n12,popt4jlib.neural.ReLU
	 * class,outputlayer,popt4jlib.neural.HardThres,0.0
	 * array,layer1_arr,popt4jlib.neural.NNNodeIntf,n00,n01
	 * array,layer2_arr,popt4jlib.neural.NNNodeIntf,n10,n11,n12
	 * array,hiddenlayers,[Ljava.lang.Object;,layer1_arr,layer2_arr
	 * matrix,ffnn.traindata,testdata/traindata0.dat
	 * dblarray,ffnn.trainlabels,testdata/trainlabels0.dat
	 * </PRE>
	 * The training data file is a data file that describes a matrix in a format
	 * readable by the method <CODE>utils.DataMgr.readMatrixFromFile()</CODE>;each
	 * row of the matrix corresponds to one training vector. The columns of the
	 * matrix correspond to the features (attributes) of the problem at hand.
	 * The training labels file is a text file that contains one (double) value in
	 * each line, and can be read by the method 
	 * <CODE>utils.DataMgr.readDoubleLabelsFromFile(filename)</CODE>.
	 * @param args String[] only the params filename arg is required.
	 */
	public static void main(String[] args) {
		try {
			HashMap params = DataMgr.readPropsFromFile(args[0]);
			final long start = System.currentTimeMillis();
			PDBatchTaskExecutor extor = null;
			if (args.length>1) {
				int nt = Integer.parseInt(args[1]);
				if (nt>1) {
					extor = PDBatchTaskExecutor.newPDBatchTaskExecutor(nt);
					params.put("ffnn.pdbtexecutor",extor);
				}
			}
			// now, produce an array of zero weights: the dimension of this vector
			// is #features*#nodes(layer_0) + 
			//    \sum_{i=0}^{i=#layers-2} [#nodes(layer_i)*#nodes(layer_{i+1}] +
			//    #nodes(layer_{#layers-1})
			Object[] hidden_layers = (Object[])params.get("hiddenlayers");
			NNNodeIntf output_node = (NNNodeIntf) params.get("outputlayer");
			double[][] train_data = (double[][])params.get("ffnn.traindata");
			int num_ws = ((Object[])hidden_layers[0]).length*train_data[0].length;
			for (int i=0; i<hidden_layers.length-1; i++) {
				num_ws += ((Object[])hidden_layers[i]).length *
					        ((Object[])hidden_layers[i+1]).length;
			}
			num_ws += ((Object[])hidden_layers[hidden_layers.length-1]).length;
			double[] weights = new double[num_ws];
			FFNN4Train func = new FFNN4Train(hidden_layers, output_node, null);
			double result = func.eval(weights, params);
			final long dur = System.currentTimeMillis()-start;
			System.out.println("final cost function value = "+result+
				                 " (took "+dur+" msecs)");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	
	/**
	 * static auxiliary class not part of the public API.
	 */
	static class FFNNMultiEvalTask implements TaskObject {
		private double[][] _inputs;
		private double[] _labels;
		private NNNodeIntf[][] _hiddenLayers;
		private NNNodeIntf _outputNode;
		private double[] _weights;
		private int _numInputVecs = 0;
		
		/**
		 * single public constructor.
		 * @param inputs double[][]
		 * @param labels double[]
		 * @param weights double[]
		 * @param hiddenLayers NNNodeIntf[][]
		 * @param outNode NNNodeIntf
		 */
		public FFNNMultiEvalTask(double[][] inputs, double[] labels, 
			                       double[] weights, 
			                       NNNodeIntf[][] hiddenLayers, NNNodeIntf outNode) {
			_inputs = inputs;
			_labels = labels;
			_hiddenLayers = hiddenLayers;
			_outputNode = outNode;
			_weights = weights;
			// finally, set the value of _numInputs
			for (_numInputVecs=0; _numInputVecs<inputs.length; _numInputVecs++){
				if (inputs[_numInputVecs]==null) {
					break;
				}
			}
		}
		
		
		/**
		 * computes the error for each of the input vectors (given their labels) 
		 * and returns them as a <CODE>double[]</CODE>.
		 * @return 
		 */
		public Serializable run() {
			double[] errors = new double[_numInputVecs];

			final int num_inputs = _inputs[0].length;
			final int num_hidden_layers = _hiddenLayers.length;
			for (int t=0; t<_numInputVecs; t++) {
				double[] inputs_t = _inputs[t];
				// compute from layer-0 to final hidden layer the node activations
				int pos = 0;  // the position index in the vector w
				// get the inputs for the layer. 
				// Inputs are same for all nodes in a layer.
				double[] layer_i_inputs = new double[num_inputs];
				for (int i=0; i<num_inputs; i++) 
					layer_i_inputs[i] = inputs_t[i];  // must NOT modify inputs_t
				for (int i=0; i<num_hidden_layers; i++) {
					NNNodeIntf[] layeri = _hiddenLayers[i];
					double[] layeri_outputs = new double[layeri.length];
					for (int j=0; j<layeri.length; j++) {
						NNNodeIntf node_i_j = layeri[j];
						layeri_outputs[j] = node_i_j.eval(layer_i_inputs, _weights, pos);
						pos += layer_i_inputs.length;
					}
					layer_i_inputs = layeri_outputs;  // set the inputs for next iteration
				}
				// compute the output node output
				double valt = _outputNode.eval(layer_i_inputs, _weights, pos);
				errors[t] = (valt-_labels[t]);
			}
			
			return errors;
		}
		
		
	  public boolean isDone() {
			throw new UnsupportedOperationException("not implemented");
		}
		public void copyFrom(TaskObject other) {
			throw new UnsupportedOperationException("not implemented");
		}
	}
	
}
