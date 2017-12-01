package utils;

import java.util.*;
import java.io.*;
import graph.*;
import parallel.*;


/**
 * builds random graphs that are "staged" between a source and a sink node.
 * The graph has user-defined "stages" that communicate with the previous and
 * the next stage. The nodes within a stage may also have edges connecting them.
 * The user defines the "depth" of the graph (number of stages), then "width"
 * of the graph (expected number of nodes in each stage), and the expected
 * number of connections between stages, as well as within any stage.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RandomStageGraphMaker {
	private int _depth;
	private int _width;
	private double _expNumArcsBetweenStages;
	private double _expNumArcsWithinStage;
	private Random _r;


	/**
	 * sole constructor.
	 * @param depth int the "depth" of the graph ie the number of stages from src
	 * node (0) to destination node (with id = #nodes-1)
	 * @param width int the expected number of nodes in each stage
	 * @param enebs int the expected number of edges connecting nodes from stage
	 * i to stage i+1
	 * @param enews int the expected number of edges connecting nodes within a
	 * stage
	 * @param seed int random number seed
	 */
	public RandomStageGraphMaker(int depth, int width,
		                           double enebs, double enews,
															 long seed) {
		_depth=depth;
		_width=width;
		this._expNumArcsBetweenStages=enebs;
		this._expNumArcsWithinStage=enews;
		_r = RndUtil.getInstance().getRandom();
		_r.setSeed(seed);
	}


	/**
	 * constructs the <CODE>graph.Graph</CODE> object corresponding to the
	 * specifications of the constructor.
	 * @return Graph
	 */
	public Graph buildGraph() {
		// first compute num_nodes=\sum{#nodes_i} + 2 (source and sink)
		// source-node-id=0
		// dest-node-id=last-graph-node-id
		int numnodes=2;
		int numarcs=0;
		List num_nodes_per_stage = new ArrayList();  // List<Integer nn_i>
		Set edges_from_stage = new HashSet();  // Set<PairIntInt s-d-pair>
		Set edges_within_stage = new HashSet();  // Set<PairIntInt s-d-pair>
		int start_node_i = 1;
		int prev_start_node_i = 0;
		for (int i=0; i<_depth; i++) {
			int nn_i = _r.nextInt(_width*2-1)+1;
			System.err.print("working on stage-"+i+" having "+nn_i+" nodes...");
			int end_node_i = start_node_i+nn_i-1;
			numnodes += nn_i;
			num_nodes_per_stage.add(new Integer(nn_i));
			// edges from previous stage to this stage
			int nebs = (int) (_r.nextDouble()*2*_expNumArcsBetweenStages);
			for (int j=0; j<nebs; j++) {
				int s = _r.nextInt(start_node_i-prev_start_node_i)+prev_start_node_i;
				int t = _r.nextInt(end_node_i+1-start_node_i)+start_node_i;
				edges_from_stage.add(new PairIntInt(s,t));
			}
			System.err.print(nebs+" edges created between stage-"+(i-1)+" & stage-"+i+"...");
			// edges within this stage
			int news = (int) (_r.nextDouble()*2*_expNumArcsWithinStage);
			for (int j=0;j<news; j++) {
				int s = _r.nextInt(end_node_i+1-start_node_i)+start_node_i;
				int t = _r.nextInt(end_node_i+1-start_node_i)+start_node_i;
				edges_within_stage.add(new PairIntInt(s,t));
			}
			System.err.println(news+" edges within stage");
			// update for next stage
			prev_start_node_i = start_node_i;
			start_node_i=end_node_i+1;
		}
		// edges from last stage to sink
		int nebs = (int) (_r.nextDouble()*2*_expNumArcsBetweenStages);
		if (nebs > ((Integer)num_nodes_per_stage.get(num_nodes_per_stage.size()-1)).
			                                         intValue()) {
			nebs = ((Integer)num_nodes_per_stage.get(num_nodes_per_stage.size()-1)).
				                                     intValue();
		}
		int t = numnodes-1;
		for (int i=0; i<nebs; i++) {
			int s = _r.nextInt(start_node_i-prev_start_node_i)+prev_start_node_i;
			edges_from_stage.add(new PairIntInt(s,t));
		}
		numarcs = edges_from_stage.size()+edges_within_stage.size();
		Graph g = Graph.newGraph(numnodes, numarcs);
		Iterator it = edges_from_stage.iterator();
		while (it.hasNext()) {
			PairIntInt stp = (PairIntInt) it.next();
			double w = _r.nextDouble();
			try {
				g.addLink(stp.getFirst(), stp.getSecond(), w);
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		it = edges_within_stage.iterator();
		while (it.hasNext()) {
			PairIntInt stp = (PairIntInt) it.next();
			double w = _r.nextDouble();
			try {
				g.addLink(stp.getFirst(), stp.getSecond(), w);
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		for (int i=0; i<numnodes; i++) {
			Node ni = g.getNode(i);
			try {
				ni.setWeight("value", new Double(1.0));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return g;
	}


  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; utils.RandomStageGraphMaker
	 * &lt;depth&gt;
   * &lt;width&gt;
	 * &lt;expected_num_edges_between_stages&gt;
	 * &lt;expected_num_edges_within_stage&gt;
	 * &lt;filename&gt;
	 * [rndseed(0)]
	 * </CODE>.
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length<5) {
      System.err.println("usage: java -cp <classpath> "+
				                 "utils.RandomStageGraphMaker "+
				                 "<depth> <width> "+
				                 "<E[#arcs_between_cons_stages]> "+
				                 "<E[#arcs_within_stage]> "+
				                 "<filename> [rndseed(0)]");
      System.exit(-1);
    }
    try {
      long start_time = System.currentTimeMillis();
      long seed=0;
      if (args.length>5) seed = Long.parseLong(args[5]);
      RandomStageGraphMaker maker = new RandomStageGraphMaker(
				                                            Integer.parseInt(args[0]),
                                                    Integer.parseInt(args[1]),
                                                    Double.parseDouble(args[2]),
                                                    Double.parseDouble(args[3]),
                                                    seed);
      Graph g = maker.buildGraph();
			long lt = System.currentTimeMillis();
      System.err.println("Graph has "+g.getNumComponents()+" components");
			long ld = System.currentTimeMillis()-lt;
			System.err.println("time to label graph="+ld+" msecs");
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

