package popt4jlib.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import parallel.ParallelBatchTaskExecutor;
import parallel.ParallelException;
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
 * Technical Report LIDS-P-2108, MIT". The same text mentions the idea of 
 * spreading an individual person's bid computations across multiple processors
 * when the number of persons bidding for objects is one or just a few. The 
 * implementation supports &epsilon;-scaling to avoid issues when the arc cost
 * coefficients vary significantly, and there are large cost coefficients.
 * Scaling is very important when solving large instances (with problem size 
 * 20000 or more) and with arc costs being in the order of 500 or more.
 * <p>
 * Testing with sparse random assignment problems with problem size up to 50000
 * showed that the speedup obtained by using many threads is unfortunately very
 * small, almost always less than 1.5, even when 4 or 8 threads are utilized.
 * <p> As a final notice, due to the sparse integer vector implementation used,
 * the problem must be such that arc costs (value a person has for an object)
 * can never be zero (as they are ignored, and the connection is not recorded).
 * If needed, the user should add a constant to all the arc costs of their 
 * problem, so that no zero arc value appears, and then subtract the constant
 * times the size of the problem from the final assignment value.
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
	private int _C = Integer.MIN_VALUE;  // the quantity max|_aij|, used in  
	                                     // infeasibility detection
	private double _infThres;  // the threshold for detecting infeasibility
	
	private DblArray2SparseMatrix _bidji;  // _bidji[j][i] is the bid recvd by 
	                                       // obj-id j from user-id i
	
	private BoolVector _s;  // the set of currently assigned users.
	private Object[] _sLocks;  // objects used to synchronize access in ASGN phase
	                           // to the _s data member
	
	private double _eps;  // the \epsilon value in the auction algorithm
	
	private final int _n;  // the length of the assignment problem
	
	private HashMap _params;  // the parameters of a single run
	
	private volatile boolean _infeasibility = false;
	
	
	/**
	 * above this number, no per-user parallelization of bids occurs.
	 */
	private final static int _MAX_OPEN_USERS = 8;
	/**
	 * this number is also required for per-user parallelization of bids to occur.
	 */
	private final static int _MIN_REQ_OBJS_PER_USR = 1000;
	/**
	 * indicates whether parallel bid calculations within the same user are 
	 * allowed or not; any value &le; 0 indicates no parallel bidding per user.
	 */
	private final int _RUN_PARALLEL_BIDDING_THREADS;
	
	
	/**
	 * sole public constructor accepts as arguments the set of objects that each
	 * user is interested in, and the value they get from each of them, if 
	 * assigned to them.
	 * @param userObjects int[][] the ids of the objects each person is interested
	 * in
	 * @param aij int[][] the values that each person gives to each object of 
	 * interest
	 * @param params HashMap should contain the key-value pair 
	 * &lt;"asgnauctionmt.numthreads",Integer num_threads&gt;. May also contain 
	 * the pair &lt;"asgnauctionmt.runBidsParallelThreads", Integer val&gt; with 
	 * -1 default, indicating 8 threads to be used in parallel bidding per user.
	 * @throws IllegalArgumentException (unchecked) if there is an empty row in 
	 * userObjects or a mismatch in the dimensions of any row in the first two
	 * arguments
	 */
	public AsgnAuctionMT(int[][] userObjects, int[][] aij, HashMap params) {
		_userObjects = userObjects;
		_aij = aij;
		_n = _userObjects.length;
		// perform quick sanity check in the data
		for (int i=0; i<_n; i++) {
			if (_userObjects[i]==null || _userObjects[i].length==0 ||
				  _userObjects[i].length!=_aij[i].length) 
				throw new IllegalArgumentException("row-"+i+" in the inputs doesn't "+
					                                 "check out...");
		}
		_eps = (1.0 / (double) (_n+1));  // < 1/_n
		_params = new HashMap(params);
		_RUN_PARALLEL_BIDDING_THREADS = 
			params.containsKey("asgnauctionmt.runBidsParallelThreads") ? 
			  ((Integer) params.get("asgnauctionmt.runBidsParallelThreads")).
					intValue() : 
			  -1;
		_asgns = new int[_n];
		_objAsgns = new int[_n];
		for (int i=0; i<_n; i++) {
			_asgns[i] = -1;
			_objAsgns[i] = -1;
		}
		// compute _C 
		if (_C==Integer.MIN_VALUE) {
			_C = Integer.MIN_VALUE;
			for (int i=0; i<_n; i++) {
				int[] ai = _aij[i];
				for (int j=0; j<ai.length; j++) {
					int maij = Math.abs(ai[j]);
					if (maij > _C) _C = maij;
				}
			}
		}
		_infThres = -(_n+_n-1)*_C - (_n-1)*_eps;
		_pj = new double[_n];  // by default, prices are zero
		_s = new BoolVector(_n);
		_sLocks = new Object[(_n+63)/64];
		for (int i=0; i<_sLocks.length; i++) _sLocks[i]=new Object();
		_bidji = new DblArray2SparseMatrix(_n,_n);
	}

	
	/**
	 * public constructor used in the epsilon-scaling version of the algorithm.
	 * @param userObjects int[][] the indices of the objects each user is 
	 * interested in
	 * @param aij int[][] the values that each user gives to each object of 
	 * interest
	 * @param pj double[] the ending object prices of the previous iteration, that
	 * will be doubled for this problem
	 * @param params HashMap should contain the key-value pair 
	 * &lt;"asgnauctionmt.numthreads",Integer num_threads&gt;. May also contain 
	 * the pair &lt;"asgnauctionmt.runBidsParallelThreads", Integer val&gt; with 
	 * -1 default, indicating no parallel bidding computation per specific user
	 * @throws IllegalArgumentException (unchecked) if there is an empty row in 
	 * userObjects or a mismatch in the dimensions of any row in the first two
	 * arguments
	 */
	public AsgnAuctionMT(int[][] userObjects, int[][] aij, double[] pj, 
		                   HashMap params) {
		this(userObjects, aij, params);
		double max_pj = Double.NEGATIVE_INFINITY;  // used in computing the lower
		                                           // bound for infeasibility
		                                           // detection
		for (int i=0; i<_n; i++) {
			_pj[i]=2.0*pj[i];
			if (Double.compare(max_pj,_pj[i])<0) max_pj = _pj[i];
		}
		_eps = 1.0;  // this is the epsilon-scaling version
		double inf_thres_neg = (_n+_n-1)*((double)_C) + (_n-1)*_eps + max_pj;
		//_infThres = -(_n+_n-1)*((double)_C) - (_n-1)*_eps - max_pj;
		_infThres = -inf_thres_neg;
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

		private ParallelBatchTaskExecutor _parBidCalcExtor;  // each aux-object
		                                                     // may have an executor
		private int _numExtorThreads=4;
		private List _tasks;  // ArrayList<BidCalcTask>
		
		
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
			// clear executor etc.
			if (_parBidCalcExtor!=null) {
				try {
					_parBidCalcExtor.shutDown();
				}
				catch (ParallelException e) {
					e.printStackTrace();
				}
				_parBidCalcExtor = null;
				_tasks = null;
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
					if (_n-_s.cardinality()<_MAX_OPEN_USERS && 
						  ai_len>_MIN_REQ_OBJS_PER_USR && 
						  _RUN_PARALLEL_BIDDING_THREADS>0) {
						// break-up bid computation into tasks and pass them to executor
						if (_parBidCalcExtor==null) {  // create executor
							_numExtorThreads = _RUN_PARALLEL_BIDDING_THREADS;
							try {
								_parBidCalcExtor=ParallelBatchTaskExecutor.
									               newParallelBatchTaskExecutor(_numExtorThreads);
							}
							catch (ParallelException e) {
								e.printStackTrace();  // not possible
							}
							_tasks = new ArrayList(_numExtorThreads);
							for (int j=0; j<_numExtorThreads; j++) 
								_tasks.add(new BidCalcTask());
						}  // if extor==null
						int load = ai_len / _numExtorThreads;
						int from = 0;
						int to = load-1;
						for (int j=0; j<_numExtorThreads-1; j++) {
							//BidCalcTask bj = new BidCalcTask(i, from, to);
              BidCalcTask bj = (BidCalcTask)_tasks.get(j);
							bj.reset(i,from,to);
							from = to+1;
							to += load;
						}
						//BidCalcTask b_last = new BidCalcTask(i, from, ai_len-1);
						BidCalcTask b_last = (BidCalcTask) _tasks.get(_tasks.size()-1);
						b_last.reset(i,from,ai_len-1);
						try {
							_parBidCalcExtor.executeBatch(_tasks);
						}
						catch (ParallelException e) {
							e.printStackTrace();  // not possible
						}
						// figure out j_star, max_vj and j_second_star and max_vj_second
						int j_star = -1;
						double max_vj = Double.NEGATIVE_INFINITY;
						int j_second_star = -1;
						double max_vj_second = Double.NEGATIVE_INFINITY;
						for (int j=0; j<_tasks.size(); j++) {
							BidCalcTask bj = (BidCalcTask) _tasks.get(j);
							if (Double.compare(bj._v_best,max_vj)>=0) {  // new best; was >
								if (j_star>=0) {  // current best exists
									if (Double.compare(max_vj,bj._v_second_best)>0) {  
                    // make current best 2nd best
										j_second_star = j_star;
										max_vj_second = max_vj;
									} 
									else if (j_second_star==-1 || // 2nd best doesn't exist yet
										       Double.compare(bj._v_second_best,max_vj_second)>0) {  
                    // cur. 2nd best not as good 
										j_second_star = bj._j_second_best;
										max_vj_second = bj._v_second_best;
									} 
								} else {  // current best doesn't exist
									max_vj_second = bj._v_second_best;
									j_second_star = bj._j_second_best;
								}
								max_vj = bj._v_best;
								j_star = bj._j_star;
							} else {  // best in task not good for 1st pos
								if (Double.compare(bj._v_best,max_vj_second)>0) {
									// best in task good for 2nd
									max_vj_second = bj._v_best;
									j_second_star = bj._j_second_best;
								}
							}
						}
						if (max_vj<_infThres) {  // infeasibility detected
							_infeasibility=true;
							mger.msg("INFEASIBILITY DETECTED:"+"user-id="+i+
								       " max_vj="+max_vj+"j_star="+j_star,0);
							if (j_star>=0) 
								mger.msg(" p[j_star]="+_pj[j_star]+" _infThres="+_infThres,
								         0);
							else mger.msg("infinite bid placed so j_star==-1",0);
							break;
						}
						final double bid_val = ai_len==1 ? 
							                       Double.POSITIVE_INFINITY : 
							                       _pj[j_star]+max_vj-max_vj_second+_eps;
						synchronized (_bidji.getIthRow(j_star)) {
							_bidji.setCoord(j_star, i, bid_val);
						}
						mger.msg("user-"+i+" places bid of value "+bid_val+
										 " for object-"+j_star+" w/ 2nd best j2="+j_second_star, 3);						
					}
					else {  // normal bidding phase
						int j_star = -1;
						double max_vj = Double.NEGATIVE_INFINITY;
						int j_second_star = -1;
						int ji = -1;
						double max_vj_second = Double.NEGATIVE_INFINITY;
						for (int j=0; j<ai_len; j++) {
							double vj = _aij[i][j]-_pj[_userObjects[i][j]];
							if (Double.compare(vj,max_vj)>=0) {  // found new best; was >
								if (j_star>=0) {  // check current best
									j_second_star = j_star;
									max_vj_second = max_vj;
								}
								j_star = _userObjects[i][j];
								ji= j;
								max_vj = vj;
							} else {  // check for second best
								if (Double.compare(vj,max_vj_second)>0) {
									j_second_star = _userObjects[i][j];
									max_vj_second = vj;
								}
							} 
						}						
						if (max_vj<_infThres) {  // infeasibility detected
							_infeasibility=true;							
							mger.msg("INFEASIBILITY DETECTED:"+"user-id="+i+
								       " max_vj="+max_vj+"j_star="+j_star,0);
							if (ji>=0) 
								mger.msg(" aij["+i+"]["+ji+"]="+_aij[i][ji]+
								         " p[j_star]="+_pj[j_star]+" _infThres="+_infThres,
								         0);
							else mger.msg("infinite bidding had occurred and thus ji=-1", 0);
							break;  // no need to proceed
						}
						final double bid_val = ai_len==1 ? 
							                       Double.POSITIVE_INFINITY : 
							                       _pj[j_star]+max_vj-max_vj_second+_eps;
						synchronized (_bidji.getIthRow(j_star)) {
							_bidji.setCoord(j_star, i, bid_val);
						}
						mger.msg("user-"+i+" places bid of value "+bid_val+
										 " for object-"+j_star+" w/ 2nd best j2="+j_second_star, 3);
					}
				}  // if i-th user is not yet assigned
			}  // for i in users interval
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
	 * auxiliary class not part of the public API.
	 */
	class BidCalcTask implements Runnable {
		private int _i;
		private int _from;
		private int _to;
		private int _j_star=-1;
		private double _v_best=Double.NEGATIVE_INFINITY;
		private int _j_second_best=-1;
		private double _v_second_best=Double.NEGATIVE_INFINITY;
		
		BidCalcTask() {
			// no-op
		}
		
		
		BidCalcTask(int i, int from, int to) {
			_i = i;
			_from = from;
			_to = to;
		}
		
		
		void reset(int i, int from, int to) {
			// set the input variables
			_i=i;
			_from=from;
			_to=to;
			// reset the output variables
			_j_star = -1;
			_v_best = Double.NEGATIVE_INFINITY;
			_j_second_best = -1;
			_v_second_best = Double.NEGATIVE_INFINITY;
		}
		
		
		public void run() {
			for (int j=_from; j<=_to; j++) {
				double vj = _aij[_i][j]-_pj[_userObjects[_i][j]];
				if (Double.compare(vj,_v_best)>=0) {  // found new best; was >
					if (_j_star>=0) {  // check current best
						_j_second_best = _j_star;
						_v_second_best = _v_best;
					}
					_j_star = _userObjects[_i][j];
					_v_best = vj;
				} else {  // check for second best
					if (Double.compare(vj,_v_second_best)>=0) {  // was >
						_j_second_best = _userObjects[_i][j];
						_v_second_best = vj;
					}
				} 
			}			
		}
	}
	
	
	private static int computeM(int[][] aij) {
		final int n = aij.length;
		int C = Integer.MIN_VALUE;
		for (int i=0; i<n; i++) {
			int[] ai = aij[i];
			for (int j=0; j<ai.length; j++) {
				int maij = Math.abs(ai[j]);
				if (maij > C) C = maij;
			}
		}
		return (int) Math.floor(Math.log((n+n+1)*C)/Math.log(2.0))+1;
	}
	
	
	private static int trunc(double x) {
		if (Double.compare(x,0)>0) return (int) Math.floor(x);
		else if (Double.compare(x,0)<0) return (int) Math.ceil(x);
		else return 0;
	}

	
	/**
	 * invoke as 
	 * <CODE>
	 * java -cp &lt;classpath&gt; popt4jlib.network.AsgnAuctionMT 
	 * &lt;asgn_file&gt; 
	 * [use_epsilon_scaling?(false)] [num_threads(1)] [dbglvl(0)] 
	 * [runBidsParallelThreads?(-1)]
	 * </CODE>. The format of the asgn_file is the one described in the javadoc
	 * for <CODE>utils.DataMgr.readIntSparseVectorsFromFile()</CODE>.
	 * @param args String[]
	 */
	public static void main(String[] args) {
		if (args.length>=2 && "true".equals(args[1]))
			main2(args);
		else main1(args);
	}
	
	
	/**
	 * runs the algorithm without &epsilon;-scaling.
	 * @param args String[]
	 */
	public static void main1(String[] args) {
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
			final int num_threads = args.length>=3 ? Integer.parseInt(args[2]) : 1;
			HashMap params = new HashMap();
			params.put("asgnauctionmt.numthreads", new Integer(num_threads));
			final int dbg = args.length >=4 ? Integer.parseInt(args[3]) : 0;
			mger.setDebugLevel(dbg);
			if (args.length>=5) params.put("asgnauctionmt.runBidsParallelThreads", 
				                             new Integer(args[4]));
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
	
	
	/**
	 * runs the algorithm with &epsilon;-scaling.
	 * @param args 
	 */
	public static void main2(String[] args) {
		try {
			Messenger mger = Messenger.getInstance();
			mger.setShowTimeStamp();
			// 1. read asng problem data
			long start = System.currentTimeMillis();
			Vector aijs = DataMgr.readIntSparseVectorsFromFile(args[0]);
			long dur = System.currentTimeMillis()-start;
			mger.msg("data read in "+dur+" msecs", 0);
			// param setting
			final int num_threads = args.length>=3 ? Integer.parseInt(args[2]) : 1;
			HashMap params = new HashMap();
			params.put("asgnauctionmt.numthreads", new Integer(num_threads));
			final int dbg = args.length >=4 ? Integer.parseInt(args[3]) : 0;
			mger.setDebugLevel(dbg);
			if (args.length>=5) params.put("asgnauctionmt.runBidsParallelThreads", 
				                             new Integer(args[4]));
			
			final int n = aijs.size();
			final int[][] userObjects = new int[n][];
			final int[][] aij = new int[n][];
			for (int i=0; i<n; i++) {
				IntArray1SparseVector ai = (IntArray1SparseVector) aijs.get(i);
				userObjects[i] = ai.getNonDefIndices();
				aij[i] = ai.getNonDefValues();
			}
			int[][] aij2 = new int[n][];
			for (int i=0; i<n; i++) {
				IntArray1SparseVector ai = (IntArray1SparseVector) aijs.get(i);
				aij2[i] = ai.getNonDefValues();
			}
			start = System.currentTimeMillis();
			final int M = computeM(aij);
			AsgnAuctionMT auctionmt=null;
			PairObjDouble result=null;
			// solve the M sub-problems with different aij each time
			for (int m=1; m<=M; m++) {
				mger.msg("AsgnAuctionMT with scaling: solving sub-problem "+m+" / "+M, 
					       0);
				// modify the aij2 values
				for (int i=0; i<n; i++) {
					int[] ai = aij[i];
					for (int j=0; j<ai.length; j++) {
						int aij_new = trunc(((double)ai[j])*(n+n+1.0) / 
							                  Math.pow(2, M-m));  // |N| is the total set 
						                                        // of nodes=2n
						aij2[i][j] = aij_new;
					}
				}
				// now run the m-th sub-problem
				if (m==1) {
					double[] prev_pj = new double[n];
					auctionmt = new AsgnAuctionMT(userObjects, aij2, prev_pj, params);
				}
				else {
					double[] prev_pj = auctionmt._pj;
					auctionmt = new AsgnAuctionMT(userObjects, aij2, prev_pj, params);
				}
				result = auctionmt.maximize();
			}	
			// divide by n+1 the value of the assignment to get the real value
			result = new PairObjDouble(result.getArg(), result.getDouble()/(n+n+1.0));
			dur = System.currentTimeMillis()-start;
			mger.msg("Done. Best asgn value="+result.getDouble()+" in "+dur+" msecs.", 
				       0);
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
}
