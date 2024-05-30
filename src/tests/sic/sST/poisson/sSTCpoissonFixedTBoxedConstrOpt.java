package tests.sic.sST.poisson;

import parallel.distributed.PDBTExecInitedClt;
import popt4jlib.OptimizerIntf;
import popt4jlib.FunctionEvaluationTask;
import popt4jlib.FunctionIntf;
import popt4jlib.OptimizerException;
import utils.Messenger;
import utils.PairObjDouble;
import java.util.ArrayList;
import java.util.HashMap;
import parallel.distributed.PDBTExecNoOpCmd;


/**
 * class implements an optimizer over the first two variables of the (s,S,T)
 * policy, namely the reorder point s and the order-up-to S variables, for given
 * (fixed) review period T, subject to the constraints S &ge; s, 
 * s &isin; [smin, smax], and S &isin; [Smin, Smax], using an exhaustive search 
 * on both variables with step size &epsilon;<sub>s</sub>. The system being 
 * optimized faces stochastic demands as described in the class 
 * <CODE>sSTCpoisson</CODE>. 
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2024</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class sSTCpoissonFixedTBoxedConstrOpt implements OptimizerIntf {
	private double _T;
	
	private final double _smin;
	private final double _smax;
	private final double _Smin;
	private final double _Smax;
	private final double _epss = 1.0;
	
	private transient PDBTExecInitedClt _pdclt;  // distribute the computations

	
		/**
	 * public constructor does not allow for distributed computation.
	 * @param T double the given review period
	 * @param smin double the lower bound on s
	 * @param smax double the upper bound on s
	 * @param Smin double the lower bound on S
	 * @param Smax double the upper bound on S
	 */
	public sSTCpoissonFixedTBoxedConstrOpt(double T, 
		                                  double smin, double smax,
																			double Smin, double Smax) {
		_T = T;
		_smin = smin;
		_smax = smax;
		_Smin= Smin;
		_Smax = Smax;
	}

	
	/**
	 * public constructor allows specifying client to the network for distributed
	 * computation.
	 * @param T double the given review period
	 * @param smin double the lower bound on s
	 * @param smax double the upper bound on s
	 * @param Smin double the lower bound on S
	 * @param Smax double the upper bound on S
	 * @param pdclt PDBTExecInitedClt the client to use to send function
	 * evaluation tasks for execution to the network
	 */
	public sSTCpoissonFixedTBoxedConstrOpt(double T, 
		                                     double smin, double smax,
											    							 double Smin, double Smax,
																			   PDBTExecInitedClt pdclt) {
		_T = T;
		_smin = smin;
		_smax = smax;
		_Smin= Smin;
		_Smax = Smax;
		_pdclt = pdclt;
	}
	
		
	/**
	 * no-op.
	 * @param p HashMap unused
	 */
	public void setParams(java.util.HashMap p) {
		// no-op
	}


	/**
	 * obtains the global minimum over all s and all S &ge; s of the function 
	 * <CODE>sSTCpoisson(s,S,T)</CODE> in this package for given T and also given
	 * the box constraints of s and S. Implements an exhaustive search procedure
	 * on the 2D grid specified by the variables' box constraints as well as the
	 * constraint S &ge; s.
	 * @param func sSTCpoisson instance
	 * @return PairObjDouble  // Pair&lt;double[] x, double cb&gt; 
	 *                        // where x[0] is s_opt, and x[1] is S_opt for 
	 *                        // _T.
	 * @throws OptimizerException 
	 */
	public PairObjDouble minimize(FunctionIntf func) 
		throws OptimizerException {
		if (!(func instanceof sSTCpoisson))
			throw new OptimizerException(
				          "sSTCpoissonFixedTBoxedConstrOpt.minimize(f): "+
				          "function passed in must be sSTCpoisson");
		sSTCpoisson f = (sSTCpoisson) func;
		final Messenger mger = Messenger.getInstance();
		mger.msg("sSTCpoissonFixedTBoxedConstrOpt.minimize(): start",2);
		try {
			// create all the function evaluation tasks needed and send them over to
			// the network for execution
			ArrayList tasks = new ArrayList();
			double curs = _smin;
			while (curs <= _smax) {
				double curS = curs+_epss;
				if (curS < _Smin) curS = _Smin;
				while (curS <= _Smax) {
					double[] arg = new double[]{curs, curS, _T};
					FET2 ft = new FET2(f, arg);
					tasks.add(ft);
					curS += _epss;
				}
				curs += _epss;
			}
			FET2[] ts = new FET2[tasks.size()];
			for (int i=0; i<ts.length; i++) ts[i] = (FET2) tasks.get(i);
			mger.msg("sSTCpoissonFixedTBoxedConstrOpt.minimize(): preparing "+
				        ts.length+" tasks", 2);
			Object[] results = _pdclt != null ?
				                   _pdclt.submitWorkFromSameHost(ts) : 
				                   runTasks(ts);
			mger.msg("sSTCpoissonFixedTBoxedConstrOpt.minimize(): got results.", 2);
			// find the best result
			double best_val = Double.POSITIVE_INFINITY;
			double[] best_arg = null;
			for (int i=0; i<results.length; i++) {
				try {
					FET2 ri = (FET2) results[i];
					if (ri.getObjValue() < best_val) {
						best_val = ri.getObjValue();
						best_arg = (double[]) ri.getArg();
					}
				}
				catch (ClassCastException e) {
					e.printStackTrace();  // ignore non-silently
				}
			}
			return new PairObjDouble(best_arg, best_val);
		}
		catch (Exception e) {
			throw new OptimizerException("minimize() threw "+e.toString());
		}
	}
	
	
	private Object[] runTasks(FET2[] tasks) {
		for (int i=0; i<tasks.length; i++) {
			tasks[i].run();
		}
		return tasks;
	}
	
	
	/**
	 * invoke as:
	 * <CODE>
	 * java -cp &lt;classpath&gt; 
	 * tests.sic.sST.poisson.sSTCpoissonFixedTBoxedConstrOpt
	 * &lt;T&gt; &lt;Kr&gt; &lt;Ko&gt; &lt;L&gt; &lt;&lambda;&gt;
	 * &lt;h&gt; &lt;p&gt; &lt;p2(0)&gt; 
	 * &lt;smin&gt; &lt;smax&gt; &lt;Smin&gt; &lt;Smax&gt; 
	 * [epss(1.0) unused] 
	 * [pdhost(localhost)]
	 * [pdport(7891)]
	 * [dbglvl(0)]
	 * </CODE>.
	 * @param args 
	 */
	public static void main(String[] args) {
		double T = Double.parseDouble(args[0]);
		double Kr = Double.parseDouble(args[1]);
		double Ko = Double.parseDouble(args[2]);
		double L = Double.parseDouble(args[3]);
		double lambda = Double.parseDouble(args[4]);
		double h = Double.parseDouble(args[5]);
		double p = Double.parseDouble(args[6]);
		double p2 = Double.parseDouble(args[7]);
		double smin = Double.parseDouble(args[8]);
		double smax = Double.parseDouble(args[9]);
		double Smin = Double.parseDouble(args[10]);
		double Smax = Double.parseDouble(args[11]);
		// double epss = args.length>12 ? Double.parseDouble(args[12]) : 1.0;
		String pdhost = "localhost";
		if (args.length>13) pdhost = args[13];
		int pdport = 7891;
		if (args.length>14) pdport = Integer.parseInt(args[14]);
		int dbglvl = args.length>15 ? Integer.parseInt(args[15]) : 0;
		Messenger.getInstance().setDebugLevel(dbglvl);
		final long st = System.currentTimeMillis();
		final sSTCpoisson f = new sSTCpoisson(Kr,Ko,L,lambda,h,p,p2);
		PDBTExecInitedClt pdclt = new PDBTExecInitedClt(pdhost, pdport);
		try {
			pdclt.submitInitCmd(new PDBTExecNoOpCmd());
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		final sSTCpoissonFixedTBoxedConstrOpt opt2D =
			new sSTCpoissonFixedTBoxedConstrOpt(T, smin, smax, Smin, Smax, pdclt);
		try {
			PairObjDouble bp = opt2D.minimize(f);
			double[] xbest = (double[]) bp.getArg();
			double ybest = bp.getDouble();
			final long dur = System.currentTimeMillis()-st;
			System.out.println("s*="+xbest[0]+" S*="+xbest[1]+" C*("+T+")="+ybest+
				                 " in "+dur+" msecs.");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}


/**
 * auxiliary class not part of the public API.
 */
final class FET2 extends FunctionEvaluationTask {
	/**
	 * sole public constructor.
	 * @param f FunctionIntf
	 * @param arg double[] 
	 */
	public FET2(FunctionIntf f, double[] arg) {
		super(f, arg, new HashMap());
	}
	
	public double[] getArg() {
		return (double[]) super.getArg();
	}
}
