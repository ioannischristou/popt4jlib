package graph;

import utils.*;
import parallel.*;
import java.util.*;
import java.io.Serializable;

/**
 * class provides a parallel implementation of the Bron-Kerbosch algorithm for
 * computing all maximal cliques in a graph (with pivoting).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class AllMWCFinderBKMT extends AllMWCFinder {
  private FasterParallelAsynchBatchTaskExecutor _executor;
  private ConditionCounter _counter = new ConditionCounter();


  /**
   * sole public constructor
   * @param g Graph
   * @param numthreads int
   * @throws GraphException
   * @throws ParallelException
   */
  public AllMWCFinderBKMT(Graph g, int numthreads) throws GraphException, ParallelException {
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
      Set cliques = new HashSet();
      runBronKerbosch2(R, P, X, minaccnodecliqueweight, cliques);
      try {
        _counter.await();
        _executor.shutDown();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
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

  private void runBronKerbosch2(Set R, Set P, Set X, double thres,
                                Set cliques) {
    final Graph g = getGraph();
    if (P.size()==0 && X.size()==0) {
      synchronized(cliques) {
        cliques.add(R);
      }
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
      // runBronKerbosch2(Rnew, Pnew, Xnew, thres, cliques);
      BKtasks.add(new RunBK2Task(Rnew, Pnew, Xnew, thres, cliques));
      P.remove(nid);
      X.add(nid);
    }
    try {
      if (BKtasks.size()>0) {
        _counter.add(BKtasks.size());
        _executor.executeBatch(BKtasks);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);  // oops...
    }
  }


  // inner-class to obtain access to method runBronKerbosch2() & _counter
  class RunBK2Task implements TaskObject {
    private final static long serialVersionUID = -6412089304129297325L;
    private Set _R;
    private Set _P;
    private Set _X;
    private double _thres;
    private Set _cliques;
    private boolean _isDone = false;

    public RunBK2Task(Set R, Set P, Set X, double thres, Set cliques) {
      _R = R;
      _P = P;
      _X = X;
      _thres = thres;
      _cliques = cliques;
    }


    public Serializable run() {
      runBronKerbosch2(_R, _P, _X, _thres, _cliques);
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

