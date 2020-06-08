package popt4jlib.neural;

import popt4jlib.*;
import parallel.TaskObject;
import parallel.distributed.PDBatchTaskExecutor;
import utils.DataMgr;
import utils.RndUtil;
import utils.Messenger;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Random;
import java.io.Serializable;


/**
 * FFNN4TrainB extends FFNN4Train and adds the requirement that all nodes in the
 * FFNN have one more trainable variable (encoded as their last weight input
 * variable), namely a bias. 
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FFNN4TrainB extends FFNN4Train {
		
	
	/**
	 * 3-arg public constructor for serial training set evaluation (unless the 
	 * "right" params are passed to the <CODE>eval()</CODE> method.)
	 * @param hiddenlayers Object[]  // NodeIntf[][]
	 * @param outputnode OutputNNNodeIntf
	 * @param f FunctionIntf the cost (error) function to measure the "cost" of 
	 * the network as a function of its errors on the training set; if null, the 
	 * L2 norm is used by default (essentially corresponding to a regression 
	 * problem).
	 */
	public FFNN4TrainB(Object[] hiddenlayers, OutputNNNodeIntf outputnode, 
		                FunctionIntf f) {
		super(hiddenlayers, outputnode, f);
	}

	
	/**
	 * 4-arg public constructor allows for shared-memory parallel training set 
	 * evaluation.
	 * @param hiddenlayers Object[]  // NodeIntf[][]
	 * @param outputnode OutputNNNodeIntf
	 * @param f FunctionIntf the cost (error) function to measure the "cost" of 
	 * the network as a function of its errors on the training set; if null, the 
	 * L2 norm is used by default (essentially corresponding to a regression 
	 * problem).
	 * @param numthreads int if &gt; 1, parallel training set evaluation occurs.
	 */
	public FFNN4TrainB(Object[] hiddenlayers, OutputNNNodeIntf outputnode, 
		                FunctionIntf f, int numthreads) {
		super(hiddenlayers, outputnode, f, numthreads);
	}
	
	
	/**
	 * allows clients to call the base-class <CODE>eval()</CODE> method.
	 * @param arg Object  // double[] inputSignal
	 * @param params HashMap  // must contain &lt;"hiddenws$i$", double[][]&gt;
	 * as well as &lt;"outputws",double[]&gt; pairs. All these arrays must be
	 * expanded to include the respective node biases.
	 * @return double
	 */
	public double evalNetworkOnInputData(Object arg, HashMap params) {
		HashMap p2 = new HashMap(params);
		p2.put("includeBiases", new Boolean(true));
		return super.evalNetworkOnInputData(arg, p2);
	}
	
	
	/**
	 * evaluates this feed-forward neural network with single output on a given
	 * training dataset, with given training labels, according to the cost 
	 * function held in data member <CODE>_costFunction</CODE> where the cost 
	 * function accepts as inputs the errors of the network on the training data.
	 * The difference from base class <CODE>FFNN4Train</CODE> is that in this 
	 * class, each node requires one more weight (coming last in the order of 
	 * weights) to be the bias for the node), so the weights variables array must
	 * have size increased by exactly the number of total nodes (hidden plus 
	 * output) in the network.
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
	 * contain NaN's for those instances that were not evaluated, and in the case
	 * of parallel execution, the errors will not in general correspond to the 
	 * positions in the training set that they were created. This should not 
	 * create any problem for cost function evaluation, because no cost function 
	 * cares about where an error has occurred (see the classes in package 
	 * <CODE>popt4jlib.neural.costfunction</CODE>).
	 * @param x Object  // double[] the weights of the network:
	 * The weights are stored consecutively, starting with the weights of the 
	 * first node of the first layer (including for each node one bias variable), 
	 * then continuing with the weights of the second node of the first layer 
	 * (again, including one bias variable for each node) ... until finally we get 
	 * the weights of the connections to the output node plus their own biases. 
	 * The FFNN class network architecture is that all nodes in one layer connect 
	 * to all nodes in the next layer and only those. Notice that x may be instead 
	 * be a <CODE>DblArray1Vector</CODE> object, in which case we obtain access to 
	 * the underlying <CODE>double[]</CODE> via the 
	 * <CODE>popt4jlib.DblArray1VectorAccess.get_x()</CODE> static method.
	 * @param params HashMap must contain the dataset as well as the labels for
	 * each input vector. The dataset is maintained as a 2-D double array
	 * <CODE>double[][]</CODE> with key "ffnn.traindata" where each row represents 
	 * one input vector, together with the expected outputs maintained in a 1-D 
	 * double array <CODE>double[]</CODE> stored for key "ffnn.trainlabels". If 
	 * either of these two keys doesn't exist in the params map, the method will
	 * attempt to retrieve them by calling the static class methods
	 * <CODE>TrainData.getTrainingVectors()</CODE> and/or 
	 * <CODE>TrainData.getTrainingLabels()</CODE> respectively. In the case of
	 * distributed function evaluations (where both the weights as well as the 
	 * parameters needed for the evaluation of the function) this allows to avoid
	 * sending the training data along with the weights for evaluation over the 
	 * network, assuming of course that when the argument reaches its destination
	 * worker JVM, the worker has been initialized via a proper command so that 
	 * the calls mentioned above actually have (cached) data to return.
	 * @return double
	 */
	public double eval(Object x, HashMap params) {
		Messenger mger = Messenger.getInstance();
		double result = 0.0;
		// w is the weights+biases of the network connections
		final double[] w = (x instanceof double[]) ?   
			                   (double[]) x :
			                   DblArray1VectorAccess.get_x((DblArray1Vector) x);
		final double[][] train_vectors = 
			params.containsKey("ffnn.traindata") ?
				(double[][]) params.get("ffnn.traindata") :
			  TrainData.getTrainingVectors();
		double[] train_labels = 
			params.containsKey("ffnn.trainlabels") ?
				(double[]) params.get("ffnn.trainlabels") :
				TrainData.getTrainingLabels();
		final int batchsize = 
			params.containsKey("ffnn.randombatchsize") ?
				((Integer)params.get("ffnn.randombatchsize")).intValue() : -1;
		final double prob_enter = batchsize > 0 ? 
				                        ((double) batchsize)/train_vectors.length : 1.0;
		final int num_inputs = train_vectors[0].length;  // get the #input_signals
		final NNNodeIntf[][] hidden_layers = getHiddenLayers();
		final int num_hidden_layers = hidden_layers.length;
		final OutputNNNodeIntf output_node = getOutputNode();
		double[] errors = new double[train_vectors.length];
		// set the values to NaN as default: only needed when using randombatchsize
		for (int i=0; i<errors.length; i++) errors[i] = Double.NaN;
			
		Integer tidI = (Integer) params.get("thread.id");
		Random rnd = null;
		if (tidI!=null) rnd = RndUtil.getInstance(tidI.intValue()).getRandom();
		else rnd = RndUtil.getInstance().getRandom();
			
		// check if there is a PDBatchTaskExecutor to use
		PDBatchTaskExecutor extor = 
			(PDBatchTaskExecutor) params.get("ffnn.pdbtexecutor");
		if (_extor!=null) {  // see if we have built-in parallel!
			extor = _extor;
		}
		if (extor!=null) {  // create task-objects to do the work for each t
			List tasks = 
				new ArrayList((int) Math.ceil(((double)train_vectors.length) /
					                            _numInputsPerTask));  
      // List<FFNNMultiEvalTaskB>
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
					FFNNMultiEvalTaskB met = 
						new FFNNMultiEvalTaskB(input_vectors, input_labels, w,
							                     hidden_layers, output_node);
					tasks.add(met);
					cnt=0;
					input_vectors = new double[_numInputsPerTask][];
					input_labels = new double[_numInputsPerTask];
				}
			}
			if (cnt > 0) {  // there is a remainder
				FFNNMultiEvalTaskB met = 
					new FFNNMultiEvalTaskB(input_vectors, input_labels, w, 
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
			params2.put("ffnn.weights", w);
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
			if (mger.getDebugLvl()>=3) {  // diagnostics
				mger.msg(" FFNN4TrainB.eval(): WORKING ON DATA train_vectors["+t+"]",2);
			}
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
					layeri_outputs[j] = node_i_j.evalB(layer_i_inputs, w, pos);
					// print out diagnostics
					if (mger.getDebugLvl()>=3) {
						mger.msg("  FFNN4TrainB.eval(): "+node_i_j.getNodeName()+"[layer="+
							       i+"][index="+j+"]:",2);
						String inps = "[ ";
						for (int k=0; k<layer_i_inputs.length; k++) {
							inps += layer_i_inputs[k]+" ";
						}
						inps += "]";
						mger.msg("   INPUTS="+inps, 2);
						String ws = "[ ";
						for (int k=0; k<layer_i_inputs.length; k++) {
							ws += w[k+pos]+" ";
						}
						ws += w[layer_i_inputs.length+pos]+"(BIAS) ";
						ws += "]";
						mger.msg("   WEIGHTS="+ws, 2);
						mger.msg("   "+node_i_j.getNodeName()+"[layer="+i+"][index="+j+
							       "] OUTPUT="+
							       layeri_outputs[j], 2);
					}  // diagnostics
					pos += layer_i_inputs.length+1;  // +1 is for the bias
				}
				layer_i_inputs = layeri_outputs;  // set the inputs for next iteration
			}
			double valt = output_node.evalB(layer_i_inputs, w, pos, train_labels[t]);
			// print out diagnostics
			if (mger.getDebugLvl()>=3) {
				mger.msg("  FFNN4TrainB.eval(): OUTPUT "+output_node.getNodeName()+":", 
					       2);
				String inps = "[ ";
				for (int k=0; k<layer_i_inputs.length; k++) {
					inps += layer_i_inputs[k]+" ";
				}
				inps += "]";
				mger.msg("   INPUTS="+inps, 2);
				String ws = "[ ";
				for (int k=0; k<layer_i_inputs.length; k++) {
					ws += w[k+pos]+" ";
				}
				ws += w[layer_i_inputs.length+pos]+"(BIAS) ";
				ws += "]";
				mger.msg("   WEIGHTS="+ws, 2);
				mger.msg("  FINAL OUTPUT for train_data["+t+"] = "+valt, 2);				
			}  // diagnostics
			errors[t] = (valt-train_labels[t]);
		}
		// finally, apply the cost function on the errors array, adding to the 
		// params the current weights as "ffnn.weights".
		HashMap params2 = new HashMap(params);
		params2.put("ffnn.weights", w);
		result = _costFunc.eval(errors, params2);
		return result;
	}
	
	
	/**
	 * invoke as:
	 * <CODE>
	 * java -cp &lt;classpath&gt; popt4jlib.neural.FFNN4TrainB 
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
	 * array,weights,double, 1,1,0,0,0, 0,0,0,0,0, 1,1,0, 1,1,0, 0,0,0, 1,1,1,1 
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
			// now, produce an array of weights: the dimension of this vector
			// is (#features+1)*#nodes(layer_0) + 
			//    \sum_{i=0}^{i=#layers-2} [(#nodes(layer_i)+1)*#nodes(layer_{i+1}] +
			//    #nodes(layer_{#layers-1})+1
			// +1 in various places in the formula above is for bias variables
			Object[] hidden_layers = (Object[])params.get("hiddenlayers");
			OutputNNNodeIntf output_node = 
				(OutputNNNodeIntf) params.get("outputlayer");
			FunctionIntf costfunc = (FunctionIntf) params.get("costfunc");
			double[][] train_data = (double[][])params.get("ffnn.traindata");
			int num_ws = ((Object[])hidden_layers[0]).length*(train_data[0].length+1);
			for (int i=0; i<hidden_layers.length-1; i++) {
				num_ws += (((Object[])hidden_layers[i]).length+1) *
					        ((Object[])hidden_layers[i+1]).length;
			}
			num_ws += ((Object[])hidden_layers[hidden_layers.length-1]).length+1;
			//double[] weights = new double[num_ws];
			// set weight values
			double[] weights = (double[]) params.get("weights");
			FFNN4TrainB func = new FFNN4TrainB(hidden_layers, output_node, costfunc);
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
	static class FFNNMultiEvalTaskB implements TaskObject {
		private double[][] _inputs;
		private double[] _labels;
		private NNNodeIntf[][] _hiddenLayers;
		private OutputNNNodeIntf _outputNode;
		private double[] _weights;
		private int _numInputVecs = 0;
		
		/**
		 * single public constructor.
		 * @param inputs double[][]
		 * @param labels double[]
		 * @param weights double[]
		 * @param hiddenLayers NNNodeIntf[][]
		 * @param outNode OutputNNNodeIntf
		 */
		public FFNNMultiEvalTaskB(double[][] inputs, double[] labels, 
			                       double[] weights, 
			                       NNNodeIntf[][] hiddenLayers, 
														 OutputNNNodeIntf outNode) {
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
						layeri_outputs[j] = node_i_j.evalB(layer_i_inputs, _weights, pos);
						pos += layer_i_inputs.length+1;
					}
					layer_i_inputs = layeri_outputs;  // set the inputs for next iteration
				}
				// compute the output node output
				double valt = _outputNode.evalB(layer_i_inputs, _weights, pos, 
					                              _labels[t]);
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
