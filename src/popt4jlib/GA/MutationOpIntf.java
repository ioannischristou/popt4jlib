package popt4jlib.GA;

import popt4jlib.OptimizerException;
import utils.*;
import java.util.*;

/**
 * public interface for mutation operators.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface MutationOpIntf {
  /**
   * the operation mutates each chromosome argument (not a DGAIndividual, not a
   * Function() arg object) and stores the chromosomes in a Pair object that
   * is returned. Often, the chromosomes passed in are the ones that undergo the
	 * mutations and are returned themselves in the return Pair. The operation may 
	 * throw if any produced chromosome is infeasible and the operator has 
	 * knowledge of that fact; in such a case, it is unspecified whether the 
	 * arguments chromosome1 and chromosome2 should be restored to their state 
	 * before the call or not. Each implementing class should document its 
	 * behavior in this case.
   * @param chromosome1 Object
   * @param chromosome2 Object
   * @param params HashMap 
   * @throws OptimizerException
   * @return Pair
   */
  public Pair doMutation(Object chromosome1, Object chromosome2, HashMap params)
      throws OptimizerException;

}



