package popt4jlib.MSSC;

import parallel.ParallelException;
import parallel.TaskObject;
import parallel.FasterParallelAsynchBatchTaskExecutor;
import parallel.ConditionCounter;
import parallel.distributed.PDBatchTaskExecutor;
import popt4jlib.VectorIntf;
import popt4jlib.GradientDescent.VecUtil;
import utils.*;
import java.util.*;
import java.io.Serializable;

/**
 * parallel implementation of Neural Gas by Martinetz & Schulten for MSSC. The
 * algorithm has many similarities with the Det. Annealing algorithm of Rose
 * (also implemented as a sequential algorithm in this package), but has much
 * greater parallelism potential in its update phase.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class NeuralGasMTClusterer implements ClustererIntf {
  private VectorIntf[] _centersA;  // size=k
  /**
   * maintains the reference VectorIntf points to be clustered by the algorithm
   */
  private Vector _docs;  // Vector<VectorIntf>, size=n
  /**
   * maintains the algorithm's parameters
   */
  private Hashtable _params;
  private int[] _clusterIndices;
  private Vector _intermediateClusters;  // Vector<Vector<Integer docid>>
  private Vector _centers;  // Vector<VectorIntf>, size=k


  /**
   * sole public constructor.
   */
  public NeuralGasMTClusterer() {
    _intermediateClusters = new Vector();
  }


  /**
   * the most important method of the class. Some parameters must have been
   * previously passed in the _params map (call <CODE>setParams(p)</CODE> to do
   * that).
   * These are:
   *
   * <li> <"neuralgasmt.TerminationCriteria",ClustererTerminationIntf> the
   * object that will decide when to stop the iterations. Mandatory. Essentially
   * only the ClustererTerminationNumIters criterion makes sense for this
   * algorithm (the ClustererTerminationNo[Center]Move neither make sense nor
   * will they work correctly if registered with this object).
   * <li> <"neuralgasmt.numthreads",Integer nt> optional, how many threads will
   * be used to compute the clustering, default is 1. Note: unless the
   * dimensionality of the points is high enough, or the ratio
   * num_points/num_clusters is big enough, so that the tasks to be executed in
   * parallel have enough "work" to do so that the overhead associated with
   * creating and running tasks in parallel in the
   * <CODE>PDBatchTaskExecutor</CODE> is justified, it will be faster to run
   * using only 1 thread (this criterion applies to most situations where tasks
   * are executed in parallel).
   * <li> <"neuralgasmt.seed",Long s> optional, if present, the seed for the
   * random number generator to be used in shuffling the data vectors for
   * presentation order to the update formula. Default is null, resulting in
   * random seed each time the method is invoked.
   * <p>
   * Also, before calling the method, the vectors to be clustered must have
   * been added to the class object via addAllVectors(Vector<VectorIntf>) or
   * via repeated calls to addVector(VectorIntf d), and an initial clustering
   * must be available via a call to
   * <CODE>setInitialClustering(Vector<VectorIntf> clusterCenters)</CODE>.
   * <p>
   * The method works by iteratively presenting one of the document vectors each
   * time t to the centers (ordered by distance to the cur. document vector x)
   * and updating the k-th center according to the formula
   * c_k(t+1) = c_k(t) + å(t)*exp(-k/ë(t))*(x-c_k(t))
   * where the sequences å(t) and ë(t) are both decreasing sequences tending to
   * zero. In this implementation, ë(t) = 1/sqrt(t) å(t) = 1/t.
   *
   * @return Vector Vector<VectorIntf> the best centers (aka codebooks) found
   * that minimize distortion error, ie MSSC.
   */
  public synchronized Vector clusterVectors() throws ClustererException {
    int nt=1;
    try {
      Integer ntI = (Integer) _params.get("neuralgasmt.numthreads");
      if (ntI!=null && ntI.intValue()>0) nt = ntI.intValue();
    }
    catch (Exception e) {
      e.printStackTrace();  // ignore
    }
    try {
      Long sL = (Long) _params.get("neuralgasmt.seed");
      if (sL!=null) {
        long s = sL.longValue();
        RndUtil.getInstance().setSeed(s);
      }
    }
    catch (Exception e) {
      e.printStackTrace();  // ignore
    }
    ClustererTerminationIntf ct = (ClustererTerminationIntf)
        _params.get("neuralgasmt.TerminationCriteria");
    ct.registerClustering(this); // register this clustering problem with ct
    final int n = _docs.size();
    final int k = _centersA.length;
    try {
      FasterParallelAsynchBatchTaskExecutor executor = 
							FasterParallelAsynchBatchTaskExecutor.
											newFasterParallelAsynchBatchTaskExecutor(nt);
      ConditionCounter cdt_counter = new ConditionCounter(nt);
      ConditionCounter uct_counter = new ConditionCounter(nt);
      ConditionCounter uit_counter = new ConditionCounter(nt);
      ArrayList docs = new ArrayList(_docs);
      ArrayList cdtasks = new ArrayList(nt);  // List<ComputeDistanceTask>
      ArrayList uctasks = new ArrayList(nt);  // List<UpdateCenterTask>
      int batchsz = k/nt;  // batch size is the floor of the division
      int starti=0, endi=batchsz-1;
      for (int i=0; i<nt-1; i++) {
        cdtasks.add(new ComputeDistanceTask(starti, endi, cdt_counter));
        uctasks.add(new UpdateCenterTask(starti, endi, uct_counter));
        starti = endi+1;
        endi += batchsz;
      }
      // add last tasks
      cdtasks.add(new ComputeDistanceTask(starti, k-1, cdt_counter));
      uctasks.add(new UpdateCenterTask(starti, k-1, uct_counter));
      int t=0;  // iteration counter
      // main loop
      PairIntDouble[] resA = new PairIntDouble[k];
      long tot_cdttime = 0;  // total time computing distances
      long tot_ucttime = 0;  // total time updating centers
      long tot_uittime = 0;  // total time updating clustering indices
      while (!ct.isDone()) {
        ++t;
        // 0. print some info
        Messenger.getInstance().msg("NeuralGasMTClusterer.clusterVectors(): Running iteration #"+t,0);
        // 1. Shuffle docs
        Collections.shuffle(docs, RndUtil.getInstance().getRandom());
        for (int i=0; i<n; i++) {  // for each vector di
          VectorIntf di = (VectorIntf) docs.get(i);
          // 2. compute rank of every center cj to di
          for (int j=0; j<nt; j++) {
            // update ComputeDistanceTasks
            ComputeDistanceTask cdtji = (ComputeDistanceTask) cdtasks.get(j);
            cdtji.setDiValue(di);
          }
          // executing ComputeDistanceTask's in parallel may be slower when
          // using many threads instead of one, depending on the problem
          // characteristics: the more clusters, or the more dimensionality,
          // the better for the multi-core approach.
          // measure time to execute ComputeDistanceTasks
          long st=System.currentTimeMillis();
          executor.executeBatch(cdtasks);  // Vector<ComputeDistanceTask>
          // wait until all tasks have finished
          cdt_counter.await();
          long dur = System.currentTimeMillis()-st;
          tot_cdttime += dur;
          // 3. order centers by distance to di
          int m=0;
          for (int j=0; j<nt; j++) {
            ComputeDistanceTask tj = (ComputeDistanceTask) cdtasks.get(j);
            for (int l=0; l<tj._pairs.length; l++) resA[m++] = tj._pairs[l];
          }
          Arrays.sort(resA);
          // 4. apply update formula to each center
          for (int j=0; j<k; j++) {
            PairIntDouble pji = (PairIntDouble) resA[j];
            int posj = pji.getInt();
            // update UpdateCenterTasks
            int ind = posj/batchsz;
            if (ind >= nt) ind = nt-1;
            UpdateCenterTask uctj = (UpdateCenterTask) uctasks.get(ind);
            uctj.setValues(di, posj, j, t);
            pji.release();  // reclaim pool space
          }
          // executing UpdateCenterTask's in parallel may be slower when
          // using many threads instead of one, depending on the problem
          // characteristics: the more clusters, or the more dimensionality,
          // the better for the multi-core approach.
          st = System.currentTimeMillis();
          executor.executeBatch(uctasks);
          // wait until all tasks have finished
          uct_counter.await();
          dur = System.currentTimeMillis()-st;
          tot_ucttime += dur;
          // reset counters
          cdt_counter.reset();
          uct_counter.reset();
        }
      }
      // 5. update _centers
      for (int j=0; j<k; j++) _centers.set(j, _centersA[j]);
      // 6. update clustering indices
      _clusterIndices = new int[n];
      ArrayList uits = new ArrayList(nt);  // ArrayList<UpdateIndexTask>
      batchsz = n/nt;
      starti=0;
      endi=batchsz-1;
      for (int i=0; i<nt-1; i++) {
        UpdateIndexTask uiti = new UpdateIndexTask(starti,endi, uit_counter);
        uits.add(uiti);
        starti = endi+1;
        endi += batchsz;
      }
      uits.add(new UpdateIndexTask(starti, n-1, uit_counter));
      // running in parallel the UpdateIndexTask's is always beneficial in terms
      // of running time no matter the problem characteristics.
      long st = System.currentTimeMillis();
      executor.executeBatch(uits);
      uit_counter.await();
      tot_uittime = System.currentTimeMillis()-st;
      Messenger.getInstance().msg("Break-Down of Parallel Processing in times (msecs): "+
                                  "CDT_time="+tot_cdttime+
                                  " UCT_time="+tot_ucttime+
                                  " UIT_time="+tot_uittime,0);
      // 7. end
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ClustererException("clusterVectors() failed...");
    }
    return _centers;
  }


  /**
   * old and slow method that also implements the Neural-Gas algorithm. This
   * method will soon be deprecated (only exists now for debugging purposes).
   * @throws ClustererException
   * @return Vector Vector<VectorIntf center>
   */
  public synchronized Vector clusterVectorsOldnSlow() throws ClustererException {
    int nt=1;
    try {
      Integer ntI = (Integer) _params.get("neuralgasmt.numthreads");
      if (ntI!=null && ntI.intValue()>0) nt = ntI.intValue();
    }
    catch (Exception e) {
      e.printStackTrace();  // ignore
    }
    try {
      Long sL = (Long) _params.get("neuralgasmt.seed");
      if (sL!=null) {
        long s = sL.longValue();
        RndUtil.getInstance().setSeed(s);
      }
    }
    catch (Exception e) {
      e.printStackTrace();  // ignore
    }
    ClustererTerminationIntf ct = (ClustererTerminationIntf)
        _params.get("neuralgasmt.TerminationCriteria");
    ct.registerClustering(this); // register this clustering problem with ct
    final int n = _docs.size();
    final int k = _centersA.length;
    try {
      PDBatchTaskExecutor executor = PDBatchTaskExecutor.
																		   newPDBatchTaskExecutor(nt);
      ArrayList docs = new ArrayList(_docs);
      ArrayList cdtasks = new ArrayList(k);  // List<ComputeDistanceTaskOldnSlow>
      ArrayList uctasks = new ArrayList(k);  // List<UpdateCenterTaskOldnSlow>
      for (int i=0; i<k; i++) {
        cdtasks.add(new ComputeDistanceTaskOldnSlow());
        uctasks.add(new UpdateCenterTaskOldnSlow());
      }
      int t=0;  // iteration counter
      // main loop
      while (!ct.isDone()) {
        ++t;
        // 0. print some info
        Messenger.getInstance().msg("NeuralGasMTClusterer.clusterVectorsOldnSlow(): Running iteration #"+t,0);
        // 1. Shuffle docs
        Collections.shuffle(docs, RndUtil.getInstance().getRandom());
        for (int i=0; i<n; i++) {  // for each vector di
          VectorIntf di = (VectorIntf) docs.get(i);
          // 2. compute rank of every center cj to di
          for (int j=0; j<k; j++) {
            VectorIntf cj = _centersA[j];
            // update ComputeDistanceTasks
            ComputeDistanceTaskOldnSlow cdtji = (ComputeDistanceTaskOldnSlow) cdtasks.get(j);
            cdtji.setValues(di,j,cj);
          }
          Vector res = executor.executeBatch(cdtasks);  // Vector<PairIntDouble>
          // 3. order centers by distance to di
          Object[] resA = res.toArray();
          Arrays.sort(resA);
          // 4. apply update formula to each center
          for (int j=0; j<k; j++) {
            PairIntDouble pji = (PairIntDouble) resA[j];
            int posj = pji.getInt();
            VectorIntf cposj = (VectorIntf) _centersA[posj];
            // update UpdateCenterTasks
            UpdateCenterTaskOldnSlow uctj = (UpdateCenterTaskOldnSlow) uctasks.get(j);
            uctj.setValues(di, j, cposj, t);
            pji.release();  // reclaim pool space
          }
          _centers = executor.executeBatch(uctasks);
          // 5. update _centersA
          for (int j=0; j<k; j++) _centersA[j] = (VectorIntf) _centers.elementAt(j);
        }
      }
      // 6. update clustering indices
      _clusterIndices = new int[n];
      ArrayList uits = new ArrayList(n);  // ArrayList<UpdateIndexTaskOldnSlow>
      for (int i=0; i<n; i++) {
        UpdateIndexTaskOldnSlow uiti = new UpdateIndexTaskOldnSlow(i);
        uits.add(uiti);
      }
      executor.executeBatch(uits);
      // 7. end
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ClustererException("clusterVectors() failed...");
    }
    return _centers;
  }


  /**
   * returns a Vector<Vector<Integer docid> > representing the ids of the
   * vectors in each cluster.
   * @return Vector
   */
  public synchronized Vector getIntermediateClusters() {
    // store the final clustering in _intermediateClusters
    int prev_ic_sz = _intermediateClusters.size();
    for (int i=0; i<_centers.size(); i++) _intermediateClusters.addElement(new Vector());
    for (int i=0; i<_clusterIndices.length; i++) {
      int c = _clusterIndices[i];
      Vector vc = (Vector) _intermediateClusters.elementAt(prev_ic_sz+c);
      vc.addElement(new Integer(i));
      _intermediateClusters.set(prev_ic_sz+c, vc);  // ensure addition
    }
    // remove any empty vectors
    for (int i=_intermediateClusters.size()-1; i>=0; i--) {
      Vector v = (Vector) _intermediateClusters.elementAt(i);
      if (v==null || v.size()==0) _intermediateClusters.remove(i);
    }
    return _intermediateClusters;
  }


  /**
   * returns the parameters to be used for clustering for this clusterer object.
   * @return Hashtable
   */
  public synchronized Hashtable getParams() {
    return _params;
  }

  /**
   * appends the argument to the current _docs collection.
   * @param d VectorIntf
   */
  public synchronized void addVector(VectorIntf d) {
    if (_docs==null) _docs = new Vector();
    _docs.addElement(d);
  }


  /**
   * adds to the end of _docs all VectorIntf's in v.
   * Will throw class cast exception if any object in v is not a VectorIntf
   * @param v Vector Vector<VectorIntf>
   */
  public synchronized void addAllVectors(Vector v) {
    if (v==null) return;
    if (_docs==null) _docs = new Vector();
    for (int i=0; i<v.size(); i++)
      _docs.addElement((VectorIntf) v.elementAt(i));
  }


  /**
   * set the initial clustering centers. The vector _centers is reconstructed,
   * as well as the centers (ie deep copy is performed).
   * @param centers Vector Vector<VectorIntf>
   * @throws ClustererException if any object in centers is not a VectorIntf
   */
  public synchronized void setInitialClustering(Vector centers) throws ClustererException {
    if (centers==null || centers.size()==0)
      throw new ClustererException("null or empty initial clusters vector");
    _centersA = new VectorIntf[centers.size()];
    _centers = null;  // force gc
    _centers = new Vector();
    try {
      for (int i = 0; i < centers.size(); i++) {
        _centersA[i] = ((VectorIntf) centers.elementAt(i)).newInstance();  // used to be newCopy();
        _centers.addElement(_centersA[i]);
      }
    }
    catch (ClassCastException e) {
      throw new ClustererException("at least one object in centers is not a VectorIntf");
    }
  }


  /**
   * returns the current centers.
   * @return Vector Vector<VectorIntf center>
   */
  public synchronized Vector getCurrentCenters() {
    return _centers;
  }


  /**
   * returns the current VectorIntf objects to be (or that have already been)
   * clustered.
   * @return Vector Vector<VectorIntf doc>
   */
  public synchronized Vector getCurrentVectors() {
    return _docs;
  }


  /**
   * the clustering params are set to p
   * @param p Hashtable
   */
  public synchronized void setParams(Hashtable p) {
    _params = null;
    _params = new Hashtable(p);  // own the params
  }


  /**
   * reset all data members so that the object can be used again in another
   * clustering process.
   */
  public synchronized void reset() {
    _docs = null;
    _centers = null;
    _centersA = null;
    _clusterIndices=null;
    _intermediateClusters.clear();
    if (_params!=null) _params.clear();
  }


  /**
   * returns the indices indicating to which cluster each vector (in the order
   * added to this object's _docs Vector) belongs. The values of the indices are
   * in the range {0, 1, 2, ... _centers.size()-1}.
   * @return int[]
   */
  public synchronized int[] getClusteringIndices() {
    return _clusterIndices;
  }


  /**
   * set clustering indices to the values specified in the argument.
   * @param a int[]
   */
  public synchronized void setClusteringIndices(int[] a) {
    if (a==null) _clusterIndices = null;
    else {
      _clusterIndices = new int[a.length];
      for (int i=0; i<a.length; i++)
        _clusterIndices[i] = a[i];
    }
  }


  /**
   * return an int[] specifying the cardinalities of each cluster in the
   * current clustering. The length of the returned array is equal to
   * <CODE>_centers.size()</CODE>, namely the number of centers passed in during
   * the call <CODE>setInitialClustering(centers)</CODE>.
   * @throws ClustererException
   * @return int[]
   */
  public synchronized int[] getClusterCards() throws ClustererException {
    if (_clusterIndices==null)
      throw new ClustererException("null _clusterIndices");
    final int k = _centersA.length;
    final int n = _docs.size();
    int[] cards = new int[k];
    for (int i=0; i<k; i++) cards[i]=0;
    for (int i=0; i<n; i++) {
      cards[_clusterIndices[i]]++;
    }
    return cards;
  }


  /**
   * returns the value for the current clustering, according to the evaluator
   * object passed in the argument.
   * @param vtor EvaluatorIntf
   * @throws ClustererException
   * @return double
   */
  public double eval(EvaluatorIntf vtor) throws ClustererException {
    return vtor.eval(this);
  }


  /**
   * auxiliary inner class helping to compute in parallel distance of a point
   * to another.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class ComputeDistanceTask implements TaskObject {
    // private final static long serialVersionUID=...L;
    private VectorIntf _di;
    private int _starti;
    private int _endi;
    private PairIntDouble[] _pairs;
    private ConditionCounter _counter;

    ComputeDistanceTask(int starti, int endi, ConditionCounter c) {
      _starti = starti;
      _endi = endi;
      _pairs = new PairIntDouble[endi-starti+1];
      _counter = c;
    }


    void setDiValue(VectorIntf di) {
      _di = di;
    }


    public Serializable run() {
      int j=0;
      for (int i=_starti; i<=_endi; i++) {
        double val = VecUtil.getEuclideanDistance(_di, _centersA[i]);
        PairIntDouble p = PairIntDouble.newInstance(i, val);
        _pairs[j++] = p;
      }
      _counter.increment();
      return this;
    }


    /**
     * method returns always true.
     * @return boolean
     */
    public boolean isDone() { return true; }


    /**
     * unsupported: always throws IllegalArgumentException.
     * @param t TaskObject
     * @throws IllegalArgumentException
     */
    public void copyFrom(TaskObject t) throws IllegalArgumentException {
      throw new IllegalArgumentException("copyFrom(t) method not supported");
    }
  }


  /**
   * auxiliary inner class helping to compute in parallel the update of a center
   * given a reference data-vector.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class UpdateCenterTask implements TaskObject {
    // private final static long serialVersionUID=...L;
    private VectorIntf _di;
    private int[] _js;  // the rank of center-i, i in the range [_starti,_endi]
    private int _t;
    private int _starti;
    private int _endi;
    private ConditionCounter _counter;


    /**
     * sole constructor.
     */
    UpdateCenterTask(int start, int end, ConditionCounter c) {
      _starti=start;
      _endi=end;
      _js = new int[end-start+1];
      _counter = c;
    }


    void setValues(VectorIntf di, int j, int posj, int t) {
      _di = di;
      _js[j-_starti] = posj;
      _t = t;
    }


    /**
     * update and return the i-th center i in [_starti, _endi] according to:
     * c_k(t+1) = c_k(t) + å(t)*exp(-k/ë(t))*(x-c_k(t))
     * where the sequences å(t) and ë(t) are both decreasing sequences tending to
     * zero. In this implementation, ë(t) = 1/sqrt(t) å(t) = 1/t.
     * @return Serializable
     */
    public Serializable run() {
      double et = 1.0/(double) _t;
      double lamdat = 1.0/Math.sqrt((double)_t);
      for (int j=0; j<_js.length; j++) {
        double multfactor = et * Math.exp( -_js[j] / lamdat);
        VectorIntf cposj = _centersA[_starti+j];
        final int dim = cposj.getNumCoords();
        for (int i = 0; i < dim; i++) {
          double cposji = cposj.getCoord(i);
          double newval = cposji + multfactor * (_di.getCoord(i) - cposji);
          try {
            cposj.setCoord(i, newval);
          }
          catch (ParallelException e) {
            e.printStackTrace(); // cannot get here
            return null;
          }
        }
      }
      _counter.increment();
      return this;
    }


    /**
     * method returns always true.
     * @return boolean
     */
    public boolean isDone() { return true; }


    /**
     * unsupported: always throws IllegalArgumentException.
     * @param t TaskObject
     * @throws IllegalArgumentException
     */
    public void copyFrom(TaskObject t) throws IllegalArgumentException {
      throw new IllegalArgumentException("copyFrom(t) method not supported");
    }
  }

  /**
   * auxiliary class to compute in parallel the index of each vector to its
   * assigned cluster.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class UpdateIndexTask implements TaskObject {
    // private static final long serialVersionUID=...L;
    private int _starti;
    private int _endi;
    private ConditionCounter _counter;


    UpdateIndexTask(int s, int e, ConditionCounter c) {
      _starti = s;
      _endi = e;
      _counter = c;
    }


    public Serializable run() {
      final int k = _centersA.length;
      for (int i=_starti; i<=_endi; i++) {
        int bestj = -1;
        double best_dist = Double.MAX_VALUE;
        VectorIntf di = (VectorIntf) _docs.elementAt(i);
        for (int j = 0; j < k; j++) {
          VectorIntf cj = _centersA[j];
          double dist_ij = VecUtil.getEuclideanDistance(di, cj);
          if (dist_ij < best_dist) {
            best_dist = dist_ij;
            bestj = j;
          }
        }
        _clusterIndices[i] = bestj;
      }
      _counter.increment();
      return this;  // not really needed
    }


    /**
     * method returns always true.
     * @return boolean
     */
    public boolean isDone() { return true; }


    /**
     * unsupported: always throws IllegalArgumentException.
     * @param t TaskObject
     * @throws IllegalArgumentException
     */
    public void copyFrom(TaskObject t) throws IllegalArgumentException {
      throw new IllegalArgumentException("copyFrom(t) method not supported");
    }

  }


  // below are old classes implementing TaskObjects used with the old n' slow
  // method clusterVectorsOldnSlow()...


  /**
   * auxiliary inner class helping to compute in parallel distance of a point
   * to another.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class ComputeDistanceTaskOldnSlow implements TaskObject {
    // private final static long serialVersionUID=...L;
    private VectorIntf _di;
    private int _j;
    private VectorIntf _cj;

    /**
     * sole constructor is a no-op.
     */
    ComputeDistanceTaskOldnSlow() {
    }


    void setValues(VectorIntf di, int j, VectorIntf cj) {
      _di = di;
      _j = j;
      _cj = cj;
    }


    public Serializable run() {
      double val = VecUtil.getEuclideanDistance(_di, _cj);
      PairIntDouble p = PairIntDouble.newInstance(_j, val);
      return p;
    }


    /**
     * method returns always true.
     * @return boolean
     */
    public boolean isDone() { return true; }


    /**
     * unsupported: always throws IllegalArgumentException.
     * @param t TaskObject
     * @throws IllegalArgumentException
     */
    public void copyFrom(TaskObject t) throws IllegalArgumentException {
      throw new IllegalArgumentException("copyFrom(t) method not supported");
    }
  }


  /**
   * auxiliary inner class helping to compute in parallel the update of a center
   * given a reference data-vector.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class UpdateCenterTaskOldnSlow implements TaskObject {
    // private final static long serialVersionUID=...L;
    private VectorIntf _di;
    private int _j;
    private VectorIntf _cposj;
    private int _t;


    /**
     * sole constructor is a no-op.
     */
    UpdateCenterTaskOldnSlow() {

    }


    void setValues(VectorIntf di, int j, VectorIntf cposj, int t) {
      _di = di;
      _j = j;
      _cposj = cposj;
      _t = t;
    }

    /**
     * update and return the k-th center according to the formula
     * c_k(t+1) = c_k(t) + å(t)*exp(-k/ë(t))*(x-c_k(t))
     * where the sequences å(t) and ë(t) are both decreasing sequences tending to
     * zero. In this implementation, ë(t) = 1/sqrt(t) å(t) = 1/t.
     * @return Serializable
     */
    public Serializable run() {
      double et = 1.0/(double) _t;
      double lamdat = 1.0/Math.sqrt((double)_t);
      double multfactor = et*Math.exp(-_j/lamdat);
      final int dim = _cposj.getNumCoords();
      for (int i=0; i<dim; i++) {
        double cposji = _cposj.getCoord(i);
        double newval = cposji + multfactor*(_di.getCoord(i)-cposji);
        try {
          _cposj.setCoord(i, newval);
        }
        catch (ParallelException e) {
          e.printStackTrace();  // cannot get here
          return null;
        }
      }
      return _cposj;
    }

    /**
     * method returns always true.
     * @return boolean
     */
    public boolean isDone() { return true; }


    /**
     * unsupported: always throws IllegalArgumentException.
     * @param t TaskObject
     * @throws IllegalArgumentException
     */
    public void copyFrom(TaskObject t) throws IllegalArgumentException {
      throw new IllegalArgumentException("copyFrom(t) method not supported");
    }
  }

  /**
   * auxiliary class to compute in parallel the index of each vector to its
   * assigned cluster.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class UpdateIndexTaskOldnSlow implements TaskObject {
    // private static final long serialVersionUID=...L;
    private int _i;


    UpdateIndexTaskOldnSlow(int i) {
      _i = i;
    }


    public Serializable run() {
      final int k = _centersA.length;
      int bestj = -1;
      double best_dist = Double.MAX_VALUE;
      VectorIntf di = (VectorIntf) _docs.elementAt(_i);
      for (int j=0; j<k; j++) {
        VectorIntf cj = _centersA[j];
        double dist_ij = VecUtil.getEuclideanDistance(di, cj);
        if (dist_ij<best_dist) {
          best_dist = dist_ij;
          bestj = j;
        }
      }
      _clusterIndices[_i] = bestj;
      return new Integer(bestj);  // not really needed
    }


    /**
     * method returns always true.
     * @return boolean
     */
    public boolean isDone() { return true; }


    /**
     * unsupported: always throws IllegalArgumentException.
     * @param t TaskObject
     * @throws IllegalArgumentException
     */
    public void copyFrom(TaskObject t) throws IllegalArgumentException {
      throw new IllegalArgumentException("copyFrom(t) method not supported");
    }

  }

}

