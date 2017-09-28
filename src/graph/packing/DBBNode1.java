package graph.packing;

import graph.*;
import parallel.*;
import parallel.distributed.*;
import utils.*;
import popt4jlib.*;
import popt4jlib.LocalSearch.*;
import java.util.*;
import java.io.*;

/**
 * represents a node in the distributed B &amp; B tree of the hybrid
 * B &amp; B - GASP scheme for the 1-packing problem (max weighted independent
 * set problem). Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DBBNode1 extends DBBNodeBase {

	private static final long _MIN_REQ_ELAPSED_TIME_4_DISTRIBUTION = 10000L;  // 10 seconds
	private boolean _immigrant = false;  // set when sent for distributed exec.
	protected Set _nodeids = null;  // HashSet<Integer> set of active node ids in current soln.
	protected int _lvl;
	protected double _bound = Double.NEGATIVE_INFINITY;

  private static ThreadLocal _lastDistributionTimes = new ThreadLocal() {
    protected Object initialValue() {
      return null;
    }
  };

	
	/**
	 * comparator between node-sets used in sorting node-sets in the
	 * <CODE>getBestNodeSets2Add()</CODE> method if the appropriate option in the
	 * parameters of the calling program is set. This sorting only makes sense
	 * when children BBNode1 objects will be "cut" short due to flags set to guide
	 * the search process.
	 */
	private static NodeSetWeightComparator _nscomtor = new NodeSetWeightComparator();


  /**
   * Sole constructor of a DBBNode1 object. Clients are not expected to
   * create such objects which are instead created dynamically through the
   * B&amp;B process.
   * @param r Set // Set&lt;Integer&gt; the set of (graph) node ids to be added
	 * to the nodes of the parent to represent a new partial solution
	 * @param lvl int  // the level at which this DBBNode1 object is
   */
  DBBNode1(Set r, int lvl) {
		_nodeids = r;
		_lvl = lvl;
		if (_lvl==0) _immigrant=true;  // root node is an immigrant too
  }


  /**
   * the main method of the class, that processes the partial solution
   * represented by this object. This method is heavily based on the 
	 * <CODE>BBNode1</CODE> class, adapted for distributed cluster parallel
	 * computing.
   */
  public Serializable run() {
			
    DConditionCounterLLCClt cond_counter=null;
		utils.Messenger mger = utils.Messenger.getInstance();
		try {
      boolean foundincumbent = false;
			DBBTree _master = DBBTree.getInstance();
			cond_counter = _master.getDConditionCounterClt();
			// step 0.
			// see if worker we're working in, is in a closing state
			boolean wrk_is_closing = PDAsynchBatchTaskExecutorWrk.isClosing();
			if (wrk_is_closing) {  // stop computations here
				return null;  
			}
      // step 1.
      // see if limit has been reached
			int cur_counter = _master.incrementCounter();  // increment this process's #DBBNode1 objects
			mger.msg("#DBBNode1 objects created by this process="+cur_counter, 2);
			if (cur_counter > _master.getMaxNodesAllowed()) {
				PDAsynchBatchTaskExecutorWrk.setServerRequestsDisabled(true);
				return null;  // this worker is done
      }
      // step 2.
      // check for pruning
      double bnd = getBound();
      if (bnd <= _master.getBound() || bnd < _master.getMinKnownBound()) {
        return null; // node is fathomed
      }
      // step 3.
      // add as many nodes as possible in GASP fashion
      Set candidates = null; // Set<Set<Node> >
      while (true) {
        candidates = getBestNodeSets2Add();
        if (candidates != null && candidates.size() == 1) {
          //_nodes.addAll( (Set) candidates.iterator().next());
					Set next_cand = (Set) candidates.iterator().next();  // Set<Node>
					Iterator it = next_cand.iterator();
					while (it.hasNext()) {
						Node n = (Node) it.next();
						_nodeids.add(new Integer(n.getId()));
					}
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
      // branch?
      if (candidates != null && candidates.size()!=0) {  // candidates.size() is in fact > 1
        try {
          List children = new ArrayList();
          Iterator it = candidates.iterator();
          int cnt_children = 0;
          while (it.hasNext()) {
            if (cnt_children++ > _master.getMaxChildrenNodesAllowed())
							break;
            Set ns = (Set) it.next();
						Set ns2 = new HashSet(_nodeids);
						Iterator it2 = ns.iterator();
						while (it2.hasNext()) {
							ns2.add(new Integer(((Node) it2.next()).getId()));
						}
						DBBNode1 child = new DBBNode1(ns2, _lvl+1);
            // check if child's bound is better than incumbent
            double childbound = child.getBound();
            if (childbound <= _master.getBound() ||
								childbound < _master.getMinKnownBound())  // not good enough
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
          int sz = children.size();
          if (sz == 0) {
            if (foundincumbent) _master.reduceTightenUpperBoundLvl();
						return null; // no children
          }
					// send the children to be executed elsewhere, only if it's time
					if (mustKeepLocally()) {  // keep them locally
						// run children
						for (int i=0; i<sz; i++) {
							DBBNode1 ci = (DBBNode1) children.get(i);
							ci.run();
						}
					}
					else {  // send (all of them) them elsewhere for execution
						TaskObject[] tasks = new TaskObject[sz];
						for (int i=0; i<sz; i++) {
							DBBNode1 ci = (DBBNode1) children.get(i);
							ci._immigrant = true;
							tasks[i] = ci;
						}
						children.clear();
						try {
							cond_counter.increment(sz);  // notify condition-counter
						}
						catch (Exception e) {
							e.printStackTrace();
							System.exit(-1);
						}
						try {
							PDAsynchBatchTaskExecutorClt.getInstance().submitWorkFromSameHost(tasks);
						}
						catch (PDAsynchBatchTaskExecutorNWAException e) {  // execute the tasks locally
							mger.msg("DBBNode1.run(): got children back due to workers' UNAVAILABILITY, will run them locally", 1);
							for (int i=0; i<tasks.length; i++) {
								// first seriously increment how many levels to keep descendants locally
								// run each child
								tasks[i].run();
							}
						}
					}
        }
        catch (Exception e) {  // insanity
          e.printStackTrace();
          System.exit( -1);
        }
      }
      else {  // no branching occurs
        if (foundincumbent) {
          _master.reduceTightenUpperBoundLvl();
          if (_master.getLocalSearch()) {  // perform a local search
						long start_time = System.currentTimeMillis();
            try {
							Set nodeids = new IntSet(_nodeids);
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
								_nodeids.clear();
								_nodeids.addAll(sn);
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
      }
			return null;
    }
    catch (ParallelException e) {
      e.printStackTrace();
			return null;
    }
		finally {
			//mger.msg("Done with node "+
			//	       " on lvl="+_lvl+" _startLvl="+_startLvl+
			//	       " _NUM_LVLS_2_RUN_ON_CURRENT="+_NUM_LVLS_2_RUN_ON_CURRENT, 1);
			if (_immigrant && cond_counter!=null) {
				try {
					cond_counter.decrement();
				}
				catch (Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
			} 
			else if (_immigrant) {  // insanity
				System.err.println("null cond_counter???");
				System.exit(-1);
			}
		}
  }


	final Set getNodeIds() {
		//return _nodes;
		return _nodeids;
	}
	
	
	protected final Set getNodeIdsAsSet() {
		return new HashSet(_nodeids);
	}


 /**
  * compares this BB-node with another BB-node, via the master BBTree's
  * BBNodeComparator object for comparing BB-nodes.
  * @param other Object
  * @return int
  */
 public int compareTo(Object other) {
    DBBNode1 o = (DBBNode1) other;
    return DBBTree.getInstance().getDBBNodeComparator().compare(this, o);
  }


	/**
	 * always returns false.
	 * @return boolean
	 */
  public boolean isDone() {
    return false;
  }


	/**
	 * unsupported operation, always throws.
	 * @param obj TaskObject
	 * @throws IllegalArgumentException
	 */
  public void copyFrom(TaskObject obj) throws IllegalArgumentException {
		throw new IllegalArgumentException("not supported");
  }


  /**
   * compares for equality using the object's <CODE>compareTo()</CODE> method.
   * @param other Object expected another BBBNode1 object
   * @return boolean return true iff the compareTo(other) method return 0.
   */
  public boolean equals(Object other) {
    int ct = compareTo(other);
    return (ct==0);
  }


  /**
   * returns the size of the nodes of this DBBNode1.
   * @return int
   */
  public int hashCode() {
		return getNodeIds().size();
	}


	/**
	 * return the sum of weights of the active nodes.
	 * @return double
	 */
	protected double getCost() {
		double res = 0.0;
		Iterator it = getNodeIds().iterator();
		Graph g = DBBTree.getInstance().getGraph();
		while (it.hasNext()) {
			//Node ni = (Node) it.next();
			Integer nid = (Integer) it.next();
			// Node ni = g.getNode(nid.intValue());
			Node ni = g.getNodeUnsynchronized(nid.intValue());  // no need for synchronization
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
  protected Set getForbiddenNodes() {
		Graph g = DBBTree.getInstance().getGraph();
    Set forbidden = new HashSet();
		Iterator it = getNodeIds().iterator();
		while (it.hasNext()) {
      Integer nid = (Integer) it.next();
			Node n = g.getNodeUnsynchronized(nid.intValue());
			forbidden.add(n);
      Set nnbors = n.getNborsUnsynchronized();
      forbidden.addAll(nnbors);
    }
    return forbidden;
  }


  /**
   * compute a max possible number of nodes weights this soln can have.
   * One such bound is _nodes.sum_weights + (gsz - getForbiddenNodes().size())*max_open_node_weight/2
   * @return double
   */
  protected double getBound() {
    if (_bound>=0) return _bound;  // cache
		Graph g = DBBTree.getInstance().getGraph();
		//Iterator nodesit = getNodes().iterator();
		Iterator nodesit = getNodeIds().iterator();
		double res = 0.0;
		while (nodesit.hasNext()) {
			//Node ni = (Node) nodesit.next();
			Integer nid = (Integer) nodesit.next();
			Node ni = g.getNodeUnsynchronized(nid.intValue());
			Double niw = ni.getWeightValueUnsynchronized("value");
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
		DBBTree master = DBBTree.getInstance();
		Double max_node_weightD = g.getMaxNodeWeight("value",forbidden);
		double mnw = max_node_weightD==null ? 1.0 : max_node_weightD.doubleValue();
    res += (master.getGraphSize()-forbidden.size())*mnw/2.0;  // itc 2015-02-11: added the division by 2
    _bound = res;
    return res;
  }


  /**
   * return Set&lt;Set&lt;Node&gt; &gt; of all maximal nodesets that can be added
	 * together to the current active <CODE>_nodeIds</CODE> set.
   * @return Set // Set&lt;Set&lt;Node&gt; &gt;
	 * @throws ParallelException never
   */
  protected Set getBestNodeSets2Add() throws ParallelException {
		DBBTree master = DBBTree.getInstance();
    final int kmax = master.getMaxAllowedItersInGBNS2A();
    final Set ccands = getBestNodes2Add(_lvl==0);
    Set result;
    if (master.getSortBestCandsInGBNS2A()) result = new TreeSet(_nscomtor);
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
    if (_lvl==0) {
			// if root, return collection of each available node as singleton sets
			// this should speed up parallel processing
      // correct GASP behavior
      result.addAll(temp);
      return result;
    }
    // figure out all the maximal subsets of ccands that are not conflicting
    // as it is, this routine does not guarantee that the nodes are being added
    // in a GASP fashion, as when one node of a set ci is added to _nodeids, the
    // other nodes in ci may no longer be the "optimal" in GASP sense to add to
    // _nodeids.
    int cnt=0;  // this counter is used to stop the max. subsets creation process from going wild
    while (temp.isEmpty()==false) {
      if (++cnt>=kmax) break;
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
      boolean cons=true;
      while (temp.isEmpty()==false) {
        Set c1 = (Set) temp.pop();
        cons = true;
        Iterator it = result.iterator();
				// /* faster loop does not do redundant work
				while (it.hasNext()) {
          Set c2 = (Set) it.next();
          if (isFeas(c1,c2)) {
            it.remove();  //used to be result.remove(c2);
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
	 * check whether children nodes must be sent for distribution or not, based
	 * on when the current thread sent children before. If it returns false, it 
	 * also updates the time of the last distribution to now.
	 * @return boolean
	 */
	private static boolean mustKeepLocally() {
		long now = System.currentTimeMillis();
		long last_time = getLastDistributionTime();
		if (now - last_time > _MIN_REQ_ELAPSED_TIME_4_DISTRIBUTION) {
			setLastDistributionTimeNow();
			return false;
		}
		return true;
	}


  /**
   * return the Set&lt;Node&gt; that are the best node(s) to add given the current
   * active <CODE>_nodeids</CODE> set. This is the set of nodes that are free to
	 * cover, have max. weight (within the fudge factor <CODE>_ff</CODE>), and
	 * have the least weight of "free" NBors() (again within the same fudge factor).
	 * Alternatively, if the "useGWMIN2criterion" flag is true, the "GWMIN2"
	 * heuristic criterion is utilized, so that the free nodes that are within
	 * <CODE>_ff</CODE> times from the maximum value of the quantity
	 * $w_n / \Sum_{v \in N^+_n}w_v$ form the return set.
	 * @param isroot boolean if true then _ff is set to zero, so that all
	 * non-forbidden nodes with min. sum of neighbors-weights are returned.
	 * @throws ParallelException
   * @return Set // Set&lt;Node&gt;
   */
  private Set getBestNodes2Add(boolean isroot) throws ParallelException {
    final DBBTree master = DBBTree.getInstance();
		final int gsz=master.getGraphSize();
    final double perc_extra_nodes2add = master.getAvgPercExtraNodes2Add();
		final double ff = isroot ? 0.0 : DBBNodeBase.getFF();
		final boolean useGWMIN2 = master.getUseGWMIN24BestNodes2Add();
    Set best = new HashSet();
    double bestcost = Double.MAX_VALUE;
    double best_node_cost = Double.NEGATIVE_INFINITY;
    Set forbidden = getForbiddenNodes();
    for (int i=0; i<gsz; i++) {
      Node ni = master.getGraph().getNodeUnsynchronized(i);
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
      int lvl = _lvl;
      if (lvl>0) {
        // add probabilistically some non-best nodes depending on the level
        // of the node, the "goodness" of the node and the user-defined quantity
				final int tid = Thread.currentThread() instanceof IdentifiableIntf ?
				    (int) ((IdentifiableIntf) Thread.currentThread()).getId() : 0;
				Random r = RndUtil.getInstance(tid).getRandom();
        for (int i = 0; i < gsz; i++) {
          Node ni = master.getGraph().getNodeUnsynchronized(i);
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
		Set nborsj = nj.getNborsUnsynchronized();  // no modification to take place
		Iterator itj = nborsj.iterator();
		while (itj.hasNext()) {
			Node nnj = (Node) itj.next();
			if (active.contains(nnj)) return false;
		}
    return true;
  }


	/**
	 * check if the nodes parameter can be added to the current active set of
	 * nodes represented by this DBBNode1 object.
	 * @param nodes Set // Set&lt;Node&gt;
	 * @return boolean true iff nodes can be added to the current solution
	 */
  private boolean isFeas(Set nodes) {
    Set allnodes = new HashSet();
		Iterator itids = _nodeids.iterator();
		Graph g = DBBTree.getInstance().getGraph();
		while (itids.hasNext()) {
			Integer nid = (Integer) itids.next();
			Node ni = g.getNodeUnsynchronized(nid.intValue());
			allnodes.add(ni);
		}
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

	
  private static long getLastDistributionTime() {
    Long p = (Long) _lastDistributionTimes.get();
    if (p==null) {
      p = new Long(0);
      _lastDistributionTimes.set(p);
    }
    return p.longValue();
  }
	private static void setLastDistributionTimeNow() {
		_lastDistributionTimes.set(new Long(System.currentTimeMillis()));
	}


  /* debug routine */
  private String printNodes() {
    String res = "[";
    /*Iterator it = getNodes().iterator();
    while (it.hasNext()) {
      Node n = (Node) it.next();
      res += n.getId();
      if (it.hasNext()) res+= ", ";
    }
		*/
		Iterator it = getNodeIds().iterator();
    while (it.hasNext()) {
      Integer nid = (Integer) it.next();
			res += nid.toString();
      if (it.hasNext()) res+= ", ";
    }
		res += "]";
    return res;
  }
}

