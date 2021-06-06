package tests.sic.sST.poisson;

import popt4jlib.FunctionIntf;
import popt4jlib.DblArray1Vector;
import utils.Messenger;
import cern.jet.random.Poisson;


/**
 * function implements the continuous-time long-run expected cost of a periodic
 * review, single echelon inventory control system facing exogenous demands 
 * generated in a period of length T, from a Poisson process with parameter 
 * &lambda;. The system faces linear holding and 
 * backorder costs with cost rates h&gt;0 and p&gt;0 (and also possibly a 2nd
 * one-shot penalty p2&ge;0 that applies as a fixed cost if a shortage occurs 
 * when placing an order and is otherwise independent of the length of delay in 
 * delivering the order). It also faces fixed costs: 
 * fixed review cost per period Kr&ge;0, and fixed order cost Ko&ge;0. Finally, 
 * there is a constant lead-time for each order being placed equal to L&ge;0.
 * The control parameters then, are s (the reorder point), S (the order-up-to
 * point), and T (the review interval). When fully optimized in all its control
 * parameters, this is the globally optimal control policy and the resulting
 * cost is the best possible cost of such an echelon (result known since the 
 * 50's by Arrow et al.)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class sSTCpoisson implements FunctionIntf {
	final double _Kr;
	double _Ko;
	double _L;
	double _lambda;
	double _h;
	double _p;
	double _p2;
	
	private final static double _eps = 1.e-9;
	private final static int _numTerms = 5;

	
	/**
	 * Function sole public constructor.
	 * @param Kr double
	 * @param Ko double
	 * @param L double
	 * @param lambda double
	 * @param h double
	 * @param p double 
	 * @param p2 double
	 */
	public sSTCpoisson(double Kr, double Ko, double L, 
		                 double lambda, 
									   double h, double p, double p2) {
		_Kr=Kr;
		_Ko=Ko;
		_L=L;
		_lambda=lambda;
		_h=h;
		_p=p;
		_p2=p2;
	}

	
	/**
	 * evaluate the long-run expected cost of a single echelon inventory control
	 * system facing Poisson demands with linear holding and backorder costs and 
	 * fixed review and order costs controlled by (s,S,T) periodic review policy.
	 * @param x double[] representing s, S and T or <CODE>DblArray1Vector</CODE>
	 * @param param HashMap if not null, it may contain value for the Kr parameter
	 * @return double
	 * @throws IllegalStateException unchecked if the computations go awry, see
	 * method <CODE>evalBoth(x)</CODE>.
	 */
	public double eval(Object x, java.util.HashMap param) {
		final Messenger mger = Messenger.getInstance();
		utils.Pair p = evalBoth(x, param);
		final double res = ((Double) p.getFirst()).doubleValue();
		if (mger.getDebugLvl()>=2) {
			String str = "sSTCpoisson.eval(";
			final double[] xarr = (double[])x;
			final double Kr = (param!=null && param.containsKey("Kr")) ?
				                  ((Double)param.get("Kr")).doubleValue() :
				                  _Kr;
			str += "s="+xarr[0]+", S="+xarr[1]+", T="+xarr[2];
			str += " | Kr="+Kr+",Ko="+_Ko+",L="+_L+",ë="+_lambda;
			str += ",h="+_h+",_p="+_p+",_p2="+_p2;
			str += ")="+res;
			mger.msg(str, 2);
		}
		return res;
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
		double p2 = _p2;
		double IC = _h;
		
		double K1 = J/T;
		Poisson pois = new Poisson2(1);
		double nom = sum1((int)r,(int)R,T,t,l,m,IC,phat,p2,_eps,pois);
		double denom = sum2((int)r,(int)R,T,l,_eps,pois);
		
		double y = K1 + (A+nom)/(T*denom);
		double lb = K1 + nom/(T*denom);
		return new utils.Pair(new Double(y), new Double(lb));
	}
	
	
	private double sum1(int r, int R, double T, double t, 
		                  double l, double m, 
											double IC, double phat, double p2, double eps, 
											Poisson pois) {
		double y = 0;
		int n = 0;
		double last = 0;
		int count = 0;
		while (true) {
			double sum = 0;
			int Rmr = R-r;
			for (int j=1; j<=Rmr; j++) {
				pois.setMean(n*l*T);
				double term = pois.pdf(Rmr-j);
				if (Double.isNaN(term)) {
					String exc = "for mean="+(n*l*T)+" arg="+(Rmr-j)+" pois.pdf is NaN?";
					throw new IllegalStateException(exc);
				}
				double term2 = H(r+j,T,t,l,m,IC,phat,p2,pois);
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
				if (ratio < eps) {
					System.err.println("sum1: n="+n+" last="+last+" y="+y+" ratio="+ratio);
					break;
				}
				else {
					System.err.println("sum1: n="+n+" last="+last+" y="+y+" ratio="+ratio);
					count = 0;
					last = 0;
				}
			}
		}
		return y;
	}
	
	
	private double sum2(int r, int R, double T, double l, double eps, 
		                  Poisson pois) {
		double y = 0;
		int n = 1;
		double last = 0;
		int count = 0;
		while (true) {
			double sum = 0;
			for (int j=1; j<=R-r; j++) {
				pois.setMean((n-1)*l*T);
				double aux = n*pois.pdf(R-r-j);
				if (Double.isNaN(aux)) {
					String exc = "n="+n+" * pdf("+(R-r-j)+","+(n-1)*l*T+") is NaN";
					throw new IllegalStateException(exc);
				}
				double aux2 = complpoisscdf(pois,j,l*T);
				if (Double.isNaN(aux2)) {
					String exc = "for n="+n+" j="+j+" r="+r+" R="+R+" aux2 is NaN";
					throw new IllegalStateException(exc);					
				}
				double all = aux*aux2;
				if (Double.isNaN(all)) {
					String exc = "for n="+n+" j="+j+" r="+r+" R="+R+
						           " aux="+aux+"*aux2="+aux2+
						           " is NaN";
					throw new IllegalStateException(exc);					
				}
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
				if (ratio < eps) {
					System.err.println("sum2: n="+n+" last="+last+" y="+y+" ratio="+ratio);
					break;
				}
				else {
					System.err.println("sum2: n="+n+" last="+last+" y="+y+" ratio="+ratio);
					count = 0;
					last = 0;
				}
			}
		}
		return y;
	}
	
	
	/**
	 * invoke as 
	 * <CODE>java -cp &lt;classpath&gt; tests.sic.sST.poisson.sSTCpoisson 
	 * &lt;s&gt; &lt;S&gt; &lt;T&gt;
	 * &lt;Kr&gt; &lt;Ko&gt; &lt;L&gt; &lt;&mu;&gt; 
	 * &lt;h&gt; &lt;p&gt; [p2(0)]</CODE>. 
	 * The constraints on the variables and parameters values are as follows:
	 * <ul>
	 * <li>s,S integer, S&ge;s
	 * <li>T&gt;0
	 * <li>&mu;&ge;0
	 * <li>h&gt;0
	 * <li>p&gt;0
	 * <li>p2&ge;0
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
		double h = Double.parseDouble(args[7]);
		double p = Double.parseDouble(args[8]);
		double p2 = 0;
		if (args.length>9) p2 = Double.parseDouble(args[9]);
		sSTCpoisson cc = new sSTCpoisson(Kr,Ko,L,m,h,p,p2);
		double[] x = new double[]{s,S,T};
		double val = cc.eval(x, null);
		System.out.println("y = "+val);
	}
	
	
	private static double H(int rpj, double T, double t, double l, double m, 
		                      double IC, double phat, double p2,
													Poisson pois) {
		double z = 0.0;
		if (Double.compare(p2, 0.0)>0) {
			z = p2*eP(rpj,T,l,t,pois);
		}
		return IC*T*(rpj-m-l*T/2.0) + (IC+phat)*bP(rpj,T,l,t,pois) + z;
	}
	
	
	static double bP(int rpj, double T, double l, double t, 
		                       Poisson pois) {
		double tpT = t+T;
		double tpT2 = tpT*tpT;
		double t1 = l*(tpT2*complpoisscdf(pois,rpj-1,l*tpT) - 
			             t*t*complpoisscdf(pois,rpj-1,l*t))/2.0;
		double t2 = (complpoisscdf(pois,rpj+1,l*tpT) - 
			           complpoisscdf(pois,rpj+1,l*t))*rpj*(rpj+1)/(2.0*l);
		double t3 = rpj*(tpT*complpoisscdf(pois,rpj,l*tpT) - 
			               t*complpoisscdf(pois,rpj,l*t));
		return t1 + t2 - t3;
	}
	
	
	static double eP(int rpj, double T, double l, double t, 
		                       Poisson pois) {
		double tpT = t+T;
		double t1 = l*tpT*complpoisscdf(pois,rpj-1,l*tpT) - 
			          rpj*complpoisscdf(pois,rpj,l*tpT);
		double t2 = l*t*complpoisscdf(pois,rpj-1,l*t) - 
			          rpj*complpoisscdf(pois,rpj,l*t);
		return t1 - t2;
	}

	
	static double complpoisscdf(Poisson pois, int x, double lm) {
		if (Double.compare(lm, 0.0)<=0) {  // corner case
			if (x<0) {
				return 1.0;  // MATLAB poisscdf returns 0
			}
			else return 0.0;  // MATLAB poisscdf returns 1 when lm=0, x>=0
		}
		pois.setMean(lm);
		return 1.0 - pois.cdf(x-1);
	} 
}

