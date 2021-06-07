package tests.sic.rnqt.poisson;

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
//import utils.PairObjTwoDouble;
import utils.PairObjThreeDouble;
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
 * class implements an optimizer over ALL three variables of the (R,nQ,T)
 * policy, namely the reorder point R, the batch size Q and the review period T.
 * The system being optimized faces stochastic demands 
 * as described in the class <CODE>RnQTCpoisson</CODE>. The solution found is 
 * guaranteed to be the global optimum (subject to the step-size constraints for
 * each of the decision variables). The optimizer is a parallel/distributed
 * method, submitting tasks to optimize over the first two variables, for fixed 
 * review period T, where the review period T is increased from the Tmin value
 * it can take on (user-defined) up to a point where the lower-bound on the cost 
 * function (namely the cost function with Ko=0) strictly exceeds the best known 
 * cost.
 * Notice that the program produces a visualization of the curve C*(T) for all T
 * it tries. Also, the same RnQTCpoissonOpt object cannot simultaneously
 * run many different optimization tasks (the method <CODE>minimize()</CODE>
 * will wait until there are no threads running the same method of the same
 * object before it starts running itself.) This is because we now need to keep
 * track of the series of the T-values tried, and their corresponding costs so
 * we can visualize them in the main program execution.
 * <p>Notes:
 * <ul>
 * <li>2021-06-07: added Tmax feature to let the optimization run up to a user
 * defined Tmax limit regardless of the lower bounds.
 * <li>2021-06-07: modified the lower bound to exclude the review costs since 
 * they are decreasing in time.
 * <li>2021-04-10: modified <CODE>setParams()</CODE> method to allow for setting
 * the pdclient object; needed when running parallel heuristic (s,S,T) 
 * optimizers.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RnQTCpoissonOpt implements OptimizerIntf {
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
	
	private final double _Tmax;
	
	/**
	 * by default, 8 tasks to be submitted each time to be processed in parallel
	 */
	private final int _batchSz;

	
	private ArrayList _tis;      // used for visualization purposes
	private ArrayList _ctis;     // again, for visualization purposes only
	private ArrayList _lbtis;    // guess for what purposes this is...
	private ArrayList _heurtis;  // or this...
	
	private int _numRunning = 0;

	
	/**
	 * sole public constructor.
	 * @param server String; default localhost
	 * @param port int; default 7891
	 * @param batchSz int &gt;0; default 8
	 * @param epsT double &gt;0; default 0.01
	 * @param Tnot double &gt;0; default 0.01
	 * @param Tmax double; default -1.0 indicating no time limit to reach; else
	 * the value specified will be reached regardless of lower bounds
	 */
	public RnQTCpoissonOpt(String server, int port, int batchSz, 
		                  double epsT, double Tnot, double Tmax) {
		if (server==null || server.length()==0) _pdsrv = "localhost";
		else _pdsrv = server;
		if (port>1024) _pdport = port;
		else _pdport = 7891;
		_batchSz = (batchSz>0) ? batchSz : 8;
		_epsT = epsT>0 ? epsT : 0.01;
		_Tnot = Tnot>0 ? Tnot : 0.01;
		_Tmax = Tmax;
		_tis = new ArrayList();
		_ctis = new ArrayList();
		_lbtis = new ArrayList();
		_heurtis = new ArrayList();
	}
	
	
	/**
	 * set the <CODE>_pdclt</CODE> client if one exists in the parameters passed 
	 * in. Notice that the method is synchronized and will wait while any other 
	 * thread is running the <CODE>minimize()</CODE> method. In general, this 
	 * method should only be called PRIOR to calling the <CODE>minimize()</CODE>
	 * main method.
	 * @param p HashMap may contain a key-value pair of the form 
	 * &lt;"rnqtcpoissonopt.pdclt", PDBTExecInitedClt clt&gt;
	 */
	public synchronized void setParams(HashMap p) {
		while (_numRunning > 0) {
			try {
				wait();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		if (p!=null && p.containsKey("rnqtcpoissonopt.pdclt")) {
			_pdclt = (PDBTExecInitedClt) p.get("rnqtcpoissonopt.pdclt");
		}
	}
	
	
	/**
	 * main class method.
	 * @param f FunctionIntf must be of type RnQTCpoisson
	 * @return PairObjDouble Pair&lt;double[] bestx, double bestcost&gt; where the
	 * bestx array contains the values (r*,Q*,T*)
	 * @throws OptimizerException 
	 */
	public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (!(f instanceof RnQTCpoisson))
			throw new OptimizerException("RnQTCpoissonOpt.minimize(f): f must be "+
				                           "function of type tests.sic.RnQTCpoisson");
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
				mger.msg("RnQTCpoissonOpt.minimize(f): connecting on "+_pdsrv+
					       " on port "+_pdport, 2);
				_pdclt = new PDBTExecInitedClt(_pdsrv, _pdport);
				try {
					_pdclt.submitInitCmd(new PDBTExecInitNoOpCmd());
					mger.msg("RnQTCpoissonOpt.minimize(f): successfully sent init cmd",2);
				}
				catch (Exception e) {
					e.printStackTrace();
					throw new OptimizerException("RnQTCpoissonOpt.mainimize(f): clt "+
						                           "failed to submit empty init-cmd to "+
						                           "network");
				}
			}
			_tis.clear();
			_ctis.clear();
			_lbtis.clear();
			_heurtis.clear();
			++_numRunning;
		}
		RnQTCpoisson rnqtc = (RnQTCpoisson) f;
		
		double Tmin = _Tnot;
		double c_cur_best = Double.POSITIVE_INFINITY;
		
		double r_star=Double.NaN;
		double q_star = Double.NaN;
		double t_star = Double.NaN;
		
		double T = Tmin;
		
		boolean done = false;
		
		while (!done) {
			// 1. prepare batch
			TaskObject[] batch = new TaskObject[_batchSz];
			double Tstart = T;
			for (int i=0; i<_batchSz; i++) {
				T += _epsT;
				batch[i] = new RnQTCpoissonFixedTOptTask(rnqtc,T,1,1,1,c_cur_best);
			}
			if (_Tmax > 0 && T >= _Tmax) done = true;
			try {
				mger.msg("RnQTCpoissonOpt.minimize(): submit a batch of "+_batchSz+
					       " tasks to network for period length from "+Tstart+" up to "+T, 
					       2);
				Object[] res = _pdclt.submitWorkFromSameHost(batch);
				for (int i=0; i<res.length; i++) {
					RnQTCpoissonFixedTOpterResult ri = 
						(RnQTCpoissonFixedTOpterResult) res[i];
					_tis.add(new Double(ri._T));  // add to tis time-series
					_ctis.add(new Double(ri._C));  // add to c(t)'s time-series
					_lbtis.add(new Double(ri._LB));  // add to lb(t)'s time-series
					// also, compute the value of the heuristic:
					double heur = ri._OC;
					_heurtis.add(new Double(heur));
					if (Double.compare(ri._LB, c_cur_best)>0) {  // done!
						mger.msg("RnQTCpoissonOpt.minimize(f): for T="+ri._T+" LB@T="+
							       ri._LB+" c@T="+ri._C+" c*="+c_cur_best+"; done.", 2);
						if (_Tmax < 0) done = true;
					}
					if (Double.compare(ri._C, c_cur_best)<0) {
						r_star = ri._R;
						q_star = ri._Q;
						t_star = ri._T;
						c_cur_best = ri._C;
						mger.msg("RnQTCpoissonOpt.minimize(f): found new better soln at T="+
							       t_star+", c="+c_cur_best+" LB@T="+ri._LB, 1);
					}
				}				
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new OptimizerException("RnQTCpoissonOpt.minimize(): failed to "+
					                           "submit tasks/process/get back results");
			}
		}
		synchronized(this) {
			if (--_numRunning==0) {
				notify();  // let another thread know that it can proceed with 
					         // client termination
			}
		}
		double[] x = new double[]{r_star,q_star,t_star};
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
		result[3] = _heurtis;
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
			throw new Error("RnQTCpoissonOpt.terminateServerConnection() "+
				              "failed?");
		}
	}
	
	
	/**
	 * invoke as 
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.rnqt.nbin.RnQTCpoissonOpt 
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
	 * [Tmax(-1.0)]
	 * [dbglvl(0)]
	 * </CODE>.
	 * @param args String[] 
	 */
	public static void main(String[] args) {
		// 1. parse inputs
		double Kr = Double.parseDouble(args[0]);
		double Ko = Double.parseDouble(args[1]);
		double L = Double.parseDouble(args[2]);
		double lambda = Double.parseDouble(args[3]);
		double h = Double.parseDouble(args[4]);
		double p = Double.parseDouble(args[5]);
		double p2 = 0.0;
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
		double Tmax = -1.0;
		if (args.length>12) Tmax = Double.parseDouble(args[12]);
		int dbglvl = 0;
		final Messenger mger = Messenger.getInstance();
		if (args.length>13) dbglvl = Integer.parseInt(args[13]);
		mger.setDebugLevel(dbglvl);
		
		// 2. create function
		RnQTCpoisson f = new RnQTCpoisson(Kr,Ko,L,lambda,h,p,p2);

		long start = System.currentTimeMillis();
		// 3. optimize function
		RnQTCpoissonOpt ropter = new RnQTCpoissonOpt(host, port, bsize, 
			                                           epst, tnot, Tmax);
		try {
			PairObjDouble result = ropter.minimize(f);
			long dur = System.currentTimeMillis()-start;
			double[] x = (double[])result.getArg();
			double c = result.getDouble();
			System.out.println("R*="+x[0]+" Q*="+x[1]+" T*="+x[2]+" ==> C*="+c);
			System.out.println("run-time="+dur+" msecs");
			ropter.terminateServerConnection();

			// finally: now visualize results
			ArrayList[] xyseries = ropter.getLatestTimeSeries();
			XYSeriesCollection xyc = new XYSeriesCollection();
			XYSeries tcs = new XYSeries("C*");
			XYSeries tlbs = new XYSeries("LB");
			XYSeries heurs = new XYSeries("Heur");
			for (int i=0; i<xyseries[1].size(); i++) {
				tcs.add(((Double)xyseries[0].get(i)).doubleValue(), 
					      ((Double)xyseries[1].get(i)).doubleValue());
			}
			for (int i=0; i<xyseries[2].size(); i++) {
				tlbs.add(((Double)xyseries[0].get(i)).doubleValue(), 
					      ((Double)xyseries[2].get(i)).doubleValue());				
			}
			for (int i=0; i<xyseries[3].size(); i++) {
				heurs.add(((Double)xyseries[0].get(i)).doubleValue(), 
					      ((Double)xyseries[3].get(i)).doubleValue());				
			}
			xyc.addSeries(tcs);
			xyc.addSeries(tlbs);
			xyc.addSeries(heurs);
			JFreeChart chart = 
				ChartFactory.createXYLineChart(
					"Optimal (r,nQ,T) Cost as Function of T",
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
			r.setSeriesPaint(2, Color.DARK_GRAY);
			
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
			plot_frame.setTitle("(r,nQ,T) Policy with Poisson Demand Plot");
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
 * <CODE>RnQTCpoisson</CODE> function, with a fixed review period T. NOT part of 
 * the public API. Notice that this class together with the similarly named but
 * ending with "Result" class are not nested inside the main class 
 * <CODE>RnQTCnbinHeurPOpt</CODE> because the outer class contains the 
 * <CODE>_pdclt</CODE> object reference which cannot (and must not) be 
 * serializable.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class RnQTCpoissonFixedTOptTask implements TaskObject {
	private RnQTCpoisson _f;
	private double _T;
	private double _curBest;
	
	public RnQTCpoissonFixedTOptTask(RnQTCpoisson f, 
		                               double T, int epsQ, int qnot, int epsR,
																   double curBest) {
		_f = f;
		_T = T;
		_curBest = curBest;
	}
	
	
	public Serializable run() {
		RnQTCpoissonFixedTOpt opter = new RnQTCpoissonFixedTOpt(_T, _curBest);
		RnQTCpoissonFixedTOpterResult res = null;
		try {
			PairObjThreeDouble p = opter.minimize(_f);
			double[] x = (double[]) p.getArg();
			res = new RnQTCpoissonFixedTOpterResult(_T, x[0], x[1], 
				                                      p.getDouble(), 
				                                      p.getSecondDouble(),
			                                        p.getThirdDouble());
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
final class RnQTCpoissonFixedTOpterResult implements Serializable {
	public final double _T;
	public final double _R;
	public final double _Q;
	public final double _C;
	public final double _LB;
	public final double _OC;  // the heuristic total cost
	
	
	public RnQTCpoissonFixedTOpterResult(double T, double R, double Q, 
		                                   double c, double lb, double oc) {
		_T = T;
		_Q = Q;
		_R = R;
		_C = c;
		_LB = lb;
		_OC = oc;
	}
	
}

