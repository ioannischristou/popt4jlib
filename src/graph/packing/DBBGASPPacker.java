package graph.packing;

import java.util.*;
import java.io.*;
import utils.*;
import graph.*;
import popt4jlib.AllChromosomeMakerClonableIntf;

/**
 * implements a hybrid parallel-distributed GASP-Branch &amp; Bound heuristic 
 * scheme for the 1-packing problem in graphs (Maximum Weighted Independent Set
 * problem - MWIS).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class DBBGASPPacker {

  private static int _totActiveNodes=0;
	private static double _totActiveNodeWeights=0.0;
	private static int _totLeafNodes=0;

  /**
   * sole public no-arg constructor.
   */
  public DBBGASPPacker() {
  }


  /**
   * run as <CODE>java -cp &lt;classpath&gt; graph.packing.DBBGASPPacker &lt;graphfilename&gt; &lt;paramsfilename&gt; [maxnumBBnodes] [numthreads]</CODE>.
   * <br>args[0]: graph file name must adhere to the format specified in the
   * description of the method <CODE>utils.DataMgr.readGraphFromFile2(String file)</CODE>
   * <br>args[1]: params file name may define parameters in lines of the following
   * form:
   * <ul>
	 * <li> acchost, $string$ optional, the internet address of the DAccumulatorSrv
	 * that will be accumulating incumbents. Default is localhost.
	 * <li> accport, $num$ optional, the port to which the DAccumulatorSrv listens.
	 * Default is 7900.
	 * <li> cchost, $string$ optional, the internet address of the DConditionCounterSrv
	 * that will be listening for distributed condition-counter requests. Default 
	 * is localhost.
	 * <li> ccport, $num$ optional, the port to which the DAccumulatorSrv listens.
	 * Default is 7899.
	 * <li> pdahost, $string$ optional, the internet address of the 
	 * PDAsynchBatchTaskExecutorSrv that will be listening for distributed 
	 * tasks execution requests. Default is localhost.
	 * <li> pdaport, $num$ optional, the port to which the asynch distributed 
	 * executor server listens. Default is 7981.
   * <li> tightenboundlevel, $num$ optional, the depth in the B&amp;B tree constructed
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
	 * incumbent solutions in the B &amp; B-tree construction process. Default is false.
	 * <li> class,localsearchtype, &lt;fullclassname&gt;[,optionalarguments] optional
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
   * leading to the possibility that many active nodes in the B&amp;B tree will
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
	 * <CODE>DBBNode1</CODE> class will be allowed to go through. Default is
	 * 100000 (specified in <CODE>DBBTree</CODE> class.)
	 * <li> useGWMIN2criterion, $boolean$ optional, if true, then when computing
	 * the best nodes to consider as a partial solution is being expanded, the
	 * "GWMIN2-heuristic" criterion (described in Sakai et. al. 2003: "A note on
	 * greedy algorithms for the MWIS problem", Discr. Appl. Math., 126:313-322)
	 * will be used for nodes selection in 1-packing problems. Default is false.
   * <li> maxnodechildren, $num$ optional, specify an upper bound on the number
   * of children any node is allowed to create. Default is Integer.MAX_VALUE.
   * <li> class,dbbnodecomparator, &lt;fullclassname&gt; optional, the full class
   * name of a class implementing the <CODE>graph.packing.DBBNodeComparatorIntf</CODE>
   * that is used to define the order in which B&amp;B nodes in the tree are picked
   * for processing. Default is <CODE>graph.packing.DefDBBNodeComparator</CODE>.
	 * <li>ff, $num$ optional, specify the "fudge factor" used in determining what
	 * constitutes the list of "best nodes" in the 1-packing problem (a.k.a. the
	 * MWIS problem) where it makes much more sense to have a "fudge factor" by
	 * which to multiply the best cost in order to determine if a node is "close
	 * enough" to the best cost to be included in the best-candidate-nodes list.
	 * Default value is <CODE>DBBNode1._ff</CODE>  (currently set to 0.85). The
	 * smaller this value, the longer it will take for the search to complete,
	 * with potentially better solutions found.
	 * <li> minknownbound, $num$ optional, a known bound to the problem at hand,
	 * which will be used to fathom B&amp;B nodes having smaller bounds than this
	 * number. Currently only applies to 1-packing problems. Default is -infinity.
	 * <li> expandlocalsearchfactor, $num$ optional, if present, then when a
	 * solution is found within the specified factor of the best known solution,
	 * a local search kicks in. Default is 1.0 (only when a best solution is found
	 * does local search kicks in). Currently only applies to 1-packing problems.
   * </ul>
   * <br>args[2]: [optional] override max num nodes in params_file to create in
	 * B&amp;B procedure
	 * <p> This implementation writes the solution in a text file called "sol.out"
	 * in the current directory, whose lines contain one number each, the id of
	 * each "active" node in the solution (id in the set {1,...graph_num_nodes}).
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      if (args.length<2) {
        System.err.println("usage: java -cp <classpath> graph.packing.DBBGASPPacker <graphfilename> <paramsfilename> [maxnumBBnodes]");
        System.exit(-1);
      }
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
      HashMap params = DataMgr.readPropsFromFile(args[1]);
      int maxnodes = -1;
      if (args.length>2)
        maxnodes = Integer.parseInt(args[2]);  // override max num nodes

      Graph[] graphs = null;
      if (g.getNumComponents()>1) {
				//graphs = g.getGraphComponents();
				System.err.println("Distributed BBGASPPacker does not currently support breaking graphs into disconnected components...");
				System.exit(-1);
			}
      else {  // optimize when there is only one component in the graph
        graphs = new Graph[1];
        graphs[0] = g;
      }
      // now run the B&B algorithm for each sub-graph
			PrintWriter pw = new PrintWriter(new FileWriter("sol.out"));
      for (int j=0; j<graphs.length; j++) {
        Graph gj = graphs[j];
        System.err.println("Solving for subgraph "+(j+1)+" w/ sz="+gj.getNumNodes()+" (/"+graphs.length+")");
        if (gj.getNumNodes()==3 && gj.getNumArcs()==2) {
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
				} else if (gj.getNumNodes()<=2) {
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
        // double bound = Double.MAX_VALUE;
        double bound = 0;
				String pdahost = "localhost";
				if (params.containsKey("pdahost")) pdahost = (String) params.get("pdahost");
				int pdaport = 7981;
				if (params.containsKey("pdaport")) pdaport = ((Integer) params.get("pdaport")).intValue();
				String cchost = "localhost";
				if (params.containsKey("cchost")) cchost = (String) params.get("cchost");
				int ccport = 7899;
				if (params.containsKey("ccport")) ccport = ((Integer) params.get("ccport")).intValue();
				String acchost = "localhost";
				if (params.containsKey("acchost")) acchost = (String) params.get("acchost");
				int accport = 7900;
				if (params.containsKey("accport")) accport = ((Integer) params.get("accport")).intValue();
        //DBBTree.init(g, bound, pdahost, pdaport, cchost, ccport, acchost, accport);
        //DBBTree t = DBBTree.getInstance();
        Boolean localSearchB = (Boolean) params.get("localsearch");
        //if (localSearchB != null) t.setLocalSearch(localSearchB.booleanValue());
				boolean localsearch = false;
				if (localSearchB!=null) localsearch = localSearchB.booleanValue();
				AllChromosomeMakerClonableIntf maker = (AllChromosomeMakerClonableIntf) params.get("localsearchtype");
				//if (maker!=null) t.setLocalSearchType(maker);
				Double ffD = (Double) params.get("ff");
				/*
				if (ffD!=null) {
					DBBNode1.setFF(ffD.doubleValue());
					DBBNode1.disallowFFChanges();
				}
				*/
				double ff = 0.85;
				if (ffD!=null) ff = ffD.doubleValue();
        Integer tlvlI = (Integer) params.get("tightenboundlevel");
        /*
				if (tlvlI != null && tlvlI.intValue() >= 1) t.setTightenUpperBoundLvl(
					tlvlI.intValue());
        */
				int tlvl = Integer.MAX_VALUE;
				if (tlvlI!=null && tlvlI.intValue()>=1) tlvl = tlvlI.intValue();
				Boolean usemaxsubsetsB = (Boolean) params.get("usemaxsubsets");
        boolean usemaxsubsets = true;
				if (usemaxsubsetsB != null)
          //t.setUseMaxSubsets(usemaxsubsetsB.booleanValue());
					usemaxsubsets = usemaxsubsetsB.booleanValue();
        int kmax = Integer.MAX_VALUE;
				Integer kmaxI = (Integer) params.get("maxitersinGBNS2A");
        if (kmaxI!=null && kmaxI.intValue()>0)
          //t.setMaxAllowedItersInGBNS2A(kmaxI.intValue());
					kmax = kmaxI.intValue();
				Boolean sortmaxsubsetsB = (Boolean) params.get("sortmaxsubsets");
				boolean sortmaxsubsets = false;
				if (sortmaxsubsetsB!=null)
					//t.setSortBestCandsInGBNS2A(sortmaxsubsetsB.booleanValue());
					sortmaxsubsets = sortmaxsubsetsB.booleanValue();
        Double avgpercextranodes2addD = (Double) params.get("avgpercextranodes2add");
				double apen2a = 0.0;
        if (avgpercextranodes2addD!=null)
          //t.setAvgPercExtraNodes2Add(avgpercextranodes2addD.doubleValue());
					apen2a = avgpercextranodes2addD.doubleValue();
				Boolean useGWMIN24BN2AB = (Boolean) params.get("useGWMIN2criterion");
				boolean ugwm2 = false;
				if (useGWMIN24BN2AB!=null)
					//t.setUseGWMIN24BestNodes2Add(useGWMIN24BN2AB.booleanValue());
					ugwm2 = useGWMIN24BN2AB.booleanValue();
				Double expandlocalsearchfactorD = (Double) params.get("expandlocalsearchfactor");
				double elsf = 1.0;
				if (expandlocalsearchfactorD!=null)
					//t.setLocalSearchExpandFactor(expandlocalsearchfactorD.doubleValue());
					elsf = expandlocalsearchfactorD.doubleValue();
				double mkb = Double.NEGATIVE_INFINITY;
				Double minknownboundD = (Double) params.get("minknownbound");
				if (minknownboundD!=null) //t.setMinKnownBound(minknownboundD.doubleValue());
					mkb = minknownboundD.doubleValue();
				int maxchildren = Integer.MAX_VALUE;
        Integer maxchildrenI = (Integer) params.get("maxnodechildren");
        if (maxchildrenI != null && maxchildrenI.intValue() > 0)
          //t.setMaxChildrenNodesAllowed(maxchildrenI.intValue());
					maxchildren = maxchildrenI.intValue();
        DBBNodeComparatorIntf bbcomp = (DBBNodeComparatorIntf) params.get("dbbnodecomparator");
        //if (bbcomp!=null) t.setDBBNodeComparator(bbcomp);
				DBBTree.init(g, bound, pdahost, pdaport, cchost, ccport, acchost, accport,
					           localsearch, maker, ff, tlvl, usemaxsubsets, kmax, 
										 sortmaxsubsets, apen2a, ugwm2, elsf, mkb, maxchildren, bbcomp);
				DBBTree t = DBBTree.getInstance();
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
				System.err.println("Total #DLS searched performed: "+t.getNumDLSPerformed()+" Total time spent on DLS: "+t.getTimeSpentOnDLS());
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

