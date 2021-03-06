package graph;

import utils.IntSet;
import utils.DataMgr;
import parallel.DMCoordinator;
import parallel.ParallelException;
import parallel.BoundedBufferArrayUnsynchronized;
import java.io.Serializable;
import java.util.*;
import parallel.BoundedMinHeapUnsynchronized;


/**
 * Graph class represents (directed) graphs, and is thread-safe with the
 * exception of the methods ending with "Unsynchronized" which for performance
 * purposes are not.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Graph implements Serializable {
  // serializable so that it can be transported over sockets and data-streams
  // private final static long serialVersionUID = 1284981207616820009L;
  private long _id;  // unique id -- uniqueness property not guaranteed when
                     // "transported" (e.g. via sockets) to other JVMs
  private Node[] _nodes;
  private Link[] _arcs;
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
	private HashMap _maxNodeWeights;  // map<String wname, Double val> maintains
	                                    // the max. node weight value for a wname.
	private HashMap _sortedNodeArrays;  // map<String name,
	                                      //     Node[] nodes_sorted_desc>
  private DMCoordinator _rwLocker=null;  // used for ensuring that the methods
                                         // are re-entrant (i.e. thread-safe)
	boolean _isDirectionReverted=false;  // set to true when edges point
	                                     // in the opposite direction than
	                                     // the original: useful in some
	                                     // SPP algorithms. Set to package-private
	                                     // access for fast access from Link class


	/**
	 * public factory construction method to avoid escaping this in the 2-arg
	 * constructor of Graph. The public static factory construction methods are
	 * in essence the only methods to construct new Graph objects.
	 * @param numnodes int
	 * @param numarcs int
	 * @return Graph
	 */
	public static Graph newGraph(int numnodes, int numarcs) {
		Graph g = new Graph(numnodes, numarcs);
		g.initialize();
		return g;
	}

	/**
	 * factory construction method to replace calls of the form
	 * <CODE>Graph g = new Graph(numnodes, numarcs, labels)</CODE>, so as to avoid
	 * escaping this in the constructor.
	 * @param numnodes int
	 * @param numarcs int
	 * @param labels Object[] the labels of the nodes of the newly constructed
	 * Graph. Notice that the method will fail if the length of this array is not
	 * equal to the <CODE>numnodes</CODE> argument. The labels must also have
	 * distinct hash-codes so that they can serve as keys in the reverse map from
	 * label-object to node-id.
	 * @return Graph
	 */
	public static Graph newGraph(int numnodes, int numarcs, Object[] labels) {
		Graph g = new Graph(numnodes, numarcs, labels);
		g.initialize();
		return g;
	}


	/**
	 * factory construction method serving as the copy-constructor for Graph
	 * objects. Avoids escaping this in the construction process.
	 * @param g Graph
	 * @return Graph
	 */
	public static Graph newGraph(Graph g) {
		Graph g2 = new Graph(g);
		g2.initialize(g);
		return g2;
	}


  /**
   * private Graph constructor, given the number of nodes and the number of arcs
   * that this Graph object will contain.
   * @param numnodes int
   * @param numarcs int
   */
  private Graph(int numnodes, int numarcs) {
    _nodes = new Node[numnodes];
    if (numarcs>0) _arcs = new Link[numarcs];
    _nodeLabels = null;
    _labelMap = null;
		_maxNodeWeights = new HashMap();
		_sortedNodeArrays = new HashMap();

    // itc 2015-02-25: move Node initialization to avoid escaping this
		// for (int i=0; i<numnodes; i++) _nodes[i] = new Node(this, i);

    // for (int i=0; i<numarcs; i++) _arcs[i] = null;  // redundant
    _id = DataMgr.getUniqueId();
    _rwLocker = DMCoordinator.getInstance("Graph"+_id);
  }


  /**
   * private Graph constructor, taking as arguments the number of nodes, the
   * number of acrs, and an Object array to form the labels of the nodes of this
   * Graph. The labels must have distinct hash-codes so as to serve as keys in
	 * a reverse map (from label to node-id), and the labels must have the same
	 * length as the <CODE>numnodes</CODE> argument.
   * @param numnodes int
   * @param numarcs int
   * @param labels Object[]
   */
  private Graph(int numnodes, int numarcs, Object[] labels) {
    this(numnodes, numarcs);
    _nodeLabels = labels;
    _labelMap = new HashMap();
    for (int i=0; i<numnodes; i++)
      _labelMap.put(_nodeLabels[i], new Integer(i));
  }


  /**
   * copy ctor: perform deep copies: each node and graph is copied to new copies
   * so that operations on the new graph or its nodes do not affect the original
   * graph. If the graph's nodes have labels however, being Object instances of
   * unknown class, the new Graph's labels themselves are just refs to the
   * original Graph's labels.
   * @param g Graph
   */
  private Graph(Graph g) {
    // 0. initialization
    _nodeLabels = null;
    _labelMap = null;
    _id = DataMgr.getUniqueId();
    // set the read/write lock correctly
    _rwLocker = DMCoordinator.getInstance("Graph"+_id);
		// itc 2015-02-25: moved below functionality to initialize(g) method
		// to avoid escaping this in the constructor.
		/*
		g.getReadAccess();
		if (g.getNumArcs()>0) _arcs = new Link[g.getNumArcs()];
		final int numnodes = g.getNumNodes();
		_nodes = new Node[numnodes];
		// 1. copy arcs and make nodes in the process
		for (int i=0; i<numnodes; i++) _nodes[i] = new Node(this, g.getNode(i), i);
		if (_arcs!=null) {
			for (int i = 0; i < _arcs.length; i++) {
				Link ai = g.getLink(i);
				try {
					addLink(ai.getStart(), ai.getEnd(), ai.getWeight());
				}
				catch (GraphException e) {
					e.printStackTrace(); // cannot occur
				}
				catch (ParallelException e) {
					e.printStackTrace();  // cannot occur
				}
			}
		}
		// 2. set the _nodeLabels, _labelMap and _maxNodeWeights if not null
		if (g._nodeLabels!=null) {
			_nodeLabels = new Object[numnodes];
			for (int i=0; i<numnodes; i++) {
				try {
					setNodeLabel(i, g._nodeLabels[i]);
				}
				catch (ParallelException e) {
					// cannot get here: no-op
					e.printStackTrace();
				}
			}
		}
		if (g._maxNodeWeights!=null)
			_maxNodeWeights = new HashMap(g._maxNodeWeights);
		// don't bother with _sortedNodeWeights: they will be re-generated on demand
		try {
			g.releaseReadAccess();
		}
		catch (ParallelException e) {
			e.printStackTrace();  // cannot occur
		}
		*/
  }


	/**
	 * method exists to avoid escaping this in the constructors.
	 */
	private void initialize() {
		final int numnodes = _nodes.length;
		for (int i=0; i<numnodes; i++) _nodes[i] = new Node(this, i);
	}


	/**
	 * method exists to avoid escaping this in the copy-ctor.
	 * @param g Graph
	 */
	private void initialize(Graph g) {
		g.getReadAccess();
		if (g.getNumArcs()>0) _arcs = new Link[g.getNumArcs()];
		final int numnodes = g.getNumNodes();
		_nodes = new Node[numnodes];
		// 1. copy arcs and make nodes in the process
		for (int i=0; i<numnodes; i++) _nodes[i] = new Node(this, g.getNode(i), i);
		if (_arcs!=null) {
			for (int i = 0; i < _arcs.length; i++) {
				Link ai = g.getLink(i);
				try {
					addLink(ai.getStart(), ai.getEnd(), ai.getWeight());
				}
				catch (GraphException e) {
					e.printStackTrace(); // cannot occur
				}
				catch (ParallelException e) {
					e.printStackTrace();  // cannot occur
				}
			}
		}
		// 2. set the _nodeLabels, _labelMap and _maxNodeWeights if not null
		if (g._nodeLabels!=null) {
			_nodeLabels = new Object[numnodes];
			for (int i=0; i<numnodes; i++) {
				try {
					setNodeLabel(i, g._nodeLabels[i]);
				}
				catch (ParallelException e) {
					// cannot get here: no-op
					e.printStackTrace();
				}
			}
		}
		if (g._maxNodeWeights!=null)
			_maxNodeWeights = new HashMap(g._maxNodeWeights);
		// don't bother with _sortedNodeWeights: they will be re-generated on demand
		try {
			g.releaseReadAccess();
		}
		catch (ParallelException e) {
			e.printStackTrace();  // cannot occur
		}
	}


	/**
	 * reverses the direction of all links in this Graph, so that after calling
	 * this method once, the methods <CODE>Node.getInLinks()</CODE>,
	 * <CODE>Node.getOutLinks()</CODE> return respectively the sets
	 * <CODE>Node._outlinks</CODE> and <CODE>Node._inlinks</CODE>, and the methods
	 * <CODE>Link.getStart(),Link.getEnd()</CODE> return respectively
	 * <CODE>Link._enda</CODE> and <CODE>Link._starta</CODE>.
	 * @throws ParallelException
	 */
	void reverseLinksDirection() throws ParallelException {
		boolean got_lock=false;
		try {
			getWriteAccess();
			got_lock=true;
			_isDirectionReverted = !_isDirectionReverted;
		}
		finally {
			if (got_lock) {
				releaseWriteAccess();
			}
		}
	}


	public boolean isLinksDirectionReversed() {
		boolean got_lock=false;
		try {
			getReadAccess();
			got_lock=true;
			return _isDirectionReverted;
		}
		finally {
			if (got_lock) {
				try {
					releaseReadAccess();
				}
				catch (ParallelException e) {  // never throws
					// no-op
				}
			}
		}
	}


  /**
   * add a (directed) link between nodes with id starta and enda.
   * @param starta int (in the interval [0, this.getNumNodes()-1])
   * @param enda int (in the interval [0, this.getNumNodes()-1])
   * @param weight double
   * @throws GraphException if the <CODE>addLink(starta,enda)</CODE> has
	 * been called <CODE>getNumArcs()</CODE> times already, or if the starta or
	 * enda arguments are out-of-bounds or if the arc (starta,enda) has already
	 * been added to this Graph
   * @throws ParallelException if the current thread also had the read-lock
   * but there was at least another reader thread in the system
   */
  public void addLink(int starta, int enda, double weight)
      throws GraphException, ParallelException {
    boolean got_wlock=false;
		try {
      getWriteAccess();
			got_wlock = true;
      if (_arcs == null)throw new GraphException("null _arcs array.");
      if (_addLinkPos >= _arcs.length)throw new GraphException(
          "cannot add more arcs.");
      if (starta < 0 || starta >= _nodes.length)
        throw new GraphException("start node " + starta + " is out-of-bounds");
      if (enda < 0 || enda >= _nodes.length)
        throw new GraphException("end node " + enda + " is out-of-bounds");
			if (existsLink(starta,enda))
				throw new GraphException("Graph.addLink("+starta+","+enda+","+weight+
					                       "): link already exists");
      _arcs[_addLinkPos] = new Link(this, _addLinkPos++, starta, enda, weight);
    }
    finally {
      try {
        if (got_wlock) releaseWriteAccess();
      }
      catch (ParallelException e) {
        // never gets here
        // no-op
        e.printStackTrace();
      }
    }
  }


	/**
	 * check whether a link exists from starta to enda.
	 * @param starta int source-node-id
	 * @param enda int end-node-id
	 * @return boolean true iff the link exists
	 * @throws GraphException if arguments are out-of-bounds
	 * @throws ParallelException never throws this exception
	 */
	public boolean existsLink(int starta, int enda)
			throws GraphException, ParallelException {
		try {
      getReadAccess();
      if (_arcs == null)throw new GraphException("null _arcs array.");
      if (starta < 0 || starta >= _nodes.length)
        throw new GraphException("start node " + starta + " is out-of-bounds");
      if (enda < 0 || enda >= _nodes.length)
        throw new GraphException("end node " + enda + " is out-of-bounds");
			Node s = _nodes[starta];
			Set s_outlinks = s.getOutLinks();
			Iterator sit = s_outlinks.iterator();
			while (sit.hasNext()) {
				int lid = ((Integer)sit.next()).intValue();
				Link l = _arcs[lid];
				if (l.getEnd()==enda) return true;
			}
			return false;
    }
    finally {
      releaseReadAccess();
    }
	}


  /**
   * return the unique id of this Graph object.
   * @return long
   */
  public long getId() {
    try {
      getReadAccess();
      return _id;
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


  /**
   * return the Link object with the given id.
   * The method will throw OutOfBoundsException if the id is out-of-bounds
   * @param id int
   * @return Link
   */
  public Link getLink(int id) {
    try {
      getReadAccess();
      return _arcs[id];
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


  /**
   * return the Node object with the given id.
   * The method will throw OutOfBoundsException if the id is out-of-bounds
   * @param id int
   * @return Node
   */
  public Node getNode(int id) {
    try {
      getReadAccess();
      return _nodes[id];
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


  /**
   * return the Node object with the given id.
	 * This operation is unsynchronized, and therefore, if used in a multi-
	 * threaded context, should be externally synchronized or otherwise provide
	 * guarantees that prevent race-conditions from occurring.
   * The method will throw OutOfBoundsException if the id is out-of-bounds
   * @param id int
   * @return Node
   */
  public Node getNodeUnsynchronized(int id) {
    return _nodes[id];
  }


  /**
   * return the number of nodes of this Graph
   * @return int
   */
  public int getNumNodes() { return _nodes.length; }


  /**
   * return the number of links of this Graph.
   * @return int
   */
  public int getNumArcs() {
    if (_arcs==null) return 0;
    else return _arcs.length;
  }


  /**
   * return the label of the node with id i (if any exists; else null).
   * @param i int
   * @return Object
   */
  public Object getNodeLabel(int i) {
    try {
      getReadAccess();
      if (_nodeLabels == null) return null;
      return _nodeLabels[i];
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


  /**
   * return the id of the node whose label is passed in the argument label (if
   * it exists, otherwise throw GraphException)
   * @param label Object
   * @throws GraphException if label doesn't exist in the map from labels to
	 * node-ids
   * @return int
   */
  public int getNodeIdByLabel(Object label) throws GraphException {
    try {
      getReadAccess();
      Integer nid = (Integer) _labelMap.get(label);
      if (nid == null) throw new GraphException("no such label");
      return nid.intValue();
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


  /**
   * set the node with id i's label to the Object o.
   * @param i int
   * @param o Object
   * @throws ParallelException if the current thread has the read-lock and there
   * was at least another thread with a read-lock
   */
  public void setNodeLabel(int i, Object o) throws ParallelException {
    boolean got_wlock=false;
		try {
      getWriteAccess();
			got_wlock=true;
      if (_nodeLabels == null) {
        _nodeLabels = new Object[_nodes.length];
        _labelMap = new HashMap();
      }
      if (_nodeLabels[i] != null) _labelMap.remove(_nodeLabels[i]);
      _nodeLabels[i] = o;
      _labelMap.put(o, new Integer(i));
    }
    finally {
      if (got_wlock) releaseWriteAccess();
    }
  }


	/**
	 * modifies the weight of the link with given id to positive infinity. Useful
	 * when re-optimizing shortest-path problems and must somehow "remove" certain
	 * paths.
	 * @param linkid int
   * @throws ParallelException if the current thread has the read-lock and there
   * was at least another thread with a read-lock
	 */
	public void setInfiniteLinkWeight(int linkid) throws ParallelException {
    boolean got_wlock=false;
		try {
      getWriteAccess();
			got_wlock=true;
			_arcs[linkid].setWeight(Double.POSITIVE_INFINITY);
    }
    finally {
      if (got_wlock) releaseWriteAccess();
    }
	}


  /**
   * obtain the read-lock for this Graph object.
   */
  public void getReadAccess() {
    _rwLocker.getReadAccess();
  }


  /**
   * release the read-lock for this Graph object.
   * @throws ParallelException if the current thread doesn't have a read-lock
   * for this Graph
   */
  public void releaseReadAccess() throws ParallelException {
    _rwLocker.releaseReadAccess();
  }


  /**
   * get a write-lock for this Graph.
   * @throws ParallelException if the current thread already has a read-lock and
   * there is another thread also having a read-lock on the system
   */
  public void getWriteAccess() throws ParallelException {
    _rwLocker.getWriteAccess();
  }


  /**
   * release the write-lock for this Graph object.
   * @throws ParallelException if the current thread doesn' have a write-lock
   * for this Graph
   */
  public void releaseWriteAccess() throws ParallelException {
    _rwLocker.releaseWriteAccess();
  }


  /**
   * return the shortest path between Node s and Node t, where the cost of a
   * path is measured in terms of link weights adding up, and all link weights
	 * being assumed non-negative. Edges are directed, and the shortest path is
	 * one in which all edges are "forward" edges. Essentially this is Dijkstra's
	 * label setting method. The method will throw if it finds any negative link
	 * weight.
   * @param s Node
   * @param t Node
   * @return double will return <CODE>Double.MAX_VALUE</CODE> if there is no
	 * path from node s to node t.
	 * @throws IllegalArgumentException if s or t is null
	 * @throws GraphException if any link weight is negative
   */
  public double getShortestPath(Node s, Node t) throws GraphException {
    try {
      getReadAccess();
      if (s == t) return 0.0;
			if (s==null || t==null)
				throw new IllegalArgumentException("null source or target node");
			BoundedMinHeapUnsynchronized queue =
				new BoundedMinHeapUnsynchronized(_nodes.length);
      queue.addElement(new utils.Pair(s,new Double(0.0)));  // Pair(Node,double)
			HashMap labels = new HashMap();  // map<Node n, Double dist>
			labels.put(s, new Double(0.0));
      while (queue.size() > 0) {
        utils.Pair fp = (utils.Pair) queue.remove();  // remove min element
				Node n = (Node) fp.getFirst();
				if (n.getId()==t.getId())
					return ((Double) fp.getSecond()).doubleValue();  // done
        double nd = ((Double) fp.getSecond()).doubleValue();
        Set outlinks = n.getOutLinks();
        if (outlinks != null) {
          Iterator itout = outlinks.iterator();
          while (itout.hasNext()) {
            Integer lid = (Integer) itout.next();
            Link lin = _arcs[lid.intValue()];
            int oe = lin.getEnd();
            double wa = lin.getWeight();
						if (wa<0)
							throw new GraphException("Graph.getShortestPath(s,t): "+
								                       "encountered negative arc weight");
            Node ne = _nodes[oe];
            double nnd = nd + wa;
            Double ned = (Double) labels.get(ne);
            if (ned == null) {
							Double nndD = new Double(nnd);
              labels.put(ne, nndD);
              queue.addElement(new utils.Pair(ne, nndD)); // insert ne in heap
            }
						else if (ned.doubleValue() > nnd) {
							Double nndD = new Double(nnd);
              labels.put(ne, nndD);
							// update ne's position in the min-heap
							queue.decreaseKey(new utils.Pair(ne,ned), new utils.Pair(ne,nndD));
						}
          }
        }
      }
      return Double.MAX_VALUE;
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


  /**
   * return the shortest path costs between Node s and all nodes in this graph,
	 * where the cost of a path is measured in terms of link weights adding up,
	 * and all link weights are assumed non-negative. Edges are directed, and the
	 * shortest path is one in which all edges are "forward" edges. Essentially
	 * this is Dijkstra's label setting method. The method will throw if it finds
	 * any negative link weight.
   * @param s Node
   * @return double[] array whose i-th element is the cost of the shortest path
	 * from s to the i-th node. It will return
	 * <CODE>Double.POSITIVE_INFINITY</CODE> if there is no path from node s to
	 * the i-th node
	 * @throws IllegalArgumentException if s or t is null
	 * @throws GraphException if any link weight is negative
   */
	public double[] getAllShortestPaths(Node s) throws GraphException {
    try {
      getReadAccess();
			if (s==null)
				throw new IllegalArgumentException("null source node");
			BoundedMinHeapUnsynchronized queue =
				new BoundedMinHeapUnsynchronized(_nodes.length);
      queue.addElement(new utils.Pair(s,new Double(0.0)));  // Pair(Node,double)
			double[] labels = new double[_nodes.length];  // labels[i]=spp from s to i
			for (int i=0; i<labels.length; i++) labels[i]=Double.POSITIVE_INFINITY;
			labels[s.getId()]=0.0;
      while (queue.size() > 0) {
        utils.Pair fp = (utils.Pair) queue.remove();  // remove min element
				Node n = (Node) fp.getFirst();
        double nd = ((Double) fp.getSecond()).doubleValue();
        Set outlinks = n.getOutLinks();
        if (outlinks != null) {
          Iterator itout = outlinks.iterator();
          while (itout.hasNext()) {
            Integer lid = (Integer) itout.next();
            Link lin = _arcs[lid.intValue()];
            int oe = lin.getEnd();
            double wa = lin.getWeight();
						if (wa<0)
							throw new GraphException("Graph.getShortestPath(s,t): "+
								                       "encountered negative arc weight");
            Node ne = _nodes[oe];
            double nnd = nd + wa;
            double ned = labels[ne.getId()];
            if (Double.isInfinite(ned) && !Double.isInfinite(nnd)) {
							Double nndD = new Double(nnd);
							labels[ne.getId()]=nnd;  // // labels.put(ne, nndD);
              queue.addElement(new utils.Pair(ne, nndD)); // insert ne in heap
            }
						else if (ned > nnd) {
							Double nndD = new Double(nnd);
              labels[ne.getId()] = nnd;  // labels.put(ne, nndD);
							// update ne's position in the min-heap
							queue.decreaseKey(new utils.Pair(ne,new Double(ned)), new utils.Pair(ne,nndD));
						}
          }
        }
      }
      return labels;
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
	}


	/**
	 * get the number of independent components of this graph. Uses a cache-based
	 * lazy evaluation technique that computes the graph's components on-demand,
	 * and is appropriately guarded with this graph's read/write locks.
	 * @return int the number of components of this graph.
	 * @throws ParallelException if <CODE>getComponents()</CODE> has not been
	 * invoked yet, the current thread has a read-lock, and another thread has the
	 * read-lock on this graph as well.
	 */
  public int getNumComponents() throws ParallelException {
    int ci;
		try {
			getReadAccess();
			ci = _compindex;
		}
		finally {
			releaseReadAccess();
		}
		if (ci==-1) getComponents();
    return _compindex;
  }


  /**
   * return all maximal connected components of this Graph.
   * The array that is returned is of length _nodes.length
   * and the i-th element has a value from [0...#Components-1]
   * indicating to which component the node belongs.
   * @return int[]
   * @throws ParallelException if this is the first time the method is invoked,
	 * the current thread has a read-lock and there is also another thread with a
	 * read-lock.
   */
  public int[] getComponents() throws ParallelException {
    boolean do_rel = true;
    try {
      getReadAccess();
      if (_comps != null) {
        do_rel = false;
        _rwLocker.releaseReadAccess();
        return _comps;
      }
      releaseReadAccess();
      getWriteAccess();
      if (_comps!=null) {  // spirit of Double-Check Locking idiom but correct!
        return _comps;
      }
      // ok, must do the work: use dipth-first strategy
      _comps = new int[_nodes.length];
      _compCards = new Vector();
      final int numnodes = _nodes.length;
      for (int i = 0; i < numnodes; i++) _comps[i] = -1; // unassigned
      _compindex = 0;
      for (int i = 0; i < numnodes; i++) {
        // Set nbors = _nodes[i].getNbors();
        if (_comps[i] == -1) {
          // new component
          int numcomps = labelComponent(i, _compindex);
          _compCards.addElement(new Integer(numcomps));
          _compindex++;
        }
      }
      return _comps;
    }
    finally {
      try {
        if (do_rel) releaseWriteAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
    }
  }


  /**
   * return all maximal components of this Graph as an array of new Graph
   * objects. Each new graph will have the _nodeLabels array non-null and
   * the label will be an Integer object indicating the node's original
   * node id.
   * @return Graph[]
   * @throws GraphException
   * @throws ParallelException
   */
  public Graph[] getGraphComponents() throws GraphException, ParallelException {
    try {
      getReadAccess();
      if (_comps==null) {
        // we now need to release the read-lock because the getComponents()
        // method may easily throw ParallelException otherwise.
        releaseReadAccess();  // temporarily release the read-lock
        getComponents();  // this is the method that may throw ParallelException
        getReadAccess();  // now restore read-lock
      }
      final int numgraphs = _compindex;
      Graph[] graphs = new Graph[numgraphs];
      Set[] gnodes = new HashSet[numgraphs];  // gnodes[i] is HashSet<Node n>
      for (int i = 0; i < gnodes.length; i++) gnodes[i] = new HashSet();
      for (int i = 0; i < _nodes.length; i++) {
        Node ni = _nodes[i];
        gnodes[_comps[ni.getId()]].add(ni);
      }
      int[] map = new int[_nodes.length]; // map[orid-id] = id in new comp. graph
      for (int i = 0; i < _nodes.length; i++) map[i] = -1;
      for (int i = 0; i < numgraphs; i++) {
        Set ginodes = gnodes[i];
        // determine arcs in the component
        int ginumarcs = 0;
        Iterator it = ginodes.iterator();
        while (it.hasNext()) {
          Node nj = (Node) it.next();
          Set nj_inlinks = nj.getInLinks(); // Set<Integer linkid>
          ginumarcs += nj_inlinks.size();
          // no need to consider outlinks
        }
        // add arcs
        it = ginodes.iterator();
        int cnt = 0;
        Integer[] rmap = new Integer[ginodes.size()]; // rmap[new-id] = orig-id of node in original graph
        while (it.hasNext()) {
          Node nit = (Node) it.next();
          map[nit.getId()] = cnt;
          rmap[cnt++] = new Integer(nit.getId());
        }
        graphs[i] = newGraph(gnodes[i].size(), ginumarcs, rmap);
        it = ginodes.iterator();
        while (it.hasNext()) {
          Node nit = (Node) it.next();
					// set new node weights
					Node nn = graphs[i].getNode(map[nit.getId()]);
					nn.copyWeightsFrom(nit);
          Set inlinkids = (Set) nit.getInLinks();
          Iterator lit = inlinkids.iterator();
          while (lit.hasNext()) {
            Integer lid = (Integer) lit.next();
            Link l = _arcs[lid.intValue()];
            if (l.getEnd() != nit.getId()) {
              throw new GraphException("getGraphComponents(): " +
                                       "in creating sub-graph+" + i +
                                       ": inconsistent link?");
            }
            int starta = l.getStart();
            graphs[i].addLink(map[starta], map[nit.getId()], l.getWeight());
          }
        }
      }
      return graphs;
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


  /**
   * return the number of nodes in max. connected component i
   * i is in the range [0...#comps-1]
   * @param i int
   * @return int
   * @throws ParallelException
   */
  public int getComponentCard(int i) throws ParallelException {
    try {
      getReadAccess();
      if (_comps == null) {
        // we now need to release the read-lock because the getComponents()
        // method may easily throw ParallelException otherwise.
        releaseReadAccess();  // temporarily release the read-lock
        getComponents();  // this is the method that may throw ParallelException
        getReadAccess();  // now restore read-lock
      }
      Integer card = (Integer) _compCards.elementAt(i);
      return card.intValue();
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


  /**
   * construct and return the dual of this graph. This is accomplished by
   * creating a node in the dual for each arc in this graph, and connecting
   * nodes in the dual that correspond to neighboring arcs in the original.
	 * The arcs in the new graph will have unit weight.
   * @return Graph
   * @throws GraphException if there are no edges in this Graph
   */
  public Graph getDual() throws GraphException {
    if (_arcs==null)
      throw new GraphException("getDual(): dual does not exist.");
    try {
      getReadAccess();
      // 1. compute numarcs of the dual graph
      int numarcs = 0;
      for (int i = 0; i < _arcs.length; i++) {
        Link li = _arcs[i];
        Node s = _nodes[li.getStart()];
        Node e = _nodes[li.getEnd()];
        numarcs += s.getNbors().size() - 1;
        numarcs += e.getNbors().size() - 1;
      }
      numarcs /= 2;
			// 2. construct new graph object
      Graph dg = newGraph(_arcs.length, numarcs);
      int count = 0;
			// 3. create all new graph links
      for (int i = 0; i < _nodes.length; i++) {
        Node ni = _nodes[i];
        Set ni_links = new HashSet(ni.getOutLinks());
        ni_links.addAll(ni.getInLinks());
        Iterator it = ni_links.iterator();
        while (it.hasNext()) {
          int lid = ( (Integer) it.next()).intValue();
          // all other links lj to node ni, with lj_id > lid form a link in the
          // dual graph
          Iterator it2 = ni_links.iterator();
          while (it2.hasNext()) {
            int ljd = ( (Integer) it2.next()).intValue();
            if (ljd > lid) {
              // add link (lid,ljd)
							try {
								dg.addLink(lid, ljd, 1);
	              ++count;
							}
							catch (ParallelException e) {  // cannot happen
								e.printStackTrace();
							}
            }
          }
        }
      }
      // 4. now add weights to dual nodes
      for (int i = 0; i < _arcs.length; i++) {
				try {
					dg.getNode(i).setWeight("value", new Double(_arcs[i].getWeight()));
				}
				catch (ParallelException e) {  // cannot happen
					e.printStackTrace();
				}
      }
      if (count != numarcs) {
        throw new GraphException("error counting arcs: numarcs should be " +
                                 numarcs + " but has count " + count);
      }
			// 5. done
      return dg;
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


	/**
	 * get the complement of this (assumed undirected) Graph. The complement of a
	 * graph g is a graph with the same nodes as g, but its arc-set is the
	 * complement of g's arcs-set in other words, an arc (i,j) (with i &lt; j) is
	 * in the complement of g if and only if i and j are not direct neighbors in g.
	 * The arcs in the new graph will have unit weight. Any node labels will be
	 * shared in the new graph.
	 * @return Graph
	 * @throws GraphException if this graph is directed: there exists a pair of
	 * nodes (i,j) such that both (i,j) and (j,i) are arcs of this Graph.
	 */
	public Graph getComplement() throws GraphException {
		getReadAccess();
		final int n = _nodes.length;
		final int _a = _arcs==null ? 0 : _arcs.length;
		final int a = (n*(n-1))/2 - _a;
		// 0. make new graph object
		Graph cg = _nodeLabels==null ? newGraph(n,a) : newGraph(n, a, _nodeLabels);
		// 1. make arcs
		for (int i=0; i<n; i++) {
			Node ni = getNode(i);
			for (int j=i+1; j<n; j++) {
				if (ni.getNborIndices(Double.NEGATIVE_INFINITY).contains(new Integer(j)))
					continue;
				try {
					cg.addLink(i, j, 1);
				}
				catch (ParallelException e) {  // cannot get here
					// no-op
					e.printStackTrace();
				}
			}
		}
		// 2. finally, take care of nodes' weights
		for (int i=0; i<n; i++) {
			cg.getNode(i).copyWeightsFrom(getNode(i));
		}
		try {
			releaseReadAccess();
		}
		catch (ParallelException e) {  // cannot get here
			// no-op
			e.printStackTrace();
		}
		return cg;
	}


  /**
   * get the support of a clique of this node in the other Graph g, in terms of
   * their labels. The method compares how many arcs in the clique also appear
   * in the Graph g, when the labels of the endpoints of the two nodes are used.
   * @param nodes Set&lt;Integer node_id&gt;
   * @param g Graph
   * @return double the percentage of arcs from the clique that are also present
   * in the Graph g
   * @throws GraphException if one of the nodes has no labels
   * @throws IllegalArgumentException if the nodes argument is null or empty
   * @throws ParallelException if the current thread has a write-lock on the
   * Graph object with the largest _id. This is needed to ensure no deadlocks
   * can arise.
   */
  public double getCliqueLabelSupport(Set nodes, Graph g) throws GraphException, ParallelException {
    if (g==this) return 1.0;
    boolean this_locked = false;
    boolean g_locked = false;
    boolean this_is_first = _id < g.getId();
    try {
      // first, ensure no deadlocks can arise
      if ((this_is_first && g._rwLocker.currentThreadHasWriteLock()) ||
          (!this_is_first && _rwLocker.currentThreadHasWriteLock()))
        throw new ParallelException("current thread has write-lock on the Graph object w/ largest _id");
      if (this_is_first) {
        getReadAccess();
        this_locked = true;
        g.getReadAccess();
        g_locked = true;
      }
      else {
        g.getReadAccess();
        g_locked = true;
        getReadAccess();
        this_locked = true;
      }
      // now do the work
      if (nodes == null || nodes.size() == 0)throw new IllegalArgumentException(
          "null of empty clique");
      if (_nodeLabels == null || g == null || g._nodeLabels == null)
        throw new GraphException("no node labels in one Graph");
      double res = 0;
      Iterator iter = nodes.iterator();
      while (iter.hasNext()) {
        Integer nid = (Integer) iter.next();
        Node n = _nodes[nid.intValue()];
        Iterator it2 = n.getNbors().iterator();
        while (it2.hasNext()) {
          Node nnbor = (Node) it2.next();
          Integer nnborid = new Integer(nnbor.getId());
          if (nnborid.intValue() > nid.intValue() && nodes.contains(nnborid)) {
            // look for this arc in the Graph g
            Object l1 = _nodeLabels[nid.intValue()];
            Object l2 = _nodeLabels[nnborid.intValue()];
            try {
              int othernid = g.getNodeIdByLabel(l1);
              int othernnborid = g.getNodeIdByLabel(l2);
              Node othernode = g.getNode(othernid);
              Node othernnbor = g.getNode(othernnborid);
              if (othernode.getNbors().contains(othernnbor))
                res += 1;
            }
            catch (GraphException e) {
              // silent continue
            }
          }
        }
      }
      double narcs = nodes.size() * (nodes.size() - 1) / 2.0;
      return (res / narcs);
    }
    finally {
      try {
        if (this_is_first) {
          if (g_locked) g.releaseReadAccess();
          if (this_locked) releaseReadAccess();
        }
        else {
          if (this_locked) releaseReadAccess();
          if (g_locked) g.releaseReadAccess();
        }
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


  /**
   * computes *all* maximal subsets of arcs in this Graph with the property
   * that any two arcs in a set s that is returned are at a distance one from
   * each other. (For k=1 this should be fast enough method)
   * @param k int the max. number of hops away any two arcs are allowed to be.
   * @throws GraphException
   * @return Set // Set&lt;Set&lt;Integer linkid&gt; &gt;
   */
  public Set getAllConnectedLinks(int k) throws GraphException {
    try {
      getReadAccess();
      if (k != 1)throw new GraphException("currently, input k must be 1");
      Vector sets = new Vector();
      for (int i = 0; i < _arcs.length; i++) {
        Set conn1links = getFullLinkNbors(i);
        sets.addElement(conn1links);
      }
      // remove duplicates and subsets from sets
      int setsize = sets.size();
      boolean todelete[] = new boolean[setsize];
      for (int i = 0; i < setsize; i++) todelete[i] = false;
      for (int i = setsize - 1; i >= 0; i--) {
        Set si = (Set) sets.elementAt(i);
        for (int j = i - 1; j >= 0; j--) {
          Set sj = (Set) sets.elementAt(j);
          if (si.containsAll(sj)) todelete[j] = true;
          if (sj.containsAll(si) && todelete[j] == false) {
            todelete[i] = true;
            break; // i is set for deletion, so go on
          }
        }
      }
      for (int i = setsize - 1; i >= 0; i--) {
        if (todelete[i]) sets.remove(i);
      }
      Set result = new HashSet(sets);
      return result;
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


  /**
   * return a Set&lt;Set&lt;Integer nodeId&gt; &gt; that is the set of all sets of
	 * nodeids in the result set that have the property that are maximal sets of
	 * nodes that are connected with each other with at most 1 hop.
   * @param k int
   * @throws GraphException
   * @return Set // Set&lt;Set&lt;Integer nodeId&gt; &gt;
   */
  public Set getAllConnectedNodes(int k) throws GraphException {
    if (k!=2) throw new GraphException("currently, input k must be 2");
    try {
      getReadAccess();
      Set t = new HashSet(); // Set<Set<Integer nodeid> >
      for (int i = 0; i < _nodes.length; i++) {
        Node v = getNode(i);
        // get the set Dv
        Set dv = v.getNNborIndices();
        dv.add(new Integer(i)); // not needed, as i is already in dv
        Vector l = new Vector(); // Set<Set<Integer nodeid> >
        Set generated = new HashSet(); // Set<IntSet s>
        l.add(dv);
        for (int j = 0; j < l.size(); j++) {
          Set s = (Set) l.elementAt(j);
          l.remove(j);
          if (isConnected1(s)) {
            t.add(s);
            continue;
          }
          j--; // go back one step
          Iterator siter = s.iterator();
          while (siter.hasNext()) {
            Integer u = (Integer) siter.next();
            if (u.intValue() == i)continue; // don't consider the current node i
            Set sp = new HashSet(s);
            sp.remove(u);
            IntSet isp = new IntSet(sp);
            if (generated.contains(isp) == false) {
              generated.add(isp);
              l.addElement(sp);
            }
          }
        }
      }
      Vector sets = new Vector();
      Iterator iter = t.iterator();
      while (iter.hasNext()) {
        sets.add(iter.next());
      }
      // remove duplicates and subsets from sets
      int setsize = sets.size();
      boolean todelete[] = new boolean[setsize];
      for (int i = 0; i < setsize; i++) todelete[i] = false;
      for (int i = setsize - 1; i >= 0; i--) {
        Set si = (Set) sets.elementAt(i);
        for (int j = i - 1; j >= 0; j--) {
          Set sj = (Set) sets.elementAt(j);
          if (si.containsAll(sj)) todelete[j] = true;
          if (sj.containsAll(si) && todelete[j] == false) {
            todelete[i] = true;
            break; // i is set for deletion, so go on
          }
        }
      }
      for (int i = setsize - 1; i >= 0; i--) {
        if (todelete[i]) sets.remove(i);
      }
      Set result = new HashSet(sets);
      return result;
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


  /**
   * return the Set&lt;IntSet nodeids&gt; of all sets of nodeids with the property
   * that they are independent (maximal) and at distance at most 2 from each
   * other within the set they are in.
   * @param maxcount Integer if not null denotes the max number of 5-cycle based
   * sets of nodes to return
   * @return Set // Set&lt;IntSet nodeids&gt;
   * @throws GraphException
   */
  public Set getAllConnectedBy1Nodes(Integer maxcount) throws GraphException {
    try {
      getReadAccess();
      Set t = new TreeSet();
      for (int i = 0; i < _nodes.length; i++) {
        // 1. create an IntSet and put in the i-th node and its neihgbors
        IntSet ti = new IntSet();
        ti.add(new Integer(i));
        Node ni = _nodes[i];
        Set inbors = ni.getNborIndices(Double.NEGATIVE_INFINITY);
        ti.addAll(inbors);
        // 2. add any neighbor of the neighbors that is connected to all the
        // i-th node's neighbors
        if (inbors.size() > 0) {
          Iterator inborit = inbors.iterator();
          Set toadd = new HashSet();
          for (int ii = 0; ii < _nodes.length; ii++) toadd.add(new Integer(ii));
          while (inborit.hasNext()) {
            Integer nnid = (Integer) inborit.next();
            Set nnborids = _nodes[nnid.intValue()].getNborIndices(Double.NEGATIVE_INFINITY);
            toadd.retainAll(nnborids);
          }
          ti.addAll(toadd);
        }
        t.add(ti);
      }
      Set t2 = new HashSet();
      // 3. add all 5-cycle-based sets: sets of the form {n1...n5} U N2
      // that are connected like (n1,n2),(n2,n3),(n3,n4),(n4,n5),(n5,n1) and each
      // other node in N2 has two neighbors that are in {n1...n5} that are at
      // distance 2 from each other (i.e. are not themselves neighbors)
      for (int i = 0; i < _arcs.length; i++) {
        Link li = _arcs[i];
        Set ti2 = get5CycleBasedConnectedNodes(li, maxcount); // Set<IntSet nodeids>
        t2.addAll(ti2);
      }
      // 4. finally, remove subsets
      Vector sets = new Vector();
      Iterator iter = t.iterator();
      while (iter.hasNext()) {
        sets.add(iter.next());
      }
      iter = t2.iterator();
      // remove duplicates and subsets from sets
      int setsize = sets.size();
      boolean todelete[] = new boolean[setsize];
      for (int i = 0; i < setsize; i++) todelete[i] = false;
      for (int i = setsize - 1; i >= 0; i--) {
        Set si = (Set) sets.elementAt(i);
        for (int j = i - 1; j >= 0; j--) {
          Set sj = (Set) sets.elementAt(j);
          if (si.containsAll(sj)) todelete[j] = true;
          if (sj.containsAll(si) && todelete[j] == false) {
            todelete[i] = true;
            break; // i is set for deletion, so go on
          }
        }
      }
      for (int i = setsize - 1; i >= 0; i--) {
        if (todelete[i]) sets.remove(i);
      }
      Set result = new HashSet(sets);
      return result;
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


  /**
   * return a Set containing a Set&lt;Integer nodeid&gt; for each node i in this
	 * Graph where each Set will comprise the immediate neighbors of node i, plus
	 * the node i itself (its id)
   * @return Set // Set&lt;Set&lt;Integer nodeid&gt; &gt;
   */
  public Set getAllNborSets() {
    try {
      getReadAccess();
      Set nborids = new HashSet(); // Set<Set<Integer nodeid> >
      for (int i = 0; i < _nodes.length; i++) {
        Set inborids = _nodes[i].getNborIndices(Double.NEGATIVE_INFINITY);
        inborids.add(new Integer(i)); // add self
        if (inborids != null && inborids.size() > 0) // should better be size() > 1
          nborids.add(inborids);
      }
      return nborids;
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


  /**
   * return all node ids of the nodes that are immediate neighbors of any of the
   * nodes whose ids are in the input set argument, excluding the nodes in the
   * input argument set.
   * @param nodeids Set // Set&lt;Integer nodeid&gt;
   * @throws GraphException if nodeids is null or empty
   * @return Set // Set&lt;Integer nodeid&gt;
   */
  public Set getNborIds(Set nodeids) throws GraphException {
    try {
      getReadAccess();
      if (nodeids == null || nodeids.size() == 0)
        throw new GraphException("empty parameter");
      Set nborids = new HashSet();
      Iterator it = nodeids.iterator();
      while (it.hasNext()) {
        Integer nid = (Integer) it.next();
        Node ni = _nodes[nid.intValue()];
        Set nibors = ni.getNborIndices(Double.NEGATIVE_INFINITY);
        nborids.addAll(nibors);
      }
      nborids.removeAll(nodeids);
      return nborids;
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


  /**
   * create a cache of the 2-distance neighbors of each node for faster access.
   * @throws ParallelException
   */
  public void makeNNbors() throws ParallelException {
      makeNNbors(false);
  }


  /**
   * forces the recomputation of the cache holding the 2-distance neighbors of
   * a node if the argument is true. Otherwise, the computation occurs only
   * if the cache is empty.
   * @param force boolean
   * @throws ParallelException
   */
  public void makeNNbors(boolean force) throws ParallelException {
    try {
      getWriteAccess();
      for (int i = 0; i < _nodes.length; i++) {
        _nodes[i].getNNbors(force); // force computation of all NNbors and storage in _nnbors cache
      }
    }
    finally {
      try {
        releaseWriteAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
    }
  }


  /**
   * forces the recomputation of the cache holding the 1-distance neighbors of
   * a node if the argument is true. Otherwise, the computation occurs only
   * if the cache is empty.
   * @param force boolean
   * @throws ParallelException
   */
  public void makeNbors(boolean force) throws ParallelException {
    try {
      getWriteAccess();
      for (int i = 0; i < _nodes.length; i++) {
        _nodes[i].getNbors(force); // force computation of all Nbors and storage in _nbors cache
      }
    }
    finally {
      try {
        releaseWriteAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
    }
  }


	/**
	 * return the max node weight value of the property with given name. When
	 * nodes set their weights, an appropriate cache is updated and is returned
	 * in this call via a simple hash-table look-up. Therefore it is a constant
	 * time operation.
	 * @param name String
	 * @return Double null if the given property name does not have a value for
	 * any node in this graph
	 */
	public Double getMaxNodeWeight(String name) {
    try {
      getReadAccess();
			return (Double) _maxNodeWeights.get(name);
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
    }
	}


	/**
	 * return the max node weight value of the property with given name.
	 * This operation is unsynchronized, and therefore, if used in a multi-
	 * threaded context, should be externally synchronized or otherwise provide
	 * guarantees that prevent race-conditions from occurring. Otherwise, it is
	 * identical to the <CODE>getMaxNodeWeight(name)</CODE> method.
	 * @param name String
	 * @return Double null if the given property name does not have a value for
	 * any node in this graph
	 */
	public Double getMaxNodeWeightUnsynchronized(String name) {
		return (Double) _maxNodeWeights.get(name);
	}


	/**
	 * return the max node weight value of the property with given name among all
	 * nodes in this graph EXCEPT those in the forbiddenNodes set. The operation
	 * runs in expected constant time in the size of the forbidden set, and linear
	 * time in the size of the nodes of the graph.
	 * @param name String
	 * @param forbiddenNodes Set  // Set&lt;Node&gt;
	 * @return Double or null if there is no node not in forbiddenNodes
	 */
	public Double getMaxNodeWeight(String name, Set forbiddenNodes) {
		if (forbiddenNodes==null || forbiddenNodes.size()==0)
			return getMaxNodeWeight(name);
		// do the work
		try {
			getReadAccess();
			Node[] arr = getNodesSortedDescByWeight(name);
			for (int i=0; i<arr.length; i++) {
				Node ai = arr[i];
				if (!forbiddenNodes.contains(ai))
					return ai.getWeightValueUnsynchronized(name);
			}
			return null;  // no free node available
		}
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
    }
	}


  /**
   * return a "printout" of this Graph.
   * @return String
   */
  public String toString() {
    try {
      getReadAccess();
      String ret = "#nodes=" + _nodes.length + " #arcs=" + _arcs.length;
      for (int i = 0; i < _arcs.length; i++)
        ret += " [" + _arcs[i].getStart() + "," + _arcs[i].getEnd() + "(" +
            _arcs[i].getWeight() + ")]";
      return ret;
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


  /**
   *
   * @param clique Set // Set&lt;Integer nodeid&gt;
   * @param minval double
   * @return Vector // Vector&lt;Integer nodeid&gt;
   */
  Vector getFullNbors(Set clique, double minval) {
    try {
      getReadAccess();
      Vector res = new Vector();
      Set nbors = new HashSet();
      for (int i = 0; i < _nodes.length; i++) {
        Integer ii = new Integer(i);
        if (clique.contains(ii) == false)
          nbors.add(ii); // don't add the members of the clique
      }
      Iterator iter = clique.iterator();
      while (iter.hasNext()) {
        Integer nid = (Integer) iter.next();
        Set nborsi = getNode(nid.intValue()).getNborIndices(minval); // Set<Integer nid>
        // remove from nbors the elements not in nborsi
        nbors.retainAll(nborsi);
      }
      // return a Vector
      res.addAll(nbors);
      return res;
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


	/**
	 * updates the max node weight for the given weight name. Only called from
	 * <CODE>Node.setWeight(String, double)</CODE> and
	 * <CODE>Node.copyWeightsFrom(Node)</CODE>.
	 * @param name String
	 * @param val double
	 */
	void updateMaxNodeWeight(String name, double val) {
		Double cur_mnw = (Double) _maxNodeWeights.get(name);
		if (cur_mnw==null || cur_mnw.doubleValue()<val) {
			_maxNodeWeights.put(name, new Double(val));
		}
		// finally, reset _sortedNodeArrays field
		_sortedNodeArrays.remove(name);
	}


	/**
	 * only called from <CODE>getMaxNodeWeight(name,forbiddennodes)</CODE>, so has
	 * already read-lock of this graph.
	 * @param wname String
	 * @return Node[] sorted descending by weight value of the property wname
	 */
	private Node[] getNodesSortedDescByWeight(String wname) {
		try {
			Node[] arr = (Node[]) _sortedNodeArrays.get(wname);
			if (arr==null) {
				releaseReadAccess();
				getWriteAccess();
				arr = (Node[]) _sortedNodeArrays.get(wname);
				if (arr==null) {  // Double-Check Locking style but correct
					// do the work
					arr = new Node[_nodes.length];
					for (int i=0; i<arr.length; i++) arr[i] = _nodes[i];
					Arrays.sort(arr, new NodeWeightComparatorUnsynchronized(wname));
					_sortedNodeArrays.put(wname, arr);
				}
				getReadAccess();
				releaseWriteAccess();
			}
			return arr;
		}
		catch (ParallelException e) {  // cannot happen
			e.printStackTrace();
			throw new Error("unexpected parallel exception in getNodesSortedDescByWeight("+wname+")");
		}
	}


  /**
   * labels the node with given id and all other nodes reachable by it, with the
	 * "label" given by the 2nd argument. Updates the <CODE>_comps</CODE> data
	 * member.
   * @param nid int represents a Node id
   * @param c int the component to which the node with id nid plus all nodes
	 * reachable by it belong to (where reachability is defined without respect
	 * for the edges' direction)
   * @return int number of nodes labeled
   * @throws ParallelException
   */
  private int labelComponent(int nid, int c) throws ParallelException {
    try {
      getWriteAccess();
      int res = 1;
      _comps[nid] = c;
			BoundedBufferArrayUnsynchronized bbau =
				new BoundedBufferArrayUnsynchronized(_nodes.length);
			/* below method is slow -about 10 times slower than method further below!
      Set nbors = new HashSet(_nodes[nid].getNbors());
      while (nbors.size() > 0) {
        Iterator it = nbors.iterator();
        Node n = (Node) it.next();
        nbors.remove(n);
        _comps[n.getId()] = c; // label node
        res++;
        // add non-labeled nbors to nbors
        Set nnbors = n.getNbors();
        it = nnbors.iterator();
        while (it.hasNext()) {
          Node nn = (Node) it.next();
          if (_comps[nn.getId()] == -1) nbors.add(nn);
        }
      }
			*/
			bbau.addElement(_nodes[nid]);
			while (bbau.size()>0) {
				Node n = (Node) bbau.remove();
				Set nbors = n.getNbors();
				Iterator it = nbors.iterator();
				while (it.hasNext()) {
					Node nn = (Node) it.next();
					if (_comps[nn.getId()] == -1) {
						_comps[nn.getId()] = c;  // label node
						++res;
						bbau.addElement(nn);
					}
				}
			}
      return res;
    }
    finally {
      try {
        releaseWriteAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
    }
  }


  /**
   *
   * @param lid int
   * @return Set // Set&lt;Integer linkid&gt;
   */
  private Set getFullLinkNbors(int lid) {
    try {
      getReadAccess();
      Set result = new TreeSet(); // Set<Integer linkid>
      result.add(new Integer(lid));
      Link l = getLink(lid);
      int s = l.getStart();
      Node sn = getNode(s);
      result.addAll(sn.getInLinks());
      result.addAll(sn.getOutLinks());
      int e = l.getEnd();
      Node en = getNode(e);
      result.addAll(en.getInLinks());
      result.addAll(en.getOutLinks());
      // also, add all nets from emanating from a neighbor of sn and ending on a
      // neighbor of en
      Set snnbors = sn.getNbors();
      Iterator iter = snnbors.iterator();
      while (iter.hasNext()) {
        Node snn = (Node) iter.next();
        Set snninlinks = snn.getInLinks();
        Iterator i2 = snninlinks.iterator();
        while (i2.hasNext()) {
          Integer inlid = (Integer) i2.next();
          Link inlink = getLink(inlid.intValue());
          Integer sid = new Integer(inlink.getStart());
          if (en.getNborIndices(Double.NEGATIVE_INFINITY).contains(sid))  // value used to be 0
            result.add(inlid);
        }
        Set snnoutlinks = snn.getOutLinks();
        Iterator i3 = snnoutlinks.iterator();
        while (i3.hasNext()) {
          Integer outlid = (Integer) i3.next();
          Link outlink = getLink(outlid.intValue());
          Integer eid = new Integer(outlink.getEnd());
          if (en.getNborIndices(Double.NEGATIVE_INFINITY).contains(eid))  // value used to be 0
            result.add(outlid);
        }
      }
      return result;
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


  /**
   * returns true iff each node w/ id in the arg. set, can reach any
   * other node whose id is in the arg set by using at most 1 hop that must also
   * be inside the arg. set.
   * @param nodeids Set // Set&lt;Integer nodeid&gt;
   * @throws GraphException if arg set is empty or null
   * @return boolean
   */
  private boolean isConnected1(Set nodeids) throws GraphException {
    if (nodeids==null || nodeids.size()==0)
      throw new GraphException("empty nodeids");
    Object[] array = nodeids.toArray();
    int len = array.length;
    try {
      getReadAccess();
      for (int i = 0; i < len - 1; i++) {
        Integer nid = (Integer) array[i];
        Node ni = getNode(nid.intValue());
        Set nibors = ni.getNbors();
        for (int j = i + 1; j < len; j++) {
          Integer njd = (Integer) array[j];
          Node nj = getNode(njd.intValue());
          if (nibors.contains(nj))continue;
          Set njbors_copy = new HashSet(nj.getNbors());
          njbors_copy.retainAll(nibors);
          if (njbors_copy.size() == 0) {
            return false;
          }
          // njbors_copy must have one in nodeids
          Iterator itj = njbors_copy.iterator();
          boolean cont = false;
          while (itj.hasNext()) {
            Node nnj = (Node) itj.next();
            if (nodeids.contains(new Integer(nnj.getId()))) {
              cont = true;
              break;
            }
          }
          if (!cont)return false;
        }
      }
      return true;
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


  /**
   *
   * @param li Link
   * @param maxsetcount Integer
   * @throws GraphException
   * @return Set // Set&lt;Set&lt;Integer nodeid&gt; &gt;
   */
  private Set get5CycleBasedConnectedNodes(Link li, Integer maxsetcount)
      throws GraphException {
    try {
      getReadAccess();
      int maxcount = Integer.MAX_VALUE;
      if (maxsetcount != null) maxcount = maxsetcount.intValue();
      Set t = new HashSet();
      Node n1 = _nodes[li.getStart()];
      Node n2 = _nodes[li.getEnd()];
      Set n1_nbors = n1.getNbors();
      Set n2_nbors = n2.getNbors();
      Iterator n1_iter = n1_nbors.iterator();
      int count = 0;
      while (n1_iter.hasNext()) {
        Node n5 = (Node) n1_iter.next();
        if (n5.getId() == n2.getId())continue;
        Set n5_nborids = n5.getNborIndices(Double.NEGATIVE_INFINITY);
        Iterator n2_iter = n2_nbors.iterator();
        while (n2_iter.hasNext()) {
          Node n3 = (Node) n2_iter.next();
          if (n3.getId() == n1.getId())continue; // nodes must be different
          Set n3_nborids = n3.getNborIndices(Double.NEGATIVE_INFINITY);
          boolean add1235 = false;
          if (n3.getNbors().contains(n5)) add1235 = true;
          n3_nborids.retainAll(n5_nborids);
          // each node id in n3_nborids is a valid n4
          Iterator n4_iter = n3_nborids.iterator();
          boolean added12345 = false;
          while (n4_iter.hasNext()) {
            Integer n4id = (Integer) n4_iter.next();
            int n4idi = n4id.intValue();
            if (n4idi == n1.getId() || n4idi == n2.getId())
              continue;
            /*  condition below is never true
                       if (n4idi==n5.getId()) {
              add1235 = true;
              continue;
                       }
             */
            // Node n4 = _nodes[n4id.intValue()];
            Set c5 = new IntSet();
            c5.add(new Integer(n1.getId()));
            c5.add(new Integer(n2.getId()));
            c5.add(new Integer(n3.getId()));
            c5.add(n4id);
            c5.add(new Integer(n5.getId()));
            // now add also any node having two nbors in the cycle that are not
            // themselves neighbors
            Set c5nborIds = getNborIds(c5);
            Iterator c5it = c5nborIds.iterator();
            boolean added = false;
            while (c5it.hasNext()) {
              Integer c5n = (Integer) c5it.next();
              Set c5test = new IntSet(c5);
              c5test.add(c5n);
              if (isConnected1(c5test)) {
                added = true;
                t.add(c5test);
                if (++count >= maxcount)return t;
              }
            }
            if (!added) {
              t.add(c5);
              if (++count >= maxcount)return t;
            }
            added12345 = true;
          }
          if (!added12345 && add1235) {
            // create the 1,2,3,5 set and add it in
            Set c4 = new IntSet();
            c4.add(new Integer(n1.getId()));
            c4.add(new Integer(n2.getId()));
            c4.add(new Integer(n3.getId()));
            c4.add(new Integer(n5.getId()));
            t.add(c4);
            if (++count >= maxcount)return t;
          }
        }
      }
      return t;
    }
    finally {
      try {
        releaseReadAccess();
      }
      catch (ParallelException e) {
        // can never get here
        e.printStackTrace();
      }
    }
  }


  /**
   * was used for debugging purposes
   * @param clique Collection  // Collection&lt;Integer nid&gt;
   */
  private void print(Collection clique) {
    System.out.print("[");
    Iterator it = clique.iterator();
    while (it.hasNext()) {
      Integer i = (Integer) it.next();
      System.out.print(i+" ");
    }
    System.out.print("]");
  }


	/**
	 * auxiliary nested inner class used to sort the graph's nodes.
	 */
	class NodeWeightComparator implements Comparator {
		private String _name;

		NodeWeightComparator(String name) {
			_name = name;
		}
		public int compare(Object o1, Object o2) {
			Node n1 = (Node) o1;
			Node n2 = (Node) o2;
			Double w1 = n1.getWeightValue(_name);
			if (w1==null) w1 = new Double(1.0);
			Double w2 = n2.getWeightValue(_name);
			if (w2==null) w2 = new Double(1.0);
			return Double.compare(w2.doubleValue(), w1.doubleValue());
		}
	}


	/**
	 * faster auxiliary nested inner class used to sort the graph's nodes.
	 */
	class NodeWeightComparatorUnsynchronized implements Comparator {
		private String _name;

		NodeWeightComparatorUnsynchronized(String name) {
			_name = name;
		}
		public int compare(Object o1, Object o2) {
			Node n1 = (Node) o1;
			Node n2 = (Node) o2;
			Double w1 = n1.getWeightValueUnsynchronized(_name);
			if (w1==null) w1 = new Double(1.0);
			Double w2 = n2.getWeightValueUnsynchronized(_name);
			if (w2==null) w2 = new Double(1.0);
			return Double.compare(w2.doubleValue(), w1.doubleValue());
		}
	}
}

