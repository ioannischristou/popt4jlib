package popt4jlib.neural;

import parallel.TaskObject;
import parallel.distributed.PDBatchTaskExecutor;
import popt4jlib.VecFunctionIntf;
import popt4jlib.VectorIntf;
import popt4jlib.DblArray1Vector;
import popt4jlib.DblArray1VectorAccess;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.io.Serializable;
import java.io.IOException;

/**
 * implements the Back-Propagation algorithm for computing the gradient of a 
 * feed-forward neural network FFNN4TrainB object, on a given (x,y) training 
 * pair. On 3-layer networks (2 hidden layers), it is about 20-40x faster than
 * the <CODE>FastFFNN4TrainBGrad</CODE> algorithm. On 4-layer networks, the
 * difference increases even more, and becomes between 24-160x faster! 
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FasterFFNN4TrainBGrad implements VecFunctionIntf {
	private FFNN4TrainB _ffnn;
	
	private PDBatchTaskExecutor _extor = null;
	
	
	/**
	 * public constructor assumes that the network has been fully initialized with
	 * appropriate call to its <CODE>finalizeInitialization(n)</CODE> method 
	 * before invoking this constructor.
	 * @param ffnn FFNN4TrainB
	 */
	public FasterFFNN4TrainBGrad(FFNN4TrainB ffnn) {
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
	public FasterFFNN4TrainBGrad(FFNN4TrainB ffnn, int num_input_signals) {
		this(ffnn);
		if (!_ffnn.isInitialized())
			_ffnn.finalizeInitialization(num_input_signals);
	}
	

	/**
	 * same as 2-arg constructor, but also initializes a thread-pool to compute
	 * the gradients of the network when given a single training pair (x,y) in 
	 * parallel.
	 * @param ffnn FFNN4TrainB the network whose gradient wrt weights this object
	 * computes
	 * @param num_input_signals int the number of dimensions of the training data
	 * @param num_threads int the number of threads in the thread-pool
	 */
	public FasterFFNN4TrainBGrad(FFNN4TrainB ffnn, int num_input_signals, 
		                         int num_threads) {
		this(ffnn, num_input_signals);
		try {
			_extor = PDBatchTaskExecutor.newPDBatchTaskExecutor(num_threads);
		}
		catch (Exception e) {  // ignore
			e.printStackTrace();
			_extor = null;
		}
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
	 * @throws IllegalArgumentException if the train data pairs cannot be found
	 */
	public VectorIntf eval(VectorIntf x, HashMap p) {
		double[] wgts;
		if (x instanceof DblArray1Vector) {  // don't do copy of array
			wgts = DblArray1VectorAccess.get_x((DblArray1Vector)x);
		}
		else wgts = x.getDblArray1();
		double[][] traindata = (double[][]) p.get("ffnn.traindata");
		double[] trainlabels = (double[]) p.get("ffnn.trainlabels");
		// if either is missing, read from TrainData
		if (traindata==null) traindata = TrainData.getTrainingVectors();
		if (trainlabels==null) trainlabels = TrainData.getTrainingLabels();
		if (traindata==null || trainlabels==null) {
			String traindatafile = (String) p.get("ffnn.traindatafile");
			String trainlabelsfile = (String) p.get("ffnn.trainlabelsfile");
			try {
				TrainData.readTrainingDataFromFiles(traindatafile, trainlabelsfile);
			}
			catch (IOException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("FasterFFNN4TrainBGrad.eval(x,p): "+
					                                 " p has no or wrong "+
					                                 "ffnn.train[data|labels]file info");
			}
			// read again
			if (traindata==null) traindata = TrainData.getTrainingVectors();
			if (trainlabels==null) trainlabels = TrainData.getTrainingLabels();
		}
		final int num_train_instances = traindata.length;
		if (_extor==null) {  // do things serially
			DblArray1Vector g = new DblArray1Vector(_ffnn.getTotalNumWeights());
			for (int t=0; t<num_train_instances; t++) {
				resetAllCaches();
				final double[] train_instance = traindata[t];
				final double train_label = trainlabels[t];
				final double[] g_t = 
					evalGradient4TermB(wgts, train_instance, train_label);
				// add g_t to g
				for (int i=0; i<wgts.length; i++) {
					final double gi_s = g.getCoord(i)+g_t[i];
					g.setCoord(i, gi_s);
				}
			}
			return g;
		}  // go serial
		else {  // go parallel
			// break training instances into up to 10 x num_threads tasks
			final int task_size = 
				Math.max(traindata.length / (_extor.getNumThreads()*10), 1);
			List tasks = new ArrayList();  // List<FastFFNN4TrainBGradTask>
			int start = 0;
			int end = task_size;
			while (end <= traindata.length) {
				FasterFFNN4TrainBGradTask met = 
					new FasterFFNN4TrainBGradTask(traindata, trainlabels,
						                            start, end-1,
						                            wgts);
				tasks.add(met);
				start = end;
				end += task_size;
			}
			if (end != traindata.length + task_size) {  // remainder
				FasterFFNN4TrainBGradTask met = 
					new FasterFFNN4TrainBGradTask(traindata, trainlabels,
						                            start, traindata.length-1,
						                            wgts);
				tasks.add(met);
			}
			double[] g = new double[wgts.length];
			try {
				Vector resi = _extor.executeBatch(tasks);
				// every element of the resi vector is a double[]  -- the sum of the
				// gradients of a number of single (x,y) training pairs
				for (int i=0; i<resi.size(); i++) {
					double[] erri = (double[]) resi.get(i);
					for (int j=0; j<erri.length; j++) g[j] += erri[j];
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			return new DblArray1Vector(g);
		}
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
		VectorIntf g = eval(x, p);
		return g.getCoord(coord);
	}
	
	
	/**
	 * implements the Back-Propagation algorithm for computing the gradient of the
	 * neural network automatically.
	 * @param w double[] the variables (weights) at which we want to evaluate the
	 * gradient
	 * @param train_inst double[] the training instance
	 * @param train_lbl double the training label
	 * @return double[] the gradient of the neural network at w
	 */
	private double[] evalGradient4TermB(double[] w, 
		                                  double[] train_inst, double train_lbl) {
		resetAllCaches();
		final double outv = 
			evalNetworkOutputOnTrainingData(w, train_inst, train_lbl);
		OutputNNNodeIntf outn = _ffnn.getOutputNode();
		final boolean out_ismcsse = outn instanceof MultiClassSSE;
		double err = ((BaseNNNode)outn).getLastEvalCache()-train_lbl;
		double D_out = ((BaseNNNode)outn).getLastDerivEvalCache2()*
			             _ffnn._costFunc.evalDerivative(err);
		// for last hidden layer down to the first
		final NNNodeIntf[][] hlayers = _ffnn.getHiddenLayers();
		final int num_layers = hlayers.length;
		final double[][] D = new double[num_layers][];
		double[] D_prev = new double[]{D_out};
		for (int l=num_layers-1; l>=0; l--) {
			NNNodeIntf[] layer_l = _ffnn.getHiddenLayers()[l];
			D[l] = new double[layer_l.length];
			// special treatment of last layer in case output layer is MultiClassSSE
			if (l==num_layers-1 && out_ismcsse) {
				final double[] ccs = ((MultiClassSSE)outn)._classCosts;
				for (int i=0; i<layer_l.length; i++) {
					final double exp_i = i==(int)train_lbl ? 1.0 : 0.0;
					final double err_i=((BaseNNNode)layer_l[i]).getLastEvalCache()-exp_i;
					D[l][i] = 
						((BaseNNNode)layer_l[i]).getLastDerivEvalCache2()*(err_i+err_i);
					if (ccs!=null) D[l][i] *= ccs[i];
				}
				D_prev = D[l];
				continue;
			}
			for (int i=layer_l.length-1; i>=0; i--) {
				final NNNodeIntf ni = layer_l[i];
				final double gPini = ((BaseNNNode)ni).getLastDerivEvalCache2();
				double sumi = 0;
				final int next_layer_start_w_ind = 
					l<num_layers-1 ?
					  _ffnn.getHiddenLayers()[l+1][0].getDirectInputWeightStartIndex() :
					  outn.getDirectInputWeightStartIndex();
				final int next_layer_size = l < num_layers-1 ? 
					                      _ffnn.getHiddenLayers()[l+1].length :
					                      1;
				final int cur_layer_size = layer_l.length;
				for (int j=0; j<next_layer_size; j++) {
					sumi += w[next_layer_start_w_ind+j*(cur_layer_size+1)+i]*D_prev[j];
				}
				D[l][i] = sumi*gPini;
			}
			D_prev = D[l];
		}
		final double[] wgt_derivs = new double[w.length];
		// first set wgt derivs for input to 1st hidden layer wgts
		int wind = 0;
		for (int i=0; i<hlayers[0].length; i++) {
			for (int j=0; j<train_inst.length; j++) {
				wgt_derivs[wind++] = train_inst[j]*D[0][i];
			}
			// now is the time for bias term derivative
			wgt_derivs[wind++] = D[0][i];  // activation for bias is always 1
		}
		// now set wgt derivs from each layer to the next
		for (int l=1; l<hlayers.length; l++) {
			final NNNodeIntf[] layerl = hlayers[l];
			final NNNodeIntf[] prev_layer = hlayers[l-1];
			for (int i=0; i<layerl.length; i++) {
				for (int j=0; j<prev_layer.length; j++) {
					wgt_derivs[wind++] = 
						((BaseNNNode)prev_layer[j]).getLastEvalCache()*D[l][i];
				}
				// now is the time for the bias term derivative
				wgt_derivs[wind++] = D[l][i];  // activation for bias is always 1
			}
		}
		// finally, set wgt derivs for output node
		NNNodeIntf[] llayer = hlayers[hlayers.length-1];
		for (int j=0; j<llayer.length; j++) {
			wgt_derivs[wind++] = ((BaseNNNode)llayer[j]).getLastEvalCache()*D_out;
		}
		// lastly, the derivative of the output node bias
		wgt_derivs[wind++] = D_out;  // activation for bias is always 1
		return wgt_derivs;
	}
	
	
	/**
	 * evaluates the output of the output node of the network when an input 
	 * (train_instance, train_label) pair is given.
	 * @param weights double[] all weights array (includes bias terms)
	 * @param train_instance double[] the training instance
	 * @param train_label double the training label
	 * @return double
	 */
	private double evalNetworkOutputOnTrainingData(double[] weights,
		                                            double[] train_instance, 
		                                            double train_label) {		
		// compute from layer-0 to final hidden layer the node activations
		final OutputNNNodeIntf outn = _ffnn.getOutputNode();
		final int num_inputs = train_instance.length;
		final int num_hidden_layers = _ffnn.getNumHiddenLayers();
		final NNNodeIntf[][] hidden_layers = _ffnn.getHiddenLayers();
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
		((BaseNNNode) outn).setLastEvalCache(valt);  // itc: HERE not needed really
		return valt;
	}

		
	/**
	 * reset all thread-local caches of the network associated with this object.
	 */
	private void resetAllCaches() {
		BaseNNNode outn = (BaseNNNode) _ffnn.getOutputNode();
		outn.resetCache();
		outn.resetGradVectorCache();
		NNNodeIntf[][] hlayers = _ffnn.getHiddenLayers();
		for (int i=0; i<hlayers.length; i++) {
			NNNodeIntf[] layeri = hlayers[i];
			for (int j=0; j<layeri.length; j++) {
				BaseNNNode hij = (BaseNNNode) layeri[j];
				hij.resetCache();
				hij.resetGradVectorCache();
			}
		}
	}
	
	
	/**
	 * auxiliary inner-class, not part of the API.
	 */
	class FasterFFNN4TrainBGradTask implements TaskObject {
		private double[][] _allTrainData;
		private double[] _allTrainLabels;
		private double[] _wgts;
		private int _start;
		private int _end;
		
		
		public FasterFFNN4TrainBGradTask(double[][] traindata, double[] trainlabels,
			                             int start, int end, double[] wgts) {
			_allTrainData = traindata;
			_allTrainLabels = trainlabels;
			_wgts = wgts;
			_start = start;
			_end = end;
		}
		
		
		public Serializable run() {
			double[] g_sum = new double[_wgts.length];
			for (int t=_start; t<=_end; t++) {
				resetAllCaches();
				final double[] train_instance = _allTrainData[t];
				final double train_label = _allTrainLabels[t];
				final double[] g_t = 
					evalGradient4TermB(_wgts, train_instance, train_label);
				// add g_t to running g_sum
				for (int i=0; i<_wgts.length; i++) {
					g_sum[i] += g_t[i];
				}
			}
			return g_sum;
		}
		
		
	  public boolean isDone() {
			throw new UnsupportedOperationException("not implemented");
		}
		public void copyFrom(TaskObject other) {
			throw new UnsupportedOperationException("not implemented");
		}
		
	}
	
}
