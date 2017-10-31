/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package graph.packing;

import graph.Graph;
import java.util.HashSet;
import java.util.Iterator;
import utils.DataMgr;
import utils.IntSet;

/**
 * class allows the evaluation of a solution to the MWIS problem contained in a
 * file ("sol.out") for a graph. 
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 *
 */
public class MWISEvaluator {
	/**
	 * invoke as <CODE>java -cp &lt;classpath&gt; graph.packing.MWISEvaluator 
	 * &lt;graph_file&gt; [sol_file("sol.out")]
	 * </CODE>.
	 * @param args String[] 
	 */
	public static void main(String[] args) {
		String graph_file = args[0];
		String sol_file = "sol.out";
		if (args.length>1) sol_file = args[1];
		try {
			Graph g = DataMgr.readGraphFromFile2(graph_file);
			IntSet sol_p_1 = DataMgr.readIntegersFromFile(sol_file);
			HashSet sol = new HashSet();
			Iterator it = sol_p_1.iterator();
			while (it.hasNext()) {
				Integer iI = (Integer) it.next();
				int ni = iI.intValue()-1;
				sol.add(new Integer(ni));
			}
			if (GRASPPacker.isFeasible(g, sol, 1)) {
				SetWeightEvalFunction f = new SetWeightEvalFunction(g);
				double wgt = f.eval(sol, null);
				System.out.println("MWIS solution with size="+sol.size()+
					                 " and weight="+wgt+" checks out ");
			}
			else {
				System.err.println("solution is infeasible...");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
