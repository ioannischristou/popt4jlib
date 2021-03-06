package utils;

import java.util.*;
import java.io.*;
import graph.*;
import parallel.*;


/**
 * builds the dual of the collision graph of an ad-hoc wireless network whose
 * nodes have been randomly placed on the surface of a sphere. The collision
 * balls of each node have a radius that can be either a constant or a random
 * variable uniformly distributed in [r/2, 3r/2].
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RandomBallGraphMaker {
  private int _numnodes;
  private double _ballradius;
  private double _r;
  private long _seed;
  private boolean _uniformr=false;

  private RandomBallGraphMaker(int numnodes, double ballradius, double radius, boolean uniformr, long seed) {
    _numnodes = numnodes;
    _ballradius = ballradius;
    _r=radius;
    if (seed>0) {
      _seed = seed;
      RndUtil.getInstance().setSeed(_seed);
    }
    _uniformr = uniformr;
  }


  private Graph buildUniformRandomDualGraph() throws GraphException, FileNotFoundException, ParallelException {
    // 1. set nodes in random places uniformly.
    double[] _phi = new double[_numnodes];  // angles
    double[] _theta = new double[_numnodes];  // angles
    double[] _rs = _uniformr ? new double[_numnodes] : null;
    for (int i = 0; i < _numnodes; i++) {
      _phi[i] = RndUtil.getInstance().getRandom().nextDouble() * 2.0 * Math.PI;
      _theta[i] = RndUtil.getInstance().getRandom().nextDouble() * 2.0 * Math.PI;
    }
    // the following is outside the previous loop so we can generate the same
    // node positions, once for const. radius, and once with uniform distr.
    if (_rs!=null) {
      for (int i = 0; i < _numnodes; i++) {
        _rs[i] = 0.5*_r + RndUtil.getInstance().getRandom().nextDouble()*_r;
      }
    }
    // 2. compute arcs
    Set[] nbors = new Set[_numnodes];
    int numarcs = 0;
    for (int i=0; i<_numnodes; i++) {
      nbors[i] = new TreeSet();
      double xi = _ballradius*Math.cos(_phi[i]);
      double yi = _ballradius*Math.sin(_phi[i]);
      double zi = _ballradius*Math.cos(_theta[i]);
      for (int j=i+1; j<_numnodes; j++) {
        double xj = _ballradius*Math.cos(_phi[j]);
        double yj = _ballradius*Math.sin(_phi[j]);
        double zj = _ballradius*Math.cos(_theta[j]);
        if (_uniformr==false && Math.pow(xi-xj,2.0)+Math.pow(yi-yj,2.0)+Math.pow(zi-zj,2.0) <= _r*_r) { // i and j are nbors
          nbors[i].add(new Integer(j));
          ++numarcs;
        }
        else if (_uniformr==true &&
                 Math.pow(xi-xj,2.0)+Math.pow(yi-yj,2.0)+Math.pow(zi-zj,2.0)<=
                 Math.pow(Math.min(_rs[i],_rs[j]),2.0)) {  // used to be _rs[i]+_rs[j]  
          // i and j are neighbors according to the undirected disk graph model
          nbors[i].add(new Integer(j));
          ++numarcs;
        }
      }
    }
    Graph g = Graph.newGraph(_numnodes, numarcs);
    double[] dgnxpos = new double[numarcs];
    double[] dgnypos = new double[numarcs];
    int aind = 0;
    for (int i=0; i<_numnodes; i++) {
      Set nborsi = nbors[i];
      Iterator it = nborsi.iterator();
      while (it.hasNext()) {
        Integer j = (Integer) it.next();
        double wij = (_rs==null) ? 1.0 : _rs[i]+_rs[j.intValue()];
        g.addLink(i, j.intValue(), wij);
      }
    }
    // 3. return dual graph
    Graph dg = g.getDual();
    return dg;
  }


  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; utils.RandomBallGraphMaker &lt;numnodes&gt;
   * &lt;ballradius&gt; &lt;radius&gt; &lt;filename&gt; [uniformr] [rndseed]</CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length<5) {
      System.err.println("usage: java -cp <classpath> utils.RandomBallGraphMaker <numnodes> <ballradius> <radius> <filename> [uniformr] [rndseed]");
      System.exit(-1);
    }
    try {
      long start_time = System.currentTimeMillis();
      boolean uniformr = false;
      if (args.length>4) uniformr = Boolean.valueOf(args[4]).booleanValue();
      long seed=0;
      if (args.length>5) seed = Long.parseLong(args[5]);
      RandomBallGraphMaker maker = new RandomBallGraphMaker(Integer.parseInt(args[0]),
                                                    Double.parseDouble(args[1]),
                                                    Double.parseDouble(args[2]),
                                                    uniformr,
                                                    seed);
      Graph g = maker.buildUniformRandomDualGraph();
      System.err.println("Dual graph has "+g.getNumComponents()+" components");
      DataMgr.writeGraphToFile2(g, args[3]);
      long duration = System.currentTimeMillis()-start_time;
      System.out.println("total time (msecs): "+duration);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}

