package tests.sic.sST.nbin;

import popt4jlib.FunctionIntf;
import popt4jlib.DblArray1Vector;
import utils.Messenger;
import tests.sic.rnqt.nbin.RnQTCnbin;


/**
 * function implements the continuous-time long-run expected cost of a periodic
 * review, single echelon inventory control system facing exogenous demands 
 * generated in a period of length T, from a Negative Binomial process with 
 * parameters &lambda; and p<sub>l</sub> (both positive). The system faces 
 * linear holding and backorder costs with cost rates h&gt;0 and p&gt;0. 
 * It also faces fixed costs: 
 * fixed review cost per period Kr&ge;0, and fixed order cost Ko&ge;0. Finally, 
 * there is a constant lead-time for each order being placed equal to L&ge;0.
 * The control parameters then, are s (the reorder point), S (the order-up-to
 * point), and T (the review interval). When fully optimized in all its control
 * parameters, this is the globally optimal control policy and the resulting
 * cost is the best possible cost of such an echelon (result known since the 
 * 50's by Arrow et al.)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class sSTCnbin implements FunctionIntf {
	final double _Kr;
	double _Ko;
	double _L;
	double _lambda;
	double _p_l;
	double _h;
	double _p;
	
	private static volatile int _maxAddedTermsInSum12 = 0;
	
	private final static double _eps = 2.e-8;  // itc-20210512: used to be 1.e-9
	private final static int _numTerms = 5;
	private final static int _maxAllowedSumTerms = 10000000;

	private final static Messenger _mger = Messenger.getInstance();
	private final static int _DISP_MOD_NUM = 5000;
	private final static int _START_DISP_MOD_NUM = 10000;
	
	/**
	 * Function sole public constructor.
	 * @param Kr double
	 * @param Ko double
	 * @param L double
	 * @param lambda double
	 * @param p_l double
	 * @param h double
	 * @param p double 
	 */
	public sSTCnbin(double Kr, double Ko, double L, 
		                 double lambda, double p_l, 
									   double h, double p) {
		_Kr=Kr;
		_Ko=Ko;
		_L=L;
		_lambda=lambda;
		_p_l = p_l;
		_h=h;
		_p=p;
	}

	
	/**
	 * evaluate the long-run expected cost of a single echelon inventory control
	 * system facing Negative Binomial demands with linear holding and backorder 
	 * costs and fixed review and order costs controlled by (s,S,T) periodic 
	 * review policy.
	 * @param x double[] representing s, S and T or <CODE>DblArray1Vector</CODE>
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
	 * system facing normal demands with linear holding and backorder costs and 
	 * fixed review and order costs controlled by (s,S,T) periodic review policy.
	 * It returns in a pair of doubles, both the actual value as well as the value
	 * when the order cost Ko is zero, forming a lower bound on the cost function.
	 * @param x double[] or popt4jlib.DblArray1Vector representing s, S and T
	 * @param param HashMap  // may contain a &lt;"Kr",$val&gt; pair
	 * @return utils.Pair  // Pair&lt;Double result, Double lowerbound&gt;
	 * @throws IllegalStateException unchecked if Po is computed outside [0,1] or
	 * if any number computed turns out to be <CODE>Double.NaN</CODE>.
	 */
	utils.Pair evalBoth(Object x, java.util.HashMap param) {
		double[] xp;
		if (x instanceof double[]) xp = (double[])x;
		else xp = ((DblArray1Vector) x).getDblArray1();
		_mger.msg("sSTCnbin.evalBoth(s="+xp[0]+",S="+xp[1]+",T="+xp[2]+"): start",
			        3);
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
		double l = _lambda;
		double m = _lambda*_L;
		double t = _L;
		double phat = _p;
		double IC = _h;
		
		double K1 = J/T;
		double nom = sum1((int)r,(int)R,T,t,l,m,IC,phat,_eps);
		double denom = sum2((int)r,(int)R,T,l,_eps);
		
		double y = K1 + (A+nom)/(T*denom);
		double lb = K1 + nom/(T*denom);
		_mger.msg("sSTCnbin.evalBoth(s="+xp[0]+",S="+xp[1]+",T="+xp[2]+"): end",
			        3);
		return new utils.Pair(new Double(y), new Double(lb));
	}
	
	
	private double sum1(int r, int R, double T, double t, 
		                  double l, double m, 
											double IC, double phat, double eps) {
		double y = 0;
		int n = 0;
		double last = 0;
		int count = 0;
		while (n < _maxAllowedSumTerms) {
			double sum = 0;
			int Rmr = R-r;
			for (int j=1; j<=Rmr; j++) {
				double term = RnQTCnbin.nbinnfoldconv(Rmr-j, _lambda*T, _p_l, n);
				if (Double.isNaN(term)) {
					String exc = "for mean="+(l*T)+" arg="+(Rmr-j)+
						           " nbin. "+n+"-fold conv is NaN?";
					throw new IllegalStateException(exc);
				}
				double term2 = H(r+j,T,t,l,_p_l,m,IC,phat);
				if (Double.isNaN(term2)) {
					String exc = "H(...) becomes NaN";
					throw new IllegalStateException(exc);
				}
				term *= term2;
				sum += term;
			}
			if (Double.isNaN(sum)) {
				String exc = "for n="+n+" sum is now NaN";
				throw new IllegalStateException(exc);
			}
			y += sum;
			last += sum;
			++n;
			if (++count==_numTerms) {
				double ratio = Math.abs(last/y);
				if (n>_START_DISP_MOD_NUM && n%_DISP_MOD_NUM == 0) {  // debug
					_mger.msg("sSTCnbin.sum1(r="+r+",R="+R+",T="+T+"...,eps="+eps+
						        "): n="+n+" cur_val="+y+" ratio="+ratio, 3);
				}
				if (ratio < eps) break;
				else {
					//System.err.println("sum1: n="+n+" last="+last+" y="+y+" ratio="+ratio);
					count = 0;
					last = 0;
				}
			}
		}
		if (n==_maxAllowedSumTerms) {
			String exc = "sSTCnbin.sum1(r="+r+",R="+R+",T="+T+",rest_args): "+ 
				           "evaluation exceeded "+_maxAllowedSumTerms+" limit";
			throw new IllegalStateException(exc);
		}
		updateMaxAddedTermsInSum12(n);
		return y;
	}
	
	
	private double sum2(int r, int R, double T, double l, double eps) {
		double y = 0;
		int n = 1;
		double last = 0;
		int count = 0;
		while (n<_maxAllowedSumTerms) {
			double sum = 0;
			double prev_aux = Double.NaN;
			double prev_aux2 = Double.NaN;
			for (int j=1; j<=R-r; j++) {
				double aux = RnQTCnbin.nbinnfoldconv(R-r-j, l*T, _p_l, n-1);
				double aux2 = 0.0;
				if (Double.isNaN(aux)) {
					String exc = (n-1)+"-nbin-conv("+(R-r-j)+";"+l*T+","+_p_l+") is NaN";
					exc += " (j="+j+" prev_aux="+prev_aux+")";
					_mger.msg(exc, 0);
					//throw new IllegalStateException(exc);
				}
				else aux2 = n*RnQTCnbin.nbincdfcompl(j, l*T, _p_l);
				if (Double.isNaN(aux2)) {
					String exc = "for n="+n+" j="+j+" r="+r+" R="+R+" aux2 is NaN";
					_mger.msg(exc, 0);
					//throw new IllegalStateException(exc);					
				}
				double all = aux*aux2;
				if (Double.isNaN(all)) {
					// try one last time the log-exp trick
					all = n_nbinnfoldconv_nbincdfcompl(R-r-j, l*T, _p_l, n, j);
					if (!Double.isFinite(all)) {
						String exc = "for n="+n+" j="+j+" r="+r+" R="+R+
							           " aux="+aux+"*aux2="+aux2+
								         " is NaN; all="+all;
						exc += "prev_aux="+prev_aux+" prev_aux2="+prev_aux2;
						// can't help but throw here
						throw new IllegalStateException(exc);					
					}
				}
				prev_aux = aux;
				prev_aux2 = aux2;				
				sum += all;
				if (Double.isNaN(sum)) {
					String exc = "for n="+n+" sum is NaN";
					throw new IllegalStateException(exc);					
				}
			}
			y += sum;
			last += sum;
			++n;
			if (++count==_numTerms) {
				double ratio = Math.abs(last/y);
				if (n>_START_DISP_MOD_NUM && n%_DISP_MOD_NUM == 0) {  // debug
					_mger.msg("sSTCnbin.sum2(r="+r+",R="+R+",T="+T+",l="+l+",eps="+eps+
						        "): n="+n+" cur_val="+y+" ratio="+ratio, 3);
				}
				if (ratio < eps) break;
				else {
					//System.err.println("sum2: n="+n+" last="+last+" y="+y+" ratio="+ratio);
					count = 0;
					last = 0;
				}
			}
		}
		if (n==_maxAllowedSumTerms) {
			String exc = "sSTCnbin.sum2(r="+r+",R="+R+",T="+T+",rest_args): "+ 
				           "evaluation exceeded "+_maxAllowedSumTerms+" limit";
			throw new IllegalStateException(exc);
		}
		updateMaxAddedTermsInSum12(n);
		return y;
	}

	
	/**
	 * update the maximum number of terms needed to add up in order to compute 
	 * either of the functions <CODE>sum1()</CODE> or <CODE>sum2()</CODE>.
	 * @param n int
	 */
	private synchronized static void updateMaxAddedTermsInSum12(int n) {
		if (n>_maxAddedTermsInSum12) 
			_maxAddedTermsInSum12 = n;
	}
	
	
	/**
	 * used in diagnostics, to get the maximum number of terms added in order to 
	 * compute the value of <CODE>sum1()</CODE> or <CODE>sum2()</CODE> sums.
	 * @return int
	 */
	public synchronized static int getMaxAddedTermsInSum12() {
		return _maxAddedTermsInSum12;
	}
	
	
	/**
	 * invoke as 
	 * <CODE>java -cp &lt;classpath&gt; tests.sic.sST.poisson.sSTCnbin 
	 * &lt;s&gt; &lt;S&gt; &lt;T&gt;
	 * &lt;Kr&gt; &lt;Ko&gt; &lt;L&gt; &lt;&lambda;&gt; &lt;p<sub>l</sub>&gt; 
	 * &lt;h&gt; &lt;p&gt;</CODE>. 
	 * The constraints on the variables and parameters values are as follows:
	 * <ul>
	 * <li>s,S integer, S&ge;s
	 * <li>T&gt;0
	 * <li>&lambda;,p<sub>l</sub>&gt;0
	 * <li>h&gt;0
	 * <li>p&gt;0
	 * </ul>
	 * All other parameters must be non-negative.
	 * @param args String[]
	 */
	public static void main(String[] args) {
		int s = Integer.parseInt(args[0]);
		int S = Integer.parseInt(args[1]);
		double T = Double.parseDouble(args[2]);
		double Kr = Double.parseDouble(args[3]);
		double Ko = Double.parseDouble(args[4]);
		double L = Double.parseDouble(args[5]);
		double m = Double.parseDouble(args[6]);
		double p_l = Double.parseDouble(args[7]);
		double h = Double.parseDouble(args[8]);
		double p = Double.parseDouble(args[9]);
		sSTCnbin cc = new sSTCnbin(Kr,Ko,L,m,p_l,h,p);
		double[] x = new double[]{s,S,T};
		double val = cc.eval(x, null);
		System.out.println("y = "+val);
	}
	
	
	private static double H(int rpj, double T, double t, double l, double pl,
		                      double m, 
		                      double IC, double phat) {
		return IC*T*(rpj-m-l*T/2.0) + (IC+phat)*bP(rpj,T,l,pl,t);
	}
	
	
	/**
	 * computes the average long-run expected unit years of shortage incurred from
	 * t+L to t+L+T of a single-echelon system facing demands that follow the 
	 * Negative Binomial (ie compound Poisson) distribution with arrival rate 
	 * lambda and process parameter of the Logarithmic distribution p, with an 
	 * (s,S,T) periodic-review policy, taking into account fixed costs (review and 
	 * ordering costs).
	 * @param rpj int the IP immediately after a review
	 * @param T double review period length
	 * @param lambda double arrival rate
	 * @param pl double second parameter of Nbin distribution
	 * @param L double lead-time
	 * @return double back-orders part of the total cost
	 */
	public static double bP(int rpj, double T, 
		                      double lambda, double pl, 
		                      double L) {
		return RnQTCnbin.b(rpj, T, lambda, pl, L);
	}
	
	
	/**
	 * last-attempt log-exp trick to compute the terms in the sum1 method.
	 * @param Rrj int the value R-r-j
	 * @param lT double the value l*T
	 * @param pl double the value p<sub>l</sub>
	 * @param n int the number of folds 
	 * @param j int the index
	 * @return double
	 */
	private static double n_nbinnfoldconv_nbincdfcompl(int Rrj, double lT, 
		                                                 double pl, int n, int j) {
		double logaux = RnQTCnbin.nbinnfoldconvlog(Rrj, lT, pl, n-1);
		double logaux2 = Math.log(n) + RnQTCnbin.nbincdfcompllog(j, lT, pl);
		double res = Math.exp(logaux + logaux2);
		if (!Double.isFinite(res)) {
			Messenger.getInstance().msg("sSTCnbin.n_nbinnfoldconv_nbincdfcompl(): "+
				                          "logaux = "+logaux+" logaux2="+logaux2, 0);
		}
		return res;
	}
	
}

