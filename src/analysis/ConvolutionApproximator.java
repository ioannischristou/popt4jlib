package analysis;

import popt4jlib.*;
import java.util.*;


/**
 * approximates the convolution operator between two functions f and g, defined
 * as $$(f*g)(S) = \int_{-\infinity}^{+\infinity}f(S-x)g(x)dx$$. Notice that
 * this version of the class works using the (parallel) IntegralApproximatorMT,
 * and requires that both functions are smooth over the entire real line in the
 * variable of the convolution, and the integrand of the convolution as a
 * function of x, decays rapidly enough in both directions.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ConvolutionApproximator implements FunctionIntf {
  private HashMap _convParams;
  private IntegralApproximatorMT _integrator;
  private double _lowerLimit = -1.0;
  private double _upperLimit = 1.0;


  /**
   * public constructor. The two functions to be convoluted must have the same
   * number of variables. In case of multi-variate functions, the convolution
   * is computed around 1 variable only, around the same values for all the
   * other variables.
   * @param f FunctionIntf must accept as input <CODE>double[]</DOUBLE> or
   * <CODE>VectorIntf</CODE> objects.
   * @param g FunctionIntf must accept as input <CODE>double[]</DOUBLE> or
   * <CODE>VectorIntf</CODE> objects.
   * @param params HashMap the params table may contain the following pairs:
   * <li> &lt;"convolutionapproximator.integrationlowerlimit", Double l&gt; optional,
   * the initial lower limit of the infinite integral involved in the convolution,
   * default is -1.
   * <li> &lt;"convolutionapproximator.integrationupperlimit", Double u&gt; optional,
   * the initial upper limit of the infinite integral involved in the convolution,
   * default is +1.
   * <li> &lt;"convolutionapproximator.varindex", Integer i&gt; optional the index of
   * the variable to be convoluted for the two functions f and g, default is 0 (
   * i.e. the 1st variable)
   * <li> Also, the following integration-related params may be specified:
   * <li> &lt;"integralapproximator.eps", Double eps&gt; if present specifies the
   * required precision, default is 1.e-6,
   * <li> &lt;"integralapproximator.levelmax", Integer level&gt; if present specifies
   * maximum level of recursion in the Simpson method, default is
   * <CODE>Integer.MAX_VALUE</CODE>.
   * @throws IllegalArgumentException
   */
  public ConvolutionApproximator(FunctionIntf f, FunctionIntf g,
                                 HashMap params) throws IllegalArgumentException {
    if (params!=null) _convParams = new HashMap(params);
    try {
      Double llD = (Double) _convParams.get(
          "convolutionapproximator.integrationlowerlimit");
      if (llD != null) _lowerLimit = llD.doubleValue();
      Double ulD = (Double) _convParams.get(
          "convolutionapproximator.integrationupperlimit");
      if (ulD != null) _upperLimit = ulD.doubleValue();
      HashMap p = new HashMap();
      p.put("integralapproximatormt.maxnumthreads", new Integer(10));
      _integrator = new IntegralApproximatorMT(new InnerConvProdFunction(f, g),
                                               p);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("ConvolutionApproximator.<init> failed due to some argument passed in");
    }
  }


  /**
   * evaluates the convolution of the two functions f and g passed in the
   * constructor at the point <CODE>x[i]</CODE> where <CODE>i</CODE> is the
   * value of the key "convolutionapproximator.varindex" specified in
   * <CODE>params</CODE>.
   * @param x Object must be either a <CODE>double[]</CODE> or a
   * <CODE>VectorIntf</CODE> object (in package <CODE>popt4jlib</CODE>).
   * @param params HashMap may contain the following pairs:
	 * <ul>
   * <li> &lt;"convolutionapproximator.varindex", Integer i&gt; optional, where i must
   * be in <CODE>[0, x.length-1]</CODE> if <CODE>x</CODE> is a <CODE>double[]</CODE>
   * else in <CODE>[0,x.getNumCoords()-1]</CODE>, specifies the variable of the
   * convolution. Default is the first variable.
   * <li> &lt;"convolutionapproximator.eps", Double e&gt; optional, specifies the gap
   * percentage below which if two successive estimations of the double-infinite
   * convolution integral are found, the computation ends, default is 1.e-6.
	 * </ul>
   * @throws IllegalArgumentException
   * @return double
   */
  public double eval(Object x, HashMap params) throws IllegalArgumentException {
    double[] t = null;
    if (x instanceof VectorIntf) t = ((VectorIntf) x).getDblArray1();
    else if (x instanceof double[]) t = (double[]) x;
    else throw new IllegalArgumentException("cannot convert x to double[]");
    int cvi = 0;
    Integer cviI = (Integer) params.get("convolutionapproximator.varindex");
    if (cviI!=null) cvi = cviI.intValue();
    double eps = 1.e-6;
    Double epsD = (Double) params.get("convolutionapproximator.eps");
    if (epsD!=null && epsD.doubleValue()>0) eps = epsD.doubleValue();
    params.put("convolutionapproximator.S", new Double(t[cvi]));
    params.put("integralapproximator.a", new Double(_lowerLimit));
    params.put("integralapproximator.integrandvarindex", new Integer(cvi));
    t[cvi] = _upperLimit;
    double val = _integrator.eval(t, params);
    while (true) {
      // increase the width of integration by 20%
      double width = _upperLimit - _lowerLimit;
      double newlowerLimit = _lowerLimit - width/10.0;
      double newupperLimit = _upperLimit + width/10.0;
      params.put("integralapproximator.a", new Double(newlowerLimit));
      t[cvi] = _lowerLimit;
      double newvalleft = _integrator.eval(t, params);
      params.put("integralapproximator.a", new Double(_upperLimit));
      t[cvi] = newupperLimit;
      double newvalright = _integrator.eval(t, params);
      if (Math.abs(val)<1.e-100) {
        if (Math.abs(newvalleft + newvalright) < val) return val;
      }
      else {
        double gap = Math.abs( (newvalleft + newvalright) / val);
        if (gap < eps) return val;
        val += newvalleft+newvalright;
        _lowerLimit = newlowerLimit;
        _upperLimit = newupperLimit;
      }
    }
  }


  /**
   * return a statistic of the total calls the integrator has made to the
   * adaptive simpson procedure.
   * @return int
   */
  public int getIntegratorTotalCalls() {
    return _integrator.getTotalNumCalls();
  }
}


class InnerConvProdFunction implements FunctionIntf {
  private FunctionIntf _f;
  private FunctionIntf _g;


  public InnerConvProdFunction(FunctionIntf f, FunctionIntf g) {
    _f = f;
    _g = g;
  }


  public double eval(Object x, HashMap params) throws IllegalArgumentException {
    double[] t = null;
    if (x instanceof VectorIntf) t = ((VectorIntf) x).getDblArray1();
    else if (x instanceof double[]) t = (double[]) x;
    else throw new IllegalArgumentException("cannot convert x to double[]");
    try {
      int ivi = 0;
      Integer ii = (Integer) params.get("convolutionapproximator.varindex");
      if (ii!=null) ivi = ii.intValue();
      Double SD = (Double) params.get("convolutionapproximator.S");
      double S = SD.doubleValue();
      double[] t2 = new double[t.length];
      for (int i=0; i<t.length; i++) t2[i] = t[i];
      t2[ivi] = S-t[ivi];
      return _f.eval(t2, params)*_g.eval(t, params);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("problem evaluating the inner conv. product");
    }
  }
}

