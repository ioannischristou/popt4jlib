package graph;

import java.util.*;

/**
 * represents nets connecting multiple nodes in hyper-graphs. Not thread-safe.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class HLink {
  private int _id;
  private Set _nodeids;  // Set<Integer nodeid>
  private double _weight;

	/**
	 * sole public constructor.
	 * @param g back-reference to the HGraph to which this net belongs.
	 * @param id net-id (in [0,g.num_arcs-1]).
	 * @param nodeids Set // Set&lt;Integer&gt; set of node-ids.
	 * @param weight double the weight of this net.
	 */
  HLink(HGraph g, int id, Set nodeids, double weight) {
    _id = id;
    _weight = weight;
    _nodeids = new TreeSet(nodeids);
    Iterator it = nodeids.iterator();
    Integer lid = new Integer(id);
    while (it.hasNext()) {
      Integer nid = (Integer) it.next();
      HNode n = g.getHNode(nid.intValue());
      n.addHLink(lid);
    }
  }


	/**
	 * return the id of this net (in [0,_g.num_arcs-1]).
	 * @return int
	 */
  public int getId() { return _id; }


	/**
	 * return the weight of this HLink.
	 * @return double
	 */
  public double getWeight() { return _weight; }


	/**
	 * set the weight of this net.
	 * @param w double
	 */
  public void setWeight(double w) { _weight = w; }


	/**
	 * return the number of nodes in this HLink.
	 * @return int
	 */
  public int getNumNodes() { return _nodeids.size(); }


	/**
	 * return an iterator to the set of node ids of this HLink.
	 * @return Iterator
	 */
  public Iterator getHNodeIds() { return _nodeids.iterator(); }

	
	/**
	 * return the set of node-ids of this HLink.
	 * @return Set  // Set&lt;Integer&gt; set of node ids
	 */
  public Set getHNodeIdsAsSet() { return _nodeids; }

	
	/**
	 * get the set of HNode objects this HLink covers.
	 * @param g HGraph the HGraph to which this HLink belongs.
	 * @return Set  // Set&lt;HGraph&gt; the set of HNode objects this HLink covers.
	 */
  public Set getHNodes(HGraph g) {
    Iterator it = _nodeids.iterator();
    Set set = new HashSet();
    while (it.hasNext()) {
      int id = ((Integer) it.next()).intValue();
      set.add(g.getHNode(id));
    }
    return set;
  }

}

