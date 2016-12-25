package graph.packing;

import java.util.*;

/**
 * Holds BBNodeBase objects to execute (to submit to the BBThreadPool). Not part
 * of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class BBQueue extends Thread {
  private TreeSet _nodes;  // TreeSet<BBNodeBase>
  private BoundedQueue _recentNodes;
  private int _maxSz;
  private boolean _isDone;


  /**
   * construct a BBQueue that can hold up to max_nodes BBNode* objects, and that
   * optionally maintains a list of up to recentsSize of BBNode* objects.
   * @param max_nodes int must be greater than zero.
   * @param recentsSize int must be greater than or equal to zero.
   */
  BBQueue(int max_nodes, int recentsSize) {
    _nodes = new TreeSet();
    if (recentsSize>0)
      _recentNodes = new BoundedQueue(recentsSize);
    else _recentNodes = null;
    _isDone = false;
    _maxSz = max_nodes;
  }


  /**
   * the run() method of the BBQueue overrides the run() method of Thread and
   * implements the loop that the thread executes and in which it pops a BBNode*
   * object at a time from queue of _nodes (implemented as a TreeSet) and
   * passes it for execution to the BBThreadPool.
   */
  public void run() {
		/* 2014-07-04: For faster execution, the "container" variable is now 
		 * declared only once, outside the loop, as an ArrayList.
		 */
		ArrayList container = new ArrayList();  // ArrayList<BBNode>
    while (true) {
      if (isDone()) return;  // done
      try {
        BBNodeBase best = popNode();
        if (best == null) {
          System.err.println("BBQueue done, exits.");
          return; // done
        }
        boolean runit = true;
        if (_recentNodes!=null) runit = !(_recentNodes.contains(best));
        container.clear();  // used to be: Vector container = new Vector();
        container.add(best);
        if (runit) BBThreadPool.getPool().executeBatch(container);
        else best.setDone();  // indicate it was done
      }
      catch (Exception e) {
        e.printStackTrace();
        System.exit(-1);  // oops...
      }
    }
  }


	/**
	 * checks if this BBQueue object is done and the thread has already completed
	 * execution or else will end in the next iteration of the loop in its run()
	 * method.
	 * @return boolean true iff done 
	 */
  synchronized boolean isDone() { return _isDone; }


  /**
   * stop the BBQueue thread.
   */
  synchronized void setDone() {
    _isDone=true;
    notify();
  }


	/**
	 * pops and returns the "best" BBNodeBase object in the <CODE>_nodes</CODE>
	 * queue, according to the <CODE>BBNodeComparatorIntf</CODE> object that is
	 * returned by the <CODE>BBTree</CODE> object <CODE>getBBNodeComparator</CODE>
	 * method.
	 * @return BBNodeBase the current best BBNode* in the _nodes queue
	 * @throws InterruptedException
	 * @throws PackingException 
	 */
  synchronized BBNodeBase popNode() throws InterruptedException, PackingException {
    while (_nodes.size()==0) {
      if (_isDone) return null;
      wait();
    }
    BBNodeBase n = (BBNodeBase) _nodes.first();
    boolean rm = _nodes.remove(n);
    if (rm==false)
      throw new PackingException("failed to remove node?");
    return n;
  }


  /**
   * get the total number of BBNode* objects in the queue.
   * @return int
   */
  synchronized int size() {
    return _nodes.size();
  }

	
	/**
	 * return the estimate on the number of currently busy threads in the pool.
	 * Notice that while the call executes, it will block any threads in the pool 
	 * from submitting more work to this queue, as the thread submit their jobs
	 * via the insertNodes() methods of this object (which are all synchronized as
	 * well).
	 * @return int
	 */
	synchronized int getNumBusyThreads() {
		return BBThreadPool.getPool().getNumBusyThreads();
	}
	

  /**
   * insert a BBNode* object in the queue to be later "run".
   * @param n BBNodeBase
   * @return boolean
   */
  synchronized boolean insertNode(BBNodeBase n) {
    if (_nodes.size()<_maxSz) {
      int old_sz = _nodes.size();
      _nodes.add(n);
      if (_nodes.size()==old_sz) {
        // node was not inserted, thus it is done
        n.setDoneNoAdd2Recent();
      }
      notify();
      return true;
    }
    return false;
  }


  /**
   * insert a List of BBNodeBase objects in the queue to be later executed.
   * @param children List // List&lt;BBNodeBase&gt;
   * @return boolean true iff the _nodes queue is not full at the time of the 
	 * call.
   */
  synchronized boolean insertNodes(List children) {
    if (_nodes.size()>=_maxSz)
      return false;  // queue is full. Signal caller
    for (int i=0; i<children.size(); i++) {
      BBNodeBase n = (BBNodeBase) children.get(i);
      int old_sz = _nodes.size();
      _nodes.add(n);
      if (_nodes.size()==old_sz) {
        // node was not inserted, thus it is done
        n.setDoneNoAdd2Recent();
      }
    }
    notify();
    return true;
  }


  /**
   * insert a TreeSet of BBNodeBase objects in the queue to be later executed.
   * @param children TreeSet // TreeSet&lt;BBNodeBase&gt;
   * @return boolean true iff the _nodes queue is not full at the time of the 
	 * call.
   */
  synchronized boolean insertNodes(TreeSet children) {
    if (_nodes.size()>=_maxSz)
      return false;  // queue is full. Signal caller
    Iterator it = children.iterator();
    while (it.hasNext()) {
      BBNodeBase n = (BBNodeBase) it.next();
      int old_sz = _nodes.size();
      _nodes.add(n);
      if (_nodes.size()==old_sz) {
        // node was not inserted, thus it is done
        n.setDoneNoAdd2Recent();
      }
    }
    notify();
    return true;
  }


  /**
   * add a BBNodeBase object in the "recentNodes" queue.
   * @param n BBNodeBase
   * @return boolean
   */
  synchronized boolean addInRecent(BBNodeBase n) {
    if (_recentNodes==null) return true;
    if (_recentNodes.contains(n)) return false;
    else _recentNodes.insert(n);
    return true;
  }


  /**
   * returns true if the argument is currently in the recentsQueue.
   * @param n BBNodeBase
   * @return boolean
   */
  synchronized boolean existsInRecent(BBNodeBase n) {
    if (_recentNodes==null) return false;
    if (_recentNodes.contains(n)) return true;
    return false;
  }
}

