package popt4jlib.PS.FA;

import popt4jlib.OptimizerException;
import utils.LightweightParams;
import java.util.HashMap;


/**
 * the "standard" implementation of the position update for a firefly towards
 * another, more "bright" one (see Wikipedia article: Firefly_Algorithm).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1StdChromosomeUpdater implements ChromosomeUpdaterIntf {
  private int _t=0;
  private HashMap _p=null;  // cache
  private double _L = Double.NaN;  // cache


  /**
   * no-param sole public constructor is a no-op.
   */
  public DblArray1StdChromosomeUpdater() {

  }


  /**
   * the main method of the class, requires certain params be present in
   * the params object. The params are as follows:
	 * <ul>
   * <li> &lt;"dfa.chromosomelength", Integer n&gt; mandatory, the dimension of the
   * double[] chromosomes.
   * <li> &lt;"thread.id", Integer id&gt; mandatory, the id of the calling thread;
   * automatically taken care of by the DFAThreadAux class objects.
   * <li> &lt;"dfa.maxallelevalue", Double v&gt; optional, the max. value any element
   * in the double[] chromosomes position can take. Default is +Infinity.
   * <li> &lt;"dfa.minallelevalue", Double v&gt; optional, the min. value any element
   * in the double[] chromosomes position can take. Default is -Infinity.
   * <li> &lt;"dfa.maxalleleval$i$", Double v&gt; optional, the max. value the i-th
   * element in the double[] chromosomes position can take. Default is +Infinity.
   * <li> &lt;"dfa.minallelevalue$i$", Double v&gt; optional, the min. value the i-th
   * element in the double[] chromosomes position can take. Default is -Infinity.
   * <li> &lt;"dfa.beta", Double v&gt; optional, the value of &beta;. Default is 1.0.
   * <li> &lt;"dfa.a0", Double v&gt; optional, the value of a_0. Default is 0.01*L.
   * <li> &lt;"dfa.delta", Double v&gt; optional, the value of &delta; which is
	 * the basis in the formula for the evolution of a_t, defined according to
	 * a_t = a_0*&delta;^t. Default is 0.97.
   * </ul>
	 * <p>Notice that in this implementation, the parameter &gamma; is defined as
	 * &gamma; = 1/sqrt(L) where L is the average of the range of values each 
	 * variable can take on.</p>
   * @param ci Object // double[]
   * @param cj Object // double[]
   * @param params HashMap
   * @throws OptimizerException if ci or cj are not double[]
   * @return Object // double[]
   */
  public Object update(Object ci, Object cj, HashMap params) throws OptimizerException {
    double beta = 1.0;
    try {
      Double bD = (Double) params.get("dfa.beta");
      if (bD!=null) beta = bD.doubleValue();
    }
    catch (Exception e) {
      e.printStackTrace();  // ignore
    }
    double[] xi = null;
    double[] xj = null;
    try {
      xi = (double[]) ci;
      xj = (double[]) cj;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("ci and/or cj are not double[]");
    }
    Integer idI = (Integer) params.get("thread.id");
    int id = idI.intValue();
    Integer lidI = (Integer) params.get("thread.localid");
    params.remove("thread.localid");
    params.remove("thread.id");  // remove for comparison purposes
    double L = 0.0;
    // note: the use of synchronization is inevitable unless we can guarantee
    // that params arg will always stay the same in the run;
    // but this is a guarantee we don't want to make
    synchronized (this) {
      L = params.equals(_p) ? _L : averageVariableScale(params);
    }
    params.put("thread.id", idI);  // re-insert
    params.put("thread.localid",lidI);
    double gamma = 1.0/Math.sqrt(L);
    double a0 = 0.01*L;
    try {
      Double a0D = (Double) params.get("dfa.a0");
      if (a0D!=null) a0 = a0D.doubleValue();
    }
    catch (Exception e) {
      e.printStackTrace();  // ignore
    }
    double delta = 0.97;
    try {
      Double dD = (Double) params.get("dfa.delta");
      if (dD!=null) delta = dD.doubleValue();
    }
    catch (Exception e) {
      e.printStackTrace();  // ignore
    }
    double a = a0*Math.pow(delta, _t);
    double rij2 = 0.0;
    for (int i=0; i<xi.length; i++) rij2 += (xi[i]-xj[i])*(xi[i]-xj[i]);
    // create new double[] to return
    double[] y = new double[xi.length];
    double maxallelevalg = Double.MAX_VALUE;
    Double maxag = (Double) params.get("dfa.maxallelevalue");
    if (maxag != null) maxallelevalg = maxag.doubleValue();
    double minallelevalg = Double.NEGATIVE_INFINITY;
    Double minag = (Double) params.get("dfa.minallelevalue");
    if (minag != null) minallelevalg = minag.doubleValue();
    for (int i=0; i<y.length; i++) {
      y[i] = xi[i] + beta*Math.exp(-gamma*rij2)*(xj[i] - xi[i]) +
          a*utils.RndUtil.getInstance(id).getRandom().nextGaussian();
      // return y[i] within bounds if any box-constraints are specified
      double minalleleval = minallelevalg;
      Double minai = (Double) params.get("dfa.minalleleval" + i);
      if (minai != null && minai.doubleValue() > minallelevalg)
        minalleleval = minai.doubleValue();
      double maxalleleval = maxallelevalg;
      Double maxai = (Double) params.get("dfa.maxalleleval" + i);
      if (maxai != null && maxai.doubleValue() < maxallelevalg)
        maxalleleval = maxai.doubleValue();
      if (y[i] < minalleleval) y[i] = minalleleval;
      else if (y[i] > maxalleleval) y[i] = maxalleleval;
    }
    return y;
  }


	/**
	 * increment the generation counter.
	 */
  public void incrementGeneration() {
    ++_t;
  }


  /**
   * under normal circumstances, will only be called once.
   * @param p HashMap
   * @throws OptimizerException if p==null
   * @return double
   */
  private double averageVariableScale(HashMap p) throws OptimizerException {
    if (p==null) throw new OptimizerException("null params");
    try {
      LightweightParams params = new LightweightParams(p);
      double avgdiffs = 0.0;
      final int nmax = params.getInteger("dfa.chromosomelength").intValue();
      double maxallelevalg = Double.MAX_VALUE;
      Double maxag = params.getDouble("dfa.maxallelevalue");
      if (maxag != null) maxallelevalg = maxag.doubleValue();
      double minallelevalg = Double.NEGATIVE_INFINITY;
      Double minag = params.getDouble("dfa.minallelevalue");
      if (minag != null) minallelevalg = minag.doubleValue();
      for (int i = 0; i < nmax; i++) {
        double minalleleval = minallelevalg;
        Double minai = params.getDouble("dfa.minalleleval" + i);
        if (minai != null && minai.doubleValue() > minallelevalg)
          minalleleval = minai.doubleValue();
        double maxalleleval = maxallelevalg;
        Double maxai = params.getDouble("dfa.maxalleleval" + i);
        if (maxai != null && maxai.doubleValue() < maxallelevalg)
          maxalleleval = maxai.doubleValue();
        avgdiffs = (avgdiffs*i + maxalleleval - minalleleval)/((double) i+1);
      }
      synchronized (this) {
        _p = new HashMap(p); // cache the params ref.
        _L = avgdiffs;  // cache the scale value
      }
      return _L;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("averageVariableScale(params) failed");
    }
  }
}

