package tests;

import utils.*;
import graph.*;
import java.util.*;

/**
 * a test-driver class to test the functionality of computing maximal weighted
 * cliques in a graph (in the graph package).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class CliqueComputingTestMT {
  /**
   * public no-arg constructor
   */
  public CliqueComputingTestMT() {
  }


  /**
   * the main method: invoke from command line as follows:
   * <CODE>java -cp &lt;classpath&gt; tests.CliqueComputingTestMT &lt;graph_file_name&gt; &lt;thres&gt; &lt;numthreads&gt;</CODE>
   * where the parameter graph_file_name contains the graph whose cliques we
   * seek, and the parameter thres defines the minimum weight that each edge
   * of the graph must have so that it "counts" as a "true edge" in the
   * computation of all maximally weighted cliques. The definition of the format
   * of the graph_file_name can be found in the documentation for class
   * <CODE>utils.DataMgr</CODE> and in particular for the method
   * <CODE>DataMgr.readGraphFromFile[2](String name)</CODE>. The third argument
   * is the number of threads to use.
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      long start = System.currentTimeMillis();
      String filename = args[0];
      double thres = Double.parseDouble(args[1]);
      int numthreads = Integer.parseInt(args[2]);
      Graph g = null;
      try {
        g = DataMgr.readGraphFromFile(filename);
      }
      catch (Exception e) {
        g = DataMgr.readGraphFromFile2(filename);  // try alternative format
      }
      AllMWCFinder finder = null;
      finder = new AllMWCFinderBKMT(g, numthreads);
      Set output = finder.getAllMaximalCliques(thres);
      long dur = System.currentTimeMillis()-start;
      Iterator it = output.iterator();
      int i=0;
      while (it.hasNext()) {
        Set c = (Set) it.next();
        System.out.print(++i +
                         ": [ ");
        Iterator it2 = c.iterator();
        while (it2.hasNext()) {
          Integer id = (Integer) it2.next();
          System.out.print(id.intValue()+" ");
        }
        System.out.println("]");
      }
      // compute sizes:
      HashMap sizes = new HashMap();  // map<Integer size, Integer numcliques>
      it = output.iterator();
      while (it.hasNext()) {
        Set c = (Set) it.next();
        int cs = c.size();
        Integer nums = (Integer) sizes.get(new Integer(cs));
        if (nums==null) {
          sizes.put(new Integer(cs), new Integer(1));
        }
        else {
          Integer nnums = new Integer(nums.intValue()+1);
          sizes.put(new Integer(cs), nnums);
        }
      }
      Iterator it2 = sizes.keySet().iterator();
      System.out.println("Clique Sizes: ");
      while (it2.hasNext()) {
        Integer size = (Integer) it2.next();
        Integer nums = (Integer) sizes.get(size);
        System.out.print("[size="+size+", card="+nums+"] ");
      }
      System.out.println();
      System.out.println("time (msecs)="+dur);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
