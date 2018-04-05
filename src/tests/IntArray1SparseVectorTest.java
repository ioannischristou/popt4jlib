/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tests;

import popt4jlib.IntArray1SparseVector;

/**
 * test-driver for the class implementing sparse integer vectors.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntArray1SparseVectorTest {
	public static void main(String[] args) {
		int n = 100;
		if (args.length>0) n = Integer.parseInt(args[0]);
		IntArray1SparseVector v = new IntArray1SparseVector(n);
		for (int i=0; i<n; i++) {
			v.setCoord(i, 1);
		}
		System.out.println("v="+v);
	}
}
