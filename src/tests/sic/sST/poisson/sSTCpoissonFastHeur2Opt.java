package tests.sic.sST.poisson;

import popt4jlib.FunctionIntf;
import popt4jlib.OptimizerException;
import popt4jlib.OptimizerIntf;
import parallel.TaskObject;
import parallel.distributed.FailedReply;
import parallel.distributed.PDBTExecInitNoOpCmd;
import parallel.distributed.PDBTExecInitedClt;
import utils.PairObjDouble;
import utils.Messenger;
import tests.sic.rnqt.poisson.*;
import java.util.ArrayList;
import java.util.HashMap;
// imports below needed for the plotting of the search
import java.util.Locale;
import java.awt.BorderLayout;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.io.Serializable;
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
 * under Poisson demands, by computing near-optimal (r,nQ,T) policy parameters
 * r*,Q*, and T* according to the heuristic implemented in class 
 * <CODE>tests.sic.rnqt.poisson.RnQTCpoissonHeurPOpt</CODE> and then computing  
 * the best (s(r*),S(r*+Q*)) parameters for t close to T* and picking the best 
 * parameters. The difference from class <CODE>sSTCpoissonFastHeurOpt</CODE> is
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
public final class sSTCpoissonFastHeur2Opt implements OptimizerIntf {
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

	private ArrayList _tis;      // used for visualization purposes
	private ArrayList _ctis;     // again, for visualization purposes only

	
	/**
	 * sole public constructor.
	 * @param server String; default localhost
	 * @param port int; default 7891
	 * @param batchSz int &gt;0; default 24
	 * @param epsT double &gt;0; default 0.01
	 * @param deltaT double &gt;0 default 0.05
	 * @param epss double unused as it is always set to 1.0
	 * @param deltas double &ge;0 default 5.0
	 */
	public sSTCpoissonFastHeur2Opt(String server, int port, int batchSz, 
		                          double epsT, double deltaT, 
															double epss, double deltas) {
		if (server==null || server.length()==0) _pdsrv = "localhost";
		else _pdsrv = server;
		if (port>1024) _pdport = port;
		else _pdport = 7891;
		_batchSz = (batchSz>0) ? batchSz : 24;
		_epsT = epsT>0 ? epsT : 0.01;
		_deltaT = deltaT>0 ? deltaT : 0.05;
		// _epss = 1.0;
		_deltas = deltas>=0 ? deltas : 5.0;
		
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
	 * @param f FunctionIntf must be of type sSTCpoisson
	 * @return PairObjDouble Pair&lt;double[] args, double bestcost&gt; where the 
	 * args is an array holding the parameters (s*,S*,T*) yielding the bestcost 
	 * value
	 * @throws OptimizerException 
	 */
	public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (!(f instanceof sSTCpoisson))
			throw new OptimizerException("sSTCpoissonFastHeur2Opt.minimize(f): f "+
				                           "not tests.sic.sST.poisson.sSTCpoisson ?");
		Messenger mger = Messenger.getInstance();
		synchronized (this) {
			if (_numRunning>0) 
				throw new OptimizerException("sSTCpoissonFastHeur2Opt.minimize(f) is "+
					                           "already running "+
					                           "(by another thread on this object)");
			if (_pdclt==null) {
				mger.msg("sSTCpoissonFastHeur2Opt.minimize(f): connecting on "+_pdsrv+
					       " on port "+_pdport, 2);
				_pdclt = new PDBTExecInitedClt(_pdsrv, _pdport);
				try {
					_pdclt.submitInitCmd(new PDBTExecInitNoOpCmd());
					mger.msg("sSTCpoissonFastHeur2Opt.minimize(f): successfully sent "+
						       "init cmd", 
						       2);
				}
				catch (Exception e) {
					e.printStackTrace();
					throw new OptimizerException("sSTCpoissonFastHeur2Opt.mainimize(f): "+
						                           "clt failed to send empty init-cmd to "+
						                           "network");
				}
			}
			_tis.clear();
			_ctis.clear();
			++_numRunning;
		}
		sSTCpoisson sSTC = (sSTCpoisson) f;
		
		double Tmin = _epsT;  // used to be zero
		
		double s_star=Double.NaN;
		double S_star = Double.NaN;
		double t_star = Double.NaN;
		
		mger.msg("sSTCpoissonFastHeur2Opt.minimize(): running "+
			       "(r,nQ,T) policy optimization", 1);
		// first, compute the optimal T* for the (r,nQ,T) policy
		final double[] rnqt_arr = getNearOptimalRnQTPolicy(sSTC);
		mger.msg("sSTCpoissonFastHeur2Opt.minimize(): "+
			       "(r,nQ,T) policy optimization returns "+
			       "R*="+rnqt_arr[0]+", Q*="+rnqt_arr[1]+", T*="+rnqt_arr[2], 1);
		
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
					if (S<=s) continue;  // don't run useless tasks
					all_tasks.add(new FET2(sSTC, new double[]{s,S,T}));
				}
			}
		}
		TaskObject[] tasks_arr = new TaskObject[all_tasks.size()];
		final int batchSz = all_tasks.size();
		for(int i=0; i<all_tasks.size(); i++) 
			tasks_arr[i] = (TaskObject) all_tasks.get(i);
		try {
			mger.msg("sSTCpoissonFastHeur2Opt.minimize(): submit a batch of "+batchSz+
				       " tasks to network for period length from "+(Tstart+_epsT)+
				       " up to "+Tmax, 
				       1);
			Object[] res = tasks_arr.length>0 ? 
										   _pdclt.submitWorkFromSameHost(tasks_arr) : 
				               null;
			mger.msg("sSTCpoissonFastHeur2Opt.minimize(): got results from entire "+
				       "batch execution.", 1);
			if (res!=null) {
				for (int i=0; i<res.length; i++) {
					FET2 ri = 
						(FET2) res[i];
					synchronized(this) {
						if (i==0 ||
								((Double)_tis.get(_tis.size()-1)).doubleValue()<ri.getArg()[2]) {
							_tis.add(new Double(ri.getArg()[2]));  // add to tis time-series
							_ctis.add(new Double(ri.getObjValue()));  // add to c(t)'s t-series
						}
						else {
							double cur_val_t=((Double)_ctis.get(_ctis.size()-1)).doubleValue();
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
						mger.msg("sSTCpoissonFastHeur2Opt.minimize(f): found new better soln"+
										 " @T="+t_star+", c="+c_cur_best, 1);
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new OptimizerException("sSTCpoissonFastHeur2Opt.minimize(): failed"+
				                           " to submit tasks/process/get back result");
		}
				
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
			throw new Error("sSTCpoissonFastHeur2Opt.terminateServerConnection() "+
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
	private synchronized double[] getNearOptimalRnQTPolicy(sSTCpoisson f) 
		throws OptimizerException {
		RnQTCpoisson rnqt = new RnQTCpoisson(f._Kr, f._Ko, f._L, f._lambda,
		                                     f._h, f._p, f._p2);
		final double tnot = 0.0;  // this will cause the default tnot value 
		                          // to be used (currently at 0.01)
		RnQTCpoissonHeurPOpt opter = 
			new RnQTCpoissonHeurPOpt(_pdsrv, _pdport, _batchSz, _epsT, tnot);
		// following code is needed with the parallel heur. optimization algorithm
		// first set the _pdclt
		HashMap p = new HashMap();
		p.put("rnqtcpoissonheurpopt.pdclt", _pdclt);
		opter.setParams(p);
		PairObjDouble res = opter.minimize(rnqt);
		return ((double[])res.getArg());
	}
	
	
	/**
	 * invoke as 
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.sST.poisson.sSTCpoissonFastHeur2Opt 
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
	 * [deltaT(0.05)]
	 * [epss(1.0) unused]
	 * [deltas(5.0)]
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
		double deltat = 0.05;
		if (args.length>10) deltat = Double.parseDouble(args[10]);
		double epss = 1.0;
		//if (args.length>11) epss = Double.parseDouble(args[11]);
		double deltas = 5.0;
		if (args.length>12) deltas = Double.parseDouble(args[12]);
		int bsize = 24;
		if (args.length>13) bsize = Integer.parseInt(args[13]);
		int dbglvl = 0;
		if (args.length>14) {
			dbglvl = Integer.parseInt(args[14]);
		}
		mger.setDebugLevel(dbglvl);
		// 2. create function
		sSTCpoisson f = new sSTCpoisson(Kr,Ko,L,lambda,h,p,p2);
		long start = System.currentTimeMillis();
		// 3. optimize function
		sSTCpoissonFastHeur2Opt ropter = 
			new sSTCpoissonFastHeur2Opt(host, port, bsize, epst, deltat, epss, deltas);
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
			plot_frame.setTitle("Fast Heuristic2 Grid-Search of (s,S,T) Policy with "+
				                  "Poisson Demands");
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

