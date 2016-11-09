package graph.packing;

import java.util.*;
import graph.*;
import parallel.*;

/**
 * This comparator is Depth-First-Search in spirit, and only differs from DFS
 * when two solutions are "very similar", in which case, the bound of each soln
 * will be the determinant factor in which soln is examined first.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class BBNodeComparator2 implements BBNodeComparatorIntf {
  public BBNodeComparator2() {
  }

  public int compare(BBNodeBase o1, BBNodeBase o2) {
    try {
      double sct = o1.getNodes().size();
      double osct = o2.getNodes().size();
      double bd = o1.getBound();
      double obd = o2.getBound();
      double ct = sct + Math.max(Math.log(bd), 0.0);
      double oct = osct + Math.max(Math.log(obd), 0.0);
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

