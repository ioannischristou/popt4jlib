package tests.sic.rnqt.poisson;

import popt4jlib.OptimizerIntf;
import popt4jlib.FunctionIntf;
import popt4jlib.OptimizerException;
import popt4jlib.DblArray1Vector;
import popt4jlib.GradientDescent.OneDStepQuantumOptimizer;
//import utils.PairObjDouble;
import utils.Pair;
import utils.PairObjThreeDouble;
//import utils.PairObjTwoDouble;
import java.util.HashMap;


/**
 * class implements an optimizer over the first two variables of the (R,nQ,T)
 * policy, namely the reorder point R and the batch size Q variables, for given
 * (fixed) review period T. The system being optimized faces stochastic demands 
 * as described in the class <CODE>RnQTCpoisson</CODE>. The solution found is 
 * guaranteed to be the global optimum for the given review period T.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RnQTCpoissonFixedTOpt implements OptimizerIntf {
	private double _T;
	
	private double _curBest;
	
	
	/**
	 * sole public constructor.
	 * @param T double the given review period
	 * @param currentMinVal double the best currently known value of the total 
	 * cost, may be <CODE>Double.POSITIVE_INFINITY</CODE> as default.
	 */
	public RnQTCpoissonFixedTOpt(double T, double currentMinVal) {
		_T = T;
		_curBest = currentMinVal;
	}
	

	/**
	 * no-op.
	 * @param p HashMap unused 
	 */
	public void setParams(HashMap p) {
		// no-op
	}
	

	/**
	 * obtains the global minimum over all integer R and all integer Q &gt; 0 of 
	 * the function <CODE>RnQTCpoisson(R,Q,T)</CODE> for the given T&ge;0. Works  
	 * by incrementing the Q variable at steps of size 1, and for
	 * each value of Q, solves the discrete convex programming problem in R, using 
	 * <CODE>OneDStepQuantumOptimizer</CODE>. The algorithm continues incrementing
	 * Q until the cost surpasses a lower bound value, in an area in which we are
	 * guaranteed that the cost function is increasing in Q when taken at the 
	 * optimal R(Q) for such Q.
	 * @param func RnQTCpoisson instance
	 * @return PairObjThreeDouble  // Pair&lt;double[] x, double cb, double lb,
	 * double oc&gt; where x[0] is s_opt and x[1] is q_opt for given _T.
	 * @throws OptimizerException 
	 */
	public PairObjThreeDouble minimize(FunctionIntf func) 
		throws OptimizerException {
		if (!(func instanceof RnQTCpoisson))
			throw new OptimizerException("RnQTCpoissonFixedTOpt.minimize(function): "+
				                           "function passed in must be RnQTCpoisson");
		RnQTCpoisson f = (RnQTCpoisson) func;
		OneDStepQuantumOptimizer onedopter = new OneDStepQuantumOptimizer();
		double Q = 1;
		double copt = Double.POSITIVE_INFINITY;
		double s0 = Math.round((f._L + _T)*f._lambda);  // order-up-to pt is integer
		double lb = Double.NEGATIVE_INFINITY;
		double ub = Double.POSITIVE_INFINITY;
		int niterbnd = 3;
		int multfactor = 2;
		double tol = 1.e-12;
		int maxiterswithsamefunctionval = 100;
		int maxnumfuncevals = Integer.MAX_VALUE;
		double[] x0 = new double[3];  // new double[]{s0, _qnot, _T};
		x0[0]=s0; x0[1]=1; x0[2]=_T;
		double[] x_best = new double[2];  // {s,Q}
		double lb_q = 0;
		double lbopt = Double.NaN;
		double capproxopt = Double.POSITIVE_INFINITY;
		while (lb_q<=Math.min(_curBest,copt)) {
			Pair p = null;
			try {
				p = onedopter.argmin(f, new DblArray1Vector(x0), null, 0, 
				                     1, lb, ub, niterbnd, multfactor, tol,
														 maxiterswithsamefunctionval, maxnumfuncevals);
			}
			catch (parallel.ParallelException e) {  // cannot get here
				e.printStackTrace();
			}
			double y_q = ((Double) p.getSecond()).doubleValue();
			x0[0] = ((Double)p.getFirst()).doubleValue();
			Pair pv = f.evalBoth(x0,null);  // needless 2nd evaluation just to get lb
			if (Double.compare(((Double)pv.getFirst()).doubleValue(), y_q)!=0) {
				// insanity
				throw new IllegalStateException("y_q="+y_q+" but pv="+pv+"?");
			}
			lb_q = ((Double)pv.getSecond()).doubleValue();
			if (Double.compare(y_q, copt)<0) {
				copt = y_q;
				// lbopt = lb_q;  // the best lower-bound is not necessarily here
				x_best[0] = ((Double)p.getFirst()).doubleValue();
				x_best[1] = x0[1];
				if (Double.compare(copt, _curBest)<0) _curBest = copt;
			}
			if (Double.compare(lb_q, lbopt)<0) {  // update lbopt
				lbopt = lb_q;
			}
			// compute capprox and compare with capproxopt: notice that we're only
			// going as high in Q for capproxopt as we are going for the "real" cost
			// but this should not be an issue
			double capprox = lb_q + 
				               f._Ko*Math.min(1.0, f._lambda*_T/x0[1]) / _T;
			if (capprox < capproxopt) capproxopt = capprox;  
			x0[1]++;  // increment Q
		}
		// itc20191118: notice the last param below used to be the order cost ordct
		PairObjThreeDouble pod = new PairObjThreeDouble(x_best, copt, 
			                                              lbopt - f._Kr/_T,
			                                              // itc-20210607: used to be
			                                              // just lbopt
			                                              capproxopt);
		return pod;
	}
	
	
	/**
	 * test-driver for the class. Invoke as:
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.rnqt.poisson.RnQTCpoissonFixedTOpt 
	 * &lt;T&gt; &lt;Kr&gt; &lt;Ko&gt; &lt;L&gt; &lt;&lambda;&gt;
	 * &lt;h&gt; &lt;p&gt; [p2(0)]
	 * </CODE>.
	 * @param args String[]
	 */
	public static void main(String[] args) {
		final double curbst = Double.POSITIVE_INFINITY;
		double T = Double.parseDouble(args[0]);
		double Kr = Double.parseDouble(args[1]);
		double Ko = Double.parseDouble(args[2]);
		double L = Double.parseDouble(args[3]);
		double lambda = Double.parseDouble(args[4]);
		double h = Double.parseDouble(args[5]);
		double p = Double.parseDouble(args[6]);
		double p2 = 0.0;
		if (args.length>7) p2 = Double.parseDouble(args[7]);
		RnQTCpoisson f = new RnQTCpoisson(Kr,Ko,L,lambda,h,p,p2);
		RnQTCpoissonFixedTOpt opt2D = new RnQTCpoissonFixedTOpt(T,curbst);
		try {
			PairObjThreeDouble bp = opt2D.minimize(f);
			double[] xbest = (double[]) bp.getArg();
			double ybest = bp.getDouble();
			double lbbest = bp.getSecondDouble();
			double ordct = bp.getThirdDouble();
			System.out.println("R*="+xbest[0]+" Q*="+xbest[1]+" C*("+T+")="+ybest+
				                 " LB*("+T+")="+lbbest+" FixedOrderCost("+T+")="+ordct);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
}

