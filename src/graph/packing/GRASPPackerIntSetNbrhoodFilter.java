package graph.packing;

import graph.*;
import popt4jlib.LocalSearch.*;
import java.util.*;

/**
 * implements a filter applicable to local search in GRASP approaches for the 
 * 2-packing problem, as implemented in the 
 * <CODE>popt4jlib.LocalSearch.DLS</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class GRASPPackerIntSetNbrhoodFilter implements IntSetNeighborhoodFilterIntf {

	/**
	 * sole constructor is no-arg, no-op.
	 */
  GRASPPackerIntSetNbrhoodFilter() {
  }


	/**
	 * returns the result of the call 
	 * <CODE>filter((Integer) x, (Set) arg, params)</CODE>.
	 * @param x Object must be Integer
	 * @param arg Object must be Set
	 * @param params must contain a pair &lt;"dls.graph", Graph g&gt;.
	 * @return List // ArrayList&lt;Integer&gt;
	 * @throws LocalSearchException if the params are incorrectly set
	 */
  public List filter(Object x, Object arg, HashMap params) throws LocalSearchException {
    try {
      List res = filter((Integer) x, (Set) arg, params);
      return res;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new LocalSearchException("filter failed.");
    }
  }

	
	/**
	 * returns ids of all neighbors at distance 1 or 2 of the node with id x.
	 * @param x Integer
	 * @param arg Set // Set&lt;Integer&gt; unused
	 * @param params HashMap must contain a pair &lt;"dls.graph", Graph g&gt;
	 * @return List // ArrayList&lt;Integer nodeid&gt;
	 * @throws LocalSearchException if params are incorrectly set
	 */
  public List filter(Integer x, Set arg, HashMap params) throws LocalSearchException {
    try {
      Graph g = (Graph) params.get("dls.graph");
      Node nx = g.getNode(x.intValue());
      List result = new ArrayList();
      Set nbors = nx.getNNborIndices();
      //if (nbors!=null) nbors.removeAll(arg);
      if (nbors!=null && nbors.size()>0) {
        result.addAll(nbors);
      }
      return result;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new LocalSearchException("filter failed...");
    }
  }


	/**
	 * return the number 3.
	 * @return 3
	 */
  public int getMaxCardinality4Search() {
    return 3;
  }
}

