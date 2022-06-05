package popt4jlib.LIP;

import popt4jlib.IntArray1SparseVector;

/**
 * test driver for <CODE>sortAsc(),sortDesc()</CODE> methods of class
 * <CODE>AdditiveSolver*</CODE>.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntArray1Test {
	public static void main(String[] args) {
		/*
		IntArray1SparseVector x = new IntArray1SparseVector(7);
		for (int i=0; i<7; i++) x.setCoord(i, -1);
		System.out.println("x="+x);
		for (int i=0; i<7; i++) x.setCoord(i, 0);
		System.out.println("x="+x);
		x.setCoord(0, 1);
		*/
		
		IntArray1SparseVector x = new IntArray1SparseVector(20);
		for (int i=4; i<10; i++) x.setCoord(i, i);
		for (int i=16; i<20; i++) x.setCoord(i, -i);
		System.err.println("x="+x);
		IntArray1SparseVector y = AdditiveSolverDepr.sortAsc(x);
		System.err.println("y="+y);
		IntArray1SparseVector z = AdditiveSolverDepr.sortDesc(x);
		System.err.println("z="+z);
		
	}
}
 