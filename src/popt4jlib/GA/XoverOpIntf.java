package popt4jlib.GA;

import popt4jlib.OptimizerException;
import utils.*;
import java.util.*;

interface XoverOpIntf {
  /**
   * the operation accepts 2 chromosomes (not DGAIndividual objects, not
   * FunctionIntf() arguments), and combines them so as to produce two new
   * such chromosomes that are returned in a Pair object. The operation may
   * throw if any of the produced children are infeasible, assuming the operator
   * has enough knowledge of that fact.
   * @param chromosome1 Object
   * @param chromosome2 Object
   * @param params HashMap 
   * @throws OptimizerException
   * @return Pair
   */
  public Pair doXover(Object chromosome1, Object chromosome2, HashMap params) throws OptimizerException;
}

