package tests;

import java.io.FileWriter;
import java.io.PrintWriter;
import popt4jlib.*;
import popt4jlib.GradientDescent.*;
import java.util.*;
import popt4jlib.neural.FFNN4Train;
import utils.RndUtil;

/**
 * Test-driver class for optimizing a Feed-Forward Neural Network using the AVD 
 * process, and writes the classification results on an output file, whose name
 * must be present in the parameters read by the program for key 
 * "ffnn.outputlabelsfile".
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class AVDFFNNTest {

  /**
   * public no-arg constructor
   */
  public AVDFFNNTest() {
  }


  /**
   * invoke as 
	 * <CODE>
	 * java -&lt;classpath&gt; tests.AVDFFNNTest &lt;params_file&gt; [rndseed]
	 * </CODE>.
   * The params_file must have the lines described in the documentation of
   * the class 
	 * <CODE>popt4jlib.GradientDescent.AlternatingVariablesDescent</CODE> and even
	 * more in the class <CODE>popt4jlib.neural.FFNN4Train</CODE>.
	 * Further, if a key-value pair of the form:
	 * &lt;"ffnn.outputlabelsfile",&lt;filename&gt;&gt; is in the parameters, that
	 * file will be used to write the outputs of the neural network for each 
	 * training instance passed in the params for key "ffnn.traindata".
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      long start_time = System.currentTimeMillis();
      HashMap params = utils.DataMgr.readPropsFromFile(args[0]);
			if (args.length>1) {
				RndUtil.getInstance().setSeed(Long.parseLong(args[1]));
			}
      int n = ((Integer) params.get("avd.numdimensions")).intValue();
      double maxargval = ((Double) params.get("avd.maxargval")).doubleValue();
      double minargval = ((Double) params.get("avd.minargval")).doubleValue();
      // add the initial point
      VectorIntf x0 = new DblArray1Vector(new double[n]);
      for (int j=0; j<n; j++) {
        double val = minargval+RndUtil.getInstance().getRandom().nextDouble()*
					           (maxargval-minargval);
        x0.setCoord(j, val);
      }
      // check out any tryorder points
      int[] tryorder = new int[n];
      boolean toexists = false;
      for (int i=0; i<n; i++) {
        Integer toi = (Integer) params.get("avd.tryorder"+i);
        if (toi!=null) {
          toexists = true;
          tryorder[i] = toi.intValue();
        }
        else tryorder[i] = -1;
      }
      if (toexists) params.put("avd.tryorder",tryorder);
			if (!toexists) {  // check if "tryallparallel" keyword is there
				Boolean tapB = (Boolean) params.get("avd.tryallparallel");
				if (tapB==null || tapB.booleanValue()==false) {
					for (int i=0; i<n; i++) {
						tryorder[i]=i;
					}
					params.put("avd.tryorder",tryorder);
				}
			}
      params.put("avd.x0", x0);
      FunctionIntf func = (FunctionIntf) params.get("avd.function");
      FunctionBase wrapper_func = new FunctionBase(func);
      AlternatingVariablesDescent opter = 
				new AlternatingVariablesDescent(params);
      utils.PairObjDouble p = opter.minimize(wrapper_func);
      VectorIntf arg = (VectorIntf) p.getArg();
      System.out.print("best soln found: ");
      System.out.print(arg);
      System.out.println(" VAL="+p.getDouble()+" #function calls="+
				                 wrapper_func.getEvalCount());
			// finally, if there exists a requirement for storing results, do so.
			String outputlabelsfile = (String) params.get("ffnn.outputlabelsfile");
			if (outputlabelsfile!=null) {
				// first, add the "hiddenws$i$ and "outputws" key-value pairs in params
				FFNN4Train ft = (FFNN4Train) func;
				double[] all_weights = arg.getDblArray1();
				final int num_hidden_layers = ft.getNumHiddenLayers();
				final double[][] matrix = (double[][]) params.get("ffnn.traindata");
				final double[] labels = (double[]) params.get("ffnn.trainlabels");
				int num_data_features = matrix[0].length;
				for (int l=0; l<num_hidden_layers; l++) {
					double[][] wi = ft.getLayerWeights(l, all_weights, num_data_features);
					params.put("hiddenws"+l, wi);
				}
				double[] outws = ft.getOutputWeights(all_weights);
				params.put("outputws", outws);
				PrintWriter pw = new PrintWriter(new FileWriter(outputlabelsfile));
				int num_errors = 0;  // itc: HERE rm asap
				for (int row=0; row<matrix.length; row++) {
					double c_label = ft.evalNetworkOnInputData(matrix[row], params);
					// itc: HERE rm asap from here 
					// let's calculate the number of errors as well for the isoft_alex data
					if (c_label > 0.5) num_errors += 1-labels[row];
					else num_errors += labels[row];
					// itc: HERE rm up to here
					pw.println(c_label);
				}
				// itc: HERE rm asap from here
				final double acc = 100*(1.0-((double)num_errors)/labels.length);
				System.err.println("accuracy on training set="+acc+"%");
				// itc: HERE rm asap up to here
				pw.flush();
				pw.close();
			}
      long dur = System.currentTimeMillis()-start_time;
      System.out.println("total time (msecs): "+dur);
      System.out.println("VVV,"+p.getDouble()+",TTT,"+dur+",NNN,"+
				                 wrapper_func.getEvalCount()+",PPP,AVD,FFF,"+args[0]);  
      // above is for parser program to extract from output
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
