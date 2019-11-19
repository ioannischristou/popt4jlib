package tests.sic.rnqt.nbin;

import analysis.IntegralApproximator;
import cern.jet.stat.Gamma;
import java.util.HashMap;
import popt4jlib.DblArray1Vector;
import popt4jlib.FunctionIntf;
import popt4jlib.VectorIntf;
import utils.Pair;


/**
 * function implements the continuous-time long-run expected cost of a periodic
 * review, single echelon inventory control system facing exogenous demands 
 * generated in a period of length T, from a Negative Binomial process with 
 * parameters &lambda;&gt;0, p_l&gt;0. The system faces linear holding and 
 * backorder costs with cost rates h&gt;0, p&gt;0. It also faces 
 * fixed costs: 
 * fixed review cost per period Kr&ge;0, and fixed order cost Ko&ge;0. Finally, 
 * there is a constant lead-time for each order being placed equal to L&ge;0.
 * The control parameters then, are r (the reorder point), Q (the batch size),
 * and T (the review interval).
 * The code below uses the formulae 5-20, 5-22...5-24, 5-27...5-5-29 and 5-33 
 * in the Hadley-Whittin (1963) textbook "Analysis of Inventory Systems".
 * Numerical integration is performed for backorders costs estimation, as there 
 * are no closed form formulae easily available. The analysis package of the
 * popt4jlib library is used for this purpose.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2019</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RnQTCnbin implements FunctionIntf {
	private double _Kr;
	private double _Ko;
	double _L;
	double _lambda; double _pl;
	double _h, _p;
	
	
	private static ThreadLocal _cachedGammaVals = new ThreadLocal() {
    protected Object initialValue() {
      return null;
    }
  };
	
	
	// compile-time constants
	private final static double _ZERO_TOL = 1.e-18;  
	private final static double _INTEGRAL_APPROX_EPS = 1.e-6;
	private final static int _NUM_INTVLS = 10;
	private final static int _MAX_SIMPSON_REC_LVL = 1000;

	
	/**
	 * Function sole public constructor.
	 * @param Kr double the fixed review cost (per review)
	 * @param Ko double the fixed ordering cost (per order)
	 * @param L double the lead-time 
	 * @param lambda double the arrival rate of the Negative Binomial distribution
	 * @param pl double the process parameter of the Logarithmic distribution of 
	 * the Nbin (viewed as compound Poisson)
	 * @param h double linear holding cost rate
	 * @param p double linear backorder penalty cost rate
	 */
	public RnQTCnbin(double Kr, double Ko, double L, 
		               double lambda, double pl, 
									 double h, double p) {
		_Kr=Kr;
		_Ko=Ko;
		_L=L;
		_lambda=lambda;
		_pl = pl;
		_h=h;
		_p=p;
	}

	
	/**
	 * evaluate the long-run expected cost of a single echelon inventory control
	 * system facing Negative Binomial demands with linear holding and backorder 
	 * costs and fixed backorder cost as well, and
	 * fixed review and order costs controlled by (r,nQ,T) periodic review policy.
	 * @param x double[] representing r (order up-to point), Q (batch size), and
	 * T (review period length)
	 * @param param HashMap if not null, it may contain value for the Kr parameter
	 * @return double
	 * @throws IllegalStateException unchecked if the computations go awry, see
	 * method <CODE>evalBoth(x)</CODE>.
	 */
	public double eval(Object x, java.util.HashMap param) {
		utils.Pair p = evalBoth(x, param);
		return ((Double) p.getFirst()).doubleValue();
	} 
	

		
	/**
	 * evaluate the long-run expected cost of a single echelon inventory control
	 * system facing demands distributed according to the Negative Binomial 
	 * probability distribution, with linear holding and backorder costs and 
	 * fixed review and order costs controlled by (r,nQ,T) periodic review policy.
	 * It returns in a pair of doubles, both the actual value as well as the value
	 * when the order cost Ko is zero, forming a lower bound on the cost function.
	 * @param x double[] or popt4jlib.DblArray1Vector representing r, Q and T
	 * @param param HashMap  // may contain a &lt;"Ko",$val&gt; pair
	 * @return utils.Pair  // Pair&lt;Double result, Double lowerbound&gt;
	 * @throws IllegalStateException unchecked if Po is computed outside [0,1] or
	 * if any number computed turns out to be <CODE>Double.NaN</CODE>.
	 */
	utils.Pair evalBoth(Object x, java.util.HashMap param) {
		double[] xp;
		if (x instanceof double[]) xp = (double[])x;
		else xp = ((DblArray1Vector) x).getDblArray1();
		int s = (int)xp[0];
		int Q = (int)xp[1];
		double T = xp[2];
		double Ko = _Ko;
		if (param!=null && param.containsKey("Ko")) 
			Ko = ((Double) param.get("Ko")).doubleValue();

		double Po = porder(Q,T,_lambda, _pl);
		
		double r = -_lambda*_L/Math.log(1-_pl);
		double ltdem = r*_pl/(1-_pl);  // mean lead-time demand
		double perdem = -_lambda*T*_pl/((1-_pl)*Math.log(1-_pl));
		double y = (_Kr+Ko*Po)/T + _h*((Q+1)/2.0 + s - ltdem - perdem/2);
		double B = (_h+_p)*bP(s,Q,T,_L,_lambda,_pl);
		y += B;
		return new Pair(new Double(y), new Double(y-Ko*Po/T));
	}

	
	/**
	 * return the value of the fixed review cost for this function.
	 * @return double
	 */
	double getKr() {
		return _Kr;
	}
	
	
	/**
	 * return the value of the fixed ordering cost for this function.
	 * @return double
	 */
	double getKo() {
		return _Ko;
	}
	
	
	/**
	 * return the mean value of the demand for this function.
	 * @return double
	 */
	double getMeanDemand() {
		double r = -_lambda/Math.log(1.0-_pl);
		double utdem = r*_pl/(1.0-_pl);  // mean unit-time demand
		return utdem;
	}
	
	
	/**
	 * evaluate the pdf of the Negative Binomial distribution with parameters 
	 * lambda, pl, at point k.
	 * @param k int
	 * @param lambda double
	 * @param pl double 
	 * @return double
	 */
	public static double nbinpdf(int k, double lambda, double pl) {
		try {
			//double res = gc(k+lambda) / (fac(k)*gc(lambda));
			double res = 1.0;
			for (int i=1; i<=k; i++) {
				res *= (k+lambda-i)/i;
			}
			if (Double.isNaN(res)) {  // use log-exp trick
				res = Math.log(gc(k+lambda)) - Math.log(gc(lambda));
				for (int i=2; i<=k; i++) res -= Math.log(i);
				res = Math.exp(res);
			}
			if (Double.isNaN(res)) {
				throw new IllegalArgumentException("nbinpdf("+k+";"+
																						lambda+","+pl+") is NaN");
			}
			res *= Math.pow(pl, lambda) * Math.pow(1-pl, k);
			return res;
		}
		catch (Exception e) {
			System.err.println("RnQTCnbin.nbinpdf("+k+";"+lambda+","+pl+
				                 "): Exception caught");
			throw e;
		}
	}
	
	
	/**
	 * evaluate the cdf of the Negative Binomial distribution with parameters 
	 * lambda, pl, at point k.
	 * @param k int
	 * @param lambda double
	 * @param pl double 
	 * @return double
	 */
	public static double nbincdf(int k, double lambda, double pl) {
		double res = 0.0;
		for (int i=0; i<=k; i++)
			res += nbinpdf(i, lambda, pl);
		return res;
	}
	
	
	/**
	 * computes the complementary cdf of the Negative Binomial distribution with 
	 * parameters lambda, pl, at point k.
	 * @param k int
	 * @param lambda double
	 * @param pl double
	 * @return double
	 */
	public static double nbincdfcompl(int k, double lambda, double pl) {
		return 1.0 - nbincdf(k-1, lambda, pl);
	}
	
	
	/**
	 * evaluates the probability of ordering given the input parameters.
	 * @param Q int the batch-size
	 * @param T double the review length
	 * @param lambda double the arrival rate of the distribution
	 * @param p double the process parameter
	 * @return double must be in [0,1]
	 */
	public static double porder(int Q, double T, double lambda, double p) {
		double z = 0.0;
		for (int j=1; j<=Q; j++) 
			z += nbincdfcompl(j, -lambda*T/Math.log(1-p), 1-p);
		return z/Q;
	}
	
	
	/**
	 * computes the average long-run expected back-order costs of a single-echelon
	 * system facing demands that follow the Negative Binomial (ie compound 
	 * Poisson) distribution with arrival rate lambda and process parameter of the
	 * Logarithmic distribution p, with an (r,nQ,T) periodic-review policy, 
	 * taking into account fixed costs (review and ordering costs).
	 * @param s int order-up to point
	 * @param Q int batch-size
	 * @param T double review period length
	 * @param L double lead-time
	 * @param lambda double arrival rate of Nbin distribution
	 * @param p double process parameter for Nbin distribution
	 * @return double back-orders part of the total cost
	 */
	public static double bP(int s, int Q, double T, double L, 
		                      double lambda, double p) {
		double intgl = 0.0;
		for (int u=s+1; u<=s+Q; u++) {
			intgl += iP(T, L, new IIP(lambda, p, u));
		}
		return intgl / (Q*T);
	}
	
	
	static double iP(double T, double L, FunctionIntf iiP) {
		double len = T / _NUM_INTVLS;
		double z = 0;
		double left = L;
		if (Double.compare(Math.abs(left), _ZERO_TOL) < 0) {  // avoid singularity
			left = _ZERO_TOL;
		}
		for (int i=1; i<=_NUM_INTVLS; i++) {
			double right = left+len;
			z += integrate(iiP, left, right);
			left = right;
		}
		return z;
	}
	
	
	/**
	 * returns the value of the mathematical Gamma function at x. Uses a thread-
	 * local cache to look-up values that may have been computed before.
	 * @param x double must be &ge;0.
	 * @return double
	 * @throws IllegalArgumentException if x &lt; 0.
	 */
	public static double gc(double x) {
		if (Double.compare(x,0.0) <= 0) {
			throw new IllegalArgumentException("RnQTnbin.gc("+x+"): x<=0?");
		}
		HashMap cvals = (HashMap) _cachedGammaVals.get();
		if (cvals == null) {
			cvals = new HashMap();
			_cachedGammaVals.set(cvals);
		}
		Double xD = new Double(x);
		if (cvals.containsKey(xD)) {
			return ((Double) cvals.get(xD)).doubleValue();
		}
		else {
			// System.err.println("RnQTCnbin.gc(x): evaluating Gamma("+x+")");
			double res = Gamma.gamma(x);
			cvals.put(xD, new Double(res));
			return res;
		}
	}
	
	
	/**
	 * trivial implementation of the factorial of an integer.
	 * @param k int
	 * @return double k!
	 */
	public static double fac(int k) {
		double res = 1;
		for (int i=2; i<=k; i++) res *= i;
		return res;
	}
	
	
	/**
	 * helper static method to allow the integration of the function f according
	 * to its 1st variable in the interval [a,b]. The function f must accept 
	 * <CODE>VectorIntf</CODE> argument as input. The requested accuracy is 
	 * <CODE>_INTEGRAL_APPROX_EPS</CODE>.
	 * @param f FunctionIntf
	 * @param a double the left end of the interval.
	 * @param b double the right end of the interval
	 * @return double the result of applying Simpson rule for integration.
	 */
	public static double integrate(FunctionIntf f, double a, double b) {
		HashMap params = new HashMap();
		params.put("integralapproximator.eps", new Double(_INTEGRAL_APPROX_EPS));
		params.put("integralapproximator.levelmax", 
			         new Integer(_MAX_SIMPSON_REC_LVL));
		IntegralApproximator ia = new IntegralApproximator(f, params);
		params.clear();  // object reuse OK
		params.put("integralapproximator.a", new Double(a));
		params.put("integralapproximator.integrandvarindex", new Integer(0));
		double[] xb = new double[]{b};
		try {
			double res = ia.eval(xb, params);
			return res;
		}
		catch (Exception e) {
			// e.printStackTrace();
			System.err.println("RnQTnbin.integrate(f,"+a+","+b+
				                 "): evaluation failed");
			throw e;
		}
	}
	
	
	/**
	 * auxiliary class for integrating the (1-D) function described in 
	 * <CODE>eval()</CODE>. Not part of the public API.
	 */
	private static class IIP implements FunctionIntf {
		private double _lambda;
		private double _p;
		private int _u;
		
		public IIP(double lambda, double p, int u) {
			_lambda = lambda;
			_p = p;
			_u = u;
		}
		
		
		public double eval(Object tarr, HashMap params) {
			double t = ((VectorIntf) tarr).getCoord(0);
			double pm = 1-_p;
			double r = -_lambda*t/Math.log(pm);
			double mean = _p*r/pm;
			double sum = 0.0;
			int u1 = _u-1;
			for (int x=1; x<=u1; x++) {
				sum += x*nbinpdf(x,r,pm);
			}
			double ms = mean - sum;
			double last_term = _u==0 ? 0 : _u*nbincdfcompl(_u,r,pm);
			return ms - last_term;
		}
	}
	
	
	/**
	 * invoke as 
	 * <CODE>java -cp &lt;classpath&gt; tests.sic.rnqt.nbin.RnQTCnbin 
	 * &lt;r&gt; &lt;Q&gt; &lt;T&gt;
	 * &lt;Kr&gt; &lt;Ko&gt; &lt;L&gt; &lt;&lambda;&gt; &lt;p_l&gt; 
	 * &lt;h&gt; &lt;p&gt;</CODE>. 
	 * The constraints on the variables and parameters values are as follows:
	 * <ul>
	 * <li>T&gt;0
	 * <li>&lambda;&gt;0
	 * <li>p_l&gt;0
	 * <li>h&gt;0
	 * <li>p&gt;0
	 * </ul>
	 * All numbers (except r) must be non-negative. The reorder point variable r
	 * may take any integer value; the batch size Q may take any positive integer
	 * value.
	 * @param args String[]
	 */
	public static void main(String[] args) {
		int r = Integer.parseInt(args[0]);
		int Q = Integer.parseInt(args[1]);
		double T = Double.parseDouble(args[2]);
		double Kr = Double.parseDouble(args[3]);
		double Ko = Double.parseDouble(args[4]);
		double L = Double.parseDouble(args[5]);
		double lambda = Double.parseDouble(args[6]);
		double p_l = Double.parseDouble(args[7]);
		double h = Double.parseDouble(args[8]);
		double p = Double.parseDouble(args[9]);
		RnQTCnbin cc = new RnQTCnbin(Kr,Ko,L,lambda,p_l,h,p);
		double[] x = new double[]{r,Q,T};
		long start = System.currentTimeMillis();
		double val = cc.eval(x, null);
		long dur = System.currentTimeMillis()-start;
		System.out.println("y = "+val);
		System.out.println("duration="+dur+" msecs");
	}

}
