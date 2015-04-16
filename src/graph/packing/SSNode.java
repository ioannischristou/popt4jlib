package graph.packing;

import graph.*;
import utils.*;
import java.util.*;

/**
 * A class implementing the Self-Stabilizing algorithm for finding a maximal
 * 2-packing in a graph.
 * @see F. Manne & M. Mjelde, "A Memory Efficient Self-stabilizing Algorithm for
 * Maximal k-Packing", LNCS, vol. 4280, pp. 428-439, 2006.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class SSNode {
  private static SSNode[] _ssnodes;
  private static Graph _g;
  final private static int _k = 2;

  private int _id;  // corresponds to Graph node _id.
  private int _pv;  // shortest distance to closest black node
  private int _bv;  // id of closest black node
  private int _pv2;  // shortest distance to 2nd closest black node
  private int _bv2;  // id of 2nd closest black node


  private SSNode(int id) {
    _id = id;
    _pv = Integer.MAX_VALUE;
    _bv = Integer.MAX_VALUE;
    _pv2 = Integer.MAX_VALUE;
    _bv2 = Integer.MAX_VALUE;
  }

  private SSNodeData getData() {
    return new SSNodeData(_pv, _bv, _pv2, _bv2);
  }
  private void setData(SSNodeData d) {
    _pv = d._pv; _bv = d._bv; _pv2 = d._pv2; _bv2 = d._bv2;
  }
  private int getId() { return _id; }

  private Set getNborData() {
    Set nbors = _g.getNode(_id).getNbors();
    Set nbordata = new HashSet();
    Iterator nborsit = nbors.iterator();
    while (nborsit.hasNext()) {
      Node n = (Node) nborsit.next();
      SSNodeData dn = _ssnodes[n.getId()].getData();
      nbordata.add(dn);
    }
    return nbordata;
  }

  private SSNodeData support() {
    SSNodeData resdata = null;
    SSNodeData ab = null;
    Set nbordata = getNborData();
    Iterator it = nbordata.iterator();
    int pbest = Integer.MAX_VALUE;
    int vbest = Integer.MAX_VALUE;
    int p2best = Integer.MAX_VALUE;
    int v2best = Integer.MAX_VALUE;
    // 1. compute (a,b)
    while (it.hasNext()) {
      SSNodeData d = (SSNodeData) it.next();
      if (d._pv<pbest && d._bv!=_id) {
        ab = new SSNodeData(d);
        pbest = d._pv;
        vbest = d._bv;
      } else if (d._pv == pbest && d._bv!=_id && d._bv < vbest) {
          ab = new SSNodeData(d);
          pbest = d._pv;
          vbest = d._bv;
      }
    }
    // 2. compute (a',b')
    SSNodeData apbp = null;
    it = nbordata.iterator();
    while (it.hasNext()) {
      SSNodeData d = (SSNodeData) it.next();
      if (d._pv<p2best && d._bv!=_id && d._bv!=ab._bv) {
        apbp = new SSNodeData(d);
        p2best = d._pv;
        v2best = d._bv;
      } else if (d._pv == p2best && d._bv!=_id && d._bv < v2best && d._bv!=ab._bv) {
          apbp = new SSNodeData(d);
          p2best = d._pv;
          v2best = d._bv;
      }
    }
    if (ab==null) ab = new SSNodeData();
    if (apbp==null) apbp = new SSNodeData();
    // 3. if-else rule
    if (ab._pv >= _k || (_pv==0 && ab._bv > _id)) {
      int pvp1 = ab._pv==Integer.MAX_VALUE ? Integer.MAX_VALUE : ab._pv+1;
      resdata = new SSNodeData(0, _id, pvp1, ab._bv);
    }
    else {
      int pvp1 = ab._pv==Integer.MAX_VALUE ? Integer.MAX_VALUE : ab._pv+1;
      int pvpp1 = apbp._pv==Integer.MAX_VALUE ? Integer.MAX_VALUE : apbp._pv+1;
      resdata = new SSNodeData(pvp1, ab._bv, pvpp1, apbp._bv2);
    }
    return resdata;
  }

  private boolean sameAs(SSNodeData d) {
    return (_pv == d._pv && _bv == d._bv && _pv2 == d._pv2 && _bv2 == d._bv2);
  }


  private static void setup(Graph g) {
    final int gsz = g.getNumNodes();
    _ssnodes = new SSNode[gsz];
    for (int i=0; i<gsz; i++) _ssnodes[i] = new SSNode(i);
    _g = g;
  }

  private static void selfstabilize() {
    final int gsz = _g.getNumNodes();
    boolean cont = true;
    int iter=0;
    while (cont) {
      cont=false;
      for (int i=0; i<gsz && !cont; i++) {
        SSNode ssni = _ssnodes[i];
        if (ssni.sameAs(ssni.support())==false) {
          ssni.setData(ssni.support());
          cont=true;
        }
      }
      ++iter;
      //System.err.println("iteration cnt="+iter);  
		}
  }

  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; graph.packing.SSNode &lt;graphfilename&gt; </CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length!=1) {
      System.err.println("usage: java -cp <classpath> graph.packing.SSNode <graphfilename>");
      System.exit(-1);
    }
    long start = System.currentTimeMillis();
    try {
      Graph g = DataMgr.readGraphFromFile2(args[0]);
      setup(g);
      selfstabilize();
      // count black nodes
      int active = 0;
      for (int i=0; i<g.getNumNodes(); i++) {
        SSNode ssni = _ssnodes[i];
        if (ssni._pv==0) active++;
      }
      long dur = System.currentTimeMillis()-start;
      System.out.println("total active nodes="+active+" in "+dur+" msecs");
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}


class SSNodeData {
  int _pv;
  int _pv2;
  int _bv;
  int _bv2;

  SSNodeData() {
    _pv=_pv2=_bv=_bv2=Integer.MAX_VALUE;
  }
  SSNodeData(int pv, int bv, int pv2, int bv2) {
    _pv = pv; _bv=bv; _pv2=pv2; _bv2=bv2;
  }
  SSNodeData(SSNodeData other) {
    _pv = other._pv;
    _bv = other._bv;
    _pv2 = other._pv2;
    _bv2 = other._bv2;
  }
}

