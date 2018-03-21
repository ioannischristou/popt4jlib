package popt4jlib.network;

import java.util.HashMap;
import java.util.Vector;
import popt4jlib.BoolVector;
import popt4jlib.IntArray1SparseVector;
import popt4jlib.DblArray1SparseVector;
import popt4jlib.DblArray2SparseMatrix;
import utils.DataMgr;
import utils.Messenger;
import utils.PairObjDouble;


/**
 * Multi-threaded auction algorithm for the assignment problem. The algorithm
 * is guaranteed to find the optimal solution of an n &times; n assignment 
 * problem, assuming a feasible solution exists, in a finite amount of time.
 * It also detects infeasibility, so it always terminates in a finite amount of
 * time, though the infeasibility detection is the "easy" method described by
 * Bertsekas (1992), based on the value v_{ij} falling below a threshold, and 
 * may take some time before this happens. For a textbook treatment of this 
 * algorithm, see "Bertsekas, D. &amp; Tsitsiklis, J. (1989) Parallel and 
 * Distributed Computation: Numerical Methods, Prentice-Hall." 
 * The detection of infeasibility is described in 
 * "Bertsekas, D. (1992): Auction Algorithms for Network Flow Problems: A 
 * Tutorial Introduction, Laboratory for Information and Decision Systems 
 * Technical Report LIDS-P-2108, MIT".
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class AsgnAuctionMT {
	private int[][] _userObjects;  // _userObjects[i][j] is the j-th object that 
												         // the i-th user is "interested in".
	private int[][] _aij;  // _aij[i][j] is the "value" that user-i has for 
	                          // object # _userObjects[i][j]
	private double[] _pj;  // _p[j] is the current "price" of the j-th object
	
	private int[] _asgns;  // _asgns[i] is the order-id of the object currently 
	                       // assigned to the i-th user, -1 if user i is not 
	                       // assigned. Example: _asgns[0]=3 means that the first
	                       // user is currently assigned to his 4-th object of 
	                       // interest, i.e. to object with id=_userObjects[0][3]
	private int[] _objAsgns;  // _objAsgns[i] is the id of the user currently 
	                          // assigned to object with id=i, -1 if no user is
	                          // currently assigned to object i.
	private int _C;  // the quantity max|_aij|, used in infeasibility detection
	private double _infThres;  // the threshold for detecting infeasibility
	
	private DblArray2SparseMatrix _bidji;  // _bidji[j][i] is the bid recvd by 
	                                       // obj-id j from user-id i
	
	private BoolVector _s;  // the set of currently assigned users.
	private Object[] _sLocks;  // objects used to synchronize access in ASGN phase
	                           // to the _s data member
	
	private final int _n;  // the length of the assignment problem
	private final double _eps;  // the \epsilon value in the auction algorithm
	
	private HashMap _params;  // the parameters of a single run
	
	private volatile boolean _infeasibility = false;
	

	/**
	 * sole public constructor accepts as arguments the set of objects that each
	 * user is interested in, and the value they get from each of them, if 
	 * assigned to them.
	 * @param userObjects int[][]
	 * @param aij int[][]
	 * @param params HashMap should contain the key-value pair 
	 * &lt;"asgnauctionmt.numthreads",Integer num_threads&gt;
	 */
	public AsgnAuctionMT(int[][] userObjects, int[][] aij, HashMap params) {
		_userObjects = userObjects;
		_aij = aij;
		_n = _userObjects.length;
		_eps = 1.0 / (double) _n - 1.e-8;
		_params = new HashMap(params);
	}
	
	
	/**
	 * runs in parallel the Auction algorithm for the Assignment problem, and 
	 * returns a <CODE>utils.PairObjDouble</CODE> containing as object an
	 * <CODE>int[]</CODE> array containing the assignment of each user to an
	 * object, and the value of the assignment as the second argument of the pair.
	 * @return PairObjDouble
	 */
	public synchronized PairObjDouble maximize() {
		Messenger mger = Messenger.getInstance();
		mger.msg("AsgnAuctionMT.maximize(): entered, _n="+_n, 2);
		_asgns = new int[_n];
		_objAsgns = new int[_n];
		for (int i=0; i<_n; i++) {
			_asgns[i] = -1;
			_objAsgns[i] = -1;
		}
		// compute _C
		_C = Integer.MIN_VALUE;
		for (int i=0; i<_n; i++) {
			int[] ai = _aij[i];
			for (int j=0; j<ai.length; j++) {
				int maij = Math.abs(ai[j]);
				if (maij > _C) _C = maij;
			}
		}
		_infThres = -(_n+_n-1)*_C - (_n-1)*_eps;
		_pj = new double[_n];  // by default, prices are zero
		_s = new BoolVector(_n);
		_sLocks = new Object[(_n+63)/64];
		for (int i=0; i<_sLocks.length; i++) _sLocks[i]=new Object();
		_bidji = new DblArray2SparseMatrix(_n,_n);
		int num_threads = 1;
		try {
			Integer ntI = (Integer) _params.get("asgnauctionmt.numthreads");
			if (ntI.intValue()>=1) num_threads = ntI.intValue();
		}
		catch (Exception e) {
			mger.msg("could not retrieve value for asgnauctionmt.numthreads, "+
				       "will use only one thread", 0);
		}
		Thread[] threads = new Thread[num_threads];
		AuctionAsgnAux[] rtis = new AuctionAsgnAux[num_threads];
		int load = _n / num_threads;
		int from=0;
		int to=load-1;
		for (int i=0; i<num_threads-1; i++) {
			rtis[i] = new AuctionAsgnAux(from, to, from, to);
			threads[i] = new Thread(rtis[i]);
			threads[i].start();
			from=to+1;
			to += load;
		}
		rtis[num_threads-1] = new AuctionAsgnAux(from, _n-1, from, _n-1);
		threads[num_threads-1] = new Thread(rtis[num_threads-1]);
		threads[num_threads-1].start();
		
		int num_cur_assigned=0;
		while (_s.cardinality()<_n) {  // terminate when all users are assigned
			//mger.msg("AsgnAuctionMT.maximize(): currently assigned objects="+_s, 2);
			if (_s.cardinality()>num_cur_assigned) {
				num_cur_assigned = _s.cardinality();
				mger.msg("AsgnAuctionMt.maximize(): currently assigned objects now="+
					       num_cur_assigned, 1);
			}
			// 1. run BIDDING phase
			mger.msg("AsgnAuctionMT.maximize(): running BIDDING PHASE", 2);
			for (int i=0; i<num_threads; i++) {
				rtis[i].runTask(AuctionAsgnAux._RUN_BID);
			}
			// 2. barrier
			mger.msg("AsgnAuctionMT.maximize(): running BARRIER1", 2);
			for (int i=0; i<num_threads; i++) {
				rtis[i].waitForTask();
			}
			if (_infeasibility) {
				break;
			}
			// 3. run ASSIGNMENT phase
			mger.msg("AsgnAuctionMT.maximize(): running ASSIGNMENT PHASE", 2);
			for (int i=0; i<num_threads; i++) {
				rtis[i].runTask(AuctionAsgnAux._RUN_ASGN);
			}
			// 4. barrier
			mger.msg("AsgnAuctionMT.maximize(): running BARRIER2", 2);
			for (int i=0; i<num_threads; i++) {
				rtis[i].waitForTask();
			}
			// 5. clear bid matrix for next iteration
			for (int i=0; i<_n; i++) {
				DblArray1SparseVector bi = _bidji.getIthRow(i);
				synchronized (bi) {
					bi.reset();
				}
			}
			mger.msg("AsgnAuctionMT.maximize(): cleared BIDS", 2);
		}
		// signal end to threads
		for (int i=0; i<num_threads; i++) {
			rtis[i].setDone();
			synchronized (rtis[i]) {
				rtis[i].notify();
			}
		}
		
		// done, now compute final results
		if (_infeasibility) 
			throw new IllegalArgumentException("AsgnAuctionMT.maximize(): "+
				                                 "problem specified is infeasible");
		double val=0.0;
		for (int i=0; i<_n; i++) {
			val += _aij[i][_asgns[i]];
		}
		int[] asgns = new int[_n];
		for (int i=0; i<_n; i++) asgns[i] = _userObjects[i][_asgns[i]];
		PairObjDouble result = new PairObjDouble(asgns, val);
		mger.msg("AsgnAuctionMT.maximize(): done.",2);
		return result;
	}
	
	
	/**
	 * auxiliary inner class for AuctionAsgnMT class, that implements the logic 
	 * both for the ASSIGNMENT as well as the BIDDING phases of the Auction 
	 * algorithm for the Assignment problem. Not part of the public API.
	 */
	class AuctionAsgnAux implements Runnable {
		private int _fromUsrId;
		private int _toUsrId;
		private int _fromObjId;
		private int _toObjId;
		private volatile boolean _done = false;
		private boolean _running = false;
		private int _what2Run = _RUN_BID;
		
		private final static int _RUN_BID=0;
		private final static int _RUN_ASGN=1;

		
		/**
		 * sole constructor. Specifies the limits of the iterations in both phases.
		 * @param fromusrid int
		 * @param tousrid int
		 * @param fromobjid int
		 * @param toobjid int
		 */
		AuctionAsgnAux(int fromusrid, int tousrid, int fromobjid, int toobjid) {
			_fromUsrId = fromusrid;
			_toUsrId = tousrid;
			_fromObjId = fromobjid;
			_toObjId = toobjid;
		}
		
		
		/**
		 * the run method of this Runnable object.
		 */
		public void run() {
			while (!isDone()) {
				go();
			}
		}
		
		
		/**
		 * the method only specifies the phase that the thread executing this 
		 * runnable is into.
		 * @param what2run int one of _RUN_BID or _RUN_ASGN.
		 */
		private synchronized void runTask(int what2run) {
			while (_running) {
				try {
					wait();  // wait for other task to complete
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}				
			}
			_what2Run = what2run;
			_running = true;
			notify();
		}
		
		
		/**
		 * once an "order" is received to run a phase, it invokes the appropriate
		 * method; once it finishes, sets its _running flag to false, and notifies
		 * the master thread.
		 */
		private synchronized void go() {
			while (!_running) {
				if (_done) return;
				try {
					wait();  // wait for order to arrive
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			switch (_what2Run) {
				case _RUN_BID:
					runBidding();
					break;
				case _RUN_ASGN:
					runAsgn();
					break;
				default: 
					throw new IllegalArgumentException("unexpected value for _what2Run");
			}
			// finished, reset flag
			_running = false;
			notify();  // the only thread waiting for me is the one running maximize()
		}
		
		
		/**
		 * implements the bidding phase of the Auction algorithm, for the users 
		 * that this object is responsible for.
		 */
		private void runBidding() {
			Messenger mger = Messenger.getInstance();
			mger.msg("in BIDDING PHASE", 3);
			// Bidding Phase
			for (int i=_fromUsrId; i<=_toUsrId; i++) {
				if (!_s.get(i)) {
					final int ai_len = _userObjects[i].length;
					//double[] v = new double[ai_len];
					int j_star = -1;
					double max_vj = Double.NEGATIVE_INFINITY;
					int j_second_star = -1;
					double max_vj_second = Double.NEGATIVE_INFINITY;
					for (int j=0; j<ai_len; j++) {
						double vj = _aij[i][j]-_pj[_userObjects[i][j]];
						if (vj>max_vj) {  // found new best
							if (j_star>=0) {  // check current best
								j_second_star = j_star;
								max_vj_second = max_vj;
							}
							j_star = _userObjects[i][j];
							max_vj = vj;
						} else {  // check for second best
							if (vj>max_vj_second) {
								j_second_star = _userObjects[i][j];
								max_vj_second = vj;
							}
						} 
					}
					if (max_vj<_infThres) {  // infeasibility detected
						_infeasibility=true;
						mger.msg("INFEASIBILITY DETECTED.",0);
					}
					final double bid_val = _pj[j_star]+max_vj-max_vj_second+_eps;
					synchronized (_bidji.getIthRow(j_star)) {
						_bidji.setCoord(j_star, i, bid_val);
					}
					mger.msg("user-"+i+" places bid of value "+bid_val+
						       " for object-"+j_star+" w/ 2nd best j2="+j_second_star, 3);
				}
			}
		}
		
		
		/**
		 * implements the assignment phase of the Auction algorithm for the objects
		 * that this object is responsible for.
		 */
		private void runAsgn() {
			Messenger mger = Messenger.getInstance();
			mger.msg("in ASSIGNMENT PHASE", 3);
			// Assignment Phase
			for (int i=_fromObjId; i<=_toObjId; i++) {
				double best_bid = Double.NEGATIVE_INFINITY;
				int highest_bidder = -1;
				DblArray1SparseVector bidij = _bidji.getIthRow(i);
				for (int j=0; j<bidij.getNumNonZeros(); j++) {
					double bidj = bidij.getIthNonZeroVal(j);
					if (bidj>best_bid) {
						best_bid = bidj;
						highest_bidder = bidij.getIthNonZeroPos(j);
					}
				}
				// assign highest_bidder to i, and set the current price of object i
				if (best_bid>Double.NEGATIVE_INFINITY) {
					// un-assign any other user currently assigned to object i, and set
					// the object i to the highest_bidder, protecting _s from concurrent
					// modifications
					if (_objAsgns[i]>=0) {
						mger.msg("object-"+i+" currently assigned to user-"+_objAsgns[i]+
							       "; unassigning.",3);
						synchronized(_sLocks[_objAsgns[i]/64]) {
							_s.unset(_objAsgns[i]);
						}
					}
					synchronized (_sLocks[highest_bidder/64]) {
						_s.set(highest_bidder);
					}
					for (int j=0; j<_userObjects[highest_bidder].length; j++) {
						if (_userObjects[highest_bidder][j]==i) {
							_asgns[highest_bidder]=j;
							break;
						}
					}
					_objAsgns[i] = highest_bidder;
					double old_pi = _pj[i];
					_pj[i] = best_bid;
					mger.msg("object-"+i+" recv'd highest bid from user-"+
						                          highest_bidder+
						                          ", new object price="+best_bid+
						                          " (old price was "+old_pi+")", 3);
				}
			}
		}
		
		
		/**
		 * the main thread (running maximize()) calls this method, to wait for the
		 * thread running a phase of this object to finish.
		 */
		private synchronized void waitForTask() {
			while (_running) {
				try {
					wait();  // wait for thread to finish its current task execution
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
		
		
		private boolean isDone() { 
			return _done;
		}
		
		
		private void setDone() {
			_done = true;
		}
	} 
	
	
	/**
	 * invoke as 
	 * <CODE>
	 * java -cp &lt;classpath&gt; popt4jlib.network.AsgnAuctionMT 
	 * &lt;asgn_file&gt; [num_threads(1)] [dbglvl(0)]
	 * </CODE>. The format of the asgn_file is the one described in the javadoc
	 * for <CODE>utils.DataMgr.readIntSparseVectorsFromFile()</CODE>.
	 * @param args String[]
	 */
	public static void main(String[] args) {
		try {
			Messenger mger = Messenger.getInstance();
			mger.setShowTimeStamp();
			// 1. read asng problem data
			long start = System.currentTimeMillis();
			Vector aijs = DataMgr.readIntSparseVectorsFromFile(args[0]);
			long dur = System.currentTimeMillis()-start;
			mger.msg("data read in "+dur+" msecs", 0);
			final int n = aijs.size();
			int[][] userObjects = new int[n][];
			int[][] aij = new int[n][];
			for (int i=0; i<n; i++) {
				IntArray1SparseVector ai = (IntArray1SparseVector) aijs.get(i);
				userObjects[i] = ai.getNonDefIndices();
				aij[i] = ai.getNonDefValues();
			}
			final int num_threads = args.length>=2 ? Integer.parseInt(args[1]) : 1;
			HashMap params = new HashMap();
			params.put("asgnauctionmt.numthreads", new Integer(num_threads));
			final int dbg = args.length >=3 ? Integer.parseInt(args[2]) : 0;
			mger.setDebugLevel(dbg);
			start = System.currentTimeMillis();
			AsgnAuctionMT auctionmt = new AsgnAuctionMT(userObjects, aij, params);
			PairObjDouble result = auctionmt.maximize();
			dur = System.currentTimeMillis()-start;
			mger.msg("Done. Best asgn value="+result.getDouble()+" in "+dur+" msecs.", 
				       0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
}
