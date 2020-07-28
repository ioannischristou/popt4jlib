package tests;

import analysis.GradApproximator;
import popt4jlib.DblArray1Vector;
import popt4jlib.neural.*;
import utils.DataMgr;
import java.util.HashMap;
import popt4jlib.GradientDescent.VecUtil;
import popt4jlib.VectorIntf;


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
public class FFNN4TrainBTest2 {
	
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
			
			/*
			// also, compute the [f(w+he_0)-f(w)]/h for h=1.e-7 and e_0=[1 0 ...0]
			// ie the partial derivative wrt the first weight (the weight connecting
			// the 1st input to the 1st hidden sigmoid node)
			wgts[0] += 1.e-7;
			double cost2 = ffnn.eval(wgts, params);
			System.out.println("diff quotient for w0 = "+(cost2-cost)/1.e-7);
			*/
			
			// now test the derivative
			System.err.println("computing the gradient (auto-diff)");
			FFNN4TrainBGrad ffnn_grad = new FFNN4TrainBGrad(ffnn);
			DblArray1Vector xw = new DblArray1Vector(wgts);
			DblArray1Vector g = 
				(DblArray1Vector) ffnn_grad.eval(xw, params);
			String str = ffnn_grad.toString(g, params);
			System.err.println("g=\n"+str);
			
			// finally compute the Richardson extrapolation of the gradient
			System.err.println("computing the gradient (Richardson extrapolation)");
			GradApproximator gapprox = new GradApproximator(ffnn);
			VectorIntf ga = gapprox.eval(xw, params);
			String str_approx = ffnn_grad.toString(ga, params);
			System.err.println("g_approx=\n"+str_approx);
			double norm_diff = VecUtil.getEuclideanDistance(g, ga);
			System.err.println("||g-ga||="+norm_diff);
		}
		catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
}
