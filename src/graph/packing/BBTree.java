package graph.packing;

import graph.*;
import parallel.*;
import java.util.*;

/**
 * represents the Branch&Bound Tree of the method.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class BBTree {

  private Graph _g;
  private int _gsz;  // cache
  private int _maxnndeg;  // cache
  private Set _incumbent;
  private BBQueue _q;
  private int _maxQSz=Integer.MAX_VALUE;
  private boolean _cutNodes=false;
  private boolean _localSearch = false;
  private double _bound;
  private BBNodeBase _root2;
  private int _maxNodesAllowed=Integer.MAX_VALUE;
  private int _counter=0;
  private int _parLvl;
  private boolean _useMaxSubsets=true;
  private int _recentMaxLen=-1;
  private int _tightenUpperBoundLvl=Integer.MAX_VALUE;  // obtain a tighter upper bound after this level
  private int _maxChildrenNodesAllowed=Integer.MAX_VALUE;
  private int _maxItersInGBNS2A=100000;
  private double _avgPercExtraNodes2Add=0.0;
  private BBNodeComparatorIntf _bbnodecomp = null;
	private int _k;  // _k is the parameter specifying the type of problem 
	                 // 1 means max. weighted independent set problem, 
	                 // 2 means 2-packing problem.
	private int _totLeafNodes=0;  // counts total leaf nodes created (representing a maximal solution)


  BBTree(Graph g, double initbound, int k) throws PackingException {
    if (g==null) throw new IllegalArgumentException("null graph passed in BBTree ctor");
		_g = g;
    // elements of the argument
    _parLvl = 2;
    _gsz = _g.getNumNodes();
    _bound = initbound;
		_k = k;
		switch (_k) {
			case 1: _root2 = new BBNode1(this, null, null); break;
			case 2: _root2 = new BBNode2(this, null, null); break;
			default: throw new IllegalArgumentException("k must be in the set {1,2}");
		}
  }


  void run() throws InterruptedException, ParallelException {
    _g.makeNNbors(true);  // ensure _nnbors cache for every node is ok before starting
    _maxnndeg=0;
    for (int i=0; i<_gsz; i++) {
      int nndeg = _g.getNode(i).getNNbors().size();
      if (nndeg>_maxnndeg) _maxnndeg = nndeg;
    }
    System.err.println("Done making NNbors");  // itc: HERE rm asap
    // use BBQueue
    _q = new BBQueue(_maxQSz, _recentMaxLen);
    _q.insertNode(_root2);
    _q.start();
    while (!_q.isDone()) {
      Thread.currentThread().sleep(100);
    }
    System.out.println("Soln found: "+getBound());  // keep FindBugs happy...
  }


  Graph getGraph() { return _g; }
  int getGraphSize() { return _gsz; }
  int getGraphMaxNNDegree() { return _maxnndeg; }

  int getParLvl() { return _parLvl; }

  BBQueue getQueue() { return _q; }


  void setMaxNodesAllowed(int n) {
    _maxNodesAllowed = n;
  }
  int getMaxNodesAllowed() { return _maxNodesAllowed; }

  double getAvgPercExtraNodes2Add() { return _avgPercExtraNodes2Add; }
  void setAvgPercExtraNodes2Add(double p) { _avgPercExtraNodes2Add = p; }

  void setMaxQSize(int s) {
    _maxQSz = s;
  }

  synchronized BBNodeComparatorIntf getBBNodeComparator() {
    // synchronized to keep FindBugs happy
    if (_bbnodecomp == null) {
      _bbnodecomp = new DefBBNodeComparator();
    }
    return _bbnodecomp;
  }
  synchronized void setBBNodeComparator(BBNodeComparatorIntf c) {
    if (_bbnodecomp!=null) return;  // can only set once
    _bbnodecomp = c;
  }
	
	synchronized void incrementTotLeafNodes() {
		++_totLeafNodes;
	}
	synchronized int getTotLeafNodes() {
		return _totLeafNodes;
	}
	
  void setUseMaxSubsets(boolean v) { _useMaxSubsets=v; }
  boolean getUseMaxSubsets() { return _useMaxSubsets; }

  void setRecentMaxLen(int len) { _recentMaxLen=len; }
  int getRecentMaxLen() { return _recentMaxLen; }

  void setMaxAllowedItersInGBNS2A(int iters) { _maxItersInGBNS2A=iters; }
  int getMaxAllowedItersInGBNS2A() { return _maxItersInGBNS2A; }

  void setMaxChildrenNodesAllowed(int n) { _maxChildrenNodesAllowed=n; }
  int getMaxChildrenNodesAllowed() { return _maxChildrenNodesAllowed; }

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
  synchronized int incrementCounter() { return ++_counter; }
  synchronized int getCounter() { return _counter; }  // used to be unsynchronized


  synchronized void setIncumbent(BBNodeBase n) {
    if (n.getCost()>_bound) {
      //_incumbent = n;
      _incumbent = new HashSet(n.getNodes());
      _bound = n.getCost();
      System.err.println("new soln found w/ val=" + _bound);
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

