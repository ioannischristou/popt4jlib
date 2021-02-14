package tests;

import popt4jlib.neural.*;
import utils.DataMgr;
import java.util.HashMap;

/**
 * test the FFNN4TrainB class and the approximation of the partial derivatives
 * with respect to any weight variable by the difference quotient computed for
 * any specified step-size.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FFNN4TrainBTest3 {
	
	/**
	 * invoke as 
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.FFNN4TrainBTest3 
	 * &lt;paramsfile&gt; &lt;weight_index&gt; &lt;step_size&gt;
	 * </CODE>.
	 * @param args 
	 */
	public static void main(String[] args) {
		try {
			final HashMap params = DataMgr.readPropsFromFile(args[0]);
			final int n = Integer.parseInt(args[1]);
			final double ss = Double.parseDouble(args[2]);
			FFNN4TrainB ffnn = (FFNN4TrainB) params.get("ffnn4trainb");
			final double[][] traindata = (double[][]) params.get("ffnn.traindata");
			final double[] trainlabels = (double[]) params.get("ffnn.trainlabels");
			final double[] weights = (double[]) params.get("weights");
			final int num_input_signals = traindata[0].length;
			ffnn.finalizeInitialization(num_input_signals);
			double cost = ffnn.eval(weights, params);
			System.out.println("cost = "+cost);
			// now compute new cost when weight variable is changed
			weights[n] += ss;
			double cost_hs = ffnn.eval(weights, params);
			System.out.println("cost_hs="+cost_hs);
			double dq = (cost_hs-cost)/ss;
			System.out.println("diff_quot="+dq);
		}
		catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
}
