package graph.packing;

import popt4jlib.LocalSearch.*;
import graph.*;
import utils.*;
import parallel.*;
import java.util.*;

class IntSetN21GraphAllMovesMaker extends IntSetN21AllMovesMaker {
  IntSetN21GraphAllMovesMaker() {
  }

  /**
   * hook method in the context of the Template Method Design Pattern.
   * Overrides this method to modify the behavior of the base move-maker.
   * @param tid Integer
   * @param x IntSet
   * @param params Hashtable
   * @return boolean
   */
  protected boolean isOK2Add(Integer tid, IntSet x, Hashtable params) {
    try {
      Graph g = (Graph) params.get("dls.graph");
      Node n = g.getNode(tid.intValue());
      Set nodes = new TreeSet();
      Iterator xiter = x.iterator();
      while (xiter.hasNext()) {
        nodes.add(g.getNode(((Integer) xiter.next()).intValue()));
      }
      return isFree2Cover(n, nodes);
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }


  /**
   * check if node nj can be set to one when the nodes in active are also set.
   * @param nj Node
   * @param active Set  // Set<Node>
   * @return boolean // true iff nj can be added to active
   * @throws ParallelException
   */
  private static boolean isFree2Cover(Node nj, Set active) throws ParallelException {
    if (active==null) return true;
    if (active.contains(nj)) return false;
    Set activated = new HashSet(active);
    Iterator it = active.iterator();
    while (it.hasNext()) {
      Node ni = (Node) it.next();
      Set nnbors = ni.getNNbors();
      activated.addAll(nnbors);
    }
    return !activated.contains(nj);
  }

}

