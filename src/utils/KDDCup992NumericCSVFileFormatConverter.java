package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.StringTokenizer;

/**
 * converts a KDDCup99-formatted data file to a CSV file containing only numeric
 * values.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class KDDCup992NumericCSVFileFormatConverter {
	/**
	 * usage:
	 * <CODE>
	 * java -cp &lt;classpath&gt; utils.KDDCup992NumericCSVFileFormatConverter
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
			BufferedReader br = new BufferedReader(new FileReader(input_filename));
			PrintWriter pw = new PrintWriter(new FileWriter(output_filename));
			if (!br.ready()) throw new java.io.IOException("cannot read?");
			int cnt=0;
			while (cnt++<max_num_rows) {
				String line = br.readLine();
				if (line==null) break;  // EOF
				StringTokenizer st = new StringTokenizer(line,",");
				String vector_line = "";
				while (st.hasMoreTokens()) {
					String token = st.nextToken();
					try {
						double val = Double.parseDouble(token);
						if (vector_line.length()>0) vector_line += ",";
						vector_line += val;
					}
					catch (NumberFormatException e) {
						// ignore this column
					}
				}
				pw.println(vector_line);
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
	
}
