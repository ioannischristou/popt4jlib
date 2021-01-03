package analysis;

import popt4jlib.*;
import java.util.*;

/**
 * Performs numerical integration in one dimension using the Adaptive Simpson
 * Procedure.
 * <p>Notes:
 * <ul>
 * <li>2021-01-02: improved the <CODE>simpson()</CODE> method that is the heart 
 * of the numerical interation procedure.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.1
 */
public class IntegralApproximator implements FunctionIntf {
  private FunctionIntf _f;
  private double _eps = 1.e-6;
  private int _maxLvl = Integer.MAX_VALUE;

  /**
   * public constructor.
   * @param f FunctionIntf must accept VectorIntf objects as argument
   * @param params HashMap may contain the following pairs:
	 * <ul>
   * <li> &lt;"integralapproximator.eps", Double eps&gt; if present specifies 
	 * the required precision, default is 1.e-6,
   * <li> &lt;"integralapproximator.levelmax", Integer level&gt; if present 
	 * specifies the maximum level of recursion in the Simpson method, default is
   * <CODE>Integer.MAX_VALUE</CODE>.
	 * </ul>
   */
  public IntegralApproximator(FunctionIntf f, HashMap params) {
    _f = f;
    if (params!=null) {
      try {
        Double epsD = (Double) params.get("integralapproximator.eps");
        if (epsD != null) _eps = epsD.doubleValue();
      }
      catch (Exception e) {
        e.printStackTrace(); // no-op
      }
      try {
        Integer mlI = (Integer) params.get("integralapproximator.levelmax");
        if (mlI != null) _maxLvl = mlI.intValue();
      }
      catch (Exception e) {
        e.printStackTrace(); // no-op
      }
    }
  }


  /**
   * computes the integral with respect to the i-th variable of the function
   * provided in the constructor, from a lower limit a defined in the params
   * HashMap, to the specified value x_i in the vector <CODE>x</CODE>.
   * @param x Object a VectorIntf or double[] object.
   * @param params HashMap must contain at least a pair of the form
   * &lt;"integralapproximator.a", Double a&gt; indicating the lower limit of
   * integration and a pair of the form
   * &lt;"integralapproximator.integrandvarindex", Integer i&gt; indicating the
   * variable index for the integration (must range in
   * [0,<CODE>x.length-1</CODE>] if <CODE>x</CODE> is a <CODE>double[]</CODE> or
   * in [0,<CODE>x.getNumCoords()-1</CODE>] if <CODE>x</CODE> is a
   * <CODE>VectorIntf</CODE> object). It may also contain a pair of the form
	 * &lt;"integralapproximator.num_pieces", Integer n&gt; indicating the number
	 * of equi-length pieces that the simpson adaptive procedure will break the 
	 * current integration interval if it is required to recursively call itself 
	 * on sub-intervals it. The larger this value, the slower the method may be,
	 * but it can save a lot on recursion calls that would otherwise be needed.
	 * Additionally, params must contain whatever parameters are needed so that 
	 * the function <CODE>f</CODE> passed in the constructor needs in order to be 
	 * evaluated.
   * @throws IllegalArgumentException if any of the two pairs required in params
   * HashMap are not present, or if the integration could not be carried out
   * with the required precision.
   * @return double
   */
  public double eval(Object x, HashMap params) throws IllegalArgumentException {
    VectorIntf t = null;
    try {
      if (x instanceof VectorIntf)
        t = ((VectorIntf) x).newCopy();  // work with a copy to guarantee no
                                         // issues with multiple threads.
      else t = new DblArray1Vector((double[]) x);
    }
    catch (Exception e) {
      throw new IllegalArgumentException("x cannot be converted to VectorIntf");
    }
    double a = 0;
    try {
      a = ((Double) params.get("integralapproximator.a")).doubleValue();
    }
    catch (Exception e) {
      throw new IllegalArgumentException("integralapproximator.a not well "+
				                                 "defined");
    }
    int ii = 0;
    try {
      ii = ((Integer) params.get("integralapproximator.integrandvarindex")).
				                       intValue();
    }
    catch (Exception e) {
      throw new IllegalArgumentException(
				          "integralapproximator.integrandvarindex not well defined");
    }
    try {
      return simpson(t, ii, a, t.getCoord(ii), 0, _eps, params);
    }
    catch (IntegrationException e) {
      throw new IllegalArgumentException("increase max level allowed");
    }
    catch (parallel.ParallelException e) {  // can never get here
      e.printStackTrace();
      return Double.NaN;  // just to stop the compiler from complaining:
                          // can never get here
    }
		finally {
			if (t instanceof PoolableObjectIntf) {
				((PoolableObjectIntf) t).release();
			}
		}
  }


  private double simpson(VectorIntf x, int intvarind, double a, double b,
                         int level, double eps,
                         HashMap p) throws IntegrationException, 
		                                       parallel.ParallelException {
    // never throws ParallelException since x is constructed locally in the
    // current thread and no other thread has access to it.
    if (level>_maxLvl) {
      throw new IntegrationException("Simpson Adaptive Procedure failed, "+
				                             "reached level="+level+
				                             " at interval ["+a+","+b+"] w/ eps="+eps);
    }
    double h = b-a;
    double c = (a+b)/2.0;
    x.setCoord(intvarind, a);
    double fa = _f.eval(x, p);
    x.setCoord(intvarind, b);
    double fb = _f.eval(x,p);
    x.setCoord(intvarind, c);
    double fc = _f.eval(x,p);
    double simpson1 = h*(fa+4*fc+fb)/6.0;
    double d = (a+c)/2.0;
    double e = (c+b)/2.0;
    x.setCoord(intvarind, d);
    double fd = _f.eval(x,p);
    x.setCoord(intvarind, e);
    double fe = _f.eval(x,p);
    double simpson2 = h*(fa+4*fd+2*fc+4*fe+fb)/12.0;
    if (Math.abs(simpson2-simpson1)<15*eps) return simpson2;
    else {
			/* 
		  // old code only breaks interval in two
      double epshalf = eps/2.0;
      double ls = simpson(x, intvarind, a, c, level+1, epshalf, p);
      double rs = simpson(x, intvarind, c, b, level+1, epshalf, p);
      return ls+rs;
		  */
			int num_pieces = 2;  // by default we break [a,b] in 2 pieces
			if (p.containsKey("integralapproximator.num_pieces")) {
				num_pieces = 
					((Integer)p.get("integralapproximator.num_pieces")).intValue();
			}
			final double psz = (b-a)/num_pieces;
			final double eps2 = eps/num_pieces;
			double res = 0.0;
			double xs = a;
			double xe = a+psz;
			for (int i=0; i<num_pieces; i++) {
				double ri = simpson(x, intvarind, xs, xe, level+1, eps2, p);
				res += ri;
				xs = xe;
				xe += psz;
			}
			return res;
    }
  }
}

