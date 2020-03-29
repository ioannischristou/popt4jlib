package popt4jlib.MinExplainSet;

import popt4jlib.BoolVector;
import parallel.BoundedMinHeapUnsynchronized;
import parallel.BufferIntf;
import parallel.UnboundedBufferArrayUnsynchronized;
import parallel.BoundedBufferArrayUnsynchronized;
import utils.Pair;
import utils.Messenger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


/**
 * Bottom-up algorithm implementation for solving the Minimum Explaining Set
 * (MES) problem. The MES problem can be described as follows: we are given a 
 * set of variables (items) X = {x_0, x_1, ..., x_(M-1)} and a set of instances 
 * D = {I_0, I_1,..., I_(d-1)} each of which contain subsets of the vars x_i. 
 * There is also a set of rules S = {r_0, r_1, ..., r_(k-1)}, each of which is 
 * both associated with a subset of X, and with a subset of D. It is assumed 
 * that for each I in D, there is at least one R in S that "satisfies" (or, 
 * "explains") I. The problem is to find a minimum cardinality set Y of 
 * variables x in X, that are sufficient to "explain" all the instances in D, in
 * that for each I in D, there is at least one rule R in S that is associated
 * with I, and whose variables are all in Y. Variables, rules, and instances are
 * all identified by consecutive integer numbers starting at 0.
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
public class BottomUpMESSolver extends BaseMESSolver {
	private int _minNumSatInsts;
	private int _maxNumSatInsts;
	private int _K;
	private BufferIntf _queue;  // FIFO fast buffer
	private int _bestCoverage = 0;
	private int _maxNumVarsAllowed = Integer.MAX_VALUE;
	
	
	/**
	 * public constructor.
	 * @param numVars int the number of variables in this MES problem
	 * @param numRules int the number of rules in this MES problem
	 * @param numInstances int the number of instances in this MES problem
	 * @param rule2vars Map&lt;Integer ruleid, Set&lt;Integer varid&gt; &gt;
	 * @param rule2insts Map&lt;Integer ruleid, Set&lt;Integer instid&gt; &gt;
	 * @param minSatInstRatio double the minimum required coverage ratio of 
	 * instances satisfied by the returned solution over the total instances
	 * satisfied by all the rules
	 * @param numvars2consider int the max number of top solutions added to the 
	 * queue at every step (can be <CODE>Integer.MAX_VALUE</CODE> for no limit in 
	 * the search)
	 * @param maxqueuesize int maximum number of candidates to keep at any time
	 * (making it a BeamForming-Search) or -1 (the default, indicating no attempt
	 * to bound the outer-loop queue)
	 * @param maxnumvarsallowed int default -1 (no limit) used to cut the search
	 * short
	 */
	public BottomUpMESSolver(int numVars, int numRules, int numInstances,
		                     HashMap rule2vars, HashMap rule2insts, 
												 double minSatInstRatio,
												 int numvars2consider,
												 int maxqueuesize,
												 int maxnumvarsallowed) {
		super(numVars, numRules, numInstances, rule2vars, rule2insts);
		BoolVector all_sat_insts = new BoolVector(_r2iA[0]);
		for (int i=1; i<_r2iA.length; i++)
			all_sat_insts.or(_r2iA[i]);
		_maxNumSatInsts = all_sat_insts.cardinality();
		_minNumSatInsts = (int) (minSatInstRatio*all_sat_insts.cardinality());
		_K = numvars2consider;
		final int init_len = numVars;
		if (maxqueuesize <= 0)
			_queue = new UnboundedBufferArrayUnsynchronized(init_len);
		else 
			_queue = new BoundedBufferArrayUnsynchronized(maxqueuesize);
		if (maxnumvarsallowed>0) _maxNumVarsAllowed = maxnumvarsallowed;
	}
	
	
	/**
	 * protected copy constructor is not part of the public API. Leaves the _K
	 * and _queue data members at their default values (0 and null respectively.)
	 * @param svr BottomUpMESSolver the object to copy from
	 */
	protected BottomUpMESSolver(BottomUpMESSolver svr) {
		super(svr._numVars, svr._numRules, svr._numInstances, 
			    svr._v2rA, svr._r2vA, svr._r2iA, svr._i2rA); 
		_minNumSatInsts = svr._minNumSatInsts;
		_maxNumSatInsts = svr._maxNumSatInsts;
		_maxNumVarsAllowed = svr._maxNumVarsAllowed;
	}
	

	/**
	 * the main class method. It can actually guarantee to find an optimal (least
	 * cardinality) solution that covers the required percentage of instances that
	 * can be covered by the rules, from the starting solution (that is normally
	 * empty), when the maxqueuesize variable in the object constructor is set to
	 * -1; otherwise, the queue size constraints can cause the search to miss out
	 * the truly optimal solution.
	 * @param cur_vars BoolVector normally empty, but can be any set that we 
	 * require to be present in the final solution
	 * @return BoolVector a minimal cardinality vector covering the requested
	 * percentage of instances that are coverable by the rules; may return null
	 * if no solution is found due to a low <CODE>_K</CODE> value set
	 */
	public BoolVector solve(BoolVector cur_vars) {
		ArrayList greedy_sols = new ArrayList();  // needed in case of Beam-Search
		BottomUpMESSolver saux = new BottomUpMESSolver(this);
		saux._queue = new BoundedBufferArrayUnsynchronized(1);
		saux._K = 1;
		
		_queue.reset();
		
		// short-cut
		if (cur_vars!=null && cur_vars.cardinality()>_maxNumVarsAllowed) 
			return null;
		
		try {
			Messenger mger = Messenger.getInstance();
			// mger.msg("BottomUpMESSolver.solve(): enter", 0);
			//if (_queue.size()==0) {
			_queue.addElement(cur_vars);
			//}
			// BFS order
			BoundedMinHeapUnsynchronized minHeap = 
				new BoundedMinHeapUnsynchronized(_numVars);
			BoolVector cur_vars_max = new BoolVector(cur_vars);
			int cnt = 0;
			while (_queue.size()>0) {
				if (++cnt % 100000 == 0) {  // itc: HERE rm asap
					mger.msg("BottomUpMESSolver.solve(): examined "+cnt+
									 " BoolVectors so far", 0);
				}
				cur_vars = (BoolVector) _queue.remove();
				
				// 0. check for threshold
				if (cur_vars.cardinality()>_maxNumVarsAllowed) continue;
				
				// 1. are we done yet?
				setLastBit(cur_vars);
				BoolVector sat_insts = getSatInsts(cur_vars);
				int si_card = sat_insts.cardinality();
				if (si_card > _bestCoverage) {
					if (_K>1)
						mger.msg("BottomUpMESSolver.solve(): best_coverage="+si_card+
										 " out of target="+_minNumSatInsts+" with vars="+
										 cur_vars.toStringSet(), 0);
					_bestCoverage = si_card;
				}
				if (si_card>=_minNumSatInsts) {
					greedy_sols.add(new BoolVector(cur_vars));
					if (_queue instanceof UnboundedBufferArrayUnsynchronized) {  // done
						return getBest(greedy_sols);
					}
				}
				unsetLastBit(cur_vars);
				// 1.1 maybe we can't be done ever?
				// BoolVector cur_vars_max = new BoolVector(cur_vars);
				cur_vars_max.copy(cur_vars);
				for (int vid = cur_vars.lastSetBit(); vid < _numVars; vid++) 
					cur_vars_max.set(vid);
				BoolVector si = getSatInsts(cur_vars_max);
				if (si.cardinality()<_minNumSatInsts) {
					continue;
				}  // fails 
				// 2. consider the best vars to add at this point.
				//    All vars to consider have ids above the highest-id in cur_vars
				minHeap.reset();
				for (int vid = cur_vars.lastSetBit()+1; vid < _numVars-1; vid++) {
					// check if vid makes sense to add given cur_vars
					if (isRedundant(vid, sat_insts)) continue;
					double cv = getCost(cur_vars, vid);
					minHeap.addElement(new Pair(new Integer(vid), new Double(cv)));
				}
				// remove the top _K vars to consider adding them to the current sol
				for (int i=0; i<_K; i++) {
					if (minHeap.size()==0) break;
					Pair toppair = (Pair) minHeap.remove();
					int vid = ((Integer)toppair.getFirst()).intValue();
					BoolVector cvid = new BoolVector(cur_vars);
					cvid.set(vid);
					try {
						_queue.addElement(cvid);
					}
					catch (Exception e) {  // queue is full, solve greedily, add to sols
						//mger.msg("BottomUpMESSolver.solve(): adding "+cvid.toStringSet()+
						//	       " to full queue: will expand greedily", 0);
						saux._bestCoverage = 0;
						saux._queue.reset();
						BoolVector sol = saux.solve(cvid);
						if (sol==null) {
							//mger.msg("BottomUpMESSolve.solve() greedy solver failed to "+
							//	       "produce a valid solution", 0);
							continue;
						}  // failed
						// tests below are wasteful and unnecessary as they will be 
						// done again when calling getBest(), so they are commented out
						//BoolVector si2 = getSatInsts(sol);
						//int si2_card = si2.cardinality();
						//if (si2_card>_bestCoverage) {
							mger.msg("BottomUpMESSolve.solve() greedy solver "+
								       "produced a new best solution: "+sol.toStringSet(), 0);
							greedy_sols.add(new BoolVector(sol));
						//}
					}
				}  // for i in 0..._K 
			}  // while queue not empty
			return getBest(greedy_sols);  // null if _K is chosen too small...
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	
	/**
	 * the second main class method. It produces all optimal sets of variables for
	 * any given length, from the starting solution (that is normally
	 * empty) upward until the entire set of instances is covered as much as the
	 * rule set allows.
	 * @param cur_vars BoolVector normally empty, but can be any set that we 
	 * require to be present in the final solution
	 * @return HashMap // HashMap&lt;Integer num_inst_covered, BoolVector vars&gt;
	 * a map containing minimal cardinality sets of variables covering more and
	 * more of the instance set that can be covered.
	 */
	public HashMap solveContinuously(BoolVector cur_vars) throws Exception {
		int best_coverage = 0;
		HashMap result = new HashMap();  // map<Integer num_inst_cov, BoolVector>
		BufferIntf queue = new UnboundedBufferArrayUnsynchronized(_K);
		Messenger mger = Messenger.getInstance();
		// mger.msg("BottomUpMESSolver.solveContinuously(): enter", 0);
		queue.addElement(cur_vars);
		// BFS order
		BoundedMinHeapUnsynchronized minHeap = 
			new BoundedMinHeapUnsynchronized(_numVars);
		BoolVector cur_vars_max = new BoolVector(cur_vars);
		while (queue.size()>0) {
			cur_vars = (BoolVector) queue.remove();
			// 1. are we done yet?
			setLastBit(cur_vars);
			BoolVector sat_insts = getSatInsts(cur_vars);
			int si_card = sat_insts.cardinality();
			if (si_card > best_coverage) {
				mger.msg("BottomUpMESSolver.solveContinuously(): top_coverage="+si_card+
					       " out of target coverage of "+_maxNumSatInsts+
					       " with vars="+cur_vars.toStringSet(), 0);
				// remove all var-sets in result of equal cardinality as cur_vars
				Iterator it = result.keySet().iterator();
				while (it.hasNext()) {
					Integer cov = (Integer) it.next();
					BoolVector vars = (BoolVector) result.get(cov);
					if (vars.cardinality()==cur_vars.cardinality()) {
						it.remove();
					}
				}
				best_coverage = si_card;
				result.put(new Integer(best_coverage), new BoolVector(cur_vars));			
			}
			if (si_card>=_maxNumSatInsts) {
				return result;
			}
			unsetLastBit(cur_vars);
			// 1.1 maybe cur_vars should be fathomed?
			// BoolVector cur_vars_max = new BoolVector(cur_vars);
			cur_vars_max.copy(cur_vars);
			for (int vid = cur_vars.lastSetBit(); vid < _numVars; vid++) 
				cur_vars_max.set(vid);
			BoolVector si = getSatInsts(cur_vars_max);
			if (si.cardinality()<=best_coverage) {
				continue;
			}  // fails
			// 2. consider the best vars to add at this point.
			//    All vars to consider have ids above the highest-id in cur_vars
			minHeap.reset();
			for (int vid = cur_vars.lastSetBit()+1; vid < _numVars-1; vid++) {
				// check if vid makes sense to add given cur_vars
				if (isRedundant(vid, sat_insts)) continue;
				double cv = getCost(cur_vars, vid);
				minHeap.addElement(new Pair(new Integer(vid), new Double(cv)));
			}
			// remove all vars and consider adding them to the current sol
			while (true) {
				if (minHeap.size()==0) break;
				Pair toppair = (Pair) minHeap.remove();
				int vid = ((Integer)toppair.getFirst()).intValue();
				BoolVector cvid = new BoolVector(cur_vars);
				cvid.set(vid);
				queue.addElement(cvid);
			} 
		}
		return null;  // may happen if _K is chosen too small
	}
	
	
	/**
	 * get the total number of covered instances from all the rules.
	 * @return int
	 */
	public int getMaxNumSatInsts() {
		return _maxNumSatInsts;
	}

	
	/**
	 * compute the number I of instances the solution vars+{vid} covers, and 
	 * return the number (|vars|+1)/(I+1). It is expected that vid is not set in
	 * vars.
	 * @param vars 
	 * @param vid
	 * @return 
	 */
	protected double getCost(BoolVector vars, int vid) {
		setLastBit(vars);
		vars.set(vid);
		BoolVector satInsts = getSatInsts(vars);
		double res = (vars.cardinality()+1) / (double)(satInsts.cardinality()+1.0);
		vars.unset(vid);
		unsetLastBit(vars);
		return res;
	}
	
	
	/**
	 * return true iff the variable vid is redundant in the sense that its 
	 * presence in the current solution does not allow for any currently non-
	 * satisfied instance to be among the "future satisfied ones" by the augmented
	 * solution. In other words, a variable is redundant for a solution if all the 
	 * rules that depend on it cover instances already covered in the current 
	 * solution.
	 * @param vid int
	 * @param cur_vars_sat_insts BoolVector
	 * @return boolean
	 */
	protected boolean isRedundant(int vid, BoolVector cur_vars_sat_insts) {
		BoolVector vid_rules = _v2rA[vid];
		for (int r=vid_rules.nextSetBit(0); r>=0; r=vid_rules.nextSetBit(r+1)) {
			BoolVector r_insts = _r2iA[r];
			if (!cur_vars_sat_insts.containsAll(r_insts)) return false;
		}
		return true;
	}
	
	
	private static void setLastBit(BoolVector bv) {
		bv.set(bv.reqSize()-1);
	}
	
	
	private static void unsetLastBit(BoolVector bv) {
		bv.unset(bv.reqSize()-1);
	}
	
	
	private static BoolVector getBest(ArrayList sols) {
		int best_card = Integer.MAX_VALUE;
		BoolVector res = null;
		for (int i=0; i<sols.size(); i++) {
			BoolVector si = (BoolVector) sols.get(i);
			int si_card = si.cardinality();
			if (si_card<best_card) {
				res = si;
				best_card = si_card;
			}
		}
		return res;
	}
	
}
