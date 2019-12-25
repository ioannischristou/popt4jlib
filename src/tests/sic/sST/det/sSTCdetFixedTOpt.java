package tests.sic.sST.det;

import java.util.HashMap;
import popt4jlib.FunctionIntf;
import popt4jlib.OptimizerException;
import popt4jlib.OptimizerIntf;
import utils.PairObjDouble;

/**
 * optimizer class that computes the exact optimal cost for a single-echelon 
 * inventory system facing deterministic demand with known and constant demand
 * rate, under a periodic review policy (s,S,T). This is the limit of the normal
 * demands case, when the variance &sigma; tends to zero; in practice, when the
 * variance of the normal distribution becomes small enough, the results of the
 * stochastic and the deterministic system become indistinguishable.
 * Notice the following assumptions:
 * <ul>
 * <li> The lead-time is assumed zero.
 * <li> There are no integrality constraints on demand (direct consequence of 
 * the constant demand rate). If the demand rate is integral, then on integral
 * review periods, total demand will also be integer.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2019</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class sSTCdetFixedTOpt implements OptimizerIntf {
	private double _T;

	
	/**
	 * sole public constructor.
	 * @param T double the given review period
	 */
	public sSTCdetFixedTOpt(double T) {
		_T = T;
	}
	
	
	/**
	 * computes the optimal parameters for an (s,S,T) reorder-point, order-up-to
	 * periodic review policy of a single-echelon inventory system facing demands
	 * with known constant rate.
	 * @param f an instance of the function <CODE>EOQ</CODE>.
	 * @return PairObjDouble, where the object part is a double[] of the numbers
	 * S,N for the given _T.
	 * @throws OptimizerException
	 */
	public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (!(f instanceof EOQ)) 
			throw new OptimizerException("sSTCdetFixedTOpt.minimize(): called with "+
				                           "non-EOQ function?");
		EOQ eoq = (EOQ) f;
		double b0 = Math.sqrt(2.0*eoq._Ko/(eoq._a*eoq._h*eoq._m));
		double b0_over_T = b0 / _T;
		if (Double.compare(Math.round(b0_over_T), b0_over_T) == 0) {
			// b0_over_T is integral: should be same as (b0 / _T) % 1 == 0
			double N = 1;
			double S = eoq._a*eoq._m*_T;
			double y = Math.sqrt(2.0*eoq._Ko*eoq._h*eoq._a*eoq._m) + eoq._Kr / _T;
			double[] arg = new double[]{S,N};
			return new PairObjDouble(arg, y);
		}
		else {
			double Nlo = Math.floor(b0/_T);
			double Slo = Nlo*_T;
			double Nhi = Math.ceil(b0/_T);
			double Shi = Nhi*_T;
			double clo = eoq.eval(new Double(Slo), null);
			double chi = eoq.eval(new Double(Shi), null);
			if (clo < chi) {
				double[] arg = new double[]{Slo,Nlo};
				return new PairObjDouble(arg, clo + eoq._Kr/_T);
			}
			else {
				double[] arg = new double[]{Shi,Nhi};
				return new PairObjDouble(arg, chi + eoq._Kr/_T);
			}
		}
	}
	
	
	/**
	 * auxiliary class that helps with the computations of the enclosing outer
	 * class. Not part of the public API.
	 */
	static class EOQ implements FunctionIntf {
		private double _Kr;  // fixed review cost
		private double _Ko;  // fixed order cost
		private double _h;   // holding cost rate
		private double _p;   // backorders penalty cost rate
		private double _m;   // constant demand rate
		private double _a;   // the alpha-service ratio (p/(p+h))
		
		public EOQ(double Kr, double Ko, double m, double h, double p) {
			_Kr = Kr;
			_Ko = Ko;
			_m = m;
			_h = h;
			_p = p;
			_a = _p / (_h+_p);
		}
		
		
		public double eval(Object x, HashMap params) {
			try {
				double S = ((Double) x).doubleValue();
				return _Ko/S + _a*_h*_m*S/2.0;
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new IllegalArgumentException("x must be positive Double");
			}
		}
	}
	
	
	/**
	 * invoke as:
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.sST.det.sSTCdetFixedTOpt 
	 * &lt;Kr&gt; &lt;Ko&gt; &lt;&mu;&gt; &lt;h&gt; &lt;p&gt; &lt;T&gt;
	 * </CODE>.
	 * @param args 
	 */
	public static void main(String[] args) {
		double Kr = Double.parseDouble(args[0]);
		double Ko = Double.parseDouble(args[1]);
		double m = Double.parseDouble(args[2]);
		double h = Double.parseDouble(args[3]);
		double p = Double.parseDouble(args[4]);
		double T = Double.parseDouble(args[5]);
		long start = System.currentTimeMillis();
		EOQ eoq = new EOQ(Kr,Ko,m,h,p);
		sSTCdetFixedTOpt opterFixedT = new sSTCdetFixedTOpt(T);
		try {
			PairObjDouble res = opterFixedT.minimize(eoq);
			double[] arg = (double[]) res.getArg();
			double cost = res.getDouble();
			long dur = System.currentTimeMillis()-start;
			System.out.println("S="+arg[0]+", N="+arg[1]+", cost="+cost);
			System.out.println("Done in "+dur+" msecs");
		}
		catch (OptimizerException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}

