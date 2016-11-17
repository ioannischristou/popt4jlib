/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tests;

import java.util.HashMap;
import popt4jlib.FunctionIntf;

/**
 * The argument boundaries are [-n, n]^n
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class QuadraticShiftedFunction implements FunctionIntf {
	
	public QuadraticShiftedFunction() {	
	}
	
	public double eval(Object arg, HashMap params) {
		double[] x = (double[]) arg;
		double y = 0.0;
		for (int i=0; i<x.length; i++) {
			y += (x[i]-(i+1))*(x[i]-(i+1));
		}
		return y;
	}
}
