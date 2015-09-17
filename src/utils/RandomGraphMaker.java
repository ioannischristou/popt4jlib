package utils;

import java.util.*;
import java.io.*;
import graph.*;
import parallel.*;


/**
 * builds the dual graph of the collision graph corresponding to an ad-hoc
 * wireless network with nodes randomly placed in a normalized H x W rectangle,
 * and with radius of transmission either constant or uniformly distributed in
 * [r/2, 3r/2]. The maximum 2-packing problem solution on the dual graph is the
 * solution to the D2EMIS problem for the original ad-hoc wireless network. May
 * also add edges between neighbors at distance 2, so that instead of 2-packing,
 * one may have to solve the (more common) maximum weighted independent set
 * (MWIS) problem. In the latter case, node weights are drawn from the discrete
 * uniform distribution in U[1,[num_mwis_nodes*r]].
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RandomGraphMaker {
  private int _numnodes;
  private double _xlen;
  private double _ylen;
  private double _r;
  private long _seed;
  private boolean _uniformr=false;
	private boolean _createMWIS=false;

  private RandomGraphMaker(int numnodes, double xdim, double ydim, double radius,
					                 boolean uniformr, long seed, boolean createMWIS) {
    _numnodes = numnodes;
    _xlen=xdim;
    _ylen=ydim;
    _r=radius;
    if (seed>0) {
      _seed = seed;
      RndUtil.getInstance().setSeed(_seed);
    }
    _uniformr = uniformr;
		_createMWIS=createMWIS;
  }


  private Graph buildUniformRandomDualGraph(String jplotfilename) throws GraphException, ParallelException, FileNotFoundException {
    PrintWriter pw = null;
    if (jplotfilename!=null && !jplotfilename.equals("null")) {
      pw = new PrintWriter(new FileOutputStream(jplotfilename));
      pw.println("double double");
      pw.println("invisible 0 0"); pw.println("invisible "+_xlen+" "+_ylen);
    }
    // 1. set nodes in random places uniformly.
    double[] _x = new double[_numnodes];
    double[] _y = new double[_numnodes];
    double[] _rs = _uniformr ? new double[_numnodes] : null;
    for (int i = 0; i < _numnodes; i++) {
      _x[i] = RndUtil.getInstance().getRandom().nextDouble() * _xlen;
      _y[i] = RndUtil.getInstance().getRandom().nextDouble() * _ylen;
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
      for (int j=i+1; j<_numnodes; j++) {
        if (_uniformr==false && Math.pow(_x[i]-_x[j],2.0)+Math.pow(_y[i]-_y[j],2.0) <= _r*_r) { // i and j are nbors
          nbors[i].add(new Integer(j));
          ++numarcs;
        }
        else if (_uniformr==true &&
                 Math.pow(_x[i]-_x[j],2.0)+Math.pow(_y[i]-_y[j],2.0)<=
                 Math.pow(_rs[i]+_rs[j],2.0)) {  // i and j are neighbors
          // according to the undirected disk graph model
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
        if (pw!=null) {
          dgnxpos[aind] = (_x[i]+_x[j.intValue()])/2.0;
          dgnypos[aind] = (_y[i]+_y[j.intValue()])/2.0;
          pw.println("dot "+dgnxpos[aind]+" "+dgnypos[aind]);
          ++aind;
        }
      }
    }
    // 3. return dual graph
    Graph dg = g.getDual();
		if (_createMWIS) {  // add the 2-neighbors of a node to its immediate nbors
			int tot_arcs = 0;
			for (int i=0; i<dg.getNumNodes(); i++) {
				Node ni = dg.getNode(i);
				Set nnborsi = ni.getNNbors();
				tot_arcs += nnborsi.size();
			}
			tot_arcs /= 2;
			Graph dg2 = Graph.newGraph(dg.getNumNodes(), tot_arcs);
			// add all the necessary arcs and node weights
			for (int i=0; i<dg2.getNumNodes(); i++) {
				Node ni = dg.getNode(i);  // get dg's node, not dg2
				Set nnborsi = ni.getNNbors();
				Iterator it = nnborsi.iterator();
				while (it.hasNext()) {
					Node nbor = (Node) it.next();
					if (nbor.getId()>i) {  // ok add arc
						dg2.addLink(i, nbor.getId(), 1);
					}
				}
				// add node weight
				int num = (int) (dg.getNumNodes()*_r*RndUtil.getInstance().getRandom().nextDouble());
				if (num==0) num = 1;
				dg2.getNode(i).setWeight("value", new Double(num));
			}
			dg = dg2;
		}
    if (pw!=null) {
      int numdualarcs = dg.getNumArcs();
      for (int i=0; i<numdualarcs; i++) {
        Link li = dg.getLink(i);
        int starta = li.getStart();
        int enda = li.getEnd();
        pw.println("line "+dgnxpos[starta]+" "+dgnypos[starta]+" "+dgnxpos[enda]+" "+dgnypos[enda]);
      }
      pw.println("go");
      pw.flush();
      pw.close();
    }
    return dg;
  }


  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; utils.RandomGraphMaker &lt;numnodes&gt;
   * &lt;xdim&gt; &lt;ydim&gt; &lt;radius&gt; &lt;filename&gt; [uniform] [rndseed] [jplotfilename(null)] [createMWIS(false)]</CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length<5) {
      System.err.println("usage: java -cp <classpath> utils.RandomGraphMaker <numnodes> <xdim> <ydim> <radius> <filename> [uniformr] [rndseed] [jplotfilename(null)] [createMWIS(false)]");
      System.exit(-1);
    }
    try {
      long start_time = System.currentTimeMillis();
      boolean uniformr = false;
      if (args.length>5) uniformr = Boolean.valueOf(args[5]).booleanValue();
      long seed=0;
      if (args.length>6) seed = Long.parseLong(args[6]);
      String jplotfilename=null;
      if (args.length>7) jplotfilename=args[7];
			boolean domwis = false;
			if (args.length>8) domwis = Boolean.valueOf(args[8]).booleanValue();
      RandomGraphMaker maker = new RandomGraphMaker(Integer.parseInt(args[0]),
                                                    Double.parseDouble(args[1]),
                                                    Double.parseDouble(args[2]),
                                                    Double.parseDouble(args[3]),
                                                    uniformr,
                                                    seed,
			                                              domwis);
      Graph g = maker.buildUniformRandomDualGraph(jplotfilename);
      //System.err.println("Dual graph has "+g.getNumComponents()+" components");
      DataMgr.writeGraphToFile2(g, args[4]);
      long duration = System.currentTimeMillis()-start_time;
      System.out.println("total time (msecs): "+duration);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}

