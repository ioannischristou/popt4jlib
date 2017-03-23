package graph.packing;

import utils.IntSet;
import java.util.*;

/**
 * Default implementation of the DBBNodeComparatorIntf uses the bound of the 
 * solution, &amp; breaks ties according to the node-ids of the solutions 
 * compared.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DefDBBNodeComparator implements DBBNodeComparatorIntf {
	/**
	 * sole public constructor.
	 */
  public DefDBBNodeComparator() {
  }

	/**
	 * compares two DBBNode objects according to the bound of the solutions they
	 * represent, and break ties (which should be a rare condition) according to
	 * the ids of the nodes contained in each solution.
	 * @param o1 DBBNodeBase
	 * @param o2 DBBNodeBase
	 * @return int // -1, 0 or +1
	 */
  public int compare(DBBNodeBase o1, DBBNodeBase o2) {
    double sct = o1.getNodeIdsAsSet().size();
    double osct = o2.getNodeIdsAsSet().size();
    double bd = o1.getBound();
    double obd = o2.getBound();
    double ct = bd / sct + sct - 1.0;
    double oct = obd / osct + osct - 1.0;
    //if (ct > oct)return -1;
    //else if (ct == oct) {
    int ct_oct_comp = Double.compare(oct, ct);
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
			/*  below code is waste
			IntSet to1 = new IntSet(o1.getNodeIds());
			IntSet to2 = new IntSet(o2.getNodeIds());
			return to1.compareTo(to2);
			*/
    }
    else return 1;
  }
}
