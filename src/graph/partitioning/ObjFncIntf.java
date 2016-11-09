package graph.partitioning;

import graph.*;
import popt4jlib.*;

public interface ObjFncIntf {
  double value(Graph g, VectorIntf[] docs, int[] partition);
  double[] values(Graph g, int[] partition);
}
