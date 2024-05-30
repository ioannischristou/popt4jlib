package tests.sic.sST.poisson;

import popt4jlib.FunctionIntf;
import popt4jlib.OptimizerException;
import popt4jlib.OptimizerIntf;
import parallel.TaskObject;
import parallel.distributed.PDBTExecInitNoOpCmd;
import parallel.distributed.PDBTExecInitedClt;
import utils.PairObjDouble;
import utils.Messenger;
import tests.sic.rnqt.poisson.*;
import java.util.HashMap;
import java.util.ArrayList;
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
 * under Poisson demands, by computing near-optimal (r,nQ,T) policy parameters
 * r*,Q*, and T* -which is usually done much faster- using the class 
 * <CODE>tests.sic.rnqt.poisson.RnQTCpoisonHeurPOpt</CODE> and then computing 
 * optimal (s(t),S(t)) parameters for t close to T* and picking the best set of
 * parameters.
 * <p>Notes:
 * <ul>
 * <li>2023-07-06: disallowed running <CODE>minimize(f)</CODE> concurrently from
 * multiple threads.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class sSTCpoissonFastHeurOpt implements OptimizerIntf {
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

	/**
	 * by default, 8 tasks to be submitted each time to be processed in parallel
	 */
	private final int _batchSz;
	
	private int _numRunning = 0;

	private ArrayList _tis;      // used for visualization purposes
	private ArrayList _ctis;     // again, for visualization purposes only

	
	/**
	 * sole public constructor.
	 * @param server String; default localhost
	 * @param port int; default 7891
	 * @param batchSz int &gt;0; default 8
	 * @param epsT double &gt;0; default 0.01
	 * @param deltaT double &gt;0 default 0.05
	 */
	public sSTCpoissonFastHeurOpt(String server, int port, int batchSz, 
		                            double epsT, double deltaT) {
		if (server==null || server.length()==0) _pdsrv = "localhost";
		else _pdsrv = server;
		if (port>1024) _pdport = port;
		else _pdport = 7891;
		_batchSz = (batchSz>0) ? batchSz : 8;
		_epsT = epsT>0 ? epsT : 0.01;
		_deltaT = deltaT>0 ? deltaT : 0.05;
		
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
	 * main class method, does not allow running concurrently from multiple 
	 * threads.
	 * @param f FunctionIntf must be of type sSTCpoisson
	 * @return PairObjDouble Pair&lt;double[] args, double bestcost&gt; where the 
	 * args is an array holding the parameters (s*,S*,T*) yielding the bestcost 
	 * value
	 * @throws OptimizerException 
	 */
	public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (!(f instanceof sSTCpoisson))
			throw new OptimizerException("sSTCpoissonFastHeurOpt.minimize(f): f must"+
				                           " be "+
				                           "of type tests.sic.sST.poisson.sSTCpoisson");
		Messenger mger = Messenger.getInstance();
		synchronized (this) {
			if (_numRunning>0) 
				throw new OptimizerException("sSTCpoissonFastHeurOpt.minimize(f) is "+
					                           "already running "+
					                           "(by another thread on this object)");						
			if (_pdclt==null) {
				mger.msg("sSTCpoissonFastHeurOpt.minimize(f): connecting on "+_pdsrv+
					       " on port "+_pdport, 2);
				_pdclt = new PDBTExecInitedClt(_pdsrv, _pdport);
				try {
					_pdclt.submitInitCmd(new PDBTExecInitNoOpCmd());
					mger.msg("sSTCpoissonFastHeurOpt.minimize(f): sent init cmd", 2);
				}
				catch (Exception e) {
					e.printStackTrace();
					throw new OptimizerException("sSTCpoissonFastHeurOpt.mainimize(f): "+
						                           "clt failed to submit empty init-cmd to"+
						                           " network");
				}
			}
			_tis.clear();
			_ctis.clear();
			++_numRunning;
		}
		sSTCpoisson sSTC = (sSTCpoisson) f;
		
		double Tmin = 0;
		double c_cur_best = Double.POSITIVE_INFINITY;
		
		double s_star=Double.NaN;
		double S_star = Double.NaN;
		double t_star = Double.NaN;
		
		mger.msg("sSTCpoissonFastHeurOpt.minimize(): running "+
			       "(r,nQ,T) policy heuristic optimization", 1);
		// first, compute the optimal T* for the (r,nQ,T) policy
		final double t_rnqt = getNearOptimalRnQTReview(sSTC);
		mger.msg("sSTCpoissonFastHeurOpt.minimize(): "+
			       "(r,nQ,T) policy heuristic optimization returns T*="+t_rnqt, 1);
		
		double T = Tmin >= t_rnqt-_deltaT ? Tmin : t_rnqt-_deltaT;
		
		final double Tmax = t_rnqt + _deltaT;
		
		while (T<Tmax) {
			// 1. prepare batch
			final int bsz = (int) Math.ceil((Tmax-T)/_epsT);
			final int batchSz = bsz < _batchSz ? bsz : _batchSz;
			TaskObject[] batch = new TaskObject[batchSz];
			double Tstart = T;
			for (int i=0; i<batchSz; i++) {
				T += _epsT;
				batch[i] = new sSTCpoissonFixedTOptTask(sSTC,T,c_cur_best);
			}
			try {
				mger.msg("sSTCpoissonFastHeurOpt.minimize(): submit a batch of "+
					       batchSz+" tasks to network for period length from "+Tstart+
					       " up to "+T, 2);
				Object[] res = _pdclt.submitWorkFromSameHost(batch);
				for (int i=0; i<res.length; i++) {
					sSTCpoissonFixedTOpterResult ri = 
						(sSTCpoissonFixedTOpterResult) res[i];
					synchronized(this) {
						_tis.add(new Double(ri._T));  // add to tis time-series
						_ctis.add(new Double(ri._C));  // add to c(t)'s time-series
					}
					if (ri._C < c_cur_best) {
						s_star = ri._s;
						S_star = ri._S;
						t_star = ri._T;
						c_cur_best = ri._C;
						mger.msg("sSTCpoissonFastHeurOpt.minimize(f): found better soln at"+
							       " T="+t_star+", c="+c_cur_best, 1);
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new OptimizerException("sSTCpoissonFastHeurOpt.minimize(): "+
					                           "failed to "+
					                           "submit tasks/process/get back results");
			}
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
			throw new Error("sSTCpoissonFastHeurOpt.terminateServerConnection() "+
				              "failed?");
		}
	}
	
	
	/**
	 * find near-optimal (r,nQ,T) policy for a system operating with parameters
	 * contained in the given function, and return the optimal review period T*.
	 * @param f sSTCpoisson
	 * @return double near-optimal review period for the (r,nQ,T) policy
	 * @throws OptimizerException
	 */
	private synchronized double getNearOptimalRnQTReview(sSTCpoisson f) 
		throws OptimizerException {
		RnQTCpoisson rnqt = new RnQTCpoisson(f._Kr, f._Ko, f._L, f._lambda,
		                                     f._h, f._p, f._p2);
		final double tnot = 0.0;  // this will cause the default tnot value 
		                          // to be used (currently at 0.01)
		RnQTCpoissonHeurPOpt opter = new RnQTCpoissonHeurPOpt(_pdsrv, _pdport, 
			                                                    _batchSz, 
			                                                    _epsT, tnot);
		// first set the _pdclt
		HashMap p = new HashMap();
		p.put("rnqtcpoissonheurpopt.pdclt", _pdclt);
		opter.setParams(p);
		PairObjDouble res = opter.minimize(rnqt);
		return ((double[])res.getArg())[2];
	}
	
	
	/**
	 * invoke as 
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.sST.poisson.sSTCpoissonFastHeurOpt 
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
		sSTCpoissonFastHeurOpt ropter = 
			new sSTCpoissonFastHeurOpt(host, port, bsize, epst, deltat);
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
					"Optimal (s,S,T) Cost as Function of T",
					"T",
					"Costs",
					xyc,
					PlotOrientation.VERTICAL,
					true, true, false);
			// customize plot
			XYPlot plot = (XYPlot) chart.getPlot();
			XYLineAndShapeRenderer r = (XYLineAndShapeRenderer) plot.getRenderer();
			//r.setSeriesShape(2, ShapeUtilities.createUpTriangle(5));
			//r.setSeriesShapesVisible(2, true);
			//r.setSeriesLinesVisible(2, false);
			Shape shape  = new Ellipse2D.Double(0,0,3,3);
			r.setSeriesShape(0, shape);
			r.setSeriesShapesVisible(0,true);
			r.setSeriesLinesVisible(0, false);
			//r.setSeriesPaint(2, Color.DARK_GRAY);
			
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
			plot_frame.setTitle("Fast Heuristic T-Search of (s,S,T) Policy with "+
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

