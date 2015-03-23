package graph.packing;

import graph.*;
import popt4jlib.FunctionIntf;
import java.util.*;

/**
 * Evaluates the total weight of the nodes whose ids are in the set-argument and 
 * returns its negative.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class SetWeightEvalFunction implements FunctionIntf {
	private Graph _g;

  /**
   * sole public constructor
	 * @param g Graph
   */
  public SetWeightEvalFunction(Graph g) {
    _g = g;
  }


  /**
   * return minus the total weight of the nodes whose ids are in the set-valued 
	 * argument.
   * @param arg Object // Set&ltInteger node-id&gt
   * @param params Hashtable may contain the name of the weight to compute, 
	 * in a ("weightname",name) key-pair, else the default "value" string will be 
	 * used.
   * @throws IllegalArgumentException if arg is not a Set.
   * @return double
   */
  public double eval(Object arg, Hashtable params) throws IllegalArgumentException {
    try {
			double w = 0.0;
      Set s = (Set) arg;
      Iterator it = s.iterator();
			while (it.hasNext()) {
				Integer id = (Integer) it.next();
				String name = "value";
				if (params!=null) {
					String n = (String) params.get("weightname");
					if (n!=null) name = n;
				}
				Double nwD = _g.getNode(id.intValue()).getWeightValue(name);
				if (nwD==null && name.length()==0) w+=1.0;
				else if (nwD!=null) w += nwD.doubleValue();
			}
			return -w;
    }
    catch (ClassCastException e) {
      throw new IllegalArgumentException("arg must be a Set<Integer>");
    }
  }
}

