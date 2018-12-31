/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tests.sic;

import java.io.IOException;
import java.io.Serializable;
import parallel.TaskObject;
import parallel.distributed.FailedReply;
import parallel.distributed.PDBTExecInitedClt;
import popt4jlib.FunctionIntf;
import popt4jlib.OptimizerException;
import popt4jlib.OptimizerIntf;
import utils.PairObjDouble;
import utils.PairObjTwoDouble;

/**
 * class implements an optimizer over ALL three variables of the (R,nQ,T)
 * policy, namely the reorder point R, the batch size Q and the review period T.
 * The system being optimized faces stochastic demands 
 * as described in the class <CODE>RnQTCnorm</CODE>. The solution found is 
 * guaranteed to be the global optimum. The optimizer is a parallel/distributed
 * method, submitting tasks to optimize over the first two variables, for fixed 
 * review period T, where the review period T is increased from the Tmin value
 * it can take on (dictated by the non-negative demands requirement in any 
 * review period for normally distributed demand process) up to a point where
 * the lower-bound on the cost function (namely the cost function with Ko=0)
 * strictly exceeds the best known cost.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RnQTCnormOpt implements OptimizerIntf {
	/**
	 * default address for PDBTExecSingleCltWrkInitSrv
	 */
	private String _pdsrv = "localhost";  
	/**
	 * default port for PDBTExecSingleCltWrkInitSrv
	 */
	private int _pdport = 7891;
	
	private PDBTExecInitedClt _pdclt;
	
	private final double _epsT;
	private final double _epsQ;
	private final double _epsR;
	
	/**
	 * by default, 8 tasks to be submitted each time to be processed in parallel
	 */
	private final int _batchSz;
	
	private int _numRunning = 0;

	
	/**
	 * sole public constructor.
	 * @param server String
	 * @param port int
	 * @param batchSz int &gt;0; default 8
	 * @param epsT double &gt;0; default 0.01
	 * @param epsQ double &gt;0; default 0.1
	 * @param epsR double &gt;0; default 0.0001
	 */
	public RnQTCnormOpt(String server, int port, int batchSz, 
		                  double epsT, double epsQ, double epsR) {
		_pdsrv = server;
		_pdport = port;
		_batchSz = (batchSz>0) ? batchSz : 8;
		_epsT = epsT>0 ? epsT : 0.01;
		_epsQ = epsQ>0 ? epsQ : 0.1;
		_epsR = epsR>0 ? epsR : 1.e-4;
	}
	
	
	/**
	 * main class method.
	 * @param f
	 * @return
	 * @throws OptimizerException 
	 */
	public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (!(f instanceof RnQTCnorm))
			throw new OptimizerException("RnQTCnormOpt.minimize(f): f must be "+
				                           "function of type tests.sic.RnQTCnorm");
		synchronized (this) {
			if (_pdclt==null) _pdclt = new PDBTExecInitedClt(_pdsrv, _pdport);
			++_numRunning;
		}
		RnQTCnorm rnqtc = (RnQTCnorm) f;
		
		double Tmin = Math.pow((3.5*rnqtc._sigma/rnqtc._mi),2.0);
		double c_cur_best = Double.MAX_VALUE;
		double lb_cur = 0;
		
		double r_star=Double.NaN;
		double q_star = Double.NaN;
		double t_star = Double.NaN;
		
		double T = Tmin;
		
		boolean done = false;
		
		while (!done) {
			// 1. prepare batch
			TaskObject[] batch = new TaskObject[_batchSz];
			for (int i=0; i<_batchSz; i++) {
				T += _epsT;
				batch[i] = new RnQTCnormFixedTOptTask(rnqtc,T,_epsQ,0,_epsR,c_cur_best);
			}
			try {
				Object[] res = _pdclt.submitWorkFromSameHost(batch);
				for (int i=0; i<res.length; i++) {
					RnQTCnormFixedTOpterResult ri = (RnQTCnormFixedTOpterResult) res[i];
					if (Double.compare(ri._LB, c_cur_best)>0) {  // done!
						done = true;
					}
					if (Double.compare(ri._C, c_cur_best)<0) {
						r_star = ri._R;
						q_star = ri._Q;
						t_star = ri._T;
						c_cur_best = ri._C;
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new OptimizerException("RnQTCnormOpt.minimize(): failed to "+
					                           "submit tasks/process/get back results");
			}
		}
		synchronized(this) {
			if (--_numRunning==0) {
				try {
					_pdclt.terminateConnection();
				}
				catch (Exception e) {
					e.printStackTrace();
					throw new Error("RnQTCnormOpt.minimize(): terminateConnection() "+
						              "failed?");
				}
			}
		}
		double[] x = new double[]{r_star,q_star,t_star};
		return new PairObjDouble(x,c_cur_best);
	}
	
	
	/**
	 * invoke as 
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.RnQTCnormOpt 
	 * &lt;Kr&gt; 
	 * &lt;Ko&gt;
	 * &lt;L&gt;
	 * &lt;&mu;&gt;
	 * &lt;&sigma;&gt;
	 * &lt;h&gt;
	 * &lt;p&gt;
	 * [pdbtserverhostname(localhost)]
	 * [pdbtserverhostport(7981)]
	 * [epsq(0.1)]
	 * [minq(0)]
	 * [epst(0.01)]
	 * [epss(1.e-4)]
	 * [batchsize(8)]
	 * </CODE>.
	 * @param args String[] 
	 */
	public static void main(String[] args) {
		// 1. parse inputs
		double Kr = Double.parseDouble(args[0]);
		double Ko = Double.parseDouble(args[1]);
		double L = Double.parseDouble(args[2]);
		double mi = Double.parseDouble(args[3]);
		double sigma = Double.parseDouble(args[4]);
		double h = Double.parseDouble(args[5]);
		double p = Double.parseDouble(args[6]);
		String host = "localhost";
		if (args.length>7) host = args[7];
		int port = 7981;
		if (args.length>8) port = Integer.parseInt(args[8]);
		double epsq = 0.1;
		if (args.length>9) epsq = Double.parseDouble(args[9]);
		double minq = 0;
		if (args.length>10) minq = Double.parseDouble(args[10]);
		double epst = 0.01;
		if (args.length>11) epst = Double.parseDouble(args[11]);
		double epss = 1.e-4;
		if (args.length>12) epss = Double.parseDouble(args[12]);
		int bsize = 8;
		if (args.length>13) bsize = Integer.parseInt(args[13]);
		
		// 2. create function
		RnQTCnorm f = new RnQTCnorm(Kr,Ko,L,mi,sigma,h,p);

		long start = System.currentTimeMillis();
		// 3. optimize function
		RnQTCnormOpt ropter = new RnQTCnormOpt(host, port, bsize, epst, epsq, epss);
		try {
			PairObjDouble result = ropter.minimize(f);
			long dur = System.currentTimeMillis()-start;
			double[] x = (double[])result.getArg();
			double c = result.getDouble();
			System.out.println("R*="+x[0]+" Q*="+x[1]+" T*="+x[2]+" ==> C*="+c);
			System.out.println("run-time="+dur+" msecs");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}


/**
 * auxiliary class encapsulating the notion of optimizing an 
 * <CODE>RnQTCnorm</CODE> function, with a fixed review period T. NOT part of 
 * the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class RnQTCnormFixedTOptTask implements TaskObject {
	private RnQTCnorm _f;
	private double _T;
	private double _epsQ;
	private double _qnot;
	private double _epsR;
	private double _curBest;
	
	public RnQTCnormFixedTOptTask(RnQTCnorm f, 
		                            double T, double epsQ, double qnot, double epsR,
																double curBest) {
		_f = f;
		_T = T;
		_epsQ = epsQ;
		_qnot = qnot;
		_epsR = epsR;
		_curBest = curBest;
	}
	
	public Serializable run() {
		RnQTCnormFixedTOpt opter = 
			new RnQTCnormFixedTOpt(_T, _epsQ, _qnot, _epsR, _curBest);
		RnQTCnormFixedTOpterResult res = null;
		try {
			PairObjTwoDouble p = opter.minimize(_f);
			double[] x = (double[]) p.getArg();
			res = new RnQTCnormFixedTOpterResult(_T, x[0], x[1], 
				                                   p.getDouble(), p.getSecondDouble());
		}
		catch (Exception e) {
			e.printStackTrace();
			return new FailedReply();
		}
		finally {
			return res;
		}
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
 * auxiliary class that is essentially just an immutable struct, holding 5 
 * double values.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class RnQTCnormFixedTOpterResult implements Serializable {
	public final double _T;
	public final double _R;
	public final double _Q;
	public final double _C;
	public final double _LB;
	
	public RnQTCnormFixedTOpterResult(double T, double R, double Q, 
		                                double c, double lb) {
		_T = T;
		_Q = Q;
		_R = R;
		_C = c;
		_LB = lb;
	}
}

