package popt4jlib.MinExplainSet;

import java.io.*;
import java.text.*;


/**
 * class allows for creating a mathematical programming problem model for the 
 * MinExplainSet problem in MPS format that is understood by virtually every 
 * optimization software available. The purpose is to obtain comparisons in
 * speed between the algorithms in this package, and modern Open-Source and 
 * commercial optimizers such as SCIP and GUROBI respectively.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class MPSWriter {

  private final static int _pos[] = {2, 5, 15, 25, 40, 50};
  private final static int _poslen[] = {2, 8, 8, 12, 8, 12};
	
	
	/**
	 * create an ASCII-based MPS-formatted text file describing a particular 
	 * MinExplainSet problem. The problem to be specified as a MIP is the 
	 * following:
	 * Given is the matrix A of dimensions |I| x |R| and a vector c of dimensions
	 * |R|. A[i][j] is 1 iff rule r_j "explains" instance d_i, 0 else. c[i] is 
	 * the number of variables that rule r_i contains (so that the objective is
	 * to find a set of rules with small number of variables each.) 
	 * The variables are x_i, i=1...|R| binary, indicate what rules are included
	 * in the solution; y_j, j=1...|I| binary, indicate if instance d_j is covered
	 * by the solution or not.
	 * The parameters b_i, i=1...|I| are set as b_i = \sum_{j=1}^|R| a[i][j].
	 * The constraints are the following 2 + 2|I| inequalities:
	 * <ul>
	 * <li>(1) \sum_{i=1}^|R| x_i &le; K (user-specified threshold)
	 * <li>(2) \sum_{i=1}^|I| y_i &ge; B (user-specified threshold)
	 * <li>(3) foreach i=1...|I|: \sum_{j=1}^|R| a[i][j]*x_j - b_i*y_i &le; 0
	 * <li>(4) foreach i=1...|I|: \sum_{j=1}^|R| a[i][j]*x_j - y_i &ge; 0
	 * </ul>
	 * and finally, the objective function is \sum_{i=1}^|R| c[i]*x_i.
	 * @param A int[][]
	 * @param c int[]
	 * @param K int
	 * @param B int
	 * @param filename
	 * @throws IOException 
	 */
  public static void writeMES2MPSFileFast(int[][] A, int[] c, int K, int B, 
		                                      String filename) throws IOException {
		final int R = A[0].length;
		final int I = A.length;
		// first, let's compute the array b:
		final double[] b = new double[I];
		for (int i=0; i<I; i++) {
			b[i] = 0;  // needless
			for (int j=0; j<R; j++) b[i] += A[i][j];
		}
		
    PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
    // write problem name
    pw.println("NAME           MESPROB");
    pw.println("ROWS");
    pw.println(" N  COST");
    int num_vars = R+I;
    int num_constrs = 2 + 2*I;
    // declare the first two rows
		// row (1) in the javadoc
		final StringBuffer l1 = newMPSLineBuffer();
		writeMPSFieldFast(l1, 0, "L");
		writeMPSFieldFast(l1, 1, "ROW1");
		pw.println(l1);
		// row (2) in the javadoc
		final StringBuffer l2 = newMPSLineBuffer();
		writeMPSFieldFast(l2, 0, "G");
		writeMPSFieldFast(l2, 1, "ROW2");
		pw.println(l2);
		pw.flush();
		// declare the first I rows (constraints (3) in the javadoc)
		for (int i=0; i<I; i++) {
      StringBuffer l = newMPSLineBuffer();
      writeMPSFieldFast(l, 0, "L");
      String ci = "3LO"+i;
      writeMPSFieldFast(l, 1, ci);
      pw.println(l);			
		}		
		// declare the last I rows (constraints (4) in the javadoc)
		for (int i=0; i<I; i++) {
      StringBuffer l = newMPSLineBuffer();
      writeMPSFieldFast(l, 0, "G");
      String ci = "4HI"+i;
      writeMPSFieldFast(l, 1, ci);
      pw.println(l);			
		}				
		pw.flush();
		
		// now the vars
    pw.println("COLUMNS");
		
		// specify all variables as integer
		StringBuffer lm = newMPSLineBuffer();
		writeMPSFieldFast(lm, 2, "IMARKER");
		writeMPSFieldFast(lm, 3, "'MARKER'");
		writeMPSFieldFast(lm, 5, "'INTORG'");
		pw.println(lm);
		
    pw.flush();
    // NumberFormat nf = new DecimalFormat("0.#######E0");
    NumberFormat nf = new DecimalFormat("00000000E0");
		// first, the x_i (rule indicators)
		for (int i=0; i<R; i++) {
      if (i%1000==0) 
				System.out.println("Now writing column x"+i);  // itc: HERE rm asap
      StringBuffer l = newMPSLineBuffer();
      writeMPSFieldFast(l, 1, "X"+i);
      writeMPSFieldFast(l, 2, "COST");
      double ci = c[i];
      String cival = nf.format(ci);
      writeMPSFieldFast(l, 3, cival);
      writeMPSFieldFast(l, 4, "ROW1");
      writeMPSFieldFast(l, 5, "1");
      pw.println(l);
      pw.flush();
			// next, for rows [0...|I]-1] write the coeffs of x_i for constr. set (3)
			boolean new_l = true;
			int r = -1;
			for (r=0; r<I; r++) {
				l = newMPSLineBuffer();
		    writeMPSFieldFast(l, 1, "X"+i);
				writeMPSFieldFast(l, 2, "3LO"+r);
				String ari = nf.format(A[r][i]);
				writeMPSFieldFast(l, 3, ari);
				if (r==I-1) {
					new_l = false;  // fill in the last 2 fields of l with the first of
					                // next constraint set (4)
					break;
				}
				writeMPSFieldFast(l, 4, "3LO"+(r+1));
				String arp1i = nf.format(A[r+1][i]);
				writeMPSFieldFast(l, 5, arp1i);
				++r;
				pw.println(l);
			}
			// next, for rows [0...|I]-1] write the coeffs of x_i for constr. set (4)
			if (!new_l) {  // must fill the last line l
				writeMPSFieldFast(l, 4, "4HI0");
				String ar0i = nf.format(A[0][i]);
				writeMPSFieldFast(l, 5, ar0i);
				pw.println(l);
				r = 1;
			} else r = 0;
			pw.flush();
			for (; r<I; r++) {
				l = newMPSLineBuffer();
		    writeMPSFieldFast(l, 1, "X"+i);
				writeMPSFieldFast(l, 2, "4HI"+r);
				String ari = nf.format(A[r][i]);
				writeMPSFieldFast(l, 3, ari);
				if (r==I-1) {
					pw.println(l);
					break;
				}
				writeMPSFieldFast(l, 4, "4HI"+(r+1));
				String arp1i = nf.format(A[r+1][i]);
				writeMPSFieldFast(l, 5, arp1i);
				++r;
				pw.println(l);
			}
		}
		pw.flush();
		// finally, the y_i (instance indicators)
		for (int i=0; i<I; i++) {
      if (i%1000==0) 
				System.out.println("Now writing column y"+i);  // itc: HERE rm asap
      StringBuffer l = newMPSLineBuffer();
      writeMPSFieldFast(l, 1, "Y"+i);
      writeMPSFieldFast(l, 2, "COST");
      writeMPSFieldFast(l, 3, "0");
      writeMPSFieldFast(l, 4, "ROW2");
      writeMPSFieldFast(l, 5, "1");
      pw.println(l);
      pw.flush();
			l = newMPSLineBuffer();
			writeMPSFieldFast(l, 1, "Y"+i);
			writeMPSFieldFast(l, 2, "3LO"+i);
			String mbi = nf.format(-b[i]);
			writeMPSFieldFast(l, 3, mbi);
			writeMPSFieldFast(l, 4, "4HI"+i);
			writeMPSFieldFast(l, 5, "-1");
			pw.println(l);
			pw.flush();
		}

		// specify all variables as integer
		lm = newMPSLineBuffer();
		writeMPSFieldFast(lm, 2, "IMARKER");
		writeMPSFieldFast(lm, 3, "'MARKER'");
		writeMPSFieldFast(lm, 5, "'INTEND'");
		pw.println(lm);
		pw.flush();
		
    // RHS
    pw.println("RHS");
		StringBuffer l = newMPSLineBuffer();
		writeMPSFieldFast(l, 1, "RHS1");
		writeMPSFieldFast(l, 2, "ROW1");
		String Kstr = nf.format(K);
		writeMPSFieldFast(l, 3, Kstr);
		writeMPSFieldFast(l, 4, "ROW2");
		String Bstr = nf.format(B);
		writeMPSFieldFast(l, 5, Bstr);
		pw.println(l);
		pw.flush();
		// all other constraints (3) and (4) have zero RHS, so no specification is
		// necessary according to the MPS format.
		
    // BOUNDS
    pw.println("BOUNDS");
    for (int i=0; i<R; i++) {
      l = newMPSLineBuffer();
      writeMPSFieldFast(l, 0, "UI");  // specifies upper bound of Integer VAR!
      writeMPSFieldFast(l, 1, "BOUND");
      writeMPSFieldFast(l, 2, "X"+i);
      writeMPSFieldFast(l, 3, "1");
      pw.println(l);
    }
    for (int i=0; i<I; i++) {
      l = newMPSLineBuffer();
      writeMPSFieldFast(l, 0, "UI");  // specifies upper bound of Integer VAR!
      writeMPSFieldFast(l, 1, "BOUND");
      writeMPSFieldFast(l, 2, "Y"+i);
      writeMPSFieldFast(l, 3, "1");
      pw.println(l);
    }
		
    // end
    pw.println("ENDATA");
    pw.flush();
    pw.close();
  }


  private static String writeMPSField(String line, int fieldnum, String val) 
		throws IOException {
    // line must be of length 61
    // assert
    if (line==null || line.length()!=61) 
			throw new IOException("incorrect line");
    String newline = line.substring(0, _pos[fieldnum]-1);
    int val_len = val.length()<=_poslen[fieldnum] ? 
			              val.length() : 
			              _poslen[fieldnum];
    String new_val = val.substring(0, val_len);  // cut-off the value of val if 
		                                             // it's too big
    newline += new_val;
    for(int i=0; i<_poslen[fieldnum]-val_len; i++) newline+=" ";
    if (line.length()>newline.length()) 
			newline += line.substring(newline.length());
    return newline;
  }
  private static void writeMPSFieldFast(StringBuffer line, int fieldnum, 
		                                    String val) throws IOException {
    // line must be of length 61
    // assert
    if (line==null || line.length()!=61) 
			throw new IOException("incorrect line: ["+line+"]");
    int val_len = val.length()<=_poslen[fieldnum] ? 
			              val.length() : _poslen[fieldnum];
    int endpos = _pos[fieldnum]-1+val_len;
    line.replace(_pos[fieldnum]-1, endpos, val);
    return;
  }
  private static String newMPSLine() {
    return "                                                             ";
  }
  private static StringBuffer newMPSLineBuffer() {
    return new StringBuffer("                               "+
			                      "                              ");
  }
  private static void writeSpaces(PrintWriter pw, int numspaces) {
    for (int i=0; i<numspaces; i++) pw.print(" ");
  }
	
}
