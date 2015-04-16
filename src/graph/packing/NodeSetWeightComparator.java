package graph.packing;

import java.util.*;
import graph.*;
import parallel.*;
import utils.IntSet;

/**
 * Comparator will sort <CODE><PRE>Set<Node></PRE></CODE> objects according to
 * their total weight in descending order (heaviest weight first). In case of
 * ties, the set with the smallest node-id will be considered heavier. 
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class NodeSetWeightComparator implements Comparator {
	/**
	 * access to <CODE>Node</CODE> weights is done without locking.
	 * @param nodes1 Object // Set&lt;Node&gt;
	 * @param nodes2 Object // Set&lt;Node&gt;
	 * @return +1, -1 or 0 depending on who's heavier
	 */
	public int compare(Object nodes1, Object nodes2) {
		double w1 = 0.0;
		Iterator it = ((Set)nodes1).iterator();
		while (it.hasNext()) {
			Double v = ((Node) it.next()).getWeightValueUnsynchronized("value");
			w1 += (v==null ? 1.0 : v.doubleValue());
		}
		double w2 = 0.0;
		it = ((Set)nodes2).iterator();
		while (it.hasNext()) {
			Double v = ((Node) it.next()).getWeightValueUnsynchronized("value");
			w2 += (v==null ? 1.0 : v.doubleValue());
		}
		int res = Double.compare(w2, w1);
		if (res==0) {  // oops, must figure out how they differ
			Set ns1 = (Set) nodes1;
			Set ns2 = (Set) nodes2;
			int ns1sz = ns1.size();
			int ns2sz = ns2.size();
			if (ns1sz>ns2sz) return -1;  // more nodes, the better
			else if (ns1sz<ns2sz) return 1;
			// go through the *very* expensive nodes discrimination
			Iterator it1 = ns1.iterator();
			IntSet nis1 = new IntSet();
			while (it1.hasNext()) {
				Node n = (Node) it1.next();
				nis1.add(new Integer(n.getId()));
			}
			Iterator it2 = ns2.iterator();
			IntSet nis2 = new IntSet();
			while (it2.hasNext()) {
				Node n = (Node) it2.next();
				nis2.add(new Integer(n.getId()));
			}
			return nis2.compareTo(nis1);
		}
		return res;
	}
}

