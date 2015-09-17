package popt4jlib.GA;

import popt4jlib.*;
import popt4jlib.GradientDescent.LocalOptimizerIntf;
import utils.*;
import java.util.*;

/**
 * experimental mutation operator based on gradient descent ideas (similar to
 * the ideas for <CODE>DblArray1GradDescXOverOp</CODE>.)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1GradientDescentMutationOp implements MutationOpIntf {

  private int _failedMutations=0;
  private int _successfulMutations=0;

  /**
   * sole public (no-arg) constructor (no-op body).
   */
  public DblArray1GradientDescentMutationOp() {
  }


  /**
   * the operation mutates each chromosome argument (not a DGAIndividual, not a
   * Function() arg object) and stores the new chromosome in a Pair object that
   * is returned.
   * @param chromosome1 Object double[]
   * @param chromosome2 Object double[]
   * @param params HashMap must contain the following key-value pairs:
	 * <ul>
   * <li> &lt;"dga.mutationrate", $value$&gt; optional, default 0.1.
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
   * <li> &lt;"dga.localoptimizer", LocalOptimizerIntf lopter&gt; optional, if
   * non-null, the value must point to a <CODE>LocalOptimizerIntf</CODE> that
   * can apply some of the methods in the <CODE>popt4jlib.GradientDescent</CODE>
   * package.
	 * </ul>
   * @throws OptimizerException
   * @return Pair whose two elements are both <CODE>double[]</CODE> objects
   */
  public Pair doMutation(Object chromosome1, Object chromosome2, HashMap params) throws OptimizerException {
    try {
      final int id = ( (Integer) params.get("thread.id")).intValue();
      double[] a1 = (double[]) chromosome1;
      double[] a2 = (double[]) chromosome2;
      // see if mutation must be done
      double mut_prob = 0.1;
      Double mProbD = (Double) params.get("dga.mutationrate");
      if (mProbD!=null && mProbD.doubleValue()>0)
        mut_prob = mProbD.doubleValue();
      double tosscoin = RndUtil.getInstance(id).getRandom().nextDouble();
      if (tosscoin > mut_prob) {
        Pair res = new Pair(a1, a2);
        return res;
      }
      double minargval = Double.NEGATIVE_INFINITY;
      double maxargval = Double.MAX_VALUE;
      // check if value exceeds any limits
      Double minargvalD = (Double) params.get("dga.minallelevalue");
      if (minargvalD != null && minargvalD.doubleValue() > minargval)
        minargval = minargvalD.doubleValue();
      Double maxargvalD = (Double) params.get("dga.maxallelevalue");
      if (maxargvalD != null && maxargvalD.doubleValue() < maxargval)
        maxargval = maxargvalD.doubleValue();
      final LocalOptimizerIntf localopter0 = (LocalOptimizerIntf) params.get("dga.localoptimizer");
      HashMap params2 = new HashMap(params);
      // mutate first chromosome
      VectorIntf x0 = new DblArray1Vector(a1);
      params2.put("gradientdescent.x0", x0);
      LocalOptimizerIntf localopter = localopter0.newInstance();
      localopter.setParams(params2);
      FunctionIntf f = (FunctionIntf) params.get("dga.function");
      try {
        PairObjDouble pod = localopter.minimize(f);
        VectorIntf xf = (VectorIntf) pod.getArg();
        for (int i = 0; i < xf.getNumCoords(); i++) {
          double xi = xf.getCoord(i);
          // restore within bounds, if any
          double maxargvali = maxargval;
          Double MaviD = (Double) params.get("dga.maxallelevalue"+i);
          if (MaviD!=null && MaviD.doubleValue()<maxargval)
            maxargvali = MaviD.doubleValue();
          double minargvali = minargval;
          Double maviD = (Double) params.get("dga.minallelevalue"+i);
          if (maviD!=null && maviD.doubleValue()>minargval)
            minargvali = maviD.doubleValue();
          a1[i] = xi < maxargvali ? (xi > minargvali ? xi : minargvali) :
              maxargvali;
        }
        incSuccessfulMutations();
      }
      catch (Exception e) {
        incFailedMutations();
      }
      // mutate second chromosome
      x0 = new DblArray1Vector(a2);
      params2.put("gradientdescent.x0", x0);
      localopter = localopter0.newInstance();
      localopter.setParams(params2);
      try {
        PairObjDouble pod = localopter.minimize(f);
        VectorIntf xf = (VectorIntf) pod.getArg();
        for (int i = 0; i < xf.getNumCoords(); i++) {
          double xi = xf.getCoord(i);
          // restore within bounds, if any
          double maxargvali = maxargval;
          Double MaviD = (Double) params.get("dga.maxallelevalue"+i);
          if (MaviD!=null && MaviD.doubleValue()<maxargval)
            maxargvali = MaviD.doubleValue();
          double minargvali = minargval;
          Double maviD = (Double) params.get("dga.minallelevalue"+i);
          if (maviD!=null && maviD.doubleValue()>minargval)
            minargvali = maviD.doubleValue();
          a2[i] = xi < maxargvali ? (xi > minargvali ? xi : minargvali) :
              maxargvali;
        }
        incSuccessfulMutations();
      }
      catch (Exception e) {
        incFailedMutations();
      }
      Pair p = new Pair(a1, a2);
      return p;
    }
    catch (Exception e) {
      e.printStackTrace();
      incFailedMutations();
      throw new OptimizerException("doMutation(): failed");
    }
  }


  public synchronized int getFailedMutations() {
    return _failedMutations;
  }
  public synchronized int getSuccessfulMutations() {
    return _successfulMutations;
  }


  private synchronized void incFailedMutations() { ++_failedMutations; }
  private synchronized void incSuccessfulMutations() { ++_successfulMutations; }

}

