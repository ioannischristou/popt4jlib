package graph.packing;

import java.util.*;
import graph.*;

/**
 * Level-dependent Search for the Max Weighted Independent Set problem (MWIS).
 * The comparison between two BB-nodes is via the "value" of each node, that is
 * however dependent on the level of the node: below a threshold level, value is
 * the BBNode's cost (total weight), whereas after that threshold level, value 
 * is the bound of the node.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class BBNodeComparator1LvlBndCstComb implements BBNodeComparatorIntf {
	private int _levelThres;
	
	/**
	 * sole public constructor.
	 * @param levelthreshold int
	 */
  public BBNodeComparator1LvlBndCstComb(int levelthreshold) {
		_levelThres = levelthreshold;
  }

	/**
	 * below the threshold level specified as the sole constructor's argument, 
	 * value is the BBNode's cost (total weight), whereas after that threshold 
	 * level, value is the bound of the node.
	 * @param o1 BBNodeBase // BBNode1
	 * @param o2 BBNodeBase // BBNode1
	 * @return -1 if o1 should come first, +1 if o2 should come first, 0 else
	 */
  public int compare(BBNodeBase o1, BBNodeBase o2) {
    try {
			int lvl = ((BBNode1) o1).getLevel();
			double sct = lvl < _levelThres ? o1.getCost() : o1.getBound();
			int olvl = ((BBNode1) o2).getLevel();
			double osct = olvl < _levelThres ? o2.getCost() : o2.getBound();
      //double sct = o1.getCost();  // used to be o1.getNodes().size();
      //double osct = o2.getCost();  // used to be o2.getNodes().size();
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

