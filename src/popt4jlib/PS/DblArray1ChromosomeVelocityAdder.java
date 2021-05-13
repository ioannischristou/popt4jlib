package popt4jlib.PS;

import popt4jlib.OptimizerException;
import utils.Messenger;
import java.util.HashMap;

/**
 * implements the ChromosomeVelocityAdderIntf for the case of fixed and same
 * length arrays of doubles (<CODE>double[]</CODE> objects).
 * <p>Notes:
 * <ul>
 * <li>2021-05-13: fixed mispelling of key "dpso.[min|max]argval$i$" when 
 * looking for specific components' minimum and/or maximum allowed values.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.1
 */
public class DblArray1ChromosomeVelocityAdder 
  implements ChromosomeVelocityAdderIntf {
	
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
   * @return Object double[]
   */
  public Object addVelocity2Chromosome(Object chromosome, Object velocity, 
		                                   HashMap params) 
		throws OptimizerException {
    if (chromosome==null) throw new OptimizerException("null chromosome arg");
    if (chromosome instanceof double[] == false)
      throw new OptimizerException("chromosome is not double[]");
    if (velocity!=null && velocity instanceof double[] == false)
      throw new OptimizerException("non-null velocity is not double[]");
		final Messenger mger = Messenger.getInstance();
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
      Double minai = (Double) params.get("dpso.minallelevalue" + i);
      if (minai != null && minai.doubleValue() > minallelevalg)
        minalleleval = minai.doubleValue();
      if (res[i]<minalleleval) res[i] = minalleleval;
      double maxalleleval = maxallelevalg;
      Double maxai = (Double) params.get("dpso.maxallelevalue" + i);
      if (maxai != null && maxai.doubleValue() < maxallelevalg)
        maxalleleval = maxai.doubleValue();
      if (res[i]>maxalleleval) res[i] = maxalleleval;
    }
		// diagnostics
		if (mger.getDebugLvl()>=2) {
			String arrstr = 
				"DblArray1ChromosomeVelocityAdder.addVelocity2Chromosome(): res=[";
			for (int i=0; i<res.length; i++) {
				arrstr += res[i];
				if (i<res.length-1) arrstr += ",";
			}
			arrstr += "]";
			mger.msg(arrstr, 2);
		}		
    return res;
  }

}

