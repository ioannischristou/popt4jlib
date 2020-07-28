package tests;

import analysis.*;
import popt4jlib.DblArray1Vector;
import popt4jlib.VectorIntf;
import popt4jlib.neural.*;
import popt4jlib.neural.costfunction.*;
import utils.Messenger;
import java.util.HashMap;
import popt4jlib.GradientDescent.VecUtil;


/**
 * tests the "auto" gradient computation (per training instance) for FeedForward
 * Neural Networks with bias accepting.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FFNN4TrainBGradTest {
	
	/** builds a very simple FFNN and tests its gradient on a simplistic training
	 * dataset.
	 * @param args 
	 */
	public static void main(String[] args) {
		Messenger.getInstance().setDebugLevel(1);
		final int num_layers = 2;
		final int num_nodes_per_layer = 2;
		NNNodeIntf[][] hidden_nodes = new NNNodeIntf[num_layers][];
		for (int i=0; i<num_layers; i++) {
			hidden_nodes[i] = new NNNodeIntf[num_nodes_per_layer];
			for (int j=0; j<num_nodes_per_layer; j++) {
				hidden_nodes[i][j] = new Sigmoid(1.0);
			}
		}
		OutputNNNodeIntf outnode = new MultiClassSSE();  // Linear();
		FFNN4TrainB ffnnb = new FFNN4TrainB(hidden_nodes, outnode, new MAE());
		ffnnb.finalizeInitialization(3);  // # of columns in dataset that follows
		
		GradApproximator g_approx = new GradApproximator(ffnnb);
		
		final int num_weights = (3+1)*2+(2+1)*2+2+1;
		// create dataset: 8 rows, 3 cols each 
		// x1 x2 x3     LBL
		// 1  0  0  --> 0.0
		// 0  1  0  --> 1.0
		// 0  0  1  --> 1.0
		// 1  1  0  --> 1.0
		// 0  1  1  --> 0.0
		// 1  0  1  --> 1.0
		// 0  0  0  --> 0.0
		// 1  1  1  --> 0.0
		double[][] traindata = {{1,0,0},{0,1,0},{0,0,1},{1,1,0},{0,1,1},
			                      {1,0,1},{0,0,0},{1,1,1}};
		double[] trainlabels = {0, 1, 1, 1, 0, 1, 0, 0};
		/*
		double[][] traindata = {{1,0,0}};
		double[] trainlabels = {0};
		*/
		HashMap p = new HashMap();
		p.put("ffnn.traindata", traindata);
		p.put("ffnn.trainlabels", trainlabels);
		
		double[] weights = {1,1,0,1, 1,2,1,0,  
			                  1,1,0, 0,1,0,
												1,1,0};
		VectorIntf x = new DblArray1Vector(weights);
		/*
		double norm_diff_1 = 0.0;
		for (int i=0; i<num_weights; i++) {
			double g_approxi_val = g_approx.evalCoord(x, p, i);
			double g_test_val = ffnnb.evalPartialDerivativeB(weights, i, p);
			double diff = Math.abs(g_test_val-g_approxi_val);
			norm_diff_1 += diff;
			System.err.println("MAIN--- g_approx["+i+"]="+g_approxi_val+
				                 " g_auto["+i+"]="+g_test_val+" |diffi|="+diff);
		}
		*/
		long sapprox = System.currentTimeMillis();
		VectorIntf ga = g_approx.eval(x, p);
		long dapprox = System.currentTimeMillis()-sapprox;
		VectorIntf gauto = new DblArray1Vector(weights);
		long sauto = System.currentTimeMillis();
		for (int i=0; i<num_weights; i++) {
			double g_auto_i = ffnnb.evalPartialDerivativeB(weights, i, p, false);
			try {
				gauto.setCoord(i, g_auto_i);
			}
			catch (Exception e) {  // cannot get here
				e.printStackTrace();
			}
		}
		long dauto = System.currentTimeMillis()-sauto;
		VectorIntf diff = VecUtil.subtract(ga, gauto);
		double norm_diff_1 = VecUtil.norm(diff, 1);
		System.out.println("gauto="+gauto);
		System.out.println("gapprox="+ga);
		System.err.println("MAIN--- ||g_approx-g_auto||_1="+norm_diff_1);
		System.err.println("gauto time="+dauto+" msecs,"+
			                 " gapprox time="+dapprox+" msecs");
	}
}
