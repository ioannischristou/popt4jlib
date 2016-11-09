package graph;

import java.util.*;

/**
 * HGraph class represents hyper-graphs where each edge ("net" to be precise), 
 * connects two or more nodes together. The class is NOT thread-safe and none
 * of its methods is properly synchronized for multi-threaded use.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0 
 */
public class HGraph {
  private HNode[] _nodes;
  private HLink[] _arcs;
  private Object[] _nodeLabels;  // if not null, it contains label for each
                                 // node, which may possibly be the node's true
                                 // id in a larger graph, of which this node
                                 // is a sub-graph, and can be used to get the
                                 // support of this graph in other graphs.
  private HashMap _labelMap;  // map<Object label, Integer nodeid>

  private int _addLinkPos = 0;  // pointer to the last arc set by addLink()
  private int _comps[]=null;  // the array describes the maximal connected
                              // components of this Graph.
                              // _comps.length = _nodes.length
  private Vector _compCards;  // holds each component's cardinality
  private int _compindex=-1;  // holds number of max. connected components of
                              // Graph


	/**
	 * public constructor specifying hyper-graph dimensions. Node labels are 
	 * initialized to null. Nets have to be added via repeated calls to 
	 * <CODE>addHLink(nodeids, weight);</CODE>.
	 * @param numnodes int number of nodes in the hyper-graph.
	 * @param numarcs int number of nets the hyper-graph has.
	 */
  public HGraph(int numnodes, int numarcs) {
    _nodes = new HNode[numnodes];
    _arcs = new HLink[numarcs];
    _nodeLabels = null;
    _labelMap = null;

    for (int i=0; i<numnodes; i++) _nodes[i] = new HNode(i);

    for (int i=0; i<numarcs; i++) _arcs[i] = null;
  }


	/**
	 * public constructor specifying hyper-graph dimensions as well as node labels.
	 * Nets have to be added via repeated calls to 
	 * <CODE>addHLink(nodeids, weight);</CODE>.
	 * @param numnodes int number of nodes.
	 * @param numarcs int number of nets.
	 * @param labels Object[] an array of labels for each of the nodes of the 
	 * hyper-graph. labels[i] is the label of node with id i.
	 */
  public HGraph(int numnodes, int numarcs, Object[] labels) {
    this(numnodes, numarcs);
    _nodeLabels = labels;
    _labelMap = new HashMap();
    for (int i=0; i<numnodes; i++)
      _labelMap.put(_nodeLabels[i], new Integer(i));
  }


	/**
	 * adds a net to this hyper-graph.
	 * @param nodeids Set // Set&lt;Integer&gt; the set of node-ids this net covers.
	 * @param weight double weight of the net.
	 * @throws GraphException if this hyper-graph already has the specified 
	 * number of nets added to it.
	 */
  public void addHLink(Set nodeids, double weight) throws GraphException {
    if (_addLinkPos>=_arcs.length) throw new GraphException("cannot add more arcs.");
    _arcs[_addLinkPos] = new HLink(this, _addLinkPos++, nodeids, weight);
  }


	/**
	 * get a reference to the i-th net.
	 * @param id int net-id (in [0,num_nets-1]).
	 * @return HLink the i-th net.
	 */
  public HLink getHLink(int id) {
    return _arcs[id];
  }


	/**
	 * get a reference to the i-th node.
	 * @param id int node-id (in [0,num_nodes-1]).
	 * @return HNode the i-th node.
	 */
  public HNode getHNode(int id) {
    return _nodes[id];
  }


	/**
	 * returns the number of nodes this HGraph has.
	 * @return int
	 */
  public int getNumNodes() { return _nodes.length; }


	/**
	 * get the total weight of all nodes in this HGraph, specified as the value
	 * of the "cardinality" weight property of each of the nodes.
	 * @return double
	 */
  public double getTotalNodeWeight() {
    double sum = 0.0;
    for (int i=0; i<_nodes.length; i++)
      sum += _nodes[i].getWeightValue("cardinality").doubleValue();
    return sum;
  }


	/**
	 * return the number of nets this HGraph has.
	 * @return int
	 */
  public int getNumArcs() { return _arcs.length; }


	/**
	 * get the label of the i-th node.
	 * @param i int (in [0,num_nodes-1]).
	 * @return Object the label of the i-th node.
	 */
  public Object getHNodeLabel(int i) {
    if (_nodeLabels==null) return null;
    return _nodeLabels[i];
  }


	/**
	 * get the id (in [0,num_nodes-1]) of the node whose label is the given 
	 * argument.
	 * @param label Object
	 * @return int node-id in [0,num_nodes-1].
	 * @throws GraphException if no node has the requested label.
	 */
  public int getHNodeIdByLabel(Object label) throws GraphException {
    Integer nid = (Integer) _labelMap.get(label);
    if (nid==null) throw new GraphException("no such label");
    return nid.intValue();
  }


	/**
	 * set the label of the i-th node. Will replace any existing label for that 
	 * node.
	 * @param i int the id of the node (in [0,num_nodes-1]).
	 * @param o  Object the label for the i-th node.
	 */
  public void setHNodeLabel(int i, Object o) {
    if (_nodeLabels==null) {
      _nodeLabels = new Object[_nodes.length];
      _labelMap = new HashMap();
    }
    if (_nodeLabels[i]!=null) _labelMap.remove(_nodeLabels[i]);
    _nodeLabels[i] = o;
    _labelMap.put(o, new Integer(i));
  }


	/**
	 * gets the number of components for this HGraph, computing them if they are
	 * not yet computed.
	 * @return int
	 */
  public int getNumComponents() {
    if (_compindex==-1) getComponents();
    return _compindex;
  }


  /**
   * return all maximal connected components of this Graph.
   * The array that is returned is of length _nodes.length
   * and the i-th element has a value from [0...#Components-1]
   * indicating to which component the node belongs.
   * @return int[]
   */
  public int[] getComponents() {
    if (_comps!=null) return _comps;
    // use dipth-first strategy
    _comps = new int[_nodes.length];
    _compCards = new Vector();
    final int numnodes= _nodes.length;
    for (int i=0; i<numnodes; i++) _comps[i] = -1;  // unassigned
    _compindex=0;
    for (int i=0; i<numnodes; i++) {
      Set nbors = _nodes[i].getNbors(this);
      if (_comps[i]==-1) {
        // new component
        int numcomps = labelComponent(i, _compindex);
        _compCards.addElement(new Integer(numcomps));
        _compindex++;
      }
    }
    return _comps;
  }


  /**
   * return the number of nodes in max connected component i, where 
   * i is in the range [0, #comps-1].
   * @param i int
   * @return int
   */
  public int getComponentCard(int i) {
    if (_comps==null) getComponents();
    Integer card = (Integer) _compCards.elementAt(i);
    return card.intValue();
  }


	/**
	 * return a String representation of this HGraph by specifying its number of 
	 * nodes and nets, and then, the node ids for each of its nets.
	 * @return String
	 */
  public String toString() {
    String ret = "#nodes="+_nodes.length+" #arcs="+_arcs.length;
    for (int i=0; i<_arcs.length; i++)
      ret += " ["+_arcs[i]+" (w="+_arcs[i].getWeight()+")]";
    return ret;
  }


  /**
   * helper method used in <CODE>getComponents()</CODE>.
   * @param nid int is a Node id
   * @param c int is the component which this node and all nodes reachable by it
   * belong to.
   * @return int number of nodes labeled
   */
  private int labelComponent(int nid, int c) {
    int res=1;
    _comps[nid] = c;
    Set nbors = new HashSet(_nodes[nid].getNbors(this));
    while (nbors.size()>0) {
      Iterator it = nbors.iterator();
      HNode n = (HNode) it.next();
      nbors.remove(n);
      _comps[n.getId()] = c;  // label node
      res++;
      // add non-labeled nbors to nbors
      Set nnbors = n.getNbors(this);
      it = nnbors.iterator();
      while (it.hasNext()) {
        HNode nn = (HNode) it.next();
        if (_comps[nn.getId()]==-1) nbors.add(nn);
      }
    }
    return res;
  }

}

