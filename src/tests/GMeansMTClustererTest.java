package tests;

import popt4jlib.MSSC.*;
import popt4jlib.*;
import utils.*;
import java.util.*;
import popt4jlib.GradientDescent.VecUtil;

/**
 * driver class for multi-threaded K-Means clusterer for MSSC (with or without
 * KMeans++ initialization of seed cluster centers).
 * <p>Note:
 * <ul>
 * <li>20181104: added parameters to the command line for selecting the 
 * initialization method (random,KMeans++, or KMeans||) as well as whether the
 * vectors in the data file are sparse or not, and setting the debug-level.
 * <li>20181105: added option to let the main K-Means iterations run until 
 * no center moves to a distance larger than a number in Euclidean norm.
 * <li>20220217: added option to add positive weights for data points, in 
 * weighted MSSC.
 * <li>20220221: added option to run the <CODE>GMeansMTClusterer</CODE> more 
 * than 1 times, if desired, and return the best (weighted) MSSC value.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2022</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.1
 */
public class GMeansMTClustererTest {

  /**
   * invoke as:
   * <CODE>
	 * java -cp &lt;classpath&gt; tests.GMeansMTClustererTest 
	 * &lt;vectorsfilename&gt; &lt;k&gt; [weightsfilename(null)] 
	 * [use_weights?(true)]
	 * [num_threads(1)] [rndseed(-1)]
	 * [init_method(0(default)=random point selection|1=KMeans++|2=KMeans||)] 
	 * [num_tries(1)]
	 * [numitersOrmaxdist(-1)]
	 * [vectors_are_sparse(false)]
	 * [project_on_empty(false)]
	 * [KMeans||_rounds(k/2)]
	 * [dbglvl(Integer.MAX_VALUE, all msgs printed)]
	 * </CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    if (args.length<2) {
      System.err.println("usage: java -cp <classpath> "+
				                 "tests.GMeansMTClustererTest <vectorsfilename> <k> "+
				                 "[weightsfilename(null)] [use_weights?(true)] "+
				                 "[num_threads(1)] [rndseed(-1)] "+
				                 "[init_method"+
				                 "(0(default)=rand pt sel|1=KMeans++|2=KMeans||)] "+
				                 "[num_tries(1)] "+
				                 "[numitersORmaxdist(-1)] "+
				                 "[vectors_are_sparse(false)] "+
				                 "[project_on_empty(false] "+
				                 "[KMEans||_rounds(-1)]"+
				                 "[dbglvl(Integer.MAX_VALUE->all msgs printed)]");
      System.exit(-1);
    }
    long start = System.currentTimeMillis();
    long start_compute = 0;
    final String file = args[0];
    final int k = Integer.parseInt(args[1]);
		final String wfile = args.length>2 && !args[2].equals("null") ? args[2] : 
			                                                              null;
		double[] wgts = null;
		if (wfile!=null) {
			try {
				wgts = DataMgr.readDoubleLabelsFromFile(wfile);
			}
			catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}
		boolean use_wgts = args.length > 3 ? Boolean.parseBoolean(args[3]) : true;
    final int nt = args.length>4 ? Integer.parseInt(args[4]) : 1;
    final long seed = args.length>5 ? Long.parseLong(args[5]) : -1;
    final int init_method = args.length>6 ? Integer.parseInt(args[6]) : 0;
		final int num_tries = args.length>7 ? Integer.parseInt(args[7]) : 1;
		ClustererTerminationIntf ct = 
			new popt4jlib.MSSC.ClustererTerminationNoMove();
		if (args.length>8) {
			try {
				int num_iters = Integer.parseInt(args[8]);
				if (num_iters>0) 
					ct = new popt4jlib.MSSC.ClustererTerminationNumIters(num_iters);
			}
			catch (NumberFormatException e) {
				try {
					double tol = Double.parseDouble(args[8]);
					ct = new popt4jlib.MSSC.ClustererTerminationNoCenterMove(tol);
				}
				catch (NumberFormatException e2) {
					System.err.println("cannot parse number "+args[8]+
						                 ". Termination criteria is full convergence.");
				}
			}
		}
		boolean vectors_sparse = args.length>9 ? args[9].startsWith("t") : false;
		boolean p_o_e = args.length>10 ? args[10].startsWith("t") : false;
		int nr = args.length>11 ? Integer.parseInt(args[11]) : -1;
		if (args.length>12) {
			int dbglvl = Integer.parseInt(args[12]);
			utils.Messenger.getInstance().setDebugLevel(dbglvl);
		}
    if (seed >= 0) RndUtil.getInstance().setSeed(seed);  // deterministic 
		                                                     // pseudo-random 
		                                                     // number generation
    try {
      System.err.print("Loading data");
			TmThread tt = new TmThread();
			tt.start();
			Vector docs = vectors_sparse ? DataMgr.readSparseVectorsFromFile(file) :
				                             DataMgr.readVectorsFromFile(file);
			tt.quit();
			System.err.println("Done.");
      start_compute = System.currentTimeMillis();
      final int n = docs.size();
      if (k>n) {
        System.err.println("<k> cannot be larger or equal to the <n>, "+
					                 "the number of vectors to cluster");
        System.exit(-1);
      }
      HashMap p = new HashMap();
      p.put("gmeansmt.numthreads", new Integer(nt));
      p.put("gmeansmt.TerminationCriteria", ct);
      p.put("gmeansmt.evaluator", new popt4jlib.MSSC.KMeansSqrEvaluator());
			if (wgts!=null) {
				if (use_wgts) p.put("gmeansmt.weights", wgts);
				else p.put("weights", wgts);
			}
			if (p_o_e) p.put("gmeansmt.projectonempty", new Boolean(true));
			double overall_best_mssc = Double.MAX_VALUE;
			for (int iter=0; iter<num_tries; iter++) {
				List init_centers = new ArrayList();
				if (init_method==0) {
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
						VectorIntf cj = ( (VectorIntf) docs.get(rj)).newInstance();  
						// used to be newCopy();
						// System.err.println("adding "+cj+" as initial center");
						init_centers.add(cj);
					}
				}
				else if (init_method==1) {
					System.err.println("Running KMeans++ for seed initialization");
					KMeansPP kmpp = new KMeansPP(docs);
					init_centers = kmpp.getInitialCenters(k);
					long dur = System.currentTimeMillis()-start_compute;
					System.err.println("KMeans++: took "+dur+" msecs to run");
				}
				else if (init_method==2) {
					final int lambda = k/2 > 0 ? k/2 : 1;
					System.err.println("Running KMeans|| for seed initialization w/ "+
														 "lambda="+lambda);
					final int numrounds = nr;
					// KMeansCC needs an unsynchronized list of data to work well in
					// parallel, and to avoid rewriting DataMgr.readXXX() methods, we 
					// resort to this "trick"
					List docslist = new ArrayList();
					docslist.addAll(docs);
					KMeansCC kmcc = new KMeansCC(docslist,lambda,nt,numrounds);
					init_centers = kmcc.getInitialCenters(k);				
					long dur = System.currentTimeMillis()-start_compute;
					System.err.println("KMeans||: took "+dur+" msecs to run");
					// compute pair-wise distances between centers and report any pairs
					// at distance shorter than 10^-8
					for (int i=0; i<k; i++) {
						VectorIntf ci = (VectorIntf) init_centers.get(i);
						for (int j=i+1; j<k; j++) {
							VectorIntf cj = (VectorIntf) init_centers.get(j);
							double dij = VecUtil.getEuclideanDistance(ci, cj);
							if (dij<1.e-8) 
								System.err.println("KMeans|| for centers "+i+","+j+
																	 " d(ci="+ci+",cj="+cj+" = "+dij);
						}
					}
				}
				GMeansMTClusterer gmclusterer = new GMeansMTClusterer();
				System.err.println("Starting Clustering with k="+k);
				gmclusterer.addAllVectors(docs);
				gmclusterer.setParams(p);
				gmclusterer.setInitialClustering(init_centers);
				List centers = gmclusterer.clusterVectors();
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
				if (vectors_sparse) {  // report sparse centers: those having 90% zeros
					for (int i=0; i<centers.size(); i++) {
						SparseVectorIntf sci = (SparseVectorIntf) centers.get(i);
						int nzi = sci.getNumNonZeros();
						if (10*nzi < sci.getNumCoords())
							System.out.println("center-"+i+" is sparse: #nonzeros="+nzi);
					}
				}
				double mssc = gmclusterer.eval(new popt4jlib.MSSC.KMeansSqrEvaluator());
				if (overall_best_mssc>mssc) overall_best_mssc = mssc;
				System.out.println("iter#"+iter+" MSSC="+mssc+
					                 " duration so far (msecs): "+dur+
					                 " compute duration so far (msecs)="+dur_compute);
				System.out.println("iter#"+iter+" Sum-Of-Variances="+
				  gmclusterer.eval(new popt4jlib.MSSC.SumOfVarianceEvaluator()));
				System.out.println("iter#"+iter+" Normalized Sum-Of-Square-Errors="+
					gmclusterer.eval(new popt4jlib.MSSC.KMeansNormSqrEvaluator(docs)));
			}  // for-each (outer) iter
			
			System.out.println("MSSC* OVERALL ("+num_tries+") iterations="+
				                 overall_best_mssc);
			final long tot_dur = System.currentTimeMillis()-start;
			System.out.println("OVERALL time="+tot_dur+" msecs.");
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

