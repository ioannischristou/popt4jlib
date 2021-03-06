package tests;

import analysis.GradApproximator;
import popt4jlib.DblArray1Vector;
import popt4jlib.VectorIntf;
import popt4jlib.neural.*;
import popt4jlib.GradientDescent.VecUtil;
import utils.*;
import java.util.HashMap;
import java.util.Random;


/**
 * tests the "auto" gradient computation (per training instance) for FeedForward
 * Neural Networks with bias accepting. Differs from FFNN4TrainBGradTest3 in 
 * that the tests are made between the FFNN4TrainBGrad, FastFFNN4TrainBGrad and
 * FasterFFNN4TrainBGrad classes. It also computes gradient based on Richardson
 * extrapolation if appropriate flag is set.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FFNN4TrainBGradTest4 {
	
	/** builds a very simple FFNN and tests its gradient. Invoke as:
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.FFNN4TrainBGradTest4 &lt;params_file&gt;
	 * [num_random_tries(1)] [num_threads(1)] [do_wgts_asc(true)] 
	 * [do_richardson(false)].
	 * </CODE>.
	 * @param args 
	 */
	public static void main(String[] args) {

		String params_file = args[0];
		int num_tries = 1;
		if (args.length>1) {
			num_tries = Integer.parseInt(args[1]);
		}
		int num_threads = 1;
		if (args.length>2) {
			num_threads = Integer.parseInt(args[2]);
		}
		boolean do_wgts_asc = true;
		if (args.length>3) {
			do_wgts_asc = Boolean.parseBoolean(args[3]);
		}
		boolean do_richardson = false;
		if (args.length>4) {
			do_richardson = Boolean.parseBoolean(args[4]);
		}
		
		try {
			HashMap params = DataMgr.readPropsFromFile(params_file);

			FFNN4TrainB ffnnb = (FFNN4TrainB) params.get("ffnn4trainb");		
			double[][] traindata = (double[][]) params.get("ffnn.traindata");
			double[] trainlabels = (double[]) params.get("ffnn.trainlabels");
			ffnnb.finalizeInitialization(traindata[0].length);  // #columns in dataset
			ffnnb.setComputingOrderAsc(do_wgts_asc);
			
			final int num_features = traindata[0].length;
			final int num_weights = ffnnb.getTotalNumWeights();
			double[] weights = new double[num_weights];
			Random r = RndUtil.getInstance().getRandom();
			GradApproximator gapprox = do_richardson ? new GradApproximator(ffnnb) :
				                                         null;
			
			for (int k=0; k<num_tries; k++) {
				// create random weights values
				for (int j=0; j<num_weights; j++) weights[j] = 10*r.nextGaussian();
				// don't use them if there already exists the weights array in params
				if (params.containsKey("weights") && num_tries==1) {
					weights = (double[]) params.get("weights");
				}
				FastFFNN4TrainBGrad gf_auto = 
					new FastFFNN4TrainBGrad(ffnnb, num_features, num_threads);
				System.err.println("done initializing FastFFNN4TrainBGrad");
				FasterFFNN4TrainBGrad gf_auto2 = 
					new FasterFFNN4TrainBGrad(ffnnb, num_features, num_threads);
				System.err.println("done initializing FasterFFNN4TrainBGrad");
				VectorIntf x = new DblArray1Vector(weights);
				System.err.println("weights="+x);
				long sfauto = System.currentTimeMillis();
				VectorIntf ga2 = gf_auto.eval(x, params);
				long dfauto = System.currentTimeMillis()-sfauto;
				System.err.println("gfast_auto computed in "+dfauto+" msecs");
				System.err.println("gfast_auto="+ga2);
				VectorIntf ga3=null;
				try {
					long sfauto2 = System.currentTimeMillis();
					ga3 = gf_auto2.eval(x, params);
					long dfauto2 = System.currentTimeMillis()-sfauto2;
					System.err.println("gfast_auto2 computed in "+dfauto2+" msecs");
					System.err.println("gfast_auto2="+ga3);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				
				VectorIntf gauto = new DblArray1Vector(weights);
				long sauto = System.currentTimeMillis();
				// clear "hiddenws" and "outputws" from params
				int num_hidden_layers = ffnnb.getNumHiddenLayers();
				for (int i=0; i<num_hidden_layers; i++) params.remove("hiddenws"+i);
				params.remove("outputws");
				for (int i=0; i<weights.length; i++) {
					double g_auto_i = ffnnb.evalPartialDerivativeB(weights, i, params,
						                                             false);
					gauto.setCoord(i, g_auto_i);
				}
				long dauto = System.currentTimeMillis()-sauto;
				//System.out.println("gauto="+gauto+" in "+dauto+" msecs");
				System.err.println("gauto="+gauto);
				VectorIntf diff = VecUtil.subtract(ga2, gauto);
				double norm_diff_1 = VecUtil.norm(diff, 1);
				//System.out.println("gapprox="+ga);
				System.err.println("MAIN--- ||gfast_auto - g_auto||_1="+norm_diff_1);

				if (ga3!=null) {
					VectorIntf diff2 = VecUtil.subtract(ga3, gauto);
					double norm_diff_2 = VecUtil.norm(diff2, 1);
					System.err.println("MAIN--- ||gfast_auto2 - g_auto||_1="+norm_diff_2);
				}
				
				System.err.println("gauto time="+dauto+" msecs,"+
													 " gfast_auto time="+dfauto+" msecs");
				
				if (gapprox!=null) {
					System.err.println("computing gradient via Richardson extrapolation");
					VectorIntf gapp = gapprox.eval(x, params);
					System.err.println("done");					
					System.err.println("gapprox="+gapp);
					VectorIntf diff_bp = VecUtil.subtract(ga2, ga3);
					double nda = VecUtil.norm(diff_bp, 1);
					System.err.println("MAIN--- ||gfast_auto2 - gfast_auto||_1="+nda);
					VectorIntf diff_bp2 = VecUtil.subtract(gapp, gauto);
					double nda2 = VecUtil.norm(diff_bp2, 1);
					System.err.println("MAIN--- ||gauto - g_approx||_1="+nda2);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
