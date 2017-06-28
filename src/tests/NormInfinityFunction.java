package tests;

import popt4jlib.*;
import java.util.*;


/**
 * The constant function trivially returns the value of the key "const" in the 
 * params, or zero if it can't find the key.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class NormInfinityFunction implements FunctionIntf {
  /**
   * public default no-arg constructor
   */
  public NormInfinityFunction() {
  }

  /**
   * return the max absolute value of the argument's components.
   * @param arg Object double[] or VectorIntf
   * @param params HashMap unused
	 * @throws IllegalArgumentException
   * @return double
   */
  public double eval(Object arg, HashMap params) 
		throws IllegalArgumentException {
		try {
			double[] x = (double[]) arg;
			double result = 0.0;
			for (int i=0; i<x.length; i++) {
				double xi = Math.abs(x[i]);
				if (xi>result) result = xi;
			}
			return result;
    }
    catch (ClassCastException e) {
			try {
				VectorIntf y = (VectorIntf) arg;
				int nd = y.getNumCoords();
				double result = 0.0;
				for (int i=0; i<nd; i++) {
					double xi = Math.abs(y.getCoord(i));
					if (xi>result) result = xi;
				}
				return result;
			}
			catch (ClassCastException e2) {
				e2.printStackTrace();
				throw new IllegalArgumentException("arg must be double[] or VectorIntf");
			}
		}
  }
}
