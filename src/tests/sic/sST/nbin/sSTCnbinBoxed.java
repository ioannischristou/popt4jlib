package tests.sic.sST.nbin;

import java.util.HashMap;


/**
 * the class extends the <CODE>sSTCnbin</CODE> class with the functionality 
 * that it projects any argument for evaluation into the feasible region of the
 * class. Essentially it projects the arguments into their box-constrained 
 * feasible region, so that the constraints s,S integer, S &ge; s and 
 * T &gt; 0 are always satisfied before attempting a function evaluation. Notice 
 * that there are no additional checks on the system parameters (ie on the 
 * parameters K<sub>r</sub> K<sub>o</sub>, L etc.)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class sSTCnbinBoxed extends sSTCnbin {
	
	private final double _Tmin = 0.01;  // default
	
	
	/**
	 * sole public constructor, simply calls the constructor of the base class.
	 * @param Kr double the review cost
	 * @param Ko double the ordering cost
	 * @param L double the lead-time
	 * @param lambda double the rate of the demand arrivals in a time unit
	 * @param p_l double the parameter of the logarithmic distribution modeling 
	 * the quantity of demand asked per demand arrival
	 * @param h double the holding cost per unit of items per unit of time
	 * @param p double the penalty cost of back-orders per unit of items back-
	 * logged per unit of time
	 */
	public sSTCnbinBoxed(double Kr, double Ko, 
		                   double L, 
						 					 double lambda, double p_l, 
											 double h, double p) {
		super(Kr, Ko, L, lambda, p_l, h, p);
	}
	
	
	/**
	 * evaluates the (s,S,T) policy on the given parameters, projecting first the
	 * parameters back into the feasible region if needed. Fractional values for
	 * s and S are rounded into the nearest integer.
	 * @param arg Object  // double[]{s,S,T}
	 * @param params HashMap unused
	 * @return double
	 */
	public double eval(Object arg, HashMap params) {
		double[] x = (double[]) arg;
		// s
		x[0] = Math.round(x[0]);
		// S
		x[1] = Math.round(x[1]);
		if (x[1] <= x[0]) x[1] = x[0]+1;
		// T
		if (x[2] <= 0) x[2] = _Tmin;
		return super.eval(x, params);
	}
}
