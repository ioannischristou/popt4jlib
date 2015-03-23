package graph.packing;

import popt4jlib.*;
import popt4jlib.LocalSearch.*;
import graph.*;
import utils.*;
import parallel.*;
import java.util.*;

/**
 * class is an implementation of the <CODE>AllChromosomeMakerIntf</CODE> 
 * interface, and implements local search in the N_{-2+P} neighborhood of ints:
 * a set of integers S1 is a neighbor of another set S, if S1 is the result of
 * subtracting two members of S, and then augmenting S by as many integers as 
 * possible without violating feasibility of the solution. The implementation 
 * works both for the 1- and 2-packing problems.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntSetN2RXPFirstImprovingGraphAllMovesMaker  implements AllChromosomeMakerClonableIntf {
	private int _k;
	private Graph _g;
	
	
  /**
   * no-arg constructor is for the 2-packing problem.
   */
  public IntSetN2RXPFirstImprovingGraphAllMovesMaker() {
    _k=2;
  }
	
	
	/**
	 * 
	 * @param k int specifies the type 
	 */
	public IntSetN2RXPFirstImprovingGraphAllMovesMaker(int k) {
		_k = k;
	}

	
	/**
	 * 
	 * @param k int specifies the type
	 * @param g Graph specifies the relevant graph reference to hold
	 */
	public IntSetN2RXPFirstImprovingGraphAllMovesMaker(int k, Graph g) {
		_k = k;
		_g = g;
	}

	
	/**
	 * return a new IntSetN2RXFirstImprovingGraphPAllMovesMaker instance, with the
	 * right <CODE>_k</CODE> value.
	 * @return IntSetN2RXPFirstImprovingGraphAllMovesMaker
	 */
	public AllChromosomeMakerClonableIntf newInstance() {
		return new IntSetN2RXPFirstImprovingGraphAllMovesMaker(_k);
	}

	
  /**
   * implements the N_{-2+P} neighborhood for sets of integers.
   * @param chromosome Object // Set&ltInteger&gt
   * @param params Hashtable must contain a key-value pair
   * &lt"dls.intsetneighborhoodfilter", IntSetNeighborhoodFilterIntf filter&gt
   * The filter must both specify what two numbers to remove, as well as what
   * ints to be tried for addition to the set given a vector of 2 ints to be
   * removed from the set. In particular, the filter(Integer x, Set s, Hashtable params)
   * method must return a List&ltIntSet&gt that comprise all the 2-int 
	 * combinations that may be tried for removal.
   * @throws OptimizerException
   * @return Vector // Vector&ltSet&ltInteger&gt &gt
   */
  public Vector createAllChromosomes(Object chromosome, Hashtable params) throws OptimizerException {
    if (chromosome==null) throw new OptimizerException("IntSetN2RXPFirstImprovingGraphAllMovesMaker.createAllChromosomes(): null chromosome");
    try {
      Set result = null;  // Set<IntSet>
      Set x0 = (Set) chromosome;
      //System.err.println("IntSetN2RXPFirstImprovingGraphAllMovesMaker.createAllChromosomes(): working w/ a soln of size="+x0.size());  
      IntSetNeighborhoodFilterIntf filter = (IntSetNeighborhoodFilterIntf)
          params.get("dls.intsetneighborhoodfilter");
      Iterator iter = x0.iterator();
      boolean cont=true;
      while (iter.hasNext() && cont) {
        Integer id = (Integer) iter.next();
        //System.err.println("IntSetN2RXPFirstImprovingGraphAllMovesMaker.createAllChromosomes(): working w/ id="+id);  
        List twoint_sets = filter.filter(id, x0, params);  // List<IntSet>
        Iterator iter2 = twoint_sets.iterator();
        while (iter2.hasNext() && cont) {
          Set rmids = (Set) iter2.next();
          List tryids = filter.filter(rmids, x0, params);  // List<Integer>
          if (tryids!=null) {
            IntSet xnew = new IntSet(x0);
            xnew.removeAll(rmids);
            // add up to as many as possible, 3 are needed for an improving soln.
            Set impr_res = createSet(xnew, rmids, tryids, 3, params);
            // impr_res is IntSet
            if (impr_res!=null) {
              result = new TreeSet();
              result.add(impr_res);
              cont=false;  // found a soln; ok, break.
            }
          }
        }
        //System.err.println("IntSetN2RXPFirstImprovingGraphAllMovesMaker.createAllChromosomes(): done w/ id="+id+
        //           " returned "+cnt+" sets.");
      }
      // convert Set<IntSet> to Vector<IntSet>
      Vector res = null;
      if (result!=null) {
        res = new Vector(result);
        //System.err.println("IntSetN2RXPAllFirstImprovingGraphMovesMaker.createAllChromosomes(): in total "+res.size()+" moves generated.");  
      }
      return res;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("IntSetN2RXPFirstImprovingGraphAllMovesMaker.createAllChromosomes(): failed");
    }
  }


  /**
   * hook method in the context of the Template Method Design Pattern.
   * Sub-classes with more domain knowledge may override this method to modify
   * the behavior of this move-maker. This method implements a depth-first
   * search on the space of neighbors to find the first improving solution
   * which it returns immediately (the soln is a maximally-improving soln in the
   * DF fashion)
   * @param res Set // IntSet
   * @param rmids Set // IntSet
   * @param tryids List // List&ltInteger&gt
   * @param maxcard int
   * @param params Hashtable must contain the pair &lt"dls.graph", Graph g&gt
	 * and optionally the pair &lt"dls.lock_graph", Boolean val&gt
   * @return Set // IntSet
   */
  protected Set createSet(Set res, Set rmids, List tryids, int maxcard, Hashtable params) {
    IntSet x = (IntSet) res;
    for (int i=0; i<tryids.size(); i++) {
      Integer tid = (Integer) tryids.get(i);
      if (rmids.contains(tid)==false) {
        if (x.contains(tid)==false && isOK2Add(tid, x, params)) {
          IntSet x2 = new IntSet(x);
          x2.add(tid);
          List tryids2 = new ArrayList(tryids);
          tryids2.remove(i);
          Set res3 = createSet(x2, rmids, tryids2, maxcard-1, params);
          if (res3!=null) return res3;
        }
      }
    }
    if (maxcard<=0) return res;
    else return null;
  }


  /**
   * hook method in the context of the Template Method Design Pattern.
   * Overrides this method to modify the behavior of the base move-maker.
   * @param tid Integer
   * @param x IntSet
   * @param params Hashtable must contain the pair &lt"dls.graph", Graph g&gt,
	 * and may optionally contain the pair &lt"dls.lock_graph", Boolean val&gt,
	 * which if present and false, indicates unsynchronized access to the graph's
	 * nodes; however, if the 2-arg constructor was used to construct this object
	 * and non-null graph was passed in, then params is ignored.
   * @return boolean
   */
  protected boolean isOK2Add(Integer tid, IntSet x, Hashtable params) {
    try {
      Graph g = _g;
			boolean lock_graph = false;
			if (g==null) {
				g = (Graph) params.get("dls.graph");
				lock_graph = params.containsKey("dls.lock_graph") ? 
							               ((Boolean) params.get("dls.lock_graph")).booleanValue() :
							               true;
			}
      Node n = lock_graph ? g.getNode(tid.intValue()) : g.getNodeUnsynchronized(tid.intValue());
      Set nodes = new TreeSet();
      Iterator xiter = x.iterator();
      while (xiter.hasNext()) {
				Node nn = lock_graph ? g.getNode(((Integer) xiter.next()).intValue()) :
								               g.getNodeUnsynchronized(((Integer) xiter.next()).intValue());
        nodes.add(nn);
      }
      return isFree2Cover(n, nodes, lock_graph);
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }


  /**
   * check if node nj can be set to one when the nodes in active are also set.
   * @param nj Node
   * @param active Set  // Set&ltNode&gt
	 * @param do_lock boolean if false, nodes' neighbors will be accessed w/o 
	 * locking the graph to which they belong
   * @return boolean true iff nj can be added to active
   * @throws ParallelException
   */
  private boolean isFree2Cover(Node nj, Set active, boolean do_lock) 
	    throws ParallelException {
    if (active==null) return true;
    if (active.contains(nj)) return false;
		/* slow
    Set activated = new HashSet(active);
    Iterator it = active.iterator();
    while (it.hasNext()) {
      Node ni = (Node) it.next();
      Set nnbors = _k==2 ? 
							       (do_lock ? ni.getNNbors() : ni.getNNborsUnsynchronized()) : 
							       (do_lock ? ni.getNbors() : ni.getNborsUnsynchronized());
      activated.addAll(nnbors);
    }
    return !activated.contains(nj);
		*/
		// /* faster: no need for HashSet's creation
		Set nborsj = do_lock ? 
						       (_k==1 ? nj.getNbors() : nj.getNNbors()) : 
						       (_k==1 ? nj.getNborsUnsynchronized() : nj.getNNborsUnsynchronized());
		Iterator itj = nborsj.iterator();
		while (itj.hasNext()) {
			Node nnj = (Node) itj.next();
			if (active.contains(nnj)) return false;
		}
		// */
    return true;
  }

}

