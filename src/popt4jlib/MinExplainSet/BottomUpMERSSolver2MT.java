package popt4jlib.MinExplainSet;

import popt4jlib.BoolVector;
import parallel.BoundedMinHeapUnsynchronized;
import parallel.FasterParallelAsynchBatchTaskExecutor;
import parallel.ConditionCounter;
import parallel.TimerThread;
import parallel.ParallelException;
import utils.Pair;
import utils.Messenger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.FileReader;


/**
 * Parallel version of the bottom-up algorithm implementation for solving the 
 * Minimum Explaining RuleSet (MERS) problem. The algorithm is a heuristic 
 * Breadth-First-Search - Beam-Search combination that is controlled by two 
 * parameters: (1) the maximum number of candidate rules to consider adding to a 
 * current solution at any point, and (2) the minimum depth size for Beam-Search
 * to kick in: the latter essentially forces the search from any solution of a 
 * specified size to continue with maximum number of candidate rules to consider
 * from that point on set to 1 (only the best candidate is added to the current
 * solution) and continuing this (beam-) search on the current thread of 
 * execution only. By setting both these parameters to infinity, the algorithm
 * becomes a pure (parallel) Breadth-First-Search for the first solution that 
 * satisfies the minimum covering instances requirement, guaranteeing that if 
 * a feasible solution exists it will be found (but because of the parallel 
 * search, it does not guarantee that the solution is the least cardinality
 * solution.) For problems with tens of thousands of rules, good values for the
 * heuristic parameters are setting <CODE>_K</CODE> around 10, and the 
 * min-depth-for-beam-search between 3 and 5.
 * The MERS problem can be described as follows: we are given a 
 * set of variables (items) X = {x_0, x_1, ..., x_(M-1)} and a set of instances 
 * D = {I_0, I_1,..., I_(d-1)} each of which contain subsets of the vars x_i. 
 * There is also a set of rules S = {r_0, r_1, ..., r_(k-1)}, each of which is 
 * both associated with a subset of X, and with a subset of D. It is assumed 
 * that for each I in D, there is at least one R in S that "satisfies" (or, 
 * "explains") I. The problem is to find a minimum cardinality set Y of 
 * rules r in S, that are sufficient to "explain" all the instances in D, in
 * that for each I in D, there is at least one rule r in Y that is associated
 * with I. Variables, rules, and instances are all identified by consecutive 
 * integer numbers starting at 0.
 * Notice that in this implementation, the information about which variable is
 * associated with what rule, is stored in a <CODE>BoolVector</CODE>, so that
 * there is a <CODE>_v2rA BoolVector[]</CODE>, and _v2rA[i] is a BoolVector 
 * containing those rule-ids that contain variable x_i. Similarly, the array
 * <CODE>BoolVector[] _r2vA</CODE> is such that _r2vA[j] is the BoolVector 
 * containing the ids of those variables participating in rule r_j. And so on,
 * for the <CODE>BoolVector[] _r2iA, _i2rA</CODE> arrays.
 * <p>
 * Notice that in this implementation, it is assumed that ALL rules include the
 * last variable (last bit), so all solutions assume -implicitly- that the last
 * bit is set, but don't take it into account when constructing expanded 
 * solutions. The solution strategy is Breadth-First Search (with possible 
 * cut-offs in the new solution generation to limit the search.) Without any 
 * limits in the search process, the solution generated is guaranteed optimal;
 * else it is necessarily a heuristic.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class BottomUpMERSSolver2MT extends BottomUpMERSSolver {
	private FasterParallelAsynchBatchTaskExecutor _executor=null;
	private ConditionCounter _condCnt = null;
	private static BoolVector _bestResult = null;
	private static int _bestCoverageMT = 0;
	private int _minDepth4BeamSearch = -1;
	
	private static boolean _isRunning = false;
	
	
	/**
	 * public constructor will create a new asynch executor to run the program
	 * in parallel.
	 * @param numVars int the number of variables in this MES problem
	 * @param numRules int the number of rules in this MES problem
	 * @param numInstances int the number of instances in this MES problem
	 * @param rule2vars Map&lt;Integer ruleid, Set&lt;Integer varid&gt; &gt;
	 * @param rule2insts Map&lt;Integer ruleid, Set&lt;Integer instid&gt; &gt;
	 * @param minSatInstRatio double the minimum required coverage ratio of 
	 * instances satisfied by the returned solution over the total instances
	 * satisfied by all the rules
	 * @param numsols2consider int the max number of top solutions added to the 
	 * queue at every step (can be <CODE>Integer.MAX_VALUE</CODE> for no limit in 
	 * the search). This is the <CODE>_K</CODE> field, and must be &ge; 1
	 * @param mindepth4beamsearch int the minimum number of rules in a solution 
	 * for a BeamForming-Search to begin or -1 (the default, indicating no attempt
	 * for beam-search)
	 * @param maxnumrulesallowed int default -1 (no limit) used to cut the search
	 * short
	 * @param numthreads int default 1.
	 * @throws ParallelException if numthreads is &le;0.
	 */
	public BottomUpMERSSolver2MT(int numVars, int numRules, int numInstances,
		                          HashMap rule2vars, HashMap rule2insts, 
												      double minSatInstRatio,
												      int numsols2consider,
												      int mindepth4beamsearch,
												      int maxnumrulesallowed,
												      int numthreads) throws ParallelException {
		super(numVars, numRules, numInstances, rule2vars, rule2insts, 
			    minSatInstRatio, numsols2consider, 
					mindepth4beamsearch, 
					maxnumrulesallowed);
		_executor = FasterParallelAsynchBatchTaskExecutor.
			            newFasterParallelAsynchBatchTaskExecutor(numthreads, false);
    // never run task on current thread
		_condCnt = new ConditionCounter();
		_minDepth4BeamSearch = mindepth4beamsearch >= 0 ? 
			                       mindepth4beamsearch : Integer.MAX_VALUE;
	}
	
	
	/**
	 * protected copy constructor is not part of the public API. Leaves the _K
	 * data member at its default value 0.
	 * Does not create new executors.
	 * @param svr BottomUpMESSolver2MT the object to copy from
	 */
	protected BottomUpMERSSolver2MT(BottomUpMERSSolver2MT svr) {
		super(svr); 
	}
	
	
	/**
	 * the main method of the class, simply calls the <CODE>solve2()</CODE> to do
	 * the work. This method is simply a wrapper that starts the condition counter
	 * that has to go down to zero so that it can notify the main thread running
	 * this method that it can finish (the method awaits until all created tasks
	 * are done.)
	 * @param cur_rules BoolVector cur_rules the initial set of rules to contain
	 * in the result-set, usually should be null
	 * @return BoolVector the minimum rule-set sought; may be null if the process
	 * fails (due to low _K)
	 */
	public BoolVector solve(BoolVector cur_rules) {
		synchronized(BottomUpMERSSolver2MT.class) {
			if (_isRunning) {
				throw new IllegalStateException("BottomUpMERSSolver2MT.solve():"+
					                              " currently running on this JVM");
			}
			//else 
			_isRunning = true;
			_bestResult = null;
			_bestCoverageMT = 0;
		}
		PrintProgressTask ppt = new PrintProgressTask(60000);  // print every minute
		TimerThread tt = new TimerThread(1000, true, ppt);
		tt.start();  // start thread to display progress on the command prompt
		_condCnt.increment();
		ArrayList ts = new ArrayList();
		ts.add(new BUMSMTTask(cur_rules));
		try {
			_executor.executeBatch(ts);
			_condCnt.await();
			return getBestResult();
		}
		catch (ParallelException e) {
			e.printStackTrace();
			return null;
		}
		finally {
			ppt.quit();  // make sure the timer thread stops after solve() is done
			synchronized(BottomUpMERSSolver2MT.class) {
				_isRunning = false;
			}
		}
	}


	/**
	 * the main "work" method. It can actually guarantee to find an optimal (least
	 * cardinality) rule-set that covers the required percentage of instances that
	 * can be covered by the rules, from the starting solution (that is normally
	 * empty), when the mindepth4beamsearch variable in the object constructor is 
	 * set to -1 and there is no limit in the value <CODE>_K</CODE> (the number of
	 * candidates to be included in the current solution at any step); otherwise, 
	 * it is only a heuristic.
	 * @param cur_rules BoolVector normally empty, but can be any set that we 
	 * require to be present in the final solution
	 * @return BoolVector a minimal cardinality vector covering the requested
	 * percentage of instances that are coverable by the rules; may return null
	 * if no solution is found due to a low <CODE>_K</CODE> value set
	 */
	private BoolVector solve2(BoolVector cur_rules) {
		ArrayList greedy_sols = new ArrayList();  // needed in case of Beam-Search
		BottomUpMERSSolver2MT saux = _K > 1 ? 
			new BottomUpMERSSolver2MT(this) : this;
		if (saux!=this) {
			saux._K = 1;
		}
		
		// short-cut
		if (cur_rules!=null && cur_rules.cardinality()>_maxNumRulesAllowed) 
			return null;
		
		// second short-cut if we're done
		if (getBestCoverageMT() >= _minNumSatInsts) {
			return null;
		}
		
		try {
			Messenger mger = Messenger.getInstance();
			// BFS order
			BoundedMinHeapUnsynchronized minHeap = 
				new BoundedMinHeapUnsynchronized(_numRules);
			BoolVector cur_rules_max = new BoolVector(cur_rules);

			// 0. check for threshold
			if (cur_rules.cardinality()>_maxNumRulesAllowed) return null;
				
			// 1. are we done yet?
			BoolVector sat_insts = getSatInsts(cur_rules);
			int si_card = sat_insts.cardinality();
			// notice that because if-stmt below is not within a class-synchronized
			// block, it is possible that the best_coverage printout messages may 
			// appear to NOT be monotonically increasing.
			int best_cov = getBestCoverageMT();
			if (si_card > best_cov) {
				mger.msg("BottomUpMERSSolver2MT.solve2(): best_coverage="+si_card+
								 " out of target="+_minNumSatInsts+" with rules="+
								 cur_rules.toStringSet(), 0);
				updateBestCoverageMT(si_card);
			}
			if (si_card>=_minNumSatInsts) {
				greedy_sols.add(new BoolVector(cur_rules));
				return getBest(greedy_sols);
			}
			// 1.1 maybe we can't be done ever?
			// BoolVector cur_vars_max = new BoolVector(cur_vars);
			for (int rid = cur_rules.lastSetBit()+1; rid < _numRules; rid++) 
				cur_rules_max.set(rid);
			BoolVector si = getSatInsts(cur_rules_max);
			int si_card_max = si.cardinality();
			if (si_card_max<_minNumSatInsts) {
				return null;
			}  // fails 
			// 2. consider the best vars to add at this point.
			//    All vars to consider have ids above the highest-id in cur_vars
			minHeap.reset();
			for (int rid = cur_rules.lastSetBit()+1; rid < _numRules; rid++) {
				// check if rid makes sense to add given cur_rules
				if (isRedundant(rid, sat_insts)) continue;
				double cv = getCost(cur_rules, rid);
				minHeap.addElement(new Pair(new Integer(rid), new Double(cv)));
			}
			// remove the top _K rules to consider adding them to the current soln.
			final boolean do_parallel = 
				_executor!=null && cur_rules.cardinality()<_minDepth4BeamSearch;
			ArrayList ts_par = new ArrayList(_K);  // init. size _K
			ArrayList ts_ser = new ArrayList(_K);  // init. size _K
			for (int i=0; i<_K; i++) {
				if (minHeap.size()==0) break;
				Pair toppair = (Pair) minHeap.remove();
				int rid = ((Integer)toppair.getFirst()).intValue();
				BoolVector crid = new BoolVector(cur_rules);
				crid.set(rid);
				if (do_parallel) {
					ts_par.add(new BUMSMTTask(crid));
				}
				else ts_ser.add(crid);
			}
			if (do_parallel) {
				_condCnt.add(ts_par.size());
				_executor.executeBatch(ts_par);
			}
			else {
				for (int i=0; i<ts_ser.size(); i++) {
					BoolVector crid = (BoolVector) ts_ser.get(i);
					BoolVector sol = saux.solve2(crid);
					if (sol==null) {
						//mger.msg("BottomUpMERSSolver2MT.solve2() greedy solver failed "+
						//	       "to produce a valid solution", 0);
						continue;
					}  // failed
					//	mger.msg("BottomUpMERSSolver2MT.solve2() greedy solver "+
					//		       "produced a new solution: "+sol.toStringSet(), 0);
					greedy_sols.add(new BoolVector(sol));
				}
			}  // for i in [0..._K-1]
			return getBest(greedy_sols);  // null if _K is chosen too small...
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * get the current number of tasks in queue.
	 * @return int
	 */
	public int getNumTasksInQueue() {
		return _executor.getNumTasksInQueue();
	}
	
	
	/**
	 * provides synchronized access to the <CODE>_bestCoverageMT</CODE> field.
	 * @return int
	 */
	public static synchronized int getBestCoverageMT() {
		return _bestCoverageMT;
	}
	
	
	/**
	 * synchronized update (if it must) of the <CODE>_bestCoverageMT</CODE> field.
	 * @param cov int
	 * @return new best coverage (may be same as old)
	 */
	private static synchronized int updateBestCoverageMT(int cov) {
		if (_bestCoverageMT<cov)
			_bestCoverageMT = cov;
		return _bestCoverageMT;
	}
	
	
	/**
	 * compute the number I of instances the solution rules+{rid} covers, and 
	 * return the number (|rules|+1)/(I+1). It is expected that rid is not set in
	 * rules.
	 * @param rules BoolVector 
	 * @param rid int
	 * @return double
	 */
	protected double getCost(BoolVector rules, int rid) {
		rules.set(rid);
		BoolVector satInsts = getSatInsts(rules);
		double res = (rules.cardinality()+1) / (double)(satInsts.cardinality()+1.0);
		rules.unset(rid);
		return res;
	}
	
	
	/**
	 * we define "best" as the rule-set with the maximum coverage of instances, 
	 * with ties between max coverage solutions resolved in favor of the shortest
	 * solution. This is in contrast to the <CODE>getBest()</CODE> implementation
	 * of the <CODE>BottomUpMESSolver</CODE> that works with variable sets instead
	 * of rule-sets.
	 * @param sols
	 * @return 
	 */
	private BoolVector getBest(ArrayList sols) {
		int best_card = Integer.MAX_VALUE;
		int best_cov = 0;
		BoolVector res = null;
		for (int i=0; i<sols.size(); i++) {
			final BoolVector si = (BoolVector) sols.get(i);
			final int si_card = si.cardinality();
			final int si_cov = getSatInsts(si).cardinality();
			if (si_cov>best_cov) {
				res = si;
				best_cov = si_cov;
				best_card = si_card;
			}
			else if (si_cov==best_cov && si_card<best_card) {
				res = si;
				best_card = si_card;
			}
		}
		if (res!=null) {
			synchronized (BottomUpMERSSolver2MT.class) {  // see if _bestResult must 
				                                            // be updated as well
				if (_bestResult==null) {
					_bestResult = res;
				}
				else {
					final int br_cov = getSatInsts(_bestResult).cardinality();
					if (br_cov < best_cov) {
						_bestResult = res;
					}
					else if (br_cov==best_cov && 
									 _bestResult.cardinality() > best_card) 
						_bestResult = res;
				}
			}
		}
		return res;
	}
	
	
	/**
	 * get the best result of the whole run.
	 * @return BoolVector
	 */
	private synchronized static BoolVector getBestResult() {
		return _bestResult;
	}
	
	
	/**
	 * auxiliary inner-class not part of the public API.
	 */
	final class BUMSMTTask implements Runnable {
		private BoolVector _cur_rules;
		
		
		public BUMSMTTask(BoolVector cur_rules) {
			_cur_rules = cur_rules;
		}
		
		
		public void run() {
			/*
			if (_executor==null) {  // sanity test
				System.err.println("saux running fom BUMSMTTask");
				System.exit(-1);
			}
			*/
			solve2(_cur_rules);
			_condCnt.decrement();
		}
	}
	
	
	/**
	 * auxiliary inner class, NOT part of the public API. Used for printing
	 * progress information periodically.
	 */
	final class PrintProgressTask implements Runnable {
		private long _intvl;
		private volatile boolean _cont = true;
		private Messenger _mger = Messenger.getInstance();
		
		/**
		 * sole constructor.
		 * @param intvl long msecs to pass between two successive print-outs
		 */
		public PrintProgressTask(long intvl) {
			_intvl = intvl;
		}
		
		
		/**
		 * periodically print progress.
		 */
		public void run() {
			long start = System.currentTimeMillis();
			while (_cont) {
				try {
					Thread.sleep(_intvl);
					long elapsed = (System.currentTimeMillis()-start) / 1000;
					_mger.msg("BottomUpMERSSolver2MT.PrintProgressTask: "+
						        "#tasks_in_queue="+getNumTasksInQueue()+
						        " elapsed time="+elapsed+" seconds", 0);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
		
		
		/**
		 * force <CODE>run()</CODE> method above to return.
		 */
		public void quit() {
			_cont = false;
		}
	}
	
	
	/**
	 * simple test-driver method. Invoke with single parameter the name of the 
	 * file containing the data as follows:
	 * filename format is:
	 * 1st-line: &lt;numVars&gt; &lt;numRules&gt; &lt;numInstances&gt;
	 * rvar &lt;ruleid&gt; &lt;var1&gt; ... &lt;varn&gt;
	 * ...
	 * rinst &lt;ruleid&gt; &lt;inst1&gt; ... &lt;instm&gt;
	 * ...
	 * Note: the program will use all the available cores in the computer it's
	 * running on.
	 * @param args 
	 */
	public static void main(String[] args) {
		String filename = args[0];
		final int numthreads = Runtime.getRuntime().availableProcessors();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			boolean first_line = true;
			int numvars = 0; int numrules = 0; int numinsts = 0;
			HashMap r2vars = new HashMap();
			HashMap r2insts = new HashMap();
			while (true) {
				String line = br.readLine();
				if (line==null) break;
				if (line.length()==0 || line.startsWith("#")) 
					continue;  // ignore comments and/or empty lines
				StringTokenizer tz = new StringTokenizer(line);
				if (first_line) {
					numvars = Integer.parseInt(tz.nextToken());
					numrules = Integer.parseInt(tz.nextToken());
					numinsts = Integer.parseInt(tz.nextToken());
					first_line = false;
					continue;
				}
				String header = tz.nextToken();
				int rid = Integer.parseInt(tz.nextToken());
				Set oids = new HashSet();
				while (tz.hasMoreTokens()) {
					int oid = Integer.parseInt(tz.nextToken());
					oids.add(new Integer(oid));
				}
				if (header.equals("rvar")) {
					r2vars.put(new Integer(rid), oids);
				}
				else {
					r2insts.put(new Integer(rid), oids);
				}
			}
			long start = System.currentTimeMillis();
			final double minSatRatio = 0.7;
			final int numsols2propagateateachnode = 10;
			final int mindepth4beamsearch = 5;
			final int maxnumrulesallowed = 15;
			BottomUpMERSSolver2MT slvr = 
				new BottomUpMERSSolver2MT(numvars, numrules, numinsts,
			                           r2vars, r2insts,
					                       minSatRatio, numsols2propagateateachnode, 
					                       mindepth4beamsearch, maxnumrulesallowed, 
					                       numthreads);
			BoolVector init_rules = new BoolVector(numrules);
			BoolVector expl_rules = slvr.solve(init_rules);
			long dur = System.currentTimeMillis()-start;
			System.out.print("Final sol ruleset=");
			for (int i=expl_rules.nextSetBit(0); i>=0; i=expl_rules.nextSetBit(i+1)) {
				System.out.print(i+" ");
			}
			System.out.println("");
			System.out.println("Done in "+dur+" msecs");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
}
