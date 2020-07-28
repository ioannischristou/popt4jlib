/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tests;

import popt4jlib.DblArray1Vector;
import utils.DataMgr;

/**
 * tests the <CODE>util.DataMgr.readMatrixFromFileAndNormalizeCols()</CODE> 
 * method.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class MatrixN01ReadTest {
	public static void main(String[] args) {
		try {
			double[][] matrix = DataMgr.readMatrixFromFileAndNormalizeCols(args[0]);
			DblArray1Vector v = new DblArray1Vector(matrix[0]);
			System.err.println("first row="+v.toString());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
