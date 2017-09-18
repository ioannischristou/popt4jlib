/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib.LIP;

import popt4jlib.IntArray1SparseVector;

/**
 *
 * @author itc
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
 