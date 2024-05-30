package tests.sic.sST.poisson;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import parallel.TaskObject;
import parallel.distributed.PDBTExecInitNoOpCmd;
import parallel.distributed.PDBTExecInitedClt;
import popt4jlib.FunctionIntf;
import popt4jlib.OptimizerException;
import tests.sic.rnqt.poisson.RnQTCpoisson;
import utils.PairObjDouble;
import utils.Messenger;
// below import needed for visualization of graph of optimal C(T) for various T
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
//import org.jfree.data.time.TimeSeries;
//import org.jfree.data.time.TimeSeriesCollection;
//import org.jfree.data.time.Year;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
//import org.jfree.util.ShapeUtilities;


/**
 * class graphs the optimal Ä*(T)=S(T)-s(T) for various T values for the (s,S,T)
 * policy.
 * The system being optimized faces Poisson distributed stochastic demands 
 * as described in the class <CODE>sSTCpoisson</CODE>. The solution found is 
 * guaranteed to be the global optimum (subject to the step-size constraint for
 * the review period variable). The optimizer is a parallel/distributed
 * method, submitting tasks to optimize over the first two variables, for fixed 
 * review period T, where the review period T is increased from the Tmin(=0) 
 * value it can take on, up to a point where
 * the lower-bound on the cost function (namely the cost function with Kr=Ko=0)
 * strictly exceeds the best known cost.
 * <p>Notes:
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class sSTCpoissonOptGraphD_T {
	/**
	 * default address for PDBTExecSingleCltWrkInitSrv
	 */
	private final String _pdsrv;  
	/**
	 * default port for PDBTExecSingleCltWrkInitSrv
	 */
	private final int _pdport;
	
	private PDBTExecInitedClt _pdclt;
	
	private final double _T_0;
	private final double _epsT;

	/**
	 * by default, 24 tasks to be submitted each time to be processed in parallel
	 */
	private final int _batchSz;
	
	private int _numRunning = 0;

	private ArrayList _tis;      // used for visualization purposes
	private ArrayList _deltatis; // again, for visualization purposes only

	
	/**
	 * sole public constructor.
	 * @param server String; default localhost
	 * @param port int; default 7891
	 * @param batchSz int &gt;0; default 24
	 * @param epsT double &gt;0; default 0.01
	 * @param T_0 double &gt;0; default 0
	 */
	public sSTCpoissonOptGraphD_T(String server, int port, 
		                            int batchSz, double epsT, double T_0) {
		if (server==null || server.length()==0) _pdsrv = "localhost";
		else _pdsrv = server;
		if (port>1024) _pdport = port;
		else _pdport = 7891;
		_batchSz = (batchSz>0) ? batchSz : 24;
		_epsT = epsT>0 ? epsT : 0.01;
		_T_0 = T_0 > 0 ? T_0 : 0;
		
		_tis = new ArrayList();
		_deltatis = new ArrayList();
	}
	

	/**
	 * no-op.
	 * @param p HashMap unused 
	 */
	public void setParams(java.util.HashMap p) {
		// no-op.
	}

		
	/**
	 * main class method, does not allow running concurrently from different 
	 * threads on the same object.
	 * @param f FunctionIntf must be of type sSTCpoisson
	 * @return PairObjDouble Pair&lt;double[] bestx, double bestcost&gt; where the
	 * bestx array contains the values (s*,S*,T*)
	 * @throws OptimizerException 
	 */
	public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (!(f instanceof sSTCpoisson))
			throw new OptimizerException("f must be tests.sic.sST.sSTCpoisson");
		Messenger mger = Messenger.getInstance();
		synchronized (this) {
			if (_numRunning>0) 
				throw new OptimizerException("minimize(f) is "+
					                           "already running "+
					                           "(by another thread on this object)");						
			if (_pdclt==null) {
				mger.msg("minimize(f): connecting on "+_pdsrv+
					       " on port "+_pdport, 2);
				_pdclt = new PDBTExecInitedClt(_pdsrv, _pdport);
				try {
					_pdclt.submitInitCmd(new PDBTExecInitNoOpCmd());
					mger.msg("minimize(f): successfully sent no-op init cmd", 2);
				}
				catch (Exception e) {
					e.printStackTrace();
					throw new OptimizerException("mainimize(f): clt "+
						                           "failed to submit empty init-cmd to "+
						                           "network");
				}
			}
			_tis.clear();
			_deltatis.clear();
			++_numRunning;
		}
		sSTCpoisson sSTC = (sSTCpoisson) f;
		
		RnQTCpoisson rnqtc_f = new RnQTCpoisson(sSTC._Kr, sSTC._Ko, 
			                                      sSTC._L, sSTC._lambda, 
			                                      sSTC._h, sSTC._p, sSTC._p2);
		
		double Tmin = 0;
		double c_cur_best = Double.POSITIVE_INFINITY;
		
		double s_star=Double.NaN;
		double S_star = Double.NaN;
		double t_star = Double.NaN;
		
		double T = Tmin;
		
		boolean done = false;
		
		while (!done) {
			// 1. prepare batch
			TaskObject[] batch = new TaskObject[_batchSz];
			double Tstart = T;
			for (int i=0; i<_batchSz; i++) {
				T += _epsT;
				batch[i] = new sSTCpoissonFixedTOptTask(sSTC,T,c_cur_best);
			}
			try {
				mger.msg("minimize(): submit a batch of "+_batchSz+
					       " tasks to network for period length from "+(Tstart+_epsT)+
					       " up to "+T, 
					       1);
				Object[] res = _pdclt.submitWorkFromSameHost(batch);
				for (int i=0; i<res.length; i++) {
					sSTCpoissonFixedTOpterResult ri = 
						(sSTCpoissonFixedTOpterResult) res[i];
					final double Ti = ri._T;
					final double deltai = ri._S - ri._s;
					if (Ti >= _T_0) {
						synchronized(this) {
							_tis.add(new Double(Ti));  // add to tis time-series
							_deltatis.add(new Double(deltai));  // add to Ä(t)'s time-series
						}
					}
					if (Double.compare(ri._LB, c_cur_best)>0) {  // done!
						mger.msg("minimize(f): for T="+Ti+" LB@T="+ri._LB+
							       " c@T="+ri._C+" c*="+c_cur_best+"; done.", 1);
						done = true;
					}
					if (Double.compare(ri._C, c_cur_best)<0) {
						s_star = ri._s;
						S_star = ri._S;
						t_star = ri._T;
						c_cur_best = ri._C;
						mger.msg("minimize(f): found new better soln at T="+
							       t_star+", c="+c_cur_best+" LB@T="+ri._LB, 1);
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new OptimizerException("minimize(f): failed to "+
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
	 * @return ArrayList[] first element is T-axis values, 
	 *                     second is optimal costs, 
	 *                     third is (s,S,T) value where s=r*(T), S=r*(T)+Q*(T) for 
	 *                     each T from (R,nQ,T) policy,
	 *                     fourth is (s,S,T) value where s=R*(T), S=R*(T)+1 for 
	 *                     each T from (R,T) policy
	 *                     fifth is EOQ-based lower bound
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
		result[1] = new ArrayList(_deltatis);
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
			throw new Error("terminateServerConnection() failed?");
		}
	}
	
	
	/**
	 * invoke as 
	 * <CODE>
	 * java -cp &lt;claspath&gt; tests.sic.sST.poisson.sSTCpoissonOptGraphD_T 
	 * &lt;T_0&gt;
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
	 * [batchsize(24)]
	 * [dbglvl(0)]
	 * </CODE>.
	 * @param args String[] 
	 */
	public static void main(String[] args) {
		final Messenger mger = Messenger.getInstance();
		
		// 1. parse inputs
		double T_0 = Double.parseDouble(args[0]);
		double Kr = Double.parseDouble(args[1]);
		double Ko = Double.parseDouble(args[2]);
		double L = Double.parseDouble(args[3]);
		double lambda = Double.parseDouble(args[4]);
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
		int bsize = 24;
		if (args.length>11) bsize = Integer.parseInt(args[11]);
		int dbglvl = 0;
		if (args.length>12) dbglvl = Integer.parseInt(args[12]);
		mger.setDebugLevel(dbglvl);
		
		mger.msg("sSTCpoissonOptGraphD_T: "+
			       "T_0="+T_0+
			       " Kr="+Kr+
			       " Ko="+Ko+
			       " L="+L+
			       " ë="+lambda+
			       " h="+h+
						 " p="+p+
			       " p2="+p2+
			       " host="+host+
			       " port="+port+
			       " epsT="+epst+
			       " bsize="+bsize+
			       " dbglevel="+dbglvl
			       , 0);
		
		// 2. create function
		sSTCpoisson f = new sSTCpoisson(Kr,Ko,L,lambda,h,p,p2);

		long start = System.currentTimeMillis();
		// 3. optimize function
		sSTCpoissonOptGraphD_T ropter = 
			new sSTCpoissonOptGraphD_T(host, port, bsize, epst, T_0);
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
			XYSeries deltacs = new XYSeries("Ä*");
			for (int i=0; i<xyseries[1].size(); i++) {
				deltacs.add(((Double)xyseries[0].get(i)).doubleValue(), 
					      ((Double)xyseries[1].get(i)).doubleValue());
			}
			xyc.addSeries(deltacs);
			JFreeChart chart = 
				ChartFactory.createXYLineChart(
					"Optimal (s,S,T) Cost Ä*(T) as Function of T",
					"T",
					"Ä",
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
			plot_frame.setTitle("(s,S,T) Policy with Poisson Demand Plot");
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


