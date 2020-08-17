package popt4jlib.neural;

import popt4jlib.VectorIntf;
import popt4jlib.DblArray1Vector;
import popt4jlib.LocalOptimizerIntf;
import popt4jlib.FunctionIntf;
import popt4jlib.IncumbentProviderIntf;
import popt4jlib.OptimizerException;
import parallel.distributed.PDBatchTaskExecutor;
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
 * gradient (the multiple being the "learning rate"), it evaluates in parallel
 * the new point that emerges when several different learning rates are applied
 * to the gradient, and picks the best one (the one that results in the largest
 * descent of the objective function.)
 * <p>Notes:
 * <ul>
 * <li> 2020-08-15: added diagnostics to print validation error after every
 * epoch when dbg-lvl is above 0, assuming "ffnn.valdata" key exists in params.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDMiniBatchBackPropagation implements LocalOptimizerIntf, 
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
	
	private double[] _inc;
	private double _incVal = Double.MAX_VALUE;
	
	private transient PDBatchTaskExecutor _extor = null;
	
	
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
   */
  public PDMiniBatchBackPropagation(HashMap params) {
    try {
      setParams(params);
    }
    catch (Exception e) {
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
   * return a new empty Adam4FFNN optimizer object (that must be
   * configured via a call to setParams(p) before it is used.)
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
	 * <CODE>TrainData</CODE> class. May also contain the key "pdmbbp.numthreads"
	 * in which case a <CODE>PDBatchTaskExecutor</CODE> of that number of threads
	 * will be created to evaluate the function in the various step-sizes 
	 * concurrently.
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
					TrainData.readTrainingDataFromFiles(traindatafile, trainlabelsfile);
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
		if (_params.containsKey("pdmbbp.c1")) {
			_c1 = ((Double)_params.get("pdmbbp.c1")).doubleValue();
		}
		if (_params.containsKey("pdmbbp.c2")) {
			_c2 = ((Double)_params.get("pdmbbp.c2")).doubleValue();
		}
		if (_params.containsKey("pdmbbp.numthreads")) {
			int nt = ((Integer)_params.get("pdmbbp.numthreads")).intValue();
			if (nt>1) {
				try {
					if (_extor!=null) _extor.shutDown();  // shutdown previous one
					// start new one
					_extor = PDBatchTaskExecutor.newPDBatchTaskExecutor(nt);
				}
				catch (ParallelException e) {  // cannot get here
					e.printStackTrace();  
				}
			}
		}
		if (_params.containsKey("pdmbbp.normgrad")) {
			_normalizeGrad = ((Boolean)_params.get("pdmbbp.normgrad")).booleanValue();
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
	 * the faster an epoch completes!.
	 * <li>&lt;"pdmbbp.normgrad", Boolean&gt; optional whether the gradient 
	 * computed will also be normalized to have unit L2-norm. Default is false.
	 * <li>&lt;"pdmbbp.num_threads", Integer&gt; optional, the number of threads
	 * that the fast auto-differentiator will use. Default is 1.
	 * <li>&lt;"pdmbbp.num_epochs", Integer&gt; optional, the number of "epochs" 
	 * (ie iterations) the algorithm will run for. Default is 100.
	 * <li>&lt;"pdmbbp.shuffle", Boolean&gt; optional, indicates whether to 
	 * present the training instances (and labels) in mini-batch formation in 
	 * random order or not. Notice it is irrelevant when mini-batch size is the 
	 * entire training set. Default is true.
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
			final int nt = _params.containsKey("pdmbbp.num_threads") ?
				               ((Integer) _params.get("pdmbbp.num_threads")).intValue():
				               1;
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
				for (int i=0; i<wgts.length; i++) wgts[i] = r.nextGaussian();
				// but set biases to zero:
				int[] biasinds = _ffnn.getIndices4BiasInWgts(_allTrainData[0].length);
				for (int i=0; i<biasinds.length; i++) wgts[biasinds[i]] = 0.0;
			}
			
			// main loop
			final double[][] train_data_batch = new double[_mbSize][];
			final double[] train_labels_batch = new double[_mbSize];
			final double[] wgts_deriv = new double[wgts.length];
			final FasterFFNN4TrainBGrad derivator = 
				new FasterFFNN4TrainBGrad(_ffnn, _allTrainData[0].length, nt);
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
					for (int j=0; j<_mbSize; j++) {
						train_data_batch[j] = 
							_allTrainData[((Integer)indx.get(pos)).intValue()];
						train_labels_batch[j] = 
							_allTrainLabels[((Integer)indx.get(pos++)).intValue()];
					}
					// compute derivative 
					// first setup the params for this batch
					_params.put("ffnn.traindata", train_data_batch);
					_params.put("ffnn.trainlabels", train_labels_batch);	
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
						/*
						for (int j=0; j<wgts.length; j++) {
							wgts[j] -= (lrate*wgts_deriv[j] - 
								          momentum*prev_wgts_diff[j]);
							prev_wgts_diff[j] = momentum*prev_wgts_diff[j] - 
								                  lrate*wgts_deriv[j];
						}
						*/
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
					_mger.msg("PDMBBP.minimize(): validation error% afer epoch "+epoch+
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
