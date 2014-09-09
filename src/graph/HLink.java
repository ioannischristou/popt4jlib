package graph;

import java.util.*;

public class HLink {
  private int _id;
  private Set _nodeids;  // Set<Integer nodeid>
  private double _weight;


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


  public int getId() { return _id; }


  public double getWeight() { return _weight; }


  public void setWeight(double w) { _weight = w; }


  public int getNumNodes() { return _nodeids.size(); }


  public Iterator getHNodeIds() { return _nodeids.iterator(); }

  public Set getHNodeIdsAsSet() { return _nodeids; }

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

