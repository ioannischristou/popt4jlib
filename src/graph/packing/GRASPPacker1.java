package graph.packing;

import graph.*;
import utils.*;
import parallel.*;
import popt4jlib.*;
import popt4jlib.LocalSearch.*;
import java.util.*;
import java.io.*;

/**
 * Implements a fast GRASP algorithm for the 1-packing problem, well-known MWIS
 * combinatorial optimization problems. The algorithm is truly adaptive in that
 * in every GRASP iteration, the cost of each available node is updated to 
 * reflect the fact that its "free" neighbors are only those that have not been
 * already de-activated by other nodes that have entered the current partial 
 * solution. The actual cost of a node at any point is the node's number of free
 * neighbors over the node's weight (the latter doesn't change). Nodes with 
 * least cost are selected first.
 * Allows for Local-Search algorithms to kick-in after a solution has been 
 * constructed.
 * Notice that the methods of this class access the relevant graph's nodes and
 * data structures in an unsynchronized manner for better performance, and are
 * therefore not thread-safe in the face of concurrent modifications to the 
 * underlying graph object.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class GRASPPacker1 {
  final private Graph _g;
	private BoundedMinHeapUnsynchronized _nodesq=null;  // MinHeap<Node>
	private HashMap _currentFreeNbors = null;  // Map<Node, Set<Node> free_nbors>
  // private TreeSet _origNodesq=null;  // TreeSet<Node>
	private double _alphaFactor=0.9;  // fudge factor to allow near-optimal
	                                  // nodes to enter the solution during
	                                  // construction
	
	private static volatile double _curBestVal = Double.NEGATIVE_INFINITY;


  /**
   * constructs an instance of the GRASPPacker1 algorithm. The Graph g passed in
	 * is not copied, but only a reference to the passed object stored.
   * @param g Graph
   * @throws ParallelException
   */
  public GRASPPacker1(Graph g) throws ParallelException {
    _g = g;
    setup();
  }


  /**
   * create a maximal weighted independent set for Graph _g via a GRASP method 
	 * and return it as a Set&lt;Node&gt; of the active nodes.
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
			System.err.println("pack(init): added "+res.size()+
				                 " nodes from init list of "+addfirstfrom.size()+
				                 " nodes.");
    }
    boolean cont = true;
    while (cont) {
      cont = false;
      // 1. get the first valid element(s) from the queue
      Set candidates = new TreeSet();  // Set<Node>
      Iterator it = _nodesq.iterator();
      if (it.hasNext() == false) break;
      Node first = (Node) ((utils.Pair) it.next()).getFirst();
      candidates.add(first);
      Node n = null;
      if (it.hasNext()) {
        while (it.hasNext()) {
          n = (Node) ((utils.Pair) it.next()).getFirst();
          if (isApproximatelyEqual(n,first)) {
            candidates.add(n);
            continue; // keep adding
          }
          else if (!isApproximatelyEqualWeight(n,first)) break;
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
	 * @param a double must be in the range [0,1].
	 */
	public void setAlphaFactor(double a) {
		_alphaFactor = a;
	}


  /**
   * check feasibility.
   * @param g Graph
   * @param active Set // Set&lt;Integer nodeid&gt;
   * @return boolean
   */
  static boolean isFeasible(Graph g, Set active) {
		if (active==null) return true;
		Iterator it = active.iterator();
		while (it.hasNext()) {
			Integer nid = (Integer) it.next();
			Node n = g.getNode(nid.intValue());
			Set nbors = n.getNborsUnsynchronized();  // used to be n.getNbors();
			Iterator it2 = nbors.iterator();
			while (it2.hasNext()) {
				Node n2 = (Node) it2.next();
				if (active.contains(new Integer(n2.getId()))) return false;
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
		Set nborsj = nj.getNborsUnsynchronized();  // no modification takes place
		Iterator itj = nborsj.iterator();
		while (itj.hasNext()) {
			Node nnj = (Node) itj.next();
			if (active.contains(nnj)) return false;
		}
		return true;
  }


	/**
	 * checks whether the node n is nearly equal to the currently "best" node
	 * to enter the partial solution of a 1-packing problem (MWIS problem).
	 * @param n Node
	 * @param first Node
	 * @return true iff the two nodes are "nearly" equal (n's weight is no worse
	 * than the first's times the fudge factor and the free nbors' sizes are at 
	 * most equal to the first's divided by the same fudge factor)
	 */
	private boolean isApproximatelyEqual(Node n, Node first) {
		Double nwD = n.getWeightValueUnsynchronized("value");
		double nw = nwD==null ? 1.0 : nwD.doubleValue();
		Double fwD = first.getWeightValueUnsynchronized("value");
		double fw = fwD==null ? 1.0 : fwD.doubleValue();
		Set ffS = (Set) _currentFreeNbors.get(first);
		double ffs = ffS==null ? 0 : ffS.size();
		Set fnS = (Set) _currentFreeNbors.get(n);
		double fns = fnS==null ? 0 : fnS.size();
		return  fw*_alphaFactor <= nw && (ffs / _alphaFactor) >= fns;
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
    Set free_nbors = (Set) _currentFreeNbors.get(n);
    // 0. remove the node n and the nnbors of n from _nodesq
    removeFromNodesQ(n);
		Set nbors = n.getNborsUnsynchronized();
		Set nnbors = n.getNNborsUnsynchronized();  
    Iterator fnn_it = free_nbors.iterator();
		while (fnn_it.hasNext()) {
			Node nn = (Node) fnn_it.next();
			removeFromNodesQ(nn);
		}
		// now update the distance-2 neighbors of n, to reflect that they have 
		// less nbors to worry about
		Iterator n2_it = nnbors.iterator();
		while (n2_it.hasNext()) {
			Node n2bor = (Node) n2_it.next();
			if (free_nbors.contains(n2bor)) continue;  // n2bor is direct nbor of n
			Set n2bor_free_nbors = (Set) _currentFreeNbors.get(n2bor);
			if (n2bor_free_nbors==null) continue;  // n2bor is already out
			double cur_n2bor_val = n2bor_free_nbors.size();
			Double n2bor_wgtD = n2bor.getWeightValueUnsynchronized("value");
			double n2bor_wgt = n2bor_wgtD==null ? 1.0 : n2bor_wgtD.doubleValue();
			Double cur_n2bor_valD = new Double(cur_n2bor_val / n2bor_wgt);
			n2bor_free_nbors.removeAll(nbors);
			n2bor_free_nbors.remove(n);
			int num_n2bors_updated = n2bor_free_nbors.size();
			if (cur_n2bor_val==num_n2bors_updated) continue;
			Double new_valD = new Double(num_n2bors_updated / n2bor_wgt);
			_nodesq.decreaseKey(new utils.Pair(n2bor, cur_n2bor_valD), 
				                  new utils.Pair(n2bor, new_valD));
		}
  }
	
	
	/**
	 * removes the Node n from the min-heap _nodesq. It does this by first doing
	 * an update of the node's value to Double.NEGATIVE_INFINITY, and then calling
	 * remove on the heap.
	 * @param n Node
	 */
	private void removeFromNodesQ(Node n) {
		Double n_wgtD = n.getWeightValueUnsynchronized("value");
		double n_wgt = n_wgtD==null ? 1.0 : n_wgtD.doubleValue();
		double old_value = (((Set) _currentFreeNbors.get(n)).size()) / n_wgt;
		_nodesq.decreaseKey(new utils.Pair(n,new Double(old_value)), 
			                  new utils.Pair(n,new Double(Double.NEGATIVE_INFINITY)));
		_nodesq.remove();
		_currentFreeNbors.remove(n);
	}
	

  /**
   * return true iff all nodes in active set can be set to one without
   * violating feasibility.
   * @param active Set  // Set&lt;Node&gt;
   * @return boolean
   * @throws ParallelException
   */
  private boolean isFeasible(Set active) throws ParallelException {
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


  /**
   * This method is only called once from this object's constructor.
   */
  private void setup() {
    final int gsz = _g.getNumNodes();
		try {
			_g.makeNNbors(false);
		}
		catch (ParallelException e) {
			e.printStackTrace();
		}
		/*
		NodeComparator4 comp = new NodeComparator4();
		_origNodesq = new TreeSet(comp);
		for (int i=0; i<gsz; i++) {
			_origNodesq.add(_g.getNodeUnsynchronized(i));  // used to be _g.getNode(i)
		}
		*/
		_nodesq = new BoundedMinHeapUnsynchronized(gsz);
		_currentFreeNbors = new HashMap();
		for (int i=0; i<gsz; i++) {
			Node ni = _g.getNodeUnsynchronized(i);
			Set ni_bors = ni.getNborsUnsynchronized();
			Double ni_wgtD = ni.getWeightValueUnsynchronized("value");
			double ni_wgt = ni_wgtD==null ? 1.0 : ni_wgtD.doubleValue();
			double ni_val = ni_bors.size()/ni_wgt;
			_nodesq.addElement(new utils.Pair(ni,new Double(ni_val)));
			_currentFreeNbors.put(ni, new HashSet(ni_bors));
		}
		System.err.println("setup() done");
  }


  /**
   * called at the end of a pack() method invocation (only).
   * @throws ParallelException
   */
  private void reset() throws ParallelException {
    _nodesq.reset();
		_currentFreeNbors.clear();
		final int gsz = _g.getNumNodes();
		for (int i=0; i<gsz; i++) {
			Node ni = _g.getNodeUnsynchronized(i);
			Set ni_bors = ni.getNborsUnsynchronized();
			Double ni_wgtD = ni.getWeightValueUnsynchronized("value");
			double ni_wgt = ni_wgtD==null ? 1.0 : ni_wgtD.doubleValue();
			double ni_val = ni_bors.size()/ni_wgt;
			_nodesq.addElement(new utils.Pair(ni,new Double(ni_val)));
			_currentFreeNbors.put(ni, new HashSet(ni_bors));
		}
  }


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; graph.packing.GRASPPacker1 
	 * &lt;graph_file&gt;
	 * [numinitnodes(0)] [numiterations(1)] 
	 * [do_local_search(false)] [dls_num_threads(1)] [max_allowed_time(ms)(+Inf)] 
	 * [use_N2RXP_4_DLS_Faster(false)]</CODE>.
   * The graph_file contains the graph in the format specified in the
   * comments of method <CODE>utils.DataMgr.readGraphFromFile2(filename)</CODE>
   * and the numinitnodes is an optional number specifying how many (non-
	 * conflicting) nodes to choose randomly as an initial seed-set to grow from 
	 * (default is zero).
   * The solution found is written in the file "sol.out" in the current 
	 * directory.
   * It contains one line for each node included in the solution, and the line
   * has the internal id of the node +1 (so the range of nodes is
   * [1,...Graph.getNumNodes()].
   * @param args String[] are as follows:
	 * <ul>
	 * <li> args[0] graph_file_name mandatory, the name of the file containing the 
	 * graph
	 * <li> args[1] numinitnodes optional, if present describes the cardinality of 
	 * an initial random population from which to attempt to construct a first 
	 * partial solution. Default is 0.
	 * <li> args[2] numiterations optional, if present describes the number of 
	 * major outer iterations to run. Default is 1.
	 * <li> args[3] do_local_search optional, if "true" implies a local search via
	 * the <CODE>popt4jlib.LocalSearch.DLS</CODE> algorithm to be performed when a
	 * solution is constructed in a major outer iteration. Default is false.
	 * <li> args[4] num_dls_threads optional, if present describes the number of 
	 * threads to use in the DLS procedure if activated. Default is 1. Currently 
	 * useful only if the last parameter (args[6]) is false (as is by default).
	 * <li> args[5] max_allowed_time optional, if present, sets the max. allowed 
	 * time for the process to run (in milliseconds). Default is +infinity.
	 * <li> args[6] use_N2RXP_4_DLS optional, if present and true, will force
	 * the use of the <CODE>IntSetN2RXP*GraphAllMovesMakerFaster</CODE> move-maker 
	 * in the local search (if args[3] evaluates to true). Otherwise, if 
	 * local-search is asked for, the 
	 * <CODE>IntSetN1RXPFirstImprovingGraphAllMovesMakerMT</CODE> class will be 
	 * used as moves-maker in local-search. Default is false.
	 * </ul>
   * @throws ParallelException
   */
  public static void main(String[] args) throws ParallelException {
		if (args.length < 1) {
			System.err.println("usage: java -cp <classpath> graph.pacing.GRASPacker1"+
				                 " <graphfile> "+
				                 "[numinitnodes] [numiterations] "+
				                 "[do_local_search] [num_dls_threads] "+
				                 "[max_allowed_time_millis] [use_N2RXPFaster_4_DLS]");
			System.exit(-1);
		}
    try {
      long st = System.currentTimeMillis();
      Graph g = DataMgr.readGraphFromFile2(args[0]);
      double best = 0;
      boolean do_local_search = false;
			boolean use_N2RXP_4_ls = false;
      int num_threads = 1;
			long max_time_ms = Long.MAX_VALUE;  // max allowed time to run (in millis)
      GRASPPacker1 p = new GRASPPacker1(g);
      Set init=null;
      int num_iters = 1;
      if (args.length>1) {
        int numinit = 0;
        try {
          numinit = Integer.parseInt(args[1]);
          if (numinit<0) numinit=0;  // ignore wrong option value and continue
        }
        catch (ClassCastException e) {
          e.printStackTrace();  // ignore wrong option value and continue
        }
        Graph gp = p._g;
        int gsz = gp.getNumNodes();
        init = new TreeSet(new NodeComparator4());
				Random rnd = RndUtil.getInstance().getRandom();
        for (int i=0; i<numinit; i++) {
          int nid = rnd.nextInt(gsz);
          Node n = gp.getNodeUnsynchronized(nid);
          init.add(n);
        }
        if (args.length>2) {
          try {
            num_iters = Integer.parseInt(args[2]);
            if (num_iters<0) num_iters = 0;
          }
          catch (ClassCastException e) {
            e.printStackTrace();  // ignore wrong option value and continue
          }
          if (args.length>3) {
            do_local_search = "true".equals(args[3]);
            if (args.length>4) {
              try {
                num_threads = Integer.parseInt(args[4]);
								if (args.length>5) {
									try {
										max_time_ms = Long.parseLong(args[5]);
									}
									catch (Exception e) {
										e.printStackTrace();
									}
									if (args.length>6)
										use_N2RXP_4_ls = "true".equals(args[6]);
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
			TimerThread t = new TimerThread(max_time_ms, cont, new Runnable() {
				public void run() {
					System.err.println("Time's up; best soln found="+_curBestVal);
					System.exit(0);
				}
			});  // exit JVM after time elapses
			t.start();
      for (int i=0; i<num_iters && t.doContinue(); i++) {
				System.err.println("GRASPPacker1: starting iteration "+i);
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
					if (use_N2RXP_4_ls) movesmaker = 
						new IntSetN2RXPAllImprovingGraphAllMovesMakerFaster();
					else movesmaker = 
						new IntSetN1RXPFirstImprovingGraphAllMovesMakerMT(1);
          IntSetNeighborhoodFilterIntf filter = 
						new GRASPPackerIntSetNbrhoodFilter2(1);
          FunctionIntf f;
          f = new SetWeightEvalFunction(g);
          HashMap dlsparams = new HashMap();
          dlsparams.put("dls.movesmaker",movesmaker);
          dlsparams.put("dls.x0", nodeids);
          dlsparams.put("dls.numthreads", new Integer(num_threads));
          dlsparams.put("dls.maxiters", new Integer(100));   // itc: HERE rm asap
					dlsparams.put("dls.maxsize", new Integer(10000));
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
        }  // if do_local_search
        int iter_best = s.size();
        double iter_w_best = 0.0;
        Iterator it = s.iterator();
        while (it.hasNext()) {
          Node n = (Node) it.next();
          Double nwD = n.getWeightValueUnsynchronized("value");  
          // used to be n.getWeightValue("value");
          double nw = nwD==null ? 1.0 : nwD.doubleValue();
          iter_w_best += nw;
        }
        System.err.println("GRASPPacker1.main(): iter: "+i+
					                 ": soln size found="+iter_best+" soln weight="+
					                 iter_w_best);
        if (iter_w_best > best) {
          best_found = s;
          best = iter_w_best;
					_curBestVal = best;
        }
      }
      long tot = System.currentTimeMillis()-st;
      System.out.println("Final Best soln found="+best+
				                 " total time="+tot+" (msecs)");
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

