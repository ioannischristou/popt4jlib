package utils;

import java.util.Random;
import java.util.Vector;
import popt4jlib.IntArray1SparseVector;


/**
 * class responsible for creating (sparse) network assignment instances with 
 * integer weights.
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
	 * &lt;file_name_to_store_assignment_data&gt;
	 * </CODE>.
	 * @param args 
	 */
	public static void main(String[] args) {
		try {
			long start = System.currentTimeMillis();
			final int n = Integer.parseInt(args[0]);
			final double p_edge = Double.parseDouble(args[1]);
			final int max_weight = Integer.parseInt(args[2]);
			final String filename = args[3];
			Vector docs = new Vector();
			Random r = RndUtil.getInstance().getRandom();
			int num_edges=0;
			for (int i=0; i<n; i++) {
				IntArray1SparseVector di = new IntArray1SparseVector(n);
				for (int j=0; j<n; j++) {
					double rj = r.nextDouble();
					if (rj<p_edge) {  // ok, edge exists
						int aij = r.nextInt(max_weight)+1;
						di.setCoord(j, aij);
						++num_edges;
					}
				}
				docs.add(di);
			}
			DataMgr.writeSparseVectorsToFile(docs, n, filename);
			long dur = System.currentTimeMillis()-start;
			System.out.println("Total of "+num_edges+
				                 " created in assignment problem in "+dur+" msecs.");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
