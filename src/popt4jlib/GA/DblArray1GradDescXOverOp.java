package popt4jlib.GA;

import popt4jlib.*;
import analysis.*;
import utils.*;
import java.util.*;

/**
 * implements an experimental cross-over operator (that is quite time-consuming
 * in practice) for chromosomes represented as <CODE>double[]</CODE> objects.
 * Uses ideas borrowed from inexact line-search algorithms for NLP (in fact,
 * implements variants of so-called "backtracking line-search" for step-size
 * determination.)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1GradDescXOverOp implements XoverOpIntf {
  static int _numFailed=0;
  static int _numSuccessful=0;

  /**
   * sole public constructor (no-op body).
   */
  public DblArray1GradDescXOverOp() {
  }


  /**
   * implements the X-over operator between two double[] objects, returning
   * new double[] objects.
   * @param c1 Object double[]
   * @param c2 Object double[]
   * @param params Hashtable must contain the following key-value pairs:
	 * <ul>
   * <li> &lt"dga.minallelevalue", $value$&gt optional, the minimum value for
   * any allele in the chromosome. Default -Infinity.
   * <li> &lt"dga.minallelevalue$i$", $value$&gt optional, the minimum value for
   * the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}. If this value is less than the global value
   * specified by the "dga.minallelevalue" key, it is ignored.
   * <li> &lt"dga.maxallelevalue", $value$&gt optional, the maximum value for
   * any allele in the chromosome. Default +Infinity.
   * <li> &lt"dga.maxallelevalue$i$", $value$&gt optional, the maximum value for
   * the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}. If this value is greater than the global value
   * specified by the "dga.maxallelevalue" key, it is ignored.
   * <li> &lt"thread.id",$integer_value"&gt mandatory, the (internal) id of the
   * thread invoking this method; this number is used so as to look-up the right
   * random-number generator associated with the current thread.
   * <li> &lt"dga.function", FunctionIntf f&gt mandatory, the function to be
   * optimized.
   * <li> &lt"dga.gradient", VecFunctionIntf g&gt optional, the gradient of the
   * function to be optimized (if null, then the
   * <CODE>analysis.GradApproximator</CODE> class will be used). Default is null.
   * <li> &lt"dga.graddescxoverop.a", $double_value$&gt optional. Default 0.25.
   * <li> &lt"dga.graddescxoverop.b", $double_value$&gt optional. Default 0.8.
   * <li> &lt"dga.graddescxoverop.maxtries", $integer_value$&gt optional.
   * Default 100.
   * <li> &lt"dga.graddescxoverop.minaccvaldiffperc", $double_value$&gt optional. Default 0.01.
   * <li> &lt"dga.graddescxoverop.maxinittries", $integer_value$&gt optional.
   * Default 20.
	 * </ul>
   * @throws OptimizerException if any of the params is not set correctly or if
	 * the process otherwise fails
   * @return Pair Pair&ltdouble[], double[]&gt
   */
  public Pair doXover(Object c1, Object c2, Hashtable params) throws OptimizerException {
    try {
      final double[] a1 = (double[]) c1;
      final double[] a2 = (double[]) c2;
      FunctionIntf f = (FunctionIntf) params.get("dga.function");
      VecFunctionIntf grad = (VecFunctionIntf) params.get("dga.gradient");
      if (grad==null) grad = new GradApproximator(f);
      double v1 = f.eval(a1, params);
      VectorIntf x1 = new DblArray1Vector(a1);
      VectorIntf g1 = grad.eval(x1, params);
      double v2 = f.eval(a2, params);
      VectorIntf x2 = new DblArray1Vector(a2);
      VectorIntf g2 = grad.eval(x2, params);
      final int n = a1.length;
      if (diffIsOK(v1,v2,params)) {
        double[] s = new double[n];
        if (v2 > v1) {
          for (int i=0; i<n; i++) s[i] = a1[i] - a2[i];
        } else {
          for (int i=0; i<n; i++) s[i] = a2[i] - a1[i];
        }
        // normalize s to be a descent direction
        double len = 0;
        for (int i=0; i<n; i++) len += s[i]*s[i];
        len = Math.sqrt(len);
        for (int i=0; i<n; i++) s[i] /= len;
        // create offspring
        double[] off1 = new double[n];
        double[] off2 = new double[n];
        // implement backtracking line-search to figure out the step-size
        double a = 0.25;  // acceptable values in (0,0.5), usually in [0.01,0.3]
        Double aD = (Double) params.get("dga.graddescxoverop.a");
        if (aD!=null &&
            aD.doubleValue()>0 &&
            aD.doubleValue()<0.5)
          a = aD.doubleValue();
        double b = 0.8;  // acceptable values in (0,1), usually 0.8
        Double bD = (Double) params.get("dga.graddescxoverop.b");
        if (bD!=null && bD.doubleValue()>0 && bD.doubleValue()<1)
          b = bD.doubleValue();
        // work with a1 -> off1
        double t = findFeasiblePoint(a1, s, b, params);
        if (t>0) {
          int maxtries = 100;
          Integer mtI = (Integer) params.get("dga.graddescxoverop.maxtries");
          if (mtI!=null) maxtries = mtI.intValue();
          // compute <g1,s> innerProduct
          double ip = 0.0;
          for (int j=0; j<n; j++) {
            double tj = g1.getCoord(j) * s[j];
            ip += (tj<0 ? tj : 0);
          }
          boolean found = false;
          for (int i=0; i<maxtries && !found; i++) {
            for (int j=0; j<n; j++) off1[j] = a1[j]+t*s[j];
            if (f.eval(off1, params) <= v1 + a*t*ip)
              found = true;
            else t = b*t;
          }
          if (!found) {
            incrFailed();
            for (int j=0; j<n; j++) off1[j] = a1[j];
          } else incrSuccessful();
        }
        else {
          incrFailed();
        }
        // work with a2 -> off2
        t = findFeasiblePoint(a2, s, b, params);
        if (t>0) {
          int maxtries = 100;
          Integer mtI = (Integer) params.get("dga.graddescxoverop.maxtries");
          if (mtI!=null) maxtries = mtI.intValue();
          // compute <g2,s> innerProduct
          double ip = 0.0;
          for (int j=0; j<n; j++) {
            double tj = g2.getCoord(j) * s[j];
            ip += (tj < 0 ? tj : 0);
          }
          boolean found = false;
          for (int i=0; i<maxtries && !found; i++) {
            for (int j=0; j<n; j++) off2[j] = a2[j]+t*s[j];
            if (f.eval(off2, params) <= v2 + a*t*ip)
              found = true;
            else t = b*t;
          }
          if (!found) {
            incrFailed();
            for (int j=0; j<n; j++) off2[j] = a2[j];
          } else incrSuccessful();
        }
        else {
          incrFailed();
        }

        Pair p = new Pair(off1, off2);
        return p;
      }
      else { // do std 1-pt cross-over
        // create offspring
        double[] off1 = new double[n];
        double[] off2 = new double[n];
        final int id = ( (Integer) params.get("thread.id")).intValue();
        int xoverind = RndUtil.getInstance(id).getRandom().nextInt(a1.length);
        for (int i = 0; i < n; i++) {
          if (i <= xoverind) {
            off1[i] = a1[i];
            off2[i] = a2[i];
          }
          else {
            off1[i] = a2[i];
            off2[i] = a1[i];
          }
        }
        return new Pair(off1, off2);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("doXover(): failed");
    }
  }


  private synchronized static void incrFailed() {
    ++_numFailed;
    if (_numFailed % 100 == 0) {
      int ok = getNumSuccessful();
      Messenger.getInstance().msg("DblArray1GradDescXOverOp: XOvers OK: "+ok+" Failed: "+_numFailed,1);
    }
  }
  private synchronized static void incrSuccessful() { ++_numSuccessful; }
  private synchronized static int getNumSuccessful() { return _numSuccessful; }


  private boolean diffIsOK(double v1, double v2, Hashtable params) {
    double minaccperc = 0.01;
    Double mapD = (Double) params.get("dga.graddescxoverop.minaccvaldiffperc");
    if (mapD!=null && mapD.doubleValue()>0)
      minaccperc = mapD.doubleValue();
    if (Math.min(v1,v2)==0) return Math.max(v1,v2)>minaccperc;  // avoid division by zero
    return Math.abs((v1-v2)/Math.min(v1,v2)) > minaccperc;
  }


  private double findFeasiblePoint(double[] a, double[] s, double b, Hashtable params) {
    double t = 1;
    double minargval = Double.NEGATIVE_INFINITY;
    Double mavD = (Double) params.get("dga.minallelevalue");
    if (mavD!=null) minargval = mavD.doubleValue();
    double maxargval = Double.POSITIVE_INFINITY;
    Double MavD = (Double) params.get("dga.maxallelevalue");
    if (MavD!=null) maxargval = MavD.doubleValue();
    int maxtries = 20;
    Integer mtI = (Integer) params.get("dga.graddescxoverop.maxinittries");
    if (mtI!=null) maxtries = mtI.intValue();
    final int n = s.length;
    for (int i=0; i<maxtries; i++) {
      t *= b;
      boolean ok = true;
      for (int j=0; j<n && ok; j++) {
        // compute xj limits
        double minaj = minargval;
        Double majD = (Double) params.get("dga.minallelevalue"+j);
        if (majD!=null && majD.doubleValue()>minaj) minaj = majD.doubleValue();
        double maxaj = maxargval;
        Double MajD = (Double) params.get("dga.maxallelevalue"+j);
        if (MajD!=null && MajD.doubleValue()<maxaj) maxaj = MajD.doubleValue();
        double xj = a[j]+t*s[j];
        if (minaj > xj || xj > maxaj) ok = false;
      }
      if (ok) return t;
    }
    return -1;  // indicate failure
  }

}
