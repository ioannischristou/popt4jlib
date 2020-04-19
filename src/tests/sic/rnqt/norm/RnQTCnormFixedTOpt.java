package tests.sic.rnqt.norm;

import popt4jlib.OptimizerIntf;
import popt4jlib.FunctionIntf;
import popt4jlib.OptimizerException;
import popt4jlib.DblArray1Vector;
import popt4jlib.GradientDescent.OneDStepQuantumOptimizer;
//import utils.PairObjDouble;
import utils.Pair;
import utils.PairObjThreeDouble;
//import utils.PairObjTwoDouble;


/**
 * class implements an optimizer over the first two variables of the (R,nQ,T)
 * policy, namely the reorder point R and the batch size Q variables, for given
 * (fixed) review period T. The system being optimized faces stochastic demands 
 * as described in the class <CODE>RnQTCnorm</CODE>. The solution found is 
 * guaranteed to be the global optimum for the given review period T.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RnQTCnormFixedTOpt implements OptimizerIntf {
	private double _T;
	
	private double _epss;
	private double _epsq;
	private double _qnot;
	
	private double _curBest;
	
	
	/**
	 * sole public constructor.
	 * @param T double the given review period
	 * @param eps_q double the step-size over Q
	 * @param q_not double the initial value of the Q variable
	 * @param eps_s double the step-size over R
	 * @param currentMinVal double the best currently known value of the total 
	 * cost, may be <CODE>Double.POSITIVE_INFINITY</CODE> as default.
	 */
	public RnQTCnormFixedTOpt(double T, double eps_q, double q_not, 
		                        double eps_s, double currentMinVal) {
		_T = T;
		_epss = eps_s;
		_epsq = eps_q;
		_qnot = q_not;
		_curBest = currentMinVal;
	}
	

	/**
	 * obtains the global minimum over all R and all Q &ge; 0 of the function 
	 * <CODE>RnQTCnorm(R,Q,T)</CODE> for the given T&ge;T_min. Works by 
	 * incrementing the Q variable at steps of size <CODE>_epsq</CODE>, and for
	 * each value of Q, solves the convex programming problem in R, using the 
	 * <CODE>OneDStepQuantumOptimizer</CODE>. The algorithm continues incrementing
	 * Q until the cost surpasses a lower bound value, in an area in which we are
	 * guaranteed that the cost function is increasing in Q when taken at the 
	 * optimal R(Q) for such Q.
	 * @param func RnQTCnorm instance
	 * @return PairObjThreeDouble  // Pair&lt;double[] x, double cb, double lb,
	 * double oc&gt; where x[0] is s_opt and x[1] is q_opt for given _T.
	 * @throws OptimizerException 
	 */
	public PairObjThreeDouble minimize(FunctionIntf func) 
		throws OptimizerException {
		if (!(func instanceof RnQTCnorm))
			throw new OptimizerException("RnQTCnormFixedTOpt.minimize(function): "+
				                           "function passed in must be RnQTCnorm");
		RnQTCnorm f = (RnQTCnorm) func;
		OneDStepQuantumOptimizer onedopter = new OneDStepQuantumOptimizer();
		double Q = _qnot;
		double copt = Double.POSITIVE_INFINITY;
		double s0 = (f._L + _T)*f._mi;
		double lb = -100.0*(f._mi*_T + 10.0*f._sigma*Math.sqrt(_T));
		double ub = +100.0*(f._mi*_T + 10.0*f._sigma*Math.sqrt(_T));
		int niterbnd = 3;
		int multfactor = 2;
		double tol = 1.e-12;
		int maxiterswithsamefunctionval = 100;
		int maxnumfuncevals = Integer.MAX_VALUE;
		double[] x0 = new double[3];  // new double[]{s0, _qnot, _T};
		x0[0]=s0; x0[1]=_qnot; x0[2]=_T;
		double[] x_best = new double[2];  // {s,Q}
		double lb_q = 0;
		double ordct = Double.NaN;
		double lbopt = Double.NaN;
		while (lb_q<=Math.min(_curBest,copt)) {
			Pair p = null;
			try {
				p = onedopter.argmin(f, new DblArray1Vector(x0), null, 0, 
				                     _epss, lb, ub, niterbnd, multfactor, tol,
														 maxiterswithsamefunctionval, maxnumfuncevals);
			}
			catch (parallel.ParallelException e) {  // cannot get here
				e.printStackTrace();
			}
			double y_q = ((Double) p.getSecond()).doubleValue();
			x0[0] = ((Double)p.getFirst()).doubleValue();
			Pair pv = f.evalBoth(x0);  // needless 2nd evaluation just to get lb
			if (Double.compare(((Double)pv.getFirst()).doubleValue(), y_q)!=0) {
				// insanity
				throw new IllegalStateException("y_q="+y_q+" but pv="+pv+"?");
			}
			lb_q = ((Double)pv.getSecond()).doubleValue();
			if (Double.compare(y_q, copt)<0) {
				copt = y_q;
				ordct = y_q - lb_q;
				// lbopt = lb_q;  // the best lower-bound is not necessarily here
				x_best[0] = ((Double)p.getFirst()).doubleValue();
				x_best[1] = x0[1];
				if (Double.compare(copt, _curBest)<0) _curBest = copt;
			}
			if (Double.compare(lb_q, lbopt)<0) {  // update lbopt
				lbopt = lb_q;
			}
			x0[1] += _epsq;
		}
		PairObjThreeDouble pod = new PairObjThreeDouble(x_best, copt, lbopt, ordct);
		return pod;
	}
	
	
	/**
	 * test-driver for the class. Invoke as:
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.rnqt.norm.RnQTCnormFixedTOpt 
	 * &lt;T&gt; &lt;Kr&gt; &lt;Ko&gt; &lt;L&gt; &lt;&mu;&gt; &lt;&sigma;&gt;
	 * &lt;h&gt; &lt;p&gt;
	 * </CODE>.
	 * @param args String[]
	 */
	public static void main(String[] args) {
		final double epsq = 0.1;
		final double qnot = 1.e-8;
		final double epss = 1.e-4;
		final double curbst = Double.POSITIVE_INFINITY;
		double T = Double.parseDouble(args[0]);
		double Kr = Double.parseDouble(args[1]);
		double Ko = Double.parseDouble(args[2]);
		double L = Double.parseDouble(args[3]);
		double mi = Double.parseDouble(args[4]);
		double sigma = Double.parseDouble(args[5]);
		double h = Double.parseDouble(args[6]);
		double p = Double.parseDouble(args[7]);
		RnQTCnorm f = new RnQTCnorm(Kr,Ko,L,mi,sigma,h,p);
		RnQTCnormFixedTOpt opt2D = new RnQTCnormFixedTOpt(T,epsq,qnot,epss,curbst);
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

