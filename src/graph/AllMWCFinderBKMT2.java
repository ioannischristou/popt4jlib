package graph;

import utils.*;
import parallel.*;
import java.util.*;
import java.io.Serializable;

/**
 * class provides a parallel implementation of the Bron-Kerbosch algorithm for
 * computing all maximal cliques in a graph (with pivoting). This implementation
 * runs usually faster but consumes much more memory than the AllMWCFinderBKMT
 * class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class AllMWCFinderBKMT2 extends AllMWCFinder {
  private FasterParallelAsynchBatchTaskExecutor _executor;
  private ConditionCounter _counter = new ConditionCounter();
  private int _maxDepth = 1;  // use parallelism at each iteration


  /**
   * sole public constructor
   * @param g Graph
   * @param numthreads int
   * @throws GraphException
   * @throws ParallelException
   */
  public AllMWCFinderBKMT2(Graph g, int numthreads) throws GraphException, ParallelException {
    super(g);
    _executor = FasterParallelAsynchBatchTaskExecutor.
						newFasterParallelAsynchBatchTaskExecutor(numthreads, false);
    // don't run on current thread when thread-pool is full
  }


  /**
   * the method computes all maximal cliques in a Graph g so that each node in a
   * resulting clique contributes to the clique with arc weights higher than
   * the specified argument; therefore, it has exponential time and space
   * complexity... Use only on small graphs.
   * The higher the argument, the less cliques it searches for, and the more
   * the computational savings.
   * The method it implements is the Bron &amp; Kerbosch (1973) algorithm
   * BronKerbosch2 (with pivoting from the set P U X).
   * It returns the cliques in a Set&lt;Set&lt;Integer nodeId&gt; &gt;
   * @param minaccnodecliqueweight double
   * @return Set // Set&lt;Set&lt;Integer nodeId&gt; &gt;
   */
  public Set getAllMaximalCliques(double minaccnodecliqueweight) {
    final Graph g = getGraph();
    try {
      g.getReadAccess();
      final int n = g.getNumNodes();
      Set P = new IntSet(); // was TreeSet
      for (int i = 0; i < n; i++) P.add(new Integer(i));
      Set R = new IntSet();
      Set X = new IntSet();
      //RunBK2Task2 root = new RunBK2Task2(null, null, null, minaccnodecliqueweight, 1);
      //runBronKerbosch2(R, P, X, minaccnodecliqueweight, root, 1);
      RunBK2Task2 root = new RunBK2Task2(R, P, X, minaccnodecliqueweight, 1);
      Vector ts = new Vector(); ts.add(root);
      try {
        _counter.add(1);
        _executor.executeBatch(ts);
        _counter.await();
        _executor.shutDown();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      // construct cliques
      Set cliques = new TreeSet();  // Set<Set<Integer> >
      gatherCliques(root, cliques);
      // finally remove singleton cliques
      Set result = new TreeSet();
      Iterator it = cliques.iterator();
      while (it.hasNext()) {
        Set si = (Set) it.next();
        if (si.size() > 1) result.add(si);
      }
      return result;
    }
    finally {
      try {
        g.releaseReadAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();  // never gets here
      }
    }
  }


  public void setMaxDepth(int depth) {
    _maxDepth = depth;
  }


  private void runBronKerbosch2(Set R, Set P, Set X, double thres, RunBK2Task2 task, int depth) {
    final Graph g = getGraph();
    if (P.size()==0 && X.size()==0) {
      task._cliques.add(R);
      return;
    }
    Set PUX = new IntSet(P);
    PUX.addAll(X);
    Iterator it = PUX.iterator();
    Integer uid = (Integer) it.next();  // choose u from P U X
    Set unbors = g.getNode(uid.intValue()).getNborIndices(thres);
    unbors.remove(uid);
    it = PUX.iterator();
    Vector BKtasks = new Vector();
    while (it.hasNext()) {
      Integer nid = (Integer) it.next();
      if (unbors.contains(nid)) continue;
      if (P.contains(nid)==false) continue;
      Set nidnbors = g.getNode(nid.intValue()).getNborIndices(thres);
      nidnbors.remove(nid);
      Set Rnew = new IntSet(R);
      Rnew.add(nid);
      Set Pnew = new IntSet(P);
      Pnew.retainAll(nidnbors);
      Set Xnew = new IntSet(X);
      Xnew.retainAll(nidnbors);
      if (depth % _maxDepth != 0) {
        runBronKerbosch2(Rnew, Pnew, Xnew, thres, task, depth+1);
      } else {
        BKtasks.add(new RunBK2Task2(Rnew, Pnew, Xnew, thres, depth+1));
      }
      P.remove(nid);
      X.add(nid);
    }
    try {
      if (BKtasks.size()>1) {
        if (task._children==null) task._children = new Vector();
        task._children.addAll(BKtasks);
        _counter.add(BKtasks.size());
        _executor.executeBatch(BKtasks);
      }
      else if (BKtasks.size()==1) {  // run on same thread to avoid executor costs
        if (task._children==null) task._children = new Vector();
        task._children.addAll(BKtasks);
        RunBK2Task2 t = (RunBK2Task2) BKtasks.elementAt(0);
        runBronKerbosch2(t._R, t._P, t._X, thres, t, t._depth);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);  // oops...
    }
  }


  private void gatherCliques(RunBK2Task2 task, Set result) {
    if (task==null) return;
    result.addAll(task._cliques);
    if (task._children!=null) {
      for (int i=0; i<task._children.size(); i++) {
        gatherCliques((RunBK2Task2) task._children.elementAt(i), result);
      }
    }
  }


  // inner-class to obtain access to method runBronKerbosch2() & _counter
  class RunBK2Task2 implements TaskObject {
    private final static long serialVersionUID = -6557815277268279949L;
    private Set _R;
    private Set _P;
    private Set _X;
    private double _thres;
    private Set _cliques;  // Set<Set<Integer> >
    private Vector _children;  // null
    private boolean _isDone = false;
    private int _depth;


    public RunBK2Task2(Set R, Set P, Set X, double thres, int depth) {
      _R = R;
      _P = P;
      _X = X;
      _thres = thres;
      _cliques = new TreeSet();
      _depth = depth;
    }


    public Serializable run() {
      runBronKerbosch2(_R, _P, _X, _thres, this, _depth);
      _counter.decrement();
      setDone();
      return this;
    }


    public synchronized boolean isDone() {
      return _isDone;
    }


    public synchronized void copyFrom(TaskObject t) throws IllegalArgumentException {
      throw new IllegalArgumentException("copyFrom(t) method not supported");
    }


    private synchronized void setDone() { _isDone = true; }  // used to be un-synchronized
  }  // end inner-class

}

