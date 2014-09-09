package graph;

import java.util.*;

public class HLinkPair {
  private Set _nodeids;
  private double _w;

  public HLinkPair(Set nids, double w) {
    _nodeids = new HashSet(nids);
    _w = w;
  }


  public boolean equals(Object o) {
    if (o==null) return false;
    try {
      HLinkPair l = (HLinkPair) o;
      return (_nodeids.equals(l._nodeids));
    }
    catch (ClassCastException e) {
      return false;
    }
  }


  public int hashCode() {
    if (_nodeids==null) return (int) Math.floor(_w);
    else return _nodeids.size()+(int) Math.floor(_w);
  }


  public Set getNodeIds() { return _nodeids; }
  public double getWeight() { return _w; }


  public double addWeight(double w) {
    _w += w;
    return _w;
  }
}

