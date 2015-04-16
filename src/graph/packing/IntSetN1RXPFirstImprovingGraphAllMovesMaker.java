package graph.packing;

import popt4jlib.*;
import popt4jlib.LocalSearch.*;
import graph.*;
import utils.*;
import parallel.*;
import java.util.*;

/**
 * class is an implementation of the <CODE>AllChromosomeMakerIntf</CODE> 
 * interface, and implements local search in the N_{-1+P} neighborhood of ints:
 * a set of integers S1 is a neighbor of another set S, if S1 is the result of
 * subtracting one member of S, and then augmenting S by as many integers as 
 * possible without violating feasibility of the solution.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntSetN1RXPFirstImprovingGraphAllMovesMaker  implements AllChromosomeMakerIntf {

  /**
   * no-arg, no-op constructor.
   */
  public IntSetN1RXPFirstImprovingGraphAllMovesMaker() {
    // no-op
  }


  /**
   * implements the N_{-1+P} neighborhood for sets of integers.
   * @param chromosome Object Set&lt;Integer node-id&gt;
   * @param params Hashtable must contain the following key-value pairs:
	 * <ul>
	 * <li>&lt;"dls.graph", Graph g&gt; the graph of the problem
   * <li>&lt;"dls.intsetneighborhoodfilter", IntSetNeighborhoodFilterIntf filter&gt;
	 * </ul>
   * It may also optionally contain a pair
   * &lt;"dls.intsetneighborhoodmaxnodestotry", Integer max_nodes&gt; which if 
	 * present will indicate the maximum number of nodes to try to remove from the 
	 * current solution in the "1RXP" local search.
   * The filter must specify what ints to be tried for addition to the set given
   * an int to be removed from the set.
   * @throws OptimizerException
   * @return Vector // Vector&lt;Set&lt;Integer&gt; &gt;
   */
  public Vector createAllChromosomes(Object chromosome, Hashtable params) throws OptimizerException {
    if (chromosome==null) throw new OptimizerException("IntSetN1RXPFirstImprovingGraphAllMovesMaker.createAllChromosomes(): null chromosome");
    boolean rlocked_graph = false;
    Graph g = null;
    try {
      g = (Graph) params.get("dls.graph");
      g.getReadAccess();
      rlocked_graph = true;
      Set result = null;  // Set<IntSet>
      Set x0 = (Set) chromosome;
      //System.err.println("IntSetN1RXPFirstImprovingGraphAllMovesMaker.createAllChromosomes(): working w/ a soln of size="+x0.size());
      IntSetNeighborhoodFilterIntf filter = (IntSetNeighborhoodFilterIntf)
          params.get("dls.intsetneighborhoodfilter");
      int max_nodes2try = Integer.MAX_VALUE;
      Integer mn2tI = null;
      try {
        mn2tI = (Integer) params.get("dls.intsetneighborhoodmaxnodestotry");
        if (mn2tI!=null) max_nodes2try = mn2tI.intValue();
      }
      catch (ClassCastException e) {
        e.printStackTrace();  // ignore
      }
      Iterator iter = x0.iterator();
      boolean cont=true;
      while (iter.hasNext() && cont) {
        if (--max_nodes2try==0) return null;  // done with the search
        Integer id = (Integer) iter.next();
        //System.err.println("IntSetN1RXPFirstImprovingGraphAllMovesMaker.createAllChromosomes(): working w/ id="+id);
        Set rmids = new IntSet();
        rmids.add(id);
        List tryids = filter.filter(rmids, x0, params);  // List<Integer>
        if (tryids!=null) {
          IntSet xnew = new IntSet(x0);
          xnew.removeAll(rmids);
          // add up to as many as possible, 2 are needed for an improving soln when no weights exist.
          Set impr_res = createSet(xnew, id.intValue(), tryids, 2, params);
          // impr_res is IntSet
          if (impr_res!=null) {
            result = new TreeSet();
            result.add(impr_res);
            cont=false;  // found a soln; ok, break.
          }
        }
        //System.err.println("IntSetN1RXPFirstImprovingGraphAllMovesMaker.createAllChromosomes(): done w/ id="+id+
        //           " returned "+cnt+" sets.");
      }
      // convert Set<IntSet> to Vector<IntSet>
      Vector res = null;
      if (result!=null) {
        res = new Vector(result);
        //System.err.println("IntSetN1RXPAllFirstImprovingGraphMovesMaker.createAllChromosomes(): in total "+res.size()+" moves generated.");
      }
      return res;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("IntSetN1RXPFirstImprovingGraphAllMovesMaker.createAllChromosomes(): failed");
    }
    finally {
      if (rlocked_graph) {
        try {
          g.releaseReadAccess();
        }
        catch (ParallelException e) {  // can never get here
          e.printStackTrace();
          throw new OptimizerException("insanity: ParallelException should not have been thrown");
        }
      }
    }
  }


  /**
   * hook method in the context of the Template Method Design Pattern.
   * Sub-classes with more domain knowledge may override this method to modify
   * the behavior of this move-maker. This method implements a depth-first
   * search on the space of neighbors to find the first improving solution
   * which it returns immediately (the soln is a maximally-improving soln in the
   * DF fashion)
   * @param res Set IntSet
   * @param rmid Integer
   * @param tryids List // List&lt;Integer&gt;
   * @param maxcard int
   * @param params Hashtable
   * @return Set // IntSet
   */
  protected Set createSet(Set res, int rmid, List tryids, int maxcard, Hashtable params) {
    IntSet x = (IntSet) res;
    for (int i=0; i<tryids.size(); i++) {
      Integer tid = (Integer) tryids.get(i);
      if (tid.intValue()!=rmid) {
        if (x.contains(tid)==false && isOK2Add(tid, x, params)) {
          IntSet x2 = new IntSet(x);
          x2.add(tid);
          Vector tryids2 = new Vector(tryids);
          tryids2.remove(i);
          Set res3 = createSet(x2, rmid, tryids2, maxcard-1, params);
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
   * @param params Hashtable
   * @return boolean
   */
  protected boolean isOK2Add(Integer tid, IntSet x, Hashtable params) {
    try {
      Graph g = (Graph) params.get("dls.graph");
      Node n = g.getNode(tid.intValue());
      Set nodes = new TreeSet();
      Iterator xiter = x.iterator();
      while (xiter.hasNext()) {
        nodes.add(g.getNode(((Integer) xiter.next()).intValue()));
      }
      return isFree2Cover(n, nodes);
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }


  /**
   * check if node nj can be set to one when the nodes in active are also set.
   * @param nj Node
   * @param active Set  // Set&lt;Node&gt;
   * @return boolean // true iff nj can be added to active
   * @throws ParallelException
   */
  private static boolean isFree2Cover(Node nj, Set active) throws ParallelException {
    if (active==null) return true;
    if (active.contains(nj)) return false;
    Set activated = new HashSet(active);
    Iterator it = active.iterator();
    while (it.hasNext()) {
      Node ni = (Node) it.next();
      Set nnbors = ni.getNNbors();
      activated.addAll(nnbors);
    }
    return !activated.contains(nj);
  }

}

