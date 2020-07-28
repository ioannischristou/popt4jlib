package tests;

import popt4jlib.neural.*;
import utils.DataMgr;
import java.util.HashMap;

/**
 * test the FFNN4TrainB class functionality (both forward evaluation, as well as
 * gradient (backward) evaluation).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FFNN4TrainBTest {
	
	public static void main(String[] args) {
		try {
			final HashMap params = DataMgr.readPropsFromFile(args[0]);
			final int n = Integer.parseInt(args[1]);
			FFNN4TrainB ffnn = (FFNN4TrainB) params.get("ffnn4trainb");
			final double[][] traindata = (double[][]) params.get("ffnn.traindata");
			final double[] trainlabels = (double[]) params.get("ffnn.trainlabels");
			final int num_input_signals = traindata[0].length;
			ffnn.finalizeInitialization(num_input_signals);
			final double[][] data0 = new double[n][];
			final double[] labels0 = new double[n];
			for (int i=0; i<n; i++) {
				data0[i] = traindata[i];
				labels0[i] = trainlabels[i];
			}
			params.put("ffnn.traindata",data0);
			params.put("ffnn.trainlabels",labels0);
			// initialize wgts to 1 except biases that get 0
			double[] wgts = new double[ffnn.getTotalNumWeights()];
			for (int i=0; i<wgts.length; i++) wgts[i] = 1;
			// set zero biases
			int[] biases = ffnn.getIndices4BiasInWgts(num_input_signals);
			for (int i=0; i<biases.length; i++) {
				wgts[biases[i]] = 0;
			}
			double cost = ffnn.eval(wgts, params);
			System.out.println("cost = "+cost);
		}
		catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
}
