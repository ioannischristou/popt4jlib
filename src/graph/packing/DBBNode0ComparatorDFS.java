package graph.packing;

import popt4jlib.*;
import java.util.*;

/**
 * Depth-First Search for the Max Weighted Independent Set problem (MWIS).
 * The comparison between two DBBNode0 objects is via the total weight of each 
 * node's solution, with a lex order in case of ties.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DBBNode0ComparatorDFS implements DBBNodeComparatorIntf {
  
	/**
	 * sole public constructor.
	 */
	public DBBNode0ComparatorDFS() {
  }

	/**
	 * compares two DBBNode0 objects according to their current weight.
	 * @param obj1 DBBNodeBase  // really, DBBNode0
	 * @param obj2 DBBNodeBase  // really, DBBNode0
	 * @return int // -1, 0 or +1
	 */
  public int compare(DBBNodeBase obj1, DBBNodeBase obj2) {
    try {
			DBBNode0 o1 = (DBBNode0) obj1;
			DBBNode0 o2 = (DBBNode0) obj2;
      double sct = o1.getCost();  // used to be o1.getNodes().size();
      double osct = o2.getCost();  // used to be o2.getNodes().size();
      int ct_oct_comp = Double.compare(osct, sct);
      // if (ct > oct)return -1;
      // else if (ct == oct) {
      if (ct_oct_comp<0) return -1;
      else if (ct_oct_comp==0) {
				BoolVector bv1 = o1.getNodeIds();
				BoolVector bv2 = o2.getNodeIds();
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

