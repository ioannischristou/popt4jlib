package utils;

import java.util.*;
import java.io.*;
import graph.*;
import parallel.*;


/**
 * builds a random graph $G_{|V|,p}, given the number of nodes and the 
 * probability of any edge (i,j) existing in the graph (and optionally of course, 
 * the random seed). The graph may also have random weights on its nodes if the
 * appropriate flag is set.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RandomGraphMaker3 {
  private int _numNodes;
  private double _edgeProb;
	private double _wvar = 0.0;
  private long _seed;

	
  private RandomGraphMaker3(int numnodes, double edgeProb, double wvar, long seed) {
    _numNodes = numnodes;
    _edgeProb = edgeProb;
    if (seed>0) {
      _seed = seed;
      RndUtil.getInstance().setSeed(_seed);
    }
		_wvar = wvar;
  }


  private Graph buildRandomGraph() throws GraphException, ParallelException {
    List arcs = new ArrayList();
		final Random r = RndUtil.getInstance().getRandom();
		for (int i=0; i<_numNodes; i++) {
			for (int j=i+1; j<_numNodes; j++) {
				double p = r.nextDouble();
				if (p<_edgeProb) {  // ok, add (i,j) to arcs
					arcs.add(new Pair(new Integer(i), new Integer(j)));
				}
			}
		}
		final int numarcs = arcs.size();
    Graph g = Graph.newGraph(_numNodes, numarcs);
    for (int i=0; i<numarcs; i++) {
      int starta = ((Integer)((Pair) arcs.get(i)).getFirst()).intValue();
      int enda = ((Integer)((Pair) arcs.get(i)).getSecond()).intValue();
			g.addLink(starta, enda, 1.0);
    }
    for (int i=0; i<_numNodes; i++) {  
      if (Double.compare(_wvar, 0.0)==0)  // set equal weights for the nodes
				g.getNode(i).setWeight("value", new Double(1.0));
			else {
				if (_wvar > 0) {  // weights drawn from Gaussian with with mean equal to node degree, and variance _wvar
					double v = _wvar*r.nextGaussian()+
									   g.getNode(i).getNbors().size();
					g.getNode(i).setWeight("value", new Double(v));
				}
				else {  // _wvar < 0; weights drawn from uniform distribution (discrete or continuous)
					// is _wvar an integer?
					if (Double.compare(_wvar, Math.round(_wvar))==0) {
						double v= r.nextInt((int) (-_wvar));
						g.getNode(i).setWeight("value", new Double(v));
					} else {
						double v= -_wvar*r.nextDouble();
						g.getNode(i).setWeight("value", new Double(v));
					}
				}
			}
    }
    return g;
  }


  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; utils.RandomGraphMaker3 &lt;numnodes&gt; &lt;dgeProbability&gt; &lt;filename&gt; [rndseed] [nodeweightsvariance]</CODE>.
	 * If the randomnodeweightsvariance option is set to some positive number then
	 * the graph will have weights assigned to its nodes that will be random 
	 * numbers drawn from the gaussian distribution, with mean equal to the 
	 * node degree, and variance the specified value. If this option is used and 
	 * set to some negative value, then the absolute value of this value will form
	 * the upper limit of the uniform distribution from which node weights will be
	 * drawn; if the value is integer, then the distribution used will be the 
	 * discrete uniform distribution, else the continuous uniform distribution. If 
	 * the option is not used, then every node will have weight equal to 1.0.
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length<3) {
      System.err.println("usage: java -cp <classpath> utils.RandomGraphMaker3 <numnodes> <edgeProb> <filename> [rndseed] [randomnodeweightsvar]");
      System.exit(-1);
    }
    try {
      long start_time = System.currentTimeMillis();
      long seed=0;
			double wvar = 0.0;
      if (args.length>3) seed = Long.parseLong(args[3]);
			if (args.length>4) wvar = Double.parseDouble(args[4]);
      RandomGraphMaker3 maker = new RandomGraphMaker3(Integer.parseInt(args[0]),
                                                    Double.parseDouble(args[1]),
                                                    wvar, seed);
      Graph g = maker.buildRandomGraph();
      //System.err.println("Graph has "+g.getNumComponents()+" components");
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

