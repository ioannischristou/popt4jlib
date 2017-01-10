package graph.packing;

import java.util.*;
import java.io.*;
import utils.*;
import graph.*;
import parallel.SimpleFasterMsgPassingCoordinator;
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

	private static double _totActiveNodeWeights=0.0;

  /**
   * sole public no-arg constructor.
   */
  public DBBGASPPacker() {
  }


  /**
   * run as <CODE>java -cp &lt;classpath&gt; graph.packing.DBBGASPPacker &lt;graphfilename&gt; &lt;paramsfilename&gt; [maxnumBBnodes]</CODE>.
   * <br>args[0]: graph file name must adhere to the format specified in the
   * description of the method <CODE>utils.DataMgr.readGraphFromFile2(String file)</CODE>
   * <br>args[1]: params file name may define parameters in lines of the following
   * form:
   * <ul>
	 * <li> acchost, $string$ optional, the internet address of the 
	 * DAccumulatorSrv that will be accumulating incumbents. Default is localhost.
	 * <li> accport, $num$ optional, the port to which the DAccumulatorSrv listens.
	 * Default is 7900.
	 * <li> accnotificationshost, $string$ optional the internet address of the
	 * server sending accumulator notifications (may simply be the DAccumulatorSrv
	 * itself, or a BCastSrv to which the DAccumulatorSrv has also subscribed for
	 * scaling-up reasons.) Default is localhost.
	 * <li> accnotificationsport, $num$ optional, the port to which the 
	 * accnotificationshost server listens for clients wishing to receive 
	 * notifications about new incumbent solutions. Default is 9900.
	 * <li> cchost, $string$ optional, the internet address of the 
	 * DConditionCounterLLCSrv that will be listening for distributed 
	 * condition-counter requests. Default is localhost.
	 * <li> ccport, $num$ optional, the port to which the DConditionCounterLLCSrv 
	 * listens. Default is 7899.
	 * <li> pdahost, $string$ optional, the internet address of the 
	 * PDAsynchBatchTaskExecutorSrv that will be listening for distributed 
	 * tasks execution requests. Default is localhost.
	 * <li> pdaport, $num$ optional, the port to which the asynch distributed 
	 * executor server listens. Default is 7981.
   * <li> tightenboundlevel, $num$ optional, the depth in the B&amp;B tree 
	 * constructed at which a stronger computation of the upper bound will be 
	 * started, default is 0.
   * <li> localsearch, $boolean$ optional, if true, then when an incumbent
   * solution is found that cannot be further improved, a local search kicks in
   * to try to improve it using (unless there is another explicit specification)
	 * the (default) N1RXP(FirstImproving) neighborhood concept that basically
	 * attempts to remove a single node from the solution and then see how many
	 * other nodes it can add to the reduced solution. This local search can
	 * become quite expensive and for this reason it is only applied to final
	 * incumbent solutions in the B &amp; B-tree construction process. Default is 
	 * false.
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
	 * graphs (arising from wireless ad-hoc networks etc.) 
	 * <li> sortmaxsubsets, $boolean$ optional, if true, then the max subsets
	 * generated in method <CODE>getBestNodeSets2Add()</CODE> will be sorted in
	 * descending weight order so that if children <CODE>BBNode*</CODE> objects
	 * are "cut", they will be the "least" heavy-weight. Default is false.
	 * <li> maxitersinGBNS2A, $num$ optional, if present, the number represents 
	 * the max number of iterations the <CODE>getBestNodeSets2Add()</CODE> method 
	 * of <CODE>DBBNode1</CODE> class will be allowed to go through. Default is
	 * 100000 (specified in <CODE>DBBTree</CODE> class.)
	 * <li> useGWMIN2criterion, $boolean$ optional, if true, then when computing
	 * the best nodes to consider as a partial solution is being expanded, the
	 * "GWMIN2-heuristic" criterion (described in Sakai et. al. 2003: "A note on
	 * greedy algorithms for the MWIS problem", Discr. Appl. Math., 126:313-322)
	 * will be used for nodes selection in 1-packing problems. Default is false.
   * <li> maxnodechildren, $num$ optional, specify an upper bound on the number
   * of children any node is allowed to create. Default is Integer.MAX_VALUE.
   * <li> maxnodesallowed, $num$ optional, specify an upper bound on the number
   * of nodes any process is allowed to create. Default is Integer.MAX_VALUE.
   * <li>class,dbbnodecomparator, &lt;fullclassname&gt;[,args] optional, the 
	 * full class name of a class implementing the 
	 * <CODE>graph.packing.DBBNodeComparatorIntf</CODE> that is used to define the 
	 * order in which B&amp;B nodes in the tree are picked for processing. Default 
	 * is <CODE>graph.packing.DefDBBNodeComparator</CODE>.
	 * <li>ff, $num$ optional, specify the "fudge factor" used in determining what
	 * constitutes the list of "best nodes" in the 1-packing problem (a.k.a. the
	 * MWIS problem) where it makes much more sense to have a "fudge factor" by
	 * which to multiply the best cost in order to determine if a node is "close
	 * enough" to the best cost to be included in the best-candidate-nodes list.
	 * Default value is <CODE>DBBNode1._ff</CODE> (currently set to 0.85). The
	 * smaller this value, the longer it will take for the search to complete,
	 * with potentially better solutions found.
	 * <li> minknownbound, $num$ optional, a known bound to the problem at hand,
	 * which will be used to fathom B&amp;B nodes having smaller bounds than this
	 * number. Default is -infinity.
	 * <li> expandlocalsearchfactor, $num$ optional, if present, then when a
	 * solution is found within the specified factor of the best known solution,
	 * a local search kicks in. Default is 1.0 (only when a best solution is found
	 * does local search kicks in). 
	 * <li> rndgen,$num$[,$numthreads$] optional, if present specifies the seed of
	 * the random number generator. This seed will be used by all participating 
	 * JVMs. The numthreads argument makes sense only if all workers will utilize
	 * the same number of threads, otherwise should not be specified.
	 * <li> dbglvl,$num$ optional, if present specifies the debug-level for all
	 * classes and all processes in this run. Default is Integer.MAX_VALUE, 
	 * printing all messages sent to the <CODE>utils.Messenger</CODE> class.
   * </ul>
   * <br>args[2]: [optional] override max num nodes in params_file to create in
	 * B&amp;B procedure
	 * <p> This implementation writes the solution in a text file called "sol.out"
	 * in the current directory, whose lines contain one number each, the id of
	 * each "active" node in the solution (id in the set {1,...graph_num_nodes}).
	 * </p>
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
				System.err.println("Distributed BBGASPPacker does not currently support breaking graphs into disconnected components...");
				System.err.println("(Total number of components in graph="+g.getNumComponents()+")");
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
				String accnotificationshost = "localhost";
				if (params.containsKey("accnotificationshost")) accnotificationshost = (String) params.get("accnotificationshost");				
				int accnotificationsport = 9900;
				if (params.containsKey("accnotificationsport")) accnotificationsport = ((Integer) params.get("accnotificationsport")).intValue();				
        Boolean localSearchB = (Boolean) params.get("localsearch");
				boolean localsearch = false;
				if (localSearchB!=null) localsearch = localSearchB.booleanValue();
				AllChromosomeMakerClonableIntf maker = (AllChromosomeMakerClonableIntf) params.get("localsearchtype");
				Double ffD = (Double) params.get("ff");
				double ff = 0.85;
				if (ffD!=null) ff = ffD.doubleValue();
        Integer tlvlI = (Integer) params.get("tightenboundlevel");
				int tlvl = Integer.MAX_VALUE;
				if (tlvlI!=null && tlvlI.intValue()>=1) tlvl = tlvlI.intValue();
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
					ugwm2 = useGWMIN24BN2AB.booleanValue();
				Double expandlocalsearchfactorD = (Double) params.get("expandlocalsearchfactor");
				double elsf = 1.0;
				if (expandlocalsearchfactorD!=null)
					elsf = expandlocalsearchfactorD.doubleValue();
				double mkb = Double.NEGATIVE_INFINITY;
				Double minknownboundD = (Double) params.get("minknownbound");
				if (minknownboundD!=null) //t.setMinKnownBound(minknownboundD.doubleValue());
					mkb = minknownboundD.doubleValue();
				int maxchildren = Integer.MAX_VALUE;
        Integer maxchildrenI = (Integer) params.get("maxnodechildren");
        if (maxchildrenI != null && maxchildrenI.intValue() > 0)
					maxchildren = maxchildrenI.intValue();
        DBBNodeComparatorIntf bbcomp = (DBBNodeComparatorIntf) params.get("dbbnodecomparator");
				long seed = RndUtil.getInstance().getSeed();  // get whatever seed was set when props were read-in.
				int maxnodesallowed = Integer.MAX_VALUE;
				Integer mnaI = (Integer) params.get("maxnodesallowed");
				if (mnaI!=null && mnaI.intValue()>0)
					maxnodesallowed = mnaI.intValue();
				if (maxnodes>0) maxnodesallowed = maxnodes;  // override value
				/*
				DBBTree.init(args[0], g, bound, pdahost, pdaport, cchost, ccport, 
					           acchost, accport, accnotificationshost, accnotificationsport,
					           localsearch, maker, ff, tlvl, kmax, 
										 sortmaxsubsets, apen2a, ugwm2, elsf, mkb, maxchildren, bbcomp, 
										 seed,
										 true,  // true value indicates that a PDAsynchInitCmd must be sent to the asynch-server.
										 maxnodesallowed,
										 Messenger.getInstance().getDebugLvl());
				*/
				// the call below, also requires that the exactly specified params file
				// lives on each worker that will participate in the distributed process
				DBBTree.init(args[0], args[1], true); 
				DBBTree t = DBBTree.getInstance();
        t.run();
        int orsoln[] = t.getSolution();
				double tanw = 0.0;
        for (int i = 0; i < orsoln.length; i++) {
          if (orsoln[i] == 1) {
						Integer miI = (Integer) gj.getNodeLabel(i);
						int mi = (miI==null) ? i : miI.intValue();  // null miI -> g connected
						pw.println((mi+1));
						Double twD = gj.getNode(i).getWeightValue("value");
						tanw += (twD==null ? 1.0 : twD.doubleValue());
          }
        }
				_totActiveNodeWeights += tanw;
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

