package graph.partitioning;

import graph.*;
import java.util.HashMap;
import java.util.Vector;
import java.util.Collections;
import java.util.Set;
import java.util.Iterator;


public class HPartitionerGKW implements HPartitionerIntf {
  private double _gain = -1.0;
  private double _devPerc = 0.3;  // default allow 30% deviation in block sizes
  private int[] _blockSizes = null;
  private int _min_block_size, _max_block_size;
  private HObjFncIntf _obj = null;

  public HPartitionerGKW() {
  }


  public void setObjectiveFunction(HObjFncIntf f) {
    _obj = f;
  }


  /**
   * returns an array of size g.getNumNodes() with values in [1,...,k]
   * implements the Greedy K-way partitioning scheme (simplification over
   * F-M style algorithms since it visits nodes randomly and makes the best
   * move iff it leads to a gain>0, in terms of weight of cut-edges.)
   * @param g HGraph
   * @param k int
   * @param props HashMap
   * @return int[]
   */
  synchronized public int[] partition(HGraph g, int k, HashMap props) {
    final int num_nodes = g.getNumNodes();
    // ensure a valid _obj to work with
    if (_obj==null) {
      HObjFncIntf obj = (HObjFncIntf) props.get("partitioning.hpartrunner.objfnc");
      if (obj==null) obj = new HObjFncSOED();
      _obj = obj;
    }
    // try to find an initial partition to work with
    int[] init_partition = (int[]) props.get("initialpartition");
    // the init_partition[] is also expected to have elements in {1,...,k}
    int[] partition = new int[num_nodes];
    // figure out balance tolerance
    Integer bI = (Integer) props.get("partitioning.HPartitioner.partitionubfactor");
    if (bI!=null) {
      int b = bI.intValue();
      double l = Math.log((double) k)/Math.log(2.0);
      // _devPerc = (Math.pow((50+b)/100.0,l)-Math.pow((50-b)/100.0,l))*((double) k)/2.0;
      _devPerc = Math.pow(b/50.0, l);
    }
    // modify _devPerc if specified in props overrides metis spec.
    Number dp = (Number) props.get("partitioning.HPartitioner.partitiondevperc");
    if (dp!=null) _devPerc = dp.doubleValue();
    //System.err.println("HPartitionerGKW._devPerc="+_devPerc);
    if (init_partition==null) {
      // no, no initial partition
      // create initial partition ensuring no empty block
      Vector v = new Vector();
      int j=0;
      for (int i=0; i<num_nodes; i++) {
        v.addElement(new Integer(++j));
        if (j==k) j=0;
      }
      Collections.shuffle(v);
      for (int i=0; i<num_nodes; i++) {
        partition[i] = ((Integer) v.elementAt(i)).intValue();
      }
    }
    else {  // yes, there is an initial partition
      for (int i=0; i<num_nodes; i++) partition[i] = init_partition[i];
    }

    if (num_nodes==k) {
      // no need for further partitioning
      for (int i=0; i<k; i++)
        partition[i] = i+1;
      return partition;
    }

    if (_devPerc<=0) {
      _min_block_size = 1;
      _max_block_size = g.getNumNodes();
    }
    else {
      final double tot_graph_weight = g.getTotalNodeWeight();
      _min_block_size = (int) ( (1 - _devPerc) * tot_graph_weight / (double) k);
      if (_min_block_size == 0) _min_block_size = 1; // set lower limit
      _max_block_size = (int) ( (1 + _devPerc) * tot_graph_weight / (double) k);
      if (_max_block_size > g.getNumNodes()) _max_block_size = (int) tot_graph_weight;
    }
    _blockSizes = new int[k];
    for (int i=0; i<k; i++) _blockSizes[i]=0;
    for (int i=0; i<num_nodes; i++) _blockSizes[partition[i]-1] += (int) (g.getHNode(i).getWeightValue("cardinality").doubleValue());

    Vector l = new Vector();
    for (int i=0; i<num_nodes; i++)
      l.addElement(new Integer(i));
    boolean cont=true;
    int cont_count = 0;
    while (cont) {
      cont_count++;
      cont=false;  // turn into true only if there is an improvement in the loop
      Collections.shuffle(l, utils.RndUtil.getInstance().getRandom());  // get a random permutation
      double oldval = _obj.value(g, partition);
      for (int i=0; i<num_nodes; i++) {
        Integer nId = (Integer) l.elementAt(i);
        HNode ni = g.getHNode(nId.intValue());
        double nweight = ni.getWeightValue("cardinality").doubleValue();
        int best_part = bestPart(g, k, partition, ni, props);
        // method bestPart updates correctly _gain
        if (_gain>0) {
          // do the move
          _blockSizes[best_part-1] += nweight;
          _blockSizes[partition[nId.intValue()]-1] -= nweight;
          partition[nId.intValue()] = best_part;
          cont = true;
        }
      }
      // break out if no gain for 1 consecutive passes
      double newval = _obj.value(g,partition);
      if (newval>=oldval && cont_count>=1)
        cont=false;
      else if (newval<oldval) cont_count=0;
    }
    return partition;
  }


  /**
   * find the best partition that node ni can go to, or -1 if constraints are
   * violated by this move. Also sets the best gain found in _gain.
   * @param g HGraph
   * @param k int
   * @param partition int[]
   * @param ni HNode
   * @param props HashMap
   * @return int the partition block to move ni, or -1 else
   */
  private int bestPartOld(HGraph g, int k, int[] partition, HNode ni,
                                    HashMap props) {
    _gain = Double.NEGATIVE_INFINITY;
    final int ni_part = partition[ni.getId()];  // 1...k
    // can we move node ni?
    if (_blockSizes[partition[ni.getId()]-1] <= _min_block_size) return -1;
    double res = 0.0;
    int best = 0;  // best part to move to
    double[] weights = new double[k+1];
    // weights[i] is the weight with which ni connects to partition block i
    // i=1...k
    for (int i=0; i<=k; i++) weights[i] = 0.0;
    Set linkids = ni.getHLinkIds();
    Iterator liter = linkids.iterator();

    while (liter.hasNext()) {
      Integer lid = (Integer) liter.next();
      HLink l = g.getHLink(lid.intValue());
      Iterator nbiter = l.getHNodeIds();
      while (nbiter.hasNext()) {
        int nborid = ((Integer) nbiter.next()).intValue();
        if (nborid==ni.getId()) continue;  // don't count self
        weights[partition[nborid]]+=l.getWeight()/((double) l.getNumNodes()-1);
      }
    }

    for (int i=1; i<=k; i++) {
      if (i==ni_part) continue;
      // can we add ni to block i?
      if (_blockSizes[i-1]>=_max_block_size) continue;
      // the move of ni to block i may not cause a reduction in the SOED objective
      // but it will "tend" to bring together highly connected nodes.
      if (_gain < weights[i] - weights[ni_part]) {
        _gain = weights[i] - weights[ni_part];
        best = i;
      }
    }
    return best;
  }


  private int bestPart(HGraph g, int k, int[] partition, HNode ni,
                       HashMap props) {
    _gain = Double.NEGATIVE_INFINITY;
    final int ni_part = partition[ni.getId()];  // 1...k
    final double nweight = ni.getWeightValue("cardinality").doubleValue();
    // can we move node ni?
    if (_blockSizes[ni_part-1]-nweight < _min_block_size) return -1;
    // sum the weights of nets that have all nodes (except ni) in another
    // partition. Sum weights of all nets that are totally in partition of ni.
    // The difference is the "gain" of moving to the other partition.
    double weights[] = new double[k+1];
    for (int i=0; i<weights.length; i++) weights[i]=0.0;
    Set linkids = ni.getHLinkIds();
    Iterator liter = linkids.iterator();
    double in_weight = 0.0;
    while (liter.hasNext()) {
      HLink l = g.getHLink(((Integer) liter.next()).intValue());
      int other_part = othersInPartition(l, ni, partition);
      if (other_part>0) {
        weights[other_part] += 2*l.getWeight();
      }
      else if (isCut(l, partition)==false) {
        in_weight += 2*l.getWeight();
      }
      else {
        // the link is cut. If we move ni to partition i!=ni_part, do we
        // pay any price in SOED?
        for (int j=1;j<=k;j++) {
          if (j == ni_part)continue;
          boolean l_onlyhasni_in_nipart = linkOnlyHasOneInPart(l, ni_part, partition);
          if (linkCutsThroughPart(l, j, partition)) {
            if (l_onlyhasni_in_nipart)
              weights[j] += l.getWeight();
          }
          else {  // l had no node in partition j
            if (l_onlyhasni_in_nipart==false)
              weights[j] -= l.getWeight();
          }
        }
      }
    }
    int best = 0;  // best part to move to
    double best_w = Double.NEGATIVE_INFINITY;
    for (int i=1; i<=k; i++) {
      if (i==ni_part) continue;  // don't consider the partition ni is already in
      // respect constraints
      if (_blockSizes[i-1]+nweight > _max_block_size) continue;
      // now we're ok
      if (best_w < weights[i]) {
        best_w = weights[i];
        best = i;
        _gain = best_w - in_weight;
      }
    }
    if (_gain > 0) {
      return best;
    }
    return -1;
  }


  private boolean isCut(HLink l, int[] partition) {
    Iterator it = l.getHNodeIds();
    int fp = partition[((Integer) it.next()).intValue()];
    while (it.hasNext()) {
      Integer nid = (Integer) it.next();
      if (partition[nid.intValue()]!=fp) return true;
    }
    return false;
  }


  /**
   * returns -1 if the link l does not have all nodes except n in a single partition.
   * Else, it returns that other partition's id.
   * @param l HLink
   * @param n HNode
   * @param partition int[]
   * @return int
   */
  private int othersInPartition(HLink l, HNode n, int[] partition) {
    int npart = partition[n.getId()];
    Iterator it = l.getHNodeIds();
    int nid = ((Integer) it.next()).intValue();
    if (nid==n.getId()) nid = ((Integer) it.next()).intValue();
    int fp = partition[nid];
    if (fp==npart) return -1;
    while (it.hasNext()) {
      Integer nid2 = (Integer) it.next();
      if (nid2.intValue()==n.getId()) continue;
      if (partition[nid2.intValue()]!=fp) return -1;
    }
    return fp;
  }


  private boolean linkOnlyHasOneInPart(HLink l, int p, int[] partition) {
    int count = 0;
    Iterator niter = l.getHNodeIds();
    while (niter.hasNext()) {
      Integer nid = (Integer) niter.next();
      if (partition[nid.intValue()]==p) count++;
    }
    return (count==1);
  }


  private boolean linkCutsThroughPart(HLink l, int p, int[] partition) {
    Iterator niter = l.getHNodeIds();
    while (niter.hasNext()) {
      Integer nid = (Integer) niter.next();
      if (partition[nid.intValue()]==p) return true;
    }
    return false;
  }
}
