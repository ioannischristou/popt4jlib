package utils;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Vector;
import popt4jlib.IntArray1SparseVector;


/**
 * class responsible for creating (sparse) network assignment instances with 
 * positive integer weights. 
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RandomAsgnMaker {
	
	/**
	 * invoke as 
	 * <CODE>
	 * java -cp &lt;classpath&gt; utils.RandomAsgnMaker 
	 * &lt;num_persons&gt; &lt;prob_edge_exists&gt; &lt;max_weight&gt;
	 * &lt;file_name_to_store_assignment_data&gt; [ensure_feasibility?(false)]
	 * [file_name_to_store_asgn_as_mcnf(null)]
	 * </CODE>. Notice that if the second argument is an integer greater than 1,
	 * then it is assumed that the problem must be constructed so that there are
	 * as many arcs emanating from each person as the number described in that
	 * 2nd argument.
	 * @param args 
	 */
	public static void main(String[] args) {
		try {
			long start = System.currentTimeMillis();
			final int n = Integer.parseInt(args[0]);
			final double p_edge = Double.parseDouble(args[1]);
			final int max_weight = Integer.parseInt(args[2]);
			final String filename = args[3];
			final boolean ensure_feasibility = args.length>4 ? 
				                                   Boolean.parseBoolean(args[4]) : 
				                                   false;
			final String mcnf_file = args.length>5 ? args[5] : null;
			PrintWriter pw = mcnf_file!=null ? 
				                 new PrintWriter(new FileWriter(mcnf_file)) : 
				                 null;
			Vector docs = new Vector();
			Random r = RndUtil.getInstance().getRandom();
			int num_edges=0;
			for (int i=0; i<n; i++) {
				IntArray1SparseVector di = new IntArray1SparseVector(n);
				if (p_edge<=1.0) {
					for (int j=0; j<n-1; j++) {
						double rj = r.nextDouble();
						if (rj<p_edge) {  // ok, edge exists
							int aij = r.nextInt(max_weight)+1;
							if (di.getCoord(j)==0) ++num_edges;
							di.setCoord(j, aij);
						}
					}
					if (ensure_feasibility) {
						if (di.getCoord(i)==0) ++num_edges;
						di.setCoord(i, 1.0);
					}
					else {
						double rj = r.nextDouble();
						if (rj<p_edge) {  // ok, edge exists
							int aij = r.nextInt(max_weight)+1;
							if (di.getCoord(n-1)==0) ++num_edges;
							di.setCoord(n-1, aij);
						}						
					}
					if (di.isAtOrigin()) {  // set all objects at value 1
						for (int j=0; j<n; j++) {
							di.setCoord(j, 1);
							++num_edges;
						}
					}
					docs.add(di);
				}
				else {
					for (int j=0; j<p_edge-1; j++) {
						int k = r.nextInt(n);  // coord
						int aik = r.nextInt(max_weight)+1;  // weight
						if (di.getCoord(k)==0) ++num_edges;
						di.setCoord(k, aik);
					}
					if (ensure_feasibility) {
						if (di.getCoord(i)==0) ++num_edges;
						di.setCoord(i, 1.0);
					}
					else {
						int k = r.nextInt(n);  // coord
						int aik = r.nextInt(max_weight)+1;  // weight
						if (di.getCoord(k)==0) ++num_edges;
						di.setCoord(k, aik);						
					}
					docs.add(di);
				}
			}
			DataMgr.writeSparseVectorsToFile(docs, n, filename);
			// write problem in mcnf format too if requested
			if (pw!=null) {
				pw.println(n+n+2);  // num nodes
				pw.println(num_edges+n+n);  // num arcs
				// node supplies
				pw.println(n);  // source node
				final int n2 = n+n;
				for (int  i=0; i<n2; i++)
					pw.println(0);
				pw.println(-n);  // sink node
				// arc data
				for (int i=1; i<=n; i++) {  // source to persons
					pw.println("1 " + i + " 0 1");
				}
				for (int i=0; i<docs.size(); i++) {  // persons to objects
					IntArray1SparseVector di = (IntArray1SparseVector) docs.get(i);
					for (int j=0; j<di.getNumNonZeros(); j++) {
						int pos = di.getIthNonZeroPos(j);
						int pn2 = pos+n+2;
						int val = di.getIthNonZeroVal(j);
						int nv = -val;
						int i2 = i+2;
						pw.println(i2+" "+pn2+" "+nv+" 1");
					}
				}
				for (int i=n+2; i<n2+2; i++) {  // objects to sink
					pw.println(i+" "+(n2+2)+" 0 1");
				}
				pw.println("100");  // debug level
				pw.flush();
				pw.close();
			}
			long dur = System.currentTimeMillis()-start;
			System.out.println("Total of "+num_edges+
				                 " arcs created in assignment problem in "+
				                 dur+" msecs.");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
