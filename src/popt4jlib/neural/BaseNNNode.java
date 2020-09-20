package popt4jlib.neural;

import popt4jlib.BoolVector;
import java.util.HashMap;


/**
 * base class for NNNodeIntf that implements common functionalities of
 * the FFNN nodes.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0 
 */
public class BaseNNNode {
	
	protected int _startWeightInd=-1;
	protected int _endWeightInd=-1;
	protected int _biasInd=-1;
	protected BoolVector _antecedentWeights;
	private int _nodeLayer=-1;
	private int _posInLayer=-1;
	
	private volatile boolean _isDropout = false;
	
	protected FFNN4TrainB _ffnn;
	
	// compile-time constant (minor optimization for the caches)
	private final static double _NaN = new Double(Double.NaN);
	
	/**
	 * the only caches we need: double for lastDerivEval, double[] for lastinputs
	 * and double for lastEval. Add to that the gradient vector cache stored also
	 * as double[].
	 */
	private ThreadLocal _lastDerivEvalCache = new ThreadLocal() {
    protected Object initialValue() {
      return _NaN;
    }
  };
	
	private ThreadLocal _lastInputsCache = new ThreadLocal() {
		protected Object initialValue() {
			return null;
		}
	};
	
	private ThreadLocal _lastEvalCache = new ThreadLocal() {
		protected Object initialValue() {
			return _NaN;
		}
	};

	private ThreadLocal _lastGradVectorCache = new ThreadLocal() {
		protected Object initialValue() {
			return null;
		}
	};
	/**
	 * the _lastDerivEvalCache2 cache holds for a node with activation function
	 * f, the value f'(net_input_sum), which is constant for the same (x,y) 
	 * training pair.
	 */
	private ThreadLocal _lastDerivEvalCache2 = new ThreadLocal() {
    protected Object initialValue() {
      return _NaN;
    }
  };
	

	
	/**
	 * sets the total number of variables (weights plus biases) for the FFNN that
	 * this node participates in. Must be called before any attempts at automatic
	 * differentiation of the network this node participates in.
	 * @param num_weights int
	 */
	public void setTotalNumWeights(int num_weights) {
		if (_antecedentWeights==null)
			_antecedentWeights = new BoolVector(num_weights);
	}
	
	
	/**
	 * return the total number of weight variables (including bias terms) for the 
	 * network this node belongs to. The <CODE>finalizeInitialization(num)</CODE>
	 * method of the containing network must have been called first.
	 * @return int
	 */
	public int getTotalNumWeights() {
		return _antecedentWeights.reqSize();
	}
	
	
	/**
	 * sets the layer in the FFNN that this node belongs to.
	 * @param layerno int can be in {0,...#hidden_layers} with #hidden_layers
	 * indicating the output layer.
	 */
	public void setNodeLayer(int layerno) {
		_nodeLayer = layerno;
	}
	
	
	/**
	 * get the layer number of this node in the FFNN (
	 * @return 
	 */
	public int getNodeLayer() {
		return _nodeLayer;
	}
	
	
	/**
	 * sets the position of this node in the layer containing it.
	 * @param pos_in_layer int in {0,...,#nodes_in_layer-1}
	 */
	public void setPositionInLayer(int pos_in_layer) {
		_posInLayer = pos_in_layer;
	} 
	
	
	/**
	 * returns the position index of this node in the layer it's part of in the 
	 * FFNN.
	 * @return int in {0,...,#nodes_in_layer-1} 
	 */
	public int getPositionInLayer() {
		return _posInLayer;
	}
	
	
	/**
	 * set the network this node belongs to.
	 * @param ffnn FFNN4TrainB
	 */
	public void setFFNN4TrainB(FFNN4TrainB ffnn) {
		_ffnn = ffnn;
	}
	
	
	/**
	 * get the network this node belongs to.
	 * @return FFNN4TrainB
	 */
	public FFNN4TrainB getFFNN4TrainB() {
		return _ffnn;
	}

	
	/**
	 * sets the range of indices in the weights vector variable that are fed into
	 * this node. These are the weights that are directly input to this node and 
	 * includes the bias variable weight for this node (which is indexed at end.)
	 * It also adds these weights as antecedents to all the nodes this node 
	 * connects to.
	 * @param start int inclusive
	 * @param end int inclusive
	 */
	public void setWeightRange(int start, int end) {
		_startWeightInd = start;
		_endWeightInd = end-1;
		_biasInd = end;
		// set these weights as antecedents to all the descendants of this node
		int layer = getHiddenNodeLayer();
		if (layer >= 0) {
			NNNodeIntf[][] hLayers = _ffnn.getHiddenLayers();
			for (int i=layer+1; i<hLayers.length; i++) {
				NNNodeIntf[] layer_i = hLayers[i];
				for (int j=0; j<layer_i.length; j++) {
					NNNodeIntf nij = layer_i[j];
					if (((BaseNNNode)nij)._antecedentWeights==null) 
						nij.setTotalNumWeights(_antecedentWeights.reqSize());
					nij.addPreviousWeightsRange(start, end);
				}
			}
		}
		// set these weights as antecedents of the output node as well
		OutputNNNodeIntf outn = _ffnn.getOutputNode();
		outn.setTotalNumWeights(_antecedentWeights.reqSize());
		outn.addPreviousWeightsRange(start, end);
	}	
	
	
	/**
	 * adds the indices of weight variables that are input to nodes in previous
	 * layers that eventually connect to this one.
	 * @param start int inclusive
	 * @param end int inclusive
	 */
	public void addPreviousWeightsRange(int start, int end) {
		for (int i=start; i<=end; i++) _antecedentWeights.set(i);
	}
	
	
	/**
	 * gets the index of the first weight variable connected directly as  
	 * input to this node.
	 * @return int
	 */
	public int getDirectInputWeightStartIndex() {
		return _startWeightInd;
	}

	
	/**
	 * gets the index of the last weight variable connected directly as  
	 * input to this node.
	 * @return int
	 */
	public int getDirectInputWeightEndIndex() {
		return _endWeightInd;
	}
	
	
	/**
	 * gets the index (in the all weights variable array) of the bias variable 
	 * connected directly as input to this node.
	 * @return int 
	 */
	public int getBiasIndex() {
		return _biasInd;
	}
	
	
	/**
	 * returns true if and only if the index represented a connection weight that
	 * connects to a node that is eventually connected to this node.
	 * @param index int
	 * @return boolean
	 */
	public boolean isWeightVariableAntecedent(int index) {
		return _antecedentWeights.get(index);
	}
	
	
	/**
	 * get the layer index where this node resides.
	 * @return int in [0, #hidden_layers-1] or -1 if this node is not found among
	 * the hidden nodes of this network (ie it's the output node)
	 */
	public int getHiddenNodeLayer() {
		final int num_layers = _ffnn.getNumHiddenLayers();
		final NNNodeIntf[][] layers = _ffnn.getHiddenLayers();
		for (int i=0; i<num_layers; i++) {
			NNNodeIntf[] layeri = layers[i];
			for (int j=0; j<layeri.length; j++) if (layeri[j]==this) return i;
		}
		return -1;
	}
	
	
	/**
	 * catch-all method for NNNodeIntf classes that do not implement this method.
	 * @param weights double[] all variables (including biases) array
	 * @param index int the index of the partial derivative to take
	 * @param inputSignals double[]
	 * @param true_lbl double
	 * @param p HashMap includes the train-data matrix and train-labels array.
	 * @return double
	 */
	public double evalPartialDerivativeB(double[] weights, int index, 
		                                   double[] inputSignals, double true_lbl,
																			 HashMap p) {
		throw new UnsupportedOperationException("method not implemented");
	}

	
	/**
	 * catch-all method for NNNodeIntf classes that do not implement this method.
	 * @param weights double[] all variables (including biases) array
	 * @param index int the index of the partial derivative to take
	 * @param inputSignals double[]
	 * @param true_lbl double
	 * @return double
	 */
	public double evalPartialDerivativeB(double[] weights, int index, 
		                                   double[] inputSignals, double true_lbl) {
		throw new UnsupportedOperationException("method not implemented");
	}

	
	/**
	 * cache last derivative evaluation.
	 * @param val double 
	 */
	protected void setLastDerivEvalCache(double val) {
		if (!Double.isFinite(val)) {
			throw new IllegalStateException("setLastDerivEvalCache(v): NOT finite v");
		}
		_lastDerivEvalCache.set(new Double(val));
	}
	
	
	/**
	 * get the last derivative evaluation.
	 * @return double maybe NaN.
	 */
	protected double getLastDerivEvalCache() {
		return ((Double)_lastDerivEvalCache.get()).doubleValue();
	}

	
	/**
	 * cache last evaluation.
	 * @param val double
	 */
	protected void setLastEvalCache(double val) {
		if (!Double.isFinite(val)) {
			throw new IllegalStateException("setLastEvalCache(v): NOT finite v");
		}
		_lastEvalCache.set(new Double(val));
	}
	
	
	/**
	 * get the last evaluation.
	 * @return double maybe NaN.
	 */
	public double getLastEvalCache() {
		return ((Double)_lastEvalCache.get()).doubleValue();
	}
	
	
	/**
	 * cache last inputs.
	 * @param inputs double[]
	 */
	protected void setLastInputsCache(double[] inputs) {
		_lastInputsCache.set(inputs);
	}
	
	
	/**
	 * get the last inputs.
	 * @return double maybe NaN.
	 */
	public double[] getLastInputsCache() {
		return (double[]) _lastInputsCache.get();
	}


	/**
	 * reset the cache(s) of this node.
	 */
	protected void resetCache() {
		_lastDerivEvalCache.set(_NaN);
		_lastInputsCache.set(null);
		_lastEvalCache.set(_NaN);
	}
	
	
	/**
	 * get the last grad vector. If it's null, a new array is created and stored
	 * in the cache (with NaN values) before being returned to the caller.
	 * @return double[]
	 */
	protected double[] getGradVectorCache() {
		double[] arr = (double[]) _lastGradVectorCache.get();
		if (arr==null) {
			arr = new double[getTotalNumWeights()];  
      for (int i=0; i<arr.length; i++) 
				arr[i] = Double.NaN;  // init to NaN values
			_lastGradVectorCache.set(arr);
		}
		return arr;
	}
	
	
	/**
	 * update the last grad vector of this node.
	 * @param g double[]
	 */
	protected void setGradVectorCache(double[] g) {
		_lastGradVectorCache.set(g);
	}
	
	
	/**
	 * set the i-th coordinate of the last grad vector of this node.
	 * @param i int in {0,1,...#weights-1}
	 * @param gi double the value of the ith component of this node's gradient 
	 */
	protected void setGradVectorCache(int i, double gi) {
		double[] g = (double[]) _lastGradVectorCache.get();
		g[i] = gi;
	}
	
	
	/**
	 * reset all components of the grad vector cache of this node to NaN values.
	 */
	protected void resetGradVectorCache() {
		double[] arr = (double[]) _lastGradVectorCache.get();
		if (arr==null) return;
		for (int i=0; i<arr.length; i++) arr[i] = Double.NaN;
		_lastDerivEvalCache2.set(_NaN);
	}

	
	/**
	 * cache last derivative evaluation for use with the grad-vector caches.
	 * @param val double
	 */
	protected void setLastDerivEvalCache2(double val) {
		if (!Double.isFinite(val)) {
			throw new IllegalStateException("setLastDerivEvalCache2(v): "+
				                              "NOT finite v");
		}
		_lastDerivEvalCache2.set(new Double(val));
	}
	
	
	/**
	 * get the last derivative evaluation that is for use with grad-vector caches.
	 * @return double maybe NaN.
	 */
	protected double getLastDerivEvalCache2() {
		return ((Double)_lastDerivEvalCache2.get()).doubleValue();
	}
	
	
	/**
	 * sets the dropout property of this node.
	 * @param val boolean
	 */
	public void setDropout(boolean val) {
		_isDropout = val;
	}
	
	
	/**
	 * gets the dropout property of this node.
	 * @return boolean
	 */
	public boolean isDropout() {
		return _isDropout;
	}

}

