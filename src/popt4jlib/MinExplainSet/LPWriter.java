package popt4jlib.MinExplainSet;

import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * class is responsible for writing a general LP, MIP, or pure Binary Program
 * in lp file format understood by most solvers today, including GUROBI, SCIP
 * and glpk. The problem is always specified as:
 * <PRE>
 * min c'x
 * s.t.
 * Ax &le; b
 * x &ge; 0
 * some x &isin; {0,1}
 * some x integer
 * </PRE>.
 * All coefficients in b, c are assumed integer, and in A are assumed double.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LPWriter {
	/**
	 * writes the Binary Programming problem min c'x subject to Ax &le; b, x
	 * binary to the lp-formatted file fname. The file must have extension ".lp".
	 * @param A double[][]
	 * @param b int[]
	 * @param c int[]
	 * @param fname String
	 */
	public static void writeLPFile(double[][]A, int[] b, int[] c, String fname) {
		final int rows = A.length;
		final int cols = A[0].length;
		try(PrintWriter pw = new PrintWriter(new FileWriter(fname))) {
			pw.println("Minimize");
			for (int i=0; i<c.length; i++) {
				if (c[i]!=0) {
					if (c[i]!=1) pw.print(" "+c[i]);
					pw.print(" x_"+i);
				}
				if (c[i]!=0 &&  // if-stmt above printed var name
					  i<c.length-1 &&  // this isn't the last var
					  nextNZCoeffIsPos(c,i))  // the next non-zero coefficient is positive 
					pw.print(" + ");
			}
			pw.println("\nSubject To");
			for (int i=0; i<rows; i++) {
				pw.print("c"+i+": ");  // constraint name
				for (int j=0; j<cols; j++) {
					if (A[i][j]!=0) {
						if (A[i][j]!=1.0) {
							if (Math.floor(A[i][j])==A[i][j]) {
								pw.print(" "+(int)A[i][j]);
							} else pw.print(" "+A[i][j]);
						}
						pw.print(" x_"+j);
					}
					if (A[i][j]!=0 && j<cols-1 && nextNZCoeffIsPos(A[i], j))
						pw.print(" + ");
				}
				pw.println(" <= " + b[i]);
			}
			pw.println("Binary");
			for (int j=0; j<cols; j++) pw.print("x_"+j+" ");
			pw.println();
			pw.flush();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * helper static function that directly writes a constraint in the file.
	 * @param pw PrintWriter object
	 * @param cnum int constraint number
	 * @param Ar float[] the coeffs of the row constraint
	 * @param br int the right hand side of the row constraint
	 */
	public static void writeRow(PrintWriter pw, int cnum, float[] Ar, int br) {
		pw.print("c"+cnum+": ");
		final int cols = Ar.length;
		for (int j=0; j<cols; j++) {
			if (Ar[j]!=0) {
				if (Ar[j]!=1.0) {
					if (Math.floor(Ar[j])==Ar[j]) {
						pw.print(" "+(int)Ar[j]);
					} else pw.print(" "+Ar[j]);
				}
				pw.print(" x_"+j);
			}
			if (Ar[j]!=0 && j<cols-1 && nextNZCoeffIsPos(Ar, j))
				pw.print(" + ");
		}
		pw.println(" <= " + br);		
	}
	
	
	/**
	 * checks if the first non-zero coefficient after position i is positive. If
	 * all coefficients after position i are zero, or if the first non-zero after
	 * i is negative, returns false.
	 * @param c int[]
	 * @param i int
	 * @return boolean
	 */
	public static boolean nextNZCoeffIsPos(int[] c, int i) {
		for (int j=i+1; j<c.length; j++) {
			if (c[j]>0) return true;
			else if (c[j]<0) return false;
		}
		return false;
	}
	
	
	/**
	 * checks if the first non-zero coefficient after position i is positive. If
	 * all coefficients after position i are zero, or if the first non-zero after
	 * i is negative, returns false.
	 * @param c double[]
	 * @param i int
	 * @return boolean
	 */
	public static boolean nextNZCoeffIsPos(double[] c, int i) {
		for (int j=i+1; j<c.length; j++) {
			if (c[j]>0) return true;
			else if (c[j]<0) return false;
		}
		return false;
	}

	
	/**
	 * checks if the first non-zero coefficient after position i is positive. If
	 * all coefficients after position i are zero, or if the first non-zero after
	 * i is negative, returns false.
	 * @param c float[]
	 * @param i int
	 * @return boolean
	 */
	public static boolean nextNZCoeffIsPos(float[] c, int i) {
		for (int j=i+1; j<c.length; j++) {
			if (c[j]>0) return true;
			else if (c[j]<0) return false;
		}
		return false;
	}
}
