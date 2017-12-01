package graph.coarsening;

import graph.*;
import parallel.*;
import java.util.*;


/**
 * reverse edge-based coarsener (merges nodes that are least connected with 
 * each other). Class is not thread-safe, and so should be 
 * protected by clients when used in a multi-threaded context.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 2.0
 */
public class CoarsenerIREC extends Coarsener {

	/**
	 * single public constructor.
	 * @param g Graph
	 * @param partition int[]
	 * @param props HashMap
	 */
  public CoarsenerIREC(Graph g, int[] partition, HashMap props) {
    super(g, partition, props);
  }


	/**
	 * implementation of base class method, this factory method constructs a new 
	 * instance of this class.
	 * @param g Graph
	 * @param partition int[]
	 * @param properties HashMap
	 * @return Coarsener  // CoarsenerIREC
	 */
  public Coarsener newInstance(Graph g, int[] partition, HashMap properties) {
    return new CoarsenerIREC(g, partition, properties);
  }


  /**
   * coarsen() performs the following loop until the number of unmatched
   * nodes is less than a ratio of the original nodes:
   * (1) we visit an unvisited node randomly;
   * (2) we match it with the neighbor that yields the minimum value of
   *     arcs connecting weight
   * (3) we mark as visited both nodes that were matched of course.
	 * Notice there is no second-order "common neighbor connecting nodes" effect
	 * calculation here, nor any lambda. But on the other hand, when computing
	 * direct neighbor weights for two nodes that are a possible match, we make
	 * sure to count any other fine nodes that are already "matched" with the 
	 * second node to form in the coarse graph a single coarse node.
	 * The result is a new "coarse" graph that can be accessed by a call to 
	 * <CODE>getCoarseGraph()</CODE> (after this method completes).
	 * Notice that the pair &lt;"ratio",Double val&gt; must be in the 
	 * <CODE>_properties</CODE> map of this object, defining the ratio of coarse
	 * nodes over fine nodes that must be reached for the process to stop. Same 
	 * for the pair &lt;"max_allowed_card",Integer num&gt; that specifies the 
	 * maximum cardinality allowed for any "fine-level" node in order to be 
	 * considered for coarsening with other nodes. 
	 * Also, if the pair
	 * &lt;"fine_mwis", Set&lt;Integer&gt;&gt; is present in the properties map
	 * then the coarsener will attempt to make sure that the fine-graph MWIS 
	 * solution can be maintained in the coarse-graph in the sense described in 
	 * the documentation of method <CODE>fineMWISFailsInCoarseGraph</CODE>.In this 
	 * case, the method will also compute the set of coarse nodes that contain the 
	 * fine-level nodes in "fine_mwis" and put it in the properties map of this 
	 * object under the name "coarse_mwis"; notice that this "coase_mwis" may not 
	 * actually be an independent (or maximal) set for the coarse graph.
   * @throws GraphException
   * @throws CoarsenerException if coarsening couldn't proceed satisfactorily,
	 * ie couldn't reach desired compression ratio as specified in "ratio" key's
	 * value in properties
   */
  public void coarsen() 
		throws GraphException, CoarsenerException, ParallelException {
    System.err.println("CoarsenerIREC.coarsen() entered");
    // System.err.println("coarsen(): _g.numnodes="+getOriginalGraph().getNumNodes());
    reset();  // remove old data
    List l = new ArrayList();  // used to be Vector
    for (int i=0; i<getOriginalGraph().getNumNodes(); i++)
			l.add(new Integer(i));
    Collections.shuffle(l, utils.RndUtil.getInstance().getRandom());  
    // get a random permutation
    final double ratio = ((Double) getProperty("ratio")).doubleValue();
    final int min_allowed_nodes = (int) (ratio*_g.getNumNodes());
    int num_seen = 0;
    int new_nodes = 0;
		final int num_nodes=l.size();
    int j;
    // main loop
    for (j=0; j<num_nodes; j++) {
      // check to see if coarsening has exceeded thresholds
      if (new_nodes+_g.getNumNodes()-num_seen<=min_allowed_nodes) break;
      Integer pos = (Integer) l.get(j);
      if (_map.get(pos)==null) {  // OK, not mapped yet
        // find best nbor
        Node best = getBestNbor(pos);
        if (best==null) {
          continue;  // node cannot be matched at this point
                     // e.g. may be isolated
        }
        ++num_seen;
        Integer best_id = new Integer(best.getId());
        // match node at pos with best
        // see if best is matched already
        Integer best_new = (Integer) _map.get(best_id);
        if (best_new!=null) {
          // put pos-th node at best_new
          _map.put(pos, best_new);  // map<oldid, newid>
          Set nids = (Set) _rmap.get(best_new);
          nids.add(pos);
        }
        else {
          // create new merger node
          Integer new_pos = new Integer(new_nodes);
          Set new_set = new HashSet();
          new_set.add(pos);
          new_set.add(best_id);
          _map.put(pos, new_pos);
          _map.put(best_id, new_pos);
          _rmap.put(new_pos, new_set);
          ++num_seen;
          ++new_nodes;
        }
      }
    }
    // check if coarsening reached desired level
    if (new_nodes+_g.getNumNodes()-num_seen>min_allowed_nodes) {
      throw new CoarsenerException(
				          "CoarsenerIREC.coarsen(): cannot proceed further");
    }
    // System.err.println("_map.size()="+_map.size()+" num_seen="+num_seen+" new_nodes="+new_nodes+" j="+j);
    // OK, go on
    // finally put remaining unmatched nodes as they are in the new graph
    for (int i=0; i<_g.getNumNodes(); i++) {
      Integer pi = new Integer(i);
      if (_map.get(pi)==null) {
        // unmatched, put it in on itself
        Integer npos = new Integer(new_nodes++);
        _map.put(pi, npos);
        Set oset = new HashSet();
        oset.add(pi);
        _rmap.put(npos, oset);
      }
    }
    // and finally create the arcs for the new graph
    // for each old arc, if it connects two new nodes, connect the new nodes.
    // if there's already an arc, add the weight of this one to the new one.
    HashMap new_arcs_table = new HashMap();  
    // map<Integer new_startid, Set<LinkPair> >
    int new_arcs=0;
    for (int i=0; i<_g.getNumArcs(); i++) {
      Link ll = _g.getLink(i);
      Integer lls = new Integer(ll.getStart());
      Integer lle = new Integer(ll.getEnd());
      Integer new_lls = (Integer) _map.get(lls);
      Integer new_lle = (Integer) _map.get(lle);
      if (new_lls.intValue()== new_lle.intValue()) continue;  // arc is hidden
      else {
        LinkPair lp = new LinkPair(new_lls.intValue(), new_lle.intValue(), 
					                         ll.getWeight());
        Set lps = (Set) new_arcs_table.get(new_lls);
        if (lps!=null && lps.contains(lp)) {
          Iterator iter = lps.iterator();
          while (iter.hasNext()) {
            LinkPair lp_ex = (LinkPair) iter.next();
            if (lp_ex.equals(lp)) {
              lp_ex.addWeight(ll.getWeight());
              break;
            }
          }
        }
        else {
          // new arc found
          ++new_arcs;
          if (lps==null) lps = new HashSet();
          lps.add(lp);
          new_arcs_table.put(new_lls, lps);
        }
      }
    }

    _coarseG = Graph.newGraph(new_nodes, new_arcs);
    Iterator new_arcs_table_iter = new_arcs_table.keySet().iterator(); 
    while (new_arcs_table_iter.hasNext()) {
      Integer new_start = (Integer) new_arcs_table_iter.next();
      Set linkpairs = (Set) new_arcs_table.get(new_start);
      Iterator lpairs_iter = linkpairs.iterator();
      while (lpairs_iter.hasNext()) {
        LinkPair lp = (LinkPair) lpairs_iter.next();
        _coarseG.addLink(lp.getStart(), lp.getEnd(), lp.getWeight());
      }
    }

    // set the right "cardinality" and "value" values for the _coarseG nodes
    for (int i=0; i<_coarseG.getNumNodes(); i++) {
      Node ni = _coarseG.getNodeUnsynchronized(i);
      Set si = (Set) _rmap.get(new Integer(i));
      ni.setWeight("cardinality", new Double(si.size()));  // shallow value of 
			                                                     // "cardinality"
			// value gets deep value
			Iterator sit = si.iterator();
			double wgtval = 0.0;
			while (sit.hasNext()) {
				Integer oid = (Integer) sit.next();
				Node no = _g.getNodeUnsynchronized(oid.intValue());
				Double ov = no.getWeightValueUnsynchronized("value");
				wgtval += ov==null ? 1.0 : ov.doubleValue();
			}
			ni.setWeight("value", new Double(wgtval));  // deep value of "value"
    }

    if (_graphPartition!=null) {
      // finally create a coarse_graph_partition array and put it into the
      // _properties under the name "coarsePartition".
      int[] coarse_graph_partition = new int[_coarseG.getNumNodes()];
      for (int i=0; i<coarse_graph_partition.length-1; i++) {
        Set xi = (Set) _rmap.get(new Integer(i));
        Integer xi_first_id = (Integer) xi.iterator().next();
        coarse_graph_partition[i] = _graphPartition[xi_first_id.intValue()];
      }
      setProperty("coarsePartition", coarse_graph_partition);
    }
		
		Set fine_mwis = (Set) getProperty("fine_mwis");  // Set<Integer nodeid>
		if (fine_mwis!=null) {
			Set coarse_mwis = new HashSet();  // Set<Integer nodeid>
			Iterator fmwis_it = fine_mwis.iterator();
			while (fmwis_it.hasNext()) {
				Integer fid = (Integer) fmwis_it.next();
				Integer cid = (Integer) _map.get(fid);
				coarse_mwis.add(cid);
			}
			setProperty("coarse_mwis", coarse_mwis);
		}

    // System.err.println("coarsen(): _coarseG.numnodes="+getCoarseGraph().getNumNodes());
    System.err.println("CoarsenerIREC.coarsen() finished");
  }


  /**
   * find the best neighbor that should be matched with node at position pos.
   * The best neighbor is the one minimizing the value
   * conn_arcs_weight assuming partition constraints and cardinality 
	 * constraints are satisfied. Also, if the "fine_mwis" property exists, then
	 * the match between nodes will attempt to preserve the fine_mwis solution as
	 * a valid independent set solution in the coarse graph.
   * @param pos Integer
   * @return Node
	 * @throws NullPointerException if a value for the key "max_allowed_card" is 
	 * not present in the properties map of this object.
   */
  private Node getBestNbor(Integer pos) {
    Node node = _g.getNodeUnsynchronized(pos.intValue());
    final int num_nodes = _g.getNumNodes();
		int node_part = -1;
    if (_graphPartition != null)
      node_part = _graphPartition[pos.intValue()];
    Node bestnode = null;
		final double mac = ((Double)getProperty("max_allowed_card")).doubleValue();
		final Set fine_mwis = (Set) getProperty("fine_mwis");
    double bestwgt = Double.MAX_VALUE;
		// first work with nodes that are not connected to the node
		for (int i=0; i<num_nodes; i++) {
			if (i==pos.intValue()) continue;  // don't choose self
			Node ni = _g.getNodeUnsynchronized(i);
			Set nodebors = node.getNborsUnsynchronized();
			if (nodebors.contains(ni)) continue;
			// check for node-weights first
			Node en=ni;
      int en_part = -1;
      if (_graphPartition!=null) en_part = _graphPartition[en.getId()];
      if (node_part!=en_part) continue;  // respect partition
      // check if en node is overweight
      if (en.getWeightValueUnsynchronized("cardinality").doubleValue()>=mac)
        continue;
			// check also if en belongs to a new coarse node, the weight of the new
			// coarse node
			Integer new_en_id = (Integer) _map.get(new Integer(en.getId()));
			if (new_en_id!=null) {
				Set bundle_ids = (Set) _rmap.get(new_en_id);
				double t_wgt = 0.0;
				Iterator bit = bundle_ids.iterator();
				while (bit.hasNext()) {
					Integer nid = (Integer) bit.next();
					Node n = _g.getNodeUnsynchronized(nid.intValue());
					t_wgt += n.getWeightValueUnsynchronized("cardinality").doubleValue();
				}
				if (t_wgt>=mac) continue;
			}			
			// check if ni is already mapped to some coarse node, and if so compute
			// weight of all arcs connecting node to nodes in coarse node
			double tot_wgt = getMappedNborsWgts(node, ni, nodebors);
			if (tot_wgt<bestwgt) {
				// make sure ni doesn't have a neighbor mapped to some coarse node that
				// contains a fine node in fine_mwis set
				if (fineMWISFailsInCoarseGraph(ni, fine_mwis)) continue;
				bestnode=ni;
				bestwgt=tot_wgt;
				if (Double.compare(tot_wgt,0.0)<=0) {  // short-cut: can't get better!
					return bestnode;
				}
			}
		}  // for i in _g._nodes that are NOT neighbors of node
    // work with node's inlinks (node is the end of the arc)
    Iterator iter = node.getInLinks().iterator();
    Set nodebors = node.getNborsUnsynchronized();
		while (iter.hasNext()) {
      Integer linkid = (Integer) iter.next();
      Link l = _g.getLink(linkid.intValue());
      double wgt = l.getWeight();
      // get node at starta
      Node en = _g.getNodeUnsynchronized(l.getStart());
      int en_part = -1;
      if (_graphPartition!=null) en_part = _graphPartition[l.getStart()];
      if (node_part!=en_part) continue;  // respect partition
      // check if en node is overweight
      if (en.getWeightValueUnsynchronized("cardinality").doubleValue()>=mac)
        continue;
			// check also if en belongs to a new coarse node, the weight of the new
			// coarse node
			Integer new_en_id = (Integer) _map.get(new Integer(en.getId()));
			if (new_en_id!=null) {
				Set bundle_ids = (Set) _rmap.get(new_en_id);
				double t_wgt = 0.0;
				Iterator bit = bundle_ids.iterator();
				while (bit.hasNext()) {
					Integer nid = (Integer) bit.next();
					Node n = _g.getNodeUnsynchronized(nid.intValue());
					t_wgt += n.getWeightValueUnsynchronized("cardinality").doubleValue();
				}
				if (t_wgt>=mac) continue;
			}
      // see if en also has an incoming arc from node
      Set en_inlinks = en.getInLinks();
      Iterator iter_en_in = en_inlinks.iterator();
      while (iter_en_in.hasNext()) {
        Integer lid = (Integer) iter_en_in.next();
        Link ll = _g.getLink(lid.intValue());
        if (ll.getStart()==node.getId()) {
          // found an arc in the other direction! Reinforcement
          wgt += ll.getWeight();
          break;
        }
      }
      double tot_wgt = wgt + getMappedNborsWgts(node, en, nodebors);
      if (tot_wgt<bestwgt) {
				// make sure ni doesn't have a neighbor mapped to some coarse node that
				// contains a fine node in fine_mwis set
				if (fineMWISFailsInCoarseGraph(en, fine_mwis)) continue;
        bestwgt = tot_wgt;
        bestnode = en;
      }
    }
    // now the same for outlinks (node is the start of the arc)
    iter = node.getOutLinks().iterator();
    while (iter.hasNext()) {
      Integer linkid = (Integer) iter.next();
      Link l = _g.getLink(linkid.intValue());
      double wgt = l.getWeight();
      // get node at enda
      Node en = _g.getNodeUnsynchronized(l.getEnd());
      int en_part = -1;
      if (_graphPartition!=null)
        en_part = _graphPartition[l.getEnd()];
      if (node_part!=en_part) continue;  // respect partition
      // check if en node is overweight
      if (en.getWeightValueUnsynchronized("cardinality").doubleValue()>=mac)
        continue;
			// check also if en belongs to a new coarse node, the weight of the new
			// coarse node
			Integer new_en_id = (Integer) _map.get(new Integer(en.getId()));
			if (new_en_id!=null) {
				Set bundle_ids = (Set) _rmap.get(new_en_id);
				double t_wgt = 0.0;
				Iterator bit = bundle_ids.iterator();
				while (bit.hasNext()) {
					Integer nid = (Integer) bit.next();
					Node n = _g.getNodeUnsynchronized(nid.intValue());
					t_wgt += n.getWeightValueUnsynchronized("cardinality").doubleValue();
				}
				if (t_wgt>=mac) continue;
			}
      // see if en also has an outgoing arc to node
      Set en_outlinks = en.getOutLinks();
      Iterator iter_en_out = en_outlinks.iterator();
      while (iter_en_out.hasNext()) {
        Integer lid = (Integer) iter_en_out.next();
        Link ll = _g.getLink(lid.intValue());
        if (ll.getEnd()==node.getId()) {
          // found an arc in the other direction! Reinforcement
          wgt += ll.getWeight();
          break;  // assume that there may exist at most one arc in
                  // a given direction connecting two nodes
        }
      }
      double tot_wgt = wgt + getMappedNborsWgts(node, en, nodebors);
      if (tot_wgt<bestwgt) {
				// make sure ni doesn't have a neighbor mapped to some coarse node that
				// contains a fine node in fine_mwis set
				if (fineMWISFailsInCoarseGraph(en, fine_mwis)) continue;
        bestwgt = tot_wgt;
        bestnode = en;
      }
    }

    return bestnode;
  }
	
	
	/**
	 * returns a string describing the size of the map of properties of this 
	 * object.
	 * @return String
	 */
  public String toString() {
    HashMap props = getProperties();
    String ret = "props=";
    if (props==null) ret+="null";
    else ret += props.size();
    return ret;
  }
	
	
	/**
	 * computes the total arc weight of all neighbor nodes of node that are mapped
	 * to the same coarse node as ni. 
	 * @param node Node
	 * @param ni Node
	 * @param nodebors Set  // Set&lt;Node&gt; the neighbors of node
	 * @return double the total weight of all arcs connecting neighbors of node 
	 * to other nodes that are mapped to the same coarse node as ni
	 */
	private double getMappedNborsWgts(Node node, Node ni, Set nodebors) {
		double tot_wgt = 0.0;
		Integer cnid = (Integer) _map.get(ni);
		if (cnid!=null) {
			Set fnids = (Set) _rmap.get(cnid);
			Iterator fnidsit = fnids.iterator();
			while (fnidsit.hasNext()) {
				Integer fnid = (Integer) fnidsit.next();
				Node fn = _g.getNodeUnsynchronized(fnid.intValue());
				if (fnid.intValue()==ni.getId()) 
					continue;  // don't count direct connection between node and ni
				if (nodebors.contains(fn)) {  // find out connecting arc weight
					// first, try in-links
					Set nodeinlinkids = node.getInLinks();
					Iterator nilidsit = nodeinlinkids.iterator();
					while (nilidsit.hasNext()) {
						Integer ilid = (Integer) nilidsit.next();
						Link l = _g.getLink(ilid.intValue());
						if (l.getStart()==fnid.intValue()) {
							tot_wgt += l.getWeight();
						}
					}
					// second, try out-links
					Set nodeoutlinkids = node.getOutLinks();
					Iterator nolidsit = nodeoutlinkids.iterator();
					while (nolidsit.hasNext()) {
						Integer olid = (Integer) nolidsit.next();
						Link l = _g.getLink(olid.intValue());
						if (l.getEnd()==fnid.intValue()) {
							tot_wgt += l.getWeight();
						}
					}
				}
			}  // while fnidsit has next
		}		
		return tot_wgt;
	}
	
	
	/**
	 * check whether the node ni has a neighbor mapped to another coarse node that
	 * contains one of the nodes of fine_mwis OR if any of the nodes ni is mapped
	 * together with, has a neighbor mapped to another coarse node that contains
	 * any of the nodes in fine_mwis.
	 * @param ni Node
	 * @param fine_mwis Set  // Set&lt;Integer&gt;
	 * @return boolean true iff the condition mentioned above, checks out
	 */
	private boolean fineMWISFailsInCoarseGraph(Node ni, Set fine_mwis) {
		if (fine_mwis==null) return false;
		Integer cnid = (Integer) _map.get(new Integer(ni.getId()));
		if (cnid==null) return false;
		Set ni_nbors = ni.getNborsUnsynchronized();
		// 1. first condition
		Iterator ni_nbors_it = ni_nbors.iterator();
		while (ni_nbors_it.hasNext()) {
			Node nnbor = (Node) ni_nbors_it.next();
			Integer cnnid = (Integer) _map.get(new Integer(nnbor.getId()));
			if (cnnid!=null) {
				Set cnn_f_nodeids = (Set) _rmap.get(cnnid);
				Iterator fit = cnn_f_nodeids.iterator();
				while (fit.hasNext()) {
					Integer fid = (Integer) fit.next();
					if (fine_mwis.contains(fid)) return true;
				}
			}
		}
		// 2. second condition
		Set together_w_ni_ids = (Set) _rmap.get(cnid);
		Iterator twni_it = together_w_ni_ids.iterator();
		while (twni_it.hasNext()) {
			Integer tid = (Integer) twni_it.next();
			if (tid.intValue()==ni.getId()) continue;  // ignore ni
			Node tn = _g.getNodeUnsynchronized(tid.intValue());
			Set tn_nbors = tn.getNborsUnsynchronized();  // Set<Node>
			Iterator tn_nbors_it = tn_nbors.iterator();
			while (tn_nbors_it.hasNext()) {
				Node tn_nbor = (Node) tn_nbors_it.next();
				Integer c_tnnbor_id = (Integer) _map.get(new Integer(tn_nbor.getId()));
				if (c_tnnbor_id==null) continue;
				Set together_c_node_ids = (Set) _rmap.get(c_tnnbor_id);
				if (intersects(together_c_node_ids,fine_mwis)) return true;
			}
		}
		return false;
	}
	
	
	private static boolean intersects(Set a, Set b) {
		if (a==null || b==null) throw new IllegalStateException("null arg?");
		Iterator bit = b.iterator();
		while (bit.hasNext()) {
			Integer bid = (Integer) bit.next();
			if (a.contains(bid)) return true;
		}
		return false;
	}

}

