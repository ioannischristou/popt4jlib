package popt4jlib.MinExplainSet;

import popt4jlib.BoolVector;
import java.io.*;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;


/**
 * Naive greedy algorithm implementation for solving the Minimum Explaining Set
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
 * <p>Copyright: Copyright (c) 2011-2019</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class GreedyMESSolver extends BaseMESSolver {
	
	
	/**
	 * public constructor.
	 * @param numVars int the number of variables in this MES problem
	 * @param numRules int the number of rules in this MES problem
	 * @param numInstances int the number of instances in this MES problem
	 * @param rule2vars Map&lt;Integer ruleid, Set&lt;Integer varid&gt; &gt;
	 * @param rule2insts Map&lt;Integer ruleid, Set&lt;Integer instid&gt; &gt;
	 */
	public GreedyMESSolver(int numVars, int numRules, int numInstances,
		                     HashMap rule2vars, HashMap rule2insts) {
		super(numVars, numRules, numInstances, rule2vars, rule2insts);
	}
	
	
	/**
	 * obtains a (hopefully) near-optimal solution to the MES problem, by 
	 * repeatedly removing variables from the solution until no variable can be
	 * removed without leaving at least one instance "unexplained".
	 * @param cur_vars BoolVector variables in the current solution
	 * @return BoolVector variables in the final solution, subset of the passed-in
	 * argument
	 */
	public BoolVector solve(BoolVector cur_vars) {
		int[] var_cost = new int[cur_vars.reqSize()];  // init to zero
		BoolVector xi_rules = new BoolVector(_r2vA.length);
		BoolVector xi_act_rules = new BoolVector(_r2vA.length);
		BoolVector cov_insts = new BoolVector(_i2rA.length);
		// BoolVector m_vars = new BoolVector(_v2rA.length);
		BoolVector result = new BoolVector(cur_vars);
		BoolVector cur_vars_copy = new BoolVector(cur_vars);  // aux. copy vector
		int best_cost = Integer.MAX_VALUE;
		int best_x = -1;
		boolean cont = true;
		while (cont) {
			// System.err.println("NEW OUTER ITERATION");  // itc: HERE rm asap
			cur_vars_copy.copy(cur_vars);
			cont = false;
			
			// for-loop below is just for computing variables' costs
			best_cost = Integer.MAX_VALUE;
			best_x = -1;
			for(int i=cur_vars.nextSetBit(0); i>=0; i=cur_vars.nextSetBit(i+1)) {
				// how many instances does xi cover via still active rules?
				xi_rules.copy(_v2rA[i]);
				xi_act_rules.clear();
				// find the active ones
				for (int j=xi_rules.nextSetBit(0); j>=0; j=xi_rules.nextSetBit(j+1)) {
					BoolVector rj_vars = _r2vA[j];
					if (cur_vars.containsAll(rj_vars)) {  // rule rj is active
						xi_act_rules.set(j);
					}
				}
				cov_insts.clear();
				for (int j=xi_act_rules.nextSetBit(0); j>=0; 
					   j=xi_act_rules.nextSetBit(j+1)) {
					BoolVector j_insts = _r2iA[j];
					cov_insts.or(j_insts);
				}
				var_cost[i] = cov_insts.cardinality();
				// System.err.println("cost of var x"+i+" is "+var_cost[i]);
				if (var_cost[i] < best_cost) {
					best_cost = var_cost[i];
					best_x = i;
					// System.err.println("new best cost is "+best_cost+" for var x"+i);
				}
			}  // for i in cur_vars
			
			// next, check all vars with best cost, to see if there is any xi such
			// that by removing it, no item remains uncovered
			for (int i=cur_vars.nextSetBit(0); i>=0; i=cur_vars.nextSetBit(i+1)) {
				if (var_cost[i]==best_cost) {
					if (best_cost == 0) {  // variable xi can definitely be removed 
						result.unset(i);
						cur_vars_copy.unset(i);
						cont = true;
					}
					else {  // check if xi can be removed
						xi_rules.copy(_v2rA[i]);
						xi_act_rules.clear();
						// find the active ones
						for (int j=xi_rules.nextSetBit(0);j>=0;j=xi_rules.nextSetBit(j+1)) {
							BoolVector rj_vars = _r2vA[j];
							if (cur_vars_copy.containsAll(rj_vars)) {  // rule rj is active
								xi_act_rules.set(j);
							}
						}
						cov_insts.clear();
						for (int j=xi_act_rules.nextSetBit(0); j>=0; 
								 j=xi_act_rules.nextSetBit(j+1)) {
							BoolVector j_insts = _r2iA[j];
							cov_insts.or(j_insts);
						}
						// cov_insts are the "suspect" instances that xi relates with.
						// for each one of those, we need to see if there is at least one
						// rule whose vars are still covered by cur_vars - xi. If any 
						// instance fails, xi is "critical" and cannot be removed from the
						// current solution
						// m_vars.clear();
						cur_vars_copy.unset(i);
						for (int k=cov_insts.nextSetBit(0); k>=0; 
							   k=cov_insts.nextSetBit(k+1)) {
							boolean inst_failed = true;
							BoolVector k_rules = _i2rA[k];
							for (int m=k_rules.nextSetBit(0);m>=0;m=k_rules.nextSetBit(m+1)) {
								BoolVector m_vars = _r2vA[m];  // m_vars.copy(_r2vA[m]);
								if (cur_vars_copy.containsAll(m_vars)) {  // inst-k is OK!
									inst_failed = false;
									break;
								}
							}
							if (inst_failed) {
								cur_vars_copy.set(i);
								break;
							}
						}
						if (!cur_vars_copy.get(i)) {  // xi is OK to remove
							result.unset(i);
							//System.err.print("result=");print(result);  // itc: HERE rm asap
							cont = true;  // go for another round
						}
					}  // else if best_cost != 0
				}
				// else if xi cost is not optimal, will be considered in final for-loop
				// below
			}  // for xi in cur_vars
			// finally, go through the non locally optimal variables 
			for (int i=cur_vars.nextSetBit(0); i>=0; i=cur_vars.nextSetBit(i+1)) {
				if (var_cost[i] > best_cost) {  // check if xi can be removed
					xi_rules.copy(_v2rA[i]);
					xi_act_rules.clear();
					// find the active ones
					for (int j=xi_rules.nextSetBit(0);j>=0;j=xi_rules.nextSetBit(j+1)) {
						BoolVector rj_vars = _r2vA[j];
						if (cur_vars_copy.containsAll(rj_vars)) {  // rule rj is active
							xi_act_rules.set(j);
						}
					}
					cov_insts.clear();
					for (int j=xi_act_rules.nextSetBit(0); j>=0; 
							 j=xi_act_rules.nextSetBit(j+1)) {
						BoolVector j_insts = _r2iA[j];
						cov_insts.or(j_insts);
					}
					// cov_insts are the "suspect" instances that xi relates with.
					// for each one of those, we need to see if there is at least one
					// rule whose vars are still covered by cur_vars - xi. If any 
					// instance fails, xi is "critical" and cannot be removed from the
					// current solution
					// m_vars.clear();
					cur_vars_copy.unset(i);
					for (int k=cov_insts.nextSetBit(0); k>=0; 
						   k=cov_insts.nextSetBit(k+1)) {
						boolean inst_failed = true;
						BoolVector k_rules = _i2rA[k];
						for (int m=k_rules.nextSetBit(0);m>=0;m=k_rules.nextSetBit(m+1)) {
							BoolVector m_vars = _r2vA[m];  // m_vars.copy(_r2vA[m]);
							if (cur_vars_copy.containsAll(m_vars)) {  // inst-k is OK!
								inst_failed = false;
								break;
							}
						}
						if (inst_failed) {
							cur_vars_copy.set(i);
							break;
						}
					}
					if (!cur_vars_copy.get(i)) {  // xi is OK to remove
						result.unset(i);
						//System.err.print("result="); print(result);  // itc: HERE rm asap
						cont = true;  // go for another round
					}
				}
			}  // for xi in cur_vars, 2nd and last loop
						
			cur_vars.copy(result);  // prepare cur_vars for the next outer iteration
		}
		return result;
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
	 * 
	 * @param args 
	 */
	public static void main(String[] args) {
		String filename = args[0];
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
			GreedyMESSolver slvr = new GreedyMESSolver(numvars, numrules, numinsts,
			                                           r2vars, r2insts);
			BoolVector init_vars = new BoolVector(numvars);
			for (int i=0; i<numvars; i++) init_vars.set(i);  // all in
			BoolVector expl_vars = slvr.solve(init_vars);
			long dur = System.currentTimeMillis()-start;
			System.out.print("Final sol=");
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
