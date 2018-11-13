package popt4jlib.MSSC;

import parallel.ParallelException;
import popt4jlib.VectorIntf;
import popt4jlib.GradientDescent.VecUtil;
import utils.Messenger;
import parallel.Barrier;
import java.util.*;

/**
 * class implements multi-threaded version of the (hard) K-Means algorithm, in
 * a thread-safe manner. Hard version of the K-Means algorithm means that every
 * vector to be clustered will belong to one and only one cluster.
 * If the <CODE>clusterVectors()</CODE> method is called with parameters set up
 * so that more than 1 thread of control will concurrently execute, the output
 * clustering is guaranteed to be deterministic only if the parameter
 * "gmeansmt.projectonempty" is false (default), else it's not due to
 * thread-scheduling that may result in a different total ordering of the data
 * points being examined and assigned in the first step of any given major
 * iteration. This ordering in the standard K-Means algorithm is irrelevant, but
 * in this version of the K-Means algorithm, if the above-mentioned parameter is
 * set to true, then ordering becomes important because if a cluster is left
 * with only a single data point in it within the range of points a thread is
 * responsible for, then the data point is not allowed to move so as not to
 * leave the cluster empty. Therefore, in such a case, the ordering in which
 * data points are looked at, becomes important.
 * With a single thread of control, the output is always the same given the same
 * initial clustering and the same data points sequence.
 * <p>Notes:
 * <ul>
 * <li>2018-11-13: <CODE>_clusterIndices</CODE> initialization where each center
 * gets one point assigned to it before threads start executing, now only occurs
 * when the "gmeansmt.projectonempty" parameter flag is true.
 * <li>2018-11-08: added barrier in <CODE>GMClustererAux.go1()</CODE> in the
 * beginning of the RUN_ASGNS phase, so that the initialization of the _numi
 * array elements is correct in each thread.
 * <li>2016-01-27: modified data structures to List interface from Vector to 
 * avoid synchronized access to array elements. 
 * <li>2016-01-27: Also, moved auxiliary classes within the GMeansMTClusterer 
 * class so as to restrict all data members access control to private.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.1
 */
final public class GMeansMTClusterer implements ClustererIntf {
  private VectorIntf[] _centersA;  // size=k
  /**
   * maintains the reference VectorIntf points to be clustered
   */
  private List _docs;  // Vector<VectorIntf>, size=n
  /**
   * maintains the algorithm's parameters
   */
  private HashMap _params;
  private int[] _clusterIndices;
  private List _intermediateClusters;  // Vector<Vector<Integer docid>>
  private List _centers;  // Vector<VectorIntf>, size=k
	
	private final Messenger _mger = Messenger.getInstance();


  /**
   * sole public constructor.
   */
  public GMeansMTClusterer() {
    _intermediateClusters = new ArrayList();  // Vector<Vector<Integer docid>>
  }


  /**
   * returns a List&lt;List&lt;Integer docid&gt; &gt; representing the ids of
   * the vectors in each cluster.
   * @return List  // List&lt;List&lt;Integer docid&gt; &gt;
   */
  public synchronized List getIntermediateClusters() {
    // store the final clustering in _intermediateClusters
    int prev_ic_sz = _intermediateClusters.size();
    for (int i=0; i<_centers.size(); i++) {
			_intermediateClusters.add(new ArrayList());
		}
    for (int i=0; i<_clusterIndices.length; i++) {
      int c = _clusterIndices[i];
      List vc = (List) _intermediateClusters.get(prev_ic_sz+c);
      vc.add(new Integer(i));
      _intermediateClusters.set(prev_ic_sz+c, vc);  // ensure addition
    }
    // remove any empty vectors
    for (int i=_intermediateClusters.size()-1; i>=0; i--) {
      List v = (List) _intermediateClusters.get(i);
      if (v==null || v.size()==0) _intermediateClusters.remove(i);
    }
    return _intermediateClusters;
  }


  /**
   * returns the parameters to be used for clustering for this clusterer object.
   * @return HashMap
   */
  public synchronized HashMap getParams() {
    return _params;
  }


  /**
   * the most important method of the class. Some parameters must have been
   * previously passed in the _params map (call <CODE>setParams(p)</CODE> to do
   * that).
   * These are:
   * <ul>
   * <li> &lt;"gmeansmt.TerminationCriteria",ClustererTerminationIntf&gt; the 
	 * object that will decide when to stop the iterations. Mandatory.
   * <li> &lt;"gmeansmt.evaluator",EvaluatorIntf&gt; the object that will 
	 * evaluate a clustering. Mandatory.
   * <li> &lt;"gmeansmt.movable",Vector&lt;Integer clusterind&gt; &gt; optional, 
	 * indicates which clusters are allowed to exchange their documents, if it 
	 * exists. Default is null.
   * <li> &lt;"gmeansmt.numthreads",Integer nt&gt; optional, how many threads 
	 * will be used to compute the clustering, default is 1.
   * <li> &lt;"gmeansmt.projectonempty",Boolean&gt; optional, if false then 
	 * whenever a cluster becomes empty the vector furthest away from any other 
	 * center will become the new center, else the whenever a point is the last in
	 * a cluster and about to be moved out, it is prevented from doing so. 
	 * Default is false.
   * <li> &lt;"gmeansmt.trycompacting",Double factor&gt; optional, if it exists, 
	 * indicates a factor by which (at what would be the normal end of a 
	 * clustering process) the average distance of any data point from its 
	 * assigned cluster center must be multiplied and be greater than the actual 
	 * distance of a data point from its nearest cluster center in order for this 
	 * point to be "broken away" from its assigned cluster; after all such 
	 * "distant" points have been "freed", the cluster centers are re-computed, 
	 * the free data points are re-assigned to their new nearest centers, and the 
	 * stopping criteria are re-evaluated to see if the clustering process must 
	 * continue or not. Default is null.
   * </ul>
   * Also, before calling the method, the documents to be clustered must have
   * been added to the class object via addAllVectors(Vector&lt;VectorIntf&gt;) 
	 * or via repeated calls to addVector(VectorIntf d), and an initial clustering
   * must be available via a call to
   * <CODE>setInitialClustering(Vector&lt;VectorIntf&gt; clusterCenters)</CODE>
   * @throws ClustererException if at some iteration, one or more clusters
   * becomes empty and the "projectonempty" property is true
   * @return List  // Vector&lt;VectorIntf center&gt;
   */
  public synchronized List clusterVectors() throws ClustererException {
    boolean project_on_empty = false;
    Boolean p_o_e = (Boolean) _params.get("gmeansmt.projectonempty");
    if (p_o_e!=null) project_on_empty = p_o_e.booleanValue();
    ClustererTerminationIntf ct = (ClustererTerminationIntf)
        _params.get("gmeansmt.TerminationCriteria");
    EvaluatorIntf evaluator = (EvaluatorIntf) _params.get("gmeansmt.evaluator");
    if (evaluator==null) 
			evaluator = (EvaluatorIntf) _params.get("gmeansmt.codocupevaluator");
    ct.registerClustering(this); // register this clustering problem with ct

    final int n = _docs.size();
    final int k = _centersA.length;
    int num_threads = 1;
    Integer ntI = (Integer) _params.get("gmeansmt.numthreads");
    if (ntI!=null) num_threads = ntI.intValue();
    Double tcI = (Double) _params.get("gmeansmt.trycompacting");
    final double try_compacting = (tcI!=null) ? 
			                              tcI.doubleValue() : -1;  // default false
    double r = Double.MAX_VALUE;
    int ind[];  // to which cluster each doc belongs
    int numi[];  // how many docs each cluster has
    if (_clusterIndices==null) {
      _mger.msg("GMeansMTClusterer.clusterVectors(): init _clusterIndices",2);
      ind = new int[n];  // to which cluster each doc belongs
      numi = new int[k];  // how many docs each cluster has
      for (int i = 0; i < k; i++) numi[i] = 0;
      for (int i = 0; i < n; i++) ind[i] = -1;
			if (project_on_empty) {
				// first assign to each center the closest document to it
				for (int i = 0; i < k; i++) {
					VectorIntf ci = _centersA[i];  // (VectorIntf) _centers.elementAt(i);
					r = Double.MAX_VALUE;
					int best_j = -1;
					for (int j = 0; j < n; j++) {
						if (ind[j] >= 0)continue; // j already taken
						VectorIntf dj = (VectorIntf) _docs.get(j);
						//double distij = distmetric.dist(ci, dj);
						double distij = VecUtil.getEuclideanDistance(ci,dj);  
						// above used to be VecUtil.norm2(VecUtil.subtract(ci,dj));
						distij *= distij;  // square it
						if (Double.compare(distij,r)<0) {
							r = distij;
							best_j = j;
						}
					}
					numi[i] = 1;  
					ind[best_j] = i;
					_mger.msg("GMeansMTClusterer.clusterVectors(): "+
										"initializing process sets pt #"+best_j+" to cluster #"+i, 3);
				}
			}
      _clusterIndices = ind;
      _mger.msg("GMeansMTClusterer.clusterVectors(): "+
                "done initializing _clusterIndices (length="+
				        _clusterIndices.length+")",1);
    }
    else {
      // _clusterIndices already exists, and is assumed to have at least one
      // element for each cluster.
      _mger.msg("GMeansMTClusterer.clusterVectors(): "+
                "_clusterIndices were already initialized (length="+
				        _clusterIndices.length+")",1);
      ind = _clusterIndices;
      numi = getClusterCards();
    }

		long uid = utils.DataMgr.getUniqueId();
		String bname = "barrier.gmeans."+uid;
		try {
			Barrier.setNumThreads(bname, num_threads);
		}
		catch (ParallelException e) {  // cannot happen
			e.printStackTrace();
			System.exit(-1);
		}
    GMClustererThread threads[] = new GMClustererThread[num_threads];
    for (int i=0; i<num_threads; i++) {
      GMClustererAux ai = new GMClustererAux(ind, numi, project_on_empty,
				                                     bname);
      threads[i] = new GMClustererThread(ai);
      threads[i].start();
    }

    final int interval = n/num_threads;
    final int kinterval = k/num_threads;

    // incumbent iteration
    int best_indices[] = new int[n];
    List best_centers = new ArrayList();
    double best_val = Double.MAX_VALUE;
    boolean stop = ct.isDone();
		int num_iters = 0;
    while (!stop) {
      // all threads except last have floor((n-1)/numthreads) work to do
      int starti = 0; int endi = interval;
      for (int t=0; t<num_threads-1; t++) {
        GMClustererAux rt = threads[t].getGMClustererAux();
        rt.runFromTo(starti, endi, GMClustererAux.RUN_ASGNS);
        starti = endi+1;
        endi += interval;
      }
      // last thread
      GMClustererAux last_aux = threads[num_threads-1].getGMClustererAux();
      last_aux.runFromTo(starti, n-1, GMClustererAux.RUN_ASGNS);
      // wait for threads to finish their current task
      for (int t=0; t<num_threads; t++) {
        GMClustererAux rt = threads[t].getGMClustererAux();
        rt.waitForTask();
      }

      // ind[] is already computed correctly as each thread had a ref to it
      _clusterIndices = ind;
/*
      System.err.print("inds: [ ");
      for (int i=0; i<n; i++) {
        System.err.print(ind[i]+" ");
      }
      System.err.println("]");
      System.err.print("centers: [ ");
      for (int i=0; i<k; i++) {
        System.err.print(_centersA[i]+" ");
      }
      System.err.println("]");
*/
      // finally compute the new centers
      // all threads except last have floor((k-1)/numthreads) work to do
      int kstarti = 0; int kendi = kinterval;
      for (int t=0; t<num_threads-1; t++) {
        GMClustererAux rt = threads[t].getGMClustererAux();
        rt.runFromTo(kstarti, kendi, GMClustererAux.RUN_CENTERS);
        kstarti = kendi+1;
        kendi += kinterval;
      }
      // last thread
      last_aux = threads[num_threads-1].getGMClustererAux();
      last_aux.runFromTo(kstarti, k-1, GMClustererAux.RUN_CENTERS);
      // wait for threads to finish their current task
      for (int t=0; t<num_threads; t++) {
        GMClustererAux rt = threads[t].getGMClustererAux();
        rt.waitForTask();
      }
      // now _centersA is also correctly computed
      // compute also _centers
      _centers.clear();
      for (int i=0; i<k; i++) {
        _centers.add(_centersA[i]);
      }

      // incumbent computation
      double new_val = eval(evaluator);
			
			_mger.msg("GMeansMTClusterer.clusterVectors(): "+" #iter="+num_iters+
				        " new clustering value="+new_val, 2);
			++num_iters;
      if (new_val<best_val) {
        best_val = new_val;
        best_centers.clear();
        for (int i=0; i<n; i++) best_indices[i] = _clusterIndices[i];
				// itc20181107: below used to be add(_centersA[i]) which is wrong if
				// project_on_empty is false, as _centersA elements don't get created
				// anew in each RUN_CENTERS phase.
        for (int i=0; i<k; i++) best_centers.add(_centersA[i].newInstance());
      }
      stop = ct.isDone();  // check if we're done
      // break up clusters that are not "compact enough"
      if (stop && try_compacting>=0) {
        // System.err.println("trying compacting clusters");
        try {
          compactClusters(try_compacting);
        }
        catch (ParallelException e) {  // can never get here
          e.printStackTrace();
        }
        stop = ct.isDone();  // see if indeed we're done
      }
    }  // while (!stop)
    // stop threads
    for (int t=0; t<num_threads; t++) {
      threads[t].getGMClustererAux().setFinish();
    }
    if (_clusterIndices==null) {
      throw new ClustererException("null _clusterIndices after running "+
				                           "clusterDocs()");
    }
    // set incumbent
    for (int i=0; i<n; i++) _clusterIndices[i] = best_indices[i];
    _centers = best_centers;
    return _centers;
  }


  /**
   * appends the argument to the current _docs collection.
   * @param d VectorIntf
   */
  public synchronized void addVector(VectorIntf d) {
    if (_docs==null) _docs = new ArrayList();
    _docs.add(d);
  }


  /**
   * adds to the end of _docs all VectorIntf's in v.
   * Will throw class cast exception if any object in v is not a VectorIntf
   * @param v List // Vector&lt;VectorIntf&gt;
	 * @throws ClassCastException 
   */
  public synchronized void addAllVectors(List v) {
    if (v==null) return;
    if (_docs==null) _docs = new ArrayList();
    for (int i=0; i<v.size(); i++)
      _docs.add((VectorIntf) v.get(i));
		_mger.msg("GMeansMTClusterer.addAllVectors(v): added "+v.size()+" points", 
			        1);
  }


  /**
   * set the initial clustering centers. The vector _centers is reconstructed,
   * as well as the centers (ie deep copy is performed).
   * @param centers List // Vector&lt;VectorIntf&gt;
   * @throws ClustererException if any object in centers is not a VectorIntf
   */
  public synchronized void setInitialClustering(List centers) 
		throws ClustererException {
    if (centers==null || centers.size()==0)
      throw new ClustererException("null or empty initial clusters vector");
    _centersA = new VectorIntf[centers.size()];
    _centers = null;  // force gc
    _centers = new ArrayList();
    try {
      for (int i = 0; i < centers.size(); i++) {
        _centersA[i] = ((VectorIntf) centers.get(i)).newInstance();  
        // newInstance() above used to be newCopy();
        _centers.add(_centersA[i]);
      }
			_mger.msg("GMeansMTClusterer.setInitialClustering(): "+
				        _centersA.length+" centers set in clusterer.",2);
    }
    catch (ClassCastException e) {
      throw new ClustererException("at least one object in centers "+
				                           "is not a VectorIntf");
    }
  }


  /**
   * returns the current centers.
   * @return List // Vector&lt;VectorIntf center&gt;
   */
  public synchronized List getCurrentCenters() {
    return _centers;
  }


  /**
   * returns the current VectorIntf objects to be (or that have already been)
   * clustered.
   * @return List // Vector&lt;VectorIntf doc&gt;
   */
  public synchronized List getCurrentVectors() {
    return _docs;
  }


  /**
   * the clustering params are set to p
   * @param p HashMap
   */
  public synchronized void setParams(HashMap p) {
    _params = null;
    _params = new HashMap(p);  // own the params
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
   * break up any clusters that do not appear compact.
   * The heuristic is the following:
   * for any cluster that contains points that are more than 2.1*ave_dist away
   * from the center, remove all points that are more than ave_dist away from
   * their center, recompute the center, and assign the removed points to their
   * nearest center
   */
  private void compactClusters(double trycompacting) 
		throws ClustererException, ParallelException {
    final int k = _centersA.length;
    final int n = _docs.size();
    // 1. compute average distance from center for each cluster
    double ave_dist[] = new double[k];
    for (int i=0; i<k; i++) ave_dist[i] = 0.0;
    for (int i=0; i<n; i++) {
      int c = _clusterIndices[i];
      VectorIntf di = (VectorIntf) _docs.get(i);
      VectorIntf cl = _centersA[c];
      //ave_dist[c] += Document.d(di, cl);
			double d2 = VecUtil.getEuclideanDistance(di, cl);  
      // itc20181103: above used to be VecUtil.norm2(VecUtil.subtract(di,cl));
			ave_dist[c] += d2;
    }
    int cards[] = getClusterCards();
    for (int i=0; i<k; i++) {
      ave_dist[i] /= cards[i];
    }
    // 2. find every document that is further than 2.1*ave_dist away
    // and unassign it
    int changed[] = new int[k];
    for (int i=0; i<k; i++) changed[i] = 0;  // init
    for (int i=0; i<n; i++) {
      int c = _clusterIndices[i];
      VectorIntf di = (VectorIntf) _docs.get(i);
      VectorIntf cl = _centersA[c];
      // double dist = Document.d(di, cl);
      double dist = VecUtil.getEuclideanDistance(di,cl);  
      // above used to be VecUtil.norm2(VecUtil.subtract(di,cl));
      if (dist>trycompacting*ave_dist[c]) {
        changed[c]++;
        _clusterIndices[i] = -1;
      }
    }
    // 3. recompute the centers
    for (int i=0; i<k; i++) {
      if (changed[i]>0) {
        VectorIntf ci = _centersA[i];
        // ci = new Document(new TreeMap(), ci.getDim());  // reset ci
        ci = ci.newCopyMultBy(0);  // reset ci
        _centers.set(i, ci);
        _centersA[i] = ci;
      }
    }
    for (int i=0; i<n; i++) {
      VectorIntf di = (VectorIntf) _docs.get(i);
      int c = _clusterIndices[i];
      if (c>=0 && changed[c]>0) {
        VectorIntf cl = _centersA[c];
        cl.addMul(1.0, di);
        _centers.set(c, cl);
        _centersA[c] = cl;
      }
    }
    for (int i=0; i<k; i++) {
      if (changed[i]>0) {
        VectorIntf ci = _centersA[i];
        ci.div((cards[i]-changed[i]));
        _centers.set(i, ci);
        _centersA[i] = ci;
      }
    }
    // 4. reassign the unassigned points
    for (int i=0; i<n; i++) {
      if (_clusterIndices[i]==-1) {  // it is unassigned
        VectorIntf di = (VectorIntf) _docs.get(i);
        double best = Double.MAX_VALUE;
        int best_ind = -1;
        for (int j=0; j<k; j++) {
          VectorIntf cj = _centersA[j];
          // double dist = Document.d(di, cj);
          double dist = VecUtil.getEuclideanDistance(di,cj);  
          // above used to be VecUtil.norm2(VecUtil.subtract(di,cj));
          if (dist<best) {
            best = dist;
            best_ind = j;
          }
        }
        _clusterIndices[i] = best_ind;
      }
    }
    // 5. finally compute the new centers
    _centers = getCenters(_docs, _clusterIndices, k);
    for (int i=0; i<k; i++) _centersA[i] = (VectorIntf) _centers.get(i);
  }


  /**
   * compute the cluster centers of this clustering described in the args
   * the clusterindices[] values range from [0...num_clusters-1].
   * @param docs List // Vector&lt;VectorIntf&gt;
   * @param clusterindices int[]
   * @param k int
   * @throws ClustererException
   * @return List  // Vector&lt;VectorIntf&gt;
   */
  private static List getCenters(List docs, int[] clusterindices, int k)
      throws ClustererException {
    final int docs_size = docs.size();
    List centers = new ArrayList();  // Vector<VectorIntf>
    for (int i=0; i<k; i++)
      centers.add(((VectorIntf) docs.get(0)).newCopyMultBy(0));
    int[] cards = new int[k];
    for (int i=0; i<k; i++) cards[i]=0;

    for (int i=0; i<docs_size; i++) {
      int ci = clusterindices[i];
      VectorIntf centeri = (VectorIntf) centers.get(ci);
      VectorIntf di = (VectorIntf) docs.get(i);
      try {
        centeri.addMul(1.0, di);
      }
      catch (parallel.ParallelException e) {  // can never get here
        e.printStackTrace();
      }
      cards[ci]++;
    }
    // divide by cards
    for (int i=0; i<k; i++) {
      VectorIntf centeri = (VectorIntf) centers.get(i);
      try {
        centeri.div((double) cards[i]);
      }
      catch (parallel.ParallelException e) {  // can never get here
        e.printStackTrace();
      }
    }
    return centers;
  }


	/**
	 * nested auxiliary class to the GMeansMTClusterer class implementing the hard
	 * 2-step process of K-Means clustering, taking into account constraints on
	 * vectors that must not be moved (if they exist), and requirements for 
	 * non-null clusters as well. Clearly not part of the public API.
	 * <p>Title: popt4jlib</p>
	 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
	 * <p>Copyright: Copyright (c) 2011-2018</p>
	 * <p>Company: </p>
	 * @author Ioannis T. Christou
	 * @version 1.0
	 */
	class GMClustererAux {
		private int _ind[];  // shared reference to master _clusterIndices
		private int _numi[];  // private thread-local array of cluster cardinalities
		private int _starti=-1;
		private int _endi=-1;  // the indices to work on [_starti, _endi]
		private boolean _finish = false;
		private boolean _projectOnEmpty = false;
		private int _what2Run=0;
		final static int RUN_ASGNS=1;
		final static int RUN_CENTERS=2;
		
		private Barrier _b;
		

		GMClustererAux(int[] ind, int[] numi, boolean projectOnEmpty, 
			             String barriername) {
			//_master = master;
			_projectOnEmpty = projectOnEmpty;
			_ind = ind;  // ref. to _master._clusterIndices
			_numi = new int[numi.length];  // holds the thread-local version of 
			                               // cluster cardinalities
			for (int i=0; i<numi.length; i++) _numi[i] = numi[i];  // own private copy
			_b = Barrier.getInstance(barriername);
		}


		void go() {
			while (getFinish()==false) {
				try {
					go1();
				}
				catch (ParallelException e) {  // can never get here
					e.printStackTrace();
				}
			}
		}


		synchronized boolean getFinish() {
			return _finish;
		}


		synchronized void setFinish() {
			_finish = true;
			notify();
		}


		synchronized void waitForTask() {
			while (_starti!=-1 || _endi!=-1) {
				try {
					wait();  // wait as other operation is still running
				}
				catch (InterruptedException e) {
					// no-op
				}
			}
		}


		synchronized void runFromTo(int starti, int endi, int what2run) {
			while (_starti!=-1 || _endi!=-1) {
				try {
					wait();  // wait as other operation is still running
				}
				catch (InterruptedException e) {
					// no-op
				}
			}
			// OK, now set values
			_starti = starti; _endi = endi;
			_what2Run = what2run;
			notify();
		}


		/**
		 * the main method, implementing both steps in classical K-Means clustering,
		 * namely the vector assignment, and the centers update phase.
		 * @throws ParallelException -can never really throw this exception, but
		 * compiler thinks so.
		 */
		private synchronized void go1() throws ParallelException {
			while (_starti==-1 || _endi==-1) {
				if (_finish) return;  // finish
				try {
					wait();  // wait for order
				}
				catch (InterruptedException e) {
					// no-op
				}
			}
			// run the code
			if (_what2Run==RUN_ASGNS) {
				// step 1. Assign each vector to its nearest cluster center
				VectorIntf[] centers = _centersA;
				List docs = _docs;
				int k = centers.length;
				int n = docs.size();
				// must first update _numi
				for (int i = 0; i < k; i++) _numi[i] = 0;
				for (int i = 0; i < n; i++) {
					if (_clusterIndices[i]>=0) _numi[_clusterIndices[i]]++;
				}
				// itc20181107: barrier needed here to prevent threads from modifying
				// _clusterIndices
				_b.barrier();
				double r;
				Vector movable = (Vector) _params.get("gmeansmt.movable");
				boolean movable_exists = (movable != null);
				for (int i = _starti; i <= _endi; i++) {
					r = Double.MAX_VALUE;
					VectorIntf di = (VectorIntf) docs.get(i);
					if (_ind[i]>=0 && _numi[_ind[i]]<=0) {  // sanity test
						System.err.println("i="+i+" _ind[i]="+_ind[i]+
							                 " _numi[i]="+_numi[i]);
						System.exit(-1);
					}
					if (_ind[i] >= 0 && _numi[_ind[i]] == 1 && _projectOnEmpty) {
						/*
						_mger.msg("GMeansMTClusterer.GMClustererAux.go1(): "+
							        "RUN_ASGNS doesn't move point #"+i+
							        " as cluster #"+_ind[i]+" has single pt",2);
						*/
						continue; // don't move the Document as it's the only one
						// at least among the docs in the [_starti, _endi] index range
					}
					if (movable_exists && _clusterIndices != null &&
							movable.contains(new Integer(_clusterIndices[i]))) {
						_ind[i] = _clusterIndices[i];
						continue; // data-point i is not allowed to move
					}
					for (int l = 0; l < k; l++) {
						if (movable_exists && _ind[i] != l && 
							  movable.contains(new Integer(l)) == false)
							continue; // cannot move to partition l
						VectorIntf cl = (VectorIntf) centers[l];
						try {
							r = compareAndAssign(i, l, r, di, cl);  
              // was synchronized but no need, only penalty(?)
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}  // for l (center-index)
				}   // for i (doc-index)
			}
			else if (_what2Run==RUN_CENTERS) {
				// step 2: Recompute each cluster center
				VectorIntf[] centers = _centersA;
				List docs = _docs;
				int[] ind = _clusterIndices;
				int k = centers.length;
				int n = docs.size();
				int dim = ((VectorIntf) docs.get(0)).getNumCoords();
				//int numi[] = new int[k];
				if (_projectOnEmpty) {
          // if _projectOnEmpty is false, don't nullify centers
					for (int i = _starti; i <= _endi; i++) centers[i] = null;
				}
				// implement hard K-Means
				for (int i=_starti; i<=_endi; i++) {
					int numi = 0;
					boolean v_asgned = false;
					VectorIntf v=null;
					if (_projectOnEmpty) 
						v = new popt4jlib.DblArray1Vector(new double[dim]);
					else v = centers[i];
					boolean first_time = true;
					for (int j=0; j<n; j++) {
						if (ind[j]==i) {
							if (first_time) {  // reset
								for (int m=0; m<v.getNumCoords(); m++) v.setCoord(m,0.0);
								first_time=false;
							}
							v.addMul(1.0, (VectorIntf) docs.get(j));
							v_asgned = true;
							++numi;  // ++numi[i];
						}
					}
					if (v_asgned || _projectOnEmpty) {
						v.div((double)numi);  // v.div(numi[i]);
						if (_projectOnEmpty) centers[i] = v;
					}
					else if (!_projectOnEmpty) {  // condition is redundant
						_mger.msg("GMeansMTClusterer.CMClustererAux.go1(): RUN_CENTERS: "+
							        "re-assigning center #"+i+" to most distant point",3);
						// centers[i] has no assigned points,
						// assign it to most distant point from thread's centers
						double max_dist = -1.0;
						VectorIntf d_max = null;
						for (int j=0; j<n; j++) {
							VectorIntf dj = (VectorIntf) docs.get(j);
							for (int m=_starti; m<=_endi; m++) {  // used to be m<=i;
								if (m==i) continue;
								VectorIntf cm = (VectorIntf) centers[m];
								double djm = VecUtil.getEuclideanDistance(dj,cm);
								if (Double.compare(djm,max_dist)>=0) {
									max_dist = djm;
									d_max = dj;
								}
							}
						}
						centers[i] = d_max.newInstance();  // d_max.newCopy();
					}
				}  // for i in [_starti, _endi] centers
			}
			// finished, reset indices
			_starti=-1; _endi=-1;
			notify();
		}


		/**
		 * used to be synchronized, but there is no need for it.
		 * @param i int
		 * @param l int
		 * @param r double
		 * @param di VectorIntf
		 * @param cl VectorIntf
		 * @return double
		 */
		private double compareAndAssign(int i, int l, double r, 
			                              VectorIntf di, VectorIntf cl) {
			double rl = VecUtil.getEuclideanDistance(di,cl);  
      // above was VecUtil.norm2(VecUtil.subtract(di, cl));
			rl *= rl; // square it
			if (Double.compare(rl,r)<0) {
				r = rl;
				if (_ind[i] >= 0 && _numi[_ind[i]] > 0) --_numi[_ind[i]];
				_numi[l]++;
				_ind[i] = l;
			}
			return r;
		}
	}


	/**
	 * nested auxiliary class to the GMeansMTClusterer class, implementing the 
	 * threads to be used in K-Means computations. Clearly not part of the public
	 * API.
	 * <p>Title: popt4jlib</p>
	 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
	 * <p>Copyright: Copyright (c) 2011-2016</p>
	 * <p>Company: </p>
	 * @author Ioannis T. Christou
	 * @version 1.0
	 */
	class GMClustererThread extends Thread {
		private GMClustererAux _r=null;


		GMClustererThread(GMClustererAux r) {
			_r = r;
		}


		public void run() {
			//System.err.println("GMClustererThread.run(): starting");
			_r.go();
			//System.err.println("GMClustererThread.run(): done");
		}


		GMClustererAux getGMClustererAux() {
			return _r;
		}
	}

}

