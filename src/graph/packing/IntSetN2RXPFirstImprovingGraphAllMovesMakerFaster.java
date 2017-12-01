/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package graph.packing;

import graph.Graph;
import graph.Node;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import popt4jlib.AllChromosomeMakerClonableIntf;
import popt4jlib.BoolVector;
import popt4jlib.OptimizerException;

/**
 * class is an implementation of the <CODE>AllChromosomeMakerIntf</CODE>
 * interface, and implements local search in the N_{-2+P} neighborhood of ints:
 * a set of integers S1 is a neighbor of another set S, if S1 is the result of
 * subtracting two members of S, and then augmenting S by as many integers as
 * possible without violating feasibility of the solution. The implementation
 * works for the 1-packing UNWEIGHTED problem (MIS), but should also be
 * expected to work with MWIS when weights are nearly uniform distributed and
 * always positive.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntSetN2RXPFirstImprovingGraphAllMovesMakerFaster
  implements AllChromosomeMakerClonableIntf {


  /**
   * no-arg constructor.
   */
  public IntSetN2RXPFirstImprovingGraphAllMovesMakerFaster() {
		// no-op
  }


	/**
	 * return a new IntSetN2RXFirstImprovingGraphPAllMovesMakerFaster instance.
	 * @return IntSetN2RXPFirstImprovingGraphAllMovesMaker
	 */
	public AllChromosomeMakerClonableIntf newInstance() {
		return new IntSetN2RXPFirstImprovingGraphAllMovesMakerFaster();
	}


	/**
	 * the major method of the class, implements a simple fast logic.
	 * (0) for each node x NOT in the current solution, consider the set H(x) of
	 * nodes IN the solution that are neighbors of x (thus "hit" the node x);
	 * (1) order these nodes x in descending order in the size of H(x) and remove
	 * all nodes that have H(x) size &gt; 2;
	 * (2) for each node x in this (cut) sorted array, consider all nodes below it
	 * to see if there are at least two more such nodes y whose set H(y) is a
	 * subset of H(x). If such a node x is found, then the set H(x) is the set
	 * to remove from the current solution, and the node x plus the two nodes y
	 * below it that match, are the entering nodes (assuming that the sum of
	 * weights of the entering nodes is greater than the sum of weights of the
	 * leaving nodes). Node accesses are unsynchronized and assume that the
	 * underlying graph object is not modified in any way.
	 * @param chromosome Object  // Set&lt;Integer&gt;
	 * @param params HashMap  // Map&lt;String key, Object val&gt; that must
	 * contain the key-value pair
	 * &lt;"dls.graph", Graph g&gt; unless this object was constructed with the
	 * 1-arg constructor
	 * @return Vector  // Vector&lt;Set&lt;Integer&gt;&gt; with vector size &le;1.
	 * @throws OptimizerException
	 */
  public Vector createAllChromosomes(Object chromosome, HashMap params) throws OptimizerException {
    if (chromosome==null) throw new OptimizerException("IntSetN2RXPFirstImprovingGraphAllMovesMakerFaster.createAllChromosomes(): null chromosome");
		Graph _g = (Graph) params.get("dls.graph");
		try {
			Vector result = new Vector();
			// 0. create array Hs[x]
			final int num_nodes = _g.getNumNodes();
			BoolVector sol_bv = new BoolVector(num_nodes);
			Set nodeids = (Set) chromosome;  // Set<Integer>
			Iterator nodeids_it = nodeids.iterator();
			while (nodeids_it.hasNext()) {
				sol_bv.set(((Integer)nodeids_it.next()).intValue());
			}
			BVI[] Hs = new BVI[num_nodes];
			for (int i=0; i<Hs.length; i++) {
				boolean set_all = nodeids.contains(new Integer(i));
				Hs[i] = new BVI(new BoolVector(num_nodes,set_all), i);
			}
			for (int i=0; i<num_nodes; i++) {
				if (sol_bv.get(i)) continue;  // i is in the current solution
				BoolVector Hsi = Hs[i]._bv;
				Node ni = _g.getNodeUnsynchronized(i);
				Set ni_bors = ni.getNborIndicesUnsynchronized(Double.NEGATIVE_INFINITY);
				Hsi.setAll(ni_bors);
				Hsi.and(sol_bv);
			}
			// 1. order array Hs
			Arrays.sort(Hs);
			// 2. consider all nodes x whose H[x].size()<=2, and see if there are
			// at least 3 other nodes y below with H[y] subset of H[x].
			int start = 0;
			for (; start < num_nodes; start++) {
				BVI hs = Hs[start];
				if (hs._bv.cardinality()<=2) break;
			}
			if (start>=num_nodes-2) return result;  // search cut-short, no solution
			for (; start<num_nodes-2; start++) {
				BoolVector bv_start = Hs[start]._bv;
				Set satisfying = new HashSet();  // Set<Integer>
				for (int j0=start+1; j0<num_nodes; j0++) {
					satisfying.clear();
					for (int j=j0; j<num_nodes; j++) {
						BoolVector bv_j = Hs[j]._bv;
						if (bv_start.containsAll(bv_j)) {
							// ensure Hs[j]._i node is not a neighbor of the existing satisfying
							// or the Hs[start]._i node
							if (!conflict(_g, satisfying, Hs[start]._i, Hs[j]._i))
								satisfying.add(new Integer(Hs[j]._i));
						}
					}  // for j=j0...num_nodes-1
					if (satisfying.size()>=2) {
						// found it.
						// remove H[start] from sol.
						for (int k=bv_start.nextSetBit(0); k>=0;
								 k=bv_start.nextSetBit(k+1)) {
							sol_bv.unset(k);
						}
						// add start, satisfying to the solution
						sol_bv.set(Hs[start]._i);
						Iterator s_it = satisfying.iterator();
						while (s_it.hasNext()) {
							Integer y = (Integer) s_it.next();
							sol_bv.set(y.intValue());
						}
						Set res = new HashSet();
						for (int k=sol_bv.nextSetBit(0); k>=0; k=sol_bv.nextSetBit(k+1)) {
							res.add(new Integer(k));
						}
						result.add(res);
						return result;
					}
				}  // for j0
			}  // for start
			return result;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new OptimizerException("createAllChromosomes() failed");
		}
	}


	/**
	 * check whether node with id j is neighbor of start, or any of the nodes
	 * whose ids are in the set satisfying. If so, return true.
	 * @param g Graph
	 * @param satisfying Set  // Set&lt;Integer&gt;
	 * @param start int
	 * @param j int
	 * @return boolean
	 */
	private static boolean conflict(Graph g, Set satisfying, int start, int j) {
		Node nstart = g.getNodeUnsynchronized(start);
		Node nj = g.getNodeUnsynchronized(j);
		Set nstart_nbors = nstart.getNborsUnsynchronized();
		if (nstart_nbors.contains(nj)) return true;
		Set nj_nbors = nj.getNborsUnsynchronized();
		Iterator sat_it = satisfying.iterator();
		while (sat_it.hasNext()) {
			Integer sid = (Integer) sat_it.next();
			Node ns = g.getNodeUnsynchronized(sid.intValue());
			if (nstart_nbors.contains(ns) || nj_nbors.contains(ns)) return true;
		}
		return false;
	}


	/**
	 * auxiliary helper class used for sorting.
	 */
	static class BVI implements Comparable {
		BoolVector _bv;
		int _i;


		BVI(BoolVector bv, int i) {
			_bv = bv;
			_i = i;
		}


		public int compareTo(Object other) {
			BVI o = (BVI) other;
			// int res = -Integer.compare(_bv.cardinality(),
                        //                            o._bv.cardinality());
                        int res = _bv.cardinality() < o._bv.cardinality() ? 1 :
                                    _bv.cardinality()==o._bv.cardinality() ? 0 :
                                      -1;
			//  if (res==0) return Integer.compare(_i, o._i);
                        if (res==0) {
                          return _i < o._i ? -1 : (_i==o._i ? 0 : 1);
                        }
			else return res;
		}
	}

}
