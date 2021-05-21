package tests.sic.sST.nbin;

// imports below needed for the plotting of the search
import java.util.ArrayList;
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
import org.jfree.util.ShapeUtilities;
import tests.sic.rnqt.nbin.RnQTCnbin;


/**
 * The class plots the partial sums <CODE>sum1</CODE> and <CODE>sum2</CODE> as 
 * more terms are added while computing the <CODE>sSTCnbin</CODE> function; 
 * alternatively, it will plot the value of the cost function as more terms are
 * added in both sums.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class sSTCnbinTermPlotter extends sSTCnbin {
	
	private ArrayList _sum1ns;
	private ArrayList _sum2ns;
	private ArrayList _valns;
	
	private boolean _doVals = false;
	
	/**
	 * used to skip some high values in plotting actual cost values.
	 */
	private final static double _maxAllowedValueRatio = 100.0;
	
	
	/**
	 * Function sole public constructor.
	 * @param Kr double
	 * @param Ko double
	 * @param L double
	 * @param lambda double
	 * @param p_l double
	 * @param h double
	 * @param p double 
	 */
	public sSTCnbinTermPlotter(double Kr, double Ko, double L, 
		                         double lambda, double p_l, 
									           double h, double p) {
		super(Kr, Ko, L, lambda, p_l, h, p);
		_sum1ns = new ArrayList();
		_sum2ns = new ArrayList();
		_valns = new ArrayList();
	}
	
	
	private ArrayList[] getTimeSeries() {
		ArrayList[] res = new ArrayList[3];
		res[0] = _sum1ns;
		res[1] = _sum2ns;
		res[2] = _valns;
		return res;
	}
	
	
	/**
	 * set the flag to indicate that we want a plot of the actual cost values vs
	 * the number of terms added in the two sums, not the partial sums sum1 and
	 * sum2 themselves.
	 * @param doVals boolean
	 */
	public void setPlotValues(boolean doVals) {
		_doVals = doVals;
	}
	
	
	/**
	 * the main method call clears out any previous sum-term values, and calls
	 * its parent's method.
	 * @param x Object  // double[]
	 * @param params HashMap unused
	 * @return double
	 */
	public double eval(Object x, java.util.HashMap params) {
		_sum1ns.clear();
		_sum2ns.clear();
		_valns.clear();
		double y = super.eval(x, params);
		if (_doVals) {  // compute the _valns list of values
			final double T = ((double[])x)[2];
			final int max_n = Math.max(_sum1ns.size(), _sum2ns.size());
			for (int i=0; i<max_n; i++) {
				// y = Kr/T + (Ko+nom)/(T*denom);
				double nom = 
					i >= _sum1ns.size() ? 
					  ((Double)_sum1ns.get(_sum1ns.size()-1)).doubleValue() : 
					  ((Double)_sum1ns.get(i)).doubleValue();
				double denom = 
					i >= _sum2ns.size() ? 
					  ((Double)_sum2ns.get(_sum2ns.size()-1)).doubleValue() : 
					  ((Double)_sum2ns.get(i)).doubleValue();
				double cv = _Kr/T+(_Ko+nom)/(T*denom);
				_valns.add(new Double(cv));
			}
		}
		return y;
	}
	
	
	/**
	 * not part of the public API. Over-rides the definition in class
	 * <CODE>sSTCnbin</CODE>.
	 * @param r int the re-order point
	 * @param R int the order-up-to point
	 * @param T double the review period length
	 * @param t double the lead-time 
	 * @param l double the mean demand-rate of the Negative Binomial distribution
	 * @param m double the mean lead-time demand
	 * @param IC double the holding cost rate
	 * @param phat double the back-orders cost rate
	 * @param eps double the threshold below which when the ratio of the last 
	 * <CODE>_numTerms</CODE> terms sum up to divided by the total number of terms
	 * added up so far falls, the computation of the infinite series stops
	 * @return double
	 */
	protected double sum1(int r, int R, double T, double t, 
		                  double l, double m, 
											double IC, double phat, double eps) {
		double y = 0;
		int n = 0;
		double last = 0;
		int count = 0;
		while (n < _maxAllowedSumTerms) {
			double sum = 0;
			int Rmr = R-r;
			for (int j=1; j<=Rmr; j++) {
				double term = RnQTCnbin.nbinnfoldconv(Rmr-j, _lambda*T, _p_l, n);
				if (Double.isNaN(term)) {
					String exc = "for mean="+(l*T)+" arg="+(Rmr-j)+
						           " nbin. "+n+"-fold conv is NaN?";
					throw new IllegalStateException(exc);
				}
				double term2 = H(r+j,T,t,l,_p_l,m,IC,phat);
				if (Double.isNaN(term2)) {
					String exc = "H(...) becomes NaN";
					throw new IllegalStateException(exc);
				}
				term *= term2;
				sum += term;
			}
			if (Double.isNaN(sum)) {
				String exc = "for n="+n+" sum is now NaN";
				throw new IllegalStateException(exc);
			}
			y += sum;
			last += sum;
			++n;
			_sum1ns.add(y);
			if (++count==_numTerms) {
				double ratio = Math.abs(last/y);
				if (n>_START_DISP_MOD_NUM && n%_DISP_MOD_NUM == 0) {  // debug
					_mger.msg("sSTCnbin.sum1(r="+r+",R="+R+",T="+T+"...,eps="+eps+
						        "): n="+n+" cur_val="+y+" ratio="+ratio, 3);
				}
				if (ratio < eps) break;
				else {
					//System.err.println("sum1: n="+n+" last="+last+" y="+y+" ratio="+ratio);
					count = 0;
					last = 0;
				}
			}
		}
		if (n==_maxAllowedSumTerms) {
			String exc = "sSTCnbin.sum1(r="+r+",R="+R+",T="+T+",rest_args): "+ 
				           "evaluation exceeded "+_maxAllowedSumTerms+" limit";
			throw new IllegalStateException(exc);
		}
		updateMaxAddedTermsInSum12(n);
		return y;
	}
	
	
	/**
	 * not part of the public API. Used to be private, but it needs to be 
	 * over-ridden here.
	 * @param r int the reorder point
	 * @param R int the order-up-to point
	 * @param T double the review period length
	 * @param l double the demand rate
	 * @param eps the same threshold as specified in <CODE>sum1()</CODE>, but for 
	 * this method.
	 * @return double
	 */
	protected double sum2(int r, int R, double T, double l, double eps) {
		double y = 0;
		int n = 1;
		double last = 0;
		int count = 0;
		while (n<_maxAllowedSumTerms) {
			double sum = 0;
			double prev_aux = Double.NaN;
			double prev_aux2 = Double.NaN;
			for (int j=1; j<=R-r; j++) {
				double aux = RnQTCnbin.nbinnfoldconv(R-r-j, l*T, _p_l, n-1);
				double aux2 = 0.0;
				if (Double.isNaN(aux)) {
					String exc = (n-1)+"-nbin-conv("+(R-r-j)+";"+l*T+","+_p_l+") is NaN";
					exc += " (j="+j+" prev_aux="+prev_aux+")";
					_mger.msg(exc, 0);
					//throw new IllegalStateException(exc);
				}
				else aux2 = n*RnQTCnbin.nbincdfcompl(j, l*T, _p_l);
				if (Double.isNaN(aux2)) {
					String exc = "for n="+n+" j="+j+" r="+r+" R="+R+" aux2 is NaN";
					_mger.msg(exc, 0);
					//throw new IllegalStateException(exc);					
				}
				double all = aux*aux2;
				if (Double.isNaN(all)) {
					// try one last time the log-exp trick
					all = n_nbinnfoldconv_nbincdfcompl(R-r-j, l*T, _p_l, n, j);
					if (!Double.isFinite(all)) {
						String exc = "for n="+n+" j="+j+" r="+r+" R="+R+
							           " aux="+aux+"*aux2="+aux2+
								         " is NaN; all="+all;
						exc += "prev_aux="+prev_aux+" prev_aux2="+prev_aux2;
						// can't help but throw here
						throw new IllegalStateException(exc);					
					}
				}
				prev_aux = aux;
				prev_aux2 = aux2;				
				sum += all;
				if (Double.isNaN(sum)) {
					String exc = "for n="+n+" sum is NaN";
					throw new IllegalStateException(exc);					
				}
			}
			y += sum;
			last += sum;
			++n;
			_sum2ns.add(y);
			if (++count==_numTerms) {
				double ratio = Math.abs(last/y);
				if (n>_START_DISP_MOD_NUM && n%_DISP_MOD_NUM == 0) {  // debug
					_mger.msg("sSTCnbin.sum2(r="+r+",R="+R+",T="+T+",l="+l+",eps="+eps+
						        "): n="+n+" cur_val="+y+" ratio="+ratio, 3);
				}
				if (ratio < eps) break;
				else {
					//System.err.println("sum2: n="+n+" last="+last+" y="+y+" ratio="+ratio);
					count = 0;
					last = 0;
				}
			}
		}
		if (n==_maxAllowedSumTerms) {
			String exc = "sSTCnbin.sum2(r="+r+",R="+R+",T="+T+",rest_args): "+ 
				           "evaluation exceeded "+_maxAllowedSumTerms+" limit";
			throw new IllegalStateException(exc);
		}
		updateMaxAddedTermsInSum12(n);
		return y;
	}

	
	/**
	 * invoke as 
	 * <CODE>java -cp &lt;classpath&gt; tests.sic.sST.poisson.sSTCnbinTermPlotter 
	 * &lt;s&gt; &lt;S&gt; &lt;T&gt;
	 * &lt;Kr&gt; &lt;Ko&gt; &lt;L&gt; &lt;&lambda;&gt; &lt;p<sub>l</sub>&gt; 
	 * &lt;h&gt; &lt;p&gt; [doVals(false)]</CODE>. 
	 * The constraints on the variables and parameters values are the same as in
	 * class <CODE>sSTCnbin</CODE>.
	 * @param args String[]
	 */
	public static void main(String[] args) {
		int s = Integer.parseInt(args[0]);
		int S = Integer.parseInt(args[1]);
		double T = Double.parseDouble(args[2]);
		double Kr = Double.parseDouble(args[3]);
		double Ko = Double.parseDouble(args[4]);
		double L = Double.parseDouble(args[5]);
		double m = Double.parseDouble(args[6]);
		double p_l = Double.parseDouble(args[7]);
		double h = Double.parseDouble(args[8]);
		double p = Double.parseDouble(args[9]);
		sSTCnbinTermPlotter cc = new sSTCnbinTermPlotter(Kr,Ko,L,m,p_l,h,p);
		boolean do_vals = false;
		if (args.length>10) {
			do_vals = Boolean.parseBoolean(args[10]);
			cc.setPlotValues(do_vals);
		}
		double[] x = new double[]{s,S,T};
		double val = cc.eval(x, null);
		System.out.println("y = "+val);
		// finally: now visualize results
		JFreeChart chart = null;
		if (!do_vals) {
			// plot the terms as functions of n
			ArrayList[] xyseries = cc.getTimeSeries();
			XYSeriesCollection xyc = new XYSeriesCollection();
			XYSeries ts1 = new XYSeries("Sum1");
			for (int i=0; i<xyseries[0].size(); i++) {
				ts1.add(new Double(i).doubleValue(), 
								((Double)xyseries[0].get(i)).doubleValue());
			}
			xyc.addSeries(ts1);
			XYSeries ts2 = new XYSeries("Sum2");
			for (int i=0; i<xyseries[1].size(); i++) {
				ts2.add(new Double(i).doubleValue(), 
								((Double)xyseries[1].get(i)).doubleValue());
			}
			xyc.addSeries(ts2);			
			chart = 
				ChartFactory.createXYLineChart(
					"S1/S2 as Function of n",
					"n",
					"Value",
					xyc,
					PlotOrientation.VERTICAL,
					true, true, false);
			// customize plot
			XYPlot plot = (XYPlot) chart.getPlot();
			XYLineAndShapeRenderer r = (XYLineAndShapeRenderer) plot.getRenderer();
			r.setSeriesShape(1, ShapeUtilities.createUpTriangle(5));
			//r.setSeriesShapesVisible(2, true);
			//r.setSeriesLinesVisible(2, false);
			Shape shape  = new Ellipse2D.Double(0,0,3,3);
			r.setSeriesShape(0, shape);
			r.setSeriesShapesVisible(0,true);
			r.setSeriesLinesVisible(0, false);
			//r.setSeriesLinesVisible(1, false);
			//r.setSeriesPaint(2, Color.DARK_GRAY);
			r.setSeriesShape(1, shape);
			r.setSeriesShapesVisible(1,true);
			r.setSeriesLinesVisible(1, false);
		}
		else {
			// plot the cost value as function of n
			ArrayList[] xyseries = cc.getTimeSeries();
			XYSeriesCollection xyc = new XYSeriesCollection();
			XYSeries ts1 = new XYSeries("Cost Values");
			double last_cv = 
				((Double)xyseries[2].get(xyseries[2].size()-1)).doubleValue();
			for (int i=0; i<xyseries[2].size(); i++) {
				double c = ((Double)xyseries[2].get(i)).doubleValue();
				if (c >= last_cv*_maxAllowedValueRatio) continue;
				ts1.add(new Double(i).doubleValue(), c);
			}
			xyc.addSeries(ts1);
			chart = 
				ChartFactory.createXYLineChart(
					"Cost Value as Function of n",
					"n",
					"Value",
					xyc,
					PlotOrientation.VERTICAL,
					true, true, false);
			// customize plot
			XYPlot plot = (XYPlot) chart.getPlot();
			XYLineAndShapeRenderer r = (XYLineAndShapeRenderer) plot.getRenderer();
			Shape shape  = new Ellipse2D.Double(0,0,3,3);
			r.setSeriesShape(0, shape);
			r.setSeriesShapesVisible(0,true);
			r.setSeriesLinesVisible(0, false);
		}
		
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
		plot_frame.setTitle("Partial Sums of (s,S,T) nominator/denominator");
		plot_frame.add(_GraphPanel);
		plot_frame.setLocationRelativeTo(null);
		plot_frame.pack();
		plot_frame.setVisible(true);						
	}
		
}

