package tests.sic.rnqt.norm;

import popt4jlib.FunctionIntf;
import popt4jlib.DblArray1Vector;
import cern.jet.random.Normal;

/**
 * function implements the continuous-time long-run expected cost of a periodic
 * review, single echelon inventory control system facing exogenous demands 
 * generated in a period of length T, from a normal process with parameters 
 * N(&mu; T, &sigma;^2 T) with &sigma;&gt;0. The system faces linear holding and 
 * backorder costs with cost rates h&gt;0 and p&gt;0. It also faces fixed costs: 
 * fixed review cost per period Kr&ge;0, and fixed order cost Ko&ge;0. Finally, 
 * there is a constant lead-time for each order being placed equal to L&ge;0.
 * The control parameters then, are r (the reorder point), Q (the batch size),
 * and T (the review interval).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RnQTCnorm implements FunctionIntf {
	private double _Kr;
	private double _Ko;
	double _L;
	double _mi;
	double _sigma;
	private double _h;
	private double _p;
	private final static Normal _norm = new Normal(0,1,null);  // _norm always 
	                                                           // available, even
	                                                           // after being
	                                                           // transmitted over
	                                                           // wire (sockets).
	
	private final static double _Qtol = 1.e-7;
	
	/**
	 * compile-time constants used in determining whether the computations for
	 * Po show some serious error in the formulae of Hadley-Whitin (1963).
	 */
	private final static double _P0tol = 1.e-8;
	private final static double _ONE_TOL = 1.0 + _P0tol;
	private final static double _ZERO_TOL = -_P0tol;
	
	
	/**
	 * Function sole public constructor.
	 * @param Kr double
	 * @param Ko double
	 * @param L double
	 * @param mi double
	 * @param sigma double
	 * @param h double
	 * @param p double 
	 */
	public RnQTCnorm(double Kr, double Ko, double L, 
		               double mi, double sigma, 
									 double h, double p) {
		_Kr=Kr;
		_Ko=Ko;
		_L=L;
		_mi=mi;
		_sigma=sigma;
		_h=h;
		_p=p;
	}
	
	
	/**
	 * evaluate the long-run expected cost of a single echelon inventory control
	 * system facing normal demands with linear holding and backorder costs and 
	 * fixed review and order costs controlled by (R,nQ,T) periodic review policy.
	 * @param x double[] representing R, Q and T
	 * @param param HashMap unused
	 * @return double
	 * @throws IllegalStateException unchecked if the computations go awry, see
	 * method <CODE>evalBoth(x)</CODE>.
	 */
	public double eval(Object x, java.util.HashMap param) {
		utils.Pair p = evalBoth(x);
		return ((Double) p.getFirst()).doubleValue();
	} 

	
	/**
	 * evaluate the long-run expected cost of a single echelon inventory control
	 * system facing normal demands with linear holding and backorder costs and 
	 * fixed review and order costs controlled by (R,nQ,T) periodic review policy.
	 * It returns in a pair of doubles, both the actual value as well as the value
	 * when the order cost Ko is zero, forming a lower bound on the cost function.
	 * @param x double[] or popt4jlib.DblArray1Vector representing R, Q and T
	 * @return utils.Pair  // Pair&lt;Double result, Double lowerbound&gt;
	 * @throws IllegalStateException unchecked if Po is computed outside [0,1] or
	 * if any number computed turns out to be <CODE>Double.NaN</CODE>.
	 */
	utils.Pair evalBoth(Object x) {
		double[] xp;
		if (x instanceof double[]) xp = (double[])x;
		else xp = ((DblArray1Vector) x).getDblArray1();
		double s = xp[0];
		double Q = xp[1];
		double T = xp[2];
		double Po;
		if (Double.compare(Q, _Qtol)<=0) 
			Po=1;  // Q close enough to zero, assume (R,T) policy
		else {
			double RT = Q/(_sigma*Math.sqrt(T));
			double MT = _mi*Math.sqrt(T)/_sigma;
			Po = 1.0 - (1.0/RT)*(normpdf(RT-MT) + (RT-MT)*normcdf(RT-MT) - 
				                   (normpdf(-MT) - MT*normcdf(-MT)));
			if (Double.compare(Po,_ONE_TOL)>0 || Double.compare(Po,_ZERO_TOL)<0 || 
				  Double.isNaN(Po)) {
				String exc ="<R="+s+",Q="+Q+",T="+T+">: "+
					          "Kr="+_Kr+" Ko="+_Ko+" L="+_L+
					          " mi="+_mi+" sigma="+_sigma+" h="+_h+" p="+_p;
				throw new IllegalStateException(exc+"--> Po="+Po);
			}
		}
		double H1 = _h*(Q/2.0 + s - _mi*_L - _mi*T/2.0);  // holding costs
		double D = _sigma*_sigma;
		if (Double.compare(Q,_Qtol)<=0) {  // compute (R,T) policy cost
			double UrLpT = U(s,_L+T,_mi,D);
			if (Double.isNaN(UrLpT)) {
				String exc ="<R="+s+",Q="+Q+",T="+T+">: "+
					          "Kr="+_Kr+" Ko="+_Ko+" L="+_L+
					          " mi="+_mi+" sigma="+_sigma+" h="+_h+" p="+_p;
				throw new IllegalStateException(exc+" --> UrLpT is "+UrLpT);
			}
			double UrT = U(s,_L,_mi, D);
			if (Double.isNaN(UrT)) {
				String exc ="<R="+s+",Q="+Q+",T="+T+">: "+
					          "Kr="+_Kr+" Ko="+_Ko+" L="+_L+
					          " mi="+_mi+" sigma="+_sigma+" h="+_h+" p="+_p;
				throw new IllegalStateException(exc+" --> UrT is "+UrT);				
			}
			double B = (UrLpT-UrT)/T;
			if (Double.isNaN(B)) {
				String exc ="<R="+s+",Q="+Q+",T="+T+">: "+
					          "Kr="+_Kr+" Ko="+_Ko+" L="+_L+
					          " mi="+_mi+" sigma="+_sigma+" h="+_h+" p="+_p;
				throw new IllegalStateException(exc+" --> B is "+B);				
			}
			double y = (_Kr+_Ko)/T + H1 + (_h+_p)*B;
			if (Double.isNaN(y) || Double.isInfinite(y)) {
				String exc ="<R="+s+",Q="+Q+",T="+T+">: "+
					          "Kr="+_Kr+" Ko="+_Ko+" L="+_L+
					          " mi="+_mi+" sigma="+_sigma+" h="+_h+" p="+_p;
				throw new IllegalStateException(exc+" --> y is "+y);
			}
			return new utils.Pair(new Double(y), new Double(y-_Ko/T));
		}
		// compute full (R,nQ,T) policy cost
		double K1 = Ksi(s,_L+T,_mi,D);
		if (Double.isNaN(K1)) {
				String exc ="<R="+s+",Q="+Q+",T="+T+">: "+
					          "Kr="+_Kr+" Ko="+_Ko+" L="+_L+
					          " mi="+_mi+" sigma="+_sigma+" h="+_h+" p="+_p;
				throw new IllegalStateException(exc+" --> K1 is "+K1);
		}
		double K2 = Ksi(s,_L,_mi,D);
		if (Double.isNaN(K2)) {
				String exc ="<R="+s+",Q="+Q+",T="+T+">: "+
					          "Kr="+_Kr+" Ko="+_Ko+" L="+_L+
					          " mi="+_mi+" sigma="+_sigma+" h="+_h+" p="+_p;
				throw new IllegalStateException(exc+" --> K2 is "+K2);
		}
		double K3 = Ksi(s+Q,_L+T,_mi,D);
		if (Double.isNaN(K3)) {
				String exc ="<R="+s+",Q="+Q+",T="+T+">: "+
					          "Kr="+_Kr+" Ko="+_Ko+" L="+_L+
					          " mi="+_mi+" sigma="+_sigma+" h="+_h+" p="+_p;
				throw new IllegalStateException(exc+" --> K3 is "+K3);
		}		
		double K4 = Ksi(s+Q,_L,_mi,D);
		if (Double.isNaN(K4)) {
				String exc ="<R="+s+",Q="+Q+",T="+T+">: "+
					          "Kr="+_Kr+" Ko="+_Ko+" L="+_L+
					          " mi="+_mi+" sigma="+_sigma+" h="+_h+" p="+_p;
				throw new IllegalStateException(exc+" --> K4 is "+K4);
		}
		double Yr = (K1-K2)/T;
		double YrQ = (K3-K4)/T;
		double B = (Yr-YrQ)/Q;		
		if (Double.isNaN(B)) {
				String exc ="<R="+s+",Q="+Q+",T="+T+">: "+
					          "Kr="+_Kr+" Ko="+_Ko+" L="+_L+
					          " mi="+_mi+" sigma="+_sigma+" h="+_h+" p="+_p;
				throw new IllegalStateException(exc+" --> B is "+B);
		}
		double lb = _Kr/T + H1 + (_h+_p)*B;
		double y = lb + _Ko*Po/T;  // (_Kr + _Ko*Po)/T + H1 + (_h+_p)*B;
		if (Double.isNaN(y) || Double.isInfinite(y))
			throw new IllegalStateException("y is Nan or Infinite...");
		return new utils.Pair(new Double(y), new Double(lb));
	} 

	
	/**
	 * evaluate the function &phi;(x) ie the pdf of the normal distribution N(0,1)
	 * at x. Uses the <CODE>cern.jet.random.Normal</CODE> class of the COLT 
	 * library for scientific computing.
	 * @param x double
	 * @return double
	 */
	private double normpdf(double x) {
		return _norm.pdf(x);
	}
	
	
	/**
	 * evaluate the function &Phi;(x) ie the cdf of the normal distribution N(0,1)
	 * at x. Uses the <CODE>cern.jet.random.Normal</CODE> class of the COLT 
	 * library for scientific computing.
	 * @param x double
	 * @return double
	 */
	private double normcdf(double x) {
		return _norm.cdf(x);
	}
	
	
	/**
	 * implements the function U(u,tau | l,D) defined in formula (5-60) in page
	 * 259 of G Hadley and TM Whitin: Analysis of Inventory Systems, 
	 * Prentice-Hall, 1963.
	 * @param u double
	 * @param tau double
	 * @param l double mean demand in one period 
	 * @param D double demand variance in one period
	 * @return double
	 */
	private double U(double u, double tau, double l, double D) {
		/*
			ultsrL = (u-l.*tau)./sqrt(D*tau);
			t1 = (1-normcdf(ultsrL)).*((D^2+2*l^4*tau^2)/(4*l^3) + (D-2*l^2*tau)*u/(2*l^2) + u^2/(2*l));
			t2 = 0.5*(sqrt(D)*tau^(1.5) - D^(1.5)*sqrt(tau)/l^2 - sqrt(D*tau)*u/l)*normpdf(ultsrL);
			t3 = D^2*exp(2*l*u/D)*(1-normcdf((u+l*tau)/sqrt(D*tau)))/(4*l^3);
			y = t1 + t2 - t3;
		*/
		double ultsrL = (u-l*tau)/Math.sqrt(D*tau);
		double t1 = (1-normcdf(ultsrL))*((D*D+2*Math.pow(l, 4)*tau*tau) / 
			          (4*Math.pow(l, 3)) + 
			          (D-2*l*l*tau)*u/(2*l*l) + u*u/(2*l));
		if (Double.isNaN(t1)) {
			String exc = "u="+u+", tau="+tau+",l="+l+",D="+D;
			throw new IllegalStateException(exc+" t1 in U is "+t1);
		}
		double t2 = 0.5*(Math.sqrt(D)*Math.pow(tau,1.5) - 
			          Math.pow(D,1.5)*Math.sqrt(tau)/(l*l) - 
			          Math.sqrt(D*tau)*u/l)*normpdf(ultsrL);
		if (Double.isNaN(t2)) {
			String exc = "u="+u+", tau="+tau+",l="+l+",D="+D;
			throw new IllegalStateException(exc+" t2 in U is "+t2);
		}
		double t3 = D*D*Math.exp(2*l*u/D)*(1-normcdf((u+l*tau) / 
			          Math.sqrt(D*tau)))/(4*l*l*l);
		if (Double.isNaN(t3)) {
			// t3 for large (u+l*tau)/sqrt(D*tau) can be problematic, use log/exp
			double logt3 = 2*Math.log(D) + 2*l*u/D;
			logt3 += Math.log(1-normcdf((u+l*tau)/Math.sqrt(D*tau)));
			logt3 -= Math.log(4*l*l*l);
			t3 = Math.exp(logt3);
			if (Double.isNaN(t3)) {
				String exc = "u="+u+", tau="+tau+",l="+l+",D="+D;
				throw new IllegalStateException(exc+" t3 in U is "+t3+" logt3="+logt3);
			}
		}
		return t1+t2-t3;
	}
	
	
	/**
	 * implements the function &Xi;(r,tau|l,D) defined in formula (5-61) in page
	 * 259 of G Hadley and TM Whitin: Analysis of Inventory Systems, 
	 * Prentice-Hall, 1963.
	 * @param r double
	 * @param tau double
	 * @param l double mean demand in one period
	 * @param D double demand variance in one period
	 * @return double
	 */
	private double Ksi(double r, double tau, double l, double D) {
		/*
			z1 = (1-normcdf((r-l*tau)/sqrt(D*tau)))*(l^2*tau^3/6 - D^2*r/(4*l^3) - l*tau^2*r/2 - D*r^2/(4*l^2) + D*tau^2/4 + tau*r^2/2 - r^3/(6*l) - D^3/(8*l^4));
			z2 = normpdf((r-l*tau)/sqrt(D*tau))*(l*sqrt(D)*(sqrt(tau))^5/6 - sqrt(D)*(sqrt(tau))^3*r/3 + sqrt(D)*sqrt(tau)*r^2/(6*l) + (sqrt(D))^3*(sqrt(tau))^3/(12*l) + (sqrt(D))^3*sqrt(tau)*r/(4*l^2) + (sqrt(D))^5*sqrt(tau)/(4*l^3));
			z3 = (1-normcdf((r+l*tau)/sqrt(D*tau)))*(D^3*exp(2*l*r/D)/(8*l^4));
			z = z1+z2+z3;		
		*/
		double z1 = (1-normcdf((r-l*tau)/Math.sqrt(D*tau)))*
			          (l*l*tau*tau*tau/6 - D*D*r/(4*l*l*l) - l*tau*tau*r/2 - 
			           D*r*r/(4*l*l) + D*tau*tau/4 + tau*r*r/2 - r*r*r/(6*l) - 
			           D*D*D/(8*l*l*l*l));
		double sqrtt3 = Math.pow(tau,1.5);
		double sqrtD3 = Math.pow(D, 1.5);
		double sqrtD5 = Math.pow(D, 2.5);
		double z2 = normpdf((r-l*tau)/Math.sqrt(D*tau))*(
			            l*Math.sqrt(D)*Math.pow(tau,2.5)/6 - 
			            Math.sqrt(D)*sqrtt3*r/3 + 
			            Math.sqrt(D)*Math.sqrt(tau)*r*r/(6*l) + 
			            sqrtD3*sqrtt3/(12*l) + 
			            sqrtD3*Math.sqrt(tau)*r/(4*l*l) + 
			            sqrtD5*Math.sqrt(tau)/(4*l*l*l));
		double z3 = (1-normcdf((r+l*tau)/Math.sqrt(D*tau)))*
			          (D*D*D*Math.exp(2*l*r/D)/(8*l*l*l*l));
		return z1+z2+z3;
	}
	
	
	/**
	 * invoke as 
	 * <CODE>java -cp &lt;classpath&gt; tests.sic.rnqt.norm.RnQTCnorm 
	 * &lt;r&gt; &lt;Q&gt; &lt;T&gt;
	 * &lt;Kr&gt; &lt;Ko&gt; &lt;L&gt; &lt;&mu;&gt; &lt;&sigma;&gt; 
	 * &lt;h&gt; &lt;p&gt;</CODE>. 
	 * The constraints on the variables and parameters values are as follows:
	 * <ul>
	 * <li>T&ge;(3.5*&sigma;/&mu;)^2
	 * <li>&sigma;&gt;0
	 * <li>h&gt;0
	 * <li>p&gt;0
	 * </ul>
	 * All numbers (except r) must be non-negative. The reorder point variable r
	 * may take any real value.
	 * @param args String[]
	 */
	public static void main(String[] args) {
		double r = Double.parseDouble(args[0]);
		double Q = Double.parseDouble(args[1]);
		double T = Double.parseDouble(args[2]);
		double Kr = Double.parseDouble(args[3]);
		double Ko = Double.parseDouble(args[4]);
		double L = Double.parseDouble(args[5]);
		double m = Double.parseDouble(args[6]);
		double s = Double.parseDouble(args[7]);
		double h = Double.parseDouble(args[8]);
		double p = Double.parseDouble(args[9]);
		RnQTCnorm cc = new RnQTCnorm(Kr,Ko,L,m,s,h,p);
		double[] x = new double[]{r,Q,T};
		double val = cc.eval(x, null);
		System.out.println("y = "+val);
	}
	
}
