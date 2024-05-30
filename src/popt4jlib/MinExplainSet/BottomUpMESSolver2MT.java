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
 * Minimum Explaining (Variables) Set (MES) problem. The algorithm is heuristic 
 * Breadth-First-Search - Beam-Search combination that is controlled by two 
 * parameters: (1) the maximum number of candidate vars to consider adding to a 
 * current solution at any point, and (2) the minimum depth size for Beam-Search
 * to kick in: the latter essentially forces the search from any solution of a 
 * specified size to continue with maximum number of candidate vars to consider
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
 * Notice that in order to speed-up the solution process, as soon as a feasible
 * solution has been found, the algorithm ends; this implies that when more than
 * one thread is used, when the problem has a solution with the given parameter
 * settings for the search, one solution is guaranteed to be found, but which 
 * one is uncertain (it will be the first found by any thread participating in 
 * the search.) When there is no solution, the "best" value will always be 
 * reported in the console printout (and will always be the same), but will not
 * be returned to the caller.
 * The MES problem can be described as follows: we are given a 
 * set of variables (items) X = {x_0, x_1, ..., x_(M-1)} and a set of instances 
 * D = {I_0, I_1,..., I_(d-1)} each of which contain subsets of the vars x_i. 
 * There is also a set of rules S = {r_0, r_1, ..., r_(k-1)}, each of which is 
 * both associated with a subset of X, and with a subset of D. It is assumed 
 * that for each I in D, there is at least one R in S that "satisfies" (or, 
 * "explains") I. The problem is to find a minimum cardinality set Y of 
 * vars x in X, that are sufficient to "explain" all the instances in D, in
 * that for each I in D, there is at least one rule r in S containing only vars
 * from Y that is associated with I. Variables, rules, and instances are all 
 * identified by consecutive integer numbers starting at 0.
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
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class BottomUpMESSolver2MT extends BottomUpMESSolver {
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
	 * @param mindepth4beamsearch int the minimum number of vars in a solution 
	 * for a BeamForming-Search to begin or -1 (the default, indicating no attempt
	 * for beam-search)
	 * @param maxnumrulesallowed int default -1 (no limit) used to cut the search
	 * short
	 * @param numthreads int default 1.
	 * @throws ParallelException if numthreads is &le;0.
	 */
	public BottomUpMESSolver2MT(int numVars, int numRules, int numInstances,
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
	protected BottomUpMESSolver2MT(BottomUpMESSolver2MT svr) {
		super(svr); 
	}
	
	
	/**
	 * the main method of the class, simply calls the <CODE>solve2()</CODE> to do
	 * the work. This method is simply a wrapper that starts the condition counter
	 * that has to go down to zero so that it can notify the main thread running
	 * this method that it can finish (the method awaits until all created tasks
	 * are done.)
	 * @param cur_vars BoolVector cur_vars the initial set of vars to contain
	 * in the result-set, usually should be empty
	 * @return BoolVector the minimum rule-set sought; may be null if the process
	 * fails (due to low _K and/or low _minDepth4BeamSearch parameter values)
	 * @throws IllegalArgumentException if cur_rules is null
	 */
	public BoolVector solve(BoolVector cur_vars) {
		if (cur_vars==null)
			throw new IllegalArgumentException("cur_vars is null");
		synchronized(BottomUpMESSolver2MT.class) {
			if (_isRunning) {
				throw new IllegalStateException("BottomUpMESSolver2MT.solve():"+
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
		ts.add(new BUMSMTTask(cur_vars));
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
			synchronized(BottomUpMESSolver2MT.class) {
				_isRunning = false;
			}
		}
	}


	/**
	 * the main "work" method. It can actually guarantee to find an optimal (least
	 * cardinality) vars-set that covers the required percentage of instances that
	 * can be covered by the rules, from the starting solution (that is normally
	 * empty), when the mindepth4beamsearch variable in the object constructor is 
	 * set to -1 and there is no limit in the value <CODE>_K</CODE> (the number of
	 * candidates to be included in the current solution at any step); otherwise, 
	 * it is only a heuristic.
	 * @param cur_vars BoolVector normally empty, but can be any set that we 
	 * require to be present in the final solution
	 */
	private void solve2(BoolVector cur_vars) {
		// short-cut
		if (cur_vars.cardinality()>_maxNumVarsAllowed) 
			return;
		
		try {
			Messenger mger = Messenger.getInstance();
				
			// 1. are we done yet?
			setLastBit(cur_vars);
			BoolVector sat_insts = getSatInsts(cur_vars);
			final int si_card = sat_insts.cardinality();
			synchronized (BottomUpMERSSolver2MT.class) {
				// second short-cut if we're done
				if (_bestCoverageMT >= _minNumSatInsts) {
					return;
				}  
				if (si_card > _bestCoverageMT) {
					mger.msg("BottomUpMESSolver2MT.solve2(): best_coverage="+si_card+
									 " out of target="+_minNumSatInsts+" with rules="+
									 cur_vars.toStringSet(), 0);
					_bestCoverageMT = si_card;
				}
			}
			if (si_card>=_minNumSatInsts) {
				updateBestResult(cur_vars);
				return;
			}
			unsetLastBit(cur_vars);
			
			// 1.1 maybe we can't be done ever?
			BoolVector cur_vars_max = new BoolVector(cur_vars);
			// BoolVector cur_rules_max = new BoolVector(cur_rules);
			for (int vid = cur_vars.lastSetBit()+1; vid < _numVars; vid++) 
				cur_vars_max.set(vid);
			BoolVector si = getSatInsts(cur_vars_max);
			int si_card_max = si.cardinality();
			if (si_card_max<_minNumSatInsts) {
				return;
			}  // fails 
			// 2. consider the best vars to add at this point.
			//    All vars to consider have ids above the highest-id in cur_vars
			// BFS order of solution cardinality.
			// the min-heap data structure orders candidates in terms of cost, and 
			// ties are broken by order of rule-id (first argument in the pair that
			// enters the heap.)
			BoundedMinHeapUnsynchronized minHeap = 
				new BoundedMinHeapUnsynchronized(_numVars);
			for (int vid = cur_vars.lastSetBit()+1; vid < _numVars; vid++) {
				// check if rid makes sense to add given cur_rules
				if (isRedundant(vid, sat_insts)) continue;
				double cv = getCost(cur_vars, vid);
				minHeap.addElement(new Pair(new Integer(vid), new Double(cv)));
			}
			// remove the top _K rules to consider adding them to the current soln.
			final boolean do_parallel = 
				_executor!=null && cur_vars.cardinality()<_minDepth4BeamSearch;
			final int K = cur_vars.cardinality()==0 ? _K*100 : _K;
			ArrayList ts_par = new ArrayList(K);  // init. size _K
			ArrayList ts_ser = new ArrayList(K);  // init. size _K
			for (int i=0; i<K; i++) {
				if (minHeap.size()==0) break;
				Pair toppair = (Pair) minHeap.remove();
				int vid = ((Integer)toppair.getFirst()).intValue();
				BoolVector cvid = new BoolVector(cur_vars);
				cvid.set(vid);
				if (do_parallel) {
					ts_par.add(new BUMSMTTask(cvid));
				}
				else ts_ser.add(cvid);
			}  // for i in [0...K-1]
			if (do_parallel) {
				_condCnt.add(ts_par.size());
				_executor.executeBatch(ts_par);
			}
			else {
				BottomUpMESSolver2MT saux = _K > 1 ? 
					new BottomUpMESSolver2MT(this) : this;
				if (saux!=this) saux._K = 1;
				for (int i=0; i<ts_ser.size(); i++) {
					BoolVector cvid = (BoolVector) ts_ser.get(i);
					saux.solve2(cvid);
				}
			}  // else if do_parallel
			return;
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
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
	 * close this object's executor if not null.
	 * @throws ParallelException if the method has been called before
	 */
	public void shutDownExecutor() throws ParallelException {
		if (_executor!=null) _executor.shutDown();
	}
	
	
	/**
	 * compute the number I of instances the solution vars+{vid} covers, and 
	 * return a number proportional to (|vars|+1)/(I+1), and also proportional to
	 * the log10 of the vid (so that early var-ids are preferred to later ones.)
	 * It is expected that vid is not set in vars.
	 * @param vars BoolVector 
	 * @param vid int
	 * @return double
	 */
	protected double getCost(BoolVector vars, int vid) {
		setLastBit(vars);
		vars.set(vid);
		BoolVector satInsts = getSatInsts(vars);
		double res = (vars.cardinality()+1) / (double)(satInsts.cardinality()+1.0);
		double rid_pos_cost = 1.0+Math.log10(vid+1);
		res *= rid_pos_cost;  // higher vids have higher cost
		vars.unset(vid);
		unsetLastBit(vars);
		return res;
	}
		
	
	/**
	 * get the best result of the whole run.
	 * @return BoolVector
	 */
	private synchronized static BoolVector getBestResult() {
		return _bestResult;
	}
	
	
	private void updateBestResult(BoolVector cur_vars) {
		final BoolVector si = getSatInsts(cur_vars);
		final int si_card = si.cardinality();
		synchronized (BottomUpMESSolver2MT.class) {
			if (_bestResult==null) {
				_bestResult = cur_vars;
			}
			else {
				final int b_card = getSatInsts(_bestResult).cardinality();
				if (b_card < si_card) {
					_bestResult = cur_vars;
				}
				else if (b_card==si_card && 
					       cur_vars.cardinality() < _bestResult.cardinality()) {
					_bestResult = cur_vars;
				}
			}
		}
	}
	
	
	private static void setLastBit(BoolVector bv) {
		bv.set(bv.reqSize()-1);
	}
	
	
	private static void unsetLastBit(BoolVector bv) {
		bv.unset(bv.reqSize()-1);
	}

		
	/**
	 * auxiliary inner-class not part of the public API.
	 */
	final class BUMSMTTask implements Runnable {
		private BoolVector _cur_vars;
		
		
		public BUMSMTTask(BoolVector cur_vars) {
			_cur_vars = cur_vars;
		}
		
		
		public void run() {
			solve2(_cur_vars);
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
					_mger.msg("BottomUpMESSolver2MT.PrintProgressTask: "+
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
			BottomUpMESSolver2MT slvr = 
				new BottomUpMESSolver2MT(numvars, numrules, numinsts,
			                           r2vars, r2insts,
					                       minSatRatio, numsols2propagateateachnode, 
					                       mindepth4beamsearch, maxnumrulesallowed, 
					                       numthreads);
			BoolVector init_vars = new BoolVector(numvars);
			BoolVector expl_vars = slvr.solve(init_vars);
			long dur = System.currentTimeMillis()-start;
			System.out.print("Final sol varset=");
			for (int i=expl_vars.nextSetBit(0); i>=0; i=expl_vars.nextSetBit(i+1)) {
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
