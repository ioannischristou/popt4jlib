package graph.packing;

import java.util.*;
import graph.*;

/**
 * Depth-First Search for the Max Weighted Independent Set problem (MWIS).
 * The comparison between two BB-nodes is via the total weight of each node's
 * solution.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class BBNodeComparator1DFS implements BBNodeComparatorIntf {
  
	/**
	 * sole public constructor.
	 */
	public BBNodeComparator1DFS() {
  }

	
	/**
	 * compares two BBNodeBase objects according to the total weight each solution
	 * represents (bigger is better).
	 * @param o1  BBNodeBase
	 * @param o2 BBNodeBase
	 * @return int // -1, 0 or +1
	 */
  public int compare(BBNodeBase o1, BBNodeBase o2) {
    try {
      double sct = o1.getCost();  // used to be o1.getNodes().size();
      double osct = o2.getCost();  // used to be o2.getNodes().size();
      int ct_oct_comp = Double.compare(osct, sct);
      // if (ct > oct)return -1;
      // else if (ct == oct) {
      if (ct_oct_comp<0) return -1;
      else if (ct_oct_comp==0) {
        Set to = o2.getNodes();
        Iterator it = o1.getNodes().iterator();
        Iterator oit = to.iterator();
        while (it.hasNext()) {
          Node mi = (Node) it.next();
          if (oit.hasNext()) {
            Node oi = (Node) oit.next();
            if (mi.getId() < oi.getId())return -1;
            else if (mi.getId() > oi.getId())return 1;
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

