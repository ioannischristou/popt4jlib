/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib.LIP;

import popt4jlib.IntArray1SparseVector;

/**
 * subclass of the <CODE>AdditiveSolver2</CODE> class, that performs variable
 * branching selection by choosing the free variable with the highest absolute
 * value for its cost-coefficient.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class AdditiveSolver3 extends AdditiveSolver2 {
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
			int ival = (int) Math.abs(c.getCoord(ixpos));
			if (ival>best_val) {
				best_val = ival;
				varid = ixpos;
			}
		}
		return varid;
	}
}
