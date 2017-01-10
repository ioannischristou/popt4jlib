/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tests;

import utils.*;
import graph.*;

/**
 * Tests the shortest-path method (Dijkstra's method) of class 
 * <CODE>graph.Graph</CODE>.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ShortestPathsTest {
	
	/**
	 * invoke as 
	 * <CODE>java -cp &lt;classpath&gt; tests.ShortestPathsTest 
	 * &lt;graphfilename&gt; &lt;startnodeid&gt; &lt;targetnodeid&gt; 
	 * </CODE>.
	 * @param args String[]
	 */
	public static void main(String[] args) {
		try {
			Graph g = DataMgr.readGraphFromFile2(args[0]);
			int si = Integer.parseInt(args[1]);
			int ti = Integer.parseInt(args[2]);
			long start = System.currentTimeMillis();
			double dist = g.getShortestPath(g.getNodeUnsynchronized(si), 
				                              g.getNodeUnsynchronized(ti));
			long dur = System.currentTimeMillis()-start;
			System.out.println("distance between s="+si+" and t="+ti+" = "+dist+
				                 " (found in "+dur+" msecs)");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
