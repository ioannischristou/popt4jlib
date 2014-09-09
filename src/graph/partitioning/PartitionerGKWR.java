package graph.partitioning;

import graph.*;
import parallel.ParallelException;
import utils.*;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Collections;
import java.util.Set;
import java.util.Iterator;
import java.util.Arrays;
import java.io.*;


public class PartitionerGKWR implements PartitionerIntf {
  private double _gain = -1.0;
  private double _devPerc = 0.3;  // default allow 30% deviation in block sizes
  private int[] _blockSizes = null;
  private int _min_block_size, _max_block_size;
  private ObjFncIntf _f;


  /**
   * the main method of the class that accepts a graphfile, a properties
   * file and the number of partitions k as input and produces
   * an output partitionfile of the graph.
   */
  public static void main(String[] args) {
    if (args.length!=4) {
      System.err.println(
          "usage: java -cp <classpath> partitioning.PartitionerGKWR "+
          "<graphfile> <propertiesfile> <k> <partfile>\n");
      return;
    }
    String graphfile = args[0];
    String propertiesfile = args[1];
    int k = Integer.parseInt(args[2]);
    String partfile = args[3];
    try {
      Graph g = DataMgr.readGraphFromhMeTiSFile(graphfile);
      Hashtable props = DataMgr.readPropsFromFile(propertiesfile);
      PartitionerIntf parter = new PartitionerGKWR();
      // do the partitioning
      int[] part = parter.partition(g, k, props);
      // write soln
      PrintWriter pw = new PrintWriter(new FileWriter(partfile));
      for (int i=0; i<part.length; i++)
        pw.println(part[i]);
      pw.flush();
      pw.close();
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }
  }


  public PartitionerGKWR() {
  }


  public void setObjectiveFunction(ObjFncIntf f) {
    _f = f;
  }


  /**
   * returns an array of size g.getNumNodes() with values in [1,...,k]
   * implements the Greedy K-way partitioning scheme (simplification over
   * F-M style algorithms since it visits nodes randomly and makes the best
   * move iff it leads to a gain>0, in terms of weight of cut-edges.)
   * @param g Graph
   * @param k int
   * @param props Hashtable
   * @return int[]
   */
  synchronized public int[] partition(Graph g, int k, Hashtable props) {
    try {
      final int num_nodes = g.getNumNodes();
      // try to find an initial partition to work with
      int[] init_partition = (int[]) props.get("initialpartition");
      int[] partition = new int[num_nodes];
      // modify _devPerc if specified in props
      Number dp = (Number) props.get("partitiondevperc");
      if (dp != null) _devPerc = dp.doubleValue();
      _f = (ObjFncIntf) props.get("partitionerobjfnc");
      if (_f == null) _f = new CutEdgesObjFnc(); // default obj function
      Integer num_triesI = (Integer) props.get("numtries");
      int num_tries = num_triesI != null ? num_triesI.intValue() :
          5000 / g.getNumNodes() + 1; // default number restarts

      if (init_partition == null) {
        // no, no initial partition
        // create initial partition ensuring no empty block
        int j = 0;
        int numcomps = g.getNumComponents();
        initializePartition(g, partition, k);
        if (numcomps == k) {
          // no need for further partitioning
          return partition;
        }
      }
      else {
        // yes, there is an initial partition
        for (int i = 0; i < num_nodes; i++) partition[i] = init_partition[i];
      }

      if (num_nodes == k) {
        // no need for further partitioning
        for (int i = 0; i < k; i++)
          partition[i] = i + 1;
        return partition;
      }

      if (_devPerc <= 0) {
        _min_block_size = 1;
        _max_block_size = g.getNumNodes();
      }
      else {
        _min_block_size = (int) ( (1 - _devPerc) * g.getNumNodes() / (double) k);
        if (_min_block_size == 0) _min_block_size = 1; // set lower limit
        _max_block_size = (int) ( (1 + _devPerc) * g.getNumNodes() / (double) k);
        if (_max_block_size > g.getNumNodes()) _max_block_size = g.getNumNodes();
      }

      _blockSizes = new int[k];

      int bestpartition[] = new int[num_nodes];
      double best_val = Double.MAX_VALUE;

      // repeat num_tries times
      for (int tries = 0; tries < num_tries; tries++) {
        for (int i = 0; i < k; i++) _blockSizes[i] = 0;
        for (int i = 0; i < num_nodes; i++) _blockSizes[partition[i] - 1]++;
        Vector l = new Vector();
        for (int i = 0; i < num_nodes; i++) l.addElement(new Integer(i));
        boolean cont = true;
        while (cont) {
          cont = false; // turn into true only if there is an improvement in the loop
          Collections.shuffle(l, utils.RndUtil.getInstance().getRandom()); // get a random permutation
          for (int i = 0; i < num_nodes; i++) {
            Integer nId = (Integer) l.elementAt(i);
            Node ni = g.getNode(nId.intValue());
            int best_part = bestPart(g, k, partition, ni, props);
            // method bestPart updates correctly _gain
            if (_gain > 0) {
              // do the move and break
              _blockSizes[best_part - 1]++;
              _blockSizes[partition[nId.intValue()] - 1]--;
              partition[nId.intValue()] = best_part;
              cont = true;
            }
          }
        }
        double cval = _f.value(g, null, partition);
        if (cval < best_val) {
          best_val = cval;
          System.out.println("new best cost=" + best_val);
          for (int i = 0; i < num_nodes; i++)
            bestpartition[i] = partition[i];
        }
        // new partition
        if (init_partition == null) initializePartition(g, partition, k);
        else {
          // reset
          System.err.println(
              "PartitionerGKWR.partition(): reseting partition to passed init_partition...");
          for (int i = 0; i < num_nodes; i++) partition[i] = init_partition[i];
        }
      }
      return bestpartition;
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }


  /**
   * find the best partition that node ni can go to, or -1 if constraints are
   * violated by this move. Also sets the best gain found in _gain.
   * @param g Graph
   * @param k int
   * @param partition int[]
   * @param ni Node
   * @param props Hashtable
   * @return int the partition block to move ni, or -1 else
   */
  private int bestPart(Graph g, int k, int[] partition, Node ni,
                                    Hashtable props) {
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
    Set inarcsnbors = ni.getInLinks();
    Iterator iter = inarcsnbors.iterator();
    while (iter.hasNext()) {
      Integer inlinkid = (Integer) iter.next();
      Link inlink = g.getLink(inlinkid.intValue());
      int nborid = inlink.getStart();
      Node nbor = g.getNode(nborid);
      weights[partition[nbor.getId()]] += inlink.getWeight();
    }
    Set outarcsnbors = ni.getOutLinks();
    iter = outarcsnbors.iterator();
    while (iter.hasNext()) {
      Integer outlinkid = (Integer) iter.next();
      Link outlink = g.getLink(outlinkid.intValue());
      int nborid = outlink.getEnd();
      Node nbor = g.getNode(nborid);
      weights[partition[nbor.getId()]] += outlink.getWeight();
    }

    for (int i=1; i<=k; i++) {
      if (i==ni_part) continue;
      // can we add ni to block i?
      if (_blockSizes[i-1]>=_max_block_size) continue;
      if (_gain < weights[i] - weights[ni_part]) {
        _gain = weights[i] - weights[ni_part];
        best = i;
      }
    }
    return best;
  }


  /**
   * returns a random number in the interval [1,...numslots]
   * @param numslots int
   * @return int
   */
  private int flipACoin(int numslots) {
    // double res = Math.random();
    double res = utils.RndUtil.getInstance().getRandom().nextDouble();
    for (int i = 1; i<=numslots; i++) {
      if (res < ((double) i / (double) numslots)) return i;
    }
    return -1;
  }


  private void initializePartition(Graph g, int partition[], int k) throws ParallelException {
    int num_nodes = g.getNumNodes();
    int comps[] = g.getComponents();
    int numcomps = g.getNumComponents();
    int cards[] = null;  // maintain block cardinalities
    if (numcomps==k) {
      cards = new int[k];
      for (int i=0; i<k; i++) cards[i] = 0;
      for (int i=0; i<num_nodes; i++) {
        partition[i] = comps[i]+1;
        cards[comps[i]]++;
      }
      // no need for further partitioning
      return;
    }
    else if (numcomps<k) {
      cards = new int[k];
      for (int i=0; i<k; i++) cards[i] = 0;
      for (int i=0; i<num_nodes; i++) {
        partition[i] = comps[i]+1;
        cards[comps[i]]++;
      }
      // must split a few components
      int remcomps = k-numcomps;
      CC compsizes[] = new CC[numcomps];
      for (int i=0; i<numcomps; i++) {
        compsizes[i] = new CC(i, g.getComponentCard(i));
      }
      Arrays.sort(compsizes);
      // if the remcomps <= numcomps then
      // break in half randomly each of the last remcomps components
      if (remcomps<=numcomps) {
        // revarrind gives the original component number for any index in
        // the sorted compsizes array. In other words, revarrind[i] is the
        // position in asc size order of the i-th component of the graph
        // revarrind[i] is of course in the range [0,...,numcomps-1]
        int revarrind[] = new int[numcomps];
        for (int i=0; i<numcomps; i++) {
          for (int j=0; j<numcomps; j++) {
            if (compsizes[j]._i==i) revarrind[i] = j;
          }
        }
        int sizebreakpoint = compsizes[numcomps-remcomps]._sz;
        for (int i = 0; i < num_nodes; i++) {
          int pi = partition[i]-1;  // translate back to original component index
          int pisize = g.getComponentCard(pi);
          if (pisize >= sizebreakpoint) {
            // must be broken
            if (flipACoin(2)==2 && cards[pi]>1) {
              // move node i to new partition
              cards[pi]--;
              cards[revarrind[pi]+remcomps]++;
              partition[i] = revarrind[pi]+remcomps+1;
            }
          }
        }
      }
      else {
        // remcomps>numcomps, so just break the last (and biggest)
        // component into remcomps+1 components randomly
        int biggestcompind = compsizes[numcomps-1]._i;
        for (int i=0; i<num_nodes; i++) {
          int pi = partition[i]-1;
          if (pi==biggestcompind) {
            int add = flipACoin(remcomps+1)-1;  // add is in [0...remcomps]
            if (add>0 && cards[pi]>1) {
              cards[pi]--;
              cards[numcomps+add-1]++;
              partition[i] = numcomps + add;
            }
          }
        }
      }
    }
    else {  // numcomponents > k
      // unite all extra components with a random one
      cards = new int[numcomps];
      for (int i=0; i<numcomps; i++) cards[i] = 0;
      for (int i=0; i<num_nodes; i++) {
        partition[i] = comps[i]+1;
        cards[comps[i]]++;
      }

      for (int i=0; i<num_nodes; i++) {
        int chosenone = flipACoin(k);
        if (partition[i]>k) {
          cards[partition[i]-1]--;
          partition[i] = chosenone;
          cards[chosenone-1]++;
        }
      }
    }
    // ensure no partition block is empty
    for (int i=0; i<k; i++) {
      if (cards[i]==0) {
        // move a node from another bigger partition here
        for (int j=0; j<num_nodes; j++) {
          if (cards[partition[j]-1]>1) {
            cards[partition[j]-1]--;
            partition[j]=i+1;
            cards[i]++;
            break;
          }
        }
      }
    }
  }


  // inner aux class
  static class CC implements Comparable {
    int _i, _sz;
    public CC(int i, int sz) {
      _i = i; _sz = sz;
    }

    public boolean equals(Object o) {
      if (o==null) return false;
      try {
        CC dd = (CC) o;
        if (_sz==dd._sz) return true;
        else return false;
      }
      catch (ClassCastException e) {
        return false;
      }
    }

    public int hashCode() {
      return (int) Math.floor(_sz);
    }


    public int compareTo(Object o) {
      CC c = (CC) o;
      // this < c => return -1
      // this == c => return 0
      // this > c => return 1
      if (_sz < c._sz) return -1;
      else if (_sz == c._sz) return 0;
      else return 1;

    }
  }
}
