package popt4jlib.MSSC1D;

import java.util.*;

/**
 * class represents a solution to an MSSC1D problem, by holding a list of the
 * resulting Cluster object-indices. Not a thread-safe class as it is not 
 * intended for use from multiple-threads.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ClusterSet {
  private Vector _clusters;  // Set<Cluster c>
  private double _val = Double.MAX_VALUE;
  private boolean _isDirty=true;
  private HashMap _startWith;  // map<Integer ind, Set<Cluster> > cache


	/**
	 * public no-arg constructor.
	 */
  public ClusterSet() {
    _clusters = new Vector();
    _isDirty=true;
    initCache();
  }


	/**
	 * public 1-arg constructor.
	 * @param clusters Set  // Set&lt;Cluster&gt;
	 */
  public ClusterSet(Set clusters) {
    _clusters = new Vector(clusters);
    _isDirty=true;
    initCache();
  }


	/**
	 * public copy-constructor.
	 * @param c ClusterSet 
	 */
  public ClusterSet(ClusterSet c) {
    _clusters = new Vector(c._clusters);
    _val = Double.MAX_VALUE;
    _isDirty=true;
    initCache();
  }


	/**
	 * public 1-argument constructor. Creates a cluster-set with a single cluster
	 * and adds the argument in this single cluster.
	 * @param i Integer
	 * @throws CException 
	 */
  public ClusterSet(Integer i) throws CException {
    _clusters = new Vector();
    Cluster s = new Cluster();
    s.add(i);
    _clusters.addElement(s);
    _val = Double.MAX_VALUE;
    _isDirty=true;
    initCache();
  }


	/**
	 * public 2-argument constructor. Creates a cluster-set with a single cluster
	 * and adds the numbers in the range [i,j] in this single cluster.
	 * @param i int
	 * @param j int
	 * @throws CException 
	 */
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


	/**
	 * return the number of clusters in this ClusterSet object.
	 * @return int
	 */
  public int size() { return _clusters.size(); }


	/**
	 * add the Cluster given in the input argument as an extra cluster in this 
	 * ClusterSet without doing any consistency check.
	 * @param c Cluster
	 */
  public void addClusterNoCheck(Cluster c) {
    _clusters.add(c);
    _isDirty = true;
    updateCache(c);
  }


	/**
	 * add the Cluster given in the input argument as an extra cluster in this 
	 * ClusterSet but first checks to ensure that the smallest value in c is 
	 * the successor of the greatest number contained in the ClusterSet (remember
	 * that ClusterSet objects hold integer index values to the sequence they 
	 * are clustering).
	 * @param c Cluster
	 * @throws CException if the consistency check fails 
	 */
  public void addCluster(Cluster c) throws CException {
    // does c start after last one in clusters?
    if (getLastIndex()+1 != c.getMin())
      throw new CException("wrong ordering in ClusterSet");
    _clusters.add(c);
    _isDirty=true;
    updateCache(c);
  }


	/**
	 * add the clusters in the input ClusterSet argument to (the right of) this
	 * ClusterSet object by calling the method <CODE>addCluster(c)</CODE> for each
	 * of the clusters of cs.
	 * @param cs ClusterSet
	 * @throws CException if the consistency check fails when adding the clusters
	 */
  public void addClustersRight(ClusterSet cs) throws CException {
    Vector clusters = cs._clusters;
    for (int i=0; i<clusters.size(); i++) {
      Cluster ci = (Cluster) clusters.elementAt(i);
      addCluster(ci);
    }
  }


	/**
	 * returns all Cluster objects in this ClusterSet containing numbers having as
	 * min value the specified input m. Normally, there will be just one such 
	 * cluster, but there are cases when a ClusterSet holds not just a partition
	 * of the sequence indices but a large covering of the sequence, in which 
	 * case the multiple-result-set is needed.
	 * @param m int
	 * @return Set  // Set&lt;Cluster&gt;
	 */
  public Set getSetsStartingWithFast(int m) {
    return (Set) _startWith.get(new Integer(m));
  }


	/**
	 * slower method implementing the same functionality of 
	 * <CODE>getSetsStartingWithFast(m)</CODE>.
	 * @param m int
	 * @return Set  // Set&lt;Cluster&gt;
	 */
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


	/**
	 * return the <CODE>Cluster</CODE> object that starts with startind and 
	 * ends with endind.
	 * @param startind int
	 * @param endind int
	 * @return Cluster
	 * @throws CException if no such Cluster exists in this ClusterSet object. 
	 */
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


	/**
	 * return the largest integer value held in any of the clusters in this 
	 * ClusterSet object.
	 * @return int
	 */
  public int getLastIndex() {
    Iterator it = _clusters.iterator();
    int res = -1;
    while (it.hasNext()) {
      Cluster c = (Cluster) it.next();
      if (c.getMax()>res) res = c.getMax();
    }
    return res;
  }


	/**
	 * evaluate the clustering represented by this object according to the 
	 * data and parameters specified in the input <CODE>Params</CODE> argument.
	 * @param p Params
	 * @return double will be <CODE>Double.MAX_VALUE</CODE> if this cluster-set
	 * doesn't contain all the indices of the data sequence in p.
	 */
  public double evaluate(Params p) { return evaluate(p, false); }


	/**
	 * evaluate the clustering represented by this object according to the 
	 * data and parameters specified in the input <CODE>Params</CODE> argument p, 
	 * allowing partial evaluation if the second argument is true.
	 * @param p Params
	 * @param allow_partial boolean if true, the value returned is the sum of 
	 * distances from center of the clusters contained in this ClusterSet, even
	 * if the ClusterSet is not a complete partitioning of the data sequence 
	 * described in p
	 * @return double will be <CODE>Double.MAX_VALUE</CODE> if this ClusterSet 
	 * does not represent a complete partition of the data sequence in p and
	 * allow_partial is false, or if any of the clusters in this ClusterSet 
	 * violates the max. sum-of-intra-distances threshold constraint specified in
	 * p
	 */
  public double evaluate(Params p, boolean allow_partial) {
    // no partial evaluations allowed
    if (allow_partial==false && getLastIndex()!=p.getSequenceLength()-1) {
      return Double.MAX_VALUE;
    }
    if (_isDirty==false) return _val;  // short-cut
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


	/**
	 * return a String representation of this ClusterSet as follows:
	 * [(val_1,...val_i),...(val_j,...val_n)] where the vals are the real values
	 * of the data sequence in p.
	 * @param p Params
	 * @return String
	 */
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

