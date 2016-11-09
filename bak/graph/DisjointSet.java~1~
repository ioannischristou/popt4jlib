package coarsening;

import java.util.*;

/**
 * Union-Find Set Data Structure.
 * Adapted from Ullman, Hopckroft, and later Tarjan's data structures.
 * Implements the disjoint-set forests data structure and algorithms.
 * For references, see Wikipedia article on Disjoint-set data structure
 * <p>Title: Coarsen-Down/Cluster-Up</p>
 * <p>Description: Hyper-Media Clustering System</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: AIT</p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DisjointSet {
  Map _objects;  // map<Object o, DisjointSetElem x>

  public DisjointSet() {
    _objects = new Hashtable();
  }


  public DisjointSetElem makeSet(Object o) {
    DisjointSetElem x = new DisjointSetElem(o);
    _objects.put(o, x);
    return makeSetI(x);
  }


  public DisjointSetElem find(Object o) {
    DisjointSetElem x = (DisjointSetElem) _objects.get(o);
    if (x==null) return null;
    return findI(x);
  }


  public void union(DisjointSetElem x, DisjointSetElem y) {
    DisjointSetElem xroot = findI(x);
    DisjointSetElem yroot = findI(y);
    if (xroot.getRank()>yroot.getRank()) yroot.setParent(xroot);
    else if (xroot.getRank()<yroot.getRank()) xroot.setParent(yroot);
    else if (xroot != yroot) {
      yroot.setParent(xroot);
      xroot.setRank(xroot.getRank()+1);
    }
  }


  DisjointSetElem makeSetI(DisjointSetElem x) {
    x.setParent(x);
    x.setRank(0);
    return x;
  }


  DisjointSetElem findI(DisjointSetElem x) {
    if (x.getParent()==x) return x;
    // return find(x.getParent());
    // x.parent = find(x.parent); return x.parent;
    x.setParent(findI(x.getParent()));
    return x.getParent();
  }

}


class DisjointSetElem {
  private DisjointSetElem _parent;
  private int _rank;
  private Object _elem;

  public DisjointSetElem(Object elem) {
    _parent = null;
    _elem = elem;
    _rank = 0;
  }


  public DisjointSetElem(Object elem, DisjointSetElem parent) {
    _elem = elem;
    _parent = parent;
  }

  public DisjointSetElem getParent() { return _parent; }
  public int getRank() { return _rank; }

  public void setParent(DisjointSetElem x) { _parent = x; }

  public void setRank(int n) { _rank = n; }
}
