package popt4jlib.GA;

import popt4jlib.*;
import popt4jlib.GradientDescent.LocalOptimizerIntf;
import utils.*;
import java.util.*;

/**
 * implements a hybrid mutation operator on double[] genes.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1HybridMutationOp implements MutationOpIntf {


  /**
   * sole public constructor has noop body.
   */
  public DblArray1HybridMutationOp() {
  }


  /**
   * the operation mutates each chromosome argument (not a DGAIndividual, not a
   * Function() arg object) and stores the new chromosome in a Pair object that
   * is returned. The arguments must be double[] objects, and the returned
   * objects in the Pair return argument are also new double[] objects.
   * @param chromosome1 Object a double[]
   * @param chromosome2 Object a double[]
   * @param params HashMap must contain the following:
	 * <ul>
   * <li> &lt;"dga.mutationrate", $double_value$&gt; optional, default 0.1.
   * <li> &lt;"dga.gdmutationrate", $double_value$&gt; optional, default 0.5.
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
   * @throws OptimizerException if any of the params above are incorrectly set,
	 * or if the process somehow fails
   * @return Pair Pair&lt;double[], double[]&gt;
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

      // check whether to do gradient-descent or simple 1-Allele mutation
      double gdmutrate = 0.5;
      Double gdProbD = (Double) params.get("dga.gdmutationrate");
      if (gdProbD!=null && gdProbD.doubleValue()>0)
        gdmutrate = gdProbD.doubleValue();
      tosscoin = RndUtil.getInstance(id).getRandom().nextDouble();
      if (tosscoin > gdmutrate) {
        // do 1-Allele mutation
        do1AlleleMutation(a1,a2,id,params);
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
      }
      catch (Exception e) {
        // no-op
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
      }
      catch (Exception e) {
        // no-op
      }
      Pair p = new Pair(a1, a2);
      return p;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("doMutation(): failed");
    }
  }


  private void do1AlleleMutation(double[] a1, double[] a2, int id, HashMap params) {
    double min = Double.MAX_VALUE;
    double max = Double.NEGATIVE_INFINITY;
    int n1 = a1.length;
    for (int i = 0; i < n1; i++) {
      if (max < a1[i]) max = a1[i];
      else if (min > a1[i]) min = a1[i];
    }
    int r1 = RndUtil.getInstance(id).getRandom().nextInt(n1);
    double rv = RndUtil.getInstance(id).getRandom().nextGaussian();
    a1[r1] += (max - min) * rv;
    // check if value exceeds any limits
    Double minargval = (Double) params.get("dga.minallelevalue");
    if (minargval != null && minargval.doubleValue() > a1[r1])
      a1[r1] = minargval.doubleValue();
    Double minargvalr1 = (Double) params.get("dga.minallelevalue"+r1);
    if (minargvalr1 != null && minargvalr1.doubleValue() > a1[r1])
      a1[r1] = minargvalr1.doubleValue();

    Double maxargval = (Double) params.get("dga.maxallelevalue");
    if (maxargval != null && maxargval.doubleValue() < a1[r1])
      a1[r1] = maxargval.doubleValue();
    Double maxargvalr1 = (Double) params.get("dga.maxallelevalue"+r1);
    if (maxargvalr1 != null && maxargvalr1.doubleValue() < a1[r1])
      a1[r1] = maxargvalr1.doubleValue();

    int n2 = a2.length;
    for (int i = 0; i < n2; i++) {
      if (max < a2[i]) max = a2[i];
      else if (min > a2[i]) min = a2[i];
    }
    int r2 = RndUtil.getInstance(id).getRandom().nextInt(n2);
    rv = RndUtil.getInstance(id).getRandom().nextGaussian();
    a2[r2] += (max - min) * rv;
    if (minargval != null && minargval.doubleValue() > a2[r2])
      a2[r2] = minargval.doubleValue();
    Double minargvalr2 = (Double) params.get("dga.minallelevalue"+r2);
    if (minargvalr2 != null && minargvalr2.doubleValue() > a2[r2])
      a2[r2] = minargvalr2.doubleValue();

    if (maxargval != null && maxargval.doubleValue() < a2[r2])
      a2[r2] = maxargval.doubleValue();
    Double maxargvalr2 = (Double) params.get("dga.maxallelevalue"+r2);
    if (maxargvalr2 != null && maxargvalr2.doubleValue() < a2[r2])
      a2[r2] = maxargvalr2.doubleValue();
  }

}
