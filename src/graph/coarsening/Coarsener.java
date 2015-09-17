package graph.coarsening;

import graph.*;
import parallel.ParallelException;
import java.util.*;

public abstract class Coarsener {
  // use of protected is necessary else each
  // data member would need accessor/setter method.
  // since the mechanism of direct inheritance is used
  // to implement different coarsening strategies
  // (as different coarseners) like in the
  // strategy design pattern, the members had better be left
  // with protected access control rights.

  // the partition of the coarse graph is an array int[] of size
  // _coarseG.numnodes which can be returned after having called
  // coarsen(), by calling getProperty("coarsePartition");

  // the document that is the fine graph's nodes is associated with
  // is passed in the coarsener as the "nodeDocumentArray" property
  // which is a Document[0..._g.numnodes-1]. The coarsener will
  // figure out the Document that is the centroid of each new node
  // in the coarse graph and put it in the property "coarseNodeDocumentArray"
  // as a Document[0..._coarseG.numnodes-1] (you get this after having called
  // coarsen() of course.

  protected HashMap _map;  // map<Integer oldId, Integer newId>
  protected HashMap _rmap;  // map<Integer newId, Set<Integer oldId> >
  protected Graph _g;
  protected Graph _coarseG;
  protected int[] _graphPartition;  // _graphPartition[i] in {1,...,k}
                                    // i in {0,..._g.numnodes-1}
  private HashMap _properties;


  public Coarsener(Graph g, int[] partition, HashMap properties) {
    _map = new HashMap();
    _rmap = new HashMap();
    _g = g;
    _coarseG = null;
    if (properties!=null) {
      _properties = new HashMap(properties);
      // make sure coarseNodeDocumentArray and coarsePartition don't exist
      _properties.remove("coarseNodeDocumentArray");
      _properties.remove("coarsePartition");
    }
    _graphPartition = partition;
  }


  public Graph getCoarseGraph() throws CoarsenerException {
    if (_coarseG==null) throw new CoarsenerException("graph not coarsened yet...");
    return _coarseG;
  }


  public Graph getOriginalGraph() {
    return _g;
  }


  public int[] getPartition() {
    return _graphPartition;
  }


  public void setPartition(int[] arr) {
    _graphPartition = arr;
  }


  public Object getProperty(String name) {
    return _properties.get(name);
  }


  public void setProperty(String name, Object obj) {
    _properties.put(name, obj);
  }


  public HashMap getProperties() {
    return _properties;
  }


  abstract public void coarsen() throws GraphException, CoarsenerException, ParallelException;


  abstract public Coarsener newInstance(Graph g, int[] partition, HashMap properties);


  protected void reset() {
    _map = new HashMap();
    _rmap = new HashMap();
    _coarseG = null;
    if (_properties!=null) {
      // make sure coarseNodeDocumentArray and coarsePartition don't exist
      _properties.remove("coarseNodeDocumentArray");
      _properties.remove("coarsePartition");
    }
  }

}

