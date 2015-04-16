/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utils;

import graph.*;
import java.util.*;
import java.io.*;

/**
 * Convert a graph (1- or 2-) packing problem to a MIP, in lp format.
 * @author itc
 */
public class GraphPacking2MIPConverter {
	private Graph _g;
	private int _k;
	
	
	private GraphPacking2MIPConverter(Graph g, int k) {
		_g = g;
		_k = k;
	}
	
	
	private void createMIP(String lpfile) throws IOException {
		PrintWriter pw = new PrintWriter(new FileWriter(lpfile));
		// 1. header
		pw.println("Problem\n mippacker\nMaximize");
		// 2. objective
		pw.print(" obj:   ");
		final int gn = _g.getNumNodes();
		for (int i=0; i<gn; i++) {
			Node ni = _g.getNode(i);
			Double wiD = ni.getWeightValue("value");
			double wi = (_k==2) ? 1.0 : (wiD==null ? 1.0 : wiD.doubleValue());
			if (Double.compare(wi, 0.0)==0) continue;  // don't include zero coeffs
			pw.print(wi+"x"+i);
			if (i<gn-1) pw.print(" + ");
			if (i>0 && i % 10 == 0) pw.print("\n        ");
			else if (i==_g.getNumNodes()-1) pw.println("");
		}
		// 3. constraints
		pw.println("Subject To");
		int ctrcounter=0;
		Set nodes = new TreeSet();  // Set<IntSet>
		for (int i=0; i<_g.getNumNodes(); i++) {
			Node ni = _g.getNode(i);
			Set ni_bors = ni.getNbors();
			if (_k==1) {
				Iterator it = ni_bors.iterator();
				while (it.hasNext()) {
					Node b = (Node) it.next();
					if (i>b.getId()) continue;
					// ok, add constraint
					pw.println(" R"+ctrcounter+":   x"+i+" + x"+b.getId()+ " <= 1");
					ctrcounter++;
				}
			} else {  // _k==2
				IntSet ss = new IntSet();
				ss.add(new Integer(i));
				Iterator it = ni_bors.iterator();
				while (it.hasNext()) {
					ss.add(new Integer(((Node) it.next()).getId()));
				}
				if (nodes.contains(ss)) continue;
				// ok, add constraint
				pw.print(" R"+ctrcounter+":   ");
				it = ss.iterator();
				while (it.hasNext()) {
					int id = ((Integer) it.next()).intValue();
					pw.print("x" + id);
					if (it.hasNext()) pw.print(" + ");
					else pw.println(" <= 1");
				}
			}
		}
		// 4. bounds constraints
		pw.println("Bounds");
		for (int i=0; i<_g.getNumNodes(); i++) 
			pw.println("x"+i+" <= 1");
		// 5. integrality constraints
		pw.println("Integer");
		for (int i=0; i<_g.getNumNodes(); i++) {
			if (i>0 && i % 10 == 0) pw.println("");
			pw.print("x" + i + " ");
			if (i==_g.getNumNodes()-1) pw.println("");
		}
		// Footer
		pw.println("End");
		pw.flush();
		pw.close();
	}
	
	
	/**
	 * invoke as
	 * java -cp &lt;classpath&gt; utils.GraphPacking2MIPConverter &lt;graphfile&gt; &lt;k&gt; &lt;lpfile&gt;
	 * @param args String[] 
	 */
	public static void main(String[] args) {
		if (args.length!=3) {
			System.err.println("usage: java -cp <classpath> utils.GraphPacking2MIPConverter <graphfile> <k> <lpfile>");
			System.exit(-1);
		}
		String gfile = args[0];
		try {
			Graph g = DataMgr.readGraphFromFile2(gfile);
			int k = Integer.parseInt(args[1]);
			String lpfile = args[2];
			GraphPacking2MIPConverter c = new GraphPacking2MIPConverter(g,k);
			c.createMIP(lpfile);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
