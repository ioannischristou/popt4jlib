package popt4jlib.GA;

import popt4jlib.OptimizerException;
import utils.*;
import java.util.*;

interface MutationOpIntf {
  /**
   * the operation mutates each chromosome argument (not a DGAIndividual, not a
   * Function() arg object) and stores the new chromosome in a Pair object that
   * is returned. The operation may throw if the produced chromosome is infeasible
   * and the operator has knowledge of that fact.
   * @param chromosome1 Object
   * @param chromosome2 Object
   * @param Hashtable params
   * @throws OptimizerException
   * @return Pair
   */
  public Pair doMutation(Object chromosome1, Object chromosome2, Hashtable params)
      throws OptimizerException;

}



