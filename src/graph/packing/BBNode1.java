package graph.packing;

import graph.*;
import parallel.*;
import utils.*;
import popt4jlib.*;
import popt4jlib.LocalSearch.*;
import java.util.*;

/**
 * represents a node in the B &amp; B tree of the hybrid B &amp; B - GASP scheme 
 * for the 1-packing problem (max weighted independent set problem). It extends
 * <CODE>BBNodeBase</CODE>, as <CODE>BBNode1</CODE> objects enter a priority
 * queue (<CODE>BBQueue</CODE>) from which nodes are selected for processing
 * (passing them to the <CODE>BBThreadPool</CODE> that is a proxy for a
 * <CODE>parallel.FasterParallelAsynchBatchTaskExecutor</CODE> executor).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class BBNode1 extends BBNodeBase {
	/**
	 * defines a multiplicative fudge factor by which the "best cost" of
	 * graph-nodes is allowed to be "over" so as to still be considered "best" for
	 * inclusion in the <CODE>getBestNodes2Add(BBNode1)</CODE> method and further
	 * consideration. This value is allowed to change until a call to
	 * <CODE>disallowFFChanges()</CODE> is made by any thread.
	 */
	private static double _ff = 0.85;
	/**
	 * guard member to ensure the value of _ff doesn't change after BBNode1
	 * objects are created.
	 */
	private static boolean _ffAllowed2Change = true;
	/**
	 * comparator between node-sets used in sorting node-sets in the
	 * <CODE>getBestNodeSets2Add()</CODE> method if the appropriate option in the
	 * parameters of the calling program is set. This sorting only makes sense
	 * when children BBNode1 objects will be "cut" short due to flags set to guide
	 * the search process.
	 */
	private static NodeSetWeightComparator _nscomtor = new NodeSetWeightComparator();


  /**
   * Sole constructor of a BBNode1 object. Clients are not expected to
   * create such objects which are instead created dynamically through the
   * B&amp;B process.
   * @param master BBTree the master BBTree object of which this is a node.
   * @param r Set // Set&lt;Node&gt; the set of (graph) nodes to be added to the
	 * nodes of the parent to represent a new partial solution.
   * @param parent BBNode1 the parent BB-node in the B&amp;B tree construction 
	 * process.
   * @throws PackingException if the second argument is non-null but the third
   * argument is null.
   */
  BBNode1(BBTree master, Set r, BBNode1 parent)
      throws PackingException {
		super(master, r, parent);
		// System.err.println("BBNode1() with set r.size()="+(r==null ? "null" : r.size())+" constructed.");
  }


  /**
   * the main method of the class, that processes the partial solution
   * represented by this object, and spawning and pushing into the
   * <CODE>BBQueue</CODE> queue new BB-node children if needed.
	 * Note:
	 * <br>2015-02-09 modified children nodes cutting-off behavior to be turned
	 * off if this BBNode is the root node, in accordance with the change to
	 * let the root node return immediately all possible best graph-nodes as
	 * independent starting singleton sets (equivalent to having kmax=0 for the
	 * root node) which should result in better parallel processing throughput.
	 * <br>2015-02-10 improved performance of the isFeas() and isFree2Cover()
	 * methods - now also avoid unnecessary HashMap objects creation.
   */
  public void run() {
    try {
      boolean foundincumbent = false;
			int _id = getId();
			BBTree _master = getMaster();
			Set _nodes = getNodes();
      if (_id % 100 == 0) {
        System.err.println("running node id=" + _id + " init. weight=" +
                           getCost() + " lvl="
                           + getLevel() + " bnd=" + getBound() + " qsize=" +
                           _master.getQueue().size()); // itc: HERE rm asap
				System.err.flush();
      }
      // step 1.
      // see if limit has been reached
      if (_master.getCounter() > _master.getMaxNodesAllowed()) {
        setDone();
        return; // stop computations
      }
      // step 2.
      // check for pruning
      double bnd = getBound();
      if (bnd <= _master.getBound() || bnd < _master.getMinKnownBound()) {
        // if (getBestNodeSets2Add().size()==0) _master.incrementTotLeafNodes();
				setDone();
        return; // node is fathomed
      }
      // step 3.
      // add as many nodes as possible in GASP fashion
      Set candidates = null; // Set<Set<Node> >
      while (true) {
        candidates = getBestNodeSets2Add();
        if (candidates != null && candidates.size() == 1) {
          _nodes.addAll( (Set) candidates.iterator().next());
        }
        else break;
      }
			// step 3.5
			// check if node is now leaf
			if (candidates==null || candidates.size()==0) {
				// System.err.println("found leaf node");
				_master.incrementTotLeafNodes();
			}
      // step 4.
      // check for incumbent
      if (getCost() >= _master.getBound()*_master.getLocalSearchExpandFactor()) {
        // itc 2015-02-26: inequality used to be strict (>)
				// itc 2015-03-20: added local-search expansion factor multiplication to
				// broaden cases where local-search kicks in.
        if (getCost()>_master.getBound()) _master.setIncumbent(this);
        foundincumbent = true;
      }
      // step 5.
      // now that _nodes won't be further altered check if node exists in _recent
      if (_master.getQueue().addInRecent(this) == false) {
        setDone();
        return; // node is fathomed
      }
      // branch?
      if (candidates != null && candidates.size()!=0) {
        try {
          List children = new ArrayList();
          Iterator it = candidates.iterator();
          int cnt_children = 0;
          while (it.hasNext()) {
            if (cnt_children++ > _master.getMaxChildrenNodesAllowed() &&
								getParent()!=null)  // cutting nodes is not allowed for the root
							break;
            Set ns = (Set) it.next();
            BBNode1 child = new BBNode1(_master, ns, this);
            // check if child's bound is better than incumbent
            double childbound = child.getBound();
            if (childbound <= _master.getBound() ||
								childbound < _master.getMinKnownBound())  // we know child cannot make it
              continue;
            // speed up processing:
            // record new child incumbent if it exists (may be a partial soln
            // that can be further augmented in step 3 above when it is processed)
            if (child.getCost() > _master.getBound()) {
							_master.setIncumbent(child);
              foundincumbent = true;  // is it needed here?
            }
            children.add(child);
          }
          setNumChildren(children.size());
          int sz = children.size();
          if (sz == 0) {
            if (foundincumbent) _master.reduceTightenUpperBoundLvl();
            setDone();
            return; // no children
          }
          boolean added_nodes = _master.getQueue().insertNodes(children);
          if (added_nodes == false) { // BBQueue grew too big
            if (_master.getCutNodes() == false) {
              for (int i = 0; i < sz; i++) {
                BBNode1 child = (BBNode1) children.get(i);
                // BBThreadPool.getPool().execute(child);
                child.run(); // run on current thread
              }
              return;
            }
            else {
              setDone();
              return; // no children
            }
          }
        }
        catch (Exception e) {
          e.printStackTrace();
          //System.exit( -1);
        }
      }
      else {
        if (foundincumbent) {
          _master.reduceTightenUpperBoundLvl();
          if (_master.getLocalSearch()) {  // perform a local search
						long start_time = System.currentTimeMillis();
            try {
              // convert s to Set<Integer>
              Set nodeids = new IntSet();
              Iterator iter = _nodes.iterator();
              while (iter.hasNext()) {
                Node n = (Node) iter.next();
                Integer nid = new Integer(n.getId());
                nodeids.add(nid);
              }
							_master.incrNumDLSPerformed();
              // now do the local search
              DLS dls = new DLS();
              AllChromosomeMakerIntf movesmaker = _master.getNewLocalSearchMovesMaker();
							if (movesmaker==null)  // use default
								movesmaker = new IntSetN1RXPFirstImprovingGraphAllMovesMakerMT(1);
              // AllChromosomeMakerIntf movesmaker = new
              //    IntSetN2RXPGraphAllMovesMaker(1);
							// IntSetN2RXPGraphAllMovesMaker(1) gives better results on G_{|V|,p} random graphs
              IntSetNeighborhoodFilterIntf filter = new
                  GRASPPackerIntSetNbrhoodFilter3(1,_master.getGraph());
              FunctionIntf f = new SetWeightEvalFunction(_master.getGraph());
              HashMap dlsparams = new HashMap();
              dlsparams.put("dls.movesmaker", movesmaker);
              dlsparams.put("dls.x0", nodeids);
              dlsparams.put("dls.numthreads", new Integer(10));  // itc: HERE parameterize asap
              dlsparams.put("dls.maxiters", new Integer(100)); // itc: HERE rm asap
              int n10 = _master.getGraph().getNumNodes()/10 + 1;
              dlsparams.put("dls.intsetneighborhoodmaxnodestotry", new Integer(n10));
              dlsparams.put("dls.graph", _master.getGraph());
							dlsparams.put("dls.lock_graph", Boolean.FALSE);
              dlsparams.put("dls.intsetneighborhoodfilter", filter);
              //dlsparams.put("dls.createsetsperlevellimit", new Integer(100));
              dls.setParams(dlsparams);
              PairObjDouble pod = dls.minimize(f);
              Set sn = (Set) pod.getArg();
              if (sn != null && -pod.getDouble() > getCost()) {
                _nodes.clear();
                Iterator sniter = sn.iterator();
                while (sniter.hasNext()) {
                  Integer id = (Integer) sniter.next();
                  Node n = _master.getGraph().getNodeUnsynchronized(id.intValue());
                  _nodes.add(n);
                }
                // record new incumbent
                _master.setIncumbent(this);
								_master.incrementTotLeafNodes();
              }
							long dur = System.currentTimeMillis()-start_time;
							_master.incrTimeSpentOnDLS(dur);
            }
            catch (Exception e2) {
              e2.printStackTrace();
            }
          }  // end local search
        }  // if foundincumbent
        // done
        setDone();
        return;
      }
    }
    catch (ParallelException e) {
      e.printStackTrace();
    }
  }


  /*
  public int compareTo(Object other) {
    BBNode1 o = (BBNode1) other;
    double ct = getBound();
    double oct = o.getBound();
    if (ct > oct) return -1;
    else if (ct == oct) {
      if (_id < o._id)return -1;
      else if (_id == o._id)return 0;
      else return 1;
    }
    else return 1;
  }
  */


 /**
  * compares this BB-node with another BB-node, via the master BBTree's
  * BBNodeComparator object for comparing BB-nodes.
  * @param other Object
  * @return int
  */
 public int compareTo(Object other) {
    BBNode1 o = (BBNode1) other;
    /*
    if (_id < o._id) return -1;
    else if (_id == o._id) return 0;
    else return 1;
    */

    /*
    double sct = _nodes.size();
    double osct = o._nodes.size();
    double bd = getBound();
    double obd = o.getBound();
    double ct = bd/sct + sct - 1.0;
    double oct = obd/osct + osct - 1.0;
    if (ct > oct) return -1;
    else if (ct == oct) {
      Set to =  o._nodes;
      Iterator it = _nodes.iterator();
      Iterator oit = to.iterator();
      while (it.hasNext()) {
        Node mi = (Node) it.next();
        if (oit.hasNext()) {
          Node oi = (Node) oit.next();
          if (mi.getId()<oi.getId()) return -1;
          else if (mi.getId()>oi.getId()) return 1;
        }
        else return 1;
      }
      if (oit.hasNext()) return -1;
      else return 0;
    }
    else return 1;
    */
    return getMaster().getBBNodeComparator().compare(this, o);
  }


  /**
   * compares for equality using the object's <CODE>compareTo()</CODE> method.
   * @param other Object expected another BBNode1 object
   * @return boolean return true iff the compareTo(other) method return 0.
   */
  public boolean equals(Object other) {
    /*
    BBNode1 o = (BBNode1) other;
    if (_id==o._id) return true;
    else return false;
    */
    int ct = compareTo(other);
    return (ct==0);
  }


  /**
   * returns the size of the nodes of this BB-node.
   * @return int
   */
  public int hashCode() {
    // return _id;
    return getNodes().size();
  }


	/**
	 * return the sum of weights of the active nodes.
	 * @return double
	 */
	double getCost() {
		double res = 0.0;
		Iterator it = getNodes().iterator();
		while (it.hasNext()) {
			Node ni = (Node) it.next();
			Double niwD = ni.getWeightValueUnsynchronized("value");  // used to be ni.getWeightValue("value");
			double niw = niwD==null ? 1.0 : niwD.doubleValue();
			res += niw;
		}
		return res;
	}


	/**
	 * return all immediate nbors of this solution's nodes, plus the solution's
	 * nodes themselves.
	 * @return Set // Set&lt;Node&gt;
	 */
  private Set getForbiddenNodes() {
    Set forbidden = new HashSet(getNodes());
    Iterator it = getNodes().iterator();
    while (it.hasNext()) {
      Node n = (Node) it.next();
      Set nnbors = n.getNborsUnsynchronized();  // used to be synchronized
      forbidden.addAll(nnbors);
    }
    return forbidden;
  }


  /*
   * compute a max. possible number of nodes this soln can have
   * One such bound is _nodes.size() + (gsz - getForbiddenNodes().size())
   * A better bound is _nodes.size() + #unconnectednodes + #(remainingopennodes)/min.{Nbors.size()}
   * @return double
   */
	/*
  private double getBound() {
    if (getLevel()<_master.getTightenUpperBoundLvl()) {
      double res = _nodes.size();
      Set forbidden = getForbiddenNodes();
      res += (_master.getGraphSize()-forbidden.size());
      return res;
    } else {
      double res = _nodes.size();
      final Set forbidden = getForbiddenNodes();
      final int gsz = _master.getGraphSize();
      final Graph g = _master.getGraph();
      int minsz = Integer.MAX_VALUE;
      int count = 0;
      int discnt = 0;
      for (int i = 0; i < gsz; i++) {
        Node ni = g.getNode(i);
        if (forbidden.contains(ni))continue;
        // otherwise, we're ok. Figure out 2-degree
        Set nnibors = ni.getNNbors();
        int nborssz = nnibors.size();
        Iterator it = nnibors.iterator();
        while (it.hasNext()) {
          Node nbor = (Node) it.next();
          if (forbidden.contains(nbor)) nborssz--;
        }
        if (nborssz == 0) discnt++;
        else {
          count++;
          if (nborssz < minsz) minsz = nborssz;
        }
      }
      res += discnt;
      res += count / minsz;
      return res;
    }
  }
  */


  /**
   * compute a max possible number of nodes weights this soln can have.
   * One such bound is _nodes.sum_weights + (gsz - getForbiddenNodes().size())*max_open_node_weight/2
   * @return double
   */
  protected double getBound() {
    if (_bound>=0) return _bound;  // cache
		Iterator nodesit = getNodes().iterator();
		double res = 0.0;
		while (nodesit.hasNext()) {
			Node ni = (Node) nodesit.next();
			Double niw = ni.getWeightValueUnsynchronized("value");  // used to be synchronized
			if (niw==null) res += 1.0;  // nodes without weights have weight value 1
			                            // as in the max. independent set problem.
			else res += niw.doubleValue();
		}
    Set forbidden = getForbiddenNodes();
		// due to the lazy-evaluation scheme used for the Graph._sortedNodeArrays
		// data member, it is not safe to call an unsynchronized version of the
		// Graph.getMaxNodeWeight(String, Set) method, unless the thread-safe version
		// of the Double-Check Locking idiom ("Single-Time Locking per thread" idiom)
		// was implemented; for this reason Graph implements no such unsynch. version
		Double max_node_weightD = getMaster().getGraph().getMaxNodeWeight("value",forbidden);
		double mnw = max_node_weightD==null ? 1.0 : max_node_weightD.doubleValue();
    res += (getMaster().getGraphSize()-forbidden.size())*mnw/2.0;  // itc 2015-02-11: added the division by 2
    _bound = res;
    return res;
  }


	/**
	 * disallow changes to <CODE>_ff</CODE>. Called only from the
	 * <CODE>BBGASPAcker</CODE> class.
	 */
	static synchronized void disallowFFChanges() {
		_ffAllowed2Change = false;
	}


	/**
	 * set the value of <CODE>_ff</CODE> field. Called only from the
	 * <CODE>BBGASPPacker</CODE> class, before any BBNode1 objects are constructed
	 * or executed on threads.
	 * @param val double
	 */
	static synchronized void setFF(double val) {
		if (_ffAllowed2Change) _ff = val;
	}


	/**
	 * it is interesting to reason as to why this method is correct in terms of
	 * memory visibility even without any locks/synchronization.
	 * @return the depth of this <CODE>BBNode1</CODE> object in the
	 * <CODE>BBTree</CODE>.
	 */
  final int getLevel() {
    int lvl=0;
    for (BBNode1 p=this; p!=null; p=(BBNode1)p.getParent()) lvl++;
    return lvl;
  }


  /**
   * return Set&lt;Set&lt;Node&gt; &gt; of all maximal nodesets that can be added
	 * together to the current active <CODE>_nodes</CODE> set.
	 * Note:
	 * <br>2014-07-22 modified Vector store to ArrayList store to enhance
	 * multi-threading speed.
	 * <br>2015-02-09 root node returns immediately with collection of singleton
	 * free nodes as sets to enhance multi-threading speed.
   * @return Set // Set&lt;Set&lt;Node&gt; &gt;
   */
  private Set getBestNodeSets2Add() throws ParallelException {
    final int kmax = getMaster().getMaxAllowedItersInGBNS2A();
    final Set ccands = getBestNodes2Add(getParent()==null);
    /* following code compiles under JDK5 but not under JDK 1.4
    Set result = getMaster().getSortBestCandsInGBNS2A() ?
						new TreeSet(_nscomtor) :
						new HashSet();  // Set<Set<Node> >
    */
    Set result;
    if (getMaster().getSortBestCandsInGBNS2A()) result = new TreeSet(_nscomtor);
    else result = new HashSet();  // Set<Set<Node> >

    List store = new ArrayList();
    Stack temp = new Stack();
    Iterator cands_it = ccands.iterator();
    while (cands_it.hasNext()) {
      Set ci = new HashSet();
      Node n = (Node) cands_it.next();
      ci.add(n);
      temp.push(ci);
    }
    if (getMaster().getUseMaxSubsets()==false || getParent()==null) {
			// if root, return collection of each available node as singleton sets
			// this should speed up parallel processing
      // correct GASP behavior
      result.addAll(temp);
      return result;
    }
    // figure out all the maximal subsets of ccands that are not conflicting
    // as it is, this routine does not guarantee that the nodes are being added
    // in a GASP fashion, as when one node of a set ci is added to _nodes, the
    // other nodes in ci may no longer be the "optimal" in GASP sense to add to
    // _nodes.
    int cnt=0;  // this counter is used to stop the max. subsets creation process from going wild
    while (temp.isEmpty()==false) {
      if (++cnt==kmax) break;
      Set t = (Set) temp.pop();
      cands_it = ccands.iterator();
      boolean expanded_t=false;
      while (cands_it.hasNext()) {
        Node n = (Node) cands_it.next();
        if (isFree2Cover(n, t)) {
          Set t2 = new HashSet(t);
          t2.add(n);
          temp.push(t2);
          expanded_t=true;
        }
      }
      if (expanded_t==false) {
        // make sure you don't insert smth that already exists
        boolean iscovered=false;
        for (int i=0; i<store.size() && !iscovered; i++) {
          Set ti = (Set) store.get(i);
          if (ti.containsAll(t)) iscovered=true;
        }
        if (!iscovered) store.add(t);
      }
    }
    if (temp.isEmpty()==false) {  // broke out because of too many combinations
/*
      System.err.println("adding max. subsets from "+temp.size()+" possible sets");
      Iterator it = temp.iterator();
      while (it.hasNext()) {
        Set t = (Set) it.next();
        // make sure you don't insert smth that already exists
        boolean iscovered=false;
        for (int i=0; i<store.size() && !iscovered; i++) {
          Set ti = (Set) store.elementAt(i);
          if (ti.containsAll(t)) iscovered=true;
        }
        if (!iscovered) store.addElement(t);
      }
 */
      boolean cons=true;
      while (temp.isEmpty()==false) {
        Set c1 = (Set) temp.pop();
        cons = true;
        Iterator it = result.iterator();
        /* slow loop involving creating new HashSet's unnecessarily
				// plus it rechecks feasibility of c1 U c2 though both
				// c1 and c2 are known to be feasible
				while (it.hasNext()) {
          Set c12 = new HashSet(c1);
          Set c2 = (Set) it.next();
          c12.addAll(c2);
          if (isFeas(c12)) {
            it.remove();  // used to be result.remove(c2);
            temp.add(c12);
            cons=false;
            break;
          }
        }
				*/
				// /* faster loop does not do redundant work
				while (it.hasNext()) {
          Set c2 = (Set) it.next();
          if (isFeas(c1,c2)) {
            it.remove();  //used to be result.remove(c2);
						/* no need to create new set c12 as c2 can be re-used
						Set c12 = new HashSet(c1);
						c12.addAll(c2);
						temp.add(c12);
						*/
						c2.addAll(c1);
						temp.add(c2);
            cons=false;
            break;
          }
        }
				// */
        if (cons) {
          // make sure you don't insert smth that already exists
          boolean iscovered=false;
          for (int i=0; i<store.size() && !iscovered; i++) {
            Set ti = (Set) store.get(i);
            if (ti.containsAll(c1)) iscovered=true;
          }
          if (!iscovered) result.add(c1);
        }
      }
    }
    result.addAll(store);
    return result;
  }


  /**
   * return the Set&lt;Node&gt; that are the best node(s) to add given the current
   * active <CODE>_nodes</CODE> set. This is the set of nodes that are free to
	 * cover, have max. weight (within the fudge factor <CODE>_ff</CODE>), and
	 * have the least weight of "free" NBors() (again within the same fudge factor).
	 * Alternatively, if the "useGWMIN2criterion" flag is true, the "GWMIN2"
	 * heuristic criterion is utilized, so that the free nodes that are within
	 * <CODE>_ff</CODE> times from the maximum value of the quantity
	 * $w_n / \Sum_{v \in N^+_n}$ form the return set.
	 * @param isroot boolean if true then _ff is set to zero, so that all
	 * non-forbidden nodes with min. sum of neighbors-weights are returned.
	 * @throws ParallelException
   * @return Set // Set&lt;Node&gt;
   */
  private Set getBestNodes2Add(boolean isroot) throws ParallelException {
    final int gsz=getMaster().getGraphSize();
    final double perc_extra_nodes2add = getMaster().getAvgPercExtraNodes2Add();
		final double ff = isroot ? 0.0 : _ff;
		final boolean useGWMIN2 = getMaster().getUseGWMIN24BestNodes2Add();
    Set best = new HashSet();
    double bestcost = Double.MAX_VALUE;
    double best_node_cost = Double.NEGATIVE_INFINITY;
    Set forbidden = getForbiddenNodes();
    for (int i=0; i<gsz; i++) {
      Node ni = getMaster().getGraph().getNodeUnsynchronized(i);
      if (forbidden.contains(ni)==false) {
        Double niwD = ni.getWeightValueUnsynchronized("value");
        double ni_weight = niwD==null ? 1.0 : niwD.doubleValue();
				if (useGWMIN2) {  // ni_weight must be divided by the sum of all free neighbors' weights plus its own
					Set nibors = ni.getNborsUnsynchronized();
					double denom = ni_weight;
					Iterator it = nibors.iterator();
					while (it.hasNext()) {
						Node nb = (Node) it.next();
						Double bD = nb.getWeightValueUnsynchronized("value");
						if (!forbidden.contains(nb))
							denom += (bD==null ? 1.0 : bD.doubleValue());
					}
					ni_weight /= denom;
				}
        if (Double.compare(ni_weight, best_node_cost)>0) {
          if (ff>=1.0) best.clear();
          else {  // remove nodes in best that are "too light" compared to ni
            Iterator bit = best.iterator();
            while (bit.hasNext()) {
              Node n = (Node) bit.next();
              Double wnD = n.getWeightValueUnsynchronized("value");
              double nw = wnD==null ? 1.0 : wnD.doubleValue();
							if (useGWMIN2) {  // nw must be divided accordingly
								Set nbors = n.getNborsUnsynchronized();
								double denom = nw;
								Iterator it = nbors.iterator();
								while (it.hasNext()) {
									Node nb = (Node) it.next();
									Double bD = nb.getWeightValueUnsynchronized("value");
									if (!forbidden.contains(nb))
										denom += (bD==null ? 1.0 : bD.doubleValue());
								}
								nw /= denom;
							}
              if (nw < ni_weight*ff) bit.remove();
            }
          }
          best.add(ni);
          best_node_cost = ni_weight;
        }
        if (ni_weight >= ff*best_node_cost && !useGWMIN2) {  // is wrong to use "else if" in this stmt
          // check for "free" nbors
          // below is a valid but slow method to compute "free" nbors
          //Set ni_nnbors = new HashSet(ni.getNNbors());
          //ni_nnbors.removeAll(forbidden);
          //int nisize = ni_nnbors.size();
          Set ni_nbors = ni.getNborsUnsynchronized();
          Iterator nnit = ni_nbors.iterator();
          double nisize = 0;
          while (nnit.hasNext()) {
            Node nbor = (Node) nnit.next();
            Double nwD = nbor.getWeightValueUnsynchronized("value");
            double nw = nwD==null ? 1.0 : nwD.doubleValue();
            if (forbidden.contains(nbor)==false) nisize += nw;
          }
          if (nisize<bestcost) {  // new best
            if (ff>=1.0) best.clear();
            else {  // remove nodes in best w/ nbors are "heavy" compared to ni
              Iterator bit = best.iterator();
              while (bit.hasNext()) {
                Node n = (Node) bit.next();
                Set n_nbors = n.getNborsUnsynchronized();
                double n_nbors_weight = 0.0;
                Iterator nn_it2 = n_nbors.iterator();
                while (nn_it2.hasNext()) {
                  Node nn = (Node) nn_it2.next();
                  Double nwD = nn.getWeightValueUnsynchronized("value");
                  double nnw = nwD==null ? 1.0 : nwD.doubleValue();
                  if (forbidden.contains(nn)==false) n_nbors_weight += nnw;
                }
                if (n_nbors_weight > nisize*(2.0-ff)) bit.remove();
              }
            }
            best.add(ni);
            bestcost = nisize;
          }
          else if (nisize <= bestcost*(2.0-ff)) {  // approx. equal to best, add to set
            best.add(ni);
          }
          // else continue;
        }
        // else continue;
      }
    }  // for i=0...gsz-1
    if (perc_extra_nodes2add>0) {
      double num_extra_nodes2add = bestcost*perc_extra_nodes2add;
      int lvl = getLevel();
      if (lvl>0) {
        // add probabilistically some non-best nodes depending on the level
        // of the node, the "goodness" of the node and the user-defined quantity
				final int tid = Thread.currentThread() instanceof IdentifiableIntf ?
				    (int) ((IdentifiableIntf) Thread.currentThread()).getId() : 0;
				Random r = RndUtil.getInstance(tid).getRandom();
        for (int i = 0; i < gsz; i++) {
          Node ni = getMaster().getGraph().getNodeUnsynchronized(i);
          if (forbidden.contains(ni)) continue;
          Set ni_nbors = ni.getNborsUnsynchronized();
          Iterator nnit = ni_nbors.iterator();
          double nisize = 0;
          while (nnit.hasNext()) {
            Node nbor = (Node) nnit.next();
						Double nwD = nbor.getWeightValueUnsynchronized("value");
						double nw = nwD==null ? 1.0 : nwD.doubleValue();
            if (forbidden.contains(nbor)==false) nisize += nw;
          }
          double fitness = nisize > 0 ? bestcost / nisize : 1;
          double prob = num_extra_nodes2add * fitness / (gsz * Math.sqrt(lvl));
          double ri = r.nextDouble();  // used to be ri = RndUtil.getInstance().getRandom().nextDouble();
          if (ri<prob) {
            best.add(ni);
          }
        }
      }
    }
    return best;
  }


  /**
   * check if node nj can be set to one when the nodes in active are also set.
   * @param nj Node
   * @param active Set // Set&lt;Node&gt;
   * @return boolean true iff nj can be added to active
   */
  private static boolean isFree2Cover(Node nj, Set active) {
    if (active.contains(nj)) return false;
    /* slow method
		Set nborsj = new HashSet(nj.getNbors());
    nborsj.retainAll(active);
    if (nborsj.size()>0) return false;
		*/
		// /* faster: no need for HashSet's creation
		Set nborsj = nj.getNborsUnsynchronized();  // no modification to take place
		Iterator itj = nborsj.iterator();
		while (itj.hasNext()) {
			Node nnj = (Node) itj.next();
			if (active.contains(nnj)) return false;
		}
		// */
    return true;
  }


	/**
	 * check if the nodes parameter can be added to the current active set of
	 * nodes represented by this BBNode1 object.
	 * @param nodes Set // Set&lt;Node&gt;
	 * @return boolean true iff nodes can be added to the current solution
	 */
  private boolean isFeas(Set nodes) {
    Set allnodes = new HashSet(getNodes());
    Iterator it = nodes.iterator();
    while (it.hasNext()) {
      Node n = (Node) it.next();
      if (isFree2Cover(n, allnodes))
        allnodes.add(n);
      else return false;
    }
    return true;
  }


	/**
	 * check if c1 conflicts with c2, in that there exist n1 \in c1 that is a
	 * neighbor of some n2 \in c2.
	 * Notice that the method assumes that both c1 and c2 are currently feasible,
	 * ie the calls isFeas(c1) and isFeas(c2) must return true.
	 * @param c1 Set // Set&lt;Node&gt;
	 * @param c2 Set // Set&lt;Node&gt;
	 * @return boolean false iff c1 U nbors(c1) contain any of c2.
	 */
	private static boolean isFeas(Set c1, Set c2) {
		if (c1.size()==0 || c2.size()==0) return true;
		Iterator it1 = c1.iterator();
		while (it1.hasNext()) {
			Node n1 = (Node) it1.next();
			if (c2.contains(n1)) continue;
			Set nbors_n1 = n1.getNborsUnsynchronized();
			Iterator itnbor1 = nbors_n1.iterator();
			while (itnbor1.hasNext()) {
				Node nbor1 = (Node) itnbor1.next();
				if (c2.contains(nbor1)) return false;
			}
		}
		return true;
	}


  /* debug routine */
  private String printNodes() {
    String res = "[";
    Iterator it = getNodes().iterator();
    while (it.hasNext()) {
      Node n = (Node) it.next();
      res += n.getId();
      if (it.hasNext()) res+= ", ";
    }
    res += "]";
    return res;
  }
}

