package tests.sic.sST.nbin;

import popt4jlib.FunctionIntf;
import popt4jlib.OptimizerException;
import popt4jlib.OptimizerIntf;
import parallel.TaskObject;
import parallel.distributed.PDBTExecInitNoOpCmd;
import parallel.distributed.PDBTExecInitedClt;
import utils.PairObjDouble;
import utils.Messenger;
import tests.sic.rnqt.nbin.*;
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
 * under compound Poisson demands modeled with the Negative Binomial 
 * distribution, by computing near-optimal (r,nQ,T) policy parameters
 * r*,Q*, and T* via the <CODE>tests.sic.rnqt.nbin.RnQTCnbinHeurPOpt</CODE> 
 * class logic -which is usually done much faster- and then computing the 
 * optimal (s(t),S(t)) parameters for t close to T* and picking the best set of
 * parameters.
 * In fact, we also compute the (R,nQ,T) optimal policy for a system with review
 * cost equal to Kr and zero ordering cost and for the resulting review period 
 * T' we compute similarly the optimal s(T') and S(T').
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class sSTCnbinFastHeurOpt implements OptimizerIntf {
	
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

	/**
	 * by default, 24 tasks to be submitted each time to be processed in parallel
	 */
	private final int _batchSz;
	
	private int _numRunning = 0;

	private ArrayList _tis;      // used for visualization purposes
	private ArrayList _ctis;     // again, for visualization purposes only
	private ArrayList _lbtis;    // guess for what purposes this is...

	
	/**
	 * sole public constructor.
	 * @param server String; default localhost
	 * @param port int; default 7891
	 * @param batchSz int &gt;0; default 24
	 * @param epsT double &gt;0; default 0.01
	 * @param deltaT double &gt;0 default 0.1
	 */
	public sSTCnbinFastHeurOpt(String server, int port, int batchSz, 
		                        double epsT, double deltaT) {
		if (server==null || server.length()==0) _pdsrv = "localhost";
		else _pdsrv = server;
		if (port>1024) _pdport = port;
		else _pdport = 7891;
		_batchSz = (batchSz>0) ? batchSz : 24;
		_epsT = epsT>0 ? epsT : 0.01;
		_deltaT = deltaT>0 ? deltaT : 0.1;
		
		_tis = new ArrayList();
		_ctis = new ArrayList();
		_lbtis = new ArrayList();
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
	 * @param f FunctionIntf must be of type sSTCnbin
	 * @return PairObjDouble Pair&lt;double[] args, double bestcost&gt; where the 
	 * args is an array holding the parameters (s*,S*,T*) yielding the bestcost 
	 * value
	 * @throws OptimizerException 
	 */
	public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (!(f instanceof sSTCnbin))
			throw new OptimizerException("sSTCnbinFastHeurOpt.minimize(f): f must be"+
				                           " of type tests.sic.sST.nbin.sSTCnbin");
		Messenger mger = Messenger.getInstance();
		synchronized (this) {
			if (_pdclt==null) {
				mger.msg("sSTCnbinFastHeurOpt.minimize(f): connecting on "+_pdsrv+
					       " on port "+_pdport, 2);
				_pdclt = new PDBTExecInitedClt(_pdsrv, _pdport);
				try {
					_pdclt.submitInitCmd(new PDBTExecInitNoOpCmd());
					mger.msg("sSTCnbinFastHeurOpt.minimize(f): sent init cmd", 2);
				}
				catch (Exception e) {
					e.printStackTrace();
					throw new OptimizerException("sSTCnbinFastHeurOpt.mainimize(f): clt "+
						                           "failed to submit empty init-cmd to "+
						                           "network");
				}
			}
			_tis.clear();
			_ctis.clear();
			_lbtis.clear();
			++_numRunning;
		}
		sSTCnbin sSTC = (sSTCnbin) f;
		
		double Tmin = 0;
		double c_cur_best = Double.POSITIVE_INFINITY;
		
		double s_star=Double.NaN;
		double S_star = Double.NaN;
		double t_star = Double.NaN;
		
		mger.msg("sSTCnbinFastHeurOpt.minimize(f): running "+
			       "(r,nQ,T) policy optimization", 1);
		
		// 1. compute the optimal T* for the (r,nQ,T) policy
		final double t_rnqt = getOptimalRnQTReview(sSTC);
		mger.msg("sSTCnbinFastHeurOpt.minimize(f): "+
			       "(r,nQ,T) policy optimization returns T*="+t_rnqt, 1);
		
		// 2. do a search around T* for the best (s,S,T) parameters
		double T = Tmin >= t_rnqt-_deltaT ? Tmin : t_rnqt-_deltaT;
		
		final double Tmax = t_rnqt + _deltaT;

		boolean done = false;
		
		while (T<Tmax && !done) {
			// 1. prepare batch
			final int bsz = (int) Math.ceil((Tmax-T)/_epsT);
			final int batchSz = bsz < _batchSz ? bsz : _batchSz;
			TaskObject[] batch = new TaskObject[batchSz];
			double Tstart = T;
			for (int i=0; i<batchSz; i++) {
				T += _epsT;
				batch[i] = new sSTCnbinFixedTOptTask(sSTC,T,c_cur_best);
			}
			try {
				mger.msg("sSTCnbinFastHeurOpt.minimize(f): submit a batch of "+batchSz+
					       " tasks to network for period length from "+(Tstart+_epsT)+
					       " up to "+T, 
					       1);
				Object[] res = _pdclt.submitWorkFromSameHost(batch);
				for (int i=0; i<res.length; i++) {
					try {
						sSTCnbinFixedTOpterResult ri = 
							(sSTCnbinFixedTOpterResult) res[i];
						_tis.add(new Double(ri._T));  // add to tis time-series
						_ctis.add(new Double(ri._C));  // add to c(t)'s time-series
						_lbtis.add(new Double(ri._LB));  // add to lb(t)'s time-series
						if (Double.compare(ri._LB, c_cur_best) > 0) {  // done!
							mger.msg("sSTCnbinFastHeurOpt.minimize(f): for T="+ri._T+
								       " LB@T="+ri._LB+
									     " c@T="+ri._C+" c*="+c_cur_best+"; done.", 1);
							done = true;
						}
						if (ri._C < c_cur_best) {
							s_star = ri._s;
							S_star = ri._S;
							t_star = ri._T;
							c_cur_best = ri._C;
							mger.msg("sSTCnbinFastHeurOpt.minimize(f): found better soln at"+
								       " T="+t_star+", c="+c_cur_best+" LB@T="+ri._LB, 1);
						}
					}
					catch (ClassCastException e) {  // FailedReply instead of result
						mger.msg("task["+i+"] failed, will ignore but optimization results"+
							       " may be unreliable.", 0);
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new OptimizerException("sSTCnbinFastHeurOpt.minimize(): failed "+
					                           "to submit tasks/process/get back result");
			}
		}  // while
		
		// 3. find the optimal T for a system with review cost Kr and 0 order cost!
		sSTCnbin sSTC2 = new sSTCnbin(sSTC._Kr, 0, sSTC._L, 
			                            sSTC._lambda, sSTC._p_l, sSTC._h, sSTC._p);
		final double t_rnqt2 = getOptimalRnQTReview(sSTC2);
		mger.msg("sSTCnbinFastHeurOpt.minimize(Kr,0): "+
			       "(r,nQ,T) policy optimization returns T*="+t_rnqt2, 1);
		
		// 4. do a search around T' for the best (s,S,T) parameters, making sure
		//    we don't cover T's that have been covered already
		T = Tmin >= t_rnqt2-_deltaT ? Tmin : t_rnqt2-_deltaT;
		if (T<=Tmax && T>=Math.max(Tmin,t_rnqt-_deltaT)) {
			// new left-end of search area is within the already searched area
			T = Tmax;
		}
		double Tmax2 = t_rnqt2 + _deltaT;
		if (Tmax2 <= Tmax && Tmax2>=Math.max(Tmin,t_rnqt-_deltaT)) {
			// Tmax2 falls within already searched area
			Tmax2 = Math.max(Tmin, t_rnqt-_deltaT);
		}
		mger.msg("sSTCnbinFastHeurOpt.minimize(f): last search area is "+
			       "["+T+","+Tmax2+"]", 1);

		done = false;
		
		while (T<Tmax2 && !done) {
			// 1. prepare batch
			final int bsz = (int) Math.ceil((Tmax2-T)/_epsT);
			final int batchSz = bsz < _batchSz ? bsz : _batchSz;
			TaskObject[] batch = new TaskObject[batchSz];
			double Tstart = T;
			for (int i=0; i<batchSz; i++) {
				T += _epsT;
				batch[i] = new sSTCnbinFixedTOptTask(sSTC,T,c_cur_best);
			}
			try {
				mger.msg("sSTCnbinFastHeurOpt.minimize(f): submit a batch of "+batchSz+
					       " tasks to network for period length from "+(Tstart+_epsT)+
					       " up to "+T, 
					       1);
				Object[] res = _pdclt.submitWorkFromSameHost(batch);
				for (int i=0; i<res.length; i++) {
					try {
						sSTCnbinFixedTOpterResult ri = 
							(sSTCnbinFixedTOpterResult) res[i];
						_tis.add(new Double(ri._T));  // add to tis time-series
						_ctis.add(new Double(ri._C));  // add to c(t)'s time-series
						_lbtis.add(new Double(ri._LB));  // add to lb(t)'s time-series
						if (Double.compare(ri._LB, c_cur_best) > 0) {  // done!
							mger.msg("sSTCnbinFastHeurOpt.minimize(f): for T="+ri._T+
								       " LB@T="+ri._LB+
									     " c@T="+ri._C+" c*="+c_cur_best+"; done.", 1);
							done = true;
						}
						if (ri._C < c_cur_best) {
							s_star = ri._s;
							S_star = ri._S;
							t_star = ri._T;
							c_cur_best = ri._C;
							mger.msg("sSTCnbinFastHeurOpt.minimize(f): found better soln at"+
								       " T="+t_star+", c="+c_cur_best+" LB@T="+ri._LB, 1);
						}
					}
					catch (ClassCastException e) {  // FailedReply recvd
						mger.msg("task["+i+"] failed, will ignore but optimization results"+
							       " may be unreliable.", 0);						
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new OptimizerException("sSTCnbinFastHeurOpt.minimize(): failed "+
					                           "to submit tasks/process/get back result");
			}
		}  // while
		
		try {
			_pdclt.submitCmd(new PDBTExecReportMaxSum12TermsCmd());
		}
		catch (Exception e) {
			e.printStackTrace();  // no-op
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
	 * costs, third is lower bound on costs
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
		ArrayList[] result = new ArrayList[4];
		result[0] = _tis;
		result[1] = _ctis;
		result[2] = _lbtis;
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
			throw new Error("sSTCnbinHeurOpt.terminateServerConnection() "+
				              "failed?");
		}
	}
	
	
	/**
	 * find the optimal (r,nQ,T) policy for a system operating with the parameters
	 * contained in the given function, and return the optimal review period T*.
	 * Notice that in order to speed up the (R,nQ,T) optimization process, we 
	 * multiply the _epsT used in the (s,S,T) outer T-search by the factor 
	 * <CODE>_EPST_MULT</CODE>.
	 * @param f sSTCnbin
	 * @return double optimal review period for the (r,nQ,T) policy
	 * @throws OptimizerException
	 */
	private synchronized double getOptimalRnQTReview(sSTCnbin f) 
		throws OptimizerException {
		RnQTCnbin rnqt = new RnQTCnbin(f._Kr, f._Ko, f._L, f._lambda, f._p_l,
		                                     f._h, f._p);
		final double tnot = 0.0;  // this will cause the default tnot value 
		                          // to be used (currently at 0.01)
		final double epsT_rnqt = _EPST_MULT*_epsT;  // use 
		RnQTCnbinHeurPOpt opter = new RnQTCnbinHeurPOpt(_pdsrv, _pdport, _batchSz, 
			                                              epsT_rnqt, tnot);
		// first set the _pdclt
		HashMap p = new HashMap();
		p.put("rnqtcnbinheurpopt.pdclt", _pdclt);
		opter.setParams(p);
		PairObjDouble res = opter.minimize(rnqt);
		return ((double[])res.getArg())[2];
	}
	
	
	/**
	 * invoke as 
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.sST.nbin.sSTCnbinFastHeurOpt 
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
	 * [deltaT(0.1)]
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
		double p_l = Double.parseDouble(args[4]);
		double h = Double.parseDouble(args[5]);
		double p = Double.parseDouble(args[6]);
		String host = "localhost";
		if (args.length>7) host = args[7];
		int port = 7891;
		if (args.length>8) port = Integer.parseInt(args[8]);
		double epst = 0.01;
		if (args.length>9) epst = Double.parseDouble(args[9]);
		double deltat = 0.1;
		if (args.length>10) deltat = Double.parseDouble(args[10]);
		int bsize = 24;
		if (args.length>11) bsize = Integer.parseInt(args[11]);
		int dbglvl = 0;
		if (args.length>12) {
			dbglvl = Integer.parseInt(args[12]);
		}
		mger.setDebugLevel(dbglvl);
		// 2. create function
		sSTCnbin f = new sSTCnbin(Kr,Ko,L,lambda,p_l,h,p);
		long start = System.currentTimeMillis();
		// 3. optimize function
		sSTCnbinFastHeurOpt ropter = 
			new sSTCnbinFastHeurOpt(host, port, bsize, epst, deltat);
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
			XYSeries tlbs = new XYSeries("LB");
			for (int i=0; i<xyseries[1].size(); i++) {
				tcs.add(((Double)xyseries[0].get(i)).doubleValue(), 
					      ((Double)xyseries[1].get(i)).doubleValue());
			}
			for (int i=0; i<xyseries[2].size(); i++) {
				tlbs.add(((Double)xyseries[0].get(i)).doubleValue(), 
					      ((Double)xyseries[2].get(i)).doubleValue());				
			}
			xyc.addSeries(tcs);
			xyc.addSeries(tlbs);
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
			r.setSeriesLinesVisible(1, false);
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
			plot_frame.setTitle("(s,S,T) Policy Heuristic T-Search w/ NBin Demands");
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

