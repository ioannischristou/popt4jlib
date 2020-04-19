package tests.sic.sST.poisson;

import cern.jet.random.Poisson;
import popt4jlib.DblArray1Vector;
import popt4jlib.OptimizerIntf;
import popt4jlib.FunctionIntf;
import popt4jlib.OptimizerException;
import popt4jlib.GradientDescent.OneDStepQuantumOptimizer;
import utils.Pair;
//import popt4jlib.DblArray1Vector;
import utils.PairObjDouble;
//import utils.Pair;
import utils.PairObjTwoDouble;


/**
 * class implements an optimizer over the first two variables of the (s,S,T)
 * policy, namely the reorder point s and the order-up-to S variables, for given
 * (fixed) review period T. The system being optimized faces stochastic demands 
 * as described in the class <CODE>sSTCpoisson</CODE>. The solution found is 
 * guaranteed to be the global optimum for the given review period T.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class sSTCpoissonFixedTOpt implements OptimizerIntf {
	private double _T;
	
	private static ThreadLocal _tlpois = new ThreadLocal() {
		protected Object initialValue() {
			return null;
		}
	};
	
	/**
	 * sole public constructor.
	 * @param T double the given review period
	 */
	public sSTCpoissonFixedTOpt(double T) {
		_T = T;
	}
	

	/**
	 * obtains the global minimum over all s and all S &ge; s of the function 
	 * <CODE>sSTCpoisson(s,S,T)</CODE> for the given T. Implements the Zheng-
	 * Federgruen algorithm for (s,S) policy optimization with appropriate 
	 * modifications in the formulae for computing the function G(y) to account 
	 * for the review length T.
	 * @param func sSTCpoisson instance
	 * @return PairObjTwoDouble  // Pair&lt;double[] x, double cb, double lb&gt; 
	 *                           // where x[0] is s_opt, and x[1] is S_opt for _T.
	 * @throws OptimizerException 
	 */
	public PairObjTwoDouble minimize(FunctionIntf func) 
		throws OptimizerException {
		if (!(func instanceof sSTCpoisson))
			throw new OptimizerException("sSTCpoissonFixedTOpt.minimize(function): "+
				                           "function passed in must be sSTCpoisson");
		sSTCpoisson f = (sSTCpoisson) func;
		final double L = f._L;
		final double lambda = f._lambda;
		final double h = f._h;
		final double p = f._p;
		final double p2 = f._p2;
		final double Ko = f._Ko;
		double copt = Double.POSITIVE_INFINITY;
		double[] x_best = new double[2];  // {s,S}
		double lbopt = Double.NaN;
		// step 1:
		int ystar = findGminarg(_T, L, lambda, h, p, p2);
		int s = ystar;
		int S_0 = ystar;
		java.util.HashMap param = new java.util.HashMap();
		param.put("Kr",new Double(0.0));
		double[] sS_0_T = new double[]{s,S_0,_T};
		double c0;
		while (true) {
			--s;
			sS_0_T[0] = s;
			c0 = f.eval(sS_0_T,param);
			double Gs = G(s,_T,L,lambda,h,p,p2);
			if (Double.compare(c0,Gs) <= 0) break;
		}
		int s0 = s;
		int Su0 = S_0;
		int S = Su0+1;
		double GS = G(S,_T,L,lambda,h,p,p2);
		// step 2:
		double[] sST = new double[]{s,S,_T};
		while (Double.compare(GS,c0) <= 0) {
			double css = f.eval(sST, param);
			if (css < c0) {
				Su0 = S;
				sST[1] = Su0;  // set S
				double csS0 = f.eval(sST, param);
				double Gsp1 = G(s+1,_T,L,lambda,h,p,p2);
				while (Double.compare(csS0, Gsp1) <= 0) {
					++s;
					sST[0] = s;  // set s
					csS0 = f.eval(sST, param);
					Gsp1 = G(s+1,_T,L,lambda,h,p,p2);
				}
				c0 = csS0;
			}
			++S;
			GS = G(S,_T,L,lambda,h,p,p2);
		}
		x_best[0] = s;
		x_best[1] = Su0;
		double[] xaux = new double[]{s,Su0,_T};
		copt = f.eval(xaux, null);  // take into account review cost as well now
		// finally, evaluate the lower-bound (S,T) Poisson policy at (S(T),T)
		// SstarT = findminST(...)
    // lbopt = RnqTCpoisson(SstarT,1,T,...)
		OneDStepQuantumOptimizer onedopter = new OneDStepQuantumOptimizer();
		FunctionIntf rnqtpois = 
			new tests.sic.rnqt.poisson.RnQTCpoisson(f._Kr, 0, f._L, f._lambda, 
				                                      f._h, f._p, f._p2);
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
			pr = onedopter.argmin(rnqtpois, new DblArray1Vector(x0), null, 
				                    0,  // the dimension s, along which to minimize f 
			                      1, lb, ub, niterbnd, multfactor, tol,
														maxiterswithsamefunctionval, maxnumfuncevals);
		}
		catch (parallel.ParallelException e) {  // cannot get here
			e.printStackTrace();
		}
		lbopt = ((Double)pr.getSecond()).doubleValue();
		
		PairObjTwoDouble pod = new PairObjTwoDouble(x_best, copt, lbopt);
		return pod;
	}

	
	private static int findGminarg(double T, double L, double lambda, 
		                             double h, double p, double p2) {
		int y = 0;
		double c = G(y, T, L, lambda, h, p, p2);
		double c2 = G(y+1, T, L, lambda, h, p, p2);
		if (Double.compare(c,c2)==0) return y;
		if (Double.compare(c,c2)<0) {
			while (true) {
				c2 = G(--y, T, L, lambda, h, p, p2);
				if (Double.compare(c2,c)>=0) return y+1;
				else c = c2;
			}
		}
		else {
			while (true) {
				c2 = G(++y, T, L, lambda, h, p, p2);
				if (Double.compare(c2,c)>=0) return y-1;
				else c = c2;
			}
		}
	}
	
	
	private static double G(int s, double T, double L, double lambda, 
		                   double h, double p, double p2) {
		double z = 0.0;
		Poisson pois = (Poisson) _tlpois.get();
		if (pois==null) {
			pois = new Poisson2(1);
			_tlpois.set(pois);
		}
		if (Double.compare(p2, 0.0)>0) {
			z += p2*sSTCpoisson.eP(s, T, lambda, L, pois);
		}
		double y = h*T*(s-L*lambda-lambda*T/2.0) + 
			         (h+p)*(sSTCpoisson.bP(s, T, lambda, L, pois)) + z;
		y /= T;  // divide by T to account for the review period length
		return y;
	}
	
	
	public static void main(String[] args) {
		double T = Double.parseDouble(args[0]);
		double Kr = Double.parseDouble(args[1]);
		double Ko = Double.parseDouble(args[2]);
		double L = Double.parseDouble(args[3]);
		double lambda = Double.parseDouble(args[4]);
		double h = Double.parseDouble(args[5]);
		double p = Double.parseDouble(args[6]);
		double p2 = args.length>7 ? Double.parseDouble(args[7]) : 0.0;
		sSTCpoisson f = new sSTCpoisson(Kr,Ko,L,lambda,h,p,p2);
		sSTCpoissonFixedTOpt opt2D = new sSTCpoissonFixedTOpt(T);
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

