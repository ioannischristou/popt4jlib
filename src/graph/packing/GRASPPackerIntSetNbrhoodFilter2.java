package graph.packing;

import graph.*;
import popt4jlib.LocalSearch.*;
import utils.*;
import java.util.*;

/**
 * implements a filter applicable to local search in GRASP approaches for the 
 * 1- or 2-packing problem, as implemented in the 
 * <CODE>popt4jlib.LocalSearch.DLS</CODE> class. Does not obtain read-lock of
 * the graph to which the nodes belong when accessing nodes' neighbors.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class GRASPPackerIntSetNbrhoodFilter2 implements IntSetNeighborhoodFilterIntf {
	private int _k;  // indicate problem type: 1- or 2-packing
	private Graph _g;  // cache to the graph
	
	
	/**
	 * no-arg constructor is for a 2-packing problem only.
	 */
  GRASPPackerIntSetNbrhoodFilter2() {
		_k = 2;
  }


	/**
	 * constructor, with argument indicating if it's a 1- or a 2-packing
	 * problem.
	 * @param k int must be 1 or 2
	 */
  GRASPPackerIntSetNbrhoodFilter2(int k) {
		_k = k;
  }
	
	
	/**
	 * constructor accepts as argument the related graph for the filtering of 
	 * nodes: the <CODE>filter()</CODE> methods do not need to get the graph via
	 * the hash-table parameters, as a reference is stored in a cache data member.
	 * @param k int type of the problem must be 1 or 2
	 * @param g Graph the related graph
	 */
	GRASPPackerIntSetNbrhoodFilter2(int k, Graph g) {
		this(k);
		_g = g;
  }


  /**
   * given two integers to be removed from a set, what integers may be
   * added to that set? the neighbors (at distance 1 or 2) of the two integers
	 * who represent node-ids, excluding the nodes in arg.
   * @param x Object // IntSet (with expected cardinality 2)
   * @param arg Object // Set&lt;Integer&gt;
   * @param params HashMap must contain a pair &lt;"dls.graph",Graph g&gt; unless
	 * this object was constructed with the 2-arg constructor
   * @throws LocalSearchException if the params are incorrectly set
   * @return List // ArrayList&lt;Integer&gt; the integers that may be tried
   */
  public List filter(Object x, Object arg, HashMap params) throws LocalSearchException {
    try {
      //System.err.print("running filter(IntSet rmids, Set sol, params) ");
      IntSet rmids = (IntSet) x;
      Set s = (Set) arg;
      if (_g==null) _g = (Graph) params.get("dls.graph");
      Set nbors = new IntSet();
      Iterator it = rmids.iterator();
      while (it.hasNext()) {
        Integer rmid = (Integer) it.next();
        Node rni = _g.getNodeUnsynchronized(rmid.intValue());
				Set toadd = _k==2 ? rni.getNNborIndicesUnsynchronized() : 
								            rni.getNborIndicesUnsynchronized(Double.NEGATIVE_INFINITY);
        nbors.addAll(toadd);
      }
      nbors.removeAll(s);  // is it necessary?
      List res = new ArrayList(nbors);
      //System.err.println("...produced "+res.size()+" tryids.");
      return res;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new LocalSearchException("filter failed.");
    }
  }


  /**
   * provide a List&lt;IntSet&gt; of sets of two integers; in each such set, one of 
	 * the integers is x, and the other is an integer y in arg which represents
	 * the id of a node ny whose immediate neighbors intersect the set of 
	 * neighbors of x at distance 1 or 2 as specified in the constructor of this
	 * object. Essentially, returns a list of sets of pairs of nodes &lt;x,y&gt; 
	 * for each y in arg, that happens to be at distance 2 or 3 from x, depending
	 * on the value of the k-packing problem that is being solved.
   * @param x // Integer
   * @param arg Set // IntSet values in arg &lte; to the value of x are ignored
   * @param params HashMap must contain a key-value pair &lt;"dls.graph", Graph g&gt;
	 * unless this object was constructed with the 2-arg constructor
   * @throws LocalSearchException if any params are incorrectly set
   * @return List // ArrayList&lt;IntSet&gt;
   */
  public List filter(Integer x, Set arg, HashMap params) throws LocalSearchException {
    try {
      //System.err.print("running filter(Integer x, IntSet sol, params). Adding ");
      if (_g==null) _g = (Graph) params.get("dls.graph");
      Node nx = _g.getNodeUnsynchronized(x.intValue());
      Set nxnnbors = _k==2 ? nx.getNNborsUnsynchronized() : 
							                 nx.getNborsUnsynchronized();
      List result = new ArrayList();
      Iterator it = arg.iterator();
      while (it.hasNext()) {
        Integer nyid = (Integer) it.next();
        if (nyid.intValue()<=x.intValue()) continue;
        Node ny = _g.getNodeUnsynchronized(nyid.intValue());
        //boolean ok = g.getShortestPath(nx, ny)<=3;
        Set nynbors = ny.getNborsUnsynchronized();
        boolean ok = intersect(nxnnbors, nynbors);
        if (ok) {
          IntSet cand = new IntSet();
          cand.add(x);
          cand.add(nyid);
          result.add(cand);
          //System.err.print("("+x+","+nyid+")...");
        }
      }
      //System.err.println("filter(Integer x, IntSet sol, params) done");
      return result;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new LocalSearchException("filter failed...");
    }
  }


	/**
	 * return the int number 5.
	 * @return 5 
	 */
  public int getMaxCardinality4Search() {
    return 5;
  }

	
	/**
	 * check if the two sets have a non-empty intersection
	 * @param x Set // Set&lt;Node&gt;
	 * @param y Set // Set&lt;Node&gt;
	 * @return true iff the intersection of x and y is not empty.
	 */
	private static boolean intersect(Set x, Set y) {
		Iterator xit = x.iterator();
		while (xit.hasNext()) {
			Node nx = (Node) xit.next();
			if (y.contains(nx)) return true;
		}
		return false;
	}

}

