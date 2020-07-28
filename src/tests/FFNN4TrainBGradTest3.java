package tests;

import analysis.*;
import popt4jlib.DblArray1Vector;
import popt4jlib.VectorIntf;
import popt4jlib.neural.*;
import popt4jlib.GradientDescent.VecUtil;
import utils.*;
import java.util.HashMap;
import java.util.Random;


/**
 * tests the "auto" gradient computation (per training instance) for FeedForward
 * Neural Networks with bias accepting. Differs from the initial 
 * <CODE>FFNN4TrainBGradTest2</CODE> class by the fact that weights are created
 * randomly.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FFNN4TrainBGradTest3 {
	
	/** builds a very simple FFNN and tests its gradient. Invoke as:
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.FFNN4TrainBGradTest2 &lt;params_file&gt;
	 * [num_random_tries(1)]
	 * </CODE>.
	 * @param args 
	 */
	public static void main(String[] args) {

		String params_file = args[0];
		int num_tries = 1;
		if (args.length>1) {
			num_tries = Integer.parseInt(args[1]);
		}
		try {
			HashMap params = DataMgr.readPropsFromFile(params_file);

			FFNN4TrainB ffnnb = (FFNN4TrainB) params.get("ffnnb");		
			double[][] traindata = (double[][]) params.get("ffnn.traindata");
			double[] trainlabels = (double[]) params.get("ffnn.trainlabels");
			ffnnb.finalizeInitialization(traindata[0].length);  // #columns in dataset

			final int num_weights = ffnnb.getTotalNumWeights();
			double[] weights = new double[num_weights];
			Random r = RndUtil.getInstance().getRandom();
			
			for (int k=0; k<num_tries; k++) {
				// create random weights values
				for (int j=0; j<num_weights; j++) weights[j] = 10*r.nextGaussian();
				
				GradApproximator g_approx = new GradApproximator(ffnnb);
				VectorIntf x = new DblArray1Vector(weights);
				long sapprox = System.currentTimeMillis();
				VectorIntf ga = g_approx.eval(x, params);
				long dapprox = System.currentTimeMillis()-sapprox;
				System.err.println("gapprox computed in "+dapprox+" msecs");

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

				VectorIntf diff = VecUtil.subtract(ga, gauto);
				double norm_diff_1 = VecUtil.norm(diff, 1);
				//System.out.println("gapprox="+ga);
				System.err.println("MAIN--- ||g_approx-g_auto||_1="+norm_diff_1);
				System.err.println("gauto time="+dauto+" msecs,"+
													 " gapprox time="+dapprox+" msecs");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
