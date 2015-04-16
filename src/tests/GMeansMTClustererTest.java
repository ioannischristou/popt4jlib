package tests;

import popt4jlib.MSSC.*;
import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * driver class for multi-threaded K-Means clusterer for MSSC (with or without
 * KMeans++ initialization of seed cluster centers).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class GMeansMTClustererTest {

  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; tests.GMeansMTClustererTest &lt;vectorsfilename&gt; &lt;k&gt; [num_threads(1)] [rndseed(-1)] [doKMeans++(false)] [numiters(-1)]</CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length<2) {
      System.err.println("usage: java -cp <classpath> tests.GMeansMTClustererTest <vectorsfilename> <k> [num_threads(1)] [rndseed(-1)] [doKMeans++(false)] [numiters(-1)]");
      System.exit(-1);
    }
    long start = System.currentTimeMillis();
    long start_compute = 0;
    final String file = args[0];
    final int k = Integer.parseInt(args[1]);
    final int nt = args.length>2 ? Integer.parseInt(args[2]) : 1;
    final long seed = args.length>3 ? Long.parseLong(args[3]) : -1;
    final boolean do_kmeanspp = args.length>4 ? Boolean.valueOf(args[4]).booleanValue() : false;
    final int num_iters = args.length>5 ? Integer.parseInt(args[5]) : -1;
    if (seed >= 0) RndUtil.getInstance().setSeed(seed);  // deterministic pseudo-random number generation
    try {
      System.err.print("Loading data");
			TmThread tt = new TmThread();
			tt.start();
			Vector docs = DataMgr.readVectorsFromFile(file);
			tt.quit();
			System.err.println("Done.");
      start_compute = System.currentTimeMillis();
      final int n = docs.size();
      if (k>n) {
        System.err.println("<k> cannot be larger or equal to the <n>, the number of vectors to cluster");
        System.exit(-1);
      }
      Hashtable p = new Hashtable();
      p.put("gmeansmt.numthreads", new Integer(nt));
      if (num_iters<=0) p.put("gmeansmt.TerminationCriteria", new popt4jlib.MSSC.ClustererTerminationNoMove());
      else p.put("gmeansmt.TerminationCriteria", new popt4jlib.MSSC.ClustererTerminationNumIters(num_iters));
      p.put("gmeansmt.evaluator", new popt4jlib.MSSC.KMeansSqrEvaluator());
      List init_centers = new Vector();
      if (do_kmeanspp==false) {
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
          init_centers.add(cj);
        }
      }
      else {
        System.err.println("Running KMeans++ for seed initialization");
        KMeansPP kmpp = new KMeansPP(docs);
        init_centers = kmpp.getInitialCenters(k);
      }
      GMeansMTClusterer gmclusterer = new GMeansMTClusterer();
			System.err.println("Starting Clustering.");
      gmclusterer.addAllVectors(docs);
      gmclusterer.setParams(p);
      gmclusterer.setInitialClustering((Vector) init_centers);
      Vector centers = gmclusterer.clusterVectors();
      long dur = System.currentTimeMillis()-start;
      long dur_compute = System.currentTimeMillis()-start_compute;
			System.err.println("Done Clustering.");
      /*
      int[] inds = gmclusterer.getClusteringIndices();
      System.err.print("final inds:[ ");
      for (int i=0; i<n; i++) {
        System.err.print(inds[i]+" ");
      }
      System.err.println("]");
      */
      System.out.println("MSSC="+gmclusterer.eval(new popt4jlib.MSSC.KMeansSqrEvaluator())+" duration (msecs): "+dur+
                         " compute duration (msecs)="+dur_compute);
			System.out.println("Sum-Of-Variances="+gmclusterer.eval(new popt4jlib.MSSC.SumOfVarianceEvaluator()));
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
	
	private static class TmThread extends Thread {
		private boolean _isDone=false;
		
		synchronized void quit() {
			_isDone = true;
			this.interrupt();
		}
		
		synchronized boolean isDone() { return _isDone; }
		
		public void run() {
			while (!isDone()) {
				try {
					Thread.sleep(1000);  // sleep 1 second
					System.err.print(".");
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();  // recommended
				}
			}
		}
	}
}

