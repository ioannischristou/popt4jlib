package tests.sic.sST.poisson;

import popt4jlib.FunctionIntf;
import popt4jlib.OptimizerException;
import popt4jlib.OptimizerIntf;
import parallel.distributed.PDBTExecInitNoOpCmd;
import parallel.distributed.PDBTExecInitedClt;
import utils.PairObjDouble;
import utils.Messenger;
import tests.sic.rnqt.poisson.*;
import java.util.HashMap;


/**
 * class computes an approximate solution to the (s,S,T) policy optimization 
 * under Poisson demands by computing the optimal (r,nQ,T) policy parameters
 * r*,Q*, and T* -which is usually done much faster- and then setting s*=r*, and
 * S*=r*+Q*.
 * In fact, we also compute the (R,nQ,T) optimal policy for a system with review
 * cost equal to Kr and zero ordering cost and for the resulting review period 
 * T' we compute similarly the optimal s(T') and S(T'), and compare with the 
 * above computed cost, and we return the winner.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class sSTCpoissonSimpleHeurOpt implements OptimizerIntf {
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
	 * by default, 24 tasks to be submitted each time to be processed in parallel
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
	public sSTCpoissonSimpleHeurOpt(String pdsrv, int pdport, 
		                           double epst, double tnot, 
															 int batchSz) {
		_pdsrv = pdsrv;
		_pdport = pdport;
		_epsT = epst;
		_Tnot = tnot;
		_batchSz = batchSz>0 ? batchSz : 24;
	}
	

	/**
	 * no-op.
	 * @param p HashMap unused 
	 */
	public void setParams(java.util.HashMap p) {
		// no-op.
	}

		
	/**
	 * main class method.
	 * @param f FunctionIntf must be of type sSTCpoisson
	 * @return PairObjDouble Pair&lt;double[] args, double bestcost&gt; where the 
	 * args is an array holding the parameters (s*,S*,T*) yielding the bestcost 
	 * value
	 * @throws OptimizerException 
	 */
	public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (!(f instanceof sSTCpoisson))
			throw new OptimizerException("sSTCpoissonSimpleHeurOpt.minimize(f): "+
				                           "f must be tests.sic.sST.nbin.sSTCpoisson");
		final Messenger mger = Messenger.getInstance();
		synchronized (this) {
			if (_pdclt==null) {
				mger.msg("sSTCpoissonSimpleHeurOpt.minimize(f): connecting on "+_pdsrv+
					       " on port "+_pdport, 2);
				_pdclt = new PDBTExecInitedClt(_pdsrv, _pdport);
				try {
					_pdclt.submitInitCmd(new PDBTExecInitNoOpCmd());
					mger.msg("sSTCpoissonSimpleHeurOpt.minimize(f): "+
						       "successfully sent init cmd", 
						       2);
				}
				catch (Exception e) {
					e.printStackTrace();
					throw new OptimizerException("sSTCpoissonSimpleHeurOpt.mainimize(f):"+
						                           " clt failed to send empty init-cmd to "+
						                           "network");
				}
			}
			++_numRunning;
		}
		sSTCpoisson sSTC = (sSTCpoisson) f;
	
		mger.msg("sSTCpoissonSimpleHeurOpt.minimize(f): running "+
			       "(r,nQ,T) policy optimization", 1);
		
		// 1. compute optimal params for the (r,nQ,T) policy
		final double[] x1 = getOptimalRnQTParams(sSTC);
		mger.msg("sSTCpoissonSimpleHeurOpt.minimize(f): "+
			       "(r,nQ,T) policy optimization returns T*="+x1[2], 1);
		
		double[] h1 = new double[]{x1[0],x1[0]+x1[1],x1[2]};
		double c1 = sSTC.eval(h1, null);
		
		// 2. compute optimal params for the (r,nQ,T) policy of the system with 
		// Kr and Ko=0.
		sSTCpoisson sSTC2 = new sSTCpoisson(sSTC._Kr, 0, sSTC._L, 
			                                  sSTC._lambda, 
			                                  sSTC._h, sSTC._p, sSTC._p2);		
		mger.msg("sSTCpoissonSimpleHeurOpt.minimize(f): running "+
			       "(r,nQ,T) policy optimization for Ko=0", 1);
		final double[] x2 = getOptimalRnQTParams(sSTC2);
		mger.msg("sSTCpoissonSimpleHeurOpt.minimize(f): "+
			       "(r,nQ,T) policy optimization for zero ordering cost returns "+
			       "T*="+x2[2], 1);
		double[] h2 = new double[]{x2[0],x2[0]+x2[1],x2[2]};
		double c2 = sSTC.eval(h2, null);
		
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
			throw new Error("sSTCpoissonSimpleHeurOpt.terminateServerConnection() "+
				              "failed?");
		}
	}
	
	
	/**
	 * find the optimal (r,nQ,T) policy for a system operating with the parameters
	 * contained in the given function, and return them.
	 * @param f sSTCpoisson
	 * @return double[] optimal review params for the (r,nQ,T) policy
	 * @throws OptimizerException
	 */
	private synchronized double[] getOptimalRnQTParams(sSTCpoisson f) 
		throws OptimizerException {
		RnQTCpoisson rnqt = new RnQTCpoisson(f._Kr, f._Ko, f._L, f._lambda,
		                                     f._h, f._p, f._p2);
		RnQTCpoissonOpt opter = new RnQTCpoissonOpt(_pdsrv, _pdport, _batchSz, 
			                                          _epsT, _Tnot);
		// first set the _pdclt
		HashMap p = new HashMap();
		p.put("rnqtcpoissonopt.pdclt", _pdclt);
		opter.setParams(p);
		PairObjDouble res = opter.minimize(rnqt);
		return ((double[])res.getArg());
	}
	
	
	/**
	 * invoke as 
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.sST.nbin.sSTCpoissonSimpleHeurOpt 
	 * &lt;Kr&gt; 
	 * &lt;Ko&gt;
	 * &lt;L&gt;
	 * &lt;&lambda;&gt;
	 * &lt;h&gt;
	 * &lt;p&gt;
	 * [p2(0)]
	 * [pdbtserverhostname(localhost)]
	 * [pdbtserverhostport(7891)]
	 * [epst(0.01)]
	 * [tnot(0.01)]
	 * [batchsize(24)]
	 * [dbglvl(0)]
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
		double h = Double.parseDouble(args[4]);
		double p = Double.parseDouble(args[5]);
		double p2 = 0;
		if (args.length>6) p2 = Double.parseDouble(args[6]);
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
		// 2. create function
		sSTCpoisson f = new sSTCpoisson(Kr,Ko,L,lambda,h,p,p2);
		long start = System.currentTimeMillis();
		// 3. optimize function
		sSTCpoissonSimpleHeurOpt ropter = 
			new sSTCpoissonSimpleHeurOpt(host, port, epst, tnot, bsize);
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

