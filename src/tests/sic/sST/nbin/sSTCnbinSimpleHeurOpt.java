package tests.sic.sST.nbin;

import popt4jlib.FunctionIntf;
import popt4jlib.OptimizerException;
import popt4jlib.OptimizerIntf;
import parallel.distributed.PDBTExecInitNoOpCmd;
import parallel.distributed.PDBTExecInitedClt;
import utils.PairObjDouble;
import utils.Messenger;
import tests.sic.rnqt.nbin.*;
import java.util.HashMap;


/**
 * class computes an approximate solution to the (s,S,T) policy optimization 
 * under compound Poisson demands modeled with the Negative Binomial 
 * distribution, by computing the optimal (r,nQ,T) policy parameters
 * r*,Q*, and T* -which is usually done much faster- and then setting s*=r*, and
 * S*=r*+Q*.
 * In fact, we also compute the (R,nQ,T) optimal policy for a system with review
 * cost equal to Kr and zero ordering cost and for the resulting review period 
 * T' we compute similarly the optimal s(T') and S(T'), and compare with the 
 * above computed cost, and we return the winner.
 * <p>Notes:
 * <ul>
 * <li>2023-07-05: dissallowed multiple threads from concurrently running the 
 * <CODE>minimize(f)</CODE> method of this class.
 * <li>2023-07-04: The 2nd heuristic above is only performed if there is no key
 * parameter called "zeroOrderingCost" with value false passed in the optimizer
 * parameters.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class sSTCnbinSimpleHeurOpt implements OptimizerIntf {
	/**
	 * default address for PDBTExecSingleCltWrkInitSrv
	 */
	private final String _pdsrv;  
	/**
	 * default port for PDBTExecSingleCltWrkInitSrv
	 */
	private final int _pdport;
	
	private PDBTExecInitedClt _pdclt;
	
	private final double _epsT;
	private final double _Tnot;
	
	/**
	 * by default, running the 2nd heuristic is true.
	 */
	private volatile boolean _run4ZeroOrderCost = true;
	
	/**
	 * by default, 24 tasks to be submitted each time to be processed in parallel.
	 */
	private final int _batchSz;
	
	
	private int _numRunning = 0;

	
	/**
	 * sole public constructor.
	 * @param pdsrv String
	 * @param pdport int
	 * @param epst double
	 * @param tnot double
	 * @param batchSz int  // default value 24
	 */
	public sSTCnbinSimpleHeurOpt(String pdsrv, int pdport, 
		                           double epst, double tnot, 
															 int batchSz) {
		_pdsrv = pdsrv;
		_pdport = pdport;
		_epsT = epst;
		_Tnot = tnot;
		_batchSz = batchSz>0 ? batchSz : 24;
	}
	

	/**
	 * checks for the "zeroOrderingCost" key (only if no other thread is running
	 * concurrently the <CODE>minimize(f)</CODE> method).
	 * @param p HashMap 
	 */
	public synchronized void setParams(java.util.HashMap p) {
		if (_numRunning>0) return;
		if (p!=null && p.containsKey("zeroOrderingCost")) {
			_run4ZeroOrderCost = ((Boolean) p.get("zeroOrderingCost")).booleanValue();
		}
	}

		
	/**
	 * main class method cannot run concurrently by multiple threads (will throw
	 * <CODE>OptimizerException</CODE> if such an attempt is made).
	 * @param f FunctionIntf must be of type sSTCnbin
	 * @return PairObjDouble Pair&lt;double[] args, double bestcost&gt; where the 
	 * args is an array holding the parameters (s*,S*,T*) yielding the bestcost 
	 * value
	 * @throws OptimizerException 
	 */
	public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (!(f instanceof sSTCnbin))
			throw new OptimizerException("sSTCnbinSimpleHeurOpt.minimize(f): "+
				                           "f must be tests.sic.sST.nbin.sSTCnbin");
		final Messenger mger = Messenger.getInstance();
		synchronized (this) {
			if (_numRunning>0) 
				throw new OptimizerException("sSTCnbinSimpleHeurOpt.minimize(f) is "+
					                           "already running "+
					                           "(by another thread on this object)");
			if (_pdclt==null) {
				mger.msg("sSTCnbinSimpleHeurOpt.minimize(f): connecting on "+_pdsrv+
					       " on port "+_pdport, 2);
				_pdclt = new PDBTExecInitedClt(_pdsrv, _pdport);
				try {
					_pdclt.submitInitCmd(new PDBTExecInitNoOpCmd());
					mger.msg("sSTCnbinSimpleHeurOpt.minimize(f): "+
						       "successfully sent init cmd", 
						       2);
				}
				catch (Exception e) {
					e.printStackTrace();
					throw new OptimizerException("sSTCnbinSimpleHeurOpt.mainimize(f): "+
						                           "clt failed to send empty init-cmd to "+
						                           "network");
				}
			}
			++_numRunning;
		}
		sSTCnbin sSTC = (sSTCnbin) f;
	
		mger.msg("sSTCnbinSimpleHeurOpt.minimize(f): running "+
			       "(r,nQ,T) policy optimization", 1);
		
		// 1. compute optimal params for the (r,nQ,T) policy
		final double[] x1 = getOptimalRnQTParams(sSTC);
		mger.msg("sSTCnbinSimpleHeurOpt.minimize(f): "+
			       "(r,nQ,T) policy optimization returns T*="+x1[2], 1);
		
		double[] h1 = new double[]{x1[0],x1[0]+x1[1],x1[2]};
		double c1 = sSTC.eval(h1, null);
		
		// 2. compute optimal params for the (r,nQ,T) policy of the system with 
		// Kr and Ko=0.
		double[] h2=null;
		double c2 = Double.POSITIVE_INFINITY;
		
		if (_run4ZeroOrderCost) {
			sSTCnbin sSTC2 = new sSTCnbin(sSTC._Kr, 0, sSTC._L, 
																		sSTC._lambda, sSTC._p_l, sSTC._h, sSTC._p);		
			mger.msg("sSTCnbinSimpleHeurOpt.minimize(f): running "+
							 "(r,nQ,T) policy optimization for Ko=0", 1);
			final double[] x2 = getOptimalRnQTParams(sSTC2);
			mger.msg("sSTCnbinSimpleHeurOpt.minimize(f): "+
							 "(r,nQ,T) policy optimization for zero ordering cost returns "+
							 "T*="+x2[2], 1);
			h2 = new double[]{x2[0],x2[0]+x2[1],x2[2]};
			c2 = sSTC.eval(h2, null);
		}
		// 3. decide which setting is best
		double[] x = c1 <= c2 ? h1 : h2;
		double c_best= Math.min(c1, c2);
		
		synchronized(this) {
			if (--_numRunning==0) {
				notify();  // let another thread know that it can proceed with 
					         // client termination
			}
		}
		
		return new PairObjDouble(x,c_best);
	}

	
	/**
	 * a thread may call this method at any point to terminate the sole connection
	 * to the workers' network. The method will terminate the connection as soon
	 * as there is no other thread running the <CODE>minimize(f)</CODE> method.
	 */
	public synchronized void terminateServerConnection() {
		while (_numRunning>0) {
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
			throw new Error("sSTCnbinSimpleHeurOpt.terminateServerConnection() "+
				              "failed?");
		}
	}
	
	
	/**
	 * find the optimal (r,nQ,T) policy for a system operating with the parameters
	 * contained in the given function, and return them.
	 * @param f sSTCnbin
	 * @return double[] optimal review params for the (r,nQ,T) policy
	 * @throws OptimizerException
	 */
	private synchronized double[] getOptimalRnQTParams(sSTCnbin f) 
		throws OptimizerException {
		RnQTCnbin rnqt = new RnQTCnbin(f._Kr, f._Ko, f._L, f._lambda, f._p_l,
		                                     f._h, f._p);
		RnQTCnbinOpt opter = new RnQTCnbinOpt(_pdsrv, _pdport, _batchSz, 
			                                    _epsT, _Tnot);
		// first set the _pdclt
		HashMap p = new HashMap();
		p.put("rnqtcnbinopt.pdclt", _pdclt);
		opter.setParams(p);
		PairObjDouble res = opter.minimize(rnqt);
		return ((double[])res.getArg());
	}
	
	
	/**
	 * invoke as 
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.sST.nbin.sSTCnbinSimpleHeurOpt 
	 * &lt;Kr&gt; 
	 * &lt;Ko&gt;
	 * &lt;L&gt;
	 * &lt;&lambda;&gt;
	 * &lt;p<sub>l</sub>&gt;
	 * &lt;h&gt;
	 * &lt;p&gt;
	 * [pdbtserverhostname(localhost)]
	 * [pdbtserverhostport(7891)]
	 * [epst(0.01)]
	 * [tnot(0.01)]
	 * [batchsize(24)]
	 * [dbglvl(0)]
	 * [run4zeroOrderCost(true)]
	 * </CODE>.
	 * @param args String[] 
	 */
	public static void main(String[] args) {
		final Messenger mger = Messenger.getInstance();
		// 1. parse inputs
		double Kr = Double.parseDouble(args[0]);
		double Ko = Double.parseDouble(args[1]);
		double L = Double.parseDouble(args[2]);
		double lambda = Double.parseDouble(args[3]);
		double p_l = Double.parseDouble(args[4]);
		double h = Double.parseDouble(args[5]);
		double p = Double.parseDouble(args[6]);
		String host = "localhost";
		if (args.length>7) host = args[7];
		int port = 7891;
		if (args.length>8) port = Integer.parseInt(args[8]);
		double epst = 0.01;
		if (args.length>9) epst = Double.parseDouble(args[9]);
		double tnot = 0.01;
		if (args.length>10) tnot = Double.parseDouble(args[10]);
		int bsize = 24;
		if (args.length>11) bsize = Integer.parseInt(args[11]);
		int dbglvl = 0;
		if (args.length>12) {
			dbglvl = Integer.parseInt(args[12]);
		}
		mger.setDebugLevel(dbglvl);
		HashMap params = new HashMap();
		if (args.length>13) {
			boolean run4ZeroOrderCost = Boolean.parseBoolean(args[13]);
			params.put("zeroOrderingCost", new Boolean(run4ZeroOrderCost));
		}
		// 2. create function
		sSTCnbin f = new sSTCnbin(Kr,Ko,L,lambda,p_l,h,p);
		long start = System.currentTimeMillis();
		// 3. optimize function
		sSTCnbinSimpleHeurOpt ropter = 
			new sSTCnbinSimpleHeurOpt(host, port, epst, tnot, bsize);
		ropter.setParams(params);
		try {
			PairObjDouble result = ropter.minimize(f);
			long dur = System.currentTimeMillis()-start;
			double[] x = (double[])result.getArg();
			double c = result.getDouble();
			System.out.println("s*="+x[0]+" S*="+x[1]+" T*="+x[2]+" ==> C*="+c);
			System.out.println("run-time="+dur+" msecs");
			ropter.terminateServerConnection();			
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}

