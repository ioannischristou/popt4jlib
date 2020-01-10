package popt4jlib.MinExplainSet;

import popt4jlib.BoolVector;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;


/**
 * Base algorithm class implementation for solving the Minimum Explaining Set
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
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public abstract class BaseMESSolver {
	protected final int _numVars, _numRules, _numInstances;
	protected final BoolVector[] _v2rA, _r2vA, _r2iA, _i2rA;
	
	
	/**
	 * public constructor.
	 * @param numVars int the number of variables in this MES problem
	 * @param numRules int the number of rules in this MES problem
	 * @param numInstances int the number of instances in this MES problem
	 * @param rule2vars Map&lt;Integer ruleid, Set&lt;Integer varid&gt; &gt;
	 * @param rule2insts Map&lt;Integer ruleid, Set&lt;Integer instid&gt; &gt;
	 */
	public BaseMESSolver(int numVars, int numRules, int numInstances,
		                     HashMap rule2vars, HashMap rule2insts) {
		_numVars = numVars;
		_numRules = numRules;
		_numInstances = numInstances;
		_v2rA = new BoolVector[numVars];
		for (int vid=0; vid<numVars; vid++) {
			_v2rA[vid] = new BoolVector(numRules);
		}
		_i2rA = new BoolVector[numInstances];
		for (int iid=0; iid<numInstances; iid++) {
			_i2rA[iid] = new BoolVector(numRules);
		}		
		_r2vA = new BoolVector[numRules];
		_r2iA = new BoolVector[numRules];
		for (int rid=0; rid<numRules; rid++) {
			// 1. populate _r2vA
			_r2vA[rid] = new BoolVector(numVars);
			Set rid_vars = (Set) rule2vars.get(new Integer(rid));
			if (rid_vars!=null) {
				Iterator it = rid_vars.iterator();
				while (it.hasNext()) {
					Integer vid = (Integer) it.next();
					_r2vA[rid].set(vid.intValue());
					_v2rA[vid.intValue()].set(rid);
				}
			}
			// 2. populate _r2iA
			_r2iA[rid] = new BoolVector(numInstances);
			Set rid_insts = (Set) rule2insts.get(new Integer(rid));
			if (rid_insts!=null) {
				Iterator it = rid_insts.iterator();
				while (it.hasNext()) {
					Integer iid = (Integer) it.next();
					_r2iA[rid].set(iid.intValue());
					_i2rA[iid.intValue()].set(rid);
				}
			}
		}
	}
	
	
	/**
	 * obtains a (hopefully) near-optimal solution to the MES problem, starting
	 * from the cur_vars solution.
	 * @param cur_vars BoolVector variables in the current solution
	 * @return BoolVector variables in the final solution
	 */
	abstract public BoolVector solve(BoolVector cur_vars);

	
	/**
	 * find and return the ids of all instances that are covered by rules that
	 * involve only variables whose ids are contained in the input BoolVector.
	 * @param vars BoolVector
	 * @return BoolVector
	 */
	public BoolVector getSatInsts(BoolVector vars) {
		BoolVector result = new BoolVector(_numInstances);
		for (int r=0; r<_numRules; r++) {
			BoolVector r_vars = _r2vA[r];
			// are the r_vars in vars?
			if (vars.containsAll(r_vars)) {
				// all instances the rule covers, make it to result
				result.or(_r2iA[r]);
			}
		}
		return result;
	}
	
	
	/**
	 * debug routine, prints in stderr the input vector x. Should be static, but
	 * then we'd have to prefix its call with "BaseMESSolver.".
	 * @param x BoolVector
	 */
	protected void print(BoolVector x) {
		System.err.print("[ ");
		for (int k=x.nextSetBit(0); k>=0; k=x.nextSetBit(k+1)) 
			System.err.print(k+" ");
		System.err.println("]");
	}
	
}
