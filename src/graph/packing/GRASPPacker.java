package graph.packing;

import graph.*;
import utils.*;
import parallel.*;
import popt4jlib.*;
import popt4jlib.LocalSearch.*;
import java.util.*;
import java.io.*;

/**
 * Implements a fast GRASP algorithm for the 1- or 2-packing problem, well-known
 * combinatorial optimization problems. In the first case, node-weights are
 * allowed, so the problem is really the maximum weighted independent set.
 * Allows for Local-Search algorithms to kick-in after a solution has been
 * constructed.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class GRASPPacker {
  final private Graph _g;
  final private int _k;  // can be 1 or 2, indicates type of problem.
	private TreeSet _nodesq=null;  // TreeSet<Node>
  private TreeSet _origNodesq=null;  // TreeSet<Node>
	private double _alphaFactor=0.9;  // fudge factor to allow near-optimal
	                                  // nodes to enter the solution during
	                                  // construction


  /**
   * constructs an instance of the GRASPPacker algorithm. The Graph g passed in
	 * is not copied, but only a reference to the passed object stored.
   * @param g Graph
	 * @param k int the type of problem (can be 1 or 2)
   * @throws ParallelException
   */
  public GRASPPacker(Graph g, int k) throws ParallelException {
    _g = g;
		_k = k;
		if (_k==2) _g.makeNNbors();
    setup();
  }


  /**
   * create a dist-_k packing for Graph _g via a GRASP method and return it
   * as a Set&lt;Node&gt; of the active nodes.
   * @param addfirstfrom Set // Set&lt;Node&gt; to add from
   * @throws PackingException
   * @throws ParallelException
   * @return Set  // Set&lt;Node&gt;
   */
  public Set pack(Set addfirstfrom) throws PackingException, ParallelException {
    Set res = new HashSet();  // Set<Node>
		Random rand = RndUtil.getInstance().getRandom();
    if (addfirstfrom!=null) {
      Iterator it = addfirstfrom.iterator();
      while (it.hasNext()) {
        Node n = (Node) it.next();
        if (isFree2Cover(n, res)) {
          res.add(n);
          updateQueue(n);
        }
      }
    }
    boolean cont = true;
    while (cont) {
      cont = false;
      // 1. get the first valid element(s) from the queue
      Set candidates = new TreeSet();  // Set<Node>
      Iterator it = _nodesq.iterator();
      if (it.hasNext() == false) break;
      Node first = (Node) it.next();
      candidates.add(first);
      //System.err.println("1. Adding first=" + first.getId() +
      //                   " to candidates set w/ card=" + first.getNNbors().size());
      Node n = null;
      if (it.hasNext()) {
        while (it.hasNext()) {
          n = (Node) it.next();
          if ((_k==2 && n.getNNbors().size() == first.getNNbors().size()) ||
							(_k==1 && isApproximatelyEqual(n,first))) {
						// 2nd condition used to be:
						// (_k==1 && n.getNbors().size() == first.getNbors().size())
            candidates.add(n);
            continue; // keep adding
          }
          else if (_k==2 || !isApproximatelyEqualWeight(n,first)) break;
        }  // while it.hasNext()
        System.err.println("candidates.size()=" + candidates.size());
        // pick a candidate node at random
        int pos = rand.nextInt(candidates.size());
        Iterator it2 = candidates.iterator();
        for (int i = 0; i < pos; i++) it2.next();
        n = (Node) it2.next();
        // 2. update the queue
        updateQueue(n);
        // 3. add the element to the result set
        res.add(n);
        System.err.println("added n=" + n.getId() + " res.size()=" +
                           res.size() + " _nodesq.size()=" + _nodesq.size());
        cont = true;
      }
      else {
        if (isFree2Cover(first, res))
          res.add(first);
      }
    }
    reset();  // return _nodesq to original situation
    return res;
  }
	
	
	/**
	 * set the alpha factor of the GRASP process. Should be called before the call
	 * to <CODE>pack()</CODE>.
	 * @param a double
	 */
	public void setAlphaFactor(double a) {
		_alphaFactor = a;
	}


  /**
   * the method <CODE>g.makeNNbors()</CODE> must have been called prior to this
	 * call if k==2.
   * @param g Graph
   * @param active Set // Set&lt;Integer nodeid&gt;
	 * @param k int // denotes 1- or 2-packing problem
   * @return boolean
   */
  static boolean isFeasible(Graph g, Set active, int k) {
		if (active==null) return true;
		if (k==1) {
			Iterator it = active.iterator();
			while (it.hasNext()) {
				Integer nid = (Integer) it.next();
				Node n = g.getNodeUnsynchronized(nid.intValue());
				Set nbors = n.getNborsUnsynchronized();  // used to be n.getNbors();
				Iterator it2 = nbors.iterator();
				while (it2.hasNext()) {
					Node n2 = (Node) it2.next();
					if (active.contains(new Integer(n2.getId()))) return false;
				}
			}
			return true;
		}
		final int gsz = g.getNumNodes();
    for (int i=0; i<gsz; i++) {
      Node nn = g.getNode(i);
			Set nnbors = nn.getNbors();  // Set<Node>
			int count=0;
			if (active.contains(new Integer(i))) count=1;
			Iterator it2 = active.iterator();
			while (it2.hasNext()) {
				Integer nid2 = (Integer) it2.next();
				Node n2 = g.getNode(nid2.intValue());
				if (nnbors.contains(n2)) {
					++count;
					if (count>1) return false;
				}
			}
    }
    return true;
  }


  /**
   * check if node nj can be set to one when the nodes in active are also set.
   * @param nj Node
   * @param active Set  // Set&lt;Node&gt;
   * @return boolean // true iff nj can be added to active
   * @throws ParallelException
   */
  private boolean isFree2Cover(Node nj, Set active) throws ParallelException {
    if (active.contains(nj)) return false;
    if (_k==2) {
			Set nnborsj = new HashSet(nj.getNNbors());
			nnborsj.retainAll(active);
			if (nnborsj.size()>0) return false;
			return true;
		} else {  // _k==1
			/* slow
			Set nborsj = new HashSet(nj.getNbors());
			nborsj.retainAll(active);
			if (nborsj.size()>0) return false;
			return true;
			*/
			// /* faster: no need for HashSet creation
			Set nborsj = nj.getNborsUnsynchronized();  // no modification takes place
			Iterator itj = nborsj.iterator();
			while (itj.hasNext()) {
				Node nnj = (Node) itj.next();
				if (active.contains(nnj)) return false;
			}
			return true;
			// */
		}
  }


	/**
	 * checks whether the node n is nearly equal to the currently "best" node
	 * to enter the partial solution of a 1-packing problem (MWIS problem).
	 * @param n Node
	 * @param first Node
	 * @return true iff the two nodes are "nearly" equal (n's weight is no worse
	 * than the first's times the fudge factor and the nbors' sizes are at least
	 * equal to the first's times the same fudge factor)
	 */
	private boolean isApproximatelyEqual(Node n, Node first) {
		Double nwD = n.getWeightValueUnsynchronized("value");
		double nw = nwD==null ? 1.0 : nwD.doubleValue();
		Double fwD = first.getWeightValueUnsynchronized("value");
		double fw = fwD==null ? 1.0 : fwD.doubleValue();
		return  fw*_alphaFactor <= nw &&
			      first.getNborsUnsynchronized().size()*_alphaFactor >= 
			      n.getNborsUnsynchronized().size();
	}


	/**
	 * check whether node n is "close enough" to the first (and "best") node
	 * according to the node-weight criterion for the MWIS problem.
	 * @param n Node
	 * @param first Node
	 * @return true iff the two nodes are "nearly" equal (n's weight is no worse
	 * than the first's times the fudge factor <CODE>_alphaFactor</CODE>).
	 */
	private boolean isApproximatelyEqualWeight(Node n, Node first) {
		Double nwD = n.getWeightValueUnsynchronized("value");
		double nw = nwD==null ? 1.0 : nwD.doubleValue();
		Double fwD = first.getWeightValueUnsynchronized("value");
		double fw = fwD==null ? 1.0 : fwD.doubleValue();
		return fw*_alphaFactor <= nw;
	}


  private void updateQueue(Node n) throws PackingException, ParallelException {
    // 0. remove the node n and the nnbors of n from _nodesq
    _nodesq.remove(n);
    Set nnbors = _k==2 ? n.getNNbors() : n.getNborsUnsynchronized();  // used to be n.getNbors();
    _nodesq.removeAll(nnbors);
		if (_k==2) {  // itc-20170307: the code in the if is only useful when _k=2
			// 1. create the nnnbors set of the nbors of _nnbors U n set
			Set nnnbors = new HashSet();  // Set<Node>
			Set nbors = n.getNborsUnsynchronized();  // itc-20170307: used to be getNbors()
			Iterator it = nbors.iterator();
			while (it.hasNext()) {
				Node nbor = (Node) it.next();
				Set nnbors2 = _k==2 ? nbor.getNNbors() : nbor.getNborsUnsynchronized();  // used to be nbor.getNbors();
				nnnbors.addAll(nnbors2);
			}
			nnnbors.removeAll(nnbors);
			nnnbors.remove(n);
			nnnbors.retainAll(_nodesq);  // don't accidentally insert back bad nodes
			// 2. remove the nnnbors nodes from the _nodesq set and re-insert them
			// (which updates correctly the _nodesq TreeSet in case of _k==2)
			// nnnbors are all the nodes at distance 3 from the node n for _k==2, distance 2 for _k==1
			// Update the _nnbors data member of those nodes.
			_nodesq.removeAll(nnnbors);
			//if (_k==2) {
				Iterator it2 = nnnbors.iterator();
				while (it2.hasNext()) {
					Node nb = (Node) it2.next();
					nb.getNNbors().removeAll(nnbors);
					nb.getNNbors().remove(n);
				}
			//}
			_nodesq.addAll(nnnbors);
			//if (_k==2) 
			  nnbors.clear();  // clear n's NNbors
		}
  }


  /**
   * return true iff all nodes in active set can be set to one without
   * violating feasibility.
   * @param active Set  // Set&lt;Node&gt;
   * @return boolean
   * @throws ParallelException
   */
  private boolean isFeasible(Set active) throws ParallelException {
    if (_k==2) {
			_g.makeNNbors();  // re-establish nnbors
	    final int gsz = _g.getNumNodes();
		  for (int i=0; i<gsz; i++) {
			  Node nn = _g.getNode(i);
				Set nnbors = nn.getNbors();  // Set<Node>
	      int count=0;
		    if (active.contains(nn)) count=1;
			  Iterator it2 = active.iterator();
				while (it2.hasNext()) {
					Node n2 = (Node) it2.next();
	        if (nnbors.contains(n2)) {
		        ++count;
			      if (count>1) return false;
				  }
				}
			}
			return true;
		}
		else {  // _k==1
			// _g.makeNbors(true);  // no need to re-establish nbors: never modified
			Iterator it = active.iterator();
			while (it.hasNext()) {
				Node n1 = (Node) it.next();
				Set n1bors = n1.getNborsUnsynchronized();
				Iterator it2 = n1bors.iterator();
				while (it2.hasNext()) {
					Node n1nbor = (Node) it2.next();
					if (active.contains(n1nbor)) return false;
				}
			}
			return true;
		}
  }


  /**
   * the method <CODE>_g.makeNNbors()</CODE> must have been called before if
	 * <CODE>_k==2</CODE>. This method is only called once from this object's
	 * constructor.
   */
  private void setup() {
    final int gsz = _g.getNumNodes();
    if (_k==2) {
			NodeComparator2 comp = new NodeComparator2();
			_origNodesq = new TreeSet(comp);
		} else { // _k==1
			NodeComparator4 comp = new NodeComparator4();
			_origNodesq = new TreeSet(comp);
		}
    for (int i=0; i<gsz; i++) {
      _origNodesq.add(_g.getNodeUnsynchronized(i));  // used to be _g.getNode(i)
    }
    _nodesq = new TreeSet(_origNodesq);
    //System.err.println("done sorting");
  }


  /**
   * called at the end of a pack() method invocation (only)
   * @throws ParallelException
   */
  private void reset() throws ParallelException {
		if (_k==2) _g.makeNNbors(true); // force reset (from cache)
		// else _g.makeNbors(true);  // don't force reset (from cache): no need
    _nodesq = new TreeSet(_origNodesq);
  }


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; graph.packing.GRASPPacker 
	 * &lt;graph_file&gt; &lt;k&gt; 
	 * [numinitnodes(0)] [numiterations(1)] 
	 * [do_local_search(false)] [dls_num_threads(1)] [max_allowed_time(ms)(+Inf)] 
	 * [use_N2RXP_4_DLS(false)]</CODE>.
   * The graph_file contains the graph in the format specified in the
   * comments of method <CODE>utils.DataMgr.readGraphFromFile2(filename)</CODE>
   * and the numinitnodes is an optional number specifying how many (non-conflicting)
   * nodes to choose randomly as an initial seed-set to grow from (default is zero).
   * The solution found is written in the file "sol.out" in the current directory.
   * It contains one line for each node included in the solution, and the line
   * has the internal id of the node +1 (so the range of nodes is
   * [1,...Graph.getNumNodes()].
   * @param args String[] are as follows:
	 * <ul>
	 * <li> args[0] graph_file_name mandatory, the name of the file containing the graph
	 * <li> args[1] k mandatory, must be 1 or 2, describing the type of the problem
	 * <li> args[2] numinitnodes optional, if present describes the cardinality of an
	 * initial random population from which to attempt to construct a first partial
	 * solution. Default is 0.
	 * <li> args[3] numiterations optional, if present describes the number of major
	 * outer iterations to run. Default is 1.
	 * <li> args[4] do_local_search optional, if "true" implies a local search via
	 * the <CODE>popt4jlib.LocalSearch.DLS</CODE> algorithm to be performed when a
	 * solution is constructed in a major outer iteration. Default is false.
	 * <li> args[5] num_dls_threads optional, if present describes the number of threads
	 * to use in the DLS procedure if activated. Default is 1.
	 * <li> args[6] max_allowed_time optional, if present, sets the max. allowed time
	 * for the process to run (in milliseconds). Default is +infinity.
	 * <li> args[7] use_N2RXP_4_DLS optional, if present and true, will force
	 * the use of the <CODE>IntSetN2RXPGraphAllMovesMaker</CODE> move-maker in the
	 * local search (if args[4] evaluates to true). Otherwise, if local-search is
	 * asked for, the <CODE>IntSetN1RXPFirstImprovingGraphAllMovesMaker</CODE>
	 * class will be used as moves-maker in local-search. Default is false.
	 * </ul>
   * @throws ParallelException
   */
  public static void main(String[] args) throws ParallelException {
		if (args.length < 2) {
			System.err.println("usage: java -cp <classpath> <graphfile> <k> "+
				                 "[numinitnodes] [numiterations] "+
				                 "[do_local_search] [num_dls_threads] "+
				                 "[max_allowed_time_millis] [use_N2RXP_4_DLS]");
			System.exit(-1);
		}
    try {
      long st = System.currentTimeMillis();
      Graph g = DataMgr.readGraphFromFile2(args[0]);
			int k = Integer.parseInt(args[1]);
      double best = 0;
      boolean do_local_search = false;
			boolean use_N2RXP_4_ls = false;
      int num_threads = 1;
			long max_time_ms = Long.MAX_VALUE;  // indicates the max allowed time to run (in millis)
      GRASPPacker p = new GRASPPacker(g,k);
      Set init=null;
      int num_iters = 1;
      if (args.length>2) {
        int numinit = 0;
        try {
          numinit = Integer.parseInt(args[2]);
          if (numinit<0) numinit=0;  // ignore wrong option value and continue
        }
        catch (ClassCastException e) {
          e.printStackTrace();  // ignore wrong option value and continue
        }
        Graph gp = p._g;
        int gsz = gp.getNumNodes();
        init = k==2 ? new TreeSet(new NodeComparator2()) : new TreeSet(new NodeComparator4());
				Random rnd = RndUtil.getInstance().getRandom();
        for (int i=0; i<numinit; i++) {
          int nid = rnd.nextInt(gsz);
          Node n = gp.getNodeUnsynchronized(nid);
          init.add(n);
        }
        if (args.length>3) {
          try {
            num_iters = Integer.parseInt(args[3]);
            if (num_iters<0) num_iters = 0;
          }
          catch (ClassCastException e) {
            e.printStackTrace();  // ignore wrong option value and continue
          }
          if (args.length>4) {
            do_local_search = "true".equals(args[4]);
            if (args.length>5) {
              try {
                num_threads = Integer.parseInt(args[5]);
								if (args.length>6) {
									try {
										max_time_ms = Long.parseLong(args[6]);
									}
									catch (Exception e) {
										e.printStackTrace();
									}
									if (args.length>7)
										use_N2RXP_4_ls = "true".equals(args[7]);
								}
              }
              catch (Exception e) {
                e.printStackTrace();  // ignore wrong option and continue
              }
            }
          }
        }
      }
      Set best_found = null;
			if (max_time_ms<0)  // -1 or any negative value indicates +Inf
				max_time_ms = Long.MAX_VALUE;
			boolean cont = true;
			TimerThread t = new TimerThread(max_time_ms, cont);
			t.start();
      for (int i=0; i<num_iters && t.doContinue(); i++) {
				System.err.println("GRASPPacker: starting iteration "+i);
        Set s = p.pack(init); // Set<Node>
        if (do_local_search) {
          // convert s to Set<Integer>
          Set nodeids = new IntSet();
          Iterator iter = s.iterator();
          while (iter.hasNext()) {
            Node n = (Node) iter.next();
            Integer nid = new Integer(n.getId());
            nodeids.add(nid);
          }
          // now do the local search
          DLS dls = new DLS();
          AllChromosomeMakerIntf movesmaker;
					if (use_N2RXP_4_ls) movesmaker = new IntSetN2RXPGraphAllMovesMaker(k);
					else movesmaker = new IntSetN1RXPFirstImprovingGraphAllMovesMakerMT(k);
          IntSetNeighborhoodFilterIntf filter = new GRASPPackerIntSetNbrhoodFilter2(k);
          //FunctionIntf f = k==2 ? new SetSizeEvalFunction() : new SetWeightEvalFunction(g);
          FunctionIntf f;
          if (k==2) f = new SetSizeEvalFunction();
          else f = new SetWeightEvalFunction(g);
          HashMap dlsparams = new HashMap();
          dlsparams.put("dls.movesmaker",movesmaker);
          dlsparams.put("dls.x0", nodeids);
          dlsparams.put("dls.numthreads", new Integer(num_threads));
          dlsparams.put("dls.maxiters", new Integer(10));   // itc: HERE rm asap
          dlsparams.put("dls.graph", g);
          dlsparams.put("dls.intsetneighborhoodfilter", filter);
          //dlsparams.put("dls.createsetsperlevellimit", new Integer(100));
          dls.setParams(dlsparams);
          PairObjDouble pod = dls.minimize(f);
          Set sn = (Set) pod.getArg();
          if (sn!=null) {
            s.clear();
            Iterator sniter = sn.iterator();
            while (sniter.hasNext()) {
              Integer id = (Integer) sniter.next();
              Node n = g.getNode(id.intValue());
              s.add(n);
            }
          }
        }
        int iter_best = s.size();
        double iter_w_best = 0.0;
        Iterator it = s.iterator();
        while (it.hasNext()) {
          Node n = (Node) it.next();
          Double nwD = n.getWeightValueUnsynchronized("value");  // used to be n.getWeightValue("value");
          double nw = nwD==null ? 1.0 : nwD.doubleValue();
          iter_w_best += nw;
        }
        System.err.println("GRASPPacker.main(): iter: "+i+": soln size found="+iter_best+" soln weight="+iter_w_best);
        if (iter_w_best > best) {
          best_found = s;
          best = iter_w_best;
        }
      }
      long tot = System.currentTimeMillis()-st;
      System.out.println("Final Best soln found="+best+" total time="+tot+" (msecs)");
      if (p.isFeasible(best_found)) {
        System.out.println("feasible soln: "+printNodes(best_found));
      }
      else System.err.println("infeasible soln");
      // write solution to file
      PrintWriter pw = new PrintWriter(new FileWriter("sol.out"));
      pw.println(best);
      Iterator it = best_found.iterator();
      while (it.hasNext()) {
        Node n = (Node) it.next();
        pw.println((n.getId()+1));
      }
      pw.flush();
      pw.close();
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }


  /* debug routine */
  private static String printNodes(Set s) {
    String res = "[";
    Iterator it = s.iterator();
    while (it.hasNext()) {
      Node n = (Node) it.next();
      res += n.getId();
      if (it.hasNext()) res+= ", ";
    }
    res += "]";
    return res;
  }

}


/**
 * auxiliary class comparing <CODE>graph.Node</CODE> objects, for use with the 
 * <CODE>graph.packing.GRASPPacker</CODE> class, not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class NodeComparator2 implements Comparator, Serializable {
  private final static long serialVersionUID = 42828339182837662L;

  public int compare(Object o1, Object o2) {
    Node n1 = (Node) o1;
    Node n2 = (Node) o2;
    try {
      int n1sz = n1.getNNbors().size();
      int n2sz = n2.getNNbors().size();
      if (n1sz < n2sz)return -1;
      else if (n1sz == n2sz) {
        int n1s = n1.getNbors().size();
        int n2s = n2.getNbors().size();
        if (n1s < n2s)return -1;
        else if (n1s == n2s) {
          if (n1.getId() < n2.getId())return -1;
          else if (n1.getId() == n2.getId())return 0;
          else return 1;
        }
        else return 1;
      }
      else return 1;
    }
    catch (ParallelException e) {
      e.printStackTrace();  // will get here only if the current thread is a
      //reader and there is another thread currently
      // owning the read-lock of n1 or n2 in which case the comparison result
      // will be wrong
      return 0;
    }
  }
}


/**
 * auxiliary class comparing <CODE>graph.Node</CODE> objects, for use with the 
 * <CODE>graph.packing.GRASPPacker</CODE> class, not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class NodeComparator3 implements Comparator, Serializable {
  private static final long serialVersionUID = 359797557701624091L;

  public int compare(Object o1, Object o2) {
    Node n1 = (Node) o1;
    Node n2 = (Node) o2;
    try {
      int n1sz = n1.getNNbors().size();
      int n2sz = n2.getNNbors().size();
      if (n1sz < n2sz)return -1;
      else if (n1sz == n2sz) {
        if (n1.getId() < n2.getId())return -1;
        else if (n1.getId() == n2.getId())return 0;
        else return 1;
      }
      else return 1;
    }
    catch (ParallelException e) {
      e.printStackTrace();  // will get here only if the current thread is a
      //reader and there is another thread currently
      // owning the read-lock of n1 or n2 in which case the comparison result
      // will be wrong
      return 0;
    }
  }
}


/**
 * auxiliary class comparing <CODE>graph.Node</CODE> objects, for use with the 
 * <CODE>graph.packing.GRASPPacker</CODE> class, not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class NodeComparator4 implements Comparator, Serializable {
  // private static final long serialVersionUID = ...L;

	/**
	 * the Node with the heaviest weight wins. Ties are broken according to weight
	 * of neighbors (lightest neighbors win). Ties at this final level are broken
	 * by Node-id in the containing Graph.
	 * @param o1 Object  // Node
	 * @param o2 Object  // Node
	 * @return int -1 if o1 is "smaller" than o2, 0 if equal, +1 else.
	 */
  public int compare(Object o1, Object o2) {
    Node n1 = (Node) o1;
    Node n2 = (Node) o2;
		Double n1wD = n1.getWeightValueUnsynchronized("value");  // used to be n1.getWeightValue("value");
		Double n2wD = n2.getWeightValueUnsynchronized("value");  // used to be n2.getWeightValue("value);
		double n1w = n1wD == null ? 1.0 : n1wD.doubleValue();
		double n2w = n2wD == null ? 1.0 : n2wD.doubleValue();
		if (n1w>n2w) return -1;
		else if (Double.compare(n2w, n1w)==0) {
			double n1sz = n1.getNborsUnsynchronized().size();
			double n2sz = n2.getNborsUnsynchronized().size();
			try {
				double n1szaux = n1.getNborWeights("value");
				double n2szaux = n2.getNborWeights("value");
				n1sz = n1szaux;
				n2sz = n2szaux;
			}
			catch (ParallelException e) {
				e.printStackTrace();
			}
			if (n1sz < n2sz) return -1;
			else if (n1sz == n2sz) {
				if (n1.getId() < n2.getId())return -1;
				else if (n1.getId() == n2.getId())return 0;
				else return 1;
			}
			else return 1;
		}
		else return 1;
  }
}


/**
 * auxiliary class comparing <CODE>graph.Node</CODE> objects, for use with the 
 * <CODE>graph.packing.GRASPPacker</CODE> class, not part of the public API.
 * Implements the opposite strategy of <CODE>NodeComparator4</CODE>.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class NodeComparator_4 implements Comparator, Serializable {
  // private static final long serialVersionUID = ...L;

	/**
	 * the Node with the lightest weight wins. Ties are broken according to weight
	 * of neighbors (heaviest neighbors win). Ties at this final level are broken
	 * by Node-id in the containing Graph.
	 * @param o1 Object  // Node
	 * @param o2 Object  // Node
	 * @return int -1 if o1 is "smaller" than o2, 0 if equal, +1 else.
	 */
  public int compare(Object o1, Object o2) {
    Node n1 = (Node) o1;
    Node n2 = (Node) o2;
		Double n1wD = n1.getWeightValueUnsynchronized("value");  // used to be n1.getWeightValue("value");
		Double n2wD = n2.getWeightValueUnsynchronized("value");  // used to be n2.getWeightValue("value);
		double n1w = n1wD == null ? 1.0 : n1wD.doubleValue();
		double n2w = n2wD == null ? 1.0 : n2wD.doubleValue();
		if (n1w<n2w) return -1;
		else if (Double.compare(n2w, n1w)==0) {
			double n1sz = n1.getNborsUnsynchronized().size();
			double n2sz = n2.getNborsUnsynchronized().size();
			try {
				double n1szaux = n1.getNborWeights("value");
				double n2szaux = n2.getNborWeights("value");
				n1sz = n1szaux;
				n2sz = n2szaux;
			}
			catch (ParallelException e) {
				e.printStackTrace();
			}
			if (n1sz > n2sz) return -1;
			else if (n1sz == n2sz) {
				if (n1.getId() < n2.getId())return -1;
				else if (n1.getId() == n2.getId())return 0;
				else return 1;
			}
			else return 1;
		}
		else return 1;
  }
}
