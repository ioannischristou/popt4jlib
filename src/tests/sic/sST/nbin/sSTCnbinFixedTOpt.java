package tests.sic.sST.nbin;

import popt4jlib.DblArray1Vector;
import popt4jlib.OptimizerIntf;
import popt4jlib.FunctionIntf;
import popt4jlib.FunctionBaseStatic;
import popt4jlib.OptimizerException;
import popt4jlib.GradientDescent.OneDStepQuantumOptimizer;
import utils.Messenger;
import utils.Pair;
import utils.PairObjDouble;
import utils.PairObjTwoDouble;


/**
 * class implements an optimizer over the first two variables of the (s,S,T)
 * policy, namely the reorder point s and the order-up-to S variables, for given
 * (fixed) review period T. The system being optimized faces stochastic demands 
 * as described in the class <CODE>sSTCnbin</CODE>. The solution found is 
 * guaranteed to be the global optimum for the given review period T.
 * <p>Notes:
 * <ul>
 * <li>2024-03-23: allowed the <CODE>G(.)</CODE> function minimization to start
 * from a given s, rather than zero every time.
 * <li>2021-06-07: excluded fixed review costs from lower bound as they decrease
 * in time.
 * <li>2021-06-06: fixed issue related to computing the <CODE>G(.)</CODE>
 * function given demand rates and process parameters.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class sSTCnbinFixedTOpt implements OptimizerIntf {
	private double _T;
	private int _s0 = Integer.MAX_VALUE;  // by default, don't use this number
	
	
	/**
	 * one-arg original public constructor.
	 * @param T double the given review period
	 */
	public sSTCnbinFixedTOpt(double T) {
		_T = T;
	}
	
	
	/**
	 * two-arg constructor allows starting the <CODE>G(.)</CODE> function 
	 * optimization to start from a "good" initial reorder point.
	 * @param T double the given review period
	 * @param s0 int the reorder point to start searching from
	 */
	public sSTCnbinFixedTOpt(double T, int s0) {
		_T = T;
		_s0 = s0;
	}

	
	/**
	 * no-op.
	 * @param p HashMap unused 
	 */
	public void setParams(java.util.HashMap p) {
		// no-op.
	}


	/**
	 * obtains the global minimum over all s and all S &ge; s of the function 
	 * <CODE>sSTCnbin(s,S,T)</CODE> for the given T. Implements the Zheng-
	 * Federgruen algorithm for (s,S) policy optimization with appropriate 
	 * modifications in the formulae for computing the function G(y) to account 
	 * for the review length T.
	 * @param func sSTCnbin instance
	 * @return PairObjTwoDouble  // Pair&lt;double[] x, double cb, double lb&gt; 
	 *                           // where x[0] is s_opt, and x[1] is S_opt for _T.
	 * @throws OptimizerException 
	 */
	public PairObjTwoDouble minimize(FunctionIntf func) 
		throws OptimizerException {
		if (!(func instanceof sSTCnbin))
			throw new OptimizerException("sSTCnbinFixedTOpt.minimize(function): "+
				                           "function passed in must be sSTCnbin");
		Messenger mger = Messenger.getInstance();
		sSTCnbin f = (sSTCnbin) func;
		FunctionIntf f2 = new FunctionBaseStatic(f);  // use FunctionBaseStatic 
		                                              // to keep track of #function
																									// calls
		final double L = f._L;
		final double lambda = f._lambda;
		final double p_l = f._p_l;
		final double h = f._h;
		final double p = f._p;
		final double Ko = f._Ko;
		double copt = Double.POSITIVE_INFINITY;
		double[] x_best = new double[2];  // {s,S}
		double lbopt = Double.NaN;
		// step 1:
		int ystar = findGminarg(_T, L, lambda, p_l, h, p);
		int s = ystar;
		int S_0 = ystar;
		java.util.HashMap param = new java.util.HashMap();
		param.put("Kr",new Double(0.0));
		double[] sS_0_T = new double[]{s,S_0,_T};
		double c0;
		while (true) {
			--s;
			sS_0_T[0] = s;
			c0 = f2.eval(sS_0_T,param);
			double Gs = G(s,_T,L,lambda,p_l,h,p);
			if (Double.compare(c0,Gs) <= 0) break;
		}
		int s0 = s;
		int Su0 = S_0;
		int S = Su0+1;
		double GS = G(S,_T,L,lambda,p_l,h,p);
		// step 2:
		double[] sST = new double[]{s,S,_T};
		while (Double.compare(GS,c0) <= 0) {
			double css = f2.eval(sST, param);
			if (css < c0) {
				Su0 = S;
				sST[1] = Su0;  // set S
				double csS0 = f2.eval(sST, param);
				double Gsp1 = G(s+1,_T,L,lambda,p_l,h,p);
				while (Double.compare(csS0, Gsp1) <= 0) {
					++s;
					sST[0] = s;  // set s
					csS0 = f2.eval(sST, param);
					Gsp1 = G(s+1,_T,L,lambda,p_l,h,p);
				}
				c0 = csS0;
			}
			++S;
			sST[1] = S;  // itc20210106: this line didn't exist before, but is 
			             // needed in order for the next iteration in the while loop
			GS = G(S,_T,L,lambda,p_l,h,p);
		}
		x_best[0] = s;
		x_best[1] = Su0;
		double[] xaux = new double[]{s,Su0,_T};
		copt = f2.eval(xaux, null);  // take into account review cost as well now
		// finally, evaluate the lower-bound (S,T) Poisson policy at (S(T),T)
		// SstarT = findminST(...)
    // lbopt = RnqTCpoisson(SstarT,1,T,...)
		OneDStepQuantumOptimizer onedopter = new OneDStepQuantumOptimizer();
		FunctionIntf rnqtnbin = 
			new tests.sic.rnqt.nbin.RnQTCnbin(f._Kr, 0, f._L, f._lambda, f._p_l, 
				                                f._h, f._p);
		double[] x0 = new double[3];  // new double[]{s, 1, _T};
		x0[0]=s; x0[1]=1; x0[2]=_T;
		final double lb = Integer.MIN_VALUE;
		final double ub = Integer.MAX_VALUE;
		final int niterbnd = 3;
		final int multfactor = 2;
		final double tol = 1.e-12;
		final int maxiterswithsamefunctionval = 100;
		final int maxnumfuncevals = Integer.MAX_VALUE;
		Pair pr = null;
		try {
			pr = onedopter.argmin(rnqtnbin, new DblArray1Vector(x0), null, 
				                    0,  // the dimension s, along which to minimize f 
			                      1, lb, ub, niterbnd, multfactor, tol,
														maxiterswithsamefunctionval, maxnumfuncevals);
		}
		catch (parallel.ParallelException e) {  // cannot get here
			e.printStackTrace();
		}
		lbopt = ((Double)pr.getSecond()).doubleValue() - f._Kr/_T;
		// itc-20210607: used to be: ((Double)pr.getSecond()).doubleValue()
		
		PairObjTwoDouble pod = new PairObjTwoDouble(x_best, copt, lbopt);
		mger.msg("sSTCnbinFixedTOpt.minimize(f): for T="+_T+
			       ": s*="+x_best[0]+" S*="+x_best[1]+" C*(T)="+copt+
			       " LB(T)="+lbopt, 1);
		return pod;
	}

	
	private int findGminarg(double T, double L, double lambda, double p_l, 
		                      double h, double p) {
		int y = 0;
		if (_s0!=Integer.MAX_VALUE) y = _s0;  // start from _s0 if passed in
		double c = G(y, T, L, lambda, p_l, h, p);
		double c2 = G(y+1, T, L, lambda, p_l, h, p);
		if (c==c2) return y;
		if (c < c2) {
			while (true) {
				c2 = G(--y, T, L, lambda, p_l, h, p);
				if (c2 >= c) return y+1;
				else c = c2;
			}
		}
		else {
			while (true) {
				c2 = G(++y, T, L, lambda, p_l, h, p);
				if (c2 >= c) return y-1;
				else c = c2;
			}
		}
	}
	
	
	private static double G(int s, double T, double L, double lambda, double p_l,
		                      double h, double p) {
		double md = sSTCnbin.getMeanDemand(lambda, p_l);
		double y = h*T*(s-L*md-md*T/2.0) +  // itc20210606: md was lambda
			         (h+p)*sSTCnbin.bP(s, T, lambda, p_l, L);
		y /= T;  // divide by T to account for the review period length
		return y;
	}
	

	/**
	 * test-driver main method to test the class functionality.
	 * Invoke as:
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.sST.nbin.sSTCnbinFixedTOpt
	 * &lt;T&gt;
	 * &lt;Kr&gt;
	 * &lt;Ko&gt;
	 * &lt;L&gt;
	 * &lt;&lambda;&gt;
	 * &lt;p<sub>l</sub>&gt;
	 * &lt;h&gt;
	 * &lt;p&gt;
	 * </CODE>.
	 * @param args String[] 
	 */
	public static void main(String[] args) {
		double T = Double.parseDouble(args[0]);
		double Kr = Double.parseDouble(args[1]);
		double Ko = Double.parseDouble(args[2]);
		double L = Double.parseDouble(args[3]);
		double lambda = Double.parseDouble(args[4]);
		double p_l = Double.parseDouble(args[5]);
		double h = Double.parseDouble(args[6]);
		double p = Double.parseDouble(args[7]);
		sSTCnbin f = new sSTCnbin(Kr,Ko,L,lambda,p_l,h,p);
		sSTCnbinFixedTOpt opt2D = new sSTCnbinFixedTOpt(T);
		try {
			PairObjDouble bp = opt2D.minimize(f);
			double[] xbest = (double[]) bp.getArg();
			double ybest = bp.getDouble();
			System.out.println("s*="+xbest[0]+" S*="+xbest[1]+" C*("+T+")="+ybest);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}

