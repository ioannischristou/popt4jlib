/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tests;

import graph.Graph;
import graph.coarsening.Coarsener;
import graph.coarsening.CoarsenerIEC;
import java.util.HashMap;
import utils.DataMgr;

/**
 *
 * @author itc
 */
public class CoarsenerIECTest {
	public static void main(String[] args) {
		String graph_file = args[0];
		try {
			Graph g = DataMgr.readGraphFromFile2(graph_file);
			int num_fine_nodes = g.getNumNodes();
			HashMap props = new HashMap();
			props.put("ratio", new Double(0.55));
			props.put("max_allowed_card", new Double(4.0));
			props.put("lamda", new Double(0.8));
			Coarsener cner = new CoarsenerIEC(g, null, props);
			int cnt=0;
			long start = System.currentTimeMillis();
			while (true) {
				try {
					++cnt;
					cner.coarsen();
					Graph cg = cner.getCoarseGraph();
					System.err.println("Level "+cnt+
						                 " Coarse Graph #nodes="+cg.getNumNodes()+
						                 " #arcs="+cg.getNumArcs());
					if (cg.getNumNodes()/(double)num_fine_nodes < 0.2) {
						System.out.println("done.");
						break;
					}
					cner = new CoarsenerIEC(cg, null, props);
				}
				catch (Exception e) {
					e.printStackTrace();
					break;
				}
			}
			long dur = System.currentTimeMillis()-start;
			System.out.println("done in "+dur+" msecs");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
	}
}
