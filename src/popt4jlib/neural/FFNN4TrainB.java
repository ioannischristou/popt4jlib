package popt4jlib.neural;

import popt4jlib.*;
import popt4jlib.neural.costfunction.FFNNCostFunctionIntf;
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
import java.util.Locale;
import java.text.NumberFormat;
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
		
	private final static int _numDerivInputsPerTask = 256;  // compile-time const
	                                                        // used in parallel
	                                                        // computation of grad

	private NumberFormat _df = null;
	
	private final static Messenger _mger = Messenger.getInstance();
	
	private boolean _isInited = false;
	
	// defines the order of computing the partial derivatives w.r.t the weights
	private boolean _computingOrderAsc = false;
	
	/**
	 * bit-vector holds indices of weights that cannot change (must remain fixed).
	 * Notice that ALL methods handling this bit-vector are unsynchronized and 
	 * therefore it is only safe to use them from the "main" thread of control
	 * before passing <CODE>FFNNMultiEvalTaskB</CODE> and/or 
	 * <CODE>FFNNMultiEvalPartialDerivTaskB</CODE> objects to compute the 
	 * neural network function value or the derivative of the neural network.
	 */
	private BoolVector _fixedWgtInds;
	
	
	/**
	 * 3-arg public constructor for serial training set evaluation (unless the 
	 * "right" params are passed to the <CODE>eval()</CODE> method.)
	 * @param hiddenlayers Object[]  // NodeIntf[][]
	 * @param outputnode OutputNNNodeIntf
	 * @param f FFNNCostFunctionIntf the cost (error) function to measure the 
	 * "cost" of the network as a function of its errors on the training set; 
	 * if null, the MSSE is used by default (essentially corresponding to a 
	 * regression problem).
	 */
	public FFNN4TrainB(Object[] hiddenlayers, OutputNNNodeIntf outputnode, 
		                FFNNCostFunctionIntf f) {
		super(hiddenlayers, outputnode, f);
		_df = NumberFormat.getInstance(Locale.US);
		_df.setGroupingUsed(false);
		_df.setMaximumFractionDigits(4);
	}

	
	/**
	 * 4-arg public constructor allows for shared-memory parallel training set 
	 * evaluation.
	 * @param hiddenlayers Object[]  // NodeIntf[][]
	 * @param outputnode OutputNNNodeIntf
	 * @param f FFNNCostFunctionIntf the cost (error) function to measure the 
	 * "cost" of the network as a function of its errors on the training set; 
	 * if null, the MSSE norm is used by default (essentially corresponding to a 
	 * regression problem).
	 * @param numthreads int if &gt; 1, parallel training set evaluation occurs.
	 */
	public FFNN4TrainB(Object[] hiddenlayers, OutputNNNodeIntf outputnode, 
		                 FFNNCostFunctionIntf f, int numthreads) {
		super(hiddenlayers, outputnode, f, numthreads);
		_df = NumberFormat.getInstance(Locale.US);
		_df.setGroupingUsed(false);
		_df.setMaximumFractionDigits(4);
	}
	
	
	/**
	 * finalize this FFNN4TrainB network's initialization by setting every node to
	 * belong to this network. Must be called once, right after construction of 
	 * this object.
	 * @param num_input_signals int the number of input signals for this network.
	 */
	public synchronized void finalizeInitialization(int num_input_signals) {
		if (_isInited) {
			throw new IllegalStateException("network has already been initialized");
		}
		final NNNodeIntf[][] hiddenLayers = getHiddenLayers();
		final int num_layers = getNumHiddenLayers();
		int pos = 0;
		for (int i=0; i<num_layers; i++) {
			NNNodeIntf[] li = hiddenLayers[i];
			if (i==0) {  // compute total #weights and set it on the first layer nodes
				int num_wgts = 0;
				int num_layer_inputs = num_input_signals;
				for (int j=0; j<num_layers; j++) {
					num_wgts += hiddenLayers[j].length * (num_layer_inputs+1);
					num_layer_inputs = hiddenLayers[j].length;
				}
				num_wgts += num_layer_inputs+1;  // weights of output node
				for (int j=0; j<hiddenLayers[0].length; j++) {
					hiddenLayers[0][j].setTotalNumWeights(num_wgts);
					hiddenLayers[0][j].setNodeLayer(0);
					hiddenLayers[0][j].setPositionInLayer(j);
				}
			}
			for (int j=0; j<li.length; j++) {
				NNNodeIntf n_ij = li[j];
				n_ij.setFFNN4TrainB(this);
				n_ij.setNodeLayer(i);
				n_ij.setPositionInLayer(j);
				final int len = i>0 ? hiddenLayers[i-1].length : num_input_signals;
				n_ij.setWeightRange(pos, pos+len);
				pos += len+1;  // +1 is for bias term
			}
		}
		final OutputNNNodeIntf outnode = getOutputNode();
		outnode.setFFNN4TrainB(this);
		outnode.setNodeLayer(num_layers);
		outnode.setPositionInLayer(0);
		outnode.setWeightRange(pos, pos+hiddenLayers[hiddenLayers.length-1].length);
		_costFunc.setFFNN(this);
		_fixedWgtInds = new BoolVector(getTotalNumWeights());
		_isInited = true;
	}
	
	
	/**
	 * synchronized method that returns true if and only if the 
	 * <CODE>finalizeInitialization(num)</CODE> method has been called on this
	 * object.
	 * @return boolean 
	 */
	public synchronized boolean isInitialized() {
		return _isInited;
	}

	
	/**
	 * get the total number of weights variables (including biases) for this net.
	 * @return 
	 */
	public int getTotalNumWeights() {
		final NNNodeIntf[][] hiddenLayers = getHiddenLayers();
		return hiddenLayers[0][0].getTotalNumWeights();
	}
	
	
	/**
	 * return an int[] containing the indices of the bias terms in the all weights
	 * variables for this network.
	 * @param num_input_signals int
	 * @return int[] values in {0, ..., all_weights.length-1}
	 */
	public int[] getIndices4BiasInWgts(int num_input_signals) {
		final NNNodeIntf[][] hlayers = getHiddenLayers();
		int num_nodes=1;  // output node
		for (int l=0; l<hlayers.length; l++) {
			num_nodes += hlayers[l].length;
		}
		int[] bias_inds = new int[num_nodes];
		int pos = 0;
		int layerl_inputs = num_input_signals;
		int offset = 0;
		for (int l=0; l<hlayers.length; l++) {
			NNNodeIntf[] layerl = hlayers[l];
			for (int k=0; k<layerl.length; k++) 
				bias_inds[pos++] = offset + (layerl_inputs+1)*(k+1)-1;
			offset += (layerl_inputs+1)*layerl.length;  // +1 is for bias term
			layerl_inputs = layerl.length;
		}
		// bias term for last (output) node
		bias_inds[bias_inds.length-1] = getTotalNumWeights()-1;
		return bias_inds;
	}

	
	/**
	 * get the NNNodeIntf node that is the "source" of the weight whose index is
	 * the given 2nd argument.
	 * @param numInputSignals int the number of input features in this FFNN4TrainB
	 * object
	 * @param weightIndex int
	 * @return NNNodeIntf may be null if weight connects input features to 1st 
	 * hidden layer or if weight is for bias term
	 * @throws IllegalArgumentException if weightIndex is invalid
	 */
	public NNNodeIntf getStartNode(int numInputSignals, int weightIndex) {
		if (weightIndex<0 || weightIndex >= getTotalNumWeights()) 
			throw new IllegalArgumentException("windex="+weightIndex+" out of range"+
				                                 "[0,"+getTotalNumWeights()+"]");
		NNNodeIntf[][] hlayers = getHiddenLayers();
		// weight connects inputs to hidden layer
		if ((numInputSignals+1)*hlayers[0].length <= weightIndex) return null;
		int cnt = (numInputSignals+1)*hlayers[0].length;
		int i=1;
		int numSignalsi = hlayers[0].length;
		while (true) {
			if (cnt+(numSignalsi+1)*hlayers[i].length >= weightIndex) {
				// requested node is in layer i-1
				NNNodeIntf[] layerim1 = hlayers[i-1];
				int start_ind = layerim1[0].getDirectInputWeightStartIndex();
				int pos = (weightIndex-start_ind) % (numSignalsi+1);
				if (pos==numSignalsi) return null;  // weight is bias term
				return layerim1[pos];
			}
			cnt += hlayers[i].length*(numSignalsi+1);
			++i;
			numSignalsi = hlayers[i].length;
		}		
	}
	
	
	/**
	 * get the NNNodeIntf node that is the "sink" of the weight whose index is
	 * the given 2nd argument.
	 * @param numInputSignals int the number of input features in this FFNN4TrainB
	 * object
	 * @param weightIndex int
	 * @return NNNodeIntf
	 * @throws IllegalArgumentException if weightIndex is invalid
	 */
	public NNNodeIntf getEndNode(int numInputSignals, int weightIndex) {
		if (weightIndex<0 || weightIndex >= getTotalNumWeights()) 
			throw new IllegalArgumentException("windex="+weightIndex+" out of range"+
				                                 "[0,"+getTotalNumWeights()+"]");
		NNNodeIntf[][] hlayers = getHiddenLayers();
		int i=0;
		int cnt = 0;
		int numSignalsi = numInputSignals;
		while (true) {
			if (cnt+(numSignalsi+1)*hlayers[i].length >= weightIndex) {
				// requested node is in layer i
				NNNodeIntf[] layeri = hlayers[i];
				int ind_in_layer = (weightIndex-cnt) / (numSignalsi+1);
				return layeri[ind_in_layer];
			}
			cnt += hlayers[i].length*(numSignalsi+1);
			++i;
			numSignalsi = hlayers[i].length;
		}
	}
	
	
	/**
	 * gets the weight indices that are currently fixed (are not allowed to 
	 * change.)
	 * @return BoolVector
	 */
	public BoolVector getFixedWgtInds() {
		return _fixedWgtInds;
	}
	
	
	/**
	 * sets the weight indices that are not currently allowed to change. 
	 * @param fixedInds BoolVector
	 */
	public void setFixedWgtInds(BoolVector fixedInds) {
		_fixedWgtInds = fixedInds;
	}
	
	
	/**
	 * add the given indices as not allowed to change. Previous weight indices 
	 * that were fixed, remain fixed.
	 * @param fxdWgts BoolVector
	 */
	public void addFixedWgtInds(BoolVector fxdWgts) {
		_fixedWgtInds.or(fxdWgts);
	}
	
	
	/**
	 * sets all values of the specified weight indices in the wgt_inds bit vector
	 * in the wgts array to the value val, and sets the fixed weight indices to 
	 * the given bit vector.
	 * @param wgts double[] must be of same size as wgt_inds bit vector
	 * @param wgt_inds BoolVector must be of same size as wgts double array
	 * @param val double (usually zero)
	 */
	public void fixWgtsAtValue(double[] wgts, BoolVector wgt_inds, double val) {
		for (int i=wgt_inds.nextSetBit(0); i>=0; i=wgt_inds.nextSetBit(i+1)) {
			wgts[i] = val;
		}
		setFixedWgtInds(wgt_inds);
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
	 * evaluates the output of the output node of the network when an input 
	 * (train_instance, train_label) pair is given.
	 * @param weights double[] all weights array (includes bias terms)
	 * @param train_instance double[] the training instance
	 * @param train_label double the training label
	 * @param params HashMap unused
	 * @return double
	 */
	public double evalNetworkOutputOnTrainingData(double[] weights,
		                                            double[] train_instance, 
		                                            double train_label,
																								HashMap params) {		
		// compute from layer-0 to final hidden layer the node activations
		final OutputNNNodeIntf outn = getOutputNode();
		final int num_inputs = train_instance.length;
		final int num_hidden_layers = getNumHiddenLayers();
		final NNNodeIntf[][] hidden_layers = getHiddenLayers();
		int pos = 0;  // the position index in the vector w
		// get the inputs for the layer. Inputs are same for all nodes in a layer.
		double[] layer_i_inputs = new double[num_inputs];
		for (int i=0; i<num_inputs; i++) layer_i_inputs[i] = train_instance[i];
		for (int i=0; i<num_hidden_layers; i++) {
			NNNodeIntf[] layeri = hidden_layers[i];
			double[] layeri_outputs = new double[layeri.length];
			for (int j=0; j<layeri.length; j++) {
				NNNodeIntf node_i_j = layeri[j];
				layeri_outputs[j] = node_i_j.evalB(layer_i_inputs, weights, pos);
				// print out diagnostics
				pos += layer_i_inputs.length+1;  // +1 is for the bias
			}
			layer_i_inputs = layeri_outputs;  // set the inputs for next iteration
		}
		double valt = outn.evalB(layer_i_inputs, weights, pos, train_label);
		// save valt in cache
		((BaseNNNode) outn).setLastEvalCache(valt);
		return valt;
	}
	
	
	/**
	 * measures network cost on validation data, using the <CODE>_costFunc</CODE>
	 * of this network as cost estimator. Clears all caches before and after every
	 * computation (operation is not really needed.) Can be easily parallelized as 
	 * well.
	 * @param weights double[] the weights of the network
	 * @param valdata double[][] the validation instances
	 * @param vallabels double[] the validation labels
	 * @return double
	 */
	public double evalNetworkCostOnValidationData(double[] weights, 
		                                            double[][] valdata, 
																								double[] vallabels) {
		resetDerivCaches();
		resetGradVectorCaches();
		final int num_instances = valdata.length;
		double[] errors = new double[num_instances];
		for (int i=0; i<num_instances; i++) {
			final double[] vdatai = valdata[i];
			final double vlabeli = vallabels[i];
			errors[i]=evalNetworkOutputOnTrainingData(weights, vdatai, vlabeli, null);
			resetDerivCaches();
			resetGradVectorCaches();
		}
		return _costFunc.eval(errors, null);
	}
	
	
	/**
	 * measures total network cost on validation data, by computing the true 
	 * network output (not train error that is output during training) on these
	 * validation data and labels, using a cost function as cost estimator. This 
	 * is the main method for measuring validation accuracy of the network after 
	 * it has been trained.
	 * Can be easily parallelized as well.
	 * @param weights double[] the weights of the network
	 * @param valdata double[][] the validation instances
	 * @param vallabels double[] the validation labels
	 * @param cf FunctionIntf the function used to measure the total cost on the 
	 * errors; if null, the <CODE>_costFunc</CODE> of this object is used.
	 * @return double
	 */
	public double evalNetworkOutputOnValidationData(double[] weights, 
		                                              double[][] valdata, 
																	  							double[] vallabels,
																									FunctionIntf cf) {
		HashMap params = new HashMap();
		final int num_input_signals = valdata[0].length;
		final int num_hlayers = getNumHiddenLayers();
		final int num_instances = valdata.length;
		for (int l=0; l<num_hlayers; l++) {
			double[][] hwsl = getLayerWeightsWithBias(l, weights, num_input_signals);
			params.put("hiddenws"+l,hwsl);
		}
		double[] outws = getOutputWeightsWithBias(weights);
		params.put("outputws",outws);
		double[] errors = new double[num_instances];
		for (int i=0; i<num_instances; i++) {
			final double[] vdatai = valdata[i];
			final double vlabeli = vallabels[i];
			errors[i] = evalNetworkOnInputData(vdatai, params) - vlabeli;
		}
		if (cf==null) cf = _costFunc;
		// pass in to the cf eval method as parameters the validation data even 
		// though it is currently not used by any of the functions of interest in 
		// the costfunction sub-package.
		params.put("ffnn.valdata",valdata);
		params.put("ffnn.vallabels", vallabels);
		return cf.eval(errors, params);
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
			if (_mger.getDebugLvl()>=4) {  // diagnostics
				_mger.msg(" FFNN4TrainB.eval(): WORKING ON DATA train_vectors["+t+"]",
					        4);
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
					if (_mger.getDebugLvl()>=4) {
						_mger.msg("  FFNN4TrainB.eval(): "+node_i_j.getNodeName()+"[layer="+
							        i+"][index="+j+"]:", 4);
						String inps = "[ ";
						for (int k=0; k<layer_i_inputs.length; k++) {
							inps += _df.format(layer_i_inputs[k])+" ";
						}
						inps += "]";
						_mger.msg("   INPUTS="+inps, 4);
						String ws = "[ ";
						for (int k=0; k<layer_i_inputs.length; k++) {
							ws += _df.format(w[k+pos])+" ";
						}
						ws += _df.format(w[layer_i_inputs.length+pos])+"(BIAS) ";
						ws += "]";
						_mger.msg("   WEIGHTS="+ws, 4);
						_mger.msg("   "+node_i_j.getNodeName()+"[layer="+i+"][index="+j+
							       "] OUTPUT="+
							       _df.format(layeri_outputs[j]), 4);
					}  // diagnostics
					pos += layer_i_inputs.length+1;  // +1 is for the bias
				}
				layer_i_inputs = layeri_outputs;  // set the inputs for next iteration
			}
			double valt = output_node.evalB(layer_i_inputs, w, pos, train_labels[t]);
			// print out diagnostics
			if (_mger.getDebugLvl()>=4) {
				_mger.msg("  FFNN4TrainB.eval(): OUTPUT "+output_node.getNodeName()+":", 
				          4);
				String inps = "[ ";
				for (int k=0; k<layer_i_inputs.length; k++) {
					inps += _df.format(layer_i_inputs[k])+" ";
				}
				inps += "]";
				_mger.msg("   INPUTS="+inps, 4);
				String ws = "[ ";
				for (int k=0; k<layer_i_inputs.length; k++) {
					ws += _df.format(w[k+pos])+" ";
				}
				ws += _df.format(w[layer_i_inputs.length+pos])+"(BIAS) ";
				ws += "]";
				_mger.msg("   WEIGHTS="+ws, 4);
				_mger.msg("  FINAL OUTPUT for train_data["+t+"] = "+valt, 4);				
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
	 * compute the partial derivative of the entire network with respect to the 
	 * weight variable indexed by index. Before calling this method, the 
	 * <CODE>finalizeInitialization(n)</CODE> method must have been called first
	 * on this object to set up the nodes in the neural net.
	 * @param weights double[] the values array for the variables of the function
	 * @param index int the index of the function to compute its partial 
	 * derivative at (range in {0,1,...weights.length-1})
	 * @param p HashMap must contain the "ffnn.traindata" and "ffnn.trainlabels"
	 * keys, else the data will be fetched from the <CODE>TrainData</CODE> class.
	 * @param clearParams boolean if true, any "hiddenws$i$" and "outputws" keys
	 * will be removed from the parameters p hashmap that is passed to the nodes
	 * of the neural network for automatic differentiation. Notice that this 
	 * parameter must become true when the weights argument no longer corresponds
	 * to the keys mentioned above in the hashmap, but should otherwise be false.
	 * @return double 
	 */
	public double evalPartialDerivativeB(double[] weights, int index, HashMap p,
		                                   boolean clearParams) {
		if (_fixedWgtInds.get(index)) {  // weight is fixed, partial derivative is 0
			return 0.0;
		}
		final double[][] traindata = p.containsKey("ffnn.traindata") ?
			                             (double[][]) p.get("ffnn.traindata") :
			                             TrainData.getTrainingVectors();
		final double[] trainlabels = p.containsKey("ffnn.trainlabels") ?
			                             (double[]) p.get("ffnn.trainlabels") :
			                             TrainData.getTrainingLabels();
		final int num_instances = traindata.length;
		long st = -1;
		if (_mger.getDebugLvl()>=3) st = System.currentTimeMillis();
		HashMap p2 = new HashMap(p);
		if (clearParams) {// remove the hiddenws$i$ and outputws from p2
			int num_layers = getNumHiddenLayers();
			for (int i=0; i<num_layers; i++) p2.remove("hiddenws"+i);
			p2.remove("outputws");
		}
		double result;
		double[] results = new double[num_instances];
		for (int i=0; i<num_instances; i++) results[i] = Double.NaN;
		// are we going parallel?
		// check if there is a PDBatchTaskExecutor to use
		PDBatchTaskExecutor extor = (PDBatchTaskExecutor)p.get("ffnn.pdbtexecutor");
		if (_extor!=null) {  // see if we have built-in parallel!
			extor = _extor;
		}
		if (extor!=null) {  // create task-objects to do the work for each t
			List tasks = 
				new ArrayList((int)Math.ceil(((double)num_instances) /
					                           _numDerivInputsPerTask));
      // List<FFNNMultiEvalPartialDerivTaskB>
			int start = 0;
			int end = _numDerivInputsPerTask;
			while (end <= traindata.length) {
				FFNNMultiEvalPartialDerivTaskB met = 
					new FFNNMultiEvalPartialDerivTaskB(traindata, trainlabels,
						                                 start, end-1,
						                                 weights, index, p2);
				tasks.add(met);
				start = end;
				end += _numDerivInputsPerTask;
			}
			if (end != traindata.length + _numDerivInputsPerTask) {  // remainder
				FFNNMultiEvalPartialDerivTaskB met = 
					new FFNNMultiEvalPartialDerivTaskB(traindata, trainlabels,
						                                 start, traindata.length-1,
						                                 weights, index, p2);
				tasks.add(met);
			}
			try {
				Vector resi = extor.executeBatch(tasks);
				// every element of the resi vector is a double[]
				int pos = 0;
				for (int i=0; i<resi.size(); i++) {
					double[] erri = (double[]) resi.get(i);
					for (int j=0; j<erri.length; j++) results[pos++] = erri[j];
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}  // if extor != null
		else {
			// main sequential loop over the training instances
			for (int i=0; i<num_instances; i++) {
				resetDerivCaches();  // critical for correct computation
				final double[] train_instance = traindata[i];
				final double train_lbl = trainlabels[i];
				double ri = _costFunc.evalPartialDerivativeB(weights, index, 
																										 train_instance, train_lbl, 
																										 p2);
				results[i] = ri;
			}
			// end main sequential loop
		}
		result = _costFunc.evalPartialDerivativeB(results);
		if (_mger.getDebugLvl()>=3) {
			long d2 = System.currentTimeMillis()-st;
			_mger.msg("FFNN4TrainB.evalPartialDerivativeB(index="+index+")"+
				       " with training set size="+traindata.length+" took "+
				       d2+" msecs",3);
		}
		return result;
	}
	
	
	/**
	 * sets the order in which the partial derivatives are to be computed in the 
	 * method <CODE>evalGradient4TermB()</CODE>.
	 * @param do_asc boolean
	 */
	public void setComputingOrderAsc(boolean do_asc) {
		_computingOrderAsc = do_asc;
	}
	
	
	/**
	 * evaluates the entire gradient of this feed-forward neural network on a 
	 * single training pair (x,y). The order of computing the partial derivatives
	 * is by default descending, meaning that the derivatives corresponding to the
	 * first hidden layer's weights will be computed last, but can be reversed
	 * by calling the method <CODE>setComputingOrderAsc(true)</CODE> of this 
	 * object before calling this method.
	 * @param wgts double[] the point at which to compute automatically the 
	 * derivative
	 * @param train_inst double[] the training instance x
	 * @param train_lbl double the training label y
	 * @param num_insts int the total number of training instances
	 * @return double[] the gradient of this network on the point wgts given the 
	 * training pair (train_inst, train_lbl)
	 */
	public double[] evalGradient4TermB(double[] wgts, 
		                                 double[] train_inst, double train_lbl,
																		 int num_insts) {
		final double[] grad4inst = new double[wgts.length];
		if (_computingOrderAsc) {
			for (int i=0; i<wgts.length; i++) {
				grad4inst[i] = 
					_costFunc.evalPartialDerivativeB(wgts, i, train_inst, train_lbl, 
						                               num_insts);
			}
		}
		else {
			for (int i=wgts.length-1; i>=0; i--) {
				grad4inst[i] =  
					_costFunc.evalPartialDerivativeB(wgts, i, train_inst, train_lbl,
						                               num_insts);
			}			
		}
		return grad4inst;
	}
	
	
	/**
	 * resets the derivative-related cache of every node in this ffnn.
	 */
	private void resetDerivCaches() {
		BaseNNNode outn = (BaseNNNode) getOutputNode();
		outn.resetCache();
		NNNodeIntf[][] hnodes = getHiddenLayers();
		for (int i=0; i<hnodes.length; i++) {
			final int ilen = hnodes[i].length;
			for (int j=0; j<ilen; j++) {
				BaseNNNode hnij = (BaseNNNode) hnodes[i][j];
				hnij.resetCache();
			}
		}
	}

	
	/**
	 * resets the gradient vector cache of every node in this ffnn.
	 */
	private void resetGradVectorCaches() {
		BaseNNNode outn = (BaseNNNode) getOutputNode();
		outn.resetGradVectorCache();
		NNNodeIntf[][] hnodes = getHiddenLayers();
		for (int i=0; i<hnodes.length; i++) {
			final int ilen = hnodes[i].length;
			for (int j=0; j<ilen; j++) {
				BaseNNNode hnij = (BaseNNNode) hnodes[i][j];
				hnij.resetGradVectorCache();
			}
		}
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
			FFNNCostFunctionIntf costfunc = 
				(FFNNCostFunctionIntf) params.get("costfunc");
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
	
	
	/**
	 * auxiliary helper class, NOT part of the API.
	 */
	class FFNNMultiEvalPartialDerivTaskB implements TaskObject {
		private double[][] _inputs;
		private double[] _labels;
		private int _start;
		private int _end;
		private double[] _weights;
		private int _index;
		private HashMap _p2;


		/**
		 * sole constructor.
		 * @param train_data double[][]
		 * @param train_labels double[]
		 * @param start int inclusive
		 * @param end int inclusive
		 * @param weights double[]
		 * @param index int
		 * @param p2 HashMap
		 */
		public FFNNMultiEvalPartialDerivTaskB(double[][] train_data, 
			                                    double[] train_labels,
																					int start, int end,
																					double[] weights, int index, 
																					HashMap p2) {
			_inputs = train_data;
			_labels = train_labels;
			_start = start;
			_end = end;
			_weights = weights;
			_index = index;
			_p2 = new HashMap(p2);
		}
		
		
		/**
		 * runs the required partial derivative evaluation method in parallel.
		 * @return double[]
		 */
		public Serializable run() {
			final int len = _end-_start+1;
			double[] results = new double[len];
			int cnt = 0;
			for (int i=_start; i<=_end; i++) {
				resetDerivCaches();  // critical for correct computation
				results[cnt++] = _costFunc.evalPartialDerivativeB(_weights, _index, 
					                                                _inputs[i],_labels[i], 
																											    _p2);
			}
			return results;
		}
		
		
	  public boolean isDone() {
			throw new UnsupportedOperationException("not implemented");
		}
		public void copyFrom(TaskObject other) {
			throw new UnsupportedOperationException("not implemented");
		}
	}
	
}
