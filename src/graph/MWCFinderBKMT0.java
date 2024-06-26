package graph;

import parallel.*;
import java.util.*;
import java.io.Serializable;


/**
 * class provides a parallel implementation of the Bron-Kerbosch algorithm for
 * computing all maximum cliques in a graph (with pivoting).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class MWCFinderBKMT0 extends AllMWCFinder {
  private FasterParallelAsynchBatchTaskExecutor _executor;
  private ConditionCounter _counter = new ConditionCounter();
  private int _maxDepth = 1;  // use parallelism at each iteration


  /**
   * sole public constructor.
   * @param g Graph
   * @param numthreads int
   * @throws GraphException
   * @throws ParallelException
   */
  public MWCFinderBKMT0(Graph g, int numthreads) 
		throws GraphException, ParallelException {
    super(g);
    _executor = FasterParallelAsynchBatchTaskExecutor.
						      newFasterParallelAsynchBatchTaskExecutor(numthreads, false);
    // don't run on current thread when thread-pool is full
  }


  /**
   * the method computes all maximum cliques in a Graph g so that each node in a
   * resulting clique contributes to the clique with arc weights higher than
   * the specified argument; therefore, it has exponential time and space
   * complexity... Use only on small graphs.
   * The higher the argument, the less cliques it searches for, and the more
   * the computational savings.
   * The method it implements is the Bron &amp; Kerbosch (1973) algorithm
   * BronKerbosch2 (with pivoting from the set P U X) but maintains only the
   * maximum weighted cliques in the graph (and not all maximal weighted ones).
   * It returns the cliques in a Set&lt;Set&lt;Integer nodeId&gt; &gt;
   * @param minaccnodecliqueweight double
   * @return Set // Set&lt;Set&lt;Integer nodeId&gt; &gt;
   */
  public Set getAllMaximalCliques(double minaccnodecliqueweight) {
    final Graph g = getGraph();
    try {
      g.getReadAccess();
      final int n = g.getNumNodes();
      Set P = new HashSet(); // was IntSet
      for (int i = 0; i < n; i++) P.add(new Integer(i));
      Set R = new HashSet();  // was IntSet
      Set X = new HashSet();  // was IntSet
      Vector cliques = new Vector();
      RunBK2Task0 root = 
				new RunBK2Task0(R, P, X, minaccnodecliqueweight, 1, cliques);
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
      // finally remove singleton cliques
      Set result = new HashSet();
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


	/**
	 * set the depth at which parallelism kicks in. Ater that, tasks will be sent
	 * to the executor only when their depth is an integer multiple of this depth.
	 * @param depth int
	 */
  public void setMaxDepth(int depth) {
    _maxDepth = depth;
  }


  private void runBronKerbosch2(Set R, Set P, Set X, double thres, 
		                            RunBK2Task0 task, int depth, Vector cliques) {
    final Graph g = getGraph();
    if (P.size()==0 && X.size()==0) {
      synchronized (cliques) {
        if (cliques.size()>0) {
          Set c = (Set) cliques.elementAt(0);
          int cs = c.size();
          int rs = R.size();
          if (rs>cs) {
            //System.err.println("found clique of size "+rs);
            cliques.clear();
            cliques.add(R);
          }
          else if (rs==cs) {
            cliques.add(R);
          }
        }
        else cliques.add(R);  // cliques was empty
      }
      return;
    }
    Set PUX = new HashSet(P);
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
      Set Rnew = new HashSet(R);
      Rnew.add(nid);
      Set Pnew = new HashSet(P);
      Pnew.retainAll(nidnbors);
      Set Xnew = new HashSet(X);
      Xnew.retainAll(nidnbors);
      if (depth % _maxDepth != 0) {
        runBronKerbosch2(Rnew, Pnew, Xnew, thres, task, depth+1, cliques);
      } else {
        BKtasks.add(new RunBK2Task0(Rnew, Pnew, Xnew, thres, depth+1, cliques));
      }
      P.remove(nid);
      X.add(nid);
    }
    try {
      if (BKtasks.size()>1) {
        _counter.add(BKtasks.size());
        _executor.executeBatch(BKtasks);
      }
      else if (BKtasks.size()==1) {  // run on same thread 2 avoid executor cost
        RunBK2Task0 t = (RunBK2Task0) BKtasks.elementAt(0);
        runBronKerbosch2(t._R, t._P, t._X, thres, t, t._depth, cliques);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);  // oops...
    }
  }


  /**
	 * inner-class to obtain access to method <CODE>runBronKerbosch2()</CODE> and 
	 * <CODE>_counter</CODE>.
	 */
  class RunBK2Task0 implements TaskObject {
    private final static long serialVersionUID = 7312101462888296968L;
    private Set _R;
    private Set _P;
    private Set _X;
    private double _thres;
    private Vector _cliques;  // Vector<Set<Integer> >
    private boolean _isDone = false;
    private int _depth;


    public RunBK2Task0(Set R, Set P, Set X, double thres, 
			                 int depth, Vector cliques) {
      _R = R;
      _P = P;
      _X = X;
      _thres = thres;
      _cliques = cliques;
      _depth = depth;
    }


    public Serializable run() {
      runBronKerbosch2(_R, _P, _X, _thres, this, _depth, _cliques);
      _counter.decrement();
      setDone();
      return this;
    }


    public synchronized boolean isDone() {
      return _isDone;
    }


    public synchronized void copyFrom(TaskObject t) 
			throws IllegalArgumentException {
      throw new IllegalArgumentException("copyFrom(t) method not supported");
    }


    private synchronized void setDone() { 
			_isDone = true; 
		}  // used to be un-synchronized
  }  // end inner-class

}

