package tests.sic.sST.norm;

import popt4jlib.FunctionIntf;
import popt4jlib.OptimizerException;
import popt4jlib.OptimizerIntf;
import parallel.distributed.PDBTExecInitNoOpCmd;
import parallel.distributed.PDBTExecInitedClt;
import utils.PairObjDouble;
import utils.Messenger;
import tests.sic.rnqt.norm.*;
import java.util.HashMap;


/**
 * class computes an approximate solution to the (s,S,T) policy optimization 
 * under normal demands, by computing the optimal (r,nQ,T) policy parameters
 * r*,Q*, and T* using the algorithm implemented in class 
 * <CODE>tests.sic.rnqt.norm.RnQTCnormOpt</CODE> and then setting s=r*,
 * S=r*+Q* and T=T*.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class sSTCnormSimpleHeurOpt implements OptimizerIntf {
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
	
	private final double _epss;
	private final double _qnot;

	/**
	 * by default, 24 tasks to be submitted each time to be processed in parallel
	 */
	private final int _batchSz;
	
	private int _numRunning = 0;

	
	/**
	 * sole public constructor.
	 * @param server String; default localhost
	 * @param port int; default 7891
	 * @param batchSz int &gt;0; default 24
	 * @param epsT double &gt;0; default 0.01
	 * @param epss double &gt;0 default 1.0
	 * @param qnot double &ge;0 default 1.e-6
	 */
	public sSTCnormSimpleHeurOpt(String server, int port, int batchSz, 
		                           double epsT, double epss, double qnot) {
		if (server==null || server.length()==0) _pdsrv = "localhost";
		else _pdsrv = server;
		if (port>1024) _pdport = port;
		else _pdport = 7891;
		_batchSz = (batchSz>0) ? batchSz : 24;
		_epsT = epsT>0 ? epsT : 0.01;
		_epss = epss>0 ? epss : 1.0;
		_qnot = qnot>=0 ? qnot : 1.e-6;		
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
	 * @param f FunctionIntf must be of type sSTCnorm
	 * @return PairObjDouble Pair&lt;double[] args, double bestcost&gt; where the 
	 * args is an array holding the parameters (s*,S*,T*) yielding the bestcost 
	 * value
	 * @throws OptimizerException 
	 */
	public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (!(f instanceof sSTCnorm))
			throw new OptimizerException("sSTCnormSimpleHeurOpt.minimize(f): f must "+
				                           "be of type tests.sic.sST.norm.sSTCnorm");
		Messenger mger = Messenger.getInstance();
		synchronized (this) {
			if (_pdclt==null) {
				mger.msg("sSTCnormSimpleHeurOpt.minimize(f): connecting on "+_pdsrv+
					       " on port "+_pdport, 2);
				_pdclt = new PDBTExecInitedClt(_pdsrv, _pdport);
				try {
					_pdclt.submitInitCmd(new PDBTExecInitNoOpCmd());
					mger.msg("sSTCnormSimpleHeurOpt.minimize(f): successfully sent init "+
						       "cmd", 
						       2);
				}
				catch (Exception e) {
					e.printStackTrace();
					throw new OptimizerException("sSTCnormSimpleHeurOpt.mainimize(f): "+
						                           "clt failed to send empty init-cmd to "+
						                           "network");
				}
			}
			++_numRunning;
		}
		sSTCnorm sSTC = (sSTCnorm) f;
		
		double s_star=Double.NaN;
		double S_star = Double.NaN;
		double t_star = Double.NaN;
		
		mger.msg("sSTCnormSimpleHeurOpt.minimize(): running "+
			       "(r,nQ,T) policy optimization", 1);
		// compute the optimal (r,nQ,T) policy params
		final double[] rnqt = getOptimalRnQTReviewParams(sSTC);
		mger.msg("sSTCnormSimpleHeurOpt.minimize(): "+
			       "(r,nQ,T) policy optimization returns T*="+rnqt[2], 1);
		final double c = sSTC.eval(rnqt, null);
		synchronized(this) {
			if (--_numRunning==0) {
				notify();  // let another thread know that it can proceed with 
					         // client termination
			}
		}
		return new PairObjDouble(rnqt, c);
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
			throw new Error("sSTCnormFastHeurOpt.terminateServerConnection() "+
				              "failed?");
		}
	}
	
	
	/**
	 * find the optimal (r,nQ,T) policy for a system operating with the 
	 * parameters contained in the given function.
	 * @param f sSTCnorm
	 * @return double[] optimal (r,nQ,T) policy parameters
	 * @throws OptimizerException
	 */
	private synchronized double[] getOptimalRnQTReviewParams(sSTCnorm f) 
		throws OptimizerException {
		RnQTCnorm rnqt = new RnQTCnorm(f._Kr, f._Ko, f._L, f._mi,f._sigma,
		                                     f._h, f._p);
		// epsQ is set to the same value as epss
		RnQTCnormOpt opter = new RnQTCnormOpt(_pdsrv, _pdport, _batchSz, 
			                                    _epsT, _epss, _epss);
		// first set the _pdclt
		HashMap p = new HashMap();
		p.put("rnqtcnormopt.pdclt", _pdclt);
		opter.setParams(p);
		PairObjDouble res = opter.minimize(rnqt);
		return ((double[])res.getArg());
	}
	
	
	/**
	 * invoke as 
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.sST.norm.sSTCnormSimpleHeurOpt 
	 * &lt;Kr&gt; 
	 * &lt;Ko&gt;
	 * &lt;L&gt;
	 * &lt;&mu;&gt;
	 * &lt;&sigma;&gt;
	 * &lt;h&gt;
	 * &lt;p&gt;
	 * [p2(0)]
	 * [pdbtserverhostname(localhost)]
	 * [pdbtserverhostport(7891)]
	 * [epst(0.01)]
	 * [epss(1.e-4)]
	 * [qnot(1.e-6)]
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
		double mi = Double.parseDouble(args[3]);
		double sigma = Double.parseDouble(args[4]);
		double h = Double.parseDouble(args[5]);
		double p = Double.parseDouble(args[6]);
		double p2 = 0;
		if (args.length>7) p2 = Double.parseDouble(args[7]);
		String host = "localhost";
		if (args.length>8) host = args[8];
		int port = 7891;
		if (args.length>9) port = Integer.parseInt(args[9]);
		double epst = 0.01;
		if (args.length>10) epst = Double.parseDouble(args[10]);
		double epss = 1.e-4;
		if (args.length>11) epss = Double.parseDouble(args[11]);
		double qnot = 1.e-6;
		if (args.length>12) qnot = Double.parseDouble(args[12]);
		int bsize = 24;
		if (args.length>13) bsize = Integer.parseInt(args[13]);
		int dbglvl = 0;
		if (args.length>14) {
			dbglvl = Integer.parseInt(args[14]);
		}
		mger.setDebugLevel(dbglvl);
		// 2. create function
		sSTCnorm f = new sSTCnorm(Kr,Ko,L,mi,sigma,h,p,p2);
		long start = System.currentTimeMillis();
		// 3. optimize function
		sSTCnormSimpleHeurOpt ropter = 
			new sSTCnormSimpleHeurOpt(host, port, bsize, epst, epss, qnot);
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

