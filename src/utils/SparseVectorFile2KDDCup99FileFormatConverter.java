/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utils;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import popt4jlib.VectorIntf;


/**
 * converts a sparse-vector format file to the KDDCup99-formatted data file so 
 * that it can be read from tools such as Apache Spark.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class SparseVectorFile2KDDCup99FileFormatConverter {
	/**
	 * usage:
	 * <CODE>
	 * java -cp &lt;classpath&gt; 
	 * utils.SparseVectorFile2KDDCup99FileFormatConverter
	 * &lt;input_filename&gt; &lt;output_filename&gt;
	 * [max_num_rows_to_use(Integer.MAX_VALUE)] 
	 * </CODE>.
	 * @param args String[] 
	 */
	public static void main(String[] args) {
		String input_filename = args[0];
		String output_filename = args[1];
		int max_num_rows = Integer.MAX_VALUE;
		if (args.length>2) max_num_rows = Integer.parseInt(args[2]);
		try {
			List svs = DataMgr.readSparseVectorsFromFile(input_filename);
			int num_rows = svs.size();
			if (max_num_rows < num_rows) num_rows = max_num_rows;
			PrintWriter pw = new PrintWriter(new FileWriter(output_filename));
			for (int i=0; i<num_rows; i++) {
				VectorIntf vi = (VectorIntf) svs.get(i);
				final int dim = vi.getNumCoords();
				for (int j=0; j<dim; j++) {
					pw.print(vi.getCoord(j));
					if (j<dim-1) pw.print(",");
					else pw.println("");
				}
			}
			pw.flush();
			pw.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
		
}
