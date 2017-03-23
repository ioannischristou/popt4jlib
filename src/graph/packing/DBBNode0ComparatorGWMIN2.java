package graph.packing;

import popt4jlib.*;
import graph.*;
import java.util.*;

/**
 * Best-First Search for the Max Weighted Independent Set problem (MWIS) 
 * according to the GWMIN2 criterion, with a lex order in case of ties.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DBBNode0ComparatorGWMIN2 implements DBBNodeComparatorIntf {
  
	/**
	 * sole public constructor.
	 */
	public DBBNode0ComparatorGWMIN2() {
  }

	/**
	 * compares two DBBNode0 objects according to their current value of GWMIN2
	 * criterion.
	 * @param obj1 DBBNodeBase  // really, DBBNode0
	 * @param obj2 DBBNodeBase  // really, DBBNode0
	 * @return int // -1, 0 or +1
	 */
  public int compare(DBBNodeBase obj1, DBBNodeBase obj2) {
    try {
			Graph g = DBBTree.getInstance().getGraph();
			final int num_nodes = g.getNumNodes();
			DBBNode0 o1 = (DBBNode0) obj1;
			DBBNode0 o2 = (DBBNode0) obj2;
      //double sct = o1.getCost();  // used to be o1.getNodes().size();	
			double sct = 0.0;
			BoolVector bv1 = o1.getNodeIds();
			int[] nbor_hit_counts = new int[num_nodes];  // count how many times
			                                             // each node is a nbor to
			                                             // a node in o1
			for (int i=bv1.nextSetBit(0); i>=0; i=bv1.nextSetBit(i+1)) {
				Node ni = g.getNodeUnsynchronized(i);
				Set nbors = ni.getNborsUnsynchronized();
				Iterator it = nbors.iterator();
				while (it.hasNext()) {
					Node nbor = (Node) it.next();
					nbor_hit_counts[nbor.getId()]++;
				}				
			}			
			for (int i=bv1.nextSetBit(0); i>=0; i=bv1.nextSetBit(i+1)) {
				Node ni = g.getNodeUnsynchronized(i);
				Double wD = ni.getWeightValueUnsynchronized("value");
				double wi = wD==null ? 1.0 : wD.doubleValue();
				Set nbors = ni.getNborsUnsynchronized();
				Iterator it = nbors.iterator();
				double sum = 0.0;
				while (it.hasNext()) {
					Node nbor = (Node) it.next();
					if (nbor_hit_counts[nbor.getId()]<=1) { // adding ni forbids nbor
						Double nbor_wD = nbor.getWeightValueUnsynchronized("value");
						sum += (nbor_wD==null ? 1.0 : nbor_wD.doubleValue());
					}
				}
				if (Double.compare(sum, 0.0)==0) sum=1;
				sct += wi / sum;
			}
      //double osct = o2.getCost();  // used to be o2.getNodes().size();
			BoolVector bv2 = o2.getNodeIds();
			double osct = 0.0;
			for (int i=0; i<nbor_hit_counts.length; i++) 
				nbor_hit_counts[i]=0;  // reset
			for (int i=bv2.nextSetBit(0); i>=0; i=bv2.nextSetBit(i+1)) {
				Node ni = g.getNodeUnsynchronized(i);
				Set nbors = ni.getNborsUnsynchronized();
				Iterator it = nbors.iterator();
				while (it.hasNext()) {
					Node nbor = (Node) it.next();
					nbor_hit_counts[nbor.getId()]++;
				}				
			}			
			for (int i=bv2.nextSetBit(0); i>=0; i=bv2.nextSetBit(i+1)) {
				Node ni = g.getNodeUnsynchronized(i);
				Double wD = ni.getWeightValueUnsynchronized("value");
				double wi = wD==null ? 1.0 : wD.doubleValue();
				Set nbors = ni.getNborsUnsynchronized();
				Iterator it = nbors.iterator();
				double sum = 0.0;
				while (it.hasNext()) {
					Node nbor = (Node) it.next();
					if (nbor_hit_counts[nbor.getId()]<=1) { // adding ni forbids nbor
						Double nbor_wD = nbor.getWeightValueUnsynchronized("value");
						sum += (nbor_wD==null ? 1.0 : nbor_wD.doubleValue());
					}
				}
				if (Double.compare(sum, 0.0)==0) sum=1;
				osct += wi / sum;
			}			
      int ct_oct_comp = Double.compare(osct, sct);
      // if (ct > oct)return -1;
      // else if (ct == oct) {
      if (ct_oct_comp<0) return -1;
      else if (ct_oct_comp==0) {
				int j=-1;
				for (int i=bv1.nextSetBit(0); i>=0; i=bv1.nextSetBit(i+1)) {
					j = bv2.nextSetBit(j+1);
					if (j>=0) {
						if (i<j) return -1;
						else if (i>j) return 1;
					}
					else return 1;
				}
				if (j>=0) return -1;
				else return 0;
      }
      else return 1;
    }
    catch (Exception e) {
      e.printStackTrace();
      return 0; // this would be wrong...
    }
  }
}

