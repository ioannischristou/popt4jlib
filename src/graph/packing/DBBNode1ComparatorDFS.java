package graph.packing;

import java.util.*;
import graph.*;

/**
 * Depth-First Search for the Max Weighted Independent Set problem (MWIS).
 * The comparison between two DBBNode1 objects is via the total weight of each 
 * node's solution.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DBBNode1ComparatorDFS implements DBBNodeComparatorIntf {
  
	/**
	 * sole public constructor.
	 */
	public DBBNode1ComparatorDFS() {
  }

	/**
	 * compares two DBBNode1 objects according to their current weight.
	 * @param o1 DBBNodeBase  // really, DBBNode1
	 * @param o2 DBBNodeBase  // really, DBBNode1
	 * @return int // -1, 0 or +1
	 */
  public int compare(DBBNodeBase o1, DBBNodeBase o2) {
    try {
      double sct = o1.getCost();  // used to be o1.getNodes().size();
      double osct = o2.getCost();  // used to be o2.getNodes().size();
      int ct_oct_comp = Double.compare(osct, sct);
      // if (ct > oct)return -1;
      // else if (ct == oct) {
      if (ct_oct_comp<0) return -1;
      else if (ct_oct_comp==0) {
        Set to = o2.getNodeIdsAsSet();
        Iterator it = o1.getNodeIdsAsSet().iterator();
        Iterator oit = to.iterator();
        while (it.hasNext()) {
          Integer mi = (Integer) it.next();
          if (oit.hasNext()) {
            Integer oi = (Integer) oit.next();
            if (mi.intValue() < oi.intValue())return -1;
            else if (mi.intValue() > oi.intValue())return 1;
          }
          else return 1;
        }
        if (oit.hasNext())return -1;
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

