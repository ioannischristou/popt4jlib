/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package graph.packing;

import popt4jlib.FunctionIntf;
import popt4jlib.LocalSearch.IntSetNeighborhoodFilterIntf;
import utils.DataMgr;

/**
 * Test driver class for the 
 * <CODE>IntSetN2RXPFirstImprovingGraphAllMovesMaker</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ISN2RXPFIGAMMTest {
	/**
	 * invoke as <CODE>java -cp &lt;classpath&gt; graph.packing.ISN2RXPFIGAMMTest &lt;graph_file&gt; &lt;int_file&gt; [k(1)]</CODE>.
	 * @param args String[]
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		graph.Graph g = DataMgr.readGraphFromFile2(args[0]);
		utils.IntSet x0 = DataMgr.readIntegersFromFile(args[1]);
		int k = 1;
		if (args.length>2) k = Integer.parseInt(args[2]);
		IntSetN2RXPGraphAllMovesMaker maker = 
		  new IntSetN2RXPGraphAllMovesMaker(k);
    IntSetNeighborhoodFilterIntf filter = new
      GRASPPackerIntSetNbrhoodFilter3(1,g);
		java.util.HashMap params = new java.util.HashMap();
		params.put("dls.graph", g);
		params.put("dls.lock_graph", Boolean.FALSE);
		{ // test the filter
			java.util.Iterator iter = x0.iterator();
			while (iter.hasNext()) {
				Integer id = (Integer) iter.next();
				java.util.List two_intsets = filter.filter(id, x0, params);
				System.out.print("filter.filter("+id+",x0="+x0+",params)={");
				for (int i=0; i<two_intsets.size(); i++) {
					utils.IntSet si = (utils.IntSet) two_intsets.get(i);
					System.out.print(si+" ");
					if (i<two_intsets.size()-1) System.out.print(",");
				}
				System.out.println("}");
			}
		}
    //FunctionIntf f = new SetWeightEvalFunction(g);
		params.put("dls.intsetneighborhoodfilter", filter);
		java.util.Vector points = maker.createAllChromosomes(x0, params);
		for (int i=0; i<points.size(); i++) {
			utils.IntSet si = (utils.IntSet) points.get(i);
			System.out.println("si="+si);
		}
		
	}
}
