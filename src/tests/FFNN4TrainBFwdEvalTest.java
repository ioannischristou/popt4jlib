package tests;

import popt4jlib.neural.*;
import utils.DataMgr;
import java.util.HashMap;
import popt4jlib.neural.costfunction.CategoricalAccuracy;

/**
 * test the FFNN4TrainB class forward evaluation functionality, and measures
 * time it takes for this evaluation.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FFNN4TrainBFwdEvalTest {
	
	/**
	 * invoke as:
	 * <CODE>
	 * java &lt;args&gt; -cp &lt;classpath&gt; tests.FFNN4TrainBFwdEvalTest
	 * &lt;params_file&gt; [num_val_insts_to_try(all)]
	 * </CODE>.
	 * The params file must contain entries that specify the neural network and 
	 * the network object itself must exist in the parameters with key 
	 * "ffnn4trainb". Also, the following data must be available:
	 * <ul>
	 * <li>&lt;"weights", double[]&gt; mandatory the weights of the network, 
	 * including of course bias terms.
	 * <li>&lt;"ffnn.valdata", double[][]&gt; mandatory the validation data to try
	 * <li>&lt;"ffnn.vallabels", double[]&gt; optional, the validation labels if
	 * they exist will be used to determine validation error on the validation set
	 * </ul>
	 * The method reports as output the average time it took to evaluate a 
	 * particular validation data pattern, and if validation labels are also 
	 * provided, it reports the total validation error on the validation set.
	 * @param args 
	 */
	public static void main(String[] args) {
		try {
			final HashMap params = DataMgr.readPropsFromFile(args[0]);
			FFNN4TrainB ffnn = (FFNN4TrainB) params.get("ffnn4trainb");
			final double[][] valdata = (double[][]) params.get("ffnn.valdata");
			final double[] vallabels = (double[]) params.get("ffnn.vallabels");
			final int num_input_signals = valdata[0].length;
			final int n = args.length>1 ? Integer.parseInt(args[1]) : valdata.length;
			ffnn.finalizeInitialization(num_input_signals);
			double[] wgts = (double[])params.get("weights");
			CategoricalAccuracy ca = new CategoricalAccuracy();
			double[] errs = new double[n];
			long avg_res_time = 0;
			// add the hiddenws$i$ and outputws in params
			final int nhlayers = ffnn.getNumHiddenLayers();
			for (int j=0; j<nhlayers; j++) {
				double[][] hws_j = ffnn.getLayerWeightsWithBias(j, wgts, 
					                                              num_input_signals);
				params.put("hiddenws"+j, hws_j);
			}
			double[] outws = ffnn.getOutputWeightsWithBias(wgts);
			params.put("outputws", outws);
			double cost=0;
			for (int i=0; i<n; i++) {
				long st = System.nanoTime();
				double outi = ffnn.evalNetworkOnInputData(valdata[i], params);
				long duri = System.nanoTime()-st;
				avg_res_time += duri;
				if (vallabels!=null) {
					errs[i] = outi - vallabels[i];
				}
			}
			avg_res_time /= n;
			cost = ca.eval(errs, params);
			System.out.println("average output (forward eval) response time="+
				                 avg_res_time+" NanoSecs");
			System.out.println("Categorical Error (%) = "+cost);
		}
		catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
}
