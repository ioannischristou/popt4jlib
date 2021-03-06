package tests.sic.sST.norm;

import popt4jlib.FiniteLRUFunctionCache;
import popt4jlib.FunctionIntf;
import popt4jlib.DblArray1Vector;
import java.util.HashMap;


/**
 * the class works as the class <CODE>sSTCnormBoxed</CODE> but the arguments are
 * s, D (representing the difference S-s) and T, which allows for specifying the
 * constraints 0 &le; D &le; D<sub>max</sub> directly on any meta-heuristic 
 * optimizing the function <CODE>sSTCnorm</CODE>. As in the 
 * <CODE>sSTCnormBoxed</CODE> class, the function evaluations are cached for 
 * fast lookup.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ssPlusDTCnormBoxed implements FunctionIntf {
	
	private final double _Tmin;
	private FiniteLRUFunctionCache _fcache;
	
	
	/**
	 * sole public constructor, simply calls the constructor of the base class.
	 * @param Kr double the review cost
	 * @param Ko double the ordering cost
	 * @param L double the lead-time
	 * @param mi double the mean demand in a time unit
	 * @param sigma double the standard deviation of the demand in a time unit
	 * @param h double the holding cost per unit of items per unit of time
	 * @param p double the penalty cost of back-orders per unit of items back-
	 * logged per unit of time
	 * @param p2 double the penalty cost of lost-sales per unit of items
	 */
	public ssPlusDTCnormBoxed(double Kr, double Ko, 
		                        double L, 
						 				      	double mi, double sigma, 
											      double h, double p, double p2) {
		sSTCnorm f = new sSTCnorm(Kr, Ko, L, mi, sigma, h, p, p2);
		_Tmin = Math.pow(3.5*sigma/mi, 2.0);
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
		xeval[0] = x[0];
		// S
		xeval[1] = xeval[0] + x[1];
		// T
		if (x[2] <= _Tmin) xeval[2] = _Tmin;
		else xeval[2] = x[2];
		return _fcache.eval(xeval, params);
	}
}
