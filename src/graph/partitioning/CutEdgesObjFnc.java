package graph.partitioning;

import graph.*;
import popt4jlib.*;
import java.util.*;

public class CutEdgesObjFnc implements ObjFncIntf {
  public CutEdgesObjFnc() {
  }


  /**
   * simply return the sum of weights of the cut-edges of g in partition.
   * @param g Graph
   * @param docs VectorIntf[]
   * @param partition int[]
   * @return double
   */
  public double value(Graph g, VectorIntf[] docs, int[] partition) {
    final int num_nodes = g.getNumNodes();
    // final int num_arcs = g.getNumArcs();
    double val = 0.0;
    for (int i=0; i<num_nodes; i++) {
      Node ni = g.getNode(i);
      Set ins = ni.getInLinks();  // Set<Integer nodeid>
      Iterator nit = ins.iterator();
      while (nit.hasNext()) {
        Integer inid = (Integer) nit.next();
        Link inlink = g.getLink(inid.intValue());
        if ((inlink.getStart()==i && partition[i] != partition[inlink.getEnd()]) ||
            (inlink.getEnd()==i && partition[i] != partition[inlink.getStart()]))
          val += inlink.getWeight();
      }
      // no need for the outlinks, since by counting the inlinks we have counted
      // all edges.
    }
    return val;
  }


  /**
   * return the sum of weights of cut edges emanating from each partition
   * @param g Graph
   * @param partition int[]
   * @return double[]
   */
  public double[] values(Graph g, int[] partition) {
    final int num_nodes = g.getNumNodes();
    // final int num_arcs = g.getNumArcs();
    // figure out num partitions
    int nparts = 0;
    for (int i=0; i<partition.length; i++)
      if (partition[i]>nparts) nparts = partition[i];
    double vals[] = new double[nparts];
    for (int i=0; i<num_nodes; i++) {
      Node ni = g.getNode(i);
      Set ins = ni.getInLinks();  // Set<Integer nodeid>
      Iterator nit = ins.iterator();
      while (nit.hasNext()) {
        Integer inid = (Integer) nit.next();
        Link inlink = g.getLink(inid.intValue());
        if ((inlink.getStart()==i && partition[i] != partition[inlink.getEnd()])) {
          vals[partition[i]-1] += inlink.getWeight();
          vals[partition[inlink.getEnd()]-1] += inlink.getWeight();
        }
        if ((inlink.getEnd()==i && partition[i] != partition[inlink.getStart()])) {
          vals[partition[i]-1] += inlink.getWeight();
          vals[partition[inlink.getStart()]-1] += inlink.getWeight();
        }
      }
      // no need for the outlinks, since by counting the inlinks we have counted
      // all edges.
    }
    return vals;
  }
}

