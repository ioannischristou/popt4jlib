package graph.packing;

import graph.*;
import popt4jlib.LocalSearch.*;
import utils.*;
import java.util.*;

public class GRASPPackerIntSetNbrhoodFilter2 implements IntSetNeighborhoodFilterIntf {
	private int _k;  // indicate problem type: 1- or 2-packing
	
	
  public GRASPPackerIntSetNbrhoodFilter2() {
		_k = 2;
  }


  public GRASPPackerIntSetNbrhoodFilter2(int k) {
		_k = k;
  }


  /**
   * given two integers to be removed from a set, what sets of integers may be
   * added to that set?
   * @param x Object IntSet (with expected cardinality 2)
   * @param arg Object Set<Integer>
   * @param params Hashtable
   * @throws LocalSearchException
   * @return Vector Vector<Integer> the integers that may be tried
   */
  public Vector filter(Object x, Object arg, Hashtable params) throws LocalSearchException {
    try {
      //System.err.print("running filter(IntSet rmids, Set sol, params) ");
      IntSet rmids = (IntSet) x;
      Set s = (Set) arg;
      Graph g = (Graph) params.get("dls.graph");
      Set nbors = new IntSet();
      Iterator it = rmids.iterator();
      while (it.hasNext()) {
        Integer rmid = (Integer) it.next();
        Node rni = g.getNode(rmid.intValue());
				Set toadd = _k==2 ? rni.getNNborIndices() : rni.getNborIndices(Double.NEGATIVE_INFINITY);
        nbors.addAll(toadd);
      }
      nbors.removeAll(s);  // is it necessary?
      Vector res = new Vector(nbors);
      //System.err.println("...produced "+res.size()+" tryids.");
      return res;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new LocalSearchException("filter failed.");
    }
  }


  /**
   * provide a Vector<IntSet> of sets of two integers one of which is x.
   * @param x Integer
   * @param arg Set IntSet
   * @param params Hashtable must contain a key-value pair <"dls.graph", Graph g>
   * @throws LocalSearchException
   * @return Vector Vector<IntSet>
   */
  public Vector filter(Integer x, Set arg, Hashtable params) throws LocalSearchException {
    try {
      //System.err.print("running filter(Integer x, IntSet sol, params). Adding ");
      Graph g = (Graph) params.get("dls.graph");
      Node nx = g.getNode(x.intValue());
      Set nxnnborids = _k==2 ? nx.getNNborIndices() : nx.getNborIndices(Double.NEGATIVE_INFINITY);
      Vector result = new Vector();
      Iterator it = arg.iterator();
      while (it.hasNext()) {
        Integer nyid = (Integer) it.next();
        if (nyid.intValue()<=x.intValue()) continue;
        Node ny = g.getNode(nyid.intValue());
        //boolean ok = g.getShortestPath(nx, ny)<=3;
        Set nynborids = ny.getNborIndices(Double.NEGATIVE_INFINITY);
        nynborids.retainAll(nxnnborids);
        boolean ok = nynborids.size()>0;
        if (ok) {
          IntSet cand = new IntSet();
          cand.add(x);
          cand.add(nyid);
          result.add(cand);
          //System.err.print("("+x+","+nyid+")...");
        }
      }
      //System.err.println("filter(Integer x, IntSet sol, params) done");
      return result;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new LocalSearchException("filter failed...");
    }
  }


  public int getMaxCardinality4Search() {
    return 5;
  }
}

