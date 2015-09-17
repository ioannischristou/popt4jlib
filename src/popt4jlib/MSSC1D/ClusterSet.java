package popt4jlib.MSSC1D;

import java.util.*;

public class ClusterSet {
  private Vector _clusters;  // Set<Cluster c>
  private double _val = Double.MAX_VALUE;
  private boolean _isDirty=true;
  private HashMap _startWith;  // map<Integer ind, Set<Cluster> > cache


  public ClusterSet() {
    _clusters = new Vector();
    _isDirty=true;
    initCache();
  }


  public ClusterSet(Set clusters) {
    _clusters = new Vector(clusters);
    _isDirty=true;
    initCache();
  }


  public ClusterSet(ClusterSet c) {
    _clusters = new Vector(c._clusters);
    _val = Double.MAX_VALUE;
    _isDirty=true;
    initCache();
  }


  public ClusterSet(Integer i) throws CException {
    _clusters = new Vector();
    Cluster s = new Cluster();
    s.add(i);
    _clusters.addElement(s);
    _val = Double.MAX_VALUE;
    _isDirty=true;
    initCache();
  }


  public ClusterSet(int i, int j) throws CException {
    _clusters = new Vector();
    Cluster s = new Cluster();
    for (int k=i; k<=j; k++) {
      Integer ki = new Integer(k);
      s.add(ki);
    }
    _clusters.addElement(s);
    _val = Double.MAX_VALUE;
    _isDirty=true;
    initCache();
  }


  public int size() { return _clusters.size(); }


  public void addClusterNoCheck(Cluster c) {
    _clusters.add(c);
    _isDirty = true;
    updateCache(c);
  }


  public void addCluster(Cluster c) throws CException {
    // does c start after last one in clusters?
    if (getLastIndex()+1 != c.getMin())
      throw new CException("wrong ordering in ClusterSet");
    _clusters.add(c);
    _isDirty=true;
    updateCache(c);
  }


  public void addClustersRight(ClusterSet cs) throws CException {
    Vector clusters = cs._clusters;
    for (int i=0; i<clusters.size(); i++) {
      Cluster ci = (Cluster) clusters.elementAt(i);
      addCluster(ci);
    }
  }


  public Set getSetsStartingWithFast(int m) {
    return (Set) _startWith.get(new Integer(m));
  }


  public Set getSetsStartingWith(int m) {
    // return Set<Cluster>
    Iterator it = _clusters.iterator();
    HashSet res = new HashSet();
    while (it.hasNext()) {
      Cluster ci = (Cluster) it.next();
      if (ci.getMin()==m) res.add(ci);
    }
    return res;
  }


  public Cluster getCluster(int startind, int endind) throws CException {
    Cluster c = null;
    Set startingsets = getSetsStartingWith(startind);
    Iterator it = startingsets.iterator();
    while (it.hasNext()) {
      c = (Cluster) it.next();
      if (c.getMax()==endind) return c;
    }
    throw new CException("no such cluster in clusterset");
  }


  public int getLastIndex() {
    Iterator it = _clusters.iterator();
    int res = -1;
    while (it.hasNext()) {
      Cluster c = (Cluster) it.next();
      if (c.getMax()>res) res = c.getMax();
    }
    return res;
  }


  public double evaluate(Params p) { return evaluate(p, false); }


  public double evaluate(Params p, boolean allow_partial) {
    if (_isDirty==false) return _val;
    // no partial evaluations allowed
    if (allow_partial==false && getLastIndex()!=p.getSequenceLength()-1) {
      _val = Double.MAX_VALUE;
      _isDirty = false;
      return _val;
    }
    // compute value
    double val = 0.0;
    Iterator it = _clusters.iterator();
    while (it.hasNext()) {
      Cluster c = (Cluster) it.next();
      if (c.isFeasible(p))
        val += c.evaluate(p);
      else {
        _val = Double.MAX_VALUE;
        _isDirty = false;
        return _val;
      }
    }
    _val = val;
    _isDirty = false;
    return _val;
  }


  public String toString(Params p) {
    String ret = "[";
    for (int i=0; i<_clusters.size(); i++) {
      if (i>0) {
        ret+=",";
      }
      ret+="(";
      Cluster c = (Cluster) _clusters.elementAt(i);
      Iterator it2 = c.iterator();
      boolean f=true;
      while (it2.hasNext()) {
        if (!f) {ret+=",";}
        else f=false;
        Integer iv = (Integer) it2.next();
        ret+=p.getSequenceValueAt(iv.intValue());
      }
      ret+=")";
    }
    ret+="]";
    return ret;
  }


  Vector getClusters() { return _clusters; }


  private void initCache() {
    _startWith = new HashMap();
    Iterator it = _clusters.iterator();
    while (it.hasNext()) {
      Cluster c = (Cluster) it.next();
      Integer m = new Integer(c.getMin());
      Set s = (Set) _startWith.get(m);
      if (s==null) {
        s = new HashSet();
        s.add(c);
        _startWith.put(m, s);
      }
      else s.add(c);
    }
  }


  private void updateCache(Cluster c) {
    Integer m = new Integer(c.getMin());
    Set s = (Set) _startWith.get(m);
    if (s==null) {
      s = new HashSet();
      s.add(c);
      _startWith.put(m, s);
    }
    else s.add(c);
  }
}

