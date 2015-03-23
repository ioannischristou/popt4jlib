package graph.packing;

import java.util.*;
import java.io.*;
import utils.*;
import graph.*;
import popt4jlib.AllChromosomeMakerClonableIntf;

/**
 * implements a hybrid parallel GASP-Branch&Bound heuristic scheme for the
 * 1- or 2-packing problem in graphs.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
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
   * <br>args[0]: graph file name must adhere to the format specified in the
   * description of the method <CODE>utils.DataMgr.readGraphFromFile2(String file)</CODE>
   * <br>args[1]: params file name may define parameters in lines of the following
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
   * to try to improve it using (unless there is another explicit specification)
	 * the (default) N1RXP(FirstImproving) neighborhood concept that basically 
	 * attempts to remove a single node from the solution and then see how many 
	 * other nodes it can add to the reduced solution. This local search can 
	 * become quite expensive and for this reason it is only applied to final 
	 * incumbent solutions in the B&B-tree construction process. Default is false.
	 * <li> class,localsearchtype, &ltfullclassname&gt[,optionalarguments] optional
	 * if present, will utilize in the local-search procedure the 
	 * <CODE>AllChromosomeMakerClonableIntf</CODE> specified in the classname, 
	 * constructed using the arguments specified (if present). For example,
	 * the line could be:
	 * <PRE>
	 * class,localsearchtype,graph.packing.IntSetN2RXPGraphAllMovesMaker,1
	 * </PRE>
	 * which would be of use with MWIS problems, for random graphs in the class
	 * C(n,p), producing G_{|V|,p} type random graphs. On the other hand, by 
	 * default, the <CODE>IntSetN1RXPFirstImprovingGraphAllMovesMakerMT</CODE>
	 * moves maker applies both for 1- and 2-packing problems local-search, which 
	 * is also better suited when solving MWIS problems arising from duals of disk
	 * graphs (arising from wireless ad-hoc networks etc.) Currently works only 
	 * with MWIS (k=1) type problems and is ignored for 2-packing problems.
   * <li> usemaxsubsets, $boolean$ optional, if false, then each GASP process
   * augmenting candidate packings will augment these sets one node at a time,
   * leading to the possibility that many active nodes in the B&B tree will
   * represent the same packing. In such a case, a "recent-nodes" queue will
   * be used to safe-guard against the possibility of having the same nodes
   * created and processed within a "short" interval. Default is true.
	 * <li> sortmaxsubsets, $boolean$ optional, if true, then the max subsets 
	 * generated in method <CODE>getBestNodeSets2Add()</CODE> will be sorted in
	 * descending weight order so that if children <CODE>BBNode*</CODE> objects 
	 * are "cut", they will be the "least" heavy-weight. Default is false.
	 * <li> maxitersinGBNS2A, $num$ optional, if present and also the
	 * "usemaxsubsets" key is true, then the number represents the max number of
	 * iterations the <CODE>getBestNodeSets2Add()</CODE> method of the
	 * <CODE>BBNode[1-2]</CODE> class will be allowed to go through. Default is
	 * 100000 (specified in <CODE>BBTree</CODE> class.)
	 * <li> useGWMIN2criterion, $boolean$ optional, if true, then when computing
	 * the best nodes to consider as a partial solution is being expanded, the 
	 * "GWMIN2-heuristic" criterion (described in Sakai et. al. 2003: "A note on 
	 * greedy algorithms for the MWIS problem", Discr. Appl. Math., 126:313-322) 
	 * will be used for nodes selection in 1-packing problems. Default is false.
   * <li> recentqueuesize, $num$ the length of the queue to be used when the
   * "usemaxsubsets" parameter is false.
   * <li> maxnodechildren, $num$ optional, specify an upper bound on the number
   * of children any node is allowed to create. Default is Integer.MAX_VALUE.
   * <li> class,bbnodecomparator, &ltfullclassname&gt optional, the full class
   * name of a class implementing the <CODE>graph.packing.BBNodeComparatorIntf</CODE>
   * that is used to define the order in which B&ampB nodes in the tree are picked
   * for processing. Default is <CODE>graph.packing.DefBBNodeComparator</CODE>.
	 * <li>ff, $num$ optional, specify the "fudge factor" used in determining what
	 * constitutes the list of "best nodes" in the 1-packing problem (a.k.a. the 
	 * MWIS problem) where it makes much more sense to have a "fudge factor" by
	 * which to multiply the best cost in order to determine if a node is "close
	 * enough" to the best cost to be included in the best-candidate-nodes list.
	 * Default value is <CODE>BBNode1._ff</CODE>  (currently set to 0.85). The 
	 * smaller this value, the longer it will take for the search to complete, 
	 * with potentially better solutions found.
	 * <li> minknownbound, $num$ optional, a known bound to the problem at hand, 
	 * which will be used to fathom B&ampB nodes having smaller bounds than this
	 * number. Currently only applies to 1-packing problems. Default is -infinity.
	 * <li> expandlocalsearchfactor, $num$ optional, if present, then when a 
	 * solution is found within the specified factor of the best known solution,
	 * a local search kicks in. Default is 1.0 (only when a best solution is found
	 * does local search kicks in). Currently only applies to 1-packing problems.
   * </ul>
   * <br>args[2]: [optional] override max num nodes in params_file to create in 
	 * B&ampB procedure
   * <br>args[3]: [optional] override numthreads in params_file to use
	 * <p> This implementation writes the solution in a text file called "sol.out"
	 * in the current directory, whose lines contain one number each, the id of 
	 * each "active" node in the solution (id in the set {1,...graph_num_nodes}).
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
					if (BBTree._curIncumbent!=null) {
						synchronized (BBTree.class) {
							Iterator it = BBTree._curIncumbent.iterator();
							while (it.hasNext()) {
								Node n = (Node) it.next();
								BBGASPPacker._totActiveNodeWeights += n.getWeightValue("value");
							}
							BBGASPPacker._totActiveNodes += BBTree._curIncumbent.size();
						}
					}
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
			Messenger.getInstance().msg("Solving a "+k+"-packing problem.",0);
      Graph[] graphs = null;
      if (g.getNumComponents()>1) graphs = g.getGraphComponents();
      else {  // optimize when there is only one component in the graph
        graphs = new Graph[1];
        graphs[0] = g;
      }
      // now run the B&B algorithm for each sub-graph
			PrintWriter pw = new PrintWriter(new FileWriter("sol.out"));
      for (int j=0; j<graphs.length; j++) {
        Graph gj = graphs[j];
        System.err.println("Solving for subgraph "+(j+1)+" w/ sz="+gj.getNumNodes()+" (/"+graphs.length+")");
        if (gj.getNumNodes()<=3 && k==2) {
          ++_totActiveNodes;
					// figure out max. node-weight
					int best_node_id=-1;
					double maxw = Double.NEGATIVE_INFINITY;
					for (int m=0; m<gj.getNumNodes(); m++) {
						Double nmwD = gj.getNode(m).getWeightValue("value");
						double nmw = nmwD==null ? 1.0 : nmwD.doubleValue();
						if (nmw>maxw) {
							maxw = nmw;
							Integer mI = (Integer) gj.getNodeLabel(m);
							best_node_id = mI==null ? m : mI.intValue();  // null mI -> g connected
						}
					}
					pw.println((best_node_id+1));
					_totActiveNodeWeights += maxw;
					++_totLeafNodes;
          continue;
        } else if (k==1 && gj.getNumNodes()==3 && gj.getNumArcs()==2) {
					_totActiveNodes += 2;
					++_totLeafNodes;
					// figure out which node is the connecting one
					int best_node_id=-1;
					for (int m=0; m<3; m++) {
						Node nm = gj.getNode(m);
						if (nm.getNbors().size()==1) {
							Double nmwD = nm.getWeightValue("value");
							double w = nmwD==null ? 1.0 : nmwD.doubleValue();
							_totActiveNodeWeights += w;
							Integer mI = (Integer) gj.getNodeLabel(m);
							best_node_id = mI == null ? m : mI.intValue();  // null mI -> g connected
							pw.println((best_node_id+1));
						}
					}
					continue;
				} else if (k==1 && gj.getNumNodes()<=2) {
					++_totActiveNodes;
					++_totLeafNodes;
					// figure out max. node-weight
					int best_node_id=-1;
					double maxw = Double.NEGATIVE_INFINITY;
					for (int m=0; m<gj.getNumNodes(); m++) {
						Double nmwD = gj.getNode(m).getWeightValue("value");
						double nmw = nmwD==null ? 1.0 : nmwD.doubleValue();
						if (nmw>maxw) {
							maxw = nmw;
							Integer mI = (Integer) gj.getNodeLabel(m);
							best_node_id = mI==null ? m : mI.intValue();  // null mI -> g connected
						}
					}
					_totActiveNodeWeights += maxw;
					pw.println((best_node_id+1));
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
				AllChromosomeMakerClonableIntf maker = (AllChromosomeMakerClonableIntf) params.get("localsearchtype");
				if (maker!=null) t.setLocalSearchType(maker);
				Double ffD = (Double) params.get("ff");
				if (ffD!=null) {
					BBNode1.setFF(ffD.doubleValue());
					BBNode1.disallowFFChanges();
				}
        Integer tlvl = (Integer) params.get("tightenboundlevel");
        if (tlvl != null && tlvl.intValue() >= 1) t.setTightenUpperBoundLvl(
					tlvl.intValue());
        Boolean usemaxsubsetsB = (Boolean) params.get("usemaxsubsets");
        if (usemaxsubsetsB != null)
          t.setUseMaxSubsets(usemaxsubsetsB.booleanValue());
        Integer kmaxI = (Integer) params.get("maxitersinGBNS2A");
        if (kmaxI!=null && kmaxI.intValue()>0)
          t.setMaxAllowedItersInGBNS2A(kmaxI.intValue());
				Boolean sortmaxsubsetsB = (Boolean) params.get("sortmaxsubsets");
				if (sortmaxsubsetsB!=null) 
					t.setSortBestCandsInGBNS2A(sortmaxsubsetsB.booleanValue());
        Double avgpercextranodes2addD = (Double) params.get("avgpercextranodes2add");
        if (avgpercextranodes2addD!=null)
          t.setAvgPercExtraNodes2Add(avgpercextranodes2addD.doubleValue());
				Boolean useGWMIN24BN2AB = (Boolean) params.get("useGWMIN2criterion");
				if (useGWMIN24BN2AB!=null)
					t.setUseGWMIN24BestNodes2Add(useGWMIN24BN2AB.booleanValue());
				Double expandlocalsearchfactorD = (Double) params.get("expandlocalsearchfactor");
				if (expandlocalsearchfactorD!=null) 
					t.setLocalSearchExpandFactor(expandlocalsearchfactorD.doubleValue());
				Double minknownboundD = (Double) params.get("minknownbound");
				if (minknownboundD!=null) t.setMinKnownBound(minknownboundD.doubleValue());
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
						Integer miI = (Integer) gj.getNodeLabel(i);
						int mi = (miI==null) ? i : miI.intValue();  // null miI -> g connected
            //System.out.print( mi + " ");
						pw.println((mi+1));
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
				System.err.println("Total #DLS searched performed: "+t.getNumDLSPerformed()+" Total time spent on DLS: "+t.getTimeSpentOnDLS()+" avg-local-search-time(per-thread)="+((double)t.getTimeSpentOnDLS()/(double)nt));
      }
			pw.flush();
			pw.close();
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

