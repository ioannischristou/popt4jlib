package graph.packing;

import popt4jlib.LocalSearch.*;
import graph.*;
import utils.*;
import parallel.*;
import java.util.*;
import popt4jlib.AllChromosomeMakerClonableIntf;

/**
 * class is an implementation of the <CODE>AllChromosomeMakerIntf</CODE> 
 * interface, and implements local search in the N_{-2+P} neighborhood of ints:
 * a set of integers S1 is a neighbor of another set S, if S1 is the result of
 * subtracting two members of S, and then augmenting S by as many integers as 
 * possible without violating feasibility of the solution. The implementation 
 * works both for the 1- and 2-packing problems. The implementation does not 
 * stop generating moves when the first improving full solution is found, and
 * therefore is in general slower than the similar "FirstImproving" 
 * implementations.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntSetN2RXPGraphAllMovesMaker extends IntSetN2RXPAllMovesMaker {
	private int _k;
	volatile private int _cspll = -1;  // cache for the "dls.createsetsperlevellimit" value
	volatile private Graph _g;  // cache for the "dls.graph" entry ref in the params
	volatile private boolean _doLock=true;  // cache for the "dls.lock_graph" entry in the params
	
	
	/**
	 * constructor specifies move-maker for the 2-packing problem.
	 */
  public IntSetN2RXPGraphAllMovesMaker() {
		_k = 2;
  }


	/**
	 * 
	 * @param k int specifies the type of problem: 1- or 2-packing problem. 
	 */
  public IntSetN2RXPGraphAllMovesMaker(int k) {
		System.err.println("IntSetN2RXPGraphAllMovesMaker("+k+") ctor called.");  // itc: HERE rm asap
		_k = k;
  }
	
	
	/**
	 * cloning method, produces copy containing only correct value for the 
	 * <CODE>_k</CODE> data member, default values for the other data members.
	 * @return IntSetN2RXPGraphAllMovesMaker
	 */
	public AllChromosomeMakerClonableIntf newInstance() {
		return new IntSetN2RXPGraphAllMovesMaker(_k);
	}


  /**
   * hook method in the context of the Template Method Design Pattern.
   * Sub-classes with more domain knowledge may override this method to modify
   * the behavior of this move-maker.
   * @param res Set // TreeSet&lt;IntSet&gt;
   * @param rmids Set // IntSet
   * @param tryids List // List&lt;Integer&gt;
   * @param maxcard int the maximum cardinality of any of the IntSet's to be 
	 * returned
   * @param params HashMap must contain the pair &lt;"dls.graph", Graph g&gt;
	 * and may optionally contain a key-value pair
   * &lt;"dls.createsetsperlevellimit", Integer limit&gt; to cut the search 
	 * short, and optionally a pair &lt;"dls.lock_graph", Boolean val&gt; which if 
	 * present and val is false, indicates no locking of graph elements
   * @return Set // TreeSet&lt;IntSet&gt;
   */
  protected Set createSets(Set res, Set rmids, List tryids, int maxcard, HashMap params) {
    if (maxcard==0) return res;
    // check if we need to cut-short the search for candidates
		if (_cspll==-1) {
			synchronized (this) {  // this "corrected-Double-Check-Locking" is to ensure thread-safety in all circumstances
				if (_cspll==-1) {
					Integer limitI = (Integer) params.get("dls.createsetsperlevellimit");
					if (limitI!=null) _cspll = limitI.intValue();
					else _cspll = Integer.MAX_VALUE;
					// store the values of the other members in params too:  
					_g = (Graph) params.get("dls.graph");
					if (params.containsKey("dls.lock_graph")) 
						_doLock = ((Boolean) params.get("dls.lock_graph")).booleanValue();
				}
			}
		}
    Set res2 = new TreeSet();
    Iterator iter = res.iterator();
    boolean any_augmentation=false;
    while (iter.hasNext()) {
      IntSet x = (IntSet) iter.next();
      boolean augmented = false;
      for (int i=0; i<tryids.size(); i++) {
        Integer tid = (Integer) tryids.get(i);
        if (rmids==null || !rmids.contains(tid)) {
          if (_cspll<=res2.size())
            break;
          if (x.contains(tid)==false && isOK2Add(tid, x, params)) {
            IntSet x2 = new IntSet(x);
            x2.add(tid);
            res2.add(x2);
            augmented = true;
            any_augmentation = true;
          }
        }
      }
      if (!augmented) res2.add(x);
    }
    if (!any_augmentation) return res;
    return createSets(res2, rmids, tryids, maxcard-1, params);
  }


  /**
   * hook method in the context of the Template Method Design Pattern.
   * Overrides this method to modify the behavior of the base move-maker.
	 * Ensures that adding tid to the set x doesn't violate feasibility of the 
	 * solution x (depending on the type of problem 1- or 2-packing).
   * @param tid Integer
   * @param x IntSet
   * @param params HashMap must contain the &lt;"dls.graph", Graph g&gt; pair
	 * and optionally a pair &lt;"dls.lock_graph", Boolean val&gt; which if present
	 * and val is false, indicates no locking of graph elements
   * @return boolean
   */
  protected boolean isOK2Add(Integer tid, IntSet x, HashMap params) {
    try {
      if (_g==null) {  // corrected-Double-Check-Locking applied only 
				// to protect the protected API contract as method is protected
				synchronized (this) {
					if (_g==null) {
						_g = (Graph) params.get("dls.graph");
						_doLock = params.containsKey("dls.lock_graph") ?
								        ((Boolean)params.get("dls.lock_graph")).booleanValue() :
								        true;
					}
				}
			}
      Node n = _doLock ? _g.getNode(tid.intValue()) : 
				                 _g.getNodeUnsynchronized(tid.intValue());
			/* slow
			Set nodes = new TreeSet();
      Iterator xiter = x.iterator();
      while (xiter.hasNext()) {
				Node ni = _doLock ? _g.getNode(((Integer) xiter.next()).intValue()) :
								            _g.getNodeUnsynchronized(((Integer) xiter.next()).intValue());
        nodes.add(ni);
      }
      return isFree2Cover(n, nodes, _doLock);
			*/
			// faster way below avoids creating new TreeSet<Node>
			Set n_nbors = _k==1 ? 
							        (_doLock ? n.getNbors() : n.getNborsUnsynchronized()) :
							        (_doLock ? n.getNNbors() : n.getNNborsUnsynchronized());
			Iterator xiter = x.iterator();
			while (xiter.hasNext()) {
				int xid = ((Integer) xiter.next()).intValue();
				Node nx = _doLock ? _g.getNode(xid) : _g.getNodeUnsynchronized(xid);
				if (n_nbors.contains(nx)) return false;
			}
			return true;
			// end faster way
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
	 * @param do_lock boolean if false nj neighbors are accessed w/o locking the
	 * graph to which they belong
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
						       (_k==1 ? nj.getNborsUnsynchronized() : 
			                      nj.getNNborsUnsynchronized());
		Iterator itj = nborsj.iterator();
		while (itj.hasNext()) {
			Node nnj = (Node) itj.next();
			if (active.contains(nnj)) return false;
		}
    return true;
		// */
  }

}

