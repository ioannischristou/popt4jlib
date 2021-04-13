package tests.sic.sST.norm;

import popt4jlib.DblArray1Vector;
import popt4jlib.OptimizerIntf;
import popt4jlib.FunctionIntf;
import popt4jlib.OptimizerException;
import popt4jlib.GradientDescent.OneDStepQuantumOptimizer;
import utils.Pair;
//import popt4jlib.DblArray1Vector;
import utils.Messenger;
import utils.PairObjDouble;
//import utils.Pair;
import utils.PairObjTwoDouble;


/**
 * class implements an optimizer over the first two variables of the (s,S,T)
 * policy, namely the reorder point s and the order-up-to S variables, for given
 * (fixed) review period T. The system being optimized faces stochastic demands 
 * as described in the class <CODE>sSTCnorm</CODE>. The solution found is 
 * guaranteed to be the global optimum for the given review period T.
 * <p>Notes:
 * <ul>
 * <li>2020-04-25: added method seParams() (public) because it was moved up from
 * LocalOptimizerIntf to the root OptimizerIntf interface class.
 * <li>2021-01-06: fixed update of S in step 2 of the <CODE>minimize()</CODE>
 * method.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class sSTCnormFixedTOpt implements OptimizerIntf {
	private double _T;
	
	private double _epss = 1.0;
	private double _qnot = 1.e-6;

	
	/**
	 * public constructor keeps epss and qnot at default values.
	 * @param T double the given review period
	 */
	public sSTCnormFixedTOpt(double T) {
		_T = T;
	}
	
	
	/**
	 * public constructor sets the <CODE>_epss, _qnot</CODE> members.
	 * @param T double
	 * @param epss double
	 * @param qnot double
	 */
	public sSTCnormFixedTOpt(double T, double epss, double qnot) {
		_T = T;
		_epss = epss;
		_qnot = qnot;
	}

	
	/**
	 * sets the value for the minimum step-size for the movement of variables
	 * s and S.
	 * @param p HashMap may contain double value for the key "epss"; default is 1.
	 * May also contain double value for the key "qnot"; default is 1.e-6.
	 */
	public void setParams(java.util.HashMap p) {
		if (p.containsKey("epss")) 
			_epss = ((Double)p.get("epss")).doubleValue();
		if (p.containsKey("qnot")) 
			_qnot = ((Double)p.get("qnot")).doubleValue();		
	}


	/**
	 * obtains the global minimum over all s and all S &ge; s of the function 
	 * <CODE>sSTCnorm(s,S,T)</CODE> in this package for the given T. Implements 
	 * the Zheng-Federgruen algorithm for (s,S) policy optimization with 
	 * appropriate modifications in the formulae for computing the function G(y) 
	 * to account for the review length T.
	 * @param func sSTCnorm instance
	 * @return PairObjTwoDouble  // Pair&lt;double[] x, double cb, double lb&gt; 
	 *                             // where x[0] is s_opt, and x[1] is S_opt for 
	 *                             // _T.
	 * @throws OptimizerException 
	 */
	public PairObjTwoDouble minimize(FunctionIntf func) 
		throws OptimizerException {
		if (!(func instanceof sSTCnorm))
			throw new OptimizerException("sSTCnormFixedTOpt.minimize(function): "+
				                           "function passed in must be sSTCnorm");
		sSTCnorm f = (sSTCnorm) func;
		final Messenger mger = Messenger.getInstance();
		mger.msg("sSTCnormFixedTOpt.minimize(): start",2);
		final double L = f._L;
		final double mi = f._mi;
		final double sigma = f._sigma;
		final double h = f._h;
		final double p = f._p;
		final double p2 = f._p2;
		final double Ko = f._Ko;
		double copt = Double.POSITIVE_INFINITY;
		double[] x_best = new double[2];  // {s,S}
		double lbopt = Double.NaN;
		// step 1:
		mger.msg("sSTCnormFixedTOpt.minimize(): starting STEP 1", 2);
		mger.msg("sSTCnormFixedTOpt.minimize(): calling findGminarg()", 2);
		double ystar = findGminarg(_T, L, mi, sigma, h, p, p2);
		mger.msg("sSTCnormFixedTOpt.minimize(): findGminarg() returns "+ystar, 2);
		double s = ystar;
		double S_0 = ystar;
		java.util.HashMap param = new java.util.HashMap();
		param.put("Kr",new Double(0.0));
		double[] sS_0_T = new double[]{s,S_0,_T};
		double c0;
		while (true) {
			s -= _epss;
			sS_0_T[0] = s;
			c0 = f.eval(sS_0_T,param);
			double Gs = G(s,_T,L,mi,sigma,h,p,p2);
			if (c0 <= Gs) break;
		}
		mger.msg("sSTCnormFixedTOpt.minimize(): ended STEP 1 w/ c0="+c0, 2);		
		double s0 = s;
		double Su0 = S_0;
		double S = Su0+_epss;
		double GS = G(S,_T,L,mi,sigma,h,p,p2);
		// step 2:
		mger.msg("sSTCnormFixedTOpt.minimize(): starting STEP 2", 2);
		double[] sST = new double[]{s,S,_T};
		double css=Double.NaN;
		while (GS <= c0) {
			css = f.eval(sST, param);
			if (css < c0) {
				Su0 = S;
				sST[1] = Su0;  // set S
				double csS0 = f.eval(sST, param);
				double Gsp1 = G(s+_epss,_T,L,mi,sigma,h,p,p2);
				while (csS0 <= Gsp1) {
					s += _epss;
					sST[0] = s;  // set s
					csS0 = f.eval(sST, param);
					Gsp1 = G(s+_epss,_T,L,mi,sigma,h,p,p2);
				}
				c0 = csS0;
			}
			S += _epss;
			GS = G(S,_T,L,mi,sigma,h,p,p2);
			sST[1] = S;
			mger.msg("sSTCnormFixedTOpt.minimize(): in STEP 2, S="+S+" GS="+GS+
				       " c0="+c0+" css="+css, 3);			
		}
		mger.msg("sSTCnormFixedTOpt.minimize(): ended STEP 2 w/ GS="+GS, 2);
		x_best[0] = s;
		x_best[1] = Su0;
		double[] xaux = new double[]{s,Su0,_T};
		copt = f.eval(xaux, null);  // take into account review cost as well now
		// finally, evaluate the lower-bound (S,T) norm policy at (S(T),T)
		// SstarT = findminST(...)
    // lbopt = RnqTCnorm(SstarT,0,T,...)
		mger.msg("sSTCnormFixedTOpt.minimize(): start computing lower bound", 3);
		OneDStepQuantumOptimizer onedopter = new OneDStepQuantumOptimizer();
		FunctionIntf rnqtnorm = 
			new tests.sic.rnqt.norm.RnQTCnorm(f._Kr, 0, f._L, f._mi, f._sigma, 
				                                f._h, f._p);  // no p2 for norm; 
		                                                  // lb still good
		double[] x0 = new double[3];  // new double[]{s, 0, _T};
		x0[0]=s; x0[1]=_qnot; x0[2]=_T;
		final double lb = Double.NEGATIVE_INFINITY;
		final double ub = Double.POSITIVE_INFINITY;
		final int niterbnd = 3;
		final int multfactor = 2;
		final double tol = 1.e-12;
		final int maxiterswithsamefunctionval = 100;
		final int maxnumfuncevals = Integer.MAX_VALUE;
		Pair pr = null;
		try {
			pr = onedopter.argmin(rnqtnorm, new DblArray1Vector(x0), null, 
				                    0,  // the dimension s, along which to minimize f 
			                      1, lb, ub, niterbnd, multfactor, tol,
														maxiterswithsamefunctionval, maxnumfuncevals);
		}
		catch (parallel.ParallelException e) {  // cannot get here
			e.printStackTrace();
		}
		lbopt = ((Double)pr.getSecond()).doubleValue();
		mger.msg("sSTCnormFixedTOpt.minimize(): ended computing lower bound="+lbopt, 
			       3);
		mger.msg("sSTCnormFixedTOpt.minimize(): done",2);
		PairObjTwoDouble pod = new PairObjTwoDouble(x_best, copt, lbopt);
		return pod;
	}

	
	private double findGminarg(double T, double L, double mi, double sigma, 
		                         double h, double p, double p2) {
		double y = 0;
		double c = G(y, T, L, mi, sigma, h, p, p2);
		double c2 = G(y+_epss, T, L, mi, sigma, h, p, p2);
		if (c==c2) return y;
		if (c < c2) {
			while (true) {
				y -= _epss;
				c2 = G(y, T, L, mi, sigma, h, p, p2);
				if (c2 >= c) return y+_epss;
				else c = c2;
			}
		}
		else {
			while (true) {
				y += _epss;
				c2 = G(y, T, L, mi, sigma, h, p, p2);
				if (c2 >= c) return y-_epss;
				else c = c2;
			}
		}
	}
	
	
	private static double G(double s, double T, double L, double mi, double sigma, 
		                   double h, double p, double p2) {
		double z = 0.0;
		if (p2 > 0.0) {
			z += p2*sSTCnorm.eP(s, T, mi, sigma, L);
		}
    z += h*T*(s - L*mi - mi*T/2) + (h+p)*sSTCnorm.bP(s,T,mi,sigma,L);
    z /= T; // need to divide by T to account for the review period length
		return z;
	}
	
	
	/**
	 * invoke as:
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.sST.norm.sSTCnormFixedTOpt
	 * &lt;T&gt; &lt;Kr&gt; &lt;Ko&gt; &lt;L&gt; &lt;&mu;&gt; &lt;&sigma;&gt;
	 * &lt;h&gt; &lt;p&gt; [p2(0)] [epss(1.0) [qnot(0)] [dbglvl(0)]
	 * </CODE>.
	 * @param args 
	 */
	public static void main(String[] args) {
		double T = Double.parseDouble(args[0]);
		double Kr = Double.parseDouble(args[1]);
		double Ko = Double.parseDouble(args[2]);
		double L = Double.parseDouble(args[3]);
		double mi = Double.parseDouble(args[4]);
		double sigma = Double.parseDouble(args[5]);
		double h = Double.parseDouble(args[6]);
		double p = Double.parseDouble(args[7]);
		double p2 = args.length>8 ? Double.parseDouble(args[8]) : 0.0;
		double epss = args.length>9 ? Double.parseDouble(args[9]) : 1.0;
		double qnot = args.length>10 ? Double.parseDouble(args[10]) : 0.0;
		int dbglvl = args.length>11 ? Integer.parseInt(args[11]) : 0;
		Messenger.getInstance().setDebugLevel(dbglvl);
		final long st = System.currentTimeMillis();
		final sSTCnorm f = new sSTCnorm(Kr,Ko,L,mi,sigma,h,p,p2);
		final sSTCnormFixedTOpt opt2D = new sSTCnormFixedTOpt(T, epss, qnot);
		try {
			PairObjDouble bp = opt2D.minimize(f);
			double[] xbest = (double[]) bp.getArg();
			double ybest = bp.getDouble();
			final long dur = System.currentTimeMillis()-st;
			System.out.println("s*="+xbest[0]+" S*="+xbest[1]+" C*("+T+")="+ybest+
				                 " in "+dur+" msecs.");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}

