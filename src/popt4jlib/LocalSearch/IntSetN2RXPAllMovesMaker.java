package popt4jlib.LocalSearch;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * the class implements the notion of the N_{-2+P} neighborhood of a set of
 * integers. For a given set of integers S a set S' belongs to this neighborhood
 * iff there are exactly two integers from S missing in S', and any number of
 * other integers included in S' (as proposed by the possible combinations of
 * integers computed by the associated IntSetNeighborhoodFilterIntf).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntSetN2RXPAllMovesMaker 
  implements AllChromosomeMakerClonableIntf {

  /**
   * public constructor.
   */
  public IntSetN2RXPAllMovesMaker() {
  }

	
	/**
	 * return a new IntSetN2RXPAllMovesMaker instance.
	 * @return IntSetN2RXPAllMovesMaker
	 */
	public AllChromosomeMakerClonableIntf newInstance() {
		return new IntSetN2RXPAllMovesMaker();
	}
	

  /**
   * implements the N_{-2+P} neighborhood for sets of integers.
   * @param chromosome Object // Set&lt;Integer&gt;
   * @param params HashMap must contain a key-value pair
   * &lt;"dls.intsetneighborhoodfilter",IntSetNeighborhoodFilterIntf filter&gt;.
   * The filter must both specify what two numbers to remove, as well as what
   * ints to be tried for addition to the set given a vector of 2 ints to be
   * removed from the set. In particular, the 
	 * <CODE>filter(Integer x, Set s, HashMap params)</CODE>
   * method must return a Vector&lt;IntSet&gt; that comprise all the 2-int 
	 * combinations that may be tried for removal.
   * @throws OptimizerException
   * @return Vector // Vector&lt;IntSet&gt;
   */
  public Vector createAllChromosomes(Object chromosome, HashMap params) 
		throws OptimizerException {
    if (chromosome==null) 
			throw new OptimizerException(
				"IntSetN2RXPAllMovesMaker.createAllChromosomes(): null chromosome");
    try {
			utils.Messenger mger = utils.Messenger.getInstance();
      TreeSet result = new TreeSet();  // Set<IntSet>
      Set x0 = (Set) chromosome;
      mger.msg("IntSetN2RXPAllMovesMaker.createAllChromosomes(): "+
				       "working w/ a soln of size="+x0.size(),2);
      IntSetNeighborhoodFilterIntf filter = (IntSetNeighborhoodFilterIntf)
          params.get("dls.intsetneighborhoodfilter");
      Iterator iter = x0.iterator();
			Set res = new TreeSet();  // Set<IntSet>
      while (iter.hasNext()) {
        Integer id = (Integer) iter.next();
        mger.msg("IntSetN2RXPAllMovesMaker.createAllChromosomes(): "+
					       "working w/ id="+id,3);
        List twoint_sets = filter.filter(id, x0, params);  // Vector<IntSet>
        final int tissz = twoint_sets.size();
        int cnt = 0;
        for (int i=0; i<tissz; i++) {
          Set rmids = (Set) twoint_sets.get(i);
          List tryids = filter.filter(rmids, x0, params);  // Vector<Integer>
          if (tryids!=null) {
            IntSet xnew = new IntSet(x0);
            xnew.removeAll(rmids);
            // add up to as many as the filter suggests.
            res.clear();  // itc 2015-03-02: used to be Set res = new TreeSet();
            res.add(xnew);
            tryids.removeAll(rmids);  // remove now all rmids from tryids 
						                          // so as not to have to do the check below
            Set allres = createSets(res, null, tryids, 
		                                filter.getMaxCardinality4Search(), params);
            // res, allres is Set<IntSet>
            result.addAll(allres);
            cnt += allres.size();
          }
        }
        mger.msg("IntSetN2RXPAllMovesMaker.createAllChromosomes(): "+
					       "done w/ id="+id+" returned "+cnt+" sets.",3);
      }
      // convert Set<IntSet> to Vector<IntSet>
      Vector fres = new Vector(result);
      mger.msg("IntSetN2RXPAllMovesMaker.createAllChromosomes(): in total "+
				       res.size()+" moves generated.",2);
      return fres;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException(
				          "IntSetN2RXPAllMovesMaker.createAllChromosomes(): failed");
    }
  }


  /**
   * hook method in the context of the Template Method Design Pattern.
   * Sub-classes with more domain knowledge may override this method to modify
   * the behavior of this move-maker.
   * @param res Set // TreeSet&lt;IntSet&gt;
   * @param rmids Set // IntSet
   * @param tryids List // List&lt;Integer&gt;
   * @param maxcard int
   * @param params HashMap unused
   * @return Set // TreeSet&lt;IntSet&gt;
   */
  protected Set createSets(Set res, Set rmids, List tryids, int maxcard, 
		                       HashMap params) {
    if (maxcard==0) return res;
    Set res2 = new TreeSet(res);  // was new TreeSet();
    for (int i=0; i<tryids.size(); i++) {
      Integer tid = (Integer) tryids.get(i);
      if (rmids==null || !rmids.contains(tid)) {
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
    return createSets(res2, rmids, tryids, maxcard-1, params);
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

