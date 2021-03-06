package popt4jlib.GA;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * yet another experimental hybrid x-over operator based on the concept of
 * descent, operating on chromosomes represented as <CODE>double[]</CODE>
 * objects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1HybridDescXOverOp implements XoverOpIntf {
  static int _numFailed=0;
  static int _numSuccessful=0;

  /**
   * sole public no-arg constructor (no-op body)
   */
  public DblArray1HybridDescXOverOp() {
  }


  /**
   * implements the x-over operator operating on <CODE>double[]</CODE> objects.
   * @param c1 Object double[]
   * @param c2 Object double[]
   * @param params HashMap should contain the following key-value pairs:
	 * <ul>
   * <li> &lt;"dga.minallelevalue", $value$&gt; optional, the minimum value for
   * any allele in the chromosome. Default -Infinity.
   * <li> &lt;"dga.minallelevalue$i$", $value$&gt; optional, the minimum value for
   * the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}. If this value is less than the global value
   * specified by the "dga.minallelevalue" key, it is ignored.
   * <li> &lt;"dga.maxallelevalue", $value$&gt; optional, the maximum value for
   * any allele in the chromosome. Default +Infinity.
   * <li> &lt;"dga.maxallelevalue$i$", $value$&gt; optional, the maximum value for
   * the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}. If this value is greater than the global value
   * specified by the "dga.maxallelevalue" key, it is ignored.
   * <li> &lt;"thread.id",$integer_value"&gt; mandatory, the (internal) id of the
   * thread invoking this method; this number is used so as to look-up the right
   * random-number generator associated with the current thread.
   * <li> &lt;"dga.function", FunctionIntf f&gt; mandatory, the function to be
   * optimized.
   * <li> &lt;"dga.hybriddescxoverop.normalizesearchdir",$boolean_value$&gt; optional.
   * If true, then the search direction used to compute the offspring, is
   * normalized to 1. Default is true.
   * <li> &lt;"dga.hybriddescxoverop.t0",$value$&gt; optional value used in
   * computing the step-size in the "line-search" part of the algorithm. Default
   * is 0.001 if normalizesearchdir is true, 0.5 else.
   * <li> &lt;"dga.hybriddescxoverop.t0min",$value$&gt; optional value. Default is
   * 1.e-6.
   * <li> &lt;"dga.hybriddescxoverop.maxitercount", $integer_value$&gt; optional
   * value used to cut-short the search. Default is
   * <CODE>Integer.MAX_VALUE</CODE>.
   * <li> &lt;"dga.hybriddescxoverop.minaccvaldiffperc", $double_value$&gt;
   * optional. Default 0.1.
   * <li> &lt;"dga.hybriddescxoverop.return2bounds", $boolean_value$&gt; optional.
   * Default is false.
	 * </ul>
   * @throws OptimizerException if any of the params above are incorrectly set,
	 * or if the process somehow fails
   * @return Pair containing two new <CODE>double[]</CODE> objects
   */
  public Pair doXover(Object c1, Object c2, HashMap params) throws OptimizerException {
    try {
      final double[] a1 = (double[]) c1;
      final double[] a2 = (double[]) c2;
      FunctionIntf f = (FunctionIntf) params.get("dga.function");
      double v1 = f.eval(a1, params);
      VectorIntf x1 = new DblArray1Vector(a1);
      double v2 = f.eval(a2, params);
      VectorIntf x2 = new DblArray1Vector(a2);
      final int n = a1.length;
      if (diffIsOK(v1,v2,params)) {
        double[] s = new double[n];
        if (v2 > v1) {
          for (int i=0; i<n; i++) s[i] = a1[i] - a2[i];
        } else {
          for (int i=0; i<n; i++) s[i] = a2[i] - a1[i];
        }
        // normalize s to be a descent direction or not?
        boolean normalize_s = true;
        Boolean nsB = (Boolean) params.get("dga.hybriddescxoverop.normalizesearchdir");
        if (nsB!=null && nsB.booleanValue()==false) normalize_s = false;
        if (normalize_s) {
          double len = 0;
          for (int i = 0; i < n; i++) len += s[i] * s[i];
          len = Math.sqrt(len);
          for (int i = 0; i < n; i++) s[i] /= len;
        }
        // create offspring
        double[] off1 = new double[n];
        double[] off2 = new double[n];
        double t0 = normalize_s ? 1.e-3 : 0.5;
        Double t0D = (Double) params.get("dga.hybriddescxoverop.t0");
        if (t0D!=null && t0D.doubleValue()>0)
          t0 = t0D.doubleValue();
        double t0_min = 1.e-6;
        Double t0mD = (Double) params.get("dga.hybriddescxoverop.t0min");
        if (t0mD!=null && t0mD.doubleValue()>0)
          t0_min = t0mD.doubleValue();
        // work with a1 -> off1
        int count = Integer.MAX_VALUE;
        Integer cI = (Integer) params.get("dga.hybriddescxoverop.maxitercount");
        if (cI!=null && cI.intValue()>0) count = cI.intValue();
        final int nmult = 5;
        final double multfactor = 2.0;
        double f1 = v1;
        double[] x = x1.getDblArray1();
        int num_fails_to_accept = nmult;
        int i=0;
        double fval = Double.NaN;
        double t0save=-1;
        while (--count>0) {
          add(x, s, t0, params, off1);
          if (isFeasiblePoint(off1, params)==false) {
            t0 /= multfactor;
            if (t0<t0_min) break;
            continue;
          }
          fval = f.eval(off1, params);
          if (fval < f1) {
            for (int j=0; j<n; j++) x[j] = off1[j];  // x = off1
            f1 = fval;  // set incumbent
            if (++i==nmult) {
              i=0;
              t0 *= multfactor;  // increment step-size
            }
            num_fails_to_accept = nmult;  // reset the failures to accept
            t0save = -1;
          } else {  // failure
            i=0;  // reset counter to multiplying step-size
            if (--num_fails_to_accept<=0) {
              if (num_fails_to_accept==0) t0 = t0save;
              t0 /= multfactor; // decrement
              if (t0 < t0_min) break;
            }
            else {
              if (Math.abs(t0save + 1) < 1.e-6) t0save = t0;  // used to be if (t0save==-1)
              t0 = t0save*num_fails_to_accept;
            }
          }
        }
        Messenger.getInstance().msg("DblArray1HybridDescXOverOp.doXover(): off1: fval="+fval+" f1="+v1+" f2="+v2, 3);
        for (int j=0; j<n; j++) off1[j] = x[j];  // set off1 appropriately
        // work with a2 -> off2
        double f2 = v2;
        x = x2.getDblArray1();
        i=0;
        count = Integer.MAX_VALUE;
        if (cI!=null && cI.intValue()>0) count = cI.intValue();
        t0 = normalize_s ? 1.e-3 : 0.5;
        if (t0D!=null && t0D.doubleValue()>0)
          t0 = t0D.doubleValue();
        num_fails_to_accept = nmult;
        t0save = -1;
        while (--count>0) {
          add(x, s, t0, params, off2);
          if (isFeasiblePoint(off2, params)==false) {
            t0 /= multfactor;
            if (t0<t0_min) break;
            continue;
          }
          fval = f.eval(off2, params);
          if (fval < f2) {
            for (int j=0; j<n; j++) x[j] = off2[j];  // x = off2
            f2 = fval;  // set incumbent
            if (++i==nmult) {
              i=0;
              t0 *= multfactor;  // increment step-size
            }
            num_fails_to_accept = nmult;
            t0save = -1;
          } else {  // failure
            i=0;  // reset counter to multiplying step-size
            if (--num_fails_to_accept<=0) {
              if (num_fails_to_accept==0) t0 = t0save;
              t0 /= multfactor; // decrement
              if (t0 < t0_min) break;
            }
            else {
              if (Math.abs(t0save + 1)<1.e-6) t0save = t0;  // was t0save == -1
              t0 = t0save*num_fails_to_accept;
            }
          }
        }
        for (int j=0; j<n; j++) off2[j] = x[j];  // set off2 appropriately
        Messenger.getInstance().msg("DblArray1HybridDescXOverOp.doXover(): off2: fval="+fval+" f1="+v1+" f2="+v2, 3);

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
      Messenger.getInstance().msg("DblArray1HybridDescXOverOp: XOvers OK: "+ok+" Failed: "+_numFailed,1);
    }
  }
  private synchronized static void incrSuccessful() { ++_numSuccessful; }
  private synchronized static int getNumSuccessful() { return _numSuccessful; }


  private boolean diffIsOK(double v1, double v2, HashMap params) {
    double minaccperc = 0.1;
    Double mapD = (Double) params.get("dga.hybriddescxoverop.minaccvaldiffperc");
    if (mapD!=null && mapD.doubleValue()>0)
      minaccperc = mapD.doubleValue();
    if (Math.min(v1,v2)==0) return Math.max(v1,v2)>minaccperc;  // avoid division by zero
    return Math.abs((v1-v2)/Math.min(v1,v2)) > minaccperc;
  }


  private boolean isFeasiblePoint(double[] x, HashMap params) {
    double minargval = Double.NEGATIVE_INFINITY;
    Double mavD = (Double) params.get("dga.minallelevalue");
    if (mavD!=null) minargval = mavD.doubleValue();
    double maxargval = Double.POSITIVE_INFINITY;
    Double MavD = (Double) params.get("dga.maxallelevalue");
    if (MavD!=null) maxargval = MavD.doubleValue();
    final int n = x.length;
    for (int j=0; j<n; j++) {
      // compute xj limits
      double minaj = minargval;
      Double majD = (Double) params.get("dga.minallelevalue"+j);
      if (majD!=null && majD.doubleValue()>minaj) minaj = majD.doubleValue();
      double maxaj = maxargval;
      Double MajD = (Double) params.get("dga.maxallelevalue"+j);
      if (MajD!=null && MajD.doubleValue()<maxaj) maxaj = MajD.doubleValue();
      if (minaj > x[j] || x[j] > maxaj) return false;
    }
    return true;
  }


  private void add(double[] a, double[] s, double t, HashMap params,
                   double[] off) {
    final int n = a.length;
    boolean do_barrier = false;
    double maxargval = Double.MAX_VALUE;
    double minargval = Double.NEGATIVE_INFINITY;
    try {
      Boolean dbB = (Boolean) params.get("dga.hybriddescxoverop.return2bounds");
      if (dbB!=null) do_barrier = dbB.booleanValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    try {
      Double MavD = (Double) params.get("dga.maxallelevalue");
      if (MavD!=null) maxargval = MavD.doubleValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    try {
      Double mavD = (Double) params.get("dga.minallelevalue");
      if (mavD!=null) minargval = mavD.doubleValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    for (int i=0; i<n; i++) {
      off[i] = a[i] + t * s[i];
      if (do_barrier) {
        double minargvali = minargval;
        double maxargvali = maxargval;
        try {
          Double MavDi = (Double) params.get("dga.maxallelevalue" + i);
          if (MavDi != null && MavDi.doubleValue() < maxargval) maxargvali =
              MavDi.doubleValue();
        }
        catch (ClassCastException e) {
          e.printStackTrace();
        }
        try {
          Double mavDi = (Double) params.get("dga.minallelevalue" + i);
          if (mavDi != null && mavDi.doubleValue() > minargval) minargvali =
              mavDi.doubleValue();
        }
        catch (ClassCastException e) {
          e.printStackTrace();
        }
        if (off[i]<minargvali) off[i] = minargvali;
        if (off[i]>maxargvali) off[i] = maxargvali;
      }
    }
  }

}
