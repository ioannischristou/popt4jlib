package tests.sic.sST.norm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import parallel.TaskObject;
import parallel.distributed.FailedReply;
import parallel.distributed.PDBTExecInitNoOpCmd;
import parallel.distributed.PDBTExecInitedClt;
import popt4jlib.FunctionIntf;
import popt4jlib.OptimizerException;
import popt4jlib.OptimizerIntf;
import utils.PairObjDouble;
import utils.PairObjTwoDouble;
import utils.Messenger;
// below import needed for visualization of graph of optimal C(T) for various T
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
 * class implements an optimizer over ALL three variables of the (s,S,T)
 * policy, namely the reorder point s the order-up-to S and the review period T.
 * The system being optimized faces stochastic demands 
 * as described in the class <CODE>sSTCnorm</CODE>. The solution found is 
 * guaranteed to be the global optimum (subject to the step-size constraints for
 * each of the decision variables). The optimizer is a parallel/distributed
 * method, submitting tasks to optimize over the first two variables, for fixed 
 * review period T, where the review period T is increased from the Tmin value
 * it can take on (dictated by the non-negative demands requirement in any 
 * review period for normally distributed demand process) up to a point where
 * the lower-bound on the cost function (namely the cost function with Ko=0)
 * strictly exceeds the best known cost.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class sSTCnormOpt implements OptimizerIntf {
	/**
	 * default address for PDBTExecSingleCltWrkInitSrv
	 */
	private final String _pdsrv;  
	/**
	 * default port for PDBTExecSingleCltWrkInitSrv
	 */
	private final int _pdport;
	
	private PDBTExecInitedClt _pdclt;
	
	private final double _epsT;  // default 0.01
	private final double _epss;  // default 0.001
	private final double _qnot;  // default 1.e-6
	
	/**
	 * by default, 8 tasks to be submitted each time to be processed in parallel
	 */
	private final int _batchSz;

	
	private ArrayList _tis;      // used for visualization purposes
	private ArrayList _ctis;     // again, for visualization purposes only
	private ArrayList _lbtis;    // guess for what purposes this is...
	
	private int _numRunning = 0;

	
	/**
	 * sole public constructor.
	 * @param server String; default localhost
	 * @param port int; default 7891
	 * @param batchSz int; default 8
	 * @param epsT double; default 0.01
	 * @param epss double; default 0.001
	 * @param qnot double; default 1.e-6
	 */
	public sSTCnormOpt(String server, int port, int batchSz, 
		                  double epsT, double epss, double qnot) {
		if (server==null || server.length()==0) _pdsrv = "localhost";
		else _pdsrv = server;
		if (port>1024) _pdport = port;
		else _pdport = 7891;
		_batchSz = (batchSz>0) ? batchSz : 8;
		_epsT = epsT>0 ? epsT : 0.01;
		_epss = epss>0 ? epss : 0.001;
		_qnot = qnot>=0 ? qnot : 1.e-6;
		_tis = new ArrayList();
		_ctis = new ArrayList();
		_lbtis = new ArrayList();
	}

	
	/**
	 * no-op.
	 * @param p HashMap unused 
	 */
	public void setParams(HashMap p) {
		// no-op.
	}

	
	/**
	 * main class method.
	 * @param f FunctionIntf must be instance of sSTCnorm
	 * @return PairObjDouble
	 * @throws OptimizerException 
	 */
	public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (!(f instanceof sSTCnorm))
			throw new OptimizerException("sSTCnormOpt.minimize(f): f must be "+
				                           "function of type "+
				                           "tests.sic.sST.norm.sSTCnorm");
		Messenger mger = Messenger.getInstance();
		synchronized (this) {
			while (_numRunning > 0) {
				try {
					wait();  // don't proceed while other threads are running minimize()
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			if (_pdclt==null) {
				mger.msg("sSTCnormOpt.minimize(f): connecting on "+_pdsrv+
					       " on port "+_pdport, 2);
				_pdclt = new PDBTExecInitedClt(_pdsrv, _pdport);
				try {
					_pdclt.submitInitCmd(new PDBTExecInitNoOpCmd());
					mger.msg("sSTCnormOpt.minimize(f): successfully sent init cmd", 2);
				}
				catch (Exception e) {
					e.printStackTrace();
					throw new OptimizerException("sSTCnormOpt.mainimize(f): clt failed "+
						                           "to submit empty init-cmd to network");
				}
			}
			_tis.clear();
			_ctis.clear();
			_lbtis.clear();
			++_numRunning;
		}
		sSTCnorm sstc = (sSTCnorm) f;
		
		double Tmin = Math.pow((3.5*sstc._sigma/sstc._mi),2.0);
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
				batch[i] = new sSTCnormFixedTOptTask(sstc,T,_qnot,_epss,c_cur_best);
			}
			try {
				mger.msg("sSTCnormOpt.minimize(): submit a batch of "+_batchSz+
					       " tasks to network for period length from "+Tstart+" up to "+T, 
					       2);
				Object[] res = _pdclt.submitWorkFromSameHost(batch);
				for (int i=0; i<res.length; i++) {
					sSTCnormFixedTOpterResult ri = (sSTCnormFixedTOpterResult) res[i];
					_tis.add(new Double(ri._T));  // add to tis time-series
					_ctis.add(new Double(ri._C));  // add to c(t)'s time-series
					_lbtis.add(new Double(ri._LB));  // add to lb(t)'s time-series
					if (ri._LB > c_cur_best) {  // done!
						mger.msg("sSTCnormOpt.minimize(f): for T="+ri._T+" LB@T="+ri._LB+
							       " c@T="+ri._C+" c*="+c_cur_best+"; done.", 2);
						done = true;
					}
					if (ri._C < c_cur_best) {
						s_star = ri._s;
						S_star = ri._S;
						t_star = ri._T;
						c_cur_best = ri._C;
						mger.msg("sSTCnormOpt.minimize(f): found new better soln at T="+
							       t_star+", c="+c_cur_best+" LB@T="+ri._LB, 1);
					}
				}				
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new OptimizerException("sSTCnormOpt.minimize(): failed to "+
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
		ArrayList[] result = new ArrayList[3];
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
			throw new Error("sSTCnormOpt.terminateConnection() "+
				              "failed?");
		}
	}
	
	
	/**
	 * invoke as 
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.rnqt.norm.sSTCnormOpt 
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
	 * [qnot(0)]
	 * [epst(0.01)]
	 * [epss(0.1)]
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
		double p2 = 0.0;
		if (args.length>7) p2 = Double.parseDouble(args[7]);
		String host = "localhost";
		if (args.length>8) host = args[8];
		int port = 7891;
		if (args.length>9) port = Integer.parseInt(args[9]);
		double minq = 0;
		if (args.length>10) minq = Double.parseDouble(args[10]);
		double epst = 0.01;
		if (args.length>11) epst = Double.parseDouble(args[11]);
		double epss = 0.1;
		if (args.length>12) epss = Double.parseDouble(args[12]);
		int bsize = 8;
		if (args.length>13) bsize = Integer.parseInt(args[13]);
		
		// 2. create function
		sSTCnorm f = new sSTCnorm(Kr,Ko,L,mi,sigma,h,p,p2);

		long start = System.currentTimeMillis();
		// 3. optimize function
		sSTCnormOpt ropter = new sSTCnormOpt(host, port, bsize, epst, epss, minq);
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
			plot_frame.setTitle("(s,S,T) Policy with Normal Demands Plot");
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
 * auxiliary class encapsulating the notion of optimizing an 
 * <CODE>sSTCnorm</CODE> function, with a fixed review period T. NOT part of 
 * the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class sSTCnormFixedTOptTask implements TaskObject {
	private final sSTCnorm _f;
	private final double _T;
	private final double _qnot;
	private final double _epss;
	private final double _curBest;
	
	public sSTCnormFixedTOptTask(sSTCnorm f, 
		                           double T, double qnot, double epss,
															 double curBest) {
		_f = f;
		_T = T;
		_epss = epss;
		_qnot = qnot;
		_curBest = curBest;
	}
	
	
	public Serializable run() {
		sSTCnormFixedTOpt opter = 
			new sSTCnormFixedTOpt(_T, _epss, _qnot);
		sSTCnormFixedTOpterResult res = null;
		try {
			PairObjTwoDouble p = opter.minimize(_f);
			double[] x = (double[]) p.getArg();
			res = new sSTCnormFixedTOpterResult(_T, x[0], x[1], 
				                                  p.getDouble(), 
				                                  p.getSecondDouble());
			return res;
		}
		catch (Exception e) {
			e.printStackTrace();
			return new FailedReply();
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
 * double values. Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class sSTCnormFixedTOpterResult implements Serializable {
	public final double _T;
	public final double _s;
	public final double _S;
	public final double _C;
	public final double _LB;
	
	public sSTCnormFixedTOpterResult(double T, double s, double S, 
		                               double c, double lb) {
		_T = T;
		_s = s;
		_S = S;
		_C = c;
		_LB = lb;
	}
}

