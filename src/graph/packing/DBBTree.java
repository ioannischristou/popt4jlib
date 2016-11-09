package graph.packing;

import graph.*;
import parallel.*;
import parallel.distributed.*;
import utils.*;
import java.util.*;
import java.io.*;
import popt4jlib.AllChromosomeMakerClonableIntf;

/**
 * singleton class represents the Branch&amp;Bound Tree of the distributed 
 * version of the BB-GASP method for MWIS.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class DBBTree {

  private Graph _g;
  private int _gsz;  // cache
  private int _maxnndeg;  // cache
  private Set _incumbent;
  private boolean _cutNodes=false;
	private boolean _sortBestCandsInGBNS2A = false;
  private boolean _localSearch = false;
	private AllChromosomeMakerClonableIntf _localSearchMovesMaker = null;
	private double _expandLocalSearchF = 1.0;
  private double _bound;
  private DBBNode1 _root2;
  private int _maxNodesAllowed=Integer.MAX_VALUE;
  private int _counter=0;
  private int _parLvl;
  private boolean _useMaxSubsets=true;
  private int _recentMaxLen=-1;
  private int _tightenUpperBoundLvl=Integer.MAX_VALUE;  // obtain a tighter upper bound after this level
  private int _maxChildrenNodesAllowed=Integer.MAX_VALUE;
  private int _maxItersInGBNS2A=100000;
  private double _avgPercExtraNodes2Add=0.0;
	private boolean _useGWMIN24BestNodes2Add=false;
	private double _minKnownBound = Double.NEGATIVE_INFINITY;
  private DBBNodeComparatorIntf _bbnodecomp = null;
	private final int _k=1;
	private int _totLeafNodes=0;  // counts total leaf nodes created (representing a maximal solution)
	private long _numDLSPerformed=0;  // counts total number of local-searches done in this tree
	private long _timeSpentOnDLS = 0;  // counts total time spend on local-searches in this tree

	private transient DConditionCounterLLCClt _condCounterClt;
	
	private static DBBTree _instance = null;
	
	
	public static synchronized DBBTree getInstance() {
		return _instance;
	}
	
	
	public static synchronized void init(Graph g, double initbound, 
		                                   String pdahost, int pdaport, 
																			 String cchost, int ccport,
																			 String acchost, int accport) {
		if (_instance==null) {
			_instance = new DBBTree(g, initbound);
			try {
				// set-up the distributed components clients
				PDAsynchBatchTaskExecutorClt.setHostPort(pdahost, pdaport);
				// send the pda init-cmd
				PDAsynchBatchTaskExecutorClt.getInstance().sendInitCmd(new PDAInitDBBTreeCmd(g,initbound,pdahost,pdaport,cchost,ccport,acchost,accport));
				String ccname = "DCondCntCoord_"+cchost+"_"+ccport;
				DAccumulatorClt.setHostPort(acchost, accport);
				_instance._condCounterClt = new DConditionCounterLLCClt(cchost, ccport, ccname);
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	
  private DBBTree(Graph g, double initbound) {
    if (g==null) throw new IllegalArgumentException("null graph passed in BBTree ctor");
		_g = g;
    // elements of the argument
    _parLvl = 2;
    _gsz = _g.getNumNodes();
    _bound = initbound;
		_root2 = new DBBNode1(null,0,0);
  }


  void run() throws InterruptedException, IOException, ClassNotFoundException,
		                ParallelException, PDAsynchBatchTaskExecutorException {
    _g.makeNNbors(true);  // ensure _nnbors cache for every node is ok before starting
    _maxnndeg=0;
    for (int i=0; i<_gsz; i++) {
      int nndeg = _g.getNode(i).getNNbors().size();
      if (nndeg>_maxnndeg) _maxnndeg = nndeg;
    }
    // System.err.println("Done making NNbors");
		// kick-start condition-counter
		_condCounterClt.increment();
		// send _root2 to the PDAsynchBatchTaskExecutorSrv via a Clt.
		TaskObject[] tasks = new TaskObject[1];
		tasks[0] = _root2;
		PDAsynchBatchTaskExecutorClt.getInstance().submitWorkFromSameHost(tasks);

		// and now must wait for the process to end via the DConditionCounterClt 
		// connected to the corresponding Srv.
		_condCounterClt.await();
		
		// finally, get the solution from the accumulator!
		IntSet sol = (IntSet) DAccumulatorClt.getArgMax();
		// print value
		double w=0;
		Iterator it = sol.iterator();
		while (it.hasNext()) {
			int nid = ((Integer) it.next()).intValue();
			double nw = _g.getNodeUnsynchronized(nid).getWeightValueUnsynchronized("value");
			w += nw;
		}
    System.out.println("Soln found of size "+sol.size()+" and weight "+w);
  }


  Graph getGraph() { return _g; }
  int getGraphSize() { return _gsz; }
  int getGraphMaxNNDegree() { return _maxnndeg; }

  int getParLvl() { return _parLvl; }

  void setMaxNodesAllowed(int n) {
    _maxNodesAllowed = n;
  }
  int getMaxNodesAllowed() { return _maxNodesAllowed; }

  double getAvgPercExtraNodes2Add() { return _avgPercExtraNodes2Add; }
  void setAvgPercExtraNodes2Add(double p) { _avgPercExtraNodes2Add = p; }
	boolean getUseGWMIN24BestNodes2Add() { return _useGWMIN24BestNodes2Add; }
	void setUseGWMIN24BestNodes2Add(boolean v) { _useGWMIN24BestNodes2Add=v; }
	void setLocalSearchExpandFactor(double v) { _expandLocalSearchF = v; }
	double getLocalSearchExpandFactor() { return _expandLocalSearchF; }
	

  synchronized DBBNodeComparatorIntf getDBBNodeComparator() {
    // synchronized to keep FindBugs happy
    if (_bbnodecomp == null) {
      _bbnodecomp = new DefDBBNodeComparator();
    }
    return _bbnodecomp;
  }
  synchronized void setDBBNodeComparator(DBBNodeComparatorIntf c) {
    if (_bbnodecomp!=null) return;  // can only set once
    _bbnodecomp = c;
  }
	
	synchronized void incrementTotLeafNodes() {
		++_totLeafNodes;
	}
	synchronized int getTotLeafNodes() {
		return _totLeafNodes;
	}
	
	DConditionCounterLLCClt getDConditionCounterClt() { return _condCounterClt; }
	
  void setUseMaxSubsets(boolean v) { _useMaxSubsets=v; }
  boolean getUseMaxSubsets() { return _useMaxSubsets; }

  void setRecentMaxLen(int len) { _recentMaxLen=len; }
  int getRecentMaxLen() { return _recentMaxLen; }

  void setMaxAllowedItersInGBNS2A(int iters) { _maxItersInGBNS2A=iters; }
  int getMaxAllowedItersInGBNS2A() { return _maxItersInGBNS2A; }

  void setMaxChildrenNodesAllowed(int n) { _maxChildrenNodesAllowed=n; }
  int getMaxChildrenNodesAllowed() { return _maxChildrenNodesAllowed; }

	void setSortBestCandsInGBNS2A(boolean v) { 
		_sortBestCandsInGBNS2A = v; 
	}
	boolean getSortBestCandsInGBNS2A() {
		return _sortBestCandsInGBNS2A;
	}
	void setMinKnownBound(double v) { _minKnownBound = v; }
	double getMinKnownBound() { return _minKnownBound; }
  synchronized void setTightenUpperBoundLvl(int lvl) {
    _tightenUpperBoundLvl=lvl;
  }
  synchronized int getTightenUpperBoundLvl() { return _tightenUpperBoundLvl; }
  synchronized void reduceTightenUpperBoundLvl() {
    if (_tightenUpperBoundLvl>2 && _tightenUpperBoundLvl!=Integer.MAX_VALUE)
      _tightenUpperBoundLvl = (int) (_tightenUpperBoundLvl*0.8);
  }
  void setCutNodes(boolean v) { _cutNodes = v; }
  boolean getCutNodes() { return _cutNodes; }
	
  void setLocalSearch(boolean v) { _localSearch = v; }
  boolean getLocalSearch() { return _localSearch; }
	void setLocalSearchType(AllChromosomeMakerClonableIntf maker) {
		_localSearchMovesMaker = maker;
	}
	AllChromosomeMakerClonableIntf getNewLocalSearchMovesMaker() { 
		return _localSearchMovesMaker==null ?
						null : _localSearchMovesMaker.newInstance();
	}
	/**
	 * get the total number of local-searches performed in this BB-Tree.
	 * @return long
	 */
	synchronized long getNumDLSPerformed() {
		return _numDLSPerformed;
	}
	/**
	 * stats book-keeping of DLS's performed.
	 */
	synchronized void incrNumDLSPerformed() { ++_numDLSPerformed; }
	synchronized void incrTimeSpentOnDLS(long dur) { _timeSpentOnDLS += dur; }
	synchronized long getTimeSpentOnDLS() { return _timeSpentOnDLS; }
  synchronized int incrementCounter() { return ++_counter; }
  synchronized int getCounter() { return _counter; }  // used to be unsynchronized


  synchronized void setIncumbent(DBBNode1 n) {
		double ncost = n.getCost();
    if (ncost>_bound) {
      //_incumbent = n;
      _incumbent = new HashSet(n.getNodes());
      _bound = ncost;
      System.err.println("new soln found w/ val=" + _bound);
			// send solution to accumulator
			IntSet sol = new IntSet();
			Iterator it = _incumbent.iterator();
			while (it.hasNext()) {
				sol.add(new Integer(((Node) it.next()).getId()));
			}
			try {
				DAccumulatorClt.addArgDblPair(sol, ncost);
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
    }
  }
/*
  public void setSolution(Set sol) {
    _incumbent = new HashSet(sol);
    _bound = sol.size();
  }
*/
  synchronized int[] getSolution() {  // synchronized to keep FindBugs happy...
    int inds[] = new int[_gsz];
    Iterator it = _incumbent.iterator();
    while (it.hasNext()) {
      Node ni = (Node) it.next();
      inds[ni.getId()]=1;
    }
    return inds;
  }

  synchronized double getBound() { return _bound; }  // used to be unsynchronized

}

