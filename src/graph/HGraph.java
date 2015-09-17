package graph;

import java.util.*;

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


  public HGraph(int numnodes, int numarcs) {
    _nodes = new HNode[numnodes];
    _arcs = new HLink[numarcs];
    _nodeLabels = null;
    _labelMap = null;

    for (int i=0; i<numnodes; i++) _nodes[i] = new HNode(i);

    for (int i=0; i<numarcs; i++) _arcs[i] = null;
  }


  public HGraph(int numnodes, int numarcs, Object[] labels) {
    this(numnodes, numarcs);
    _nodeLabels = labels;
    _labelMap = new HashMap();
    for (int i=0; i<numnodes; i++)
      _labelMap.put(_nodeLabels[i], new Integer(i));
  }


  public void addHLink(Set nodeids, double weight) throws GraphException {
    if (_addLinkPos>=_arcs.length) throw new GraphException("cannot add more arcs.");
    _arcs[_addLinkPos] = new HLink(this, _addLinkPos++, nodeids, weight);
  }


  public HLink getHLink(int id) {
    return _arcs[id];
  }


  public HNode getHNode(int id) {
    return _nodes[id];
  }


  public int getNumNodes() { return _nodes.length; }


  public double getTotalNodeWeight() {
    double sum = 0.0;
    for (int i=0; i<_nodes.length; i++)
      sum += _nodes[i].getWeightValue("cardinality").doubleValue();
    return sum;
  }


  public int getNumArcs() { return _arcs.length; }


  public Object getHNodeLabel(int i) {
    if (_nodeLabels==null) return null;
    return _nodeLabels[i];
  }


  public int getHNodeIdByLabel(Object label) throws GraphException {
    Integer nid = (Integer) _labelMap.get(label);
    if (nid==null) throw new GraphException("no such label");
    return nid.intValue();
  }


  public void setHNodeLabel(int i, Object o) {
    if (_nodeLabels==null) {
      _nodeLabels = new Object[_nodes.length];
      _labelMap = new HashMap();
    }
    if (_nodeLabels[i]!=null) _labelMap.remove(_nodeLabels[i]);
    _nodeLabels[i] = o;
    _labelMap.put(o, new Integer(i));
  }


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
   * return the number of nodes in max. connected component i
   * i is in the range [0...#comps-1]
   * @param i int
   * @return int
   */
  public int getComponentCard(int i) {
    if (_comps==null) getComponents();
    Integer card = (Integer) _compCards.elementAt(i);
    return card.intValue();
  }


  public String toString() {
    String ret = "#nodes="+_nodes.length+" #arcs="+_arcs.length;
    for (int i=0; i<_arcs.length; i++)
      ret += " ["+_arcs[i]+" (w="+_arcs[i].getWeight()+")]";
    return ret;
  }


  /**
   * nid is a Node id
   * c is the component which this node and all nodes reachable by it
   * belong to.
   * @param nid int
   * @param c int
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

