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
class GRASPPackerIntSetNbrhoodFilter3 implements IntSetNeighborhoodFilterIntf {
	private int _k;  // indicate problem type: 1- or 2-packing
	private Graph _g;  // cache to the graph
	
	
	/**
	 * no-arg constructor is for a 2-packing problem only.
	 */
  GRASPPackerIntSetNbrhoodFilter3() {
		_k = 2;
  }


	/**
	 * constructor, with argument indicating if it's a 1- or a 2-packing
	 * problem.
	 * @param k int must be 1 or 2
	 */
  GRASPPackerIntSetNbrhoodFilter3(int k) {
		_k = k;
  }
	
	
	/**
	 * constructor accepts as argument the related graph for the filtering of 
	 * nodes: the <CODE>filter()</CODE> methods do not need to get the graph via
	 * the hash-table parameters, as a reference is stored in a cache data member.
	 * @param k int type of the problem must be 1 or 2
	 * @param g Graph the related graph
	 */
	GRASPPackerIntSetNbrhoodFilter3(int k, Graph g) {
		this(k);
		_g = g;
  }


  /**
   * given one or two integers to be removed from a set, what integers may be
   * added to that set? the neighbors (at distance _k) of the one or two ints
	 * who represent node-ids, excluding the nodes in arg.
   * @param x Object // IntSet (with expected cardinality 1 or 2)
   * @param arg Object // Set&lt;Integer&gt;
   * @param params Hashtable must contain a pair &lt;"dls.graph",Graph g&gt; unless
	 * this object was constructed with the 2-arg constructor
   * @throws LocalSearchException if the params are incorrectly set
   * @return List // ArrayList&lt;Integer&gt; the integers that may be tried
   */
  public List filter(Object x, Object arg, Hashtable params) throws LocalSearchException {
    try {
      //System.err.print("running filter(IntSet rmids, Set sol, params) ");
      IntSet rmids = (IntSet) x;
      Set s = (Set) arg;
      if (_g==null) _g = (Graph) params.get("dls.graph");
      /* slow
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
			*/
			// /* faster
			Set nbors = new HashSet();  // Set<Node>
      Iterator it = rmids.iterator();
      while (it.hasNext()) {
        Integer rmid = (Integer) it.next();
        Node rni = _g.getNodeUnsynchronized(rmid.intValue());
				Set toadd = _k==2 ? rni.getNNborsUnsynchronized() :
								            rni.getNborsUnsynchronized();
				nbors.addAll(toadd);
			}
			ArrayList res = new ArrayList(nbors.size());
			Iterator nborsit = nbors.iterator();
			while (nborsit.hasNext()) {
				Node n = (Node) nborsit.next();
				Integer nidI = new Integer(n.getId());
				if (s.contains(nidI)) continue;
				// where to insert n? in case of _k==1, insert at beginning if 
				// n is adjacent to both nodes in rmids
				if (_k==1 && isAdjacent2Both(n,rmids)) res.add(0, nidI);
				else res.add(nidI);
			}
			// */
      //System.err.println("...produced "+res.size()+" tryids.");
      return res;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new LocalSearchException("filter failed.");
    }
  }


  /**
   * provide a List&lt;IntSet&gt; of sets of two integers to be removed together 
	 * from the solution to create a new one; in each such set, one of 
	 * the integers is x, and the other is an integer y in arg which represents
	 * the id of a node ny whose immediate neighbors intersect the set of 
	 * neighbors of x at distance 1 for 1-packing problems, or distance 3 for 
	 * 2-packing problems. Essentially, returns a list of sets of pairs 
	 * of nodes &lt;x,y&gt; for each y in arg, that happens to be at distance &lte;  
	 * 2*_k from x.
   * @param x // Integer
   * @param arg Set // IntSet values in arg &lte; to the value of x are ignored
   * @param params Hashtable must contain a key-value pair &lt;"dls.graph", Graph g&gt;
	 * unless this object was constructed with the 2-arg constructor
   * @throws LocalSearchException if any params are incorrectly set
   * @return List // ArrayList&lt;IntSet&gt;
   */
  public List filter(Integer x, Set arg, Hashtable params) throws LocalSearchException {
    try {
      //System.err.print("running filter(Integer x, IntSet sol, params). Adding ");
      if (_g==null) _g = (Graph) params.get("dls.graph");
      Node nx = _g.getNodeUnsynchronized(x.intValue());
      // Set nxnnborids = nx.getNNborIndicesUnsynchronized();
			Set nxnnbors = _k==2 ? nx.getNNborsUnsynchronized() : nx.getNborsUnsynchronized();
      List result = new ArrayList();
      Iterator it = arg.iterator();
      while (it.hasNext()) {
        Integer nyid = (Integer) it.next();
        if (nyid.intValue()<=x.intValue()) continue;
        Node ny = _g.getNodeUnsynchronized(nyid.intValue());
        //boolean ok = g.getShortestPath(nx, ny)<=3;
        // Set nynborids = ny.getNNborIndicesUnsynchronized();
        // nynborids.retainAll(nxnnborids);
        // boolean ok = nynborids.size()>0;
				Set nynnbors = _k == 2 ? ny.getNNborsUnsynchronized() :
								                 ny.getNborsUnsynchronized();
				boolean ok = intersect(nxnnbors, nynnbors);
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
	 * return +infinity.
	 * @return <CODE>Integer.MAX_VALUE</CODE>
	 */
  public int getMaxCardinality4Search() {
    return Integer.MAX_VALUE;
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
	
	
	private boolean isAdjacent2Both(Node n, Set rmids) {
		Iterator it = rmids.iterator();
		while (it.hasNext()) {
			Integer rmidI = (Integer) it.next();
			Node rni = _g.getNodeUnsynchronized(rmidI.intValue());
			if (!rni.getNborsUnsynchronized().contains(n)) return false;
		}
		return true;
	}
}

