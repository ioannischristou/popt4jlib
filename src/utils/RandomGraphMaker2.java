package utils;

import java.util.*;
import java.io.*;
import graph.*;
import parallel.*;


/**
 * builds a random graph, given the number of nodes and the average node degree
 * in the graph (and optionally of course, the random seed).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RandomGraphMaker2 {
  private int _numNodes;
  private double _aveNodeDegree;
	private double _wvar = -1.0;
  private long _seed;

  private RandomGraphMaker2(int numnodes, double avgNodeDeg, double wvar, long seed) {
    _numNodes = numnodes;
    _aveNodeDegree = avgNodeDeg;
    if (seed>0) {
      _seed = seed;
      RndUtil.getInstance().setSeed(_seed);
    }
		if (wvar>0) _wvar = wvar;
  }


  private Graph buildRandomGraph() throws GraphException, ParallelException {
    final int numarcs = (int) _aveNodeDegree*_numNodes/2;
    Graph g = new Graph(_numNodes, numarcs);
    for (int i=0; i<numarcs; i++) {
      int starta = RndUtil.getInstance().getRandom().nextInt(_numNodes);
      int enda=0;
      while (true) {
        enda = RndUtil.getInstance().getRandom().nextInt(_numNodes);
        if (enda != starta) break;
      }
      g.addLink(starta, enda, 1.0);
    }
    for (int i=0; i<_numNodes; i++) {  // set equal weights for the nodes
      if (_wvar <= 0)
				g.getNode(i).setWeight("value", new Double(1.0));
			else {
				double v = _wvar*RndUtil.getInstance().getRandom().nextGaussian()+
								   g.getNode(i).getNbors().size();
				if (v<0) v = 0.0;
				g.getNode(i).setWeight("value", new Double(v));
			}
    }
    return g;
  }


  /**
   * invoke as:
   * <CODE>java -cp &ltclasspath&gt utils.RandomGraphMaker2 &ltnumnodes&gt &ltavgnodedegree&gt &ltfilename&gt [rndseed] [nodeweightsvariance]</CODE>
	 * If the randomnodeweightsvariance option is set to some number, then
	 * the graph will have weights assigned to its nodes that will be random 
	 * numbers drawn from the gaussian distribution, with mean equal to the 
	 * node degree, and variance the specified value.
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length<3) {
      System.err.println("usage: java -cp <classpath> utils.RandomGraphMaker2 <numnodes> <avgnodedegree> <filename> [rndseed] [randomnodeweightsvar]");
      System.exit(-1);
    }
    try {
      long start_time = System.currentTimeMillis();
      long seed=0;
			double wvar = -1.0;
      if (args.length>3) seed = Long.parseLong(args[3]);
			if (args.length>4) wvar = Double.parseDouble(args[4]);
      RandomGraphMaker2 maker = new RandomGraphMaker2(Integer.parseInt(args[0]),
                                                    Double.parseDouble(args[1]),
                                                    wvar, seed);
      Graph g = maker.buildRandomGraph();
      System.err.println("Graph has "+g.getNumComponents()+" components");  // itc: HERE rm asap
      DataMgr.writeGraphToFile2(g, args[2]);
      long duration = System.currentTimeMillis()-start_time;
      System.out.println("total time (msecs): "+duration);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}

