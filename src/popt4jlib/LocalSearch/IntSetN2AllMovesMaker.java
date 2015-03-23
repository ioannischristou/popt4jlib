package popt4jlib.LocalSearch;

import popt4jlib.*;
import java.util.*;

/**
 * the class implements the notion of the N_2 neighborhood of a set of integers.
 * In particular, given a subset S of set M={1,2,...n}, the N_2(S) is the set
 * whose members S_{ij} are subsets of M, have the same cardinality ||S_ij|| as 
 * S, and have ||S||-1 members in common with S. 
 * It is equivalent to the N_{-1+P} where P=1 neighborhood defined in the 
 * <CODE>graph.packing.IntSetN1RXPFirstImprovingGraphAllMovesMaker</CODE> class,
 * without being related/specific to graphs.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntSetN2AllMovesMaker  implements AllChromosomeMakerIntf {
	
  /**
   * public constructor.
   */
  public IntSetN2AllMovesMaker() {
  }


  /**
   * implements the N_2 neighborhood for sets of integers.
   * @param chromosome Object // Set&ltInteger&gt
   * @param params Hashtable must contain either a key-value pair
   * &lt"dls.maxvalue", int_num&gt or a pair
   * &lt"dls.intsetneighborhoodfilter", IntSetNeighborhoodFilterIntf filter&gt
   * and optionally a key-value pair &lt"dls.minvalue", int_num&gt (default=0). 
	 * If the filter is present, it specifies the ints to be tried for addition to
   * the set given an int to be removed from the set, and therefore the min and
   * max values are not used.
   * @throws OptimizerException
   * @return Vector // Vector&ltSet&ltInteger&gt &gt
   */
  public Vector createAllChromosomes(Object chromosome, Hashtable params) throws OptimizerException {
    if (chromosome==null) throw new OptimizerException("IntSetN2AllMovesMaker.createAllChromosomes(): null chromosome");
    try {
      Set x0 = (Set) chromosome;  // Set<Integer>
      Vector result = new Vector();
      IntSetNeighborhoodFilterIntf filter = (IntSetNeighborhoodFilterIntf)
          params.get("dls.intsetneighborhoodfilter");
      if (filter!=null) {
        Iterator iter = x0.iterator();
        while (iter.hasNext()) {
          Integer id = (Integer) iter.next();
          List tryids = filter.filter(id, x0, params);  // List<Integer>
          if (tryids!=null) {
            final int sz = tryids.size();
            for (int i=0; i<sz; i++) {
              Set xnew = new TreeSet(x0);
              xnew.remove(id);
              xnew.add(tryids.get(i));
              result.add(xnew);
            }
          }
        }
        return result;
      }
      int max_val = ((Integer) params.get("dls.maxvalue")).intValue();
      int min_val = 0;
      Integer min_valI = (Integer) params.get("dls.minval");
      if (min_valI!=null && min_valI.intValue()<max_val)
        min_val = min_valI.intValue();
      for (int i=min_val; i<=max_val; i++) {
        Integer ii = new Integer(i);
        if (x0.contains(ii)) continue;
        // ok, add ii
        Iterator iter = x0.iterator();
        while (iter.hasNext()) {
          Integer xi = (Integer) iter.next();
          Set xnew = new TreeSet(x0);
          xnew.add(ii);
          xnew.remove(xi);
          result.add(xnew);
        }
      }
      return result;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("IntSetN2AllMovesMaker.createAllChromosomes(): failed");
    }
  }
}

