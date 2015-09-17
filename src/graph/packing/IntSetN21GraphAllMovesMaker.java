package graph.packing;

import popt4jlib.LocalSearch.*;
import graph.*;
import utils.*;
import parallel.*;
import java.util.*;

/**
 * class is an implementation of the <CODE>AllChromosomeMakerIntf</CODE> 
 * interface, and implements local search in the N_{2+1} neighborhood of ints:
 * read the documentation of its super-classes for a detailed explanation. Works
 * with the 1- and 2-packing problems.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntSetN21GraphAllMovesMaker extends IntSetN21AllMovesMaker {
	private int _k=2;
	
	/** 
	 * no-arg constructor is for k=2.
	 */
  public IntSetN21GraphAllMovesMaker() {
  }
	
	
	/**
	 * 
	 * @param k int specifies the type of packing problem: 1- or 2-packing. 
	 */
	public IntSetN21GraphAllMovesMaker(int k) {
		_k = k;
	}

	
  /**
   * hook method in the context of the Template Method Design Pattern.
   * Overrides this method to modify the behavior of the base move-maker.
   * @param tid Integer
   * @param x IntSet
   * @param params HashMap must contain the pair &lt;"dls.graph", Graph g&gt;,
	 * and may optionally contain the pair &lt;"dls.lock_graph", boolean_val&gt;
	 * which if present and false, will avoid locking graph elements.
   * @return boolean
   */
  protected boolean isOK2Add(Integer tid, IntSet x, HashMap params) {
    try {
      Graph g = (Graph) params.get("dls.graph");
			boolean lock_graph = params.containsKey("dls.lock_graph") ?
							               ((Boolean) params.get("dls.lock_graph")).booleanValue() :
							               true;
      Node n = g.getNode(tid.intValue());
      Set nodes = new TreeSet();
      Iterator xiter = x.iterator();
      while (xiter.hasNext()) {
				Node ni = lock_graph ? g.getNode(((Integer) xiter.next()).intValue()) :
								               g.getNodeUnsynchronized(((Integer) xiter.next()).intValue());
        nodes.add(ni);
      }
      return isFree2Cover(n, nodes, lock_graph);
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }


  /**
   * check if node nj can be set to one when the nodes in active are also set.
   * @param nj Node
   * @param active Set  // Set&lt;Node&gt;
	 * @param do_lock boolean
   * @return boolean true iff nj can be added to active
	 * @throws ParallelException
   */
  private boolean isFree2Cover(Node nj, Set active, boolean do_lock) 
	    throws ParallelException {
    if (active==null) return true;
    if (active.contains(nj)) return false;
    Set activated = new HashSet(active);
    Iterator it = active.iterator();
    while (it.hasNext()) {
      Node ni = (Node) it.next();
      Set nnbors = _k==2 ? 
							       (do_lock ? ni.getNNbors() : ni.getNNborsUnsynchronized()) :
							       (do_lock ? ni.getNbors() : ni.getNborsUnsynchronized());
      activated.addAll(nnbors);
    }
    return !activated.contains(nj);
  }

}

