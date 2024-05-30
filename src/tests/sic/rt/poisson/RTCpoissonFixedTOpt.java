package tests.sic.rt.poisson;

import popt4jlib.OptimizerIntf;
import popt4jlib.FunctionIntf;
import popt4jlib.OptimizerException;
import popt4jlib.DblArray1Vector;
import popt4jlib.GradientDescent.OneDStepQuantumOptimizer;
import utils.Pair;
import utils.PairObjDouble;
import tests.sic.rnqt.poisson.RnQTCpoisson;
import java.util.HashMap;


/**
 * class implements an optimizer over the reorder-point variable R of the (R,T)
 * policy for given (fixed) review period T. The system being optimized faces 
 * stochastic demands as described in the class <CODE>RnQTCpoisson</CODE> but 
 * for fixed Q=1. The solution found is guaranteed to be the global optimum for 
 * the given review period T.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RTCpoissonFixedTOpt implements OptimizerIntf {
	private final double _T;
	
	
	/**
	 * sole public constructor.
	 * @param T double the given review period
	 */
	public RTCpoissonFixedTOpt(double T) {
		_T = T;
	}
	

	/**
	 * no-op.
	 * @param p HashMap unused 
	 */
	public void setParams(HashMap p) {
		// no-op
	}
	

	/**
	 * obtains the global minimum over all integer R of the function 
	 * <CODE>RnQTCpoisson(R,Q,T)</CODE> for the given Q=1, T&ge;0. Works by 
	 * solving the discrete convex programming problem in R, using 
	 * <CODE>OneDStepQuantumOptimizer</CODE>. 
	 * @param func RnQTCpoisson instance
	 * @return PairObjDouble  // Pair&lt;double[] x, double cb&gt; where x[0] is 
	 * s_opt for given _T. Also, x.length = 1
	 * @throws OptimizerException 
	 */
	public PairObjDouble minimize(FunctionIntf func) 
		throws OptimizerException {
		if (!(func instanceof RnQTCpoisson))
			throw new OptimizerException("RTCpoissonFixedTOpt.minimize(function): "+
				                           "function passed in must be RnQTCpoisson");
		RnQTCpoisson f = (RnQTCpoisson) func;
		OneDStepQuantumOptimizer onedopter = new OneDStepQuantumOptimizer();
		final double Q = 1.0;
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
		x0[0]=s0; x0[1]=Q; x0[2]=_T;
		double[] x_best = new double[1];  // {s}
		Pair p = null;
		try {
			p = onedopter.argmin(f, new DblArray1Vector(x0), null, 0, 
			                     1, lb, ub, niterbnd, multfactor, tol,
													 maxiterswithsamefunctionval, maxnumfuncevals);
		}
		catch (parallel.ParallelException e) {  // cannot get here
			e.printStackTrace();
		}
		copt = ((Double) p.getSecond()).doubleValue();
		x_best[0] = ((Double)p.getFirst()).doubleValue();
		PairObjDouble pod = new PairObjDouble(x_best, copt);
		return pod;
	}
	
	
	/**
	 * test-driver for the class. Invoke as:
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.rt.poisson.RTCpoissonFixedTOpt 
	 * &lt;T&gt; &lt;Kr&gt; &lt;Ko&gt; &lt;L&gt; &lt;&lambda;&gt;
	 * &lt;h&gt; &lt;p&gt; [p2(0)]
	 * </CODE>.
	 * @param args String[]
	 */
	public static void main(String[] args) {
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
		RTCpoissonFixedTOpt opt2D = new RTCpoissonFixedTOpt(T);
		try {
			PairObjDouble bp = opt2D.minimize(f);
			double[] xbest = (double[]) bp.getArg();
			double ybest = bp.getDouble();
			System.out.println("R*="+xbest[0]+" C*("+T+")="+ybest);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
}

