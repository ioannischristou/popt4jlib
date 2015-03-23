package graph.packing;

import java.util.*;
import graph.*;
import parallel.*;

/**
 * Balanced comparator that mostly takes into account the bound-based estimate
 * for the corresponding BB-node (resembling a Best-First Search), turning into
 * Depth-First Search when the bound estimates are close to each other.
 * The main disadvantage of this approach is that it generates too many open
 * nodes in the beginning of the search process, and therefore takes too long
 * before it locates high-quality solutions that can be used to prune other
 * parts of the B&B-tree.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class BBNodeComparator1 implements BBNodeComparatorIntf {
  public BBNodeComparator1() {
  }

  public int compare(BBNodeBase o1, BBNodeBase o2) {
    try {
      double sct = o1.getCost();  // used to be o1.getNodes().size();
      double osct = o2.getCost();  // used to be o2.getNodes().size();
      double bd = o1.getBound();
      double obd = o2.getBound();
      double ct = bd/sct + sct - 1.0;
      double oct = obd/osct + osct - 1.0;
			// with random wgts, the bd is usually highly over-estimated until too late
      //double ct = bd + sct / Math.max(Math.log(bd), 1.0);
      //double oct = obd + osct / Math.max(Math.log(obd), 1.0);
      int ct_oct_comp = Double.compare(oct, ct);
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
    catch (ParallelException e) {
      e.printStackTrace();
      return 0; // this would be wrong...
    }
  }
}

