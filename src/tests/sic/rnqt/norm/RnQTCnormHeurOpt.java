package tests.sic.rnqt.norm;

import java.util.HashMap;
import popt4jlib.DblArray1Vector;
import popt4jlib.FunctionIntf;
import popt4jlib.GradientDescent.OneDStepQuantumOptimizer;
import popt4jlib.OptimizerException;
import popt4jlib.OptimizerIntf;
import utils.Messenger;
import utils.Pair;
import utils.PairObjDouble;


/**
 * class implements an approximate heuristic optimizer over ALL three variables 
 * of the (R,nQ,T) policy, namely the reorder point R, the batch size Q and the 
 * review period T as described in Christou, Skouri and Lagodimos (2020): "Fast
 * evaluation of a periodic review inventory policy", Computers and Industrial
 * Engineering, 144.
 * The system being optimized faces stochastic demands as described in the class 
 * <CODE>RnQTCnorm</CODE>. 
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RnQTCnormHeurOpt implements OptimizerIntf {
	private RnQTCnorm _norm;
	private double _epsT;
	private double _epsQ;
	private double _epsR;

	// default constants for search step-sizes
	private static final double _MIN_EPST = 0.01;
	private static final double _MIN_EPSQ = 0.1;
	private static final double _MIN_EPSR = 0.1;
	
	
	/**
	 * sole constructor.
	 * @param epsT double if zero is passed, the constant _MIN_EPST is used
	 * @param epsQ double if zero is passed, the constant _MIN_EPSQ is used
	 * @param epsR double if zero is passed, the constant _MIN_EPSR is used
	 */
	public RnQTCnormHeurOpt(double epsT, double epsQ, double epsR) {
		_epsT = epsT > 0 ? epsT : _MIN_EPST;
		_epsQ = epsQ > 0 ? epsQ : _MIN_EPSQ;
		_epsR = epsR > 0 ? epsR : _MIN_EPSR;
	}

	
	/**
	 * no-op.
	 * @param p HashMap unused 
	 */
	public void setParams(HashMap p) {
		// no-op.
	}

	
	/**
	 * the most important class method. It is thread-safe, and it computes a near
	 * optimal (in all tests performed, the real optimal) parameter set for
	 * the (R,nQ,T) periodic review policy.
	 * @param f FunctionIntf must be of type RnQTCnorm
	 * @return PairObjDouble  // Pair&lt;double[]{R, Q, T}, double copt&gt;
	 * @throws OptimizerException 
	 */
	public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (!(f instanceof RnQTCnorm)) 
			throw new IllegalArgumentException("RnQTCnormHeurOpt.minimize(f): "+
				                                 "f must be of type RnQTCnorm");
		synchronized(this) {
			while (_norm!=null) {
				try {
					wait();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			_norm = (RnQTCnorm) f;
		}
		try {
			final Messenger mger = Messenger.getInstance();
			// 0. initialize one-dimensional optimizer in R.
			final double h = _norm._h;
			final double p = _norm._p;
			final double Kr = _norm.getKr();
			final double Ko = _norm.getKo();
			final double L = _norm._L;
			final double mi = _norm._mi;
			final double sigma = _norm._sigma;
			final OneDStepQuantumOptimizer onedopter = new OneDStepQuantumOptimizer();			
			final FunctionIntf f2 = new RnQTCnorm(0, 0, L, mi, sigma, h, p);
			mger.msg("RnQTCnormHeurOpt.minimize(f): step 0 (init) done.", 1);
			// 1. initialize return variables
			double sopt = Double.NaN;
			double qopt = Double.NaN;
			double topt = Double.NaN;
			double copt = Double.POSITIVE_INFINITY;
			mger.msg("RnQTCnormHeurOpt.minimize(f): step 1 (init) done.", 1);
			// 2. compute EOQ quantities
			double T_star_eoq = Math.sqrt(2.0*Ko*(p+h)/(mi*h*p));
			double Q_star_eoq = Math.sqrt(2.0*Ko*mi*(p+h)/(h*p));
			mger.msg("RnQTCnormHeurOpt.minimize(f): step 2 (EOQ) done. "+
				       " T*_EOQ="+T_star_eoq+" Q*_EOQ="+Q_star_eoq, 1);
			// 3. compute optimal (S,T)
			final double tnot = Math.pow(3.5*sigma/mi, 2);
			mger.msg("RnQTCnormHeurOpt.minimize(f): step 3 (ST) tnot="+tnot, 1);
			final double qnot = _epsQ < 1.e-6 ? _epsQ : 1.e-6;
			double step_s = _epsR;
			double start_T = Math.max(Math.ceil(T_star_eoq/_epsT)*_epsT, tnot);
			double[] argopt_ST = STCnormOptFastApprox(start_T, Kr, Ko, L, 
				                                        mi, sigma, h, p, qnot);
			// argopt_ST is double[]{s,T,c}
			// itc: apparently, starting at start_T doesn't work out so good in some
			// cases. This is the formula in MATLAB as well.
			double T = tnot;  // start_T;
			final double T_limit_st = argopt_ST[2]-_norm.getKr()/argopt_ST[1];
			mger.msg("RnQTCnormHeurOpt.minimize(f): step 3 (ST) done.", 1);
			// 4. main iteration loop
			final double[] x0 = new double[3];  // new double[]{s0, _qnot, _T};
			while (true) {
				mger.msg("RnQTCnormHeurOpt.minimize(f): running step 4 w/ T="+T, 1);
				// short-cut heuristic when Kr=0
				if (_norm.getKr()==0 && T>tnot) break;
				
				// heuristically speed-up the T-search
				if (T>=0.25 && _epsT<0.01) _epsT = 0.01;
				else if (T>=2 && _epsT<0.1) _epsT = 0.1;
				else if (T>=2.5 && _epsT<0.15) _epsT = 0.15;
				mger.msg("RnQTCnormHeurOpt.minimize(f): running step 4 set T="+T, 1);
				
				double Q = Math.ceil(Q_star_eoq/_epsQ)*_epsQ;
				double s0 = mi*(L+T);
				double c_prev = Double.POSITIVE_INFINITY;
				double c = Double.POSITIVE_INFINITY;
				while (true) {
					// inner loop: Q-search
					mger.msg("RnQTCnormHeurOpt.minimize(f): running step 4 "+
						       "inner-loop w/ Q="+Q, 2);
					final double lb = -100.0*(_norm._mi*T+10.0*_norm._sigma*Math.sqrt(T));
					final double ub = +100.0*(_norm._mi*T+10.0*_norm._sigma*Math.sqrt(T));
					final int niterbnd = 3;
					final int multfactor = 2;
					final double tol = 1.e-12;
					final int maxiterswithsamefunctionval = 100;
					final int maxnumfuncevals = Integer.MAX_VALUE;
					x0[0]=s0; x0[1]=Q; x0[2]=T;
					Pair popt = null;
					try {
						final DblArray1Vector x0_vec = new DblArray1Vector(x0);
						popt = onedopter.argmin(f2, x0_vec, null, 0, 
								   								  _epsR, lb, ub, niterbnd, multfactor, tol,
																    maxiterswithsamefunctionval, 
																		maxnumfuncevals);
					}
					catch (parallel.ParallelException e) {  // cannot get here
						e.printStackTrace();
					}
					// double y_q = ((Double) popt.getSecond()).doubleValue();
					x0[0] = ((Double)popt.getFirst()).doubleValue();
	        // x0[0] is the current cost minimizer given (Q,T)
					c = snQTCnormApprox2(x0[0], Q, T);
					if (c < copt) {
						sopt = x0[0];
						qopt = Q;
						topt = T;
						copt = c;
						mger.msg("RnQTCnormHeurOpt.minimize(f): running step 4 T-loop "+
							       "at "+T+" found better approx sol w/ c="+copt+
							       " (s="+sopt+",Q="+qopt+")",1);
					}
					s0 = x0[0];
					if (c_prev < c) break;
					c_prev = c;
					Q += _epsQ;
				}  // while true Q-inner loop
				// stopping condition on the T-search
				double lhs = c-Kr/T;
				if (lhs >= T_limit_st) break;
				mger.msg("RnQTCnormHeurOpt.minimize(f): running step 4 T-loop "+
					       "cost distance to zero for stopping lhs="+lhs+
					       " rhs="+T_limit_st, 1);
				T += _epsT;
			}  // while true T-outer loop
			mger.msg("RnQTCnormHeurOpt.minimize(f): running step 4 done", 1);
			// 5. finally compare with  base-stock policy
			x0[0] = sopt; x0[1] = qopt; x0[2] = topt;
			copt = _norm.eval(x0, null);
			if (copt > argopt_ST[2]) {
				sopt = argopt_ST[0];
				qopt = qnot;
				topt = argopt_ST[1];
				copt = argopt_ST[2];
			}
			final double[] xopt = new double[]{sopt, qopt, topt};
			return new PairObjDouble(xopt, copt);
		}
		finally {
			synchronized(this) {
				_norm = null;
				notify();
			}
		}
	}
	
	
	/**
	 * computes the approximate cost function arising from the P<sub>o</sub> 
	 * approximation by the formula K<sub>o</sub>min(1, &mu;T/Q). Note that for 
	 * this function to work, the <CODE>_norm</CODE> data field must have been 
	 * correctly set prior to calling the method.
	 * @param s double reorder point
	 * @param Q double batch size must be positive
	 * @param T double review period must be &ge; (3.5&sigma;/&mu;)<sup>2</sup>
	 * @return double the approximation value
	 */
	private double snQTCnormApprox2(double s, double Q, double T) {
		// y = snQTCnormApprox2(s,Q,T,Kr,K0,L,mi,sigma,h,p,p2,Qmin)
		Pair p2 = _norm.evalBoth(new double[]{s, Q, T});
		double res = ((Double)p2.getSecond()).doubleValue();
		res += _norm.getKo()*Math.min(1.0, _norm._mi*T/Q)/T;
		return res;
	}

	
	/**
	 * auxiliary method computes the optimal parameters for the corresponding 
	 * base-stock (S,T) periodic review policy, not part of the public API.
	 * @param startT double the starting T
	 * @param Kr double
	 * @param Ko double
	 * @param L double
	 * @param mi double
	 * @param sigma double
	 * @param h double
	 * @param p double
	 * @param qnot double
	 * @return double[]  // [sopt, topt, copt]
	 * @throws OptimizerException 
	 */
	private double[] STCnormOptFastApprox(double startT, double Kr, double Ko,
		                                    double L, double mi, double sigma,
																				double h, double p, 
																				double qnot) throws OptimizerException {
		final OneDStepQuantumOptimizer onedopter = new OneDStepQuantumOptimizer();			
		final FunctionIntf f2 = new RnQTCnorm(0, 0, L, mi, sigma, h, p);
		final Messenger mger = Messenger.getInstance();
		mger.msg("RnQTCnormHeurOpt.STCnormOptFastApprox(): started", 1);
		double[] res = new double[3];
		double T = startT;
		double c_prev = Double.POSITIVE_INFINITY;
		double c_opt_st = c_prev;
		double s_opt_st = Double.NaN;
		double t_opt_st = Double.NaN;
		double s0 = mi*(L+T);
		final double[] x0 = new double[3];
		while (true) {
			mger.msg("RnQTCnormHeurOpt.STCnormOptFastApprox(): running w/ T="+T, 2);
			final double lb = -100.0*(_norm._mi*T+10.0*_norm._sigma*Math.sqrt(T));
			final double ub = +100.0*(_norm._mi*T+10.0*_norm._sigma*Math.sqrt(T));
			final int niterbnd = 3;
			final int multfactor = 2;
			final double tol = 1.e-12;
			final int maxiterswithsamefunctionval = 100;
			final int maxnumfuncevals = Integer.MAX_VALUE;
			x0[0]=s0; x0[1]=qnot; x0[2]=T;
			Pair popt = null;
			try {
				final DblArray1Vector x0_vec = new DblArray1Vector(x0);
				popt = onedopter.argmin(f2, x0_vec, null, 0, 
						   								  _epsR, lb, ub, niterbnd, multfactor, tol,
														    maxiterswithsamefunctionval, 
																maxnumfuncevals);
			}
			catch (parallel.ParallelException e) {  // cannot get here
				e.printStackTrace();
			}
			x0[0] = ((Double)popt.getFirst()).doubleValue();
			s0 = x0[0];
			double c = _norm.eval(x0, null);
			if (c < c_opt_st) {
				mger.msg("RnQTCnormHeurOpt.STCnormOptFastApprox(): found new best c="+c, 
					       2);
				s_opt_st = x0[0];
				t_opt_st = T;
				c_opt_st = c;
			}
			if (c > c_prev) break;  // stopping condition
			c_prev = c;
			T += _epsT;
		}
		mger.msg("RnQTCnormHeurOpt.STCnormOptFastApprox(): done", 1);
		res[0] = s_opt_st;
		res[1] = t_opt_st;
		res[2] = c_opt_st;
		return res;
	}
	

	/**
	 * test-driver method to test the class functionality. Invoke as:
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.rnqt.norm.RnQTCnormHeurOpt
	 * &lt;Kr&gt; &lt;Ko&gt; &lt;L&gt; 
	 * &lt;&mu;&gt; &lt;&sigma;&gt; 
	 * &lt;h&gt; &lt;p&gt;
	 * [epsT(0.01)]
	 * [epsQ(0.1)]
	 * [epsR(1.e-4)]
	 * [dbglvl(0)]
	 * </CODE>.
	 * It prints out the heuristic set of best (R,Q,T) parameters it could find
	 * plus the associated cost, and the elapsed time.
	 * @param args String[]
	 */
	public static void main(String[] args) {
		final Messenger mger = Messenger.getInstance();
		double Kr = Double.parseDouble(args[0]);
		double Ko = Double.parseDouble(args[1]);
		double L = Double.parseDouble(args[2]);
		double mi = Double.parseDouble(args[3]);
		double sigma = Double.parseDouble(args[4]);
		double h = Double.parseDouble(args[5]);
		double p = Double.parseDouble(args[6]);
		double epsT = 0.01;
		if (args.length>7) epsT = Double.parseDouble(args[7]);
		double epsQ = 0.1;
		if (args.length>8) epsQ = Double.parseDouble(args[8]);
		double epsR = 1.e-4;
		if (args.length>9) epsR = Double.parseDouble(args[9]);
		int dbglvl = 0;
		if (args.length>10) dbglvl = Integer.parseInt(args[10]);
		mger.setDebugLevel(dbglvl);
		FunctionIntf f = new RnQTCnorm(Kr, Ko, L, mi, sigma, h, p);
		RnQTCnormHeurOpt heuropt = new RnQTCnormHeurOpt(epsT, epsQ, epsR);
		long start = System.currentTimeMillis();
		PairObjDouble res=null;
		try {
			res = heuropt.minimize(f);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		long dur = System.currentTimeMillis()-start;
		double[] x = (double[]) res.getArg();
		double cost = res.getDouble();
		System.out.println("R_h="+x[0]+" Q_h="+x[1]+" T_h="+x[2]+" C_h="+cost+
			                 " in "+dur+" msecs");
	}
}
