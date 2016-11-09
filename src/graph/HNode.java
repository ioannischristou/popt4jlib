package graph;

import java.util.*;

/**
 * the analogue of the Node class for hyper-graphs. Not thread-safe.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class HNode {
  private int _id;
  private boolean _dirtyflag = false;
  private Set _links;  // Set<Integer linkid>
  private Set _nbors;  // Set<Node nbor>
  private HashMap _weights;  // map<String wname, Double wvalue>


  HNode(int id) {
    _id=id;
    _nbors = null;
    _links = new HashSet();
    _weights = new HashMap();
  }


  public int getId() { return _id; }


  void addHLink(Integer linkid) {
    _links.add(linkid);
    _dirtyflag = true;
  }


  public void setWeight(String name, Double val) {
    _weights.put(name, val);
  }


  public Double getWeightValue(String name) {
    return (Double) _weights.get(name);
  }


  /**
   * return the neighbors of this HNode in the HGraph g.
   * @param g HGraph
   * @return Set // Set&lt;HNode node&gt;
   */
  public Set getNbors(HGraph g) {
    if (_nbors==null || _dirtyflag) {
      // recompute nbors
      _nbors = new HashSet();
      Iterator liter = _links.iterator();
      while (liter.hasNext()) {
        Integer lid = (Integer) liter.next();
        Set nodes = g.getHLink(lid.intValue()).getHNodes(g);
        _nbors.addAll(nodes);
      }
      _nbors.remove(this);  // remove self
      _dirtyflag = false;
    }
    return _nbors;
  }


  public Set getNborIndices(HGraph g, double val) {
    Set indices = new TreeSet();
    Iterator it = _links.iterator();
    while (it.hasNext()) {
      Integer lid = (Integer) it.next();
      HLink l = g.getHLink(lid.intValue());
      if (l.getWeight()>=val) indices.addAll(l.getHNodes(g));
    }
    indices.remove(new Integer(_id));  // remove self
    return indices;
  }


  public Set getHLinkIds() { return _links; }


  public double getArcWeights(HGraph g) {
    double res = 0.0;
    Iterator itin = _links.iterator();
    while (itin.hasNext()) {
      Integer lid = (Integer) itin.next();
      HLink l = g.getHLink(lid.intValue());
      res += l.getWeight();
    }
    return res;
  }

}
