package graph.partitioning;

import graph.*;
import java.util.*;
import utils.*;

public class HPartRunner {
  public HPartRunner() {
  }

  public static void main(String[] args) {
    if (args.length!=3) {
      System.err.println("usage: java -cp <classpath> graph.partitioning.HPartRunner <hgraphfile> <numparts> <propsfile>");
      return;
    }
    long start_time = System.currentTimeMillis();
    final String gfile = args[0];
    final int k = Integer.parseInt(args[1]);
    final String pfile = args[2];
    try {
      HGraph g = DataMgr.readHGraphFromhMeTiSFile(gfile);
      HashMap props = DataMgr.readPropsFromFile(pfile);
      props.put("k", new Integer(k));  // add k = num_parts required into props
      HPartitionerIntf hp = (HPartitionerIntf) props.get("partitioning.hpartrunner.partitioner");
      HObjFncIntf objfnc = (HObjFncIntf) props.get("partitioning.hpartrunner.objfnc");
      if (objfnc==null)
        objfnc = new HObjFncSOED();
      hp.setObjectiveFunction(objfnc);
      int[] partition = hp.partition(g, k, props);
      double val = objfnc.value(g, partition);
      System.out.println("partition value="+val);
      String ofile = gfile+"."+k+".part";
      DataMgr.writeIntArrayToFile(partition, ofile);
      // print out the imbalances
      int[] pszes = new int[k];
      for (int i=0; i<k; i++) pszes[i]=0;
      for (int i=0; i<g.getNumNodes(); i++) {
        pszes[partition[i]-1]++;
      }
      System.err.println("Partition Sizes:");
      int max_sz = 0;
      int min_sz = Integer.MAX_VALUE;
      for (int i=0; i<k; i++) {
        if (i % 7 == 0) System.err.println();
        System.err.print(pszes[i]+" ");
        if (pszes[i]==0) {
          System.err.println("HGraph g="+g);
          throw new PartitioningException("Partitioner failed...");
        }
        if (max_sz<pszes[i]) max_sz=pszes[i];
        if (min_sz>pszes[i]) min_sz = pszes[i];
      }
      System.err.println("\n Max Sz="+max_sz+" Min Sz="+min_sz);
      long end_time = System.currentTimeMillis();
      double tot_time = (end_time - start_time)/(double) 1000.0;
      System.out.println("Total Wall-clock Time (secs)="+tot_time);
      return;
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }

  }
}
