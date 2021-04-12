package analysis;

import popt4jlib.FunctionIntf;
import utils.PairIntInt;

import java.util.HashMap;
import tests.sic.rnqt.nbin.RnQTCnbin;


/**
 * class implements the n-fold convolution of any discrete mass probability 
 * function, for pmfs that are non-zero only in the non-negative integers, using 
 * the recursive definition of n-fold convolution defined in 
 * Hadley and Whitin (1963): Analysis of Inventory Systems, page 121.
 * The class uses the <CODE>popt4jlib.FiniteLRUFunctionCache</CODE> class to 
 * speed up computations.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class MultiFoldPMFConvolution implements FunctionIntf {
	private FunctionIntf _pmf;
	
	/**
	 * sole public constructor.
	 * @param f FunctionIntf the pmf to get its n-fold convolution at any k&ge;0
	 * that must accept as input Integer objects
	 */
	public MultiFoldPMFConvolution(FunctionIntf f) {
		_pmf = f;
	}
		

	/**
	 * implements n-fold convolution, much faster than a recursive definition.
	 * @param arg Object PairIntInt // (k, n)
	 * @param params HashMap
	 * @return double
	 */
	public double eval(Object arg, HashMap params) {
		final PairIntInt args = (PairIntInt) arg;
		final int k = args.getFirst();
		final int n = args.getSecond();
		if (k<0) return 0.0;  // by requirement of pmfs accepted for this class
		if (n==1) return _pmf.eval(new Integer(k), params);
		// n>1
		double[] p1s = new double[k+1];
		for (int y=0; y<=k; y++) p1s[y] = _pmf.eval(new Integer(y), params);
		double pmm1s[] = new double[k+1];
		for (int i=0; i<=k; i++) pmm1s[i] = p1s[i];
		double pms[] = new double[k+1];
		for (int m=2; m<=n; m++) {
			for (int x=0; x<=k; x++) {
				pms[x] = 0;
				for (int y=0; y<=x; y++) {
					pms[x] += pmm1s[y]*p1s[x-y];
				}
			}
			for (int x=0; x<=k; x++) pmm1s[x] = pms[x];
		}
		return pms[k];
	}
	
		
	/**
	 * test-driver program tests the class by computing n-fold convolution of the
	 * Negative Binomial distribution. Invoke as:
	 * <CODE>
	 * java -cp &lt;classpath&gt; analysis.MultifoldPMFConvolution 
	 * &lt;lambda&gt; 
	 * &lt;pl&gt;
	 * &lt;k&gt;
	 * &lt;n&gt;
	 * </CODE>. 
	 * The program will return the value p<sup>(n)</sup>(k;&lambda;,p<sub>l</sub>)
	 * @param args String[]
	 */
	public static void main(String[] args) {
		double lambda = Double.parseDouble(args[0]);
		double pl = Double.parseDouble(args[1]);
		int k = Integer.parseInt(args[2]);
		int n = Integer.parseInt(args[3]);
		FunctionIntf nbin_pmf = new NBinPMF(lambda, pl);
		MultiFoldPMFConvolution pmf_conv = new MultiFoldPMFConvolution(nbin_pmf);
		long st = System.currentTimeMillis();
		double val = pmf_conv.eval(new PairIntInt(k,n), null);
		long dur = System.currentTimeMillis()-st;
		System.out.println("nbin^{("+n+")}("+k+";"+lambda+","+pl+")="+val+
			                 " in "+dur+" msecs.");
		// compute the real conv. value
		double real_val = RnQTCnbin.nbinnfoldconv(k, lambda, pl, n);
		double gap_perc = 100*(val-real_val)/real_val;
		System.out.println("real value="+real_val+" gap%="+gap_perc);
	}

}


/**
 * auxiliary test function not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class PoissonPMF implements FunctionIntf {
	private double _l;

	
	public PoissonPMF(double lambda) {
		_l = lambda;
	}
	
	
	public double eval(Object arg, HashMap params) {
		Integer k = (Integer) arg;
		double fac = 1.0;
		for (int i=1; i<=k; i++) fac *= i;
		return Math.exp(-_l)*Math.pow(_l, k) / fac;
	}
}


/**
 * auxiliary test function not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class NBinPMF implements FunctionIntf {
	private double _l;
	private double _pl;

	
	public NBinPMF(double lambda, double pl) {
		_l = lambda;
		_pl = pl;
	}
	
	
	public double eval(Object arg, HashMap params) {
		Integer k = (Integer) arg;
		return RnQTCnbin.nbinpdf(k.intValue(), _l, _pl);
	}
}
