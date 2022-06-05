package popt4jlib.LIP;

import popt4jlib.IntArray1SparseVector;

/**
 * subclass of the <CODE>AdditiveSolverMT</CODE> class, that performs variable
 * branching selection by choosing the free variable with the highest value for 
 * its cost-coefficient (all of them are non-negative by the problem 
 * requirements).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2022</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.1
 */
public class AdditiveSolver3 extends AdditiveSolverMT {
	protected int selectVariable(Node current) {
		int varid = -1;
		IntArray1SparseVector c = getCVector();
		IntArray1SparseVector x = current.getX();
		int nz = x.getNumNonZeros();
		int best_val = Integer.MIN_VALUE;
		for (int i=0; i<nz; i++) {
			int ixpos = x.getIthNonZeroPos(i);
			int ixval = x.getIntIthNonZeroVal(i);
			if (ixval>=0) continue;  // only interested in free vars
			int ival = (int) c.getCoord(ixpos);
			if (ival>best_val) {
				best_val = ival;
				varid = ixpos;
			}
		}
		return varid;
	}
}
