package popt4jlib.LocalSearch;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * the class implements the notion of the N_{2+1} neighborhood of a set of 
 * integers. In particular, it is the same as the 
 * <CODE>IntSetN2AllMovesMaker</CODE> plus it adds to each one of the previously 
 * created sets one or more integers (the selection is only from the set of 
 * filter results).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntSetN21AllMovesMaker  implements AllChromosomeMakerIntf {
  /**
   * public constructor.
   */
  public IntSetN21AllMovesMaker() {
  }


  /**
   * implements the N_2+1 neighborhood for sets of integers.
   * @param chromosome Object // Set&lt;Integer&gt;
   * @param params HashMap must contain a key-value pair
   * &lt;"dls.intsetneighborhoodfilter", IntSetNeighborhoodFilterIntf filter&gt;
   * The filter specifies the ints to be tried for addition to
   * the set given an int to be removed from the set.
   * @throws OptimizerException
   * @return Vector // Vector&lt;IntSet&gt;
   */
  public Vector createAllChromosomes(Object chromosome, HashMap params) throws OptimizerException {
    if (chromosome==null) throw new OptimizerException("IntSetN21AllMovesMaker.createAllChromosomes(): null chromosome");
    try {
      TreeSet result = new TreeSet();
      Set x0 = (Set) chromosome;
      IntSetNeighborhoodFilterIntf filter = (IntSetNeighborhoodFilterIntf)
          params.get("dls.intsetneighborhoodfilter");
      Iterator iter = x0.iterator();
      while (iter.hasNext()) {
        Integer id = (Integer) iter.next();
        List tryids = filter.filter(id, x0, params);  // List<Integer>
        if (tryids!=null) {
          IntSet xnew = new IntSet(x0);
          xnew.remove(id);  // remove one
          // add up to as many as the filter suggests.
          Set res = new TreeSet();  // Set<IntSet>
          res.add(xnew);
          Set allres = createSets(res, id, tryids, filter.getMaxCardinality4Search(), params);
          // res is Set<IntSet>
          result.addAll(allres);
        }
      }
      // convert Set<IntSet> to Vector<IntSet>
      Vector res = new Vector(result);
      //System.err.println("IntSetN21AllMovesMaker.createAllChromosomes(): in total "+res.size()+" moves generated.");  
      return res;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("IntSetN21AllMovesMaker.createAllChromosomes(): failed");
    }
  }


  /**
   * hook method in the context of the Template Method Design Pattern.
   * Sub-classes with more domain knowledge may override this method to modify
   * the behavior of this move-maker.
   * @param res Set // TreeSet&lt;IntSet&gt;
   * @param id Integer
   * @param tryids List // List&lt;Integer&gt;
   * @param maxcard int
   * @param params HashMap
   * @return Set // TreeSet&lt;IntSet&gt;
   */
  protected Set createSets(Set res, Integer id, List tryids, int maxcard, HashMap params) {
    if (maxcard==0) return res;
    Set res2 = new TreeSet(res);  // was new TreeSet();
    for (int i=0; i<tryids.size(); i++) {
      Integer tid = (Integer) tryids.get(i);
      if (tid.intValue()!=id.intValue()) {
        Iterator iter = res.iterator();
        while (iter.hasNext()) {
          IntSet x = (IntSet) iter.next();
          if (x.contains(tid)==false && isOK2Add(tid, x, params)) {
            IntSet x2 = new IntSet(x);
            x2.add(tid);
            res2.add(x2);
          }
        }
      }
    }
    return createSets(res2, id, tryids, maxcard-1, params);
  }


  /**
   * always returns true. Hook method in the context of the Template Method 
	 * Design Pattern. Sub-classes with more domain knowledge may override this 
	 * method to modify the behavior of this move-maker.
   * @param tid Integer unused
   * @param x IntSet unused
   * @param params HashMap unused
   * @return boolean true
   */
  protected boolean isOK2Add(Integer tid, IntSet x, HashMap params) {
    return true;
  }
}

