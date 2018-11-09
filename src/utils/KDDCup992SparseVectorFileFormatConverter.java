/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

/**
 * converts a KDDCup99-formatted data file to a sparse-vector format file so 
 * that it can be read from methods such as 
 * <CODE>DataMgr.readSparseVectorsFromFile(file)</CODE>.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class KDDCup992SparseVectorFileFormatConverter {
	/**
	 * usage:
	 * <CODE>
	 * java -cp &lt;classpath&gt; utils.KDDCup992SparseVectorFileFormatConverter
	 * &lt;input_filename&gt; &lt;output_filename&gt;
	 * [max_num_rows_to_use(Integer.MAX_VALUE)] 
	 * </CODE>
	 * @param args String[] 
	 */
	public static void main(String[] args) {
		String input_filename = args[0];
		String output_filename = args[1];
		int max_num_rows = Integer.MAX_VALUE;
		if (args.length>2) max_num_rows = Integer.parseInt(args[2]);
		try {
			int num_rows = getNumRows(input_filename, max_num_rows);
			if (max_num_rows < num_rows) num_rows = max_num_rows;
			BufferedReader br = new BufferedReader(new FileReader(input_filename));
			PrintWriter pw = new PrintWriter(new FileWriter(output_filename));
			if (!br.ready()) throw new java.io.IOException("cannot read?");
			int dims = 0;
			int cnt=0;
			while (cnt++<num_rows) {
				String line = br.readLine();
				if (line==null) break;  // EOF
				StringTokenizer st = new StringTokenizer(line,",");
				int dim=0;
				String sparse_vector_line = "";
				while (st.hasMoreTokens()) {
					String token = st.nextToken();
					try {
						double val = Double.parseDouble(token);
						++dim;
						if (Double.compare(val, 0.0)!=0) {
							if (sparse_vector_line.length()>0) sparse_vector_line += " ";
							sparse_vector_line += dim+","+val;
						}
					}
					catch (NumberFormatException e) {
						// ignore this column
					}
				}
				if (dim>dims) {  // first time
					dims = dim;
					pw.println(num_rows+" "+dims);
				}
				pw.println(sparse_vector_line);
				pw.flush();
			}
			br.close();
			pw.flush();
			pw.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	
	public static int getNumRows(String filename, int max_num_rows) 
		throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));
		int num_rows = 0;
		while (true) {
			String line = br.readLine();
			if (line==null) break;
			if (++num_rows==max_num_rows) break;  // reached the max we need
		}
		br.close();
		return num_rows;
	}
}
