package tests.sic.rnqt.poisson;

import java.io.Serializable;
import java.util.HashMap;
import parallel.TaskObject;
import parallel.distributed.FailedReply;
import parallel.distributed.PDBTExecInitNoOpCmd;
import parallel.distributed.PDBTExecInitedClt;
import popt4jlib.DblArray1Vector;
import popt4jlib.FunctionIntf;
import popt4jlib.GradientDescent.OneDStepQuantumOptimizer;
import popt4jlib.OptimizerException;
import popt4jlib.OptimizerIntf;
import utils.Messenger;
import utils.Pair;
import utils.PairObjDouble;


/**
 * class implements the heuristic published in Christou, Skouri and 
 * Lagodimos (2020): "Fast evaluation of a periodic review inventory policy", 
 * Computers and Industrial Engineering, 144, in a fully parallel/distributed
 * manner. Notice that the heuristic is mathematically justified only for 
 * normal demands, but it can be applied equally well to all demands. Here it is
 * implemented for Poisson distributed demands.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RnQTCpoissonHeurPOpt implements OptimizerIntf {
	private RnQTCpoisson _poiss;
	private volatile double _epsT;
	final private double _Tnot;
	final private String _pdsrv;
	final private int _pdport;
	final private int _batchSz;
	
	final private double _epsQ = 1;  // discrete demands
	final private double _epsR = 1;  // discrete demands
			
	private PDBTExecInitedClt _pdclt;
	
	
	/**
	 * sole constructor.
	 * @param server String default "localhost"
	 * @param port int must be &gt; 1024, default 7891
	 * @param batchSz int must be positive, default 24
	 * @param epsT double must be positive, default 0.01
	 * @param Tnot double must be positive, default 0.01
	 */
	public RnQTCpoissonHeurPOpt(String server, int port, int batchSz,
		                       double epsT, double Tnot) {
		if (server==null || server.length()==0) _pdsrv = "localhost";
		else _pdsrv = server;
		if (port>1024) _pdport = port;
		else _pdport = 7891;
		_batchSz = (batchSz>0) ? batchSz : 24;
		_epsT = epsT>0 ? epsT : 0.01;
		_Tnot = Tnot>0 ? Tnot : 0.01;
	}

	
	/**
	 * set the <CODE>_pdclt</CODE> client if one exists in the parameters passed 
	 * in. Notice that the method is synchronized and will wait while any other 
	 * thread is running the <CODE>minimize()</CODE> method. In general, this 
	 * method should only be called PRIOR to calling the <CODE>minimize()</CODE>
	 * main method.
	 * @param p HashMap may contain a key-value pair of the form 
	 * &lt;"rnqtcpoissonheurpopt.pdclt", PDBTExecInitedClt clt&gt;
	 */
	public synchronized void setParams(HashMap p) {
		while (_poiss != null) {
			try {
				wait();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		if (p!=null && p.containsKey("rnqtcpoissonheurpopt.pdclt")) {
			_pdclt = (PDBTExecInitedClt) p.get("rnqtcpoissonheurpopt.pdclt");
		}
	}

	
	/**
	 * the most important class method. It is thread-safe, and it computes a near
	 * optimal parameter set for the (R,nQ,T) periodic review policy.
	 * @param f FunctionIntf must be of type RnQTCpoisson
	 * @return PairObjDouble  // Pair&lt;double[]{R, Q, T}, double copt&gt;
	 * @throws OptimizerException 
	 */
	public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (!(f instanceof RnQTCpoisson)) 
			throw new IllegalArgumentException("RnQTCpoissonHeurPOpt.minimize(f): "+
				                                 "f must be of type RnQTCpoisson");
		final Messenger mger = Messenger.getInstance();
		synchronized(this) {
			while (_poiss!=null) {
				try {
					wait();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			_poiss = (RnQTCpoisson) f;
			if (_pdclt==null) {
				mger.msg("RnQTCpoissonHeurPOpt.minimize(f): connecting on "+_pdsrv+
					       " on port "+_pdport, 2);
				_pdclt = new PDBTExecInitedClt(_pdsrv, _pdport);
				try {
					_pdclt.submitInitCmd(new PDBTExecInitNoOpCmd());
					mger.msg("RnQTCpoissonHeurPOpt.minimize(f): sent init cmd", 2);
				}
				catch (Exception e) {
					e.printStackTrace();
					throw new OptimizerException("RnQTCpoissonHeurPOpt.mainimize(f): "+
						                           "clt failed "+
						                           "to submit empty init-cmd to network");
				}
			}
		}
		try {
			// 0. initialize one-dimensional optimizer in R.
			final double h = _poiss._h;
			final double p = _poiss._p;
			final double p2 = _poiss._p2;
			final double Kr = _poiss._Kr;
			final double Ko = _poiss._Ko;
			final double L = _poiss._L;
			final double lambda = _poiss._lambda;
			mger.msg("RnQTCpoissonHeurPOpt.minimize(f): step 0 (init) done.", 1);
			// 1. initialize return variables
			double sopt = Double.NaN;
			double qopt = Double.NaN;
			double topt = Double.NaN;
			double copt = Double.POSITIVE_INFINITY;
			mger.msg("RnQTCpoissonHeurPOpt.minimize(f): step 1 (init) done.", 1);
			// 2. compute EOQ quantities
			final double mi = _poiss._lambda;
			double T_star_eoq = Math.sqrt(2.0*Ko*(p+h)/(mi*h*p));
			double Q_star_eoq = Math.sqrt(2.0*Ko*mi*(p+h)/(h*p));
			mger.msg("RnQTCpoissHeurPOpt.minimize(f): step 2 (EOQ) done. "+
				       " T*_EOQ="+T_star_eoq+" Q*_EOQ="+Q_star_eoq, 1);
			// 3. compute optimal (S,T)
			final double tnot = _Tnot;
			mger.msg("RnQTCpoissonHeurPOpt.minimize(f): step 3 (ST) tnot="+tnot, 1);
			final double qnot = _epsQ;
			double start_T = Math.max(Math.ceil(T_star_eoq/_epsT)*_epsT, tnot);
			double[] argopt_ST = STCpoissonOptFastApprox(start_T, Kr, Ko, L, 
				                                           lambda, h, p, p2);
			// argopt_ST is double[]{s,T,c}
			// itc: apparently, starting at start_T doesn't work out so good in some
			// cases. This is the formula in MATLAB as well for normal demands as well
			double T = tnot-_epsT;  // start_T;
			final double T_limit_st = argopt_ST[2]-Kr/argopt_ST[1];
			mger.msg("RnQTCpoissonHeurPOpt.minimize(f): step 3 (ST) done.", 1);
			// 4. main iteration loop
			final double[] x0 = new double[3];  // new double[]{s0, _qnot, _T};
			boolean stop = false;
			while (!stop) {
				mger.msg("RnQTCpoissonHeurPOpt.minimize(f): running step 4 w/ T="+T, 1);
				// short-cut heuristic when Kr=0
				if (Kr==0 && T>tnot) break;
				
				// heuristically speed-up the T-search
				if (T>=0.25 && _epsT<0.01) _epsT = 0.01;
				else if (T>=2 && _epsT<0.1) _epsT = 0.1;
				else if (T>=2.5 && _epsT<0.15) _epsT = 0.15;
				mger.msg("RnQTCpoissonHeurPOpt.minimize(f): running step 4 set T="+T,1);
					
				double c;
				// 4.1. prepare batch
				TaskObject[] batch = new TaskObject[_batchSz];
				double Tstart = T;
				for (int i=0; i<_batchSz; i++) {
					T += _epsT;
					batch[i] = new RnQTCpoissonFixedTHeurPOptTask(_poiss, T);
				}
				try {
					mger.msg("RnQTCpoissonHeurPOpt.minimize(): submit a batch of "+
						       _batchSz+
						       " tasks to network for period length from "+(Tstart+_epsT)+
						       " up to "+T, 
						       1);
					Object[] res = _pdclt.submitWorkFromSameHost(batch);
					for (int i=0; i<res.length; i++) {
						RnQTCpoissonFixedTHeurPOpterResult ri = 
							(RnQTCpoissonFixedTHeurPOpterResult) res[i];
						c = ri._C;
						if (c < copt) {
							sopt = ri._R;
							qopt = ri._Q;
							topt = ri._T;
							copt = c;
						}
						// stopping condition on the T-search
						double lhs = c-Kr/ri._T;
						if (lhs >= T_limit_st) stop = true;
					}
				}
				catch (Exception e) {  // cannot get here
					e.printStackTrace();
					throw new OptimizerException("RnQTCpoissonHeurPOpt.minimize(f): "+
						                           "failed");
				}				
				// T += _epsT;
			}  // while true T-outer loop
			mger.msg("RnQTCpoissonHeurPOpt.minimize(f): running step 4 done", 1);
			// 5. finally compare with  base-stock policy
			x0[0] = sopt; x0[1] = qopt; x0[2] = topt;
			copt = _poiss.eval(x0, null);
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
				_poiss = null;
				notify();
			}
		}
	}
	
	
	/**
	 * computes the approximate cost function arising from the P<sub>o</sub> 
	 * approximation by the formula K<sub>o</sub>min(1, &mu;T/Q) where &mu; is the
	 * mean demand in a time-unit. Note that for this function to work, the 
	 * <CODE>_poiss</CODE> data field must have been correctly set prior to 
	 * calling the method.
	 * @param f RnQTCpoisson the function to evaluate
	 * @param s double reorder point must be integer-valued
	 * @param Q double batch size must be positive integer-valued
	 * @param T double review period must be positive
	 * @return double the approximation value
	 */
	static double snQTCpoissonApprox2(RnQTCpoisson f, 
		                                double s, double Q, double T) {
		Pair p2 = f.evalBoth(new double[]{s, Q, T}, null);
		double res = ((Double)p2.getSecond()).doubleValue();
		res += f._Ko*Math.min(1.0, f._lambda*T/Q)/T;
		return res;
	}

	
	/**
	 * auxiliary method computes the optimal parameters for the corresponding 
	 * base-stock (S,T) periodic review policy, not part of the public API.
	 * @param startT double the starting T
	 * @param Kr double
	 * @param Ko double
	 * @param L double
	 * @param lambda double
	 * @param h double
	 * @param p double
	 * @param p2 double
	 * @return double[]  // [sopt, topt, copt]
	 * @throws OptimizerException 
	 */
	private double[] STCpoissonOptFastApprox(double startT, double Kr, double Ko,
		                                       double L, double lambda,
																					 double h, double p, double p2) 
		throws OptimizerException {
		final OneDStepQuantumOptimizer onedopter = new OneDStepQuantumOptimizer();			
		final FunctionIntf f2 = new RnQTCpoisson(0, 0, L, lambda, h, p, p2);
		final Messenger mger = Messenger.getInstance();
		mger.msg("RnQTCpoissonHeurPOpt.STCpoissonOptFastApprox(): started", 1);
		double[] res = new double[3];
		double T = startT;
		double c_prev = Double.POSITIVE_INFINITY;
		double c_opt_st = c_prev;
		double s_opt_st = Double.NaN;
		double t_opt_st = Double.NaN;
		double s0 = Math.ceil(_poiss._lambda*(L+T));
		final double[] x0 = new double[3];
		while (true) {
			mger.msg("RnQTCpoissonHeurPOpt.STCpoissonOptFastApprox(): running T="+T,
				       2);
			final double lb = Integer.MIN_VALUE;
			final double ub = Integer.MAX_VALUE;
			final int niterbnd = 3;
			final int multfactor = 2;
			final double tol = 1.e-12;
			final int maxiterswithsamefunctionval = 100;
			final int maxnumfuncevals = Integer.MAX_VALUE;
			x0[0]=s0; x0[1]=1; x0[2]=T;
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
			double c = _poiss.eval(x0, null);
			if (c < c_opt_st) {
				mger.msg("RnQTCpoissonHeurPOpt.STCpoissonOptFastApprox(): best c="+c,2);
				s_opt_st = x0[0];
				t_opt_st = T;
				c_opt_st = c;
			}
			if (c > c_prev) break;  // stopping condition
			c_prev = c;
			T += _epsT;
		}
		mger.msg("RnQTCpoissonHeurPOpt.STCpoissonOptFastApprox(): done", 1);
		res[0] = s_opt_st;
		res[1] = t_opt_st;
		res[2] = c_opt_st;
		return res;
	}
	
	
	/**
	 * a thread may call this method at any point to terminate the sole connection
	 * to the workers' network. The method will terminate the connection as soon
	 * as there is no other thread running the <CODE>minimize(f)</CODE> method.
	 */
	public synchronized void terminateServerConnection() {
		while (_poiss!=null) {
			try {
				wait();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		try {
			_pdclt.terminateConnection();
		}
		catch (Exception e) {
			e.printStackTrace();  // ignore further
			throw new Error("RnQTCpoissonHeurPOpt.terminateServerConnection() "+
				              "failed?");
		}
	}

	
	/**
	 * test-driver method to test the class functionality. Invoke as:
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.rnqt.poisson.RnQTCpoissonHeurPOpt
	 * &lt;Kr&gt; &lt;Ko&gt; &lt;L&gt; 
	 * &lt;&lambda;&gt;
	 * &lt;h&gt; &lt;p&gt; [p2(0)]
	 * [srv(localhost)]
	 * [port(7891)]
	 * [batchSz(24)]
	 * [epsT(0.01)]
	 * [Tnot(0.01)]
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
		double lambda = Double.parseDouble(args[3]);
		double h = Double.parseDouble(args[4]);
		double p = Double.parseDouble(args[5]);
		double p2 = 0;
		if (args.length>6) p2 = Double.parseDouble(args[6]);
		String srv = "localhost";
		if (args.length>7) srv = args[7];
		int port = 7891;
		if (args.length>8) port = Integer.parseInt(args[8]);
		int batchSz = 24;
		if (args.length>9) batchSz = Integer.parseInt(args[9]);
		double epsT = 0.01;
		if (args.length>10) epsT = Double.parseDouble(args[10]);
		double Tnot = 0.01;
		if (args.length>11) Tnot = Double.parseDouble(args[11]);
		int dbglvl = 0;
		if (args.length>12) dbglvl = Integer.parseInt(args[12]);
		mger.setDebugLevel(dbglvl);
		FunctionIntf f = new RnQTCpoisson(Kr, Ko, L, lambda, h, p, p2);
		RnQTCpoissonHeurPOpt heuropt = new RnQTCpoissonHeurPOpt(srv, port, batchSz, 
			                                                       epsT, Tnot);
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


/**
 * auxiliary class encapsulating the notion of heuristically optimizing an 
 * <CODE>RnQTCpoisson</CODE> function, with a fixed review period T. NOT part of 
 * the public API. Notice that this class together with the similarly named but
 * ending with "Result" class are not nested inside the main class 
 * <CODE>RnQTCpoissonHeurPOpt</CODE> because the outer class contains the 
 * <CODE>_pdclt</CODE> object reference which cannot (and must not) be 
 * serializable.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class RnQTCpoissonFixedTHeurPOptTask implements TaskObject {
	private RnQTCpoisson _f;
	private double _T;
		public RnQTCpoissonFixedTHeurPOptTask(RnQTCpoisson f, double T) {
		_f = f;
		_T = T;
	}

	public Serializable run() {
		final Messenger mger = Messenger.getInstance();
		final OneDStepQuantumOptimizer onedopter = new OneDStepQuantumOptimizer();			
		final FunctionIntf f2 = new RnQTCpoisson(0, 0, _f._L, 
																					_f._lambda, 
																					_f._h, _f._p, _f._p2);
		final double Q_star_eoq = 
			Math.sqrt(2*_f._Ko*_f._lambda*(_f._p+_f._h)/(_f._h*_f._p));
		double Q = Math.ceil(Q_star_eoq);
		double s0 = Math.ceil(_f._lambda*(_f._L+_T));
		double c_prev = Double.POSITIVE_INFINITY;
		double c;
		double copt = Double.POSITIVE_INFINITY;
		double sopt = Double.NaN;
		double qopt = Double.NaN;
		double topt = Double.NaN;
		double[] x0 = new double[3];
		while (true) {
			// inner loop: Q-search
			mger.msg("RnQTCpoissonFixedTHeurPOptTask.run(): running step 4 "+
							 "inner-loop w/ Q="+Q, 2);
			final double lb = Integer.MIN_VALUE;
			final double ub = Integer.MAX_VALUE;
			final int niterbnd = 3;
			final int multfactor = 2;
			final double tol = 1.e-12;
			final int maxiterswithsamefunctionval = 100;
			final int maxnumfuncevals = Integer.MAX_VALUE;
			x0[0]=s0; x0[1]=Q; x0[2]=_T;
			Pair popt = null;
			try {
				final DblArray1Vector x0_vec = new DblArray1Vector(x0);
				popt = onedopter.argmin(f2, x0_vec, null, 0, 
																1, lb, ub, niterbnd, multfactor, tol,
																maxiterswithsamefunctionval, 
																maxnumfuncevals);
			}
			catch (Exception e) {  // cannot get here
				e.printStackTrace();
				return new FailedReply();
			}
			x0[0] = ((Double)popt.getFirst()).doubleValue();
			// x0[0] is the current cost minimizer given (Q,T)
			c = RnQTCpoissonHeurPOpt.snQTCpoissonApprox2(_f, x0[0], Q, _T);
			if (c < copt) {
				sopt = x0[0];
				qopt = Q;
				topt = _T;
				copt = c;
				mger.msg("RnQTCpoissonFixedTHeurPOptTask.run(): running step 4 T-loop "+
										 "at "+_T+" found better approx sol w/ c="+copt+
										 " (s="+sopt+",Q="+qopt+")",2);
			}
			s0 = x0[0];
			if (c_prev < c) break;
			c_prev = c;
			Q += 1;  // _epsQ
		}  // while true Q-inner loop
		// finally, return the result
		RnQTCpoissonFixedTHeurPOpterResult res = 
			new RnQTCpoissonFixedTHeurPOpterResult(topt, sopt, qopt, copt);
		return res;
	}

	/**
	 * always throws.
	 * @throws UnsupportedOperationException unchecked.
	 */
	public boolean isDone() {
		throw new UnsupportedOperationException("isDone: NOT implemented");
	}

	/**
	 * always throws.
	 * @param other unused.
	 * @throws UnsupportedOperationException unchecked.
	 */
	public void copyFrom(TaskObject other) {
		throw new UnsupportedOperationException("copyFrom: NOT implemented");		
	}
}


/**
 * auxiliary class that is essentially just an immutable struct, holding 4 
 * double values. Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class RnQTCpoissonFixedTHeurPOpterResult implements Serializable {
	public final double _T;
	public final double _R;
	public final double _Q;
	public final double _C;
	
	public RnQTCpoissonFixedTHeurPOpterResult(double T, double R, double Q, 
  																	     double c) {
		_T = T;
		_Q = Q;
		_R = R;
		_C = c;
	}
}
