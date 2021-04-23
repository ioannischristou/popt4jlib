package tests.sic.rnqt.norm;

import java.util.HashMap;


/**
 * the class extends the <CODE>RnQTCnorm</CODE> class with the functionality 
 * that it projects any argument for evaluation into the feasible region of the
 * class. Essentially it projects the arguments into their box-constrained 
 * feasible region, so that the constraints Q &ge; 0 and T &ge; T<sub>min</sub>
 * are always satisfied before attempting a function evaluation. Notice that 
 * there are no additional checks on the system parameters (ie on the parameters
 * K<sub>r</sub> K<sub>o</sub>, L etc.)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RnQTCnormBoxed extends RnQTCnorm {
	
	private final double _Tmin;
	
	
	/**
	 * sole public constructor, simply calls the constructor of the base class.
	 * @param Kr double the review cost
	 * @param Ko double the ordering cost
	 * @param L double the lead-time
	 * @param mi double the mean of the demand in a time unit
	 * @param sigma double the standard deviation of the demands in a time unit
	 * @param h double the holding cost per unit of items per unit of time
	 * @param p double the penalty cost of back-orders per unit of items back-
	 * logged per unit of time
	 */
	public RnQTCnormBoxed(double Kr, double Ko, 
		                    double L, 
												double mi, double sigma, 
												double h, double p) {
		super(Kr, Ko, L, mi, sigma, h, p);
		_Tmin = Math.pow(3.5*_sigma/_mi, 2);
	}
	
	
	/**
	 * evaluates the (R,nQ,T) policy on the given parameters, projecting first the
	 * parameters back into the feasible region if needed.
	 * @param arg Object  // double[]{R,Q,T}
	 * @param params HashMap unused
	 * @return double
	 */
	public double eval(Object arg, HashMap params) {
		double[] x = (double[]) arg;
		if (x[1] <= _Qtol) x[1] = _Qtol;
		if (x[2] < _Tmin) x[2] = _Tmin;
		return super.eval(x, params);
	}
}
