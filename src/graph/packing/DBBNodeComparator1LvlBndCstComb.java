package graph.packing;

import popt4jlib.BoolVector;

/**
 * Level-dependent Search for the Max Weighted Independent Set problem (MWIS).
 * The comparison between two BB-nodes of type <CODE>DBBNode0</CODE> only is via 
 * the "value" of each node, that is however dependent on the level of the node: 
 * below a threshold level, value is the DBBNode0's cost (total weight), whereas 
 * after that threshold level, value is the bound of the node.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DBBNodeComparator1LvlBndCstComb implements DBBNodeComparatorIntf {
	private int _levelThres;
	
	/**
	 * sole public constructor.
	 * @param levelthreshold int
	 */
  public DBBNodeComparator1LvlBndCstComb(int levelthreshold) {
		_levelThres = levelthreshold;
  }

	/**
	 * below the threshold level specified as the sole constructor's argument, 
	 * value is the BBNode's cost (total weight), whereas after that threshold 
	 * level, value is the bound of the node.
	 * @param o1 DBBNodeBase // BBNode0
	 * @param o2 DBBNodeBase // BBNode0
	 * @return -1 if o1 should come first, +1 if o2 should come first, 0 else
	 */
  public int compare(DBBNodeBase o1, DBBNodeBase o2) {
    try {
			int lvl = ((DBBNode0) o1)._lvl;
			double sct = lvl < _levelThres ? o1.getCost() : o1.getBound();
			int olvl = ((DBBNode0) o2)._lvl;
			double osct = olvl < _levelThres ? o2.getCost() : o2.getBound();
      //double sct = o1.getCost();  // used to be o1.getNodes().size();
      //double osct = o2.getCost();  // used to be o2.getNodes().size();
      int ct_oct_comp = Double.compare(osct, sct);
      // if (ct > oct)return -1;
      // else if (ct == oct) {
      if (ct_oct_comp<0) return -1;
      else if (ct_oct_comp==0) {
				BoolVector bv2 = ((DBBNode0) o2)._nodeids;
				BoolVector bv1 = ((DBBNode0) o1)._nodeids;
				return bv1.compareTo(bv2);
      }
      else return 1;
    }
    catch (Exception e) {
      e.printStackTrace();
      return 0; // this would be wrong...
    }
  }
}

