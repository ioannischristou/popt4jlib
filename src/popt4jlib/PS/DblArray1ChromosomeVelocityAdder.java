package popt4jlib.PS;

import popt4jlib.OptimizerException;
import java.util.HashMap;

/**
 * implements the ChromosomeVelocityAdderIntf for the case of fixed and same
 * length arrays of doubles (<CODE>double[]</CODE> objects).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1ChromosomeVelocityAdder implements ChromosomeVelocityAdderIntf {
  /**
   * sole public constructor is a no-op.
   */
  public DblArray1ChromosomeVelocityAdder() {
  }


  /**
   * produces a new <CODE>double[]</CODE> object that is the sum of the two
   * <CODE>double[]</CODE> objects passed as first and second argument. If the
   * result is out-of-bounds given by any box-constraints specified in the
   * params table, then the result is projected back to the box-constrained-
   * feasible region.
   * @param chromosome Object
   * @param velocity Object
   * @param params HashMap may contain the following params:
	 * <ul>
   * <li> &lt;"dpso.minallelevalue", $value$&gt; optional, the minimum value
   * for any allele in the chromosome.
   * <li> &lt;"dpso.minallelevalue$i$", $value$&gt; optional, the minimum value
   * for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}). If this value is less than the global value
   * specified by the "dpso.minallelevalue" key, it is ignored.
   * <li> &lt;"dpso.maxallelevalue", $value$&gt; optional, the maximum value
   * for any allele in the chromosome.
   * <li> &lt;"dpso.maxallelevalue$i$", $value$&gt; optional, the maximum value
   * for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}). If this value is greater than the global
   * value specified by the "dpso.maxallelevalue" key, it is ignored.
	 * </ul>
   * @throws OptimizerException if any of the arguments is null or not of type
   * <CODE>double[]</CODE> or not of same length.
   * @return Object
   */
  public Object addVelocity2Chromosome(Object chromosome, Object velocity, HashMap params) throws OptimizerException {
    if (chromosome==null) throw new OptimizerException("null chromosome arg");
    if (chromosome instanceof double[] == false)
      throw new OptimizerException("chromosome is not double[]");
    if (velocity!=null && velocity instanceof double[] == false)
      throw new OptimizerException("non-null velocity is not double[]");
    final double[] c = (double[]) chromosome;
    final double[] v = (double[]) velocity;
    final int n = c.length;
    if (v.length!=n) throw new OptimizerException("args not of same dimension");
    double maxallelevalg = Double.MAX_VALUE;
    Double maxag = (Double) params.get("dpso.maxallelevalue");
    if (maxag != null) maxallelevalg = maxag.doubleValue();
    double minallelevalg = Double.NEGATIVE_INFINITY;
    Double minag = (Double) params.get("dpso.minallelevalue");
    if (minag != null) minallelevalg = minag.doubleValue();
    double[] res = new double[n];
    for (int i=0; i<n; i++) {
      res[i] = c[i] + v[i];
      double minalleleval = minallelevalg;
      Double minai = (Double) params.get("dpso.minalleleval" + i);
      if (minai != null && minai.doubleValue() > minallelevalg)
        minalleleval = minai.doubleValue();
      if (res[i]<minalleleval) res[i] = minalleleval;
      double maxalleleval = maxallelevalg;
      Double maxai = (Double) params.get("dpso.maxalleleval" + i);
      if (maxai != null && maxai.doubleValue() < maxallelevalg)
        maxalleleval = maxai.doubleValue();
      if (res[i]>maxalleleval) res[i] = maxalleleval;
    }
    return res;
  }

}

