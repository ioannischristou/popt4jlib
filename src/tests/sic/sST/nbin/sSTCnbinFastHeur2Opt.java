package tests.sic.sST.nbin;

import popt4jlib.FunctionEvaluationTask;
import popt4jlib.FunctionIntf;
import popt4jlib.FunctionBaseStatic;
import popt4jlib.OptimizerException;
import popt4jlib.OptimizerIntf;
import parallel.TaskObject;
import parallel.distributed.PDBTExecInitedClt;
import parallel.distributed.PDBTExecInitNoOpCmd;
import utils.PairObjDouble;
import utils.Messenger;
import tests.sic.rnqt.nbin.*;
import java.util.ArrayList;
import java.util.HashMap;
// imports below needed for the plotting of the search
import java.util.Locale;
import java.awt.BorderLayout;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.text.NumberFormat;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


/**
 * class computes an approximate solution to the (s,S,T) policy optimization 
 * under compound Poisson demands expressed by the Negative Binomial 
 * distribution, by computing near-optimal (r,nQ,T) policy parameters
 * r*,Q*, and T* according to the heuristic implemented in (parallel) class 
 * <CODE>tests.sic.rnqt.nbin.RnQTCnbinHeurPOpt</CODE> and then computing the 
 * best (s(r*),S(r*+Q*)) parameters for t close to T* and picking the best set 
 * of parameters. The difference from class <CODE>sSTCnbinFastHeurOpt</CODE> is
 * that while the former class computes the (s*,S*) parameters for a given T*
 * using the Zheng-Federgruen algorithm, this class simply performs a grid 
 * search around the points (r*, r*+Q*). This can result in tens or hundreds of
 * thousands of function evaluation tasks required.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2024</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class sSTCnbinFastHeur2Opt implements OptimizerIntf {

	/**
	 * compile-time constant that is used to speed up the search for the optimal
	 * (R,nQ,T) policy parameters.
	 */
	private final static double _EPST_MULT = 3.0;

	
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
	
	private final double _deltaT;
	
	private final double _epss = 1.0;
	private final double _deltas;

	
	/**
	 * by default, 24 tasks to be submitted each time to be processed in parallel.
	 */
	private final int _batchSz;
	
	private int _numRunning = 0;

	/**
	 * whether there will be a loop around the approximate (r,nQ,T) solution with
	 * zero ordering cost Ko=0.
	 */
	private final boolean _run4ZeroOrderCost;
	
	private ArrayList _tis;      // used for visualization purposes
	private ArrayList _ctis;     // again, for visualization purposes only

	
	/**
	 * sole public constructor.
	 * @param server String; default "localhost"
	 * @param port int; default 7891
	 * @param batchSz int &gt;0; default 24
	 * @param epsT double &gt;0; default 0.01
	 * @param deltaT double &gt;0 default 0.05
	 * @param epss double unused (always set to 1.0)
	 * @param deltas double &ge;0 default 5.0
	 * @param run4zeroordercost boolean default true
	 */
	public sSTCnbinFastHeur2Opt(String server, int port, int batchSz, 
		                          double epsT, double deltaT, 
															double epss, double deltas,
															boolean run4zeroordercost) {
		if (server==null || server.length()==0) _pdsrv = "localhost";
		else _pdsrv = server;
		if (port>1024) _pdport = port;
		else _pdport = 7891;
		_batchSz = (batchSz>0) ? batchSz : 24;
		_epsT = epsT>0 ? epsT : 0.01;
		_deltaT = deltaT>0 ? deltaT : 0.05;
		//_epss = epss>0 ? epss : 1.0;
		_deltas = deltas>=0 ? deltas : 5.0;
		_run4ZeroOrderCost = run4zeroordercost;
		
		_tis = new ArrayList();
		_ctis = new ArrayList();
	}
	

	/**
	 * no-op.
	 * @param p HashMap unused 
	 */
	public void setParams(java.util.HashMap p) {
		// no-op.
	}

		
	/**
	 * main class method, creates sets of function evaluations as tasks to be 
	 * submitted to the cluster, for more parallelization.
	 * @param f FunctionIntf must be of type sSTCnbin
	 * @return PairObjDouble Pair&lt;double[] args, double bestcost&gt; where the 
	 * args is an array holding the parameters (s*,S*,T*) yielding the bestcost 
	 * value
	 * @throws OptimizerException 
	 */
	public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (!(f instanceof sSTCnbin))
			throw new OptimizerException("sSTCnbinFastHeur2Opt.minimize(f): f must "+
				                           "be of type tests.sic.sST.nbin.sSTCnbin");
		long init_time = System.currentTimeMillis();
		Messenger mger = Messenger.getInstance();
		synchronized (this) {
			if (_numRunning>0) 
				throw new OptimizerException("sSTCnbinFastHeur2Opt.minimize(f) is "+
					                           "already running "+
					                           "(by another thread on this object)");
			if (_pdclt==null) {
				mger.msg("sSTCnbinFastHeur2Opt.minimize(f): connecting on "+_pdsrv+
					       " on port "+_pdport, 2);
				_pdclt = new PDBTExecInitedClt(_pdsrv, _pdport);
				try {
					_pdclt.submitInitCmd(new PDBTExecInitNoOpCmd());
					//_pdclt.submitInitCmd(new PDBTExecShowFuncEvalsOnExitCmd());
					mger.msg("sSTCnbinFastHeur2Opt.minimize(f): successfully sent init "+
						       "cmd", 
						       2);
				}
				catch (Exception e) {
					e.printStackTrace();
					throw new OptimizerException("sSTCnbinFastHeur2Opt.mainimize(f): "+
						                           "clt failed to send empty init-cmd to "+
						                           "network");
				}
			}
			_tis.clear();
			_ctis.clear();
			++_numRunning;
		}
		sSTCnbin sSTC = (sSTCnbin) f;
		
		double Tmin = _epsT;  // used to be zero
		
		double s_star=Double.NaN;
		double S_star = Double.NaN;
		double t_star = Double.NaN;
		
		long init_dur = System.currentTimeMillis()-init_time;
		mger.msg("sSTCnbinFastHeur2Opt.minimize(): init took "+init_dur+" msecs.",
			       0);
		
		mger.msg("sSTCnbinFastHeur2Opt.minimize(): running "+
			       "(r,nQ,T) policy optimization", 1);
		// first, compute the near-optimal T* for the (r,nQ,T) policy
		final double[] rnqt_arr = getNearOptimalRnQTPolicy(sSTC);
		final double t_rnqt = rnqt_arr[2];
		mger.msg("sSTCnbinFastHeur2Opt.minimize(): "+
			       "(r,nQ,T) policy optimization returns "+
			       "R*="+rnqt_arr[0]+", Q*="+rnqt_arr[1]+", T*="+rnqt_arr[2], 1);
		
		long st_time = System.currentTimeMillis();  // time the time it takes to 
		                                            // run the sSTCnbin func evals
		double T = Tmin >= rnqt_arr[2]-_deltaT ? Tmin : rnqt_arr[2]-_deltaT;
		double Tstart = T;
		
		final double Tmax = rnqt_arr[2] + _deltaT;
		
		double c_cur_best = Double.POSITIVE_INFINITY;
		
		ArrayList all_tasks = new ArrayList();
		// create all function evaluation tasks
		final double smin = rnqt_arr[0]-_deltas;
		final double smax = rnqt_arr[0]+_deltas;
		final double Smin = rnqt_arr[0]+rnqt_arr[1]-_deltas;
		final double Smax = rnqt_arr[0]+rnqt_arr[1]+_deltas;
		for(; T<Tmax; T+=_epsT) {
			for(double s=smin; s<=smax; s+=_epss) {
				for(double S=Smin; S<=Smax; S+=_epss) {
					if (S <= s) continue;  // don't include useless tasks
					all_tasks.add(new FET2(sSTC, new double[]{s,S,T}));
				}
			}
		}
		TaskObject[] tasks_arr = new TaskObject[all_tasks.size()];
		final int batchSz = all_tasks.size();
		for(int i=0; i<all_tasks.size(); i++) 
			tasks_arr[i] = (TaskObject) all_tasks.get(i);
		try {
			mger.msg("sSTCnbinFastHeur2Opt.minimize(): submit a batch of "+batchSz+
				       " tasks to network for period length from "+(Tstart+_epsT)+
				       " up to "+Tmax, 
				       1);
			if (tasks_arr.length > 0) {
				Object[] res = _pdclt.submitWorkFromSameHost(tasks_arr);
				mger.msg("sSTCnbinFastHeur2Opt.minimize(): got back results from " +
								 "batch execution.",1);
				for (int i=0; i<res.length; i++) {
					FET2 ri = 
						(FET2) res[i];
					synchronized(this) {
						if (i==0 ||
								((Double)_tis.get(_tis.size()-1)).doubleValue()<ri.getArg()[2]){
							_tis.add(new Double(ri.getArg()[2]));  // add to tis time-series
							_ctis.add(new Double(ri.getObjValue()));  // add to c(t) t-series
						}
						else {
							double cur_val_t = 
								((Double)_ctis.get(_ctis.size()-1)).doubleValue();
							if (ri.getObjValue() < cur_val_t) {
								_ctis.set(_ctis.size()-1, ri.getObjValue());
							} 
						}
					}
					if (ri.getObjValue() < c_cur_best) {
						s_star = ri.getArg()[0];
						S_star = ri.getArg()[1];
						t_star = ri.getArg()[2];
						c_cur_best = ri.getObjValue();
						mger.msg("sSTCnbinFastHeur2Opt.minimize(f): found new better soln "+
										 " @T="+t_star+", c="+c_cur_best, 1);
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new OptimizerException("sSTCnbinFastHeur2Opt.minimize(): failed "+
				                           "to submit tasks/process/get back result");
		}
		
		long dur1 = System.currentTimeMillis() - st_time;
		
		// find the optimal T for a system with review cost Kr and 0 order cost!
		// if _run4ZeroOrderCost is true of course
		if (_run4ZeroOrderCost) {
			sSTCnbin sSTC2 = new sSTCnbin(sSTC._Kr, 0, sSTC._L,
																		sSTC._lambda, sSTC._p_l, sSTC._h, sSTC._p);
			final double[] rnqt2 = getNearOptimalRnQTPolicy(sSTC2);
			
			st_time = System.currentTimeMillis();
			
			mger.msg("sSTCnbinFastHeur2Opt.minimize(Kr,0): "+
							 "(r,nQ,T) policy optimization returns T*="+rnqt2[2], 1);

			// do a search around s',S',T' for the best (s,S,T) parameters, making
			// sure we don't cover T's that have been covered already
			T = Tmin >= rnqt2[2]-_deltaT ? Tmin : rnqt2[2]-_deltaT;
			if (T<=Tmax && T>=Math.max(Tmin, t_rnqt-_deltaT)) {
				// new left-end of search area is within the already searched area
				T = Tmax;
			}
			Tstart = T;
			double Tmax2 = rnqt2[2] + _deltaT;
			if (Tmax2 <= Tmax && Tmax2>=Math.max(Tmin, t_rnqt-_deltaT)) {
				// Tmax2 falls within already searched area
				Tmax2 = Math.max(Tmin, t_rnqt-_deltaT);
			}
			mger.msg("sSTCnbinFastHeur2Opt.minimize(f): last search area is "+
							 "["+T+","+Tmax2+"]", 1);
	
			ArrayList all_tasks2 = new ArrayList();
			// create all function evaluation tasks
			final double smin2 = rnqt2[0]-_deltas;
			final double smax2 = rnqt2[0]+_deltas;
			final double Smin2 = rnqt2[0]+rnqt2[1]-_deltas;
			final double Smax2 = rnqt2[0]+rnqt2[1]+_deltas;
			for(; T<Tmax2; T+=_epsT) {
				for(double s=smin2; s<=smax2; s+=_epss) {
					for(double S=Smin2; S<=Smax2; S+=_epss) {
						if (S <= s) continue;  // don't include useless tasks
						all_tasks2.add(new FET2(sSTC, new double[]{s,S,T}));
					}
				}
			}
			TaskObject[] tasks_arr2 = new TaskObject[all_tasks2.size()];
			final int batchSz2 = all_tasks2.size();
			for(int i=0; i<all_tasks2.size(); i++) 
				tasks_arr2[i] = (TaskObject) all_tasks2.get(i);
			try {
				mger.msg("sSTCnbinFastHeur2Opt.minimize(): submit a batch of "+batchSz2+
								 " tasks to network for period length from "+(Tstart+_epsT)+
								 " up to "+Tmax2, 
								 1);
				if (tasks_arr2.length > 0) {
					Object[] res = _pdclt.submitWorkFromSameHost(tasks_arr2);
					mger.msg("sSTCnbinFastHeur2Opt.minimize(): got back results "+
									 "from entire batch execution.",1);
					for (int i=0; i<res.length; i++) {
						FET2 ri = 
							(FET2) res[i];
						synchronized(this) {
							if (i==0 ||
									((Double)_tis.get(_tis.size()-1)).doubleValue() < 
								  ri.getArg()[2]) {
								_tis.add(new Double(ri.getArg()[2]));  // add to tis time-series
								_ctis.add(new Double(ri.getObjValue()));  // add to c(t)  series
							}
							else {
								double cur_val_t = 
									((Double)_ctis.get(_ctis.size()-1)).doubleValue();
								if (ri.getObjValue() < cur_val_t) {
									_ctis.set(_ctis.size()-1, ri.getObjValue());
								} 
							}
						}
						if (ri.getObjValue() < c_cur_best) {
							s_star = ri.getArg()[0];
							S_star = ri.getArg()[1];
							t_star = ri.getArg()[2];
							c_cur_best = ri.getObjValue();
							mger.msg("sSTCnbinFastHeur2Opt.minimize(f): found better soln "+
											 " @T="+t_star+", c="+c_cur_best, 1);
						}
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new OptimizerException("sSTCnbinFastHeur2Opt.minimize(): failed "+
																		 "to submit tasks/process/get back result");
			}
		}
		else {
			dur1 = 0;  // st_time wasn't reset, don't use dur1
		}
		
		long dur = System.currentTimeMillis() - st_time + dur1;
		mger.msg("sSTCnbinFastHeur2Opt.minimize(): sSTC function evals took "+dur+
			       " msecs", 0);
		
		synchronized(this) {
			if (--_numRunning==0) {
				notify();  // let another thread know that it can proceed with 
					         // client termination
			}
		}
		double[] x = new double[]{s_star,S_star,t_star};
		return new PairObjDouble(x,c_cur_best);
	}
	
		
	/**
	 * get the time-series from the latest run.
	 * @return ArrayList[] first element is T-axis values, second is optimal 
	 * costs
	 */
	public synchronized ArrayList[] getLatestTimeSeries() {
		while (_numRunning>0) {
			try {
				wait();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		ArrayList[] result = new ArrayList[2];
		result[0] = new ArrayList(_tis);
		result[1] = new ArrayList(_ctis);
		return result;
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
			throw new Error("sSTCnbinFastHeur2Opt.terminateServerConnection() "+
				              "failed?");
		}
	}
	
	
	/**
	 * find a near-optimal (r,nQ,T) policy for a system operating with the 
	 * parameters contained in the given function, and return the optimal review 
	 * period T*. Uses the heuristic described in the CAIE paper by Christou,
	 * Skouri and Lagodimos (2020).
	 * @param f sSTCnorm
	 * @return double[] near-optimal parameters for the (r,nQ,T) policy
	 * @throws OptimizerException
	 */
	private synchronized double[] getNearOptimalRnQTPolicy(sSTCnbin f) 
		throws OptimizerException {
		RnQTCnbin rnqt = new RnQTCnbin(f._Kr, f._Ko, f._L, f._lambda ,f._p_l,
		                                     f._h, f._p);
		final double tnot = 0.0;  // this will cause the default tnot value 
		                          // to be used (currently at 0.01)
		final double epsT_rnqt = _EPST_MULT*_epsT;  // use 
		RnQTCnbinHeurPOpt opter = 
			new RnQTCnbinHeurPOpt(_pdsrv, _pdport, _batchSz, epsT_rnqt, tnot);
		// following code is needed with the heuristic optimization algorithm
		// first set the _pdclt
		HashMap p = new HashMap();
		p.put("rnqtcnbinheurpopt.pdclt", _pdclt);
		opter.setParams(p);
		PairObjDouble res = opter.minimize(rnqt);
		return ((double[])res.getArg());
	}
	
	
	/**
	 * invoke as 
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.sST.nbin.sSTCnbinFastHeur2Opt 
	 * &lt;Kr&gt; 
	 * &lt;Ko&gt;
	 * &lt;L&gt;
	 * &lt;&lambda;&gt;
	 * &lt;p<sub>l</sub>;&gt;
	 * &lt;h&gt;
	 * &lt;p&gt;
	 * [p2(0) unused]
	 * [pdbtserverhostname(localhost)]
	 * [pdbtserverhostport(7891)]
	 * [epst(0.01)]
	 * [deltaT(0.05)]
	 * [epss(1.0) unused]
	 * [deltas(5.0)]
	 * [batchsize(24)]
	 * [dbglvl(0)]
	 * [run4zeroordercost(true)]
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
		double p2 = 0;
		if (args.length>7) p2 = Double.parseDouble(args[7]);
		String host = "localhost";
		if (args.length>8) host = args[8];
		int port = 7891;
		if (args.length>9) port = Integer.parseInt(args[9]);
		double epst = 0.01;
		if (args.length>10) epst = Double.parseDouble(args[10]);
		double deltat = 0.05;
		if (args.length>11) deltat = Double.parseDouble(args[11]);
		double epss = 1.0;
		//if (args.length>12) epss = Double.parseDouble(args[12]);
		double deltas = 5.0;
		if (args.length>13) deltas = Double.parseDouble(args[13]);
		int bsize = 24;
		if (args.length>14) bsize = Integer.parseInt(args[14]);
		int dbglvl = 0;
		if (args.length>15) {
			dbglvl = Integer.parseInt(args[15]);
		}
		mger.setDebugLevel(dbglvl);
		boolean run4zeroordercost = true;
		if (args.length>16) {
			run4zeroordercost = Boolean.parseBoolean(args[16]);
		}

		// 2. create function
		sSTCnbin f = new sSTCnbin(Kr,Ko,L,lambda,p_l,h,p);
		long start = System.currentTimeMillis();
		// 3. optimize function
		sSTCnbinFastHeur2Opt ropter = 
			new sSTCnbinFastHeur2Opt(host, port, bsize, epst, deltat, epss, deltas,
			                         run4zeroordercost);
		try {
			PairObjDouble result = ropter.minimize(f);
			long dur = System.currentTimeMillis()-start;
			double[] x = (double[])result.getArg();
			double c = result.getDouble();
			System.out.println("s*="+x[0]+" S*="+x[1]+" T*="+x[2]+" ==> C*="+c);
			System.out.println("run-time="+dur+" msecs");
			ropter.terminateServerConnection();
			
			// finally: now visualize results
			ArrayList[] xyseries = ropter.getLatestTimeSeries();
			XYSeriesCollection xyc = new XYSeriesCollection();
			XYSeries tcs = new XYSeries("C*");
			for (int i=0; i<xyseries[1].size(); i++) {
				tcs.add(((Double)xyseries[0].get(i)).doubleValue(), 
					      ((Double)xyseries[1].get(i)).doubleValue());
			}
			xyc.addSeries(tcs);
			JFreeChart chart = 
				ChartFactory.createXYLineChart(
					"(Near)-Optimal (s,S,T) Cost as Function of T",
					"T",
					"Costs",
					xyc,
					PlotOrientation.VERTICAL,
					true, true, false);
			// customize plot
			XYPlot plot = (XYPlot) chart.getPlot();
			XYLineAndShapeRenderer r = (XYLineAndShapeRenderer) plot.getRenderer();
			// set actual costs graphics
			Shape shape  = new Ellipse2D.Double(0,0,3,3);
			r.setSeriesShape(0, shape);
			r.setSeriesShapesVisible(0,true);
			r.setSeriesLinesVisible(0, false);
			
			ChartPanel cp = new ChartPanel(chart);
			// create new JPanel for each frame we show
			javax.swing.JPanel _GraphPanel = new javax.swing.JPanel();
			_GraphPanel.setLayout(new BorderLayout());
			_GraphPanel.add(cp, BorderLayout.CENTER);  
			_GraphPanel.validate();
			_GraphPanel.repaint();
			// set in a frame and show
			javax.swing.JFrame plot_frame = new javax.swing.JFrame();
			plot_frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
			NumberFormat df = NumberFormat.getInstance(Locale.US);
			df.setGroupingUsed(false);
			df.setMaximumFractionDigits(2);
			plot_frame.setTitle("Fast Heuristic2 Grid-Search of (s,S,T) Policy w/ "+
				                  "Negative Binomial Demands");
			plot_frame.add(_GraphPanel);
			plot_frame.setLocationRelativeTo(null);
			plot_frame.pack();
			plot_frame.setVisible(true);						
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
		super(new FunctionBaseStatic(f), arg, new HashMap());
	}
	
	public double[] getArg() {
		return (double[]) super.getArg();
	}
}

