package graph.packing;

import java.util.*;
import parallel.*;
import graph.*;

/**
 * an implementation of the DBBNodeComparatorIntf.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DefDBBNodeComparator implements DBBNodeComparatorIntf {
  public DefDBBNodeComparator() {
  }

  public int compare(DBBNode1 o1, DBBNode1 o2) {
    double sct = o1.getNodes().size();
    double osct = o2.getNodes().size();
    double bd = o1.getBound();
    double obd = o2.getBound();
    double ct = bd / sct + sct - 1.0;
    double oct = obd / osct + osct - 1.0;
    //if (ct > oct)return -1;
    //else if (ct == oct) {
    int ct_oct_comp = Double.compare(oct, ct);
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
}
