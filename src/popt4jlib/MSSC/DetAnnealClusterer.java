package popt4jlib.MSSC;

import popt4jlib.*;
import popt4jlib.GradientDescent.VecUtil;
import java.util.*;


/**
 * the class implements the Deterministic Annealing algorithm for MSSC by Rose.
 * The class is neither multi-threaded, nor is it thread-safe.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class DetAnnealClusterer implements ClustererIntf {
  private List _centers;  // Vector<VectorIntf>, size=k
  private List _docs;  // Vector<VectorIntf>, size=n
  private HashMap _params;
  private int[] _clusterIndices;
  private KMeansSqrEvaluator _evaluator;
  private List _intermediateClusters;  // Vector<Vector<Integer docid>>
  //private final DocumentDistIntf _distmetric = new DocumentDistL2Sqr();
  private static double _eps = 1.e-3;


  /**
   * no-arg constructor.
   */
  public DetAnnealClusterer() {
    _evaluator = new KMeansSqrEvaluator();
    _intermediateClusters = new ArrayList();
  }


  /**
   * return all the intermediate clusters produced by the
   * <CODE>clusterVectors()</CODE> method.
   * @throws ClustererException
   * @return List // Vector&lt;Vector&lt;Integer vecid&gt;&gt;
   */
  public List getIntermediateClusters() throws ClustererException {
    return _intermediateClusters;
  }


  /**
   * returns the parameters for this clusterer.
   * @return HashMap
   */
  public HashMap getParams() {
    return _params;
  }


  /**
   * the most important method of the class. Some parameters must have been
   * previously passed in the _params map (call setParams(p) to do that).
   * These are:
   * <ul>
   * <li> &lt;"a",Double&gt; the rate of annealing
   * <li> &lt;"Tmin",Double&gt; the Tmin value which when reached, we stop the annealing.
   * <li> &lt;"lmax",Double&gt; the ëmax(Cx) which instead of being computed must be passed in
   * <li> &lt;"delta",Double&gt; the ä perturbation when creating a new VectorIntf center
   * </ul>
   * Also, before calling the method, the documents to be clustered must have
   * been added to the class object via 
	 * <CODE>addAllVectors(List&lt;VectorIntf&gt;)</CODE> or
   * via repeated calls to <CODE>addVector(VectorIntf d)</CODE>
   *
   * @throws ClustererException if at some iteration, one or more clusters becomes empty.
   * @return List  // Vector&lt;VectorIntf&gt;
   */
  public List clusterVectors() throws ClustererException {
    // 1. init
    final double a = ( (Double) _params.get("a")).doubleValue();
    final double lmax = ( (Double) _params.get("lmax")).doubleValue();
    final double Tmin = ( (Double) _params.get("Tmin")).doubleValue();
    final double delta = ( (Double) _params.get("delta")).doubleValue();
    final int n = _docs.size();
    final int k = ((Integer) _params.get("k")).intValue();
    final int dims = ((VectorIntf) _docs.get(0)).getNumCoords();
    // set a new _eps for deciding when centers have settled
    _eps = _eps > delta/dims ? delta/dims : _eps;
    double[] arr = new double[dims];
    VectorIntf one = new DblArray1Vector(arr);

    double T = 2*lmax+1;
    int k_cur = 2;  // count the duplicate too
    double[] p_y = new double[2*k];  //  p_y[i] = p_{y_i}
    double[][] p_yx = new double[2*k][n];  // p_yx[i=0...2k-1][j=0...n-1] = p_{y[i]|j}
    for (int i=0; i<2*k; i++) {  // init arrays
      p_y[i] = 0.0;
      for (int j=0; j<n; j++) p_yx[i][j] = 0.0;
    }

    if (_centers!=null) _centers.clear();
    else _centers = new Vector();  // _centers is not supposed to be initialized
    VectorIntf y1 = VecUtil.getCenter(_docs);
    VectorIntf y1_dup = y1.newInstance();  // used to be y1.newCopy();  // duplicate the center
    try {
      y1_dup.addMul(delta, one);
    }
    catch (parallel.ParallelException e) {  // can never get here
      e.printStackTrace();
    }
    p_y[0] = 0.5;
    p_y[1] = 0.5;  // duplicate's value
    // put centers in _centers vector
    _centers.add(y1);
    _centers.add(y1_dup);

    try {
      // 2. Main Loop: Deterministic Annealing Algorithm
      while (T > 0) {
        List new_centers = new ArrayList(); // updated centers go here
        int numiter = 0;
        while (true) {
          numiter++;
          utils.Messenger.getInstance().msg("Updating iteration " + numiter,0);
          // compute ||x-y||^2 for each x in _docs, y in _centers
          double dists[][] = new double[n][k_cur];
          for (int i = 0; i < n; i++) {
            VectorIntf x = (VectorIntf) _docs.get(i);
            for (int j = 0; j < k_cur; j++) {
              VectorIntf y = (VectorIntf) _centers.get(j);
              //dists[i][j] = _distmetric.dist(x, y);
              dists[i][j] = VecUtil.norm2(VecUtil.subtract(x,y));
              dists[i][j] *= dists[i][j];  // square it
            }
          }
          // 3. Update
          new_centers.clear();
          for (int i = 0; i < k_cur; i++) {
            for (int j = 0; j < n; j++) {
              // compute p_yx[i][j] by computing its inverse first
              double p_yi_x_inv = 0;
              for (int r=0; r<k_cur; r++) {
                p_yi_x_inv += p_y[r]*Math.exp((-dists[j][r]+dists[j][i])/T);
              }
              p_yx[i][j] = p_y[i]/p_yi_x_inv;
              if (Double.isNaN(p_yx[i][j])) {
                throw new ClustererException("trouble computing p_yx["+i+"]["+j+"]");
              }
              // end new way of updating p_yx[i][j]
            }
            // update p_{y_i}
            p_y[i] = 0;
            for (int j = 0; j < n; j++) p_y[i] += p_yx[i][j];
            p_y[i] /= (double) n;
            // update y_i
            DblArray1Vector y_i = new DblArray1Vector(new double[dims]);
            for (int j = 0; j < n; j++) {
              VectorIntf xj = (VectorIntf) _docs.get(j);
              double mul = p_yx[i][j] / (n * p_y[i]);
              y_i.addMul(mul, xj);
            }
            new_centers.add(y_i);
          }
          // 4. check for convergence
          if (converged(_centers, new_centers)) {
            _centers = new_centers;
            break;
          }
          _centers = new_centers;
        } // while (true) keep updating _centers, p_y[], p_yx[][]
        // 5. exit condition: post-processing K-Means then implements limit for T=0
        if (T <= Tmin)break; //
        // 6. cooling step
        T = a * T;
        utils.Messenger.getInstance().msg("Temperature cooling, T=" + T,1);
        // 7. check phase-transition
        int klim = k_cur;
        if (_centers.size() != k_cur) {
          throw new ClustererException("wrong num clusters"); // sanity check
        }
        for (int j = 0; j < klim; j += 2) {
          if (k_cur < 2 * k && clusterHasPhaseTransition(j, delta)) {
            utils.Messenger.getInstance().msg("Phase Transition for cluster center j=" + j,0);
            VectorIntf yj = (VectorIntf) _centers.get(j);
            // set the duplicate of yj to be the same again
            VectorIntf yj_dup = yj.newInstance();  // yj.newCopy();
            _centers.set(j + 1, yj_dup);
            VectorIntf y_new = yj.newInstance();  // yj.newCopy();
            try {
              y_new.addMul(delta, one);
            }
            catch (parallel.ParallelException e) {  // can never get here
              e.printStackTrace();
            }
            utils.Messenger.getInstance().msg("Added new center (tot.real centers=" +
                               k_cur / 2 + ") : " + y_new,1);
            VectorIntf y_new2 = y_new.newInstance();  // y_new.newCopy();
            try {
              y_new2.addMul(delta, one);
            }
            catch (parallel.ParallelException e) {  // can never get here
              e.printStackTrace();
            }
            _centers.add(y_new);
            _centers.add(y_new2);
            double t = p_y[j];
            p_y[k_cur] = t / 2.0;
            p_y[k_cur + 1] = t / 2.0;
            p_y[j] = t / 2.0;
            p_y[j + 1] = t / 2.0;
            utils.Messenger.getInstance().msg("probability of cluster centers " + j + ", " +
                               k_cur + " set to " + t / 2, 2);
            k_cur += 2; // increment K
          }
        }
      } // 8. repeat while T>=0
    }
    catch (ClustererException e) {
      // exception was thrown because a temperature was reached that denom~0
      // go to zero temperature and perform standard K-Means as a post-processing
      // step from ClusteringTester2 calling method
      // no-op
    }
    // 9. Post-processing steps
    utils.Messenger.getInstance().msg("post-processing: k_cur="+k_cur+" _centers.size()="+_centers.size(),0);
    for (int i=k_cur-1; i>=1; i-=2) {
      _centers.remove(i);
    }
    k_cur = _centers.size();
    //utils.Messenger.getInstance().msg("final k_cur="+k_cur,1);
    _clusterIndices = new int[n];
    for (int j=0; j<n; j++) {
      VectorIntf dj = (VectorIntf) _docs.get(j);
      double mindist = Double.MAX_VALUE;
      int bij = -1;
      for (int i = 0; i < k_cur; i++) {
        VectorIntf ci = (VectorIntf) _centers.get(i);
        //double dij = _distmetric.dist(dj,ci);
        double dij = VecUtil.norm2(VecUtil.subtract(dj,ci));
        dij *= dij;  // square it
        if (dij < mindist) {
          bij = i;
          mindist = dij;
        }
      }
      _clusterIndices[j] = bij;
    }
    if (_clusterIndices==null) {
      throw new ClustererException("null _clusterIndices after running clusterDocs()");
    }
    // test for intermediate clusters
    if (_intermediateClusters.size()==0) {
      // put the final clustering in
      for (int i=0; i<_centers.size(); i++) _intermediateClusters.add(new ArrayList());
      for (int i=0; i<_clusterIndices.length; i++) {
        int c = _clusterIndices[i];
        List vc = (List) _intermediateClusters.get(c);
        vc.add(new Integer(i));
        _intermediateClusters.set(c, vc); // ensure addition
      }
    }
    return _centers;
  }


  /**
   * adds the arg passed in to the vectors to be clustered together.
   * @param d VectorIntf
   */
  public void addVector(VectorIntf d) {
    if (_docs==null) _docs = new ArrayList();
    _docs.add(d);
  }


  /**
   * appends to the end of _docs all Documents in v.
   * Will throw class cast exception if any object in v is not a VectorIntf.
   * @param v List  // Vector&lt;VectorIntf&gt;
   */
  public void addAllVectors(List v) {
    if (v==null) return;
    if (_docs==null) _docs = new ArrayList();
    for (int i=0; i<v.size(); i++)
      _docs.add((VectorIntf) v.get(i));
  }


  /**
   * set the initial clustering centers.
   * The vector _centers is reconstructed, but the Document objects
   * that are the cluster centers are simply passed as references.
   * the _centers doesn't own copies of them, but references to the
   * objects inside the centers vector that is passed in the parameter list.
   * @param centers List // Vector&lt;VectorIntf&gt;
   * @throws ClustererException if argument is null or if any of the contained
   * objects is not a VectorIntf
   */
  public void setInitialClustering(List centers) throws ClustererException {
    if (centers==null) throw new ClustererException("null initial clusters vector");
    List oldcenters = _centers;
    try {
      _centers = new ArrayList();
      for (int i = 0; i < centers.size(); i++)
        _centers.add( (VectorIntf) centers.get(i));
    }
    catch (ClassCastException e) {
      e.printStackTrace();
      _centers = oldcenters;  // restore prior centers, whatever they were
      throw new ClustererException("at least one object in centers vector is not a VectorIntf");
    }
  }

  /**
   * returns current centers.
   * @return List  // Vector&lt;VectorIntf&gt;
   */
  public List getCurrentCenters() {
    return _centers;
  }


  /**
   * return current set of VectorIntf objects to be clustered.
   * @return List  // Vector&lt;VectorIntf&gt;
   */
  public List getCurrentVectors() {
    return _docs;
  }


  /**
   * the clustering params are set to p
   * @param p HashMap
   */
  public void setParams(HashMap p) {
    _params = null;
    _params = new HashMap(p);  // own the params
  }


  /**
   * resets data structures for another clustering call.
   */
  public void reset() {
    _docs = null;
    _centers = null;
    _clusterIndices=null;
    _intermediateClusters.clear();
  }


  /**
   * returns the cluster indices for each of the clustered vectors. This value
   * will be null if neither <CODE>setClusteringIndices()</CODE> nor the
   * <CODE>clusterVectors()</CODE> method have been called prior to this call.
   * @return int[] range is in [0...k-1] where k is the number of clusters
   * required.
   */
  public int[] getClusteringIndices() {
    return _clusterIndices;
  }


  /**
   * sets the clustering indices to the specified int array.
   * @param a int[]
   */
  public void setClusteringIndices(int[] a) {
    if (a==null) _clusterIndices = null;
    else {
      _clusterIndices = new int[a.length];
      for (int i=0; i<a.length; i++)
        _clusterIndices[i] = a[i];
    }
  }


  /**
   * return an array of the cardinality (size) of each cluster specified by the
   * clustering indices of this object.
   * @throws ClustererException
   * @return int[]
   */
  public int[] getClusterCards() throws ClustererException {
    if (_clusterIndices==null)
      throw new ClustererException("null _clusterIndices");
    final int k = _centers.size();
    final int n = _docs.size();
    int[] cards = new int[_centers.size()];
    for (int i=0; i<k; i++) cards[i]=0;
    for (int i=0; i<n; i++) {
      cards[_clusterIndices[i]]++;
    }
    return cards;
  }


  /**
   * evaluate the clustering produced by this object using the argument passed in.
   * @param vtor EvaluatorIntf
   * @throws ClustererException
   * @return double
   */
  public double eval(EvaluatorIntf vtor) throws ClustererException {
    return vtor.eval(this);
  }


  private boolean converged(List centers, List new_centers) throws ClustererException {
    for (int i=0; i<centers.size(); i++) {
      VectorIntf c1 = (VectorIntf) centers.get(i);
      VectorIntf c2 = (VectorIntf) new_centers.get(i);
      //double dist = _distmetric.dist(c1,c2);
      double dist = VecUtil.norm2(VecUtil.subtract(c1,c2));
      dist *= dist;  // square it
      if (dist > _eps) return false;
    }
    return true;
  }


  private boolean clusterHasPhaseTransition(int j, double delta) throws ClustererException {
    VectorIntf dj = (VectorIntf) _centers.get(j);
    VectorIntf djp1 = (VectorIntf) _centers.get(j+1);
    final int dims = dj.getNumCoords();
    double dist = VecUtil.norm2(VecUtil.subtract(dj,djp1));
    dist *= dist;  // square it
    if (dist <= delta*delta*dims+delta)  // add fudge of delta
      return false;
    else return true;
  }
}

