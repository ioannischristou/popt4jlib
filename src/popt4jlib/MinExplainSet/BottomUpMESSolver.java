package popt4jlib.MinExplainSet;

import popt4jlib.BoolVector;
import parallel.BoundedMinHeapUnsynchronized;
import parallel.UnboundedBufferArrayUnsynchronized;
import utils.Pair;
import utils.Messenger;
import java.util.HashMap;


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
 * containing those rule-ids that contain varialbe x_i. Similarly, the array
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
	private int _K;
	private UnboundedBufferArrayUnsynchronized _queue;  // FIFO fast buffer
	
	
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
	 */
	public BottomUpMESSolver(int numVars, int numRules, int numInstances,
		                     HashMap rule2vars, HashMap rule2insts, 
												 double minSatInstRatio,
												 int numvars2consider) {
		super(numVars, numRules, numInstances, rule2vars, rule2insts);
		BoolVector all_sat_insts = new BoolVector(_r2iA[0]);
		for (int i=1; i<_r2iA.length; i++)
			all_sat_insts.or(_r2iA[i]);
		_minNumSatInsts = (int) (minSatInstRatio*all_sat_insts.cardinality());
		_K = numvars2consider;
		final int init_len = numVars;
		_queue = new UnboundedBufferArrayUnsynchronized(init_len);
	}
	

	/**
	 * the main class method. It actually guarantees to find an optimal (minimum
	 * cardinality) solution that covers the required percentage of instances that
	 * can be covered by the rules, from the starting solution (that is normally
	 * empty.)
	 * @param cur_vars BoolVector normally empty
	 * @return BoolVector a minimal cardinality vector covering the requested
	 * percentage of instances that are coverable by the rules
	 */
	public BoolVector solve(BoolVector cur_vars) {
		int best_coverage = 0;
		Messenger mger = Messenger.getInstance();
		// mger.msg("BottomUpMESSolver.solve(): enter", 0);
		if (_queue.size()==0) {
			_queue.addElement(cur_vars);
		}
		// BFS order
		BoundedMinHeapUnsynchronized minHeap = 
			new BoundedMinHeapUnsynchronized(_numVars);
		while (_queue.size()>0) {
			cur_vars = (BoolVector) _queue.remove();
			// 1. are we done yet?
			setLastBit(cur_vars);
			BoolVector sat_insts = getSatInsts(cur_vars);
			int si_card = sat_insts.cardinality();
			if (si_card > best_coverage) {
				mger.msg("BottomUpMESSolver.solve(): best_coverage="+si_card+
					       " out of target="+_minNumSatInsts, 0);
				best_coverage = si_card;
			}
			if (si_card>=_minNumSatInsts) return cur_vars;
			unsetLastBit(cur_vars);
			// 1.1 maybe we can't be done ever?
			BoolVector cur_vars_max = new BoolVector(cur_vars);
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
				_queue.addElement(cvid);
			} 
		}
		return null;  // cannot happen
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
	
}
