/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package graph.packing;

import java.util.Set;
import java.util.TreeSet;
import parallel.ParallelException;

/**
 * represents a node in the B&amp;B tree of the hybrid B&amp;B - GASP scheme for
 * packing problems. It implements the Comparable interface as
 * <CODE>BBNode[1,2]</CODE> objects enter a priority queue (<CODE>BBQueue</CODE>)
 * from which nodes are selected for processing (passing them to the
 * <CODE>BBThreadPool</CODE> that is a proxy for a
 * <CODE>parallel.ParallelAsynchBatchTaskExecutor</CODE> executor). It also
 * implements the Runnable interface as the executor requires runnable objects
 * to process.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
abstract class BBNodeBase implements Comparable, Runnable {
  private Set _nodes = null;  // TreeSet<Node node> set of active nodes in current soln.
  private int _numChildren;
  final private BBNodeBase _parent;  // node's parent
  final private BBTree _master;  // pointer to book-keeping structure
  private boolean _isDone;  // indicates if node is fathomed
  private int _id;
  protected double _bound=-1.0;  // needs to be protected instead of private
	                               // as it's essentially just a cache

	
  /**
   * Sole constructor of a BBNodeBase object. Clients are not expected to
   * create such objects which are instead created dynamically through the
   * B&amp;B process.
   * @param master BBTree the master BBTree object of which this is a node.
   * @param r Set // Set&lt;Node&gt; the set of (graph) nodes to be added to the 
	 * nodes of the parent to represent a new partial solution.
   * @param parent BBNodeBase the parent BB-node in the B&amp;B tree construction process.
   * @throws PackingException if the second argument is non-null but the third
   * argument is null.
   */
  protected BBNodeBase(BBTree master, Set r, BBNodeBase parent) throws PackingException {
    //if (master==null)
    //  throw new ExactPackingException("null args");
    _master = master;
    _isDone = false;
    _parent = parent;
    _numChildren = 0;
    if (parent==null) {  // i am the root
      if (r!=null) {
        throw new PackingException("root has r!=null");
      }
      _nodes = new TreeSet();
      _id = _master.incrementCounter();
    }
    else {  // i am not the root
      // Node rn = _master.getGraph().getNode(r);
      _nodes = new TreeSet(parent._nodes);
      _nodes.addAll(r);
      _id = _master.incrementCounter();
    }
  }


  /**
   * returns the number of nodes contained in this solution. This number will
   * likely change when this BBNode's run() method is called.
   * @return double
   */
  abstract double getCost();


  /**
   * returns the (unique) id of the node in the BBTree into which it belongs.
   * @return int
   */
  final protected int getId() { return _id; }


  /**
   * returns the parent of this node (or null if this BB-node is the root of the
   * tree).
   * @return BBNodeBase
   */
  final protected BBNodeBase getParent() { return _parent; }


  /**
   * returns the Set of graph nodes contained in the solution represented
   * by this BB-node.
   * @return Set // Set&lt;Node&gt;
   */
  final Set getNodes() { return _nodes; }


  /**
   * return true iff this node is "done" i.e. no further processing on it is
   * required (i.e. it has either being "opened" or has been "fathomed").
   * @return boolean
   */
  final protected boolean isDone() { return _isDone; }  // intentionally left unsynchronized


  /**
   * sets the BB-node's "done" status to true without adding this BB-node to the
   * BBTree's "recent" queue (even if the recent-queue exists). It also notifies
   * the parent BB-node of its status as "done".
   */
  final protected void setDoneNoAdd2Recent() {
    _numChildren = 0;
    _isDone = true;
    if (_parent!=null) _parent.notifyDone();
    if (_nodes==null || _nodes.size()==0) _master.getQueue().setDone();  // notify BBTree to finish
  }


  /**
   * acts exactly as the <CODE>setDoneNoAdd2Recent()</CODE> method, and also
   * adds this BB-node to the "recents" queue of the master BBTree, if such a
   * queue exists (such a queue is not necessary when the "usemaxsubsets" flag
   * is set to true for the BBTree object.)
   */
  final protected void setDone() {
    _numChildren = 0;
    _isDone = true;
    if (_parent!=null) _parent.notifyDone();
    else _master.getQueue().setDone();  // this is the root and must notify BBTree to finish
    // if (_nodes==null || _nodes.size()==0) _master.getQueue().setDone();  // notify BBTree to finish
    _master.getQueue().addInRecent(this);  // add processed node in _recentNodes pool
  }

	
	/**
	 * recursive method, only invoked from the setDone*() methods of this class.
	 * Its purpose is to eventually notify the <CODE>BBQueue</CODE> thread that
	 * the B&amp;B tree construction process has finished, so as to exit its run-
	 * loop.
	 */
  final protected void notifyDone() {
    boolean go_up = false;
    synchronized(this) {
      --_numChildren;
      if (_numChildren == 0) {
        _isDone = true;
        if (_parent != null) go_up = true;
      }
    }
    if (go_up) _parent.notifyDone();
    else if (_parent==null && _numChildren==0) {
      _master.getQueue().setDone(); // notify BBQueue to finish
      // _master.notify(); // notify master to finish
    }
  }

	
	/**
	 * result depends on the type of problem.
	 * @return double a valid upper bound on the current solution
	 * @throws ParallelException 
	 */
	abstract protected double getBound() throws ParallelException;
	
	
	// extra methods needed to be added
	
	
	final protected BBTree getMaster() { return _master; }
	final protected void setNumChildren(int n) { _numChildren = n; }
	
}

