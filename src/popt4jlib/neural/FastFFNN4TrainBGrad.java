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


/**
 * FastFFNN4TrainBGrad implements the gradient function of an FFNN4TrainB feed
 * forward neural network function but faster than FFNN4TrainBGrad because it
 * doesn't compute each partial derivative with separate function calls, but 
 * all in the same call per training instance, thus maintaining in the cache the
 * computations. It uses the auto-differentiation functionality built-into the 
 * <CODE>FFNN4TrainB</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FastFFNN4TrainBGrad implements VecFunctionIntf {
	private FFNN4TrainB _ffnn;
	
	private PDBatchTaskExecutor _extor = null;
	
	
	/**
	 * public constructor assumes that the network has been fully initialized with
	 * appropriate call to its <CODE>finalizeInitialization(n)</CODE> method 
	 * before invoking this constructor.
	 * @param ffnn FFNN4TrainB
	 */
	public FastFFNN4TrainBGrad(FFNN4TrainB ffnn) {
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
	public FastFFNN4TrainBGrad(FFNN4TrainB ffnn, int num_input_signals) {
		this(ffnn);
		if (!_ffnn.isInitialized())
			_ffnn.finalizeInitialization(num_input_signals);
	}
	
	
	public FastFFNN4TrainBGrad(FFNN4TrainB ffnn, int num_input_signals, 
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
		// itc: HERE above functionality is missing
		final int num_train_instances = traindata.length;
		if (_extor==null) {  // do things serially
			DblArray1Vector g = new DblArray1Vector(_ffnn.getTotalNumWeights());
			for (int t=0; t<num_train_instances; t++) {
				resetAllCaches();
				final double[] train_instance = traindata[t];
				final double train_label = trainlabels[t];
				final double[] g_t = 
					_ffnn.evalGradient4TermB(wgts, train_instance, train_label);
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
			final int task_size = traindata.length / (_extor.getNumThreads()*10);
			List tasks = new ArrayList();  // List<FastFFNN4TrainBGradTask>
			int start = 0;
			int end = task_size;
			while (end <= traindata.length) {
				FastFFNN4TrainBGradTask met = 
					new FastFFNN4TrainBGradTask(traindata, trainlabels,
						                          start, end-1,
						                          wgts);
				tasks.add(met);
				start = end;
				end += task_size;
			}
			if (end != traindata.length + task_size) {  // remainder
				FastFFNN4TrainBGradTask met = 
					new FastFFNN4TrainBGradTask(traindata, trainlabels,
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
	class FastFFNN4TrainBGradTask implements TaskObject {
		private double[][] _allTrainData;
		private double[] _allTrainLabels;
		private double[] _wgts;
		private int _start;
		private int _end;
		
		
		public FastFFNN4TrainBGradTask(double[][] traindata, double[] trainlabels,
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
					_ffnn.evalGradient4TermB(_wgts, train_instance, train_label);
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
