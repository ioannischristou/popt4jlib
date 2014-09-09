package graph.packing;

import graph.*;
import popt4jlib.*;
import java.util.*;

public class GraphPackingEvalFunction implements FunctionIntf {
  private Graph _g;
	private int _k;

	
  public GraphPackingEvalFunction(Graph g, int k) {
    _g = g;
		_k = k;
  }

	
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
