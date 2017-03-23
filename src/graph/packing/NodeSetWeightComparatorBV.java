package graph.packing;

import graph.*;
import popt4jlib.BoolVector;
import java.util.*;


/**
 * Comparator will sort <CODE>BoolVector</CODE> objects according to
 * their total weight in descending order (heaviest weight first). In case of
 * ties, the numbers of bits set count as weight. In case of tie again, the
 * two BoolVectors are compared via the BoolVector.compareTo() method, which 
 * does element-wise comparison (not bit-wise comparison).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class NodeSetWeightComparatorBV implements Comparator {
	
	/**
	 * access to <CODE>Node</CODE> weights is done without locking.
	 * @param nodes1 Object // BoolVector
	 * @param nodes2 Object // BoolVector
	 * @return +1, -1 or 0 depending on who's heavier
	 */
	public int compare(Object nodes1, Object nodes2) {
		Graph g = DBBTree.getInstance().getGraph();
		double w1 = 0.0;
		BoolVector bv1 = (BoolVector) nodes1;
		BoolVector bv2 = (BoolVector) nodes2;
		for (int i=bv1.nextSetBit(0); i>=0; i=bv1.nextSetBit(i+1)) {
			Double v = 
				g.getNodeUnsynchronized(i).getWeightValueUnsynchronized("value");
			w1 += (v==null ? 1.0 : v.doubleValue());
		}
		double w2 = 0.0;
		for (int i=bv2.nextSetBit(0); i>=0; i=bv2.nextSetBit(i+1)) {
			Double v = 
				g.getNodeUnsynchronized(i).getWeightValueUnsynchronized("value");
			w2 += (v==null ? 1.0 : v.doubleValue());
		}
		int res = Double.compare(w2, w1);
		if (res==0) {  // oops, must figure out how they differ
			int sz1 = bv1.cardinality();
			int sz2 = bv2.cardinality();
			if (sz1!=sz2) return Integer.compare(sz2, sz1);
			// lex-order comparison
			return bv1.compareTo(bv2);
		}
		return res;		
	}
}

