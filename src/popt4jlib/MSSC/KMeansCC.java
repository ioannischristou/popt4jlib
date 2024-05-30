package popt4jlib.MSSC;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import popt4jlib.GradientDescent.VecUtil;
import popt4jlib.VectorIntf;
import utils.DataMgr;
import utils.Messenger;
import utils.RndUtil;


/**
 * implements the KMeans|| method for seeds initialization for the K-Means
 * algorithm. Multi-threaded implementation assumes that data-set fits in main
 * memory. Notice that any weights attached to the data points (case of weighted
 * clustering) are taken into account by having the weights of the candidate
 * data-points weigh as much as the sum of the weights of the points assigned to
 * them, instead of the number of the points assigned unto them.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2024</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.1
 */
public class KMeansCC implements ClustererInitIntf {
	/**
	 * <CODE>_data</CODE> is the array of data points to be clustered.
	 */
	final private List _data;  // List<VectorIntf>
	
	/**
	 * <CODE>_Cind</CODE> is the set of indices indicating which of the data in 
	 * the list <CODE>_data</CODE> are included in the current set C.
	 */
	final private Set _CInd;  // Set<Integer>
	
	/**
	 * for the current set of centers C, <CODE>_minPtDist2FromC[i]</CODE> is the 
	 * quantity d(_data[i],C)^2.
	 */
	private double[] _minPtDist2FromC;  
	
	/**
	 * <CODE>_l</CODE> is the parameter &lambda; in K-Means|| algorithm.
	 */
	final private int _l;
	
	/**
	 * the number of threads to use.
	 */
	final private int _nthreads;
	
	/**
	 * the number of rounds to iterate, if it's positive, else use the logy value.
	 */
	final private int _numrounds;
	
	private double[] _pfs;  // double[] holds partial sums of d^2(x,C) for
	                        // several x's and current C.
	private double[] _w;  // double[] holds the final weights of the candidates in
	                      // _CInd, zero for the rest elements
	private int[] _asgn;  // int[] holds the current center index to which the 
	                      // point is currently closest to
	
	final private double[] _weights;  // double[] holds data-point weights [opt]
	
	final private static Messenger _mger = Messenger.getInstance();

	
	/**
	 * first public constructor assumes no weights for the data-points.
	 * @param datapts List  // List&lt;VectorIntf&gt;
	 * @param lambda int the parameter of the algorithm
	 * @param numthreads int the number of threads to use
	 * @param numrounds int if this is positive, denotes the number of rounds to
	 * run the main KMeansCC iteration instead of the logy number.
	 */
	public KMeansCC(List datapts, int lambda, int numthreads, int numrounds) {
		_data = datapts;
		_l = lambda;
		_CInd = new HashSet();
		_nthreads = numthreads;
		_numrounds = numrounds;
		_weights = new double[_data.size()];
		for (int i=0; i<_weights.length; i++) _weights[i] = 1.0;
	}
	

	/**
	 * last public constructor accepts weights for the data-points.
	 * @param datapts List  // List&lt;VectorIntf&gt;
	 * @param weights double[] may be null, else all numbers must be positive
	 * @param lambda int the parameter of the algorithm
	 * @param numthreads int the number of threads to use
	 * @param numrounds int if this is positive, denotes the number of rounds to
	 * run the main KMeansCC iteration instead of the logy number.
	 * @throws IllegalArgumentException if weights is not null and its length is
	 * different than the size of the datapts list.
	 */
	public KMeansCC(List datapts, double[] weights, int lambda, int numthreads, 
		              int numrounds) {
		if (weights!=null && weights.length != datapts.size())
			throw new IllegalArgumentException("datapts and weights arrays sizes "+
				                                 "don't match");
		_data = datapts;
		_l = lambda;
		_CInd = new HashSet();
		_nthreads = numthreads;
		_numrounds = numrounds;
		if (weights==null) {
			_weights = new double[_data.size()];
			for (int i=0; i<_weights.length; i++) _weights[i] = 1.0;	
		}
		else _weights = weights;
	}
	
	
	/**
	 * the main method of the class. Implements the K-Means|| algorithm as 
	 * described in the paper "Scalable K-Means++" by Bahmani, Moseley, Vattani
	 * Kumar and Vassilvitskii, Proc. of the VLDB Endowment, 2012. 
	 * <p>Notes:
	 * <ul>
	 * <li>2024-05-07: There is a slight variation in that we support positive 
	 * weights attached to the data points for weighted-MSSC clustering.
	 * </ul>
	 * @param k int
	 * @return List  // List&lt;VectorIntf&gt;
	 * @throws IllegalArgumentException if k&gt;n, the number of data points to be
	 * clustered, or if k&le;0.
	 */
	public synchronized List getInitialCenters(int k) {
		if (k>_data.size() || k<=0) 
			throw new IllegalArgumentException("wrong k!");
		_minPtDist2FromC = new double[_data.size()];  // zero array elems by def.
		_pfs = new double[_nthreads];
		_w = new double[_data.size()];  // init to zero
		_asgn = new int[_data.size()];
		for (int i=0; i<_asgn.length; i++) _asgn[i]=-1;  // init to -1
		_CInd.clear();
		List final_centers = new ArrayList();
		Random r = RndUtil.getInstance().getRandom();
		// 0. pick initial center point
		int rind = r.nextInt(_data.size());
		_CInd.add(rind);
		// 1. thread initialization "sees" initial _CInd set and _w
		KMeansCCThread[] threads = new KMeansCCThread[_nthreads];
		int from=0;
		int load = _data.size()/_nthreads;
		int to = load-1;
		for (int i=0; i<_nthreads-1; i++) {
			threads[i] = new KMeansCCThread(from,to,i);
			threads[i].start();
			from = to+1;
			to += load;
		}
		threads[_nthreads-1] = new KMeansCCThread(from,_data.size()-1,_nthreads-1);
		threads[_nthreads-1].start();
		_mger.msg("KMeansCC: starting distances update", 1);
		// 2. update dists from C, then compute fi_X(C)
		for (int i=0; i<_nthreads; i++) {
			threads[i].startUpdateDists(null);  // update distances
		}
		//itc-20240507: wait until all threads are done
		for (int i=0; i<_nthreads; i++) {
			threads[i].waitForIdleState();
		}
		_mger.msg("KMeansCC: starting partial fi_X(C) computation", 1);
		for (int i=0; i<_nthreads; i++) {
			threads[i].startPartialFI_X();
		}
		double fi_X=0;
		for (int i=0; i<_nthreads; i++) {
			threads[i].waitForIdleState();
			fi_X += _pfs[i];
		}
		_mger.msg("fi_X(C)="+fi_X, 1);
		int logy = (int) Math.ceil(Math.log(fi_X));
		// 3. for logy times, sample each point in dataset independently
		final List C_new_points = new ArrayList();  // List<Integer>
		final int numiters = _numrounds <= 0 ? logy : _numrounds;
		_mger.msg("KMeansCC): will iterate for "+numiters+" times", 1);
		for (int n=0; n<numiters; n++) {
			_mger.msg("KMeansCC: in iteration "+n+"/"+logy, 1);
			C_new_points.clear();
			// 4. do the sampling and choose the new points
			/* following is slow, serial version
			for (int i=0; i<_data.size(); i++) {
				double pi_C = _minPtDist2FromC[i]*_l / fi_X;
				double rand_val = r.nextDouble();
				if (rand_val <= pi_C) {
					Integer iI = new Integer(i);
					if (!_CInd.contains(iI)) C_new_points.add(iI);
				}
			}
			*/
			// parallel version
			_mger.msg("KMeansCC: sampling in parallel", 1);
			for (int i=0; i<_nthreads; i++) {
				threads[i].startSamplingPts(fi_X);
			}
			for (int i=0; i<_nthreads; i++) {
				threads[i].waitForIdleState();
				C_new_points.addAll(threads[i]._CNewInds);  
			}
			_mger.msg("KMeansCC: selected "+C_new_points.size()+" new pts", 1);
			// 4.1 update dists according to d2(x,C(n+1)) = min{d2(x,C(n)),d2(x,Cnew)}
			_mger.msg("KMeansCC: distance updating in parallel", 1);
			for (int i=0; i<_nthreads; i++) {
				threads[i].startUpdateDists(C_new_points);
			}
			//itc-20240507: wait until all threads are done
			for (int i=0; i<_nthreads; i++) {
				threads[i].waitForIdleState();
			}
			// 4.2 get new fi_X, add C_new_points into _CInd, and repeat
			_mger.msg("KMeansCC: fi_X(C) computing in parallel", 1);
			for (int i=0; i<_nthreads; i++) {
				threads[i].startPartialFI_X();
			}
			_mger.msg("KMeansCC: waiting for all threads to reach idle state", 2);
			fi_X=0;
			for (int i=0; i<_nthreads; i++) {
				threads[i].waitForIdleState();
				fi_X += _pfs[i];
			}
			_mger.msg("fi_X(C)="+fi_X, 1);
			// 5. add to _CInd the new pts
			_CInd.addAll(C_new_points);
		}
		// 5.1 ensure there are at least k points in _C
		while (_CInd.size()<k) {
			int pi = r.nextInt(_data.size());
			_CInd.add(new Integer(pi));
		}
		_mger.msg("KMeansCC: initial cluster set w/ "+_CInd.size()+" pts created.", 
 			        0);
		// 6.-8. final reclustering
		if (_CInd.size()==k) {
			// return the exact points
			Iterator cit = _CInd.iterator();
			while (cit.hasNext()) {
				Integer iI = (Integer) cit.next();
				VectorIntf ci = (VectorIntf)_data.get(iI.intValue());
				final_centers.add(ci.newInstance());
			}
			for (int i=0; i<_nthreads; i++) threads[i].setDone();
			_mger.msg("KMeansCC: done (w/ need of re-clustering)", 0);
			return final_centers;
		}
		List c_ind_list = new ArrayList();
		Iterator it = _CInd.iterator();
		while (it.hasNext()) {
			Integer iI = (Integer) it.next();
			c_ind_list.add(iI);
		}
		_mger.msg("KMEansCC: reclustering", 0);
		/* useless, as it is done within the distance update computation.
		 * also, it is extremely slow as it has to do O(N*|C|) computations;
		 * even in parallel, that is a huge number...
		for (int i=0; i<_nthreads; i++) {
			threads[i].startCWeighting();
		}
		for (int i=0; i<_nthreads; i++) {
			threads[i].waitForIdleState();
		}	
		*/
		_mger.msg("KMeansCC: done computing weights", 3);
		/* wrong way of getting k centers from the current candidates
		double tot_weight = _data.size();
		while (final_centers.size()<k) {
			int rn = r.nextInt(_CInd.size());
			int rn2 = ((Integer)c_ind_list.get(rn)).intValue();
			double p = r.nextDouble();
			if (Double.compare(p, _w[rn2]/tot_weight)<0) {
				VectorIntf crn2 = (VectorIntf) _data.get(rn2);
				final_centers.add(crn2.newInstance());
				tot_weight -= _w[rn2];
				_CInd.remove(new Integer(rn2));
				c_ind_list.remove(rn);
			}
		}
		*/
		for (int i=0; i<_nthreads; i++) threads[i].setDone();
		List init_centers = new ArrayList();
		double[] w = new double[_CInd.size()];
		Iterator cind_it = _CInd.iterator();
		int cnt=0;
		while (cind_it.hasNext()) {
			final int ii = ((Integer)cind_it.next()).intValue();
			w[cnt++] = _w[ii];
			init_centers.add(_data.get(ii));
		}
		KMeansPP kmeanspp = new KMeansPP(init_centers);
		final_centers = kmeanspp.getInitialCenters(k,w);
		_mger.msg("KMeansCC: done", 0);
		return final_centers;
	} 
	
	
	/**
	 * test-driver method for KMeansCC, not to be used as part of the public API.
	 * Usage:
	 * <CODE>
	 * java -cp &lt;classpath&gt; popt4jlib.MSSC.KMeansCC 
	 * &lt;input_vectors_file&gt; &lt;k&gt; &lt;lambda&gt; 
	 * [numthreads(4)] 
	 * [numrounds(-1)]
	 * [weights_file(null)]
	 * [dbglvl(Integer.MAX_VALUE-&gt;print all msgs)]
	 * </CODE>.
	 * @param args 
	 */
	public static void main(String[] args) {
		try {
			String input_file = args[0];
			Vector datapts0 = DataMgr.readSparseVectorsFromFile(input_file);
			// Vector is synchronized, and we need asynch access to the data
			// code below is sub-optimal but avoids rewriting DataMgr.readXXX() method
			List datapts = new ArrayList();
			datapts.addAll(datapts0);
			datapts0.clear();
			int k = Integer.parseInt(args[1]);
			int l = Integer.parseInt(args[2]);
			int nthreads = 4;
			if (args.length>3) nthreads = Integer.parseInt(args[3]);
			int numrounds = -1;
			if (args.length>4) numrounds = Integer.parseInt(args[4]);
			double[] weights = null;
			if (args.length>5) {
				weights = DataMgr.readDoubleLabelsFromFile(args[5]);
			}
			if (args.length>6) {
				int dbglvl = Integer.parseInt(args[6]);
				Messenger.getInstance().setDebugLevel(dbglvl);
			}
			KMeansCC kmcc = new KMeansCC(datapts, weights, l, nthreads, numrounds);
			long start_time = System.currentTimeMillis();
			List centers = kmcc.getInitialCenters(k);
			long dur = System.currentTimeMillis()-start_time;
			System.out.println("KMeans|| computation done in "+dur+" msecs.");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	
	/**
	 * auxiliary nested class, not part of the public API.
	 */
	private class KMeansCCThread extends Thread {
		private final static int _IDLE=-1;
		private final static int _UPDATE_DISTS=0;
		private final static int _COMPUTE_PARTIAL_FI_X=1;
		private final static int _SAMPLE_PTS=2;
		private final static int _COMPUTE_WEIGHTS=3;
		private final static int _IS_DONE=4;
		private int _fromInd;
		private int _toInd;
		private int _myind;
		private List _CNewInds;  // List<Integer>, may be null, is unique per thread
		private double _fi_X;
		private volatile int _cmd=-1;  // -1=idle, 0=updateDist, 
                                   // 1=computePartialF, 2=samplePoints
		                               // 3=computeWeights, 4=done
		
		KMeansCCThread(int from, int to, int myind) {
			_fromInd = from;
			_toInd = to;
			_myind = myind;
		}
		
		public void run() {
			long dur = 0;
			final long startTime = System.currentTimeMillis();
			long idleMillis = 0;
			while (_cmd!=_IS_DONE) {
				synchronized(this) {
					dur = System.currentTimeMillis();
					while (_cmd==_IDLE) {
						try {
							wait();
						} 
						catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
						}
					}
					dur = System.currentTimeMillis()-dur;
					idleMillis += dur;
					switch (_cmd) {
						case _UPDATE_DISTS: 
							updateDists();
							_cmd = _IDLE;
							break;
						case _COMPUTE_PARTIAL_FI_X:
							double pf = getPartialF();
							_pfs[_myind] = pf;
							_cmd = _IDLE;
							break;
						case _SAMPLE_PTS:
							samplePoints();
							_cmd = _IDLE;
							break;
						case _COMPUTE_WEIGHTS:
							computeWeights();
							_cmd = _IDLE;
							break;
						case _IS_DONE:
							long now = System.currentTimeMillis();
							long tot_dur = now-startTime;
							double ip = 100.0*idleMillis/((double)tot_dur);
							_mger.msg("T#"+_myind+": spent "+idleMillis+
								                 " msecs idling ("+ip+"% of total time="+
								                 tot_dur+")",0);
							break;
						default:
							_cmd = _IDLE;
							break;
					}
				}
			}
		}
		
		// the following synchronized methods are the "command" methods called from
		// the main program thread (KMEansCC.getInitialCenters(k) method).
		
		private synchronized void startUpdateDists(List Cnew) {
			_cmd = _UPDATE_DISTS;
			//_CNewInds = Cnew;
			if (Cnew!=null) {
				if (_CNewInds==null) _CNewInds = new ArrayList();
				else _CNewInds.clear();
				_CNewInds.addAll(Cnew);
			} // else nothing, _CNewInds is already null
			notify();
		}
		
		
		private synchronized void startPartialFI_X() {
			_cmd = _COMPUTE_PARTIAL_FI_X;
			notify();
		}
		
		
		private synchronized void startSamplingPts(double fi_X) {
			_cmd = _SAMPLE_PTS;
			if (_CNewInds!=null) _CNewInds.clear();
			else _CNewInds = new ArrayList();
			_fi_X = fi_X;
			notify();
		}

		
		/**
		 * unused as it is too expensive to compute weights as a separate step.
		 * Weights are computed within the distance computation method in 
		 * <CODE>updateDists()</CODE>.
		 */
		private synchronized void startCWeighting() {
			_cmd = _COMPUTE_WEIGHTS;
			notify();
		}

		
		private synchronized void waitForIdleState() {
			while (_cmd!=_IDLE) {
				try {
					wait();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
		
		
		private synchronized void setDone() {
			_cmd = _IS_DONE;
			notify();
		}
		
		
		// actual work methods
		
		
		private double getPartialF() {
			long st = System.currentTimeMillis();
			double res = 0.0;
			for (int i=_fromInd; i<=_toInd; i++) {
				res += _minPtDist2FromC[i]*_weights[i];  // itc-20240507: RHS used to be 
				                                         // _minPtDist2FromC[i]
			}
			long dur = System.currentTimeMillis()-st;
			_mger.msg("T#"+_myind+": getPartialF() took "+dur+" msecs.", 3);
			// notify anyone waiting we're done
			synchronized (this) {
				notify();
			}
			return res;
		}
		
		
		private void updateDists() {
			long st = System.currentTimeMillis();
			if (_CNewInds==null) {  // first time
				for (int i=_fromInd; i<=_toInd; i++) {
					VectorIntf xi = (VectorIntf) _data.get(i);
					Iterator it = _CInd.iterator();
					_minPtDist2FromC[i] = Double.MAX_VALUE;
					while (it.hasNext()) {
						int cii = ((Integer) it.next()).intValue();
						VectorIntf ci = (VectorIntf) _data.get(cii);
						double d = VecUtil.getEuclideanDistance(xi, ci);
						d *= d;  // square
						if (Double.compare(d,_minPtDist2FromC[i])<0) {
							_minPtDist2FromC[i] = d;
							_asgn[i] = cii;
						}
					}
				}
				double wtot = 0.0;
				for(int i=0; i<_weights.length; i++) wtot += _weights[i];
				synchronized (_w) {
					_w[_asgn[_fromInd]] = wtot;  // itc2024-0507: used to be _data.size()
				}
			}
			else {  // NOT the first time, compare with new kids in the block
				for (int i=_fromInd; i<=_toInd; i++) {
					VectorIntf xi = (VectorIntf) _data.get(i);
					Iterator it = _CNewInds.iterator();
					while (it.hasNext()) {
						int cii = ((Integer) it.next()).intValue();
						VectorIntf ci = (VectorIntf) _data.get(cii);
						double d = VecUtil.getEuclideanDistance(xi, ci);
						d *= d;  // square
						if (Double.compare(d,_minPtDist2FromC[i])<0) {
							_minPtDist2FromC[i] = d;
							synchronized (_w) {
								if (_asgn[i]>=0) {
									_w[_asgn[i]] -= _weights[i];  // itc-20240507: used to be 
									                              // --_w[asgn[i]];
								}
								_w[cii] += _weights[i];         // itc-20240507: used to be 
								                                // ++_w[cii];
								_asgn[i] = cii;
							}
						}
					}
				}
			}
			long dur = System.currentTimeMillis()-st;
			_mger.msg("T#"+_myind+": updateDists() took "+dur+" msecs.", 3);			
			// notify anyone waiting we're done
			synchronized (this) {
				notify();
			}
		}
		
		
		private void samplePoints() {
			long st = System.currentTimeMillis();
			Random r = RndUtil.getInstance(_myind).getRandom();
			int cnt = 0;
			for (int i=_fromInd; i<=_toInd; i++) {
				double pi_C = _minPtDist2FromC[i]*_weights[i]*_l / _fi_X;  
        // itc-20240507: above RHS didn't include _weights[i]
				double rand_val = r.nextDouble();
				if (rand_val <= pi_C) {
					Integer iI = new Integer(i);
					if (!_CInd.contains(iI)) {
						++cnt;
						_CNewInds.add(iI);
					}
				}
			}
			long dur = System.currentTimeMillis()-st;
			_mger.msg("T#"+_myind+": samplePoints() in ["+_fromInd+","+_toInd+
				        "] added "+cnt+" points in "+dur+" msecs.", 3);			
			// notify anyone waiting we're done
			synchronized (this) {
				notify();
			}
		}
		
		
		/**
		 * useless, as it is very expensive. The weight computation is done inline
		 * within distance updates, in the <CODE>updateDists()</CODE> method.
		 */
		private void computeWeights() {
			final long st = System.currentTimeMillis();
			for (int i=_fromInd; i<=_toInd; i++) {
				final Iterator cind_it = _CInd.iterator();
				Integer c0_ind = (Integer) cind_it.next();
				int best_ind = c0_ind.intValue();
				VectorIntf di = (VectorIntf)_data.get(i);
				VectorIntf c0 = (VectorIntf)_data.get(c0_ind.intValue());
				double minw = VecUtil.getEuclideanDistance(di,c0);
				while (cind_it.hasNext()) {
					Integer c_ind = (Integer) cind_it.next();
					VectorIntf c = (VectorIntf) _data.get(c_ind);
					double d = VecUtil.getEuclideanDistance(di, c);
					if (Double.compare(d,minw)<0) {
						minw = d;
						best_ind = c_ind.intValue();
					}
				}
				synchronized (_w) {
					_w[best_ind] += _weights[i];  // itc20240507:used to be ++_w[best_ind]
				}
			}
			final long dur = System.currentTimeMillis()-st;
			_mger.msg("T#"+_myind+": computeWeights() took "+dur+" msecs.", 3);
			// notify anyone waiting we're done
			synchronized (this) {
				notify();
			}
		}
		
	}
	
}

