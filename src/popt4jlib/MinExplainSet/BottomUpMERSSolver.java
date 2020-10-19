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


/**
 * Bottom-up algorithm implementation for solving the Minimum Explaining RuleSet
 * (MERS) problem. The MERS problem can be described as follows: we are given a 
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
public class BottomUpMERSSolver extends BaseMESSolver {
	private int _minNumSatInsts;
	private int _maxNumSatInsts;
	private int _K;
	private BufferIntf _queue;  // FIFO fast buffer
	private int _bestCoverage = 0;
	private int _maxNumRulesAllowed = Integer.MAX_VALUE;
	
	
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
	 * @param numsols2consider int the max number of top solutions added to the 
	 * queue at every step (can be <CODE>Integer.MAX_VALUE</CODE> for no limit in 
	 * the search)
	 * @param maxqueuesize int maximum number of candidates to keep at any time
	 * (making it a BeamForming-Search) or -1 (the default, indicating no attempt
	 * to bound the outer-loop queue)
	 * @param maxnumrulesallowed int default -1 (no limit) used to cut the search
	 * short
	 */
	public BottomUpMERSSolver(int numVars, int numRules, int numInstances,
		                     HashMap rule2vars, HashMap rule2insts, 
												 double minSatInstRatio,
												 int numsols2consider,
												 int maxqueuesize,
												 int maxnumrulesallowed) {
		super(numVars, numRules, numInstances, rule2vars, rule2insts);
		BoolVector all_sat_insts = new BoolVector(_r2iA[0]);
		for (int i=1; i<_r2iA.length; i++)
			all_sat_insts.or(_r2iA[i]);
		_maxNumSatInsts = all_sat_insts.cardinality();
		_minNumSatInsts = (int) (minSatInstRatio*all_sat_insts.cardinality());
		_K = numsols2consider;
		final int init_len = numVars;
		if (maxqueuesize <= 0)
			_queue = new UnboundedBufferArrayUnsynchronized(init_len);
		else 
			_queue = new BoundedBufferArrayUnsynchronized(maxqueuesize);
		if (maxnumrulesallowed>0) _maxNumRulesAllowed = maxnumrulesallowed;
	}
	
	
	/**
	 * protected copy constructor is not part of the public API. Leaves the _K
	 * and _queue data members at their default values (0 and null respectively.)
	 * @param svr BottomUpMESSolver the object to copy from
	 */
	protected BottomUpMERSSolver(BottomUpMERSSolver svr) {
		super(svr._numVars, svr._numRules, svr._numInstances, 
			    svr._v2rA, svr._r2vA, svr._r2iA, svr._i2rA); 
		_minNumSatInsts = svr._minNumSatInsts;
		_maxNumSatInsts = svr._maxNumSatInsts;
		_maxNumRulesAllowed = svr._maxNumRulesAllowed;
	}
	

	/**
	 * the main class method. It can actually guarantee to find an optimal (least
	 * cardinality) rule-set that covers the required percentage of instances that
	 * can be covered by the rules, from the starting solution (that is normally
	 * empty), when the maxqueuesize variable in the object constructor is set to
	 * -1; otherwise, the queue size constraints can cause the search to miss out
	 * the truly optimal solution.
	 * @param cur_rules BoolVector normally empty, but can be any set that we 
	 * require to be present in the final solution
	 * @return BoolVector a minimal cardinality vector covering the requested
	 * percentage of instances that are coverable by the rules; may return null
	 * if no solution is found due to a low <CODE>_K</CODE> value set
	 */
	public BoolVector solve(BoolVector cur_rules) {
		ArrayList greedy_sols = new ArrayList();  // needed in case of Beam-Search
		BottomUpMERSSolver saux = new BottomUpMERSSolver(this);
		saux._queue = new BoundedBufferArrayUnsynchronized(1);
		saux._K = 1;
		
		_queue.reset();
		
		// short-cut
		if (cur_rules!=null && cur_rules.cardinality()>_maxNumRulesAllowed) 
			return null;
		
		try {
			Messenger mger = Messenger.getInstance();
			//mger.msg("BottomUpMERSSolver.solve(): enter", 1);
			//if (_queue.size()==0) {
			_queue.addElement(cur_rules);
			//}
			// BFS order
			BoundedMinHeapUnsynchronized minHeap = 
				new BoundedMinHeapUnsynchronized(_numRules);
			BoolVector cur_rules_max = new BoolVector(cur_rules);
			int cnt = 0;
			while (_queue.size()>0) {
				if (++cnt % 100000 == 0) {  // itc: HERE rm asap
					mger.msg("BottomUpMERSSolver.solve(): examined "+cnt+
									 " BoolVectors so far", 1);
				}
				cur_rules = (BoolVector) _queue.remove();
				
				// 0. check for threshold
				if (cur_rules.cardinality()>_maxNumRulesAllowed) continue;
				
				// 1. are we done yet?
				BoolVector sat_insts = getSatInsts(cur_rules);
				int si_card = sat_insts.cardinality();
				if (si_card > _bestCoverage) {
					if (_K>1)
						mger.msg("BottomUpMERSSolver.solve(): best_coverage="+si_card+
										 " out of target="+_minNumSatInsts+" with rules="+
										 cur_rules.toStringSet(), 0);
					_bestCoverage = si_card;
				}
				if (si_card>=_minNumSatInsts) {
					greedy_sols.add(new BoolVector(cur_rules));
					if (_queue instanceof UnboundedBufferArrayUnsynchronized) {  // done
						return getBest(greedy_sols);
					}
				}
				// 1.1 maybe we can't be done ever?
				// BoolVector cur_vars_max = new BoolVector(cur_vars);
				cur_rules_max.copy(cur_rules);
				for (int rid = cur_rules.lastSetBit(); rid < _numRules; rid++) 
					cur_rules_max.set(rid);
				BoolVector si = getSatInsts(cur_rules_max);
				if (si.cardinality()<_minNumSatInsts) {
					continue;
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
				// remove the top _K rules to consider adding them to the current sol
				for (int i=0; i<_K; i++) {
					if (minHeap.size()==0) break;
					Pair toppair = (Pair) minHeap.remove();
					int rid = ((Integer)toppair.getFirst()).intValue();
					BoolVector crid = new BoolVector(cur_rules);
					crid.set(rid);
					try {
						_queue.addElement(crid);
					}
					catch (Exception e) {  // queue is full, solve greedily, add to sols
						//mger.msg("BottomUpMESSolver.solve(): adding "+cvid.toStringSet()+
						//	       " to full queue: will expand greedily", 0);
						saux._bestCoverage = 0;
						saux._queue.reset();
						BoolVector sol = saux.solve(crid);
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
							mger.msg("BottomUpMERSSolve.solve() greedy solver "+
								       "produced a new solution: "+sol.toStringSet(), 0);
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
	 * get all instances satisfied by one or more of the rules in the specified 
	 * bit-vector.
	 * @param rules BoolVector
	 * @return BoolVector
	 */
	public BoolVector getSatInsts(BoolVector rules) {
		BoolVector insts = new BoolVector(_numInstances);
		for (int i=rules.nextSetBit(0); i>=0; i=rules.nextSetBit(i+1)) {
			insts.or(_r2iA[i]);
		}
		return insts;
	}
	
	
	/**
	 * get the total number of covered instances from all the rules.
	 * @return int
	 */
	public int getMaxNumSatInsts() {
		return _maxNumSatInsts;
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
	 * return true iff the rule rid is redundant in the sense that its 
	 * presence in the current solution does not allow for any currently non-
	 * satisfied instance to be among the "future satisfied ones" by the augmented
	 * solution. In other words, a rule is redundant for a solution if all the 
	 * instances it covers are already covered in the current solution.
	 * @param rid int
	 * @param cur_rules_sat_insts BoolVector
	 * @return boolean
	 */
	protected boolean isRedundant(int rid, BoolVector cur_rules_sat_insts) {
		BoolVector rid_insts = _r2iA[rid];
		return cur_rules_sat_insts.containsAll(rid_insts);
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
		return res;
	}
	
}
