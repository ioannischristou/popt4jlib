package graph.packing;

import java.util.*;
import utils.*;
import graph.*;

/**
 * implements a hybrid parallel GASP-Branch&Bound heuristic scheme for the
 * 1- or 2-packing problem in graphs.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class BBGASPPacker {

  private static int _totActiveNodes=0;
	private static double _totActiveNodeWeights=0.0;
	private static int _totLeafNodes=0;

  /**
   * sole public no-arg constructor.
   */
  public BBGASPPacker() {
  }


  /**
   * run as <CODE>java -cp &ltclasspath&gt graph.packing.BBGASPPacker &ltgraphfilename&gt &ltparamsfilename&gt [maxnumBBnodes] [numthreads]</CODE>.
   * args[0]: graph file name must adhere to the format specified in the
   * description of the method <CODE>utils.DataMgr.readGraphFromFile2(String file)</CODE>
   * args[1]: params file name may define parameters in lines of the following
   * form:
   * <ul>
	 * <li> k, $num$ optional, the type of packing problem (1- or 2-packing).
	 * Default is 2.
   * <li> numthreads, $num$ optional, the number of threads to use. Default is 1.
   * <li> maxqsize, $num$ optional, the maximum size of the BBGASP Task-Queue.
   * Default is Integer.MAX_VALUE.
   * <li> tightenboundlevel, $num$ optional, the depth in the B&B tree constructed
   * at which a stronger computation of the upper bound will be started, default
   * is 0.
   * <li> cutnodes, $boolean$ optional, if true, then when the BBQueue of nodes
   * is full, any new nodes created will be discarded instead of processed on
   * the same thread. Default is false.
   * <li> localsearch, $boolean$ optional, if true, then when an incumbent
   * solution is found that cannot be further improved, a local search kicks in
   * to try to improve it using the N1RXP(FirstImproving) neighborhood concept
   * that basically attempts to remove a single node from the solution and then
   * see how many other nodes it can add to the reduced solution. This local
   * search can become quite expensive and for this reason it is only applied
   * to final incumbent solutions in the B&B-tree construction process. Default
   * is false.
   * <li> usemaxsubsets, $boolean$ optional, if false, then each GASP process
   * augmenting candidate packings will augment these sets one node at a time,
   * leading to the possibility that many active nodes in the B&B tree will
   * represent the same packing. In such a case, a "recent-nodes" queue will
   * be used to safe-guard against the possibility of having the same nodes
   * created and processed within a "short" interval. Default is true.
	 * <li> maxitersinGBNS2A, $num$ optional, if present and also the
	 * "usemaxsubsets" key is true, then the number represents the max number of
	 * iterations the <CODE>getBestNodeSets2Add()</CODE> method of the
	 * <CODE>BBNode[1-2]</CODE> class will be allowed to go through. Default is
	 * 100000 (specified in <CODE>BBTree</CODE> class.)
   * <li> recentqueuesize, $num$ the length of the queue to be used when the
   * "usemaxsubsets" parameter is false.
   * <li> maxnodechildren, $num$ optional, specify an upper bound on the number
   * of children any node is allowed to create. Default is Integer.MAX_VALUE.
   * <li> class,bbnodecomparator, &ltfullclassname&gt optional, the full class
   * name of a class implementing the <CODE>graph.packing.BBNodeComparatorIntf</CODE>
   * that is used to define the order in which B&B nodes in the tree are picked
   * for processing. Default is <CODE>graph.packing.DefBBNodeComparator</CODE>.
   * </ul>
   * args[2]: [optional] override max num nodes in params_file to create in B&B procedure
   * args[3]: [optional] override numthreads in params_file to use
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      if (args.length<2) {
        System.err.println("usage: java -cp <classpath> graph.packing.BBGASPPacker <graphfilename> <paramsfilename> [maxnumBBnodes] [numthreads]");
        System.exit(-1);
      }
      // /*
      // register handle to show best soln if we stop the program via ctrl-c
      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          System.err.println("best soln="+BBGASPPacker._totActiveNodes+" totWeight="+BBGASPPacker._totActiveNodeWeights);
          System.err.flush();
        }
      }
      );
      // */
      long start = System.currentTimeMillis();
      Graph g = DataMgr.readGraphFromFile2(args[0]);
      // print out total value weight of the nodes
      {
        double totw = 0.0;
        for (int i=0; i<g.getNumNodes(); i++) {
          Double w = g.getNode(i).getWeightValue("value");
          totw += w==null ? 1.0 : w.doubleValue();
        }
        System.err.println("Graph total nodes' weight="+totw);
      }
      Hashtable params = DataMgr.readPropsFromFile(args[1]);
      int maxnodes = -1;
      if (args.length>2)
        maxnodes = Integer.parseInt(args[2]);  // override max num nodes
      if (args.length>3)
        params.put("numthreads",new Integer(args[3]));  // override num threads

      int nt = 1;
      Integer num_threadsI = (Integer) params.get("numthreads");
      if (num_threadsI!=null)
        nt = num_threadsI.intValue();
      if (nt>1) BBThreadPool.setNumthreads(nt);

			int k = 2;
			Integer kI = (Integer) params.get("k");
			if (kI!=null) k = kI.intValue();
			System.err.println("Solving a "+k+"-packing problem.");  // itc: HERE rm asap
      Graph[] graphs = null;
      if (g.getNumComponents()>1) graphs = g.getGraphComponents();
      else {  // optimize when there is only one component in the graph
        graphs = new Graph[1];
        graphs[0] = g;
      }
      // now run the B&B algorithm for each sub-graph
      for (int j=0; j<graphs.length; j++) {
        Graph gj = graphs[j];
        System.err.println("Solving for subgraph "+(j+1)+" w/ sz="+gj.getNumNodes()+" (/"+graphs.length+")");
        if (gj.getNumNodes()<=3 && k==2) {
          ++_totActiveNodes;
					// figure out max. node-weight
					double maxw = Double.NEGATIVE_INFINITY;
					for (int m=0; m<gj.getNumNodes(); m++) {
						Double nmwD = gj.getNode(m).getWeightValue("value");
						double nmw = nmwD==null ? 1.0 : nmwD.doubleValue();
						if (nmw>maxw) maxw = nmw;
					}
					_totActiveNodeWeights += maxw;
					++_totLeafNodes;
          continue;
        } else if (k==1 && gj.getNumNodes()==3 && gj.getNumArcs()==2) {
					_totActiveNodes += 2;
					++_totLeafNodes;
					// figure out which node is the connecting one
					for (int m=0; m<3; m++) {
						Node nm = gj.getNode(m);
						if (nm.getNbors().size()==1) {
							Double nmwD = nm.getWeightValue("value");
							double w = nmwD==null ? 1.0 : nmwD.doubleValue();
							_totActiveNodeWeights += w;
						}
					}
					continue;
				} else if (k==1 && gj.getNumNodes()<=2) {
					++_totActiveNodes;
					++_totLeafNodes;
					// figure out max. node-weight
					double maxw = Double.NEGATIVE_INFINITY;
					for (int m=0; m<gj.getNumNodes(); m++) {
						Double nmwD = gj.getNode(m).getWeightValue("value");
						double nmw = nmwD==null ? 1.0 : nmwD.doubleValue();
						if (nmw>maxw) maxw = nmw;
					}
					_totActiveNodeWeights += maxw;
					continue;
				}
        BBTree t = null;
        // double bound = Double.MAX_VALUE;
        double bound = 0;
        t = new BBTree(gj, bound, k);
        if (maxnodes > 0) t.setMaxNodesAllowed(maxnodes);
        Integer maxQSizeI = (Integer) params.get("maxqsize");
        if (maxQSizeI != null && maxQSizeI.intValue() > 0)
          t.setMaxQSize(maxQSizeI.intValue());
        Boolean cutNodesB = (Boolean) params.get("cutnodes");
        if (cutNodesB != null) t.setCutNodes(cutNodesB.booleanValue());
        Boolean localSearchB = (Boolean) params.get("localsearch");
        if (localSearchB != null) t.setLocalSearch(localSearchB.booleanValue());
        Integer tlvl = (Integer) params.get("tightenboundlevel");
        if (tlvl != null && tlvl.intValue() >= 1) t.setTightenUpperBoundLvl(
					tlvl.intValue());
        Boolean usemaxsubsetsB = (Boolean) params.get("usemaxsubsets");
        if (usemaxsubsetsB != null)
          t.setUseMaxSubsets(usemaxsubsetsB.booleanValue());
        Integer kmaxI = (Integer) params.get("maxitersinGBNS2A");
        if (kmaxI!=null && kmaxI.intValue()>0)
          t.setMaxAllowedItersInGBNS2A(kmaxI.intValue());
        Double avgpercextranodes2addD = (Double) params.get("avgpercextranodes2add");
        if (avgpercextranodes2addD!=null)
          t.setAvgPercExtraNodes2Add(avgpercextranodes2addD.doubleValue());
        Integer recentI = (Integer) params.get("recentqueuesize");
        if (recentI != null && recentI.intValue() > 0
            && t.getUseMaxSubsets()==false)
          t.setRecentMaxLen(recentI.intValue());
        Integer maxchildrenI = (Integer) params.get("maxnodechildren");
        if (maxchildrenI != null && maxchildrenI.intValue() > 0)
          t.setMaxChildrenNodesAllowed(maxchildrenI.intValue());
        BBNodeComparatorIntf bbcomp = (BBNodeComparatorIntf) params.get("bbnodecomparator");
        if (bbcomp!=null) t.setBBNodeComparator(bbcomp);
        t.run();
        int orsoln[] = t.getSolution();
        int tan=0;
				double tanw = 0.0;
        for (int i = 0; i < orsoln.length; i++) {
          if (orsoln[i] == 1) {
						int mi = ((Integer)gj.getNodeLabel(i)).intValue();
            System.out.print( mi + " ");
            ++tan;
						Double twD = gj.getNode(i).getWeightValue("value");
						tanw += (twD==null ? 1.0 : twD.doubleValue());
          }
        }
				// tanw == t.getBound()
        System.err.println("Total BB-nodes=" + t.getCounter());
				System.err.println("Total leaf BB-nodes="+t.getTotLeafNodes());
        _totActiveNodes += tan;
				_totActiveNodeWeights += tanw;
				_totLeafNodes += t.getTotLeafNodes();
        System.err.println("Total active nodes so far: "+_totActiveNodes+" active node weights="+_totActiveNodeWeights+" total overall trees leaf BB nodes="+_totLeafNodes);
      }
      long time = System.currentTimeMillis() - start;
      System.out.println("Best Soln = "+_totActiveNodeWeights);
      System.out.println("\nWall-clock Time (msecs): "+time);
      System.out.println("Done.");
      System.exit(0);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

}

