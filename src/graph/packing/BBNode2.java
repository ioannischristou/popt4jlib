package graph.packing;

import graph.*;
import parallel.*;
import utils.*;
import popt4jlib.*;
import popt4jlib.LocalSearch.*;
import java.util.*;

/**
 * represents a node in the B&B tree of the hybrid B&B - GASP scheme for the
 * 2-packing problem. It implements the Comparable interface as
 * <CODE>BBNode2</CODE> objects enter a priority queue (<CODE>BBQueue</CODE>)
 * from which nodes are selected for processing (passing them to the
 * <CODE>BBThreadPool</CODE> that is a proxy for a
 * <CODE>parallel.FasterParallelAsynchBatchTaskExecutor</CODE> executor). It 
 * also implements the Runnable interface as the executor requires runnable 
 * objects to process.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class BBNode2 extends BBNodeBase {

  /**
   * Sole constructor of a BBNode2 object. Clients are not expected to
   * create such objects which are instead created dynamically through the
   * B&B process.
   * @param master BBTree the master BBTree object of which this is a node.
   * @param r Set Set<Node> the set of (graph) nodes to be added to the nodes
   * of the parent to represent a new partial solution.
   * @param parent BBNode2 the parent BB-node in the B&B tree construction process.
   * @throws PackingException if the second argument is non-null but the third
   * argument is null.
   */
  BBNode2(BBTree master, Set r, BBNode2 parent)
      throws PackingException {
		super(master, r, parent);
  }


  /**
   * the main method of the class, that processes the partial solution
   * represented by this object, and spawning and pushing into the
   * <CODE>BBQueue</CODE> queue new BB-node children if needed.
   */
  public void run() {
    try {
      boolean foundincumbent = false;
			int _id = getId();
			BBTree _master = getMaster();
			Set _nodes = getNodes();
      if (_id % 100 == 0) {
        System.err.println("running node id=" + _id + " init. size=" +
                           getCost() + " lvl="
                           + getLevel() + " bnd=" + getBound() + " qsize=" +
                           _master.getQueue().size()); // itc: HERE rm asap
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
      if (bnd <= _master.getBound()) {
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
			if (candidates==null || candidates.size()==0) {
				_master.incrementTotLeafNodes();  // node represents a maximal soln
			}
      // step 4.
      // check for incumbent
      if (_nodes.size() > _master.getBound()) {
        _master.setIncumbent(this);
        foundincumbent = true;
      }
      // step 5.
      // now that _nodes won't be further altered check if node exists in _recent
      if (_master.getQueue().addInRecent(this) == false) {
        setDone();
        return; // node is fathomed
      }
      // branch?
      if (candidates != null && candidates.size()>0) {
        try {
          List children = new ArrayList();
          Iterator it = candidates.iterator();
          int cnt_children = 0;
          while (it.hasNext()) {
            if (cnt_children++ > _master.getMaxChildrenNodesAllowed())break;
            Set ns = (Set) it.next();
            BBNode2 child = new BBNode2(_master, ns, this);
            // check if child's bound is better than incumbent
            double childbound = child.getBound();
            if (Math.floor(childbound) <= _master.getBound())
              continue;
            // speed up processing:
            // record new child incumbent if it exists (may be a partial soln
            // that can be further augmented in step 3 above when it is processed)
            if (child.getNodes().size() > _master.getBound()) {
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
                BBNode2 child = (BBNode2) children.get(i);
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
            try {
              // convert s to Set<Integer>
              Set nodeids = new IntSet();
              Iterator iter = _nodes.iterator();
              while (iter.hasNext()) {
                Node n = (Node) iter.next();
                Integer nid = new Integer(n.getId());
                nodeids.add(nid);
              }
              // now do the local search
              DLS dls = new DLS();
              AllChromosomeMakerIntf movesmaker = new
                  IntSetN1RXPFirstImprovingGraphAllMovesMakerMT(2);
              IntSetNeighborhoodFilterIntf filter = new
                  GRASPPackerIntSetNbrhoodFilter2(2);
              FunctionIntf f = new SetSizeEvalFunction();
              HashMap dlsparams = new HashMap();
              dlsparams.put("dls.movesmaker", movesmaker);
              dlsparams.put("dls.x0", nodeids);
              dlsparams.put("dls.numthreads", new Integer(10));  // itc: HERE parameterize asap
              dlsparams.put("dls.maxiters", new Integer(10)); // itc: HERE rm asap
              int n10 = _master.getGraph().getNumNodes()/10 + 1;
              dlsparams.put("dls.intsetneighborhoodmaxnodestotry", new Integer(n10));
              dlsparams.put("dls.graph", _master.getGraph());
              dlsparams.put("dls.intsetneighborhoodfilter", filter);
              //dlsparams.put("dls.createsetsperlevellimit", new Integer(100));
              dls.setParams(dlsparams);
              PairObjDouble pod = dls.minimize(f);
              Set sn = (Set) pod.getArg();
              if (sn != null && sn.size() > _nodes.size()) {
                _nodes.clear();
                Iterator sniter = sn.iterator();
                while (sniter.hasNext()) {
                  Integer id = (Integer) sniter.next();
                  Node n = _master.getGraph().getNode(id.intValue());
                  _nodes.add(n);
                }
                // record new incumbent
                _master.setIncumbent(this);
								_master.incrementTotLeafNodes();
              }
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
    BBNode2 o = (BBNode2) other;
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
    BBNode2 o = (BBNode2) other;
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
   * @param other Object expected another BBNode2 object
   * @return boolean return true iff the compareTo(other) method return 0.
   */
  public boolean equals(Object other) {
    /*
    BBNode2 o = (BBNode2) other;
    if (_id==o._id) return true;
    else return false;
    */
    int ct = compareTo(other);
    return (ct==0);
  }


  /**
   * returns the id of this BB-node.
   * @return int
   */
  public int hashCode() {
    // return _id;
    return getNodes().size();
  }

	
	double getCost() { return getNodes().size(); }
	

  private Set getForbiddenNodes() throws ParallelException {
    Set forbidden = new HashSet(getNodes());
    Iterator it = getNodes().iterator();
    while (it.hasNext()) {
      Node n = (Node) it.next();
      Set nnbors = n.getNNbors();
      forbidden.addAll(nnbors);
    }
    return forbidden;
  }


  /**
   * compute a max. possible number of nodes this soln can have
   * One such bound is _nodes.size() + (gsz - getForbiddenNodes().size())
   * A better bound is _nodes.size() + #unconnectednodes + #(remainingopennodes)/min.{NNbors.size()}
   * @return double
   *
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
   * compute a max. possible number of nodes this soln can have
   * One such bound is _nodes.size() + (gsz - getForbiddenNodes().size())
   * A better bound is _nodes.size() + #unconnectednodes + #(remainingopennodes)/min.{NNbors.size()}
   * An even better is to compute the NNborsOpen.size() for each node and divide
   * by this number+1 (so that the total nodes with only one open NNbor are divided by two
   * when considering the bound as a sum, and so on).
   * @return double
   */
  protected double getBound() throws ParallelException {
    if (_bound>=0) return _bound;  // cache
    if (getLevel()<getMaster().getTightenUpperBoundLvl()) {
      double res = getNodes().size();
      Set forbidden = getForbiddenNodes();
      res += (getMaster().getGraphSize()-forbidden.size());
      _bound = res;
      return res;
    } else {
      final Set forbidden = getForbiddenNodes();
      final int gsz = getMaster().getGraphSize();
      final Graph g = getMaster().getGraph();
      int[] discounted = new int[getMaster().getGraphMaxNNDegree()+1];  // reset to zero
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
        discounted[nborssz]++;
      }
      double res=getNodes().size();
      double minsz=-1.0;
      for (int i=0; i<discounted.length; i++) {
        if (i<=2) res += discounted[i] / (double) (i + 1.0);
        else {
          if (discounted[i]>0 && minsz<0) minsz = (double) i;
          res += discounted[i] / minsz;
        }
      }
      _bound = res;
      return res;
    }
  }


  final private int getLevel() {
    int lvl=0;
    for (BBNode2 p=this; p!=null; p=(BBNode2)p.getParent()) lvl++;
    return lvl;
  }


  /**
   * return Set<Set<Node> > of all maximal nodesets that can be added together
   * to the current active _nodes set.
   * @return Set
   */
  private Set getBestNodeSets2Add() throws ParallelException {
    int kmax = getMaster().getMaxAllowedItersInGBNS2A();
    Set ccands = getBestNodes2Add();
    Set result = new HashSet();  // Set<Set<Node> >
    Vector store = new Vector();
    Stack temp = new Stack();
    Iterator cands_it = ccands.iterator();
    while (cands_it.hasNext()) {
      Set ci = new HashSet();
      Node n = (Node) cands_it.next();
      ci.add(n);
      temp.push(ci);
    }
    if (getMaster().getUseMaxSubsets()==false) {
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
          Set ti = (Set) store.elementAt(i);
          if (ti.containsAll(t)) iscovered=true;
        }
        if (!iscovered) store.addElement(t);
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
        while (it.hasNext()) {
          Set c12 = new HashSet(c1);
          Set c2 = (Set) it.next();
          c12.addAll(c2);
          if (isFeas(c12)) {
            result.remove(c2);
            temp.add(c12);
            cons=false;
            break;
          }
        }
        if (cons) {
          // make sure you don't insert smth that already exists
          boolean iscovered=false;
          for (int i=0; i<store.size() && !iscovered; i++) {
            Set ti = (Set) store.elementAt(i);
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
   * return the Set<Node> that are the best node(s) to add given the current
   * active _nodes set. This is the set of nodes that are free to cover and
   * has the least number of "free" NNBors().
   * @return Set
   */
  private Set getBestNodes2Add() throws ParallelException {
    final int gsz=getMaster().getGraphSize();
    final double perc_extra_nodes2add = getMaster().getAvgPercExtraNodes2Add();
    Set best = new HashSet();
    double bestcost = Double.MAX_VALUE;
    Set forbidden = getForbiddenNodes();
    for (int i=0; i<gsz; i++) {
      Node ni = getMaster().getGraph().getNode(i);
      if (forbidden.contains(ni)==false) {
        // check for "free" nnbors
        // below is a valid but slow method to compute "free" nbors
        //Set ni_nnbors = new HashSet(ni.getNNbors());
        //ni_nnbors.removeAll(forbidden);
        //int nisize = ni_nnbors.size();
        Set ni_nnbors = ni.getNNbors();
        Iterator nnit = ni_nnbors.iterator();
        int nisize = 0;
        while (nnit.hasNext()) {
          Node nbor = (Node) nnit.next();
          if (forbidden.contains(nbor)==false) nisize++;
        }
        if (nisize<bestcost) {  // new best
          best.clear();
          best.add(ni);
          bestcost = nisize;
        }
        else if (nisize==bestcost) {  // equal to best, add to set
          best.add(ni);
        }
        // else continue;
      }
    }
    if (perc_extra_nodes2add>0) {
      double num_extra_nodes2add = best.size()*perc_extra_nodes2add;
      int lvl = getLevel();
      if (lvl>0) {
        // add probabilistically some non-best nodes depending on the level
        // of the node, the "goodness" of the node and the user-defined quantity
        for (int i = 0; i < gsz; i++) {
          Node ni = getMaster().getGraph().getNode(i);
          if (forbidden.contains(ni)) continue;
          Set ni_nnbors = ni.getNNbors();
          Iterator nnit = ni_nnbors.iterator();
          double nisize = 0;
          while (nnit.hasNext()) {
            Node nbor = (Node) nnit.next();
            if (forbidden.contains(nbor)==false) nisize++;
          }
          double fitness = nisize > 0 ? bestcost / nisize : 1;
          double prob = num_extra_nodes2add * fitness / (gsz * Math.sqrt(lvl));
          double ri = RndUtil.getInstance().getRandom().nextDouble();
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
   * @param active Set  // Set<Node>
   * @return boolean // true iff nj can be added to active
   */
  private boolean isFree2Cover(Node nj, Set active) throws ParallelException {
    if (active.contains(nj)) return false;
    Set nnborsj = new HashSet(nj.getNNbors());
    nnborsj.retainAll(active);
    if (nnborsj.size()>0) return false;
    return true;
  }


  private boolean isFeas(Set nodes) throws ParallelException {
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

