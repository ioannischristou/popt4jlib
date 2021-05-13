package tests.sic.sST.nbin;

import popt4jlib.FiniteLRUFunctionCache;
import popt4jlib.FunctionIntf;
import popt4jlib.DblArray1Vector;
import java.util.HashMap;


/**
 * the class works as the class <CODE>sSTCnbinBoxed</CODE> but the arguments are
 * s, D (representing the difference S-s) and T, which allows for specifying the
 * constraints 1 &le; D &le; D<sub>max</sub> directly on any meta-heuristic 
 * optimizing the function <CODE>sSTCnbin</CODE>. As in the 
 * <CODE>sSTCnbinBoxed</CODE> class, the function evaluations are cached for 
 * fast lookup.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ssPlusDTCnbinBoxed implements FunctionIntf {
	
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
	public ssPlusDTCnbinBoxed(double Kr, double Ko, 
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
	 * @param arg Object  // double[]{s,S,T}, but also accepts DblArray1Vector
	 * @param params HashMap unused
	 * @return double
	 */
	public double eval(Object arg, HashMap params) {
		double[] x = arg instanceof DblArray1Vector ? 
			             ((DblArray1Vector)arg).getDblArray1() :
			             (double[]) arg;
		// must create new array to set values for (s,S,T) evaluation
		// otherwise most meta-heuristic operators manipulating the solutions
		// might corrupt data
		double[] xeval = new double[3];
		// s
		xeval[0] = Math.round(x[0]);
		// S
		xeval[1] = xeval[0] + Math.round(x[1]);
		// T
		if (x[2] <= _Tmin) xeval[2] = _Tmin;
		else xeval[2] = x[2];
		return _fcache.eval(xeval, params);
	}
}
