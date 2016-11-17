/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import java.util.HashMap;
import popt4jlib.*;

/**
 *
 * @author itc
 */
public class AVDTest2 {

	public static void main(String[] args) {
		HashMap params = new HashMap();
		FunctionIntf my_fun = new QuadraticShiftedFunction();  // replace the QuadraticShiftedFunction with anything you like
		FunctionIntf my_fun2 = new DblArray1Vector2DblArray1AdapterFunction(my_fun);
		params.put("avd.function", my_fun2);  // this is not really needed
		VectorIntf x0 = new DblArray1Vector(10);
		params.put("avd.x0", x0);
		params.put("avd.minargval", new Double(-10.0));
		params.put("avd.maxargval", new Double(10.0));
		params.put("avd.numtries", new Integer(10));
		// finally, run AVD
		popt4jlib.GradientDescent.AlternatingVariablesDescent optimizer = 
			new popt4jlib.GradientDescent.AlternatingVariablesDescent(params);
		try {
			utils.PairObjDouble result = optimizer.minimize(my_fun2);
			// get the best objective value found
			double best_value = result.getDouble();
			// get the minimizer vector
			double[] best_arg = (double[]) ((popt4jlib.VectorIntf)result.getArg()).getDblArray1();
			// done…
			System.err.println("best value found is=" + best_value);
			System.err.print("x=[ ");
			for (int i = 0; i < best_arg.length; i++) {
				System.err.print(best_arg[i] + " ");
			}
			System.err.println("]");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
