package popt4jlib.neural;

import popt4jlib.BoolVector;
import popt4jlib.VectorIntf;
import popt4jlib.DblArray1Vector;
import popt4jlib.LocalOptimizerIntf;
import popt4jlib.FunctionIntf;
import popt4jlib.IncumbentProviderIntf;
import popt4jlib.OptimizerException;
import parallel.ParallelException;
import utils.RndUtil;
import utils.Messenger;
import utils.PairObjDouble;
import java.util.HashMap;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;
import popt4jlib.GradientDescent.VecUtil;


/**
 * implements a parallel/distributed version of the mini-batch Back Propagation
 * classical algorithm for neural network training (BackProp here means not just
 * the automatic differentiation of the feed-forward neural network, but the 
 * stochastic gradient descent with fixed step-size as well). It works on mini 
 * batches of the entire training set (could be batches of size 1, in which case 
 * it degenerates to the classical online BackProp) to compute the gradient of
 * the <CODE>FFNN4TrainB</CODE> object the algorithm essentially trains. The 
 * extra feature it adds is that instead of modifying each time the current
 * iterate point (i.e. weights vector) by subtracting from it a multiple of the
 * gradient (the multiple being the "learning rate"), it evaluates in sequence
 * the new point that emerges when several different learning rates are applied
 * to the gradient, and picks the best one (the one that results in the largest
 * descent of the objective function.)
 * <p>Notes:
 * <ul>
 * <li> 2020-08-15: added diagnostics to print validation error after every
 * epoch when dbg-lvl is above 0, assuming "ffnn.valdata" key exists in params.
 * <li> 2020-09-17: added feature to drop-out a percentage of random nodes in
 * the hidden layers during a training epoch; also added feature to fix a 
 * percentage of weights to constant values throughout a training epoch.
 * <li> 2020-09-18: when validation accuracy is set for display, it also shows
 * cost function value on the (entire) training set.
 * <li> 2021-09-24: removed the PDBatchTaskExecutor <CODE>_extor</CODE> data 
 * member from the class since it was unused.
 * <li>2021-10-16: completed adding distributed processing capabilities to this
 * class.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 2.0
 */
final public class PDMiniBatchBackPropagation implements LocalOptimizerIntf, 
	                                                 IncumbentProviderIntf {

	private Messenger _mger = Messenger.getInstance();
	
	private HashMap _params;  // null by default

	private FFNN4TrainB _ffnn;  // null by default
	
	private double[][] _allTrainData = null;
	private double[] _allTrainLabels = null;
	private int _mbSize = 1;
	
	// default values for decaying learning rates
	private double _c1 = 10.0;
	private double _c2 = 10.0;
	// indicates whether to normalize the gradient to have L2-norm of 1 or not
	private boolean _normalizeGrad = false;
	
	// fixed weights percentage among all weights during a training epoch
	double _fixedWgtsPerc = 0.0;
	// percentage of hidden nodes to be dropped out during a training epoch
	double _dropoutRate = 0.0;
	
	private double[] _inc;
	private double _incVal = Double.MAX_VALUE;
	
	
  /**
   * public no-arg constructor.
   */
  public PDMiniBatchBackPropagation() {
  }


  /**
   * public constructor. The parameters passed in the argument are copied
   * in the data member <CODE>_params</CODE> so that later modifications to the 
	 * argument do not affect this object or its methods.
   * @param params HashMap
	 * @see <CODE>setParams()</CODE>
   */
  public PDMiniBatchBackPropagation(HashMap params) {
    try {
      setParams(params);
    }
    catch (OptimizerException e) {
      // no-op: cannot reach this point
    }
  }


  /**
   * return a copy of the parameters. Modifications to the returned object
   * do not affect the data member.
   * @return HashMap
   */
  public synchronized HashMap getParams() {
    return new HashMap(_params);
  }


  /**
   * return a new empty <CODE>PDMiniBatchBackPropagation</CODE> optimizer object 
	 * (that must be configured via a call to <CODE>setParams(p)</CODE> before it
	 * can be used).
   * @return LocalOptimizerIntf
   */
  public LocalOptimizerIntf newInstance() {
    return new PDMiniBatchBackPropagation();
  }


  /**
   * the optimization params are set to a copy of p.
   * @param p HashMap should contain values for keys "ffnn.traindata", 
	 * "ffnn.trainlabels" and "ffnn.minibatchsize" (default is 1). If the keys
	 * for either of the first two is missing, the data will be read from the 
	 * <CODE>TrainData</CODE> class. May contain the key "pdmbbp.numthreads"
	 * in which case a <CODE>PDBatchTaskExecutor</CODE> of that number of threads
	 * will be created in the class <CODE>FasterFFNN4TrainBGrad</CODE>, but it may
	 * alternatively contain values for the keys "ffnn.pdbtsrv" and "ffnn.port" 
	 * for distributed processing of the gradient computation (in which case, the
	 * entire training set is used as "mini-batch".)
   * @throws OptimizerException if another thread is currently executing the
   * <CODE>minimize(f)</CODE> method of this object or if the data are not in 
	 * the parameters and cannot be read from files either.
   */
  public synchronized void setParams(HashMap p) throws OptimizerException {
    if (_ffnn!=null) 
			throw new OptimizerException("cannot modify parameters while running");
    _params = new HashMap(p);  // own the params
		if (!_params.containsKey("ffnn.traindata") || 
			  !_params.containsKey("ffnn.trainlabels")) {
			if (TrainData.getTrainingVectors()==null) {
				// read training data and labels 
				String traindatafile = (String) _params.get("ffnn.traindatafile");
				String trainlabelsfile = (String) _params.get("ffnn.trainlabelsfile");
				try {
					TrainData.readTrainingDataFromFilesIfNull(traindatafile, 
						                                        trainlabelsfile);
				}
				catch (IOException e) {
					e.printStackTrace();
					throw new OptimizerException("failed to read data from files "+
						                           traindatafile+" and/or "+
						                           trainlabelsfile);
				}
				_allTrainData = TrainData.getTrainingVectors();
				_allTrainLabels = TrainData.getTrainingLabels();
			}
		}
		else {  
			_allTrainData = (double[][]) _params.get("ffnn.traindata");
			_allTrainLabels = (double[]) _params.get("ffnn.trainlabels");
		}
		if (_params.containsKey("ffnn.minibatchsize")) {
			_mbSize = (int) _params.get("ffnn.minibatchsize");
		}
		if (_params.containsKey("ffnn.pdbtsrv") && 
			  _params.containsKey("ffnn.pdbtport")) {
			// override _mbSize if by accident key "ffnn.minibatchsize" exists
			_mbSize = _allTrainLabels.length;
		}
		if (_params.containsKey("pdmbbp.c1")) {
			_c1 = ((Double)_params.get("pdmbbp.c1")).doubleValue();
		}
		if (_params.containsKey("pdmbbp.c2")) {
			_c2 = ((Double)_params.get("pdmbbp.c2")).doubleValue();
		}
		if (_params.containsKey("pdmbbp.normgrad")) {
			_normalizeGrad = ((Boolean)_params.get("pdmbbp.normgrad")).booleanValue();
		}
		if (_params.containsKey("pdmbbp.fixedwgtsperc")) {
			_fixedWgtsPerc = 
				((Double)_params.get("pdmbbp.fixedwgtsperc")).doubleValue();
		}
		if (_params.containsKey("pdmbbp.dropoutrate")) {
			_dropoutRate = 
				((Double)_params.get("pdmbbp.dropoutrate")).doubleValue();
		}
  }
	
	
	/**
	 * return the currently best known solution.
	 * @return Object double[]
	 */
	public synchronized Object getIncumbent() {
		return _inc;
	}
	
	
	/**
	 * return the currently best known solution value.
	 * @return double
	 */
	public synchronized double getIncumbentValue() {
		return _incVal;
	}
	
	
	/**
	 * compares the second argument with the currently best known solution value
	 * and replaces the incumbent if the second argument value is less than the 
	 * currently known best.
	 * @param wgts double[]
	 * @param val double
	 */
	private synchronized void setIncumbent(double[] wgts, double val) {
		if (Double.compare(val, _incVal) < 0) {
			_mger.msg("PDMiniBatchBackPropagation.setIncument(): "+
				        "found better solution value="+val+
				        " (%diff from prev="+100.0*(_incVal-val)/val+")", 0);
			if (_inc==null) {
				_inc = new double[wgts.length];
			}
			for (int i=0; i<wgts.length; i++) {
				_inc[i] = wgts[i];
			}
			_incVal = val;
		}
	}
	
	
	/**
	 * the main method of the class, attempts to minimize the loss function of the
	 * network described by the function passed in as argument, using a parallel
	 * distributed version of the mini-batch classical(?) Back-Propagation 
	 * algorithm for training feed-forward neural networks. The mini-batch allows
	 * everything from online (mini-batch size of 1) training to the original(?)
	 * batch (BP) training (mini-batch size equal to the training set.) The 
	 * parallelism currently allows the parallel evaluation of the (true) gradient
	 * of the function, as well as the parallel/distributed evaluation of the 
	 * function itself at any point. The parallelism is achieved by computing the
	 * terms of the sum comprising the function (or its partial derivatives) at 
	 * different training (inst,lbl) pairs in parallel and independently.
	 * Notice that the following parameters will be used if they existed in the
	 * hashmap passed when calling <CODE>setParams()</CODE> (or alternatively
	 * when this object was constructed):
	 * <ul>
	 * <li>&lt;"pdmbbp.learning_rates",double[]&gt; optional, the learning rates 
	 * (ie the step-sizes in the line search along the direction of steepest 
	 * descent) to be tried to find the next iterate point (the one that gives the 
	 * best function value among all points determined by the learning rates 
	 * provided). Default is just one with value 0.1.
	 * <li>&lt;"pdmbbp.c1", Double&gt; optional, the value to be used as nominator
	 * in the decay rate equation for reducing the learning rates as iterations 
	 * progress. Default is 10.0.
	 * <li>&lt;"pdmbbp.c2", Double&gt; optional, the value to be used as sum term
	 * to the number of epochs that form the denominator in the decay rate 
	 * equation for reducing the learning rates as iterations progress. Default is 
	 * also 10.0.
	 * <li>&lt;"pdmbbp.momentum", Double&gt; optional the "momentum" parameter 
	 * used in most BP implementations. Default is 0.1.
	 * <li>&lt;"ffnn.minibatchsize", Integer&gt; optional the mini-batch size 
	 * that will be used to compute the derivative of the network. Default is 1 
	 * (online learning). Notice that in general, the larger the mini-batch size
	 * the faster an epoch completes!. If the key "ffnn.pdbtsrv" exists (go for
	 * distributed processing), then the value of this parameter, if it exists is
	 * used for the batch-size of each task to submit to the distributed cluster,
	 * else the value 128 is used.
	 * <li>&lt;"pdmbbp.fixedwgtsperc", Double&gt; optional, if present indicates
	 * the (approximate) percentage of weights during an epoch that must remain
	 * fixed and not change by the weight update rule of the SGD. Default is zero.
	 * <li>&lt;"pdmbbp.dropoutrate", Double&gt; optional, if present indicates the
	 * (approximate) percentage of hidden nodes that will be outputting zeros 
	 * during an epoch. Default is zero.
	 * <li>&lt;"pdmbbp.normgrad", Boolean&gt; optional whether the gradient 
	 * computed will also be normalized to have unit L2-norm. Default is false.
	 * <li>&lt;"pdmbbp.num_threads", Integer&gt; optional, the number of threads
	 * that the fast auto-differentiator will use. Default is 1.
	 * <li>&lt;"ffnn.pdbtsrv", String&gt; optional, the IP address of the server
	 * to send distributed tasks for gradient computation. Default is null.
	 * <li>&lt;"ffnn.pdbtport", Integer&gt; optional, the port where pdbtexec 
	 * server listens to. Default is -1. If this key and the previous are present
	 * then the PDMiniBatchBackPropagation class enters distributed computing 
	 * training mode, whereby the gradient computation becomes exact, and the 
	 * minibatchsize is set to the entire training set size.
	 * <li>&lt;"pdmbbp.num_epochs", Integer&gt; optional, the number of "epochs" 
	 * (ie iterations) the algorithm will run for. Default is 100.
	 * <li>&lt;"pdmbbp.shuffle", Boolean&gt; optional, indicates whether to 
	 * present the training instances (and labels) in mini-batch formation in 
	 * random order or not. Notice it is irrelevant when mini-batch size is the 
	 * entire training set or when doing distributed processing. Default is true.
	 * <li>&lt;"pdmbbp.use_var_wgt_var, Boolean&gt; optional indicates whether 
	 * weight initialization is such that every weight is normally distributed 
	 * with zero mean and standard deviation 1/sqrt(fan_in) of the node the weight
	 * leads to (is input to). Default is false.
	 * <li>&lt;"ffnn.traindata", double[][]&gt; optional, the (entire) training
	 * instances set for this given problem. If it doesn't exist, it will be 
	 * fetched from the <CODE>TrainData</CODE> class, if necessary calling first
	 * the method <CODE>TrainData.readTrainingDataFromFiles()</CODE>. In such a
	 * case it may be necessary for the parameters to contain values for the 
	 * parameters "ffnn.traindatafile" and "ffnn.trainlabelsfile" to give to the
	 * latter method mentioned here.
	 * <li>&lt;"ffnn.trainlabels", double[]&gt; optional, the labels for the 
	 * (entire) training set. Exactly the same notes as for the "ffnn.traindata"
	 * parameter key hold here too.
	 * </ul>
	 * @param f FunctionIntf  FFNN4TrainB object
	 * @return PairObjDouble // Pair&lt;double[] wgts, Double val&gt;
	 * @throws OptimizerException if another thread is concurrently running this
	 * method of this object, or if an invalid (or null) network is passed as 
	 * argument
	 */
	public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null || !(f instanceof FFNN4TrainB)) {
			throw new OptimizerException("PDMiniBatchBackPropagation.minimize(): "+
				                           "null or invalid f passed in");
		}
		try {
			synchronized (this) {
				if (_ffnn != null)
					throw new OptimizerException("PDMiniBatchBackPropagation.minimize():"+
																			 " another thread is concurrently"+
																			 " executing the method on this object");
				_ffnn = (FFNN4TrainB) f;
				// ensure ffnn is initialized
				if (!_ffnn.isInitialized()) {
					int num_input_signals = _allTrainData[0].length;
					_ffnn.finalizeInitialization(num_input_signals);
				}
				_inc = null;
				_incVal = Double.MAX_VALUE;  // reset
			}
			// do the work
			double[] learning_rates = 
				(double[]) _params.get("pdmbbp.learning_rates");
			if (learning_rates==null) {
				// just use a=0.1
				learning_rates = new double[]{0.1};
			}
			// get momentum value (default 0.1)
			final double momentum = 
				_params.containsKey("pdmbbp.momentum") ?
			    ((Double)_params.get("pdmbbp.momentum")).doubleValue() : 0.1;
			// get number of epochs (default 100)
			final int num_epochs = 
				_params.containsKey("pdmbbp.num_epochs") ?
					((Integer)_params.get("pdmbbp.num_epochs")).intValue() :
					100;  // default			
			final int nt = 
				_params.containsKey("pdmbbp.num_threads") ?
				  ((Integer) _params.get("pdmbbp.num_threads")).intValue() :
				  1;  // default
			final String pdbtsrv = 
				_params.containsKey("ffnn.pdbtsrv") ?
				  (String) _params.get("ffnn.pdbtsrv") : 
				  null;  // default
			final int pdbtport = 
				_params.containsKey("ffnn.pdbtport") ?
				  ((Integer)_params.get("ffnn.pdbtport")).intValue() :
				  -1;  // default
			final boolean go_distr = pdbtsrv!=null && pdbtport>0;
			final int div = _allTrainData.length / _mbSize;
			final int rem = _allTrainData.length % _mbSize;
			final boolean shuffle = 
				_params.containsKey("pdmbbp.shuffle") ?
				  ((Boolean) _params.get("pdmbbp.shuffle")).booleanValue() :
				  true;  // default
			double[] wgts = null;
			if (_params.containsKey("pdmbbp.x0")) { 
				wgts = (double[]) _params.get("pdmbbp.x0");
			}
			else {  // randomly initialize weights
				wgts = new double[_ffnn.getTotalNumWeights()];
				Random r = RndUtil.getInstance().getRandom();
				Boolean use_varying_wgt_varB = 
					(Boolean) _params.get("pdmbbp.use_var_wgt_var");
				final boolean uvwv = 
					use_varying_wgt_varB == null ? false : 
					                               use_varying_wgt_varB.booleanValue();
				// for (int i=0; i<wgts.length; i++) wgts[i] = r.nextGaussian();
				int fan_in = _allTrainData[0].length + 1;
				double s = 1.0/Math.sqrt((double)fan_in);
				int k = 0;
				// hidden node weights
				for (int i=0; i<_ffnn.getNumHiddenLayers(); i++) {
					int num_wgts_i = _ffnn.getHiddenLayers()[i].length*fan_in;
					for (int j=0; j<num_wgts_i; j++) {
						wgts[k] = r.nextGaussian();
						if (uvwv) wgts[k] *= s;
						++k;
					}
					fan_in = _ffnn.getHiddenLayers()[i].length+1;
					s = 1.0/Math.sqrt((double)fan_in);
				}
				// output node weights
				final int num_wgts_out = fan_in;
				for (int j=0; j<num_wgts_out; j++) {
					wgts[k] = r.nextGaussian();
					if (uvwv) wgts[k] *= s;
					++k;
				}
				if (k!=wgts.length) // sanity test
					throw new Error("k="+k+" but wgts.len="+wgts.length);
				
				// but set biases to zero:
				int[] biasinds = _ffnn.getIndices4BiasInWgts(_allTrainData[0].length);
				for (int i=0; i<biasinds.length; i++) wgts[biasinds[i]] = 0.0;
			}
			
			// set target number of fixed weights
			final int num_fixed_wgts = (int) (_fixedWgtsPerc*wgts.length);
			// set target number of drop-out nodes
			final int num_hidden_layers = _ffnn.getNumHiddenLayers();
			int num_hidden_nodes=0;
			for (int i=0; i<num_hidden_layers; i++) {
				num_hidden_nodes += _ffnn.getHiddenLayers()[i].length;
			}
			final int num_dropout_nodes = (int) (_dropoutRate*num_hidden_nodes);
			
			// main loop
			final double[][] train_data_batch = new double[_mbSize][];
			final double[] train_labels_batch = new double[_mbSize];
			final double[] wgts_deriv = new double[wgts.length];
			// distributed processing mode: use the value of mini-batch size specified 
			// in params for batch-size of distributed processing if it exists, else 
			// use default 128
			final int bsize = 
				_params.containsKey("ffnn.minibatchsize") ?
				  ((Integer)_params.get("ffnn.minibatchsize")).intValue() :
				  128;
			final FasterFFNN4TrainBGrad derivator = 
				go_distr==false ? 
				  new FasterFFNN4TrainBGrad(_ffnn, _allTrainData[0].length, nt) :
				  new FasterFFNN4TrainBGrad(_ffnn, _allTrainData[0].length, 
						                        bsize, pdbtsrv, pdbtport);
			DblArray1Vector wgts_vec = new DblArray1Vector(wgts.length);
			// prev_wgts_diff is used for momentum update
			final double[] prev_wgts_diff = new double[wgts.length];  // init to zero
			final List indx = new ArrayList(_allTrainData.length);
			for (int i=0; i<_allTrainData.length; i++) indx.add(new Integer(i));
			Random r = RndUtil.getInstance().getRandom();
			for (int epoch=0; epoch<num_epochs; epoch++) {
				long st = System.currentTimeMillis();
				if (shuffle) Collections.shuffle(indx, r);  // randomly shuffle the data
				_mger.msg("PDMBBackProp: running epoch "+epoch, 1);
				// approximately set the drop-out nodes
				if (num_dropout_nodes>0) {
					int cnt_dropouts = 0;
					final boolean dhl = 
						_ffnn.getOutputNode() instanceof MultiClassSSE ||
						_ffnn.getOutputNode() instanceof CategoricalXEntropyLoss;
					final int do_hls = dhl ? num_hidden_layers-1 : num_hidden_layers;
					for (int j=0; j<do_hls; j++) {
						NNNodeIntf[] nodesj = _ffnn.getHiddenLayers()[j];
						for (int k=0; k<nodesj.length; k++) {
							nodesj[k].setDropout(false);
							if (cnt_dropouts < num_dropout_nodes && 
								  r.nextDouble()<_dropoutRate) {
								nodesj[k].setDropout(true);
								++cnt_dropouts;
							}
						}
					}
				}
				// approximately set the fixed weights
				if (num_fixed_wgts>0) {
					final int total_num_wgts = _ffnn.getTotalNumWeights();
					int cnt_fixedwgts = 0;
					BoolVector fxdWgtInds = _ffnn.getFixedWgtInds();
					for (int j=0; j<total_num_wgts; j++) {
						fxdWgtInds.set(j, false);
						if (cnt_fixedwgts<num_fixed_wgts && 
							  r.nextDouble() < _fixedWgtsPerc) {
							fxdWgtInds.set(j, true);
							++cnt_fixedwgts;
						}
					}
				}
				// prepare train batch
				int pos = 0;
				while (pos<_allTrainData.length-rem) {
					// prepare batch
					if (pos % 1000 == 0 && _mger.getDebugLvl()>=3) {  // costly diagnostic
						// evaluate the current soln
						_params.put("ffnn.traindata", _allTrainData);
						_params.put("ffnn.trainlabels", _allTrainLabels);					
						double val = _ffnn.eval(wgts, _params);
						_mger.msg("PDMBBackProp: running batch from ["+pos+
							        ","+(pos+_mbSize)+") with current val="+val, 3);
					}
					if (!go_distr) {  // go parallel or even serial 
						for (int j=0; j<_mbSize; j++) {
							train_data_batch[j] = 
								_allTrainData[((Integer)indx.get(pos)).intValue()];
							train_labels_batch[j] = 
								_allTrainLabels[((Integer)indx.get(pos++)).intValue()];
						}
						// to compute derivative 
						// first setup the params for this batch
						_params.put("ffnn.traindata", train_data_batch);
						_params.put("ffnn.trainlabels", train_labels_batch);
					}
					else {  // go distributed!
						// to compute derivative, set params to null
						_params.put("ffnn.traindata", null);
						_params.put("ffnn.trainlabels", null);
						pos = _allTrainData.length;  // in distributed mode, we use ALL data
					}
					// compute the derivative automatically
					for (int i=0; i<wgts_deriv.length; i++) wgts_vec.setCoord(i, wgts[i]);
					VectorIntf g = derivator.eval(wgts_vec, _params);
					if (_normalizeGrad) {
						final double g_norm = VecUtil.norm2(g);
						_mger.msg("PDMiniBatchBackPropagation.minimize(): in epoch "+
							        epoch+" mini-batch ||grad||_2="+g_norm,2);
						try {
							g.div(g_norm);
						}
						catch (ParallelException e) {  // can never get here
							e.printStackTrace();
						}
						catch (IllegalArgumentException e2) {
							_mger.msg("PDMiniBatchBackPropagation.minimize(): in epoch "+
								        epoch+" mini_batch ||grad||_2="+g_norm+
								        "; will continue instead",2);
							continue;
						}
					} 
					for (int i=0; i<wgts_deriv.length; i++) wgts_deriv[i] = g.getCoord(i);
					// now try with all the different learning rates to see which gives
					// the best f(new_wgts) value
					double[][] ws = new double[learning_rates.length][];
					int best_i = -1;
					double best_a_val = Double.POSITIVE_INFINITY;
					if (learning_rates.length==1) { // don't evaluate, just update weights
						final double lrate = learning_rates[0]*_c1 / (epoch+_c2);
						for (int j=0; j<wgts.length; j++) {
							prev_wgts_diff[j] = momentum*prev_wgts_diff[j] - 
								                  lrate*wgts_deriv[j];
							wgts[j] += prev_wgts_diff[j];
						}						
					}
					else {
						// compute the error of the network on ws[i] on the WHOLE dataset
						_params.put("ffnn.traindata", _allTrainData);
						_params.put("ffnn.trainlabels", _allTrainLabels);
						//double cur_val = _ffnn.eval(wgts, _params);
						for (int i=0; i<learning_rates.length; i++) {
							final double ai = _c1*learning_rates[i] / (_c2+epoch);
							ws[i] = new double[wgts.length];
							for (int j=0; j<wgts.length; j++) {
								ws[i][j] = wgts[j] - wgts_deriv[j]*ai + 
									                   momentum*prev_wgts_diff[j];
							}
							double err_i = _ffnn.eval(ws[i], _params);
							if (Double.compare(err_i, best_a_val) < 0) {
								best_i = i;
								best_a_val = err_i;
								setIncumbent(ws[i], err_i);
							}
						}
						// pick as weights the ones in best_i
						for (int j=0; j<wgts.length; j++) {
							prev_wgts_diff[j] = ws[best_i][j]-wgts[j];  // update changes
							wgts[j] = ws[best_i][j];
						}
					}
				}  // while pos
				if (pos < _allTrainData.length-1) {  // there is a remainder
					// may possibly come here ONLY when go_distr==false
					// don't bother with many learning rates here, just use the first
					final double[][] last_train_data = new double[rem][];
					final double[] last_train_labels = new double[rem];
					int cnt=0;
					for (; pos<_allTrainData.length; pos++) {
						last_train_data[cnt] = 
							_allTrainData[((Integer)indx.get(pos)).intValue()];
						last_train_labels[cnt++] = 
							_allTrainLabels[((Integer)indx.get(pos)).intValue()];
					}
					final double a = learning_rates[0]*_c1 / (_c2+epoch);
					_params.put("ffnn.traindata", last_train_data);
					_params.put("ffnn.trainlabels", last_train_labels);
					/*
					// add "hiddenws$i$" and "outputws" in _params
					setHiddenAndOutputWsInParams(wgts);
					for (int j=0; j<wgts.length; j++) {
						wgts_deriv[j] = _ffnn.evalPartialDerivativeB(wgts, j, _params, 
							                                           false);
					}
					*/					
					// compute the derivative automatically
					for (int i=0; i<wgts_deriv.length; i++) wgts_vec.setCoord(i, wgts[i]);
					VectorIntf g = derivator.eval(wgts_vec, _params);
					if (_normalizeGrad) {
						final double g_norm = VecUtil.norm2(g);
						_mger.msg("PDMiniBatchBackPropagation.minimize(): in epoch "+
							        epoch+" LAST mini-batch ||grad||_2="+g_norm,2);
						try {
							g.div(g_norm);
						}
						catch (ParallelException e) {  // can never get here
							e.printStackTrace();
						}
						catch (IllegalArgumentException e2) {  // but it can get here
							_mger.msg("PDMiniBatchBackPropagation.minimize(): in epoch "+
								        epoch+" mini_batch ||grad||_2="+g_norm+
								        "; will continue instead",0);
							// continue;  // let's allow time duration computation
						}
					}
					for (int i=0; i<wgts_deriv.length; i++) wgts_deriv[i] = g.getCoord(i);
					// update the weights
					for (int j=0; j<wgts.length; j++) {
						wgts[j] -= (wgts_deriv[j]*a - momentum*prev_wgts_diff[j]);
						prev_wgts_diff[j] = momentum*prev_wgts_diff[j] - 
							                  a*wgts_deriv[j];
					}
				}
				long dur = System.currentTimeMillis()-st;
				_mger.msg("PDMBBP.minimize(): epoch "+epoch+" took "+dur+" msecs", 1);

				// clear out drop-out nodes or fixed weights
				if (num_dropout_nodes>0) {
					for (int j=0; j<num_hidden_layers; j++) {
						NNNodeIntf[] nodesj = _ffnn.getHiddenLayers()[j];
						for (int k=0; k<nodesj.length; k++) {
							nodesj[k].setDropout(false);
						}
					}
				}
				if (num_fixed_wgts>0) {
					BoolVector fxdWgtInds = _ffnn.getFixedWgtInds();
					fxdWgtInds.clear();
				}
								
				// measure validation accuracy on this epoch if data exist
				double[][] valdata = (double[][]) _params.get("ffnn.valdata");
				if (valdata!=null && _mger.getDebugLvl()>=1) {
					double[] vallabels = (double[]) _params.get("ffnn.vallabels");
					FunctionIntf cf = 
						(FunctionIntf) _params.get("ffnn.validationcostfunction");
					final double verr = _ffnn.evalNetworkOutputOnValidationData(wgts, 
						                                                          valdata, 
																																			vallabels, 
																																			cf);
					// evaluate the current soln
					_params.put("ffnn.traindata", _allTrainData);
					_params.put("ffnn.trainlabels", _allTrainLabels);
					final double cost = _ffnn.eval(wgts, _params);					
					if (go_distr) {  // go distributed
						_params.put("ffnn.traindata", null);
						_params.put("ffnn.trainlabels", null);						
					}
					// evaluate the overall gradient at the current solution
					final VectorIntf gall = 
						derivator.eval(new DblArray1Vector(wgts), _params);
					final double gall_norm_2 = VecUtil.norm(gall, 2);
					_mger.msg("PDMBBP.minimize(): "+
						        "training (cost function) value="+cost+" "+
						        "||g_ALLDATA||_2="+gall_norm_2+" "+
						        "validation error% afer epoch "+epoch+
						        "="+verr, 1);
				}
			}
			// end main loop
			
			if (_inc==null) {
				_params.put("ffnn.traindata", _allTrainData);
				_params.put("ffnn.trainlabels", _allTrainLabels);				
				double tot_err = _ffnn.eval(wgts, _params);
				setIncumbent(wgts, tot_err);
			}
			return new PairObjDouble(_inc, _incVal);
		}
		finally {
			synchronized (this) {
				_ffnn = null;
			}
		}
	}
	
	
	/**
	 * sets the right values for the "hiddenws$i$" and "outputws" keys based on
	 * the weights passed in.
	 * @param wgts double[]
	 */
	private void setHiddenAndOutputWsInParams(double[] wgts) {
		final int islen = _allTrainData[0].length;
		for (int i=0; i<_ffnn.getNumHiddenLayers(); i++) {
			double[][] wgtsi = _ffnn.getLayerWeightsWithBias(i, wgts, islen);
			_params.put("hiddenws"+i, wgtsi);
		}
		double[] outwgts = _ffnn.getOutputWeightsWithBias(wgts);
		_params.put("outputws", outwgts);
	}
	
}
