package tests.sic.sST.poisson;

import cern.jet.random.Poisson;

/**
 * auxiliary class to make Poisson computations from the Colt library behave the
 * same as in MATLAB, at least for the purposes needed in this package.
 * NOT part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2019</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Poisson2 extends Poisson {
	public Poisson2(double mean) {
		super(mean,null);
	}
	
	public double pdf(int k) {
		if (k<=0) {
			if (Double.compare(super.mean,0.0)<=0) return 1.0;  // MATLAB does this
			else return 0.0;
		}
		return super.pdf(k);
	}
}
