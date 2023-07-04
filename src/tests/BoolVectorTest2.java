package tests;

import popt4jlib.BoolVector;

/**
 * Second tester class for BoolVector objects, and in particular, the lastSetBit
 * method of the class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class BoolVectorTest2 {

	public static void main(String[] args) {
		int _numVars = 14;
		BoolVector v = new BoolVector(_numVars);
		for (int vid = v.lastSetBit()+1; vid < _numVars; vid++) v.set(vid);
		System.out.println("v="+v);
	}
}
