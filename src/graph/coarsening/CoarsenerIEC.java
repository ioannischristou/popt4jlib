package graph.coarsening;

import graph.*;
import parallel.*;
import popt4jlib.VectorIntf;  // for coarse nodes' centers in Vector Space
import popt4jlib.DblArray1Vector;  // double[] impl of VectorIntf
import java.util.*;


/**
 * edge-based coarsener. Class is not thread-safe, and so should be protected
 * by clients when used in a multi-threaded context.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 2.0
 */
public class CoarsenerIEC extends Coarsener {

	/**
	 * single public constructor.
	 * @param g Graph
	 * @param partition int[]
	 * @param props HashMap
	 */
  public CoarsenerIEC(Graph g, int[] partition, HashMap props) {
    super(g, partition, props);
  }


	/**
	 * implementation of base class method, this factory method constructs a new 
	 * instance of this class.
	 * @param g Graph
	 * @param partition int[]
	 * @param properties HashMap
	 * @return Coarsener  // CoarsenerIEC
	 */
  public Coarsener newInstance(Graph g, int[] partition, HashMap properties) {
    return new CoarsenerIEC(g, partition, properties);
  }


  /**
   * coarsen() performs the following loop until the number of unmatched
   * nodes is less than a ratio of the original nodes:
   * (1) we visit an unvisited node randomly;
   * (2) we match it with the neighbor that yields the maximum value of
   *     arcs connecting weight * lamda + 
	 *                     (1-lamda)*sum_of_weights_of_arcs_conn_common_nbors
   * (3) we mark as visited both nodes that were matched of course.
	 * The result is a new "coarse" graph that can be accessed by a call to 
	 * <CODE>getCoarseGraph()</CODE> (after this method completes).
	 * Notice that the pair &lt;"ratio",Double val&gt; must be in the 
	 * <CODE>_properties</CODE> map of this object, defining the ratio of coarse
	 * nodes over fine nodes that must be reached for the process to stop. Same 
	 * for the pair &lt;"max_allowed_card",Integer num&gt; that specifies the 
	 * maximum cardinality allowed for any "fine-level" node in order to be 
	 * considered for coarsening with other nodes. Same for the pair 
	 * &lt;"lamda",Double val&gt; that specifies the value lambda in the 
	 * expression in step (2) above.
   * @throws GraphException
   * @throws CoarsenerException if coarsening couldn't proceed satisfactorily,
	 * ie couldn't reach desired compression ratio as specified in "ratio" key's
	 * value in properties
   */
  public void coarsen() 
		throws GraphException, CoarsenerException, ParallelException {
    System.err.println("CoarsenerIEC.coarsen() entered");
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
    int j;
    // main loop
    for (j=0; j<l.size(); j++) {
      // check to see if coarsening has exceeded thresholds
      if (new_nodes+_g.getNumNodes()-num_seen<=min_allowed_nodes)
        break;
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
				          "CoarsenerIEC.coarsen(): cannot proceed further");
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

    // if there is a mapping for each node in _g to a Document, compute
    // the Document that represents the centroid for each of the new
    // coarse nodes and put it into _properties under the name
    // "coarseNodeDocumentArray"
    VectorIntf[] node_document_array = (VectorIntf[])
        getProperty("nodeDocumentArray");
    if (node_document_array != null) {
      VectorIntf[] coarse_doc_array = new DblArray1Vector[new_nodes];
      for (int i=0; i<new_nodes; i++) {
        Vector c_i = new Vector();  // Vector<Document>
        Set ri = (Set) _rmap.get(new Integer(i));
        Iterator riter = ri.iterator();
        while (riter.hasNext()) {
          Integer rin = (Integer) riter.next();
          VectorIntf drin = node_document_array[rin.intValue()];
          c_i.addElement(drin);
        }
        try {
          VectorIntf c_i_new = popt4jlib.GradientDescent.VecUtil.getCenter(c_i);
          coarse_doc_array[i] = c_i_new;
        }
        catch (Exception ce) {
          ce.printStackTrace();
          throw new CoarsenerException(
						          "failed to create coarseNodeDocumentArray");
        }
      }
      setProperty("coarseNodeDocumentArray", coarse_doc_array);
    }
    else System.err.println("coarsen():nodeDocumentArray null or not found...");

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

    // System.err.println("coarsen(): _coarseG.numnodes="+getCoarseGraph().getNumNodes());
    System.err.println("CoarsenerIEC.coarsen() finished");
  }


  /**
   * find the best neighbor that should be matched with node at position pos.
   * The best neighbor is the one maximizing the value
   * lamda*conn_arcs_weight + (1-lamda)*conn_nbors_weight
   * where the second term takes into account the common nbors of the two
   * neighboring nodes, and sums the weights along their connecting arcs
   * (second order effect) assuming partition constraints and cardinality 
	 * constraints are satisfied.
   * @param pos Integer
   * @return Node
	 * @throws NullPointerException if a value for the key "lamda" (not "lambda")
	 * or for the key "max_allowed_card" is not present in the properties map of 
	 * this object.
   */
  private Node getBestNbor(Integer pos) {
    Node node = _g.getNodeUnsynchronized(pos.intValue());
    int node_part = -1;
    if (_graphPartition != null)
      node_part = _graphPartition[pos.intValue()];
    Node bestnode = null;
    final double lamda = ((Double) getProperty("lamda")).doubleValue();
		final double mac = ((Double)getProperty("max_allowed_card")).doubleValue();
    Link bestlink = null;
    double bestwgt = 0.0;
    // first work with node's inlinks (node is the end of the arc)
    Iterator iter = node.getInLinks().iterator();
    while (iter.hasNext()) {
      Integer linkid = (Integer) iter.next();
      Link l = _g.getLink(linkid.intValue());
      double wgt = l.getWeight();
      // get node at starta
      Node en = _g.getNodeUnsynchronized(l.getStart());
      int en_part = -1;
      if (_graphPartition!=null)
        en_part = _graphPartition[l.getStart()];
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
      // get common neighbors
      Set en_nbors = new HashSet(en.getNborsUnsynchronized());
      en_nbors.retainAll(node.getNborsUnsynchronized());
      Iterator cniter = en_nbors.iterator();
      double conn_wgt = 0.0;
      while (cniter.hasNext()) {
        Node nn = (Node) cniter.next();
        // find the sum of weights with which nn connects to both node and en
        Iterator nn_ins = nn.getInLinks().iterator();
        int nodeid = node.getId();
        int enid = en.getId();
        while (nn_ins.hasNext()) {
          Integer lid = (Integer) nn_ins.next();
          Link nnl = _g.getLink(lid.intValue());
          int nnls = nnl.getStart();
          if (nnls==nodeid || nnls==enid)
            conn_wgt += nnl.getWeight();
        }
        // same for outlinks
        Iterator nn_outs = nn.getOutLinks().iterator();
        while (nn_outs.hasNext()) {
          Integer lid = (Integer) nn_outs.next();
          Link nnl = _g.getLink(lid.intValue());
          int nnle = nnl.getEnd();
          if (nnle==nodeid || nnle==enid)
            conn_wgt += nnl.getWeight();
        }
      }
      double tot_wgt = lamda*wgt + (1.0-lamda)*conn_wgt;
      if (tot_wgt>bestwgt) {
        bestwgt = tot_wgt;
        bestlink = l;
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
      // get common neighbors
      Set en_nbors = new HashSet(en.getNborsUnsynchronized());
      en_nbors.retainAll(node.getNborsUnsynchronized());
      Iterator cniter = en_nbors.iterator();
      double conn_wgt = 0.0;
      while (cniter.hasNext()) {
        Node nn = (Node) cniter.next();
        // find the sum of weights with which nn connects to both node and en
        Iterator nn_ins = nn.getInLinks().iterator();
        int nodeid = node.getId();
        int enid = en.getId();
        while (nn_ins.hasNext()) {
          Integer lid = (Integer) nn_ins.next();
          Link nnl = _g.getLink(lid.intValue());
          int nnls = nnl.getStart();
          if (nnls==nodeid || nnls==enid)
            conn_wgt += nnl.getWeight();
        }
        // same for outlinks
        Iterator nn_outs = nn.getOutLinks().iterator();
        while (nn_outs.hasNext()) {
          Integer lid = (Integer) nn_outs.next();
          Link nnl = _g.getLink(lid.intValue());
          int nnle = nnl.getEnd();
          if (nnle==nodeid || nnle==enid)
            conn_wgt += nnl.getWeight();
        }
      }

      double tot_wgt = lamda*wgt + (1.0-lamda)*conn_wgt;
      if (tot_wgt>bestwgt) {
        bestwgt = tot_wgt;
        bestlink = l;
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
}

