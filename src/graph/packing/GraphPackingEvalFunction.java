package graph.packing;

import graph.*;
import popt4jlib.*;
import java.util.*;

/**
 * evaluates sets of <CODE>Node</CODE> objects for feasibility as k-packing
 * solutions, and returns minus the number of nodes in the solution as long as
 * the solution is feasible, or <CODE>Double.MAX_VALUE</CODE> in case of an
 * infeasible solution. 
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class GraphPackingEvalFunction implements FunctionIntf {
  private Graph _g;
	private int _k;

	
	/**
	 * sole constructor.
	 * @param g Graph
	 * @param k the type of packing problem must be 1 or 2
	 */
  public GraphPackingEvalFunction(Graph g, int k) {
    _g = g;
		_k = k;
  }

	
	/**
	 * return <CODE>-((Set)arg).size()</CODE> if arg is a feasible solution, else
	 * returns +infinity.
	 * @param arg Set Set&lt;Node&gt;
	 * @param params Hashtable unused
	 * @return double specified above
	 * @throws IllegalArgumentException if arg is not a Set&lt;Node&gt; 
	 */
  public double eval(Object arg, Hashtable params) throws IllegalArgumentException {
    try {
      double val = Double.MAX_VALUE;
      Set s = (Set) arg;
      boolean feas = GRASPPacker.isFeasible(_g, s, _k);
      if (feas) val = -s.size();
      else val = Double.MAX_VALUE;
      return val;
    }
    catch (ClassCastException e) {
      throw new IllegalArgumentException("arg must be a Set<Integer>");
    }
  }
}
