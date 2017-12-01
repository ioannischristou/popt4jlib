package graph.coarsening;

import graph.*;
import parallel.ParallelException;
import java.util.*;

/**
 * base class for coarsening graphs, which is useful in problems such as graph
 * partitioning. None of the methods are thread-safe; the class itself is not
 * thread-safe and must be protected for concurrent use under a multi-threading
 * context.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
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
  // coarsen() of course.)

  protected HashMap _map;  // map<Integer oldId, Integer newId>
  protected HashMap _rmap;  // map<Integer newId, Set<Integer oldId> >
  protected Graph _g;
  protected Graph _coarseG;
  protected int[] _graphPartition;  // _graphPartition[i] in {1,...,k}
                                    // i in {0,..._g.numnodes-1}
  private HashMap _properties;


	/**
	 * base coarsener constructor.
	 * @param g Graph
	 * @param partition int[] partition[i] must be in {1,...,k} where k is the 
	 * number of required partitions
	 * @param properties HashMap may be null
	 */
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


	/**
	 * return the coarse graph resulting from an invocation of the 
	 * <CODE>coarsen()</CODE> method.
	 * @return Graph the coarser graph
	 * @throws CoarsenerException if graph has not been coarsened yet
	 */
  public Graph getCoarseGraph() throws CoarsenerException {
    if (_coarseG==null) throw new CoarsenerException("graph not coarsened yet...");
    return _coarseG;
  }


	/**
	 * return the original graph.
	 * @return Graph
	 */
  public Graph getOriginalGraph() {
    return _g;
  }


	/**
	 * return the partition array, specifying a partition of original node ids,
	 * among k components. The range of the map is in {1,...,k}.
	 * Notice that the exact data member is returned, so changes to the elements
	 * of the returned array are directly reflected to this object's 
	 * <CODE>_graphPartition</CODE> data member.
	 * @return int[]
	 */
  public int[] getPartition() {
    return _graphPartition;
  }

	
	/**
	 * return the coarse-node id of the node of the fine-level graph with given 
	 * id.
	 * @param finegraphid int the id of the fine-level (original) graph
	 * @return int
	 * @throws IllegalArgumentException if finegraphid doesn't exist or if 
	 * <CODE>coarsen()</CODE> hasn't been called yet.
	 */
	public int getCoarseNodeId(int finegraphid) {
		try {
			int cid = ((Integer) _map.get(new Integer(finegraphid))).intValue();
			return cid;
		}
		catch (Exception e) {
			throw new IllegalArgumentException("id "+finegraphid+
				                                 " doesn't exist or _map is null");
		}
	}
	
	
	/**
	 * return the set of fine-level node ids that correspond to the coarse level
	 * graph node with id the given id.
	 * @param coarsegraphid int the id of the coarse-level graph
	 * @return Set  // Set&lt;Integer&gt; that may be null
	 * @throws IllegalArgumentException if <CODE>coarsen()</CODE> hasn't been 
	 * called yet.
	 */
	public Set getFineLevelNodeIds(int coarsegraphid) {
		try {
			Set rids = (Set) _rmap.get(new Integer(coarsegraphid));
			return rids;
		}
		catch (Exception e) {
			throw new IllegalArgumentException("_rmap is null");			
		}
	}

	
	/**
	 * set the partition array to the input argument. The array must have exactly
	 * as many elements as the original graph has nodes, and the values of the 
	 * elements must be in {1,...,k} where k is the number of partitions required.
	 * @param arr int[]
	 */
  public void setPartition(int[] arr) {
    _graphPartition = arr;
  }


	/**
	 * get the property mapped for the given key provided as argument.
	 * @param name String
	 * @return Object
	 */
  public Object getProperty(String name) {
    return _properties.get(name);
  }


	/**
	 * set the property for the given key provided as first argument, to be the
	 * object provided as second argument.
	 * @param name String
	 * @param obj Object
	 */
  public void setProperty(String name, Object obj) {
    _properties.put(name, obj);
  }


	/**
	 * return the properties map of this object.
	 * @return HashMap
	 */
  public HashMap getProperties() {
    return _properties;
  }


	/**
	 * the main method of this class. Sub-classes must implement this method.
	 * @throws GraphException
	 * @throws CoarsenerException
	 * @throws ParallelException 
	 */
  abstract public void coarsen() 
		throws GraphException, CoarsenerException, ParallelException;


  abstract public Coarsener newInstance(Graph g, 
		                                    int[] partition, 
																				HashMap properties);


	/**
	 * resets this object so it may be reused in another coarsening for the same
	 * graph. Does NOT reset the <CODE>_graphPartition</CODE> array data member.
	 */
  protected void reset() {
    //_map = new HashMap();
		_map.clear();
    //_rmap = new HashMap();
		_rmap.clear();
    _coarseG = null;
    if (_properties!=null) {
      // make sure coarseNodeDocumentArray and coarsePartition don't exist
      _properties.remove("coarseNodeDocumentArray");
      _properties.remove("coarsePartition");
    }
  }

}

