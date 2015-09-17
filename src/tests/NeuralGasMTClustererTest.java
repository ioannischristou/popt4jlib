package tests;

import popt4jlib.MSSC.*;
import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * driver class for multi-threaded NeuralGas clusterer for MSSC (with random
 *  initialization of seed cluster centers).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class NeuralGasMTClustererTest {

  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; tests.NeuralGasMTClustererTest &lt;vectorsfilename&gt; &lt;k&gt; [num_threads(1)] [rndseed(-1)] [numiters(1000)] [useoldnslowmethod(false)]</CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length<2) {
      System.err.println("usage: java -cp <classpath> tests.GMeansMTClustererTest <vectorsfilename> <k> [num_threads(1)] [rndseed(-1)] [numiters(1000)] [useoldnslowmethod(false)]");
      System.exit(-1);
    }
    long start = System.currentTimeMillis();
    long start_compute = 0;
    final String file = args[0];
    final int k = Integer.parseInt(args[1]);
    final int nt = args.length>2 ? Integer.parseInt(args[2]) : 1;
    final long seed = args.length>3 ? Long.parseLong(args[3]) : -1;
    final int num_iters = args.length>4 ? Integer.parseInt(args[4]) : 1000;
    final boolean useoldnslowmethod = args.length>5 ? Boolean.valueOf(args[5]).booleanValue() : false;
    if (seed >= 0) RndUtil.getInstance().setSeed(seed);  // deterministic pseudo-random number generation
    try {
      Vector docs = DataMgr.readVectorsFromFile(file);
      start_compute = System.currentTimeMillis();
      final int n = docs.size();
      if (k>n) {
        System.err.println("<k> cannot be larger or equal to the <n>, the number of vectors to cluster");
        System.exit(-1);
      }
      HashMap p = new HashMap();
      p.put("neuralgasmt.numthreads", new Integer(nt));
      if (num_iters<=0) {
        System.err.println("<numiters> cannot be less than zero");
        System.exit(-1);
      }
      else p.put("neuralgasmt.TerminationCriteria", new popt4jlib.MSSC.ClustererTerminationNumIters(num_iters));
      //p.put("gmeansmt.evaluator", new popt4jlib.MSSC.KMeansSqrEvaluator());
      Vector init_centers = new Vector();
      // initialize centers
      Set ics = new TreeSet();
      for (int i = 0; i < k; i++) {
        int rj = -1;
        while (true) {
          rj = RndUtil.getInstance().getRandom().nextInt(n);
          Integer rjI = new Integer(rj);
          if (ics.contains(rjI) == false) {
            ics.add(rjI);
            break;
          }
        }
        VectorIntf cj = ( (VectorIntf) docs.elementAt(rj)).newInstance();  // used to be newCopy();
        // System.err.println("adding "+cj+" as initial center");
        init_centers.addElement(cj);
      }
      // done initializing clustering
      NeuralGasMTClusterer ngclusterer = new NeuralGasMTClusterer();
      ngclusterer.addAllVectors(docs);
      ngclusterer.setParams(p);
      ngclusterer.setInitialClustering(init_centers);
      Vector centers = useoldnslowmethod==false ? ngclusterer.clusterVectors() :
          ngclusterer.clusterVectorsOldnSlow();
      long dur = System.currentTimeMillis()-start;
      long dur_compute = System.currentTimeMillis()-start_compute;
      System.out.println("MSSC="+ngclusterer.eval(new popt4jlib.MSSC.KMeansSqrEvaluator())+" duration (msecs): "+dur+
                         " compute duration (msecs)="+dur_compute);
      // System.out.println("Total RegisteredParcel objs created="+parallel.RegisteredParcel.getTotalNumObjs());
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}

