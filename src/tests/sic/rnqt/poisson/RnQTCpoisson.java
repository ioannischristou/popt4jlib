package tests.sic.rnqt.poisson;

import popt4jlib.FunctionIntf;
import popt4jlib.DblArray1Vector;
import utils.Pair;
import cern.jet.random.Poisson;


/**
 * function implements the continuous-time long-run expected cost of a periodic
 * review, single echelon inventory control system facing exogenous demands 
 * generated in a period of length T, from a Poisson process with parameters 
 * &lambda;&gt;0. The system faces linear holding and 
 * backorder costs with cost rates h&gt;0, p&gt;0 and p2&ge;0. It also faces 
 * fixed costs: 
 * fixed review cost per period Kr&ge;0, and fixed order cost Ko&ge;0. Finally, 
 * there is a constant lead-time for each order being placed equal to L&ge;0.
 * The control parameters then, are r (the reorder point), Q (the batch size),
 * and T (the review interval).
 * The code below uses the formulae 5-20, 5-22...5-24, 5-27...5-5-29 and 5-33 
 * in the Hadley-Whittin (1963) textbook "Analysis of Inventory Systems".
 * <p>Notes:
 * <ul>
 * <li>2021-04-07: fixed a bug in the computation of the holding and back-orders
 * costs (missing parentheses in the computation of terms contributing to the
 * inventory costs)
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2019</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RnQTCpoisson implements FunctionIntf {
	private double _Kr;
	double _Ko;
	double _L;
	double _lambda;
	double _h, _p, _p2;
	
	
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
	public RnQTCpoisson(double Kr, double Ko, double L, 
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
	 * fixed backorder cost as well, and
	 * fixed review and order costs controlled by (r,nQ,T) periodic review policy.
	 * @param x double[] representing r (order up-to), Q (batch size) and T 
	 * (review period)
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
	 * system facing Poisson demands with linear holding and backorder costs and 
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
		Poisson pois = new tests.sic.sST.poisson.Poisson2(1);
		
		pois.setMean(_lambda*T);
		double Po = _lambda*T*pois.cdf(Q-1)/Q + complpoisscdf(pois,Q+1,_lambda*T);
		
		// itc-20210407: sum of 4 terms multiplying _h below wasn't in parenthesis
		double y = (_Kr+Ko*Po)/T + _h*((Q+1)/2.0 + s - _lambda*_L - _lambda*T/2.0);
		y += (_h+_p)*bP(s,Q,T,_L,_lambda,pois) + _p2*eP(s,Q,T,_L,_lambda,pois);
		return new Pair(new Double(y), new Double(y-Ko*Po/T));
	}

	
	private static double bP(int s, int Q, double T, double L, double lambda, 
		               Poisson pois) {
		return (yP(s,T,L,lambda,pois) - yP(s+Q,T,L,lambda,pois))/Q;
	}
	
	
	private static double yP(int v, double T, double L, double lambda, 
		                       Poisson pois) {
		return KsiP(v,T+L,lambda,T,pois) - KsiP(v,L,lambda,T,pois);
	}
	
	
	private static double KsiP(int v, double t, double lambda, double Tcap,
		                         Poisson pois) {
		double z31 = -lambda*v*t*t*complpoisscdf(pois,v,lambda*t) / 
			           (2.0*Tcap);
		double z32 = (lambda*lambda)*(t*t*t)*complpoisscdf(pois,v-1,lambda*t) /
			           (6.0*Tcap);
		double z33 = v*(v+1)*t*complpoisscdf(pois,v+1,lambda*t) / (2.0*Tcap);
		double z34 = -v*(v+1)*(v+2)*complpoisscdf(pois,v+2,lambda*t) / 
			           (6.0*lambda*Tcap);
		return z31 + z32 + z33 + z34;
	}
	
	
	private static double eP(int s, int Q, double T, double L, double lambda, 
		                       Poisson pois) {
		return (lambdaP(s,T,L,lambda,pois) - lambdaP(s+Q,T,L,lambda,pois)) / Q;
	}
	
	
	private static double lambdaP(int v, double T, double L, double lambda, 
		                            Poisson pois) {
		return (betaP(v,L+T,lambda,pois) - betaP(v,L,lambda,pois))/T;
	}
	
	
	private static double betaP(int v, double t, double lambda, Poisson pois) {
		double lt = lambda*t;
		double w31 = lt*lt*complpoisscdf(pois,v-1,lt)/2.0;
		double w32 = lt*v*complpoisscdf(pois,v,lt);
		double w33 = v*(v+1)*complpoisscdf(pois,v+1,lt)/2.0;
		return w31 - w32 + w33;
	}
	
	
	private static double complpoisscdf(Poisson pois, int x, double lm) {
		if (Double.compare(lm, 0.0)<=0) {  // corner case
			if (x<0) {
				return 1.0;  // MATLAB poisscdf returns 0
			}
			else return 0.0;  // MATLAB poisscdf returns 1 when lm=0, x>=0
		}
		pois.setMean(lm);
		return 1.0 - pois.cdf(x-1);
	} 

	
	/**
	 * invoke as 
	 * <CODE>java -cp &lt;classpath&gt; tests.sic.rnqt.poisson.RnQTCpoisson
	 * &lt;r&gt; &lt;Q&gt; &lt;T&gt;
	 * &lt;Kr&gt; &lt;Ko&gt; &lt;L&gt; &lt;&lambda;&gt;
	 * &lt;h&gt; &lt;p&gt; [p2(0)]</CODE>. 
	 * The constraints on the variables and parameters values are as follows:
	 * <ul>
	 * <li>T&gt;0
	 * <li>&lambda;&gt;0
	 * <li>p_l&gt;0
	 * <li>h&gt;0
	 * <li>p&gt;0
	 * <li>p2&ge;0
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
		double h = Double.parseDouble(args[7]);
		double p = Double.parseDouble(args[8]);
		double p2 = 0.0;
		if (args.length>9) p2 = Double.parseDouble(args[9]);
		RnQTCpoisson cc = new RnQTCpoisson(Kr,Ko,L,lambda,h,p,p2);
		double[] x = new double[]{r,Q,T};
		long start = System.currentTimeMillis();
		double val = cc.eval(x, null);
		long dur = System.currentTimeMillis()-start;
		System.out.println("y = "+val);
		System.out.println("duration="+dur+" msecs");
	}

}

