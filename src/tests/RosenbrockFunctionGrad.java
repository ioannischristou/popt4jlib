package tests;

import popt4jlib.*;
import java.util.*;
import popt4jlib.GradientDescent.VecUtil;

/**
 * This class implements the Rosenbrock Function Gradient in n-dimensions.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */

public class RosenbrockFunctionGrad implements VecFunctionIntf {
  /**
   * public no-arg constructor
   */
  public RosenbrockFunctionGrad() {
  }


  /**
   * evaluates the Rosenbrock function gradient at the specified point x.
   * @param x VectorIntf
   * @param p HashMap unused
   * @return VectorIntf
   * @throws IllegalArgumentException if any of the arguments does not adhere
   * to the specifications.
   */
  public VectorIntf eval(VectorIntf x, HashMap p) {
    try {
      int n = x.getNumCoords();
      VectorIntf g = x.newInstance();  // x.newCopy();
			for (int i=0; i<n; i++) {
				g.setCoord(i, evalCoord(x, null, i));
			}
      return g;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("gradient cannot be evaluated "+
				                                 "with given arguments");
    }
  }


  /**
   * evaluate partial derivative at i.
   * @param x VectorIntf
   * @param p HashMap unused
   * @param i int
	 * @throws IllegalArgumentException
   * @return double
   */
  public double evalCoord(VectorIntf x, HashMap p, int i) {
		final int n = x.getNumCoords();
		if (i<0 || i>=n) 
			throw new IllegalArgumentException("i="+i+" is outside "+
			                                   "valid range [0,"+n+")");
		final double xi = x.getCoord(i);
		double gi = 0;
		if (i<n-1) {
			gi -= 2*(1.0 - xi);
			gi -= 400*(x.getCoord(i+1)-xi*xi)*xi;
		}
		if (i>0) {
			final double xim1 = x.getCoord(i-1);
			gi += 200*(xi - xim1*xim1);
		}
		return gi;
  }
	
	
	/**
	 * test driver of the Rosenbrock function gradient.
	 * @param args 
	 */
	public static void main(String[] args) {
		double[] x = new double[args.length];
		for (int i=0; i<x.length; i++) x[i] = Double.parseDouble(args[i]);
		VectorIntf xv = new DblArray1Vector(x);
		RosenbrockFunctionGrad g = new RosenbrockFunctionGrad();
		VectorIntf y = g.eval(xv, null);
		System.out.println("g(x="+xv+")="+y);
		analysis.GradApproximator gapprox = 
			new analysis.GradApproximator(new RosenbrockFunction());
		VectorIntf ya = gapprox.eval(xv, null);
		double normdiff = VecUtil.getEuclideanDistance(y, ya);
		System.out.println("ga="+ya);
		System.out.println("||g-ga||="+normdiff);
	}
}

