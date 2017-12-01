package graph.coarsening;

import graph.*;
import java.util.*;

/**
 * analogue of <CODE>Coarsener</CODE> class in same package for hyper-graphs.
 * The class if not thread-safe, and must be protected when used in a multi-
 * threaded context.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public abstract class HCoarsener {
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
  protected HGraph _g;
  protected HGraph _coarseG;
  protected int[] _graphPartition;  // _graphPartition[i] in {1,...,k}
                                    // i in {0,..._g.numnodes-1}
  private HashMap _properties;


	/**
	 * single public constructor.
	 * @param g HGraph
	 * @param partition int[] describes the partition of the nodes of the fine
	 * hgraph (partition[i] must be in {1,...,k} where k is the number of 
	 * partitions required.
	 * @param properties HashMap
	 */
  public HCoarsener(HGraph g, int[] partition, HashMap properties) {
    _map = new HashMap();
    _rmap = new HashMap();
    _g = g;
    _coarseG = null;
    if (properties!=null) {
      _properties = new HashMap(properties);
      // make sure coarseNodeDocumentArray and coarsePartition don't exist
      _properties.remove("coarsening.HCoarsener.coarseNodeDocumentArray");
      _properties.remove("coarsening.HCoarsener.coarsePartition");
    }
    _graphPartition = partition;
  }


	/**
	 * returns the coarse hgraph object produced after invoking 
	 * <CODE>coarsen()</CODE>.
	 * @return HGraph
	 * @throws CoarsenerException if the method coarsen() has not been called yet 
	 */
  public HGraph getCoarseHGraph() throws CoarsenerException {
    if (_coarseG==null) throw new CoarsenerException("Hgraph not coarsened yet...");
    return _coarseG;
  }


	/**
	 * return the original (fine-level) hgraph.
	 * @return HGraph
	 */
  public HGraph getOriginalHGraph() {
    return _g;
  }


	/**
	 * returns the partition of the fine graph. Notice: returns the actual data
	 * member array.
	 * @return int[]
	 */
  public int[] getPartition() {
    return _graphPartition;
  }


	/**
	 * returns a new int[] representing the partition of the fine-level nodes
	 * given a partition of the coarser hgraph's nodes.
	 * @param coarsepartition int[]
	 * @return int[]
	 * @throws CoarsenerException if coarsen() has not been invoked yet
	 */
  public int[] getFinePartition(int[] coarsepartition) throws CoarsenerException {
    if (_coarseG==null) throw new CoarsenerException("HGraph not coarsened yet");
    final int finenodes = _g.getNumNodes();
    int[] finepartition = new int[finenodes];
    for (int i=0; i<finenodes; i++) {
      Integer cid = (Integer) _map.get(new Integer(i));
      int p = coarsepartition[cid.intValue()];
      finepartition[i] = p;
    }
    return finepartition;
  }


	/**
	 * set the partition for this object's fine-level graph.
	 * @param arr int[]
	 */
  public void setPartition(int[] arr) {
    _graphPartition = arr;
  }


	/**
	 * return the property associated with key name.
	 * @param name String
	 * @return Object
	 */
  public Object getProperty(String name) {
    return _properties.get(name);
  }


	/**
	 * set the property value obj associated with key name.
	 * @param name String
	 * @param obj Object
	 */
  public void setProperty(String name, Object obj) {
    _properties.put(name, obj);
  }


	/**
	 * return the hash-map of properties for this object.
	 * @return HashMap
	 */
  public HashMap getProperties() {
    return _properties;
  }


	/** 
	 * the main method of the class. Sub-classes must implement this method.
	 * @throws GraphException
	 * @throws CoarsenerException 
	 */
  abstract public void coarsen() throws GraphException, CoarsenerException;


	/**
	 * factory method. Sub-classes must implement this method.
	 * @param g HGraph
	 * @param partition int[]
	 * @param properties HashMap
	 * @return HCoarsener
	 */
  abstract public HCoarsener newInstance(HGraph g, int[] partition, HashMap properties);


	/**
	 * resets object's members so that it can be re-used in another coarsening
	 * method invocation. Does not reset the <CODE>_partition</CODE> data member.
	 */
  protected void reset() {
    _map = new HashMap();
    _rmap = new HashMap();
    _coarseG = null;
    if (_properties!=null) {
      // make sure coarseNodeDocumentArray and coarsePartition don't exist
      _properties.remove("coarsening.HCoarsener.coarseNodeDocumentArray");
      _properties.remove("coarsening.HCoarsener.coarsePartition");
    }
  }

}

