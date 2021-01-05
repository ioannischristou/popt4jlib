package tests.sic.sST.norm;

import analysis.IntegralApproximator;
import utils.Messenger;
import popt4jlib.FunctionIntf;
import popt4jlib.VectorIntf;
import popt4jlib.DblArray1Vector;
import cern.jet.random.Normal;
import java.util.HashMap;


/**
 * function implements the continuous-time long-run expected cost of a periodic
 * review, single echelon inventory control system facing exogenous demands 
 * generated in a period of length T, from a normal process with parameters 
 * N(&mu; T, &sigma;<sup>2</sup>T) with &mu;,&sigma;&gt;0. The system faces 
 * linear holding and backorder costs with cost rates h&gt;0 and p&gt;0 and 
 * optionally, lost-sales costs p&#770;&ge;0.
 * It also faces fixed costs: fixed review cost per period Kr&ge;0, and fixed 
 * order cost Ko&ge;0. 
 * Finally, there is a constant lead-time for each order being placed equal to 
 * L&ge;0.
 * The control parameters then, are s (the reorder point), S (the order-up-to 
 * point), and T (the review interval). It is known that the policy itself is 
 * the optimal policy for controlling the long-run expected costs of such a 
 * single-echelon installation.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class sSTCnorm implements FunctionIntf {
	final double _Kr;
	final double _Ko;
	final double _L;
	final double _mi;
	final double _sigma;
	final double _h;
	final double _p;   // Hadley-Whitin p-hat -ie penalty that multiplies
	                   // the time an order is in the books
	final double _p2;  // Hadley-Whitin p -ie penalty incurred if an
	                   // order cannot be satisfied directly from the 
	                   // stock on-hand (cost of lost-sale)
	private final static Normal _norm = new Normal(0,1,null);  // _norm always 
	                                                           // available, even
	                                                           // after being
	                                                           // transmitted over
	                                                           // wire (sockets).
	
	private static final Messenger _mger = Messenger.getInstance();
	
	/**
	 * compile-time constants.
	 */
	private final static double _eps = 1.e-8;	
	private final static double _P0tol = 1.e-4;
	private final static double _ONE_TOL = 1.0 - _P0tol;
	private final static double _ONE_TOL_RELAXED = 0.99;  // last chance...
	private final static double _ZERO_TOL = -_P0tol;
	private final static double _POS_ZERO_TOL = 1.e-18;
	private final static double _SUM_TOL = 1.e-5;
	private final static double _SUM_CONV_TOL = 1.e-24;
	private final static int _MAX_NUM_CONVS = 1000;
	private final static double _INTEGRAL_APPROX_EPS = 1.e-6;
	private final static int _NUM_INTVLS = 10;
	private final static int _MAX_NUM_INTS = 1000;
	private final static int _NUM_LAST_INT_ADDS = 20;
	private final static int _MIN_NUM_INT_ADDS = 50;
	private final static int _MAX_SIMPSON_REC_LVL = 1000;
	private final static int _INTEGRATION_NUM_PIECES = 10;
	
	
	/**
	 * Function sole public constructor.
	 * @param Kr double
	 * @param Ko double
	 * @param L double
	 * @param mi double
	 * @param sigma double
	 * @param h double
	 * @param p double 
	 * @param p2 double
	 */
	public sSTCnorm(double Kr, double Ko, double L, 
		               double mi, double sigma, 
									 double h, double p, double p2) {
		_Kr=Kr;
		_Ko=Ko;
		_L=L;
		_mi=mi;
		_sigma=sigma;
		_h=h;
		_p=p;
		_p2=p2;
	}
	
	
	/**
	 * evaluate the long-run expected cost of a single echelon inventory control
	 * system facing normal demands with linear holding and backorder costs as 
	 * well as lost-sales costs, and fixed review and order costs controlled by 
	 * the (s,S,T) periodic review policy.
	 * @param x double[] representing s, S and T or <CODE>DblArray1Vector</CODE>
	 * @param param HashMap  // may contain a &lt;"Kr",$val&gt; pair
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
	 * system facing normal demands with linear holding and backorder costs and
	 * optionally lost-sales costs, and fixed review and order costs controlled by 
	 * the (s,S,T) periodic review policy.
	 * It returns a pair of doubles, both the actual value as well as the value
	 * when the order cost Ko is zero, forming a lower bound on the cost function.
	 * @param x double[] or popt4jlib.DblArray1Vector representing s, S and T
	 * @param param HashMap  // may contain a &lt;"Kr",$val&gt; pair
	 * @return utils.Pair  // Pair&lt;Double result, Double lowerbound&gt;
	 * @throws IllegalStateException unchecked if Po is computed outside [0,1] or
	 * if any number computed turns out to be <CODE>Double.NaN</CODE>.
	 */
	utils.Pair evalBoth(Object x, HashMap param) {
		double[] xp;
		if (x instanceof double[]) xp = (double[])x;
		else xp = ((DblArray1Vector) x).getDblArray1();
		double s = xp[0];
		double S = xp[1];
		double T = xp[2];
		// convert to Hadley-Whittin notation
		double J = (param==null || !param.containsKey("Kr")) ? 
			           _Kr :
			           ((Double) param.get("Kr")).doubleValue();
		double A = _Ko;
		double r = s;
		double R = S;
		double l = _mi;
		double m = _mi*_L;
		double t = _L;
		double phat = _p;
		double p2 = _p2;
		double IC = _h;
		
		double K1 = J/T;
		try {
			final double nom = sum1(r,R,T,t,l,_sigma,m,IC,phat,p2,_eps) + 
				           H(R,T,t,l,_sigma,m,IC,phat,p2);
			//_mger.msg("sSTCnorm.evalBoth(): sum1() returns "+nom, 3);
			final double cmpl = complnormcdf(R-r, l*T, _sigma*Math.sqrt(T));
			//_mger.msg("sSTCnorm.evalBoth(): complnormcdf() returns "+cmpl, 3);
			final double sum2v = sum2(r,R,T,l,_sigma,_eps, cmpl);
			//_mger.msg("sSTCnorm.evalBoth(): sum2() returns "+sum2v, 3);
			final double denom = sum2v + cmpl;
			final double y = K1 + (A+nom)/(T*denom);
			final double lb = K1 + nom/(T*denom);
			final double Po = 1.0/denom;
			//_mger.msg("sSTCnorm.evalBoth(s="+s+",S="+S+"): y="+y+", lb="+lb+"\n"+
			//	        "                                  : Po="+Po+",nom="+nom+"\n"+
			//	        "                                  : sum2="+sum2v+
			//	        ", denom="+denom, 3);
			return new utils.Pair(new Double(y), new Double(lb));
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException("sSTCnorm.evalBoth(): failed");
		}
	} 

	
	/**
	 * evaluates holding and backorder linear costs and adds to them the lost 
	 * sales (optional) costs (if p2&gt;0).
	 * @param rpj double
	 * @param T double
	 * @param t double
	 * @param l double
	 * @param sigma double
	 * @param m double
	 * @param IC double
	 * @param phat double
	 * @param p2 double
	 * @return double
	 */
	private static double H(double rpj, double T, double t, 
		                      double l, double sigma, double m, 
													double IC, double phat, double p2) {
		//_mger.msg("sSTCnorm.H() called", 3);
		double z = 0.0;
		if (p2>0.0) {
			z = p2*eP(rpj,T,l,sigma,t);
		}
		double res = IC*T*(rpj - m - l*T/2) + (IC+phat)*bP(rpj,T,l,sigma,t) + z;
		//_mger.msg("sSTCnorm.H() returns "+res, 3);
		return res;
	}
	
	
	/**
	 * compute linear back-order costs in an (s,S,T) echelon installation facing 
	 * normal demands.
	 * @param rpj double
	 * @param T double
	 * @param l double
	 * @param sigma double
	 * @param t double
	 * @return double
	 */
	static double bP(double rpj, double T, 
		                       double l, double sigma, double t) {
		//_mger.msg("sSTCnorm.bP() called", 3);
		double x = rpj, mi=l, L=t;
		double t1 = (sigma*sigma)*(W1(x,L+T,mi,sigma) - W1(x,L,mi,sigma));
		double t2 = mi*(V1(x,L+T,mi,sigma)-V1(x,L,mi,sigma)) - 
			          x*(V0(x,L+T,mi,sigma)-V0(x,L,mi,sigma));
		double res = t1 + t2;
		//_mger.msg("sSTCnorm.bP() returns "+res, 3);
		return res;
	}
	
	
	/**
	 * compute fixed penalty costs (lost-sales) in an (s,S,T) echelon installation
	 * facing normal demands. Requires the computation of an improper integral 
	 * (that is, the integral of the function <CODE>FH</CODE> on the interval 
	 * [rpj, +Inf)).
	 * @param rpj double
	 * @param T double
	 * @param l double
	 * @param sigma double
	 * @param t double
	 * @return double
	 */
	static double eP(double rpj, double T, 
		                       double l, double sigma, double t) {
		//_mger.msg("sSTCnorm.eP() called", 3);
		FH fh = new FH();
		double stepsize = 100.0;
		double a = rpj;
		double b = rpj+stepsize;
		double z = 1;
		double y = 0;
		DblArray1Vector x0 = new DblArray1Vector(6);
		x0.setCoord(0, b);
		x0.setCoord(1, rpj);
		x0.setCoord(2, T);
		x0.setCoord(3, l);
		x0.setCoord(4, sigma);
		x0.setCoord(5, t);
		while (Math.abs(z)>_POS_ZERO_TOL) {
			z = integrate(fh, a, b, x0);
			y += z;
			a = b;
			b += stepsize;
		}
		//_mger.msg("sSTCnorm.eP() returns "+y, 3);
		return y;
	}
	
	
	/**
	 * computes the sum in the nominator of the fraction that represents the 
	 * holding and backorder (and possibly lost-sales) costs.
	 * @param r double reorder point
	 * @param R double order-up-to point
	 * @param T double review period length
	 * @param t double lead-time 
	 * @param l double demand rate miu
	 * @param sigma double demand standard deviation
	 * @param m double lead-time demand rate
	 * @param IC double holding cost rate
	 * @param phat double back-order penalty cost rate
	 * @param p2 double lost-sales penalty cost
	 * @param ceps double convergence threshold for the summation
	 * @return double
	 * @throws Exception 
	 */
	private static double sum1(double r, double R, double T, 
		                         double t, double l, double sigma, double m, 
											       double IC, double phat, double p2,
											       double ceps) throws Exception {
		final FunctionIntf fin2 = new FInner2();
		//_mger.msg("sSTCnorm.sum1() called", 3);
		double y = 0;
		int n=1;
		double last = 0; 
		int count = 0;
		VectorIntf x0 = new DblArray1Vector(12);
		// r(1),R(2),T(3),l(4),sigma(5),t(6),n(7),m(8),IC(9),phat(10),p2(11)
		x0.setCoord(0, R-r);
		x0.setCoord(1, r);
		x0.setCoord(2, R);
		x0.setCoord(3, T);
		x0.setCoord(4, l);
		x0.setCoord(5, sigma);
		x0.setCoord(6, t);
		x0.setCoord(7, n);
		x0.setCoord(8, m);
		x0.setCoord(9, IC);
		x0.setCoord(10, phat);
		x0.setCoord(11, p2);
		while (true) {
			double sum = integrate(fin2, 0, R-r, x0);
      // itc20181006: the integral may run into instabilities resulting in zero
      // values because the shape of the function fin2 may come to look like
      // a gaussian with extrememly narrow spread, in which case, the numerical
      // integrator may come to think of the result as zero; in such cases
      // we break up the interval [0,R-r] into _NUM_INTVLS intervals, integrate
      // over each one, and add up the results.
			if (!Double.isFinite(sum) || Math.abs(sum)<_SUM_TOL) {
				//_mger.msg("sSTCnorm.sum1(): breaking [0,"+(R-r)+"] into "+
				//	        _NUM_INTVLS+" intervals", 3);
				double sz = (R-r)/_NUM_INTVLS;
				double ll=0, ul=sz;
				sum = 0;
				for (int i=0; i<_NUM_INTVLS; i++) {
					x0.setCoord(0, ul);  // redundant
					double saux = integrate(fin2, ll, ul, x0);
					if (Double.isFinite(saux)) sum += saux;
					ll = ul;
					ul += sz;
				}
				//_mger.msg("sSTCnorm.sum1(): integration res on [0,R-r] is "+sum, 3);
			}
			y += sum;
			last += sum;
			if (++count==_NUM_LAST_INT_ADDS) { 
			  if (n >= _MIN_NUM_INT_ADDS) {  // ensure we add at least this many terms
					//_mger.msg("sSTCnorm.sum1(): last="+last+", y="+y, 3);
					if (Math.abs(last/y) < ceps || 
							(Math.abs(last)<_SUM_CONV_TOL && Math.abs(y) < _SUM_CONV_TOL)) {
						//_mger.msg("sSTCnorm.sum1() returns "+y, 3);
						return y;
					} else {
						count = 0;
						last = 0;
					}
				} else {
					count = 0;
					last = 0;
				}
			}
			if (++n == _MAX_NUM_CONVS) {
				throw new IllegalStateException("sum1: too many convolutions needed?");
			}
			else {
				x0.setCoord(7, n);  // change n
				//_mger.msg("sSTCnorm.sum1(): added up "+n+"-th conv. term, y="+y, 3);
			}
		}
	}
	
	
	/**
	 * computes the sum of the first term in the denominator of the fraction that
	 * represents holding and penalty costs. This sum, together with the 
	 * complementary norm CDF at the right point, represents the inverse of the 
	 * probability of ordering Po.
	 * @param r double reorder point
	 * @param R double order-up-to point
	 * @param T double review period length
	 * @param l double demand mean rate
	 * @param sigma double demand standard deviation
	 * @param ceps double convergence threshold for the summation
	 * @param cmpl double used to check that the denom is greater than or equal to 
	 * one.
	 * @return double
	 * @throws Exception 
	 */
	private static double sum2(double r, double R, double T, 
		                         double l, double sigma,
		                         double ceps,
														 double cmpl) throws Exception {
		final FunctionIntf fin3 = new FInner3();
		//_mger.msg("sSTCnorm.sum2() called", 3);
		VectorIntf x0 = new DblArray1Vector(7);
		// r(1),R(2),T(3),l(4),sigma(5),n(6)
		x0.setCoord(0, R-r);
		x0.setCoord(1, r);
		x0.setCoord(2, R);
		x0.setCoord(3, T);
		x0.setCoord(4, l);
		x0.setCoord(5, sigma);
		int numintvls = _NUM_INTVLS/10;
		double y0 = Double.NaN;  // last effort
		while (true) {  // keep computing entire sum until y+cmpl>=1
			double y = 0.0;
			double last = 0; 
			int count = 0;
			int n=2;
			x0.setCoord(6, n);
			numintvls *= 10;
			if (numintvls > _MAX_NUM_INTS) {
				if (y0+cmpl >= _ONE_TOL_RELAXED) return y0;
				throw new IllegalStateException("sSTCnorm.sum2(): numintvls="+numintvls+
					                              " which is too much...");
			} 
			while (true) {
				double sum = integrate(fin3, 0, R-r, x0);
				// itc20181006: the integral may run into instabilities resulting in 0
				// values because the shape of the function fin3 may come to look like
				// a gaussian with extrememly narrow spread, in which case the numerical
				// integrator may come to think of the result as zero; in such cases
				// we break up the interval [0,R-r] into _NUM_INTVLS intervals integrate
				// over each one, and add up the results.
				if (!Double.isFinite(sum) || Math.abs(sum)<_SUM_TOL) {
					//_mger.msg("sSTCnorm.sum2(): breaking [0,"+(R-r)+"] into "+
					//					numintvls+" intervals", 4);
					double sz = (R-r)/numintvls;
					double ll=0, ul=sz;
					sum = 0;
					for (int i=0; i<numintvls; i++) {
						x0.setCoord(0, ul);  // redundant
						double saux = integrate(fin3, ll, ul, x0);
						if (Double.isFinite(saux)) {
							sum += saux;
						}
						ll = ul;
						ul += sz;
					}
					//_mger.msg("sSTCnorm.sum2(): integration res on [0,R-r] is "+sum,4);
				}
				y += sum;
				last += sum;
				if (++count==_NUM_LAST_INT_ADDS) {
					if (n >= _MIN_NUM_INT_ADDS) {  // ensure adding at least so many terms
						//_mger.msg("sSTCnorm.sum2(): last="+last+", y="+y, 4);
						if (Math.abs(last/y) < ceps || 
								(Math.abs(last)<_SUM_CONV_TOL && Math.abs(y) < _SUM_CONV_TOL)) {
							if (y+cmpl >= _ONE_TOL) {  // OK
								//_mger.msg("sSTCnorm.sum2() returns "+y+" w/ n="+n, 3);
								return y;
							}
							else {  // NOT OK
								//_mger.msg("sSTCnorm.sum2(): y+cmpl="+(y+cmpl)+" (y="+y+")@n="+
								//					n+" starting over",4);
								y0 = y;
								break;
							}
						} else {
							count = 0;
							last = 0;
						}
					}
					else {
						count = 0;
						last = 0;
					}
				}
				if (++n == _MAX_NUM_CONVS) {
					throw new IllegalStateException("sum2: too many convolutions reqd?");
				}
				else {
					x0.setCoord(6, n);  // change n
					//_mger.msg("sSTCnorm.sum2(): added up "+n+"-th convolution term", 3);
				}
			}  // adding convolution terms
		}  // keep computing entire sum until sum+cmpl>=1
	}
	
	
	/**
	 * corresponds to the function V<sub>0</sub> in Hadley-Whitin's analysis.
	 * @param x double
	 * @param T double
	 * @param m double
	 * @param s double
	 * @return double
	 */
	private static double V0(double x, double T, double m, double s) {
		double xstn = (x-m*T)/(s*Math.sqrt(T));
		if (!Double.isFinite(xstn)) xstn=0;
		double xstp = (x+m*T)/(s*Math.sqrt(T));
		if (!Double.isFinite(xstp)) xstp=0;
		double t1 = (T-x/m-(s*s)/(2*m*m))*complnormcdf(xstn,0,1);
		double t2 = s*Math.sqrt(T)*normpdf(xstn,0,1)/m;
		// itc20181005: rearranged computation to avoid Inf*0 multiplication
		double cncdfp2 = (complnormcdf(xstp,0,1)/(2*m*m));
		double t3;
		if (Math.abs(cncdfp2) < _POS_ZERO_TOL) {  // rearrange computations
			// compute exp(2mx/s^2)*complnormcdf(xstp) more accurately
			double lnq1 = 2*m*x/(s*s);
			double lnq2 = Math.log(cncdfp2);
			double q = Math.exp(lnq1+lnq2);
			t3 = (s*s)*q/(2*m*m);
		} else {
			t3 = cncdfp2*Math.exp(2*m*x/(s*s))*(s*s);
		}
		return t1 + t2 + t3;
	}
	
	
	/**
	 * corresponds to the function V<sub>1</sub> function in Hadley-Whitin's 
	 * analysis.
	 * @param x double
	 * @param T double
	 * @param m double
	 * @param s double
	 * @return double
	 */
	private static double V1(double x, double T, double m, double s) {
		double xstn = (x-m*T)/(s*Math.sqrt(T));
		if (!Double.isFinite(xstn)) xstn=0;
		double xstp = (x+m*T)/(s*Math.sqrt(T));
		if (!Double.isFinite(xstp)) xstp=0;
		double t1 = complnormcdf(xstn,0,1)*(T*T - (x/m)*(x/m) - 
			                                  2.0*(s*s)*x/(m*m*m) - 
			                                  3.0*(s*s*s*s)/(2*m*m*m*m))/2.0;
		double t2 = normpdf(xstn)*s*Math.sqrt(T)*(m*T + 3.0*s*s/m + x) / (2.0*m*m);
		// itc20181005: rearranged computation to avoid Inf*0 multiplication
		double cncdfp2 = (complnormcdf(xstp,0,1)/(2.0*m*m*m));
		double t3;
		if (Math.abs(cncdfp2) < _POS_ZERO_TOL) {  // rearrange computations
			double lnq1 = 2.0*m*x/(s*s);
			double lnq2 = Math.log(cncdfp2);
			double q = Math.exp(lnq1+lnq2);
			t3 = q*(s*s)*(x - 3.0*(s*s)/(2.0*m));
		} else {
			t3 = cncdfp2*Math.exp(2.0*m*x/(s*s))*(s*s)*(x - 3.0*(s*s)/(2.0*m));
		}
		return t1 + t2 - t3;
	}
	
	
	/**
	 * corresponds to the function W<sub>1</sub> in Hadley-Whitin's analysis.
	 * @param x double
	 * @param T double
	 * @param m double
	 * @param s double
	 * @return double
	 */
	private static double W1(double x, double T, double m, double s) {
		double xstn = (x-m*T)/(s*Math.sqrt(T));
		if (!Double.isFinite(xstn)) xstn=0;
		double xstp = (x+m*T)/(s*Math.sqrt(T));
		if (!Double.isFinite(xstp)) xstp=0;
		double t1 = (s*s)*(1 + m*x/(s*s))*complnormcdf(xstn,0,1)/(m*m*m);
		double t2 = 2.0*s*Math.sqrt(T)*normpdf(xstn)/(m*m);
		// itc20181005: rearranged computation to avoid Inf*0 multiplication
		double cncdfm2 = (complnormcdf(xstp,0,1)/(m*m));
		double t3;
		if (Math.abs(cncdfm2) < _POS_ZERO_TOL) {  // rearrange computations
			double lnq1 = 2.0*m*x/(s*s);
			double lnq2 = Math.log(cncdfm2);
			double q = Math.exp(lnq1+lnq2);
			t3 = (x-(s*s)/m)*q;
		} else {
			t3 = cncdfm2*Math.exp(2.0*m*x/(s*s))*(x-(s*s)/m);
		}
		return t1 - t2 + t3;
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
	double getMiu() {
		return _mi;
	}
	
	
	/**
	 * evaluate the function &phi;(x) ie the pdf of the normal distribution N(0,1)
	 * at x. Uses the <CODE>cern.jet.random.Normal</CODE> class of the COLT 
	 * library for scientific computing.
	 * @param x double
	 * @return double
	 */
	private static double normpdf(double x) {
		return _norm.pdf(x);
	}
	
	
	/**
	 * same as <CODE>normpdf(x)</CODE> but for arbitrary &mu; and &sigma; 
	 * parameters of the distribution.
	 * @param x double
	 * @param m double
	 * @param s double must be positive
	 * @return double
	 */
	private static double normpdf(double x, double m, double s) {
		return _norm.pdf((x-m)/s) / s;
	}
	
	
	/**
	 * evaluate the function &Phi;(x) ie the cdf of the normal distribution N(0,1)
	 * at x. Uses the <CODE>cern.jet.random.Normal</CODE> class of the COLT 
	 * library for scientific computing.
	 * @param x double
	 * @return double
	 */
	private static double normcdf(double x) {
		return _norm.cdf(x);
	}
	
	
	/**
	 * same as <CODE>normcdf</CODE> but for arbitrary &mu; and &sigma; parameters
	 * of the distribution.
	 * @param x double
	 * @param m double
	 * @param s double must be positive
	 * @return double
	 */
	private static double normcdf(double x, double m, double s) {
		return _norm.cdf((x-m)/s);
	}
	
	
	/**
	 * evaluate the function &Phi;&#772;(x,&mu;,&sigma;), ie the complementary
	 * CDF of the normal distribution N(&mu;, &sigma;<sup>2</sup>). Uses the 
	 * <CODE>cern.jet.random.Normal</CODE> class of the COLT library for 
	 * scientific computing.
	 * @param x double
	 * @param mi double
	 * @param sigma double must be &gt;0.
	 * @return double
	 */
	private static double complnormcdf(double x, double mi, double sigma) {
		return 1.0 - normcdf((x-mi)/sigma);
	}
		
	
	/**
	 * invoke as 
	 * <CODE>java -cp &lt;classpath&gt; tests.sic.sST.norm.sSTCnorm 
	 * &lt;s&gt; &lt;S&gt; &lt;T&gt;
	 * &lt;Kr&gt; &lt;Ko&gt; &lt;L&gt; &lt;&mu;&gt; &lt;&sigma;&gt; 
	 * &lt;h&gt; &lt;p&gt; [p2(0)]</CODE>. 
	 * The constraints on the variables and parameters values are as follows:
	 * <ul>
	 * <li>S&ge;s
	 * <li>T&ge;(3.5*&sigma;/&mu;)^2
	 * <li>&sigma;&gt;0
	 * <li>h&gt;0
	 * <li>p&gt;0
	 * <li>p2&ge;0
	 * </ul>
	 * All numbers (except s and S) must be non-negative. 
	 * @param args String[]
	 */
	public static void main(String[] args) {
		//_mger.setDebugLevel(0);
		double s = Double.parseDouble(args[0]);
		System.out.println("s="+s);
		double S = Double.parseDouble(args[1]);
		System.out.println("S="+S);
		double T = Double.parseDouble(args[2]);
		System.out.println("T="+T);
		double Kr = Double.parseDouble(args[3]);
		System.out.println("Kr="+Kr);
		double Ko = Double.parseDouble(args[4]);
		System.out.println("Ko="+Ko);
		double L = Double.parseDouble(args[5]);
		System.out.println("L="+L);
		double miu = Double.parseDouble(args[6]);
		System.out.println("ì="+miu);
		double sigma = Double.parseDouble(args[7]);
		System.out.println("ó="+sigma);
		double h = Double.parseDouble(args[8]);
		System.out.println("h="+h);
		double p = Double.parseDouble(args[9]);
		System.out.println("p="+p);
		double p2 = args.length > 10 ? Double.parseDouble(args[10]) : 0;
		System.out.println("p2="+p2);
		if (S<s || T<Math.pow(3.5*sigma/miu,2.0)) {
			System.err.println("s>S and/or T<(3.5ó/ì)^2");
			System.exit(-1);
		}
		sSTCnorm cc = new sSTCnorm(Kr,Ko,L,miu,sigma,h,p,p2);
		double[] x = new double[]{s,S,T};
		long st = System.currentTimeMillis();
		double val = cc.eval(x, null);
		long dur = System.currentTimeMillis()-st;
		System.out.println("y = "+val+" in "+dur+" msecs.");
	}
	
	
	/**
	 * helper static method to allow the integration of the function f according
	 * to its 1st variable in the interval [a,b]. The function f must accept 
	 * <CODE>VectorIntf</CODE> argument as input, and it is assumed that it is the
	 * first variable in the vector that the integration is about. The requested 
	 * accuracy is <CODE>_INTEGRAL_APPROX_EPS</CODE>. 
	 * @param f FunctionIntf
	 * @param a double the left end of the interval.
	 * @param b double the right end of the interval
	 * @return double the result of applying Simpson rule for integration.
	 */
	public static double integrate(FunctionIntf f, double a, double b, 
		                             VectorIntf x0) { 
		HashMap params = new HashMap();
		params.put("integralapproximator.eps", new Double(_INTEGRAL_APPROX_EPS));
		params.put("integralapproximator.levelmax", 
			         new Integer(_MAX_SIMPSON_REC_LVL));
		IntegralApproximator ia = new IntegralApproximator(f, params);
		params.clear();  // object reuse OK
		params.put("integralapproximator.a", new Double(a));
		params.put("integralapproximator.integrandvarindex", new Integer(0));
		params.put("integralapproximator.num_pieces", 
			         new Integer(_INTEGRATION_NUM_PIECES));
		try {
			x0.setCoord(0, b);
			double res = ia.eval(x0, params);
			return res;
		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("sSTCnorm.integrate("+f+","+a+","+b+","+x0+
				                 "): evaluation failed");
			throw new IllegalStateException("sSTCnorm.integrate(): failed");
		}
	}
	
	
	/**
	 * private auxiliary inner class NOT part of the public API. Represents the 
	 * integrand function of the (improper) integral that is required for the 
	 * computation of the function <CODE>eP()</CODE> that computes the lost-sales
	 * (expected) costs.
	 */
	private static class FH implements FunctionIntf {
		public double eval(Object xarr, HashMap params) {
			VectorIntf xv = (VectorIntf) xarr;
			double x = xv.getCoord(0);
			// rpj(1),T(2),l(3),sigma(4),t(5)
			double rpj = xv.getCoord(1);
			double T = xv.getCoord(2);
			double l = xv.getCoord(3);
			double sigma = xv.getCoord(4);
			double t = xv.getCoord(5);
			return (x-rpj)*(normpdf(x,l*(t+T),sigma*Math.sqrt(t+T)) -
				              normpdf(x,l*T,sigma*Math.sqrt(T)));
		}
		
		
		public String toString() {
			return "FH";
		}
	}
	
	
	/**
	 * private auxiliary inner class NOT part of the public API. Represents the 
	 * integrand function of the integral that forms the terms being added up in
	 * <CODE>sum1()</CODE>.
	 */
	private static class FInner2 implements FunctionIntf {
		public double eval(Object xarr, HashMap params) {
			VectorIntf xv = (VectorIntf) xarr;
			double x = xv.getCoord(0);
			// r(1),R(2),T(3),l(4),sigma(5),t(6),n(7),m(8),IC(9),phat(10),p2(11)
			double r = xv.getCoord(1);
			double R = xv.getCoord(2);
			double T = xv.getCoord(3);
			double l = xv.getCoord(4);
			double sigma = xv.getCoord(5);
			double t = xv.getCoord(6);
			int n = (int) xv.getCoord(7);
			double m = xv.getCoord(8);
			double IC = xv.getCoord(9);
			double phat = xv.getCoord(10);
			double p2 = xv.getCoord(11);
			// y = H(r+x,T,t,l,sigma,m,IC,phat,p2).*
			//     normpdf(R-r-x, n.*l.*T, sigma.*sqrt(n.*T));
			return H(r+x,T,t,l,sigma,m,IC,phat,p2)*
				     normpdf(R-r-x, n*l*T, sigma*Math.sqrt(n*T));
		}
		
		public String toString() {
			return "FInner2";
		}
	}
	
	
	/**
	 * private auxiliary inner class NOT part of the public API. Represents the 
	 * integrand function of the integral that forms the terms being added up in
	 * <CODE>sum2()</CODE>.
	 */
	private static class FInner3 implements FunctionIntf {
		public double eval(Object xarr, HashMap params) {
			VectorIntf xv = (VectorIntf) xarr;
			double x = xv.getCoord(0);
			// r(1),R(2),T(3),l(4),sigma(5),n(6)
			double r = xv.getCoord(1);
			double R = xv.getCoord(2);
			double T = xv.getCoord(3);
			double l = xv.getCoord(4);
			double sigma = xv.getCoord(5);
			int n = (int) xv.getCoord(6);
			return complnormcdf(x, l*T, 
				                  sigma*Math.sqrt(T))*
				                    normpdf(R-r-x,(n-1)*l*T,sigma*Math.sqrt((n-1)*T))*n;
		}
		
		public String toString() {
			return "FInner3";
		}
	}
	
}

