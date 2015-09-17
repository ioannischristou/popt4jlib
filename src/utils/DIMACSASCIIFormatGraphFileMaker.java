/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utils;

import java.io.*;
import java.util.*;

/**
 * utility class that takes as argument a graph file and converts it into a
 * file whose format is the DIMACS GRAPH ASCII FORMAT, or vice-versa.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DIMACSASCIIFormatGraphFileMaker {
	/**
	 * invoke as <CODE>java -cp &lt;classpath&gt; utils.DIMACSASCIIFormatGraphFileMaker &lt;graphfile&gt; &lt;DIMACSASCIIfile&gt; [convert_the_complement?(false)] [weightsfile] [create_graphfile_from_dimacs?(false)]</CODE>
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if (args.length<2) {
			System.err.println("usage: java -cp <classpath> "+
							           "utils.DIMACSASCIIFormatGraphFileMaker "+
							           "<input_graphfile> <output_DIMACSASCIIgraphfile> [convert_the_complement?(false)] [weightsfile(null)] [graph_from_dimacs?(false)]");
			System.exit(-1);
		}
		String input_file = args[0];
		String output_file = args[1];
		boolean do_complement = false;
		if (args.length>2) do_complement = Boolean.valueOf(args[2]).booleanValue();
		String weights_file = null;
		if (args.length>3 && "null".equals(args[3])==false) weights_file = args[3];
		boolean graph_from_dimacs = false;
		if (args.length>4) graph_from_dimacs = Boolean.valueOf(args[4]).booleanValue();
		if (graph_from_dimacs) {  // conversion goes opposite direction
			if (do_complement || weights_file!=null) {
				System.err.println("complement operation or existence of weights_file not allowed in converse DIMACS->graph_file transformation");
				System.exit(-1);
			}
			convertASCIIDIMACS2Graph2(output_file,input_file);
			return;
		}
		graph.Graph g = DataMgr.readGraphFromFile2(input_file);
		PrintWriter pw = new PrintWriter(new FileWriter(output_file));
		PrintWriter pw2 = weights_file==null ? null : new PrintWriter(new FileWriter(weights_file));
		// header
		pw.println("p edge "+g.getNumNodes()+" "+g.getNumArcs());
		// node weights
		for (int i=0; i<g.getNumNodes(); i++) {
			Double vi = g.getNodeUnsynchronized(i).getWeightValueUnsynchronized("value");
			if (Double.compare(vi.doubleValue(), Math.round(vi.doubleValue()))!=0) {
				System.err.println("Node ni-weight="+vi);
				System.exit(-1);
			}
			pw.println("n "+(i+1)+" "+vi.intValue());
			if (pw2!=null) pw2.println(vi);
		}
		if (pw2!=null) {
			pw2.flush();
			pw2.close();
		}
		// arcs
		if (!do_complement) {
			for (int i=0; i<g.getNumArcs(); i++) {
				graph.Link li = g.getLink(i);
				pw.println("e "+(li.getStart()+1)+" "+(li.getEnd()+1));
			}
		} else {
			for (int i=0; i<g.getNumNodes(); i++) {
				graph.Node ni = g.getNodeUnsynchronized(i);
				System.err.print("Computing edges for node "+i+"...");
				int cnt=0;
				Set ni_nbors = ni.getNborsUnsynchronized();
				for (int j=i+1; j<g.getNumNodes(); j++) {
					if (ni_nbors.contains(g.getNodeUnsynchronized(j))) continue;  // don't add arc
					pw.println("e "+(i+1)+" "+(j+1));
					++cnt;
				}
				System.err.println("added "+cnt+" edges");
			}
		}
		pw.flush();
		pw.close();
	}


	private static void convertASCIIDIMACS2Graph2(String dimacs_graph_file, String graph_file) throws IOException {
		BufferedReader bw = new BufferedReader(new FileReader(dimacs_graph_file));
		PrintWriter pw = new PrintWriter(new FileWriter(graph_file));
		String[] edges = null;
		double[] nwts = null;
		int acnt = 0;
		boolean exist_weights = false;
		while (true) {
			String line = bw.readLine();
			if (line==null) {  // EOF
				break;
			}
			StringTokenizer st = new StringTokenizer(line, " ");
			String f = st.nextToken();
			if ("p".equals(f)) {  // header
				st.nextToken();  // "edge"
				int numnodes = Integer.parseInt(st.nextToken());
				nwts = new double[numnodes];
				int numarcs = Integer.parseInt(st.nextToken());
				edges = new String[numarcs];
				pw.println(numarcs+" "+numnodes);
				continue;
			}
			if ("e".equals(f)) {  // edges
				edges[acnt++] = line.substring(1).trim();
				continue;
			}
			if ("n".equals(f)) {  // weights
				int nid = Integer.parseInt(st.nextToken());
				double nw = Double.parseDouble(st.nextToken());
				nwts[nid-1] = nw;
				exist_weights = true;
				continue;
			}
		}
		// print the graph
		for (int i=0; i<edges.length; i++) {
			pw.println(edges[i]);
		}
		for (int i=0; i<nwts.length; i++) {
			if (exist_weights) pw.println(nwts[i]);
			else pw.println("1.0");
		}
		pw.flush();
		pw.close();
	}
}
