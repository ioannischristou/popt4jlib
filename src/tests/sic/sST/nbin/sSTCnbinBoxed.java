package tests.sic.sST.nbin;

import popt4jlib.FiniteLRUFunctionCache;
import popt4jlib.FunctionIntf;
import java.util.HashMap;


/**
 * the class uses the <CODE>sSTCnbin</CODE> class with the functionality 
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
public class sSTCnbinBoxed implements FunctionIntf {
	
	private final double _Tmin = 0.01;  // default
	private FiniteLRUFunctionCache _fcache;
	
	
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
		sSTCnbin f = new sSTCnbin(Kr, Ko, L, lambda, p_l, h, p);
		_fcache = new FiniteLRUFunctionCache(f, null, 10000);
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
		// must create new array to set values for (s,S,T) evaluation
		// otherwise most meta-heuristic operators manipulating the solutions
		// might corrupt data
		double[] xeval = new double[x.length];
		// s
		xeval[0] = Math.round(x[0]);
		// S
		xeval[1] = Math.round(x[1]);
		if (xeval[1] < xeval[0]) {  // swap values
			double tmp = xeval[0];
			xeval[0] = xeval[1];
			xeval[1] = tmp;
		} else if (Double.compare(xeval[1], xeval[0])==0) {
			xeval[1]++;
		}
		// T
		if (x[2] <= _Tmin) xeval[2] = _Tmin;
		else xeval[2] = x[2];
		return _fcache.eval(xeval, params);
	}
}
