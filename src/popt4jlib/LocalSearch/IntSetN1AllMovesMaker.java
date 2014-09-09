package popt4jlib.LocalSearch;

import popt4jlib.*;
import java.util.*;

/**
 * the class implements the notion of the N_1 neighborhood of a set of integers.
 * In particular, given a subset S of the set M={1,2,...n}, the N_1(S) is the set
 * whose members are the subsets S_i of S with cardinality ||S||-1 as well as
 * the supersets S_i of S with cardinality ||S||+1 that are still subsets of M.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntSetN1AllMovesMaker  implements AllChromosomeMakerIntf {
  /**
   * public constructor.
   */
  public IntSetN1AllMovesMaker() {
  }


  /**
   * implements the N_1 neighborhood for sets of integers.
   * @param chromosome Object Set<Integer>
   * @param params Hashtable must contain:
   * <li> a key-value pair <"dls.maxvalue", num> mandatory
   * <li> a key-value pair <"dls.minvalue", num> optional (default is 0).
   * <li> a key-value pair <"dls.augmentonly", boolean> optional (default is false).
   * <li> a key-value pair <"dls.shrinkonly", boolean> optional (default is false).
   * @throws OptimizerException
   * @return Vector Vector<Set points>
   */
  public Vector createAllChromosomes(Object chromosome, Hashtable params) throws OptimizerException {
    if (chromosome==null) throw new OptimizerException("IntSetN1AllMovesMaker.createAllChromosomes(): null chromosome");
    try {
      Set x0 = (Set) chromosome;
      int max_val = ((Integer) params.get("dls.maxvalue")).intValue();
      int min_val = 0;
      Integer min_valI = (Integer) params.get("dls.minval");
      if (min_valI!=null && min_valI.intValue()<max_val)
        min_val = min_valI.intValue();
      boolean augment_only = false;
      try {
        Boolean augB = (Boolean) params.get("dls.augmentonly");
        if (augB!=null) augment_only = augB.booleanValue();
      }
      catch (ClassCastException e) {
        e.printStackTrace();
      }
      boolean shrink_only = false;
      try {
        Boolean shrB = (Boolean) params.get("dls.shrinkonly");
        if (shrB!=null) augment_only = shrB.booleanValue();
      }
      catch (ClassCastException e) {
        e.printStackTrace();
      }
      Vector result = new Vector();
      // 1. first all the subsets of x0
      if (!augment_only) {
        Iterator iter = x0.iterator();
        while (iter.hasNext()) {
          Integer xi = (Integer) iter.next();
          Set xnew = new TreeSet(x0);
          xnew.remove(xi);
          result.add(xnew);
        }
      }
      // 2. then add the 1-supersets of x0
      if (!shrink_only) {
        for (int i = min_val; i <= max_val; i++) {
          Set xnew = new TreeSet(x0);
          Integer ii = new Integer(i);
          if (x0.contains(ii))continue;
          xnew.add(ii);
          result.add(xnew);
        }
      }
      return result;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("IntSetN1AllMovesMaker.createAllChromosomes(): failed");
    }
  }
}

