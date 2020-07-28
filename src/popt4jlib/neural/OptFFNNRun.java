package popt4jlib.neural;

import popt4jlib.LocalOptimizerIntf;
import popt4jlib.*;
import java.util.*;
import parallel.distributed.PDBatchTaskExecutor;
import utils.DataMgr;
import utils.LightweightParams;
import utils.RndUtil;
import java.io.FileWriter;
import java.io.PrintWriter;


/**
 * Test-driver program for optimizing feed-forward neural networks via ANY of 
 * the meta-heuristic optimizers available in this library. Post-processing 
 * local optimization is possible via the optional "opt.localoptimizer" 
 * parameter.
 * Notes:
 * <ul>
 * <li> 2020-07-22: the wrapper function in the <CODE>minimize(f)</CODE> main
 * method will not create a <CODE>popt4jlib.FunctionBase</CODE> to pass to the
 * minimizer, but passes the function f directly.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class OptFFNNRun {

  /**
   * public no-arg (no-op) constructor
   */
  public OptFFNNRun() {

  }


  /**
   * run as 
	 * <CODE> 
	 * java -cp &lt;classpath&gt; popt4jlib.neural.OptFFNNRun &lt;params_file&gt; 
	 * [random_seed] [maxfuncevalslimit]
	 * </CODE>.
   * 
	 * The params_file must contain lines that can be parsed by the utility method
	 * <CODE>utils.DataMgr.readPropsFromFile(params_file)</CODE> that will be 
	 * understood by the optimizer specified in a particular line of the form:
	 * <ul>
   * <li> class,ffnn.mainoptimizer, &lt;fullclassnameoffunction&gt; mandatory, 
	 * the name of the java class defining the optimizer to use; example name:
	 * "popt4jlib.GA.DGA".
	 * </ul>
	 * <p> There are also a number of other lines specifying mandatory key,value 
	 * pairs, in addition to what is required by the used classes themselves: one
	 * line must specify a key-value pair of the form:
	 * &lt;"opt.function", popt4jlib.neural.FFNN4Train[B] func&gt; which denotes 
	 * the actual function to optimize.
	 * <ul>
	 * <li> Further, if a key-value pair of the form:
	 * &lt;"ffnn.outputlabelsfile",&lt;filename&gt;&gt; is in the parameters, that
	 * file will be used to write the outputs of the neural network for each 
	 * training instance passed in the params for key "ffnn.traindata".
	 * <li> Also notice that in case the optimizer chosen is capable of 
	 * using a cluster of machines to distribute the function evaluations (e.g. 
	 * DGA, DPSO etc.), then an appropriate line specifying the initialization cmd
	 * for each of the workers in the cluster must be specified, e.g.
	 * "class,dga.pdbtexecinitedwrkcmd,
	 *  popt4jlib.neural.FFNN4TrainEvalPDBTExecInitCmd,
	 *  testdata/traindata1.dat,testdata/trainlabels1.dat".
	 * In such a case, instead of having lines such as those below in the params
	 * file:
	 * <pre>
	 * # train data:
	 * matrix,ffnn.traindata,testdata/traindata1.dat
	 * # train labels
	 * dblarray,ffnn.trainlabels,testdata/trainlabels1.dat
	 * </pre>
	 * the params file should ONLY have the following two lines:
	 * <pre>
	 * ffnn.traindatafile,testdata/traindata1.dat
	 * ffnn.trainlabelsfile,testdata/trainlabels1.dat
	 * </pre>
	 * which will NOT cause the parameters to contain the actual matrix and labels
	 * data.
   * </ul>
	 * 
   * <p> The optional second argument, if present, overrides the initial random
   * seed specified in the params_file.
   *
   * <p> The optional third argument, if present, overrides any max. limit set
   * on the number of function evaluations allowed. After this limit, the
   * function will always return Double.MAX_VALUE instead, and won't increase
   * the evaluation count.
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      long start_time = System.currentTimeMillis();
      HashMap params = utils.DataMgr.readPropsFromFile(args[0]);
			LightweightParams pl = new LightweightParams(params);
      FunctionIntf func = (FunctionIntf) pl.getObject("opt.function");
			// get training data/labels from the start
			// check for existence of the "ffnn.train[data|labels]file" key in which
			// case we must invoke the TrainData.readTrainingDataFromFiles() function
			if (params.containsKey("ffnn.traindatafile")) {
				String tdatafile = (String) params.get("ffnn.traindatafile");
				String tlabelsfile = (String) params.get("ffnn.trainlabelsfile");
				TrainData.readTrainingDataFromFiles(tdatafile, tlabelsfile);
			}
			final double[][] matrix = params.containsKey("ffnn.traindata") ?
				(double[][]) params.get("ffnn.traindata") :
				TrainData.getTrainingVectors();
			final double[] labels = params.containsKey("ffnn.trainlabels") ?
			  (double[]) params.get("ffnn.trainlabels") :
				TrainData.getTrainingLabels();			
			// finalize ffnnb initialization in case it's a FFFNN4TrainB object 
			// and needs initialization
			if (func instanceof FFNN4TrainB) {
				FFNN4TrainB f2 = (FFNN4TrainB) func;
				if (!f2.isInitialized())
					f2.finalizeInitialization(matrix[0].length);
			}
      Integer maxfuncevalI = pl.getInteger("function.maxevaluationtime");
      Boolean doreentrantMT = pl.getBoolean("function.reentrantMT");
			int nt = 1;
      Integer ntI = pl.getInteger("opt.numthreads");
      if (ntI!=null) nt = ntI.intValue();
			if (args.length>1) {
        long seed = Long.parseLong(args[1]);
        RndUtil.getInstance().setSeed(seed);  // updates all extra instances too
      }
      if (args.length>2) {
        long num = Long.parseLong(args[2]);
        params.put("maxfuncevalslimit",new Long(num));
      }
      FunctionIntf wrapper_func = null;
      if (maxfuncevalI!=null && maxfuncevalI.intValue()>0)
        wrapper_func = 
					new LimitedTimeEvalFunction(func, maxfuncevalI.longValue());
      else if (doreentrantMT!=null && doreentrantMT.booleanValue()==true) {
        wrapper_func = new ReentrantFunctionBaseMT(func, nt);
      }
      // else wrapper_func = new FunctionBase(func);
			else wrapper_func = func;
      params.put("function",wrapper_func);
			OptimizerIntf opter = (OptimizerIntf) pl.getObject("ffnn.mainoptimizer");
			opter.setParams(params);
      // check for an ObserverIntf
      ObserverIntf obs=(ObserverIntf) params.get("opt.observerlocaloptimizer");
      if (obs!=null) {
        ((GLockingObservableObserverBase) opter).registerObserver(obs);
        ((LocalOptimizerIntf) obs).setParams(params);
      }

      // register handle to show soln if we stop the program via ctrl-c
      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
					if (opter instanceof IncumbentProviderIntf) {
							final double val = 
								((IncumbentProviderIntf)opter).getIncumbentValue();
						double[] arg=null;
						try {
							arg = 
								(double[]) ((IncumbentProviderIntf)opter).getIncumbent();
							
						}
						catch (ClassCastException e) {
							VectorIntf arg2 = 
								(VectorIntf) ((IncumbentProviderIntf)opter).getIncumbent();
							arg = arg2.getDblArray1();  // makes copy, but nevermind here
						}
						if (arg==null) {
							System.err.println("OptFFNN Shutdown hook: null weights...");
							return;
						}
						System.out.println("best val found so far: "+val);
						// compute accuracy
						// first, add the "hiddenws$i$ and "outputws" key-value pairs in p
						final FFNN4Train ft = (FFNN4Train) func;
						final boolean include_biases = ft instanceof FFNN4TrainB;
						final double[] all_weights = arg;
						final int num_hidden_layers = ft.getNumHiddenLayers();
						int num_data_features = matrix[0].length;
						for (int l=0; l<num_hidden_layers; l++) {
							double[][] wi = include_biases ?
								ft.getLayerWeightsWithBias(l, all_weights, num_data_features) :
								ft.getLayerWeights(l, all_weights, num_data_features);
							params.put("hiddenws"+l, wi);
						}
						double[] outws = include_biases ? 
															 ft.getOutputWeightsWithBias(all_weights) :
															 ft.getOutputWeights(all_weights);
						params.put("outputws", outws);
						int num_errors = 0;
						for (int row=0; row<matrix.length; row++) {
							double c_label = ft.evalNetworkOnInputData(matrix[row], params);
							// calculate the number of errors as well, assuming labels are
							// integer -consecutive- numbers.
							int comp_lbl = (int)Math.round(c_label);
							if (comp_lbl != (int) labels[row]) ++num_errors;
						}
						final double acc = 100*(1.0-((double)num_errors)/labels.length);
						System.out.println("accuracy on training set="+acc+"%");
					}
				}
			}
			);
			
      utils.PairObjDouble p = opter.minimize(wrapper_func);
      double[] arg = null;
      try {
        arg = (double[]) p.getArg();
      }
      catch (ClassCastException e) {
				try {
          DblArray1Vector y = (DblArray1Vector) p.getArg();
          arg = y.getDblArray1();
				}
				catch (ClassCastException e2) {
					// no-op
				}
      }
			utils.PairObjDouble p2 = null;
      if (arg!=null) {
        System.out.print("best soln found:[");
        for (int i = 0; i < arg.length; i++) System.out.print(arg[i] + " ");
        System.out.println("] VAL=" + p.getDouble());
				// final local optimization
        LocalOptimizerIntf lasdst = 
					(LocalOptimizerIntf) params.get("opt.localoptimizer");
        if (lasdst!=null) {
          VectorIntf x0 = new popt4jlib.DblArray1Vector(arg);
          params.put("gradientdescent.x0", x0);
					// remove any batch-size optimization, as this is a local search 
					// process about to take place
					// params.remove("ffnn.randombatchsize");
					// instead, do the following: if there was a randombatchsize specified
					// quadruple it now for the local-search process
					if (params.containsKey("ffnn.randombatchsize")) {
						int orig_size = 
							((Integer)params.get("ffnn.randombatchsize")).intValue();
						params.put("ffnn.randombatchsize", new Integer(orig_size*4));
					}
					if (nt>1) {  
            // add an executor to the params to allow for parallel evaluation of 
						// the FFNN4Train[B] function on the training dataset! 
						PDBatchTaskExecutor extor = 
							PDBatchTaskExecutor.newPDBatchTaskExecutor(nt);
						params.put("ffnn.pdbtexecutor",extor);
					}					
          lasdst.setParams(params);
          p2 = lasdst.minimize(wrapper_func);
          VectorIntf xf = (VectorIntf) p2.getArg();
          System.out.print("Optimized (via LocalOptimizer) best soln found:[");
          for (int i = 0; i < xf.getNumCoords(); i++)
            System.out.print(xf.getCoord(i) + " ");
          System.out.println("] VAL=" + p2.getDouble());
        }
      } else System.err.println("Optimizer did not find any solution.");
      double val = (p2==null || p2.getDouble()>=Double.MAX_VALUE) ? 
				             p.getDouble() : p2.getDouble();
			if (wrapper_func instanceof FunctionBase)
        System.out.println("total function evaluations="+
					                 ((FunctionBase) wrapper_func).getEvalCount());
      else if (wrapper_func instanceof ReentrantFunctionBase)
        System.out.println("total function evaluations="+
					                 ((ReentrantFunctionBase) wrapper_func).
														 getEvalCount());
      else if (wrapper_func instanceof ReentrantFunctionBaseMT)
        System.out.println("total function evaluations="+
					                 ((ReentrantFunctionBaseMT) wrapper_func).
														 getEvalCount());
      else if (wrapper_func instanceof LimitedTimeEvalFunction) {
        LimitedTimeEvalFunction f = (LimitedTimeEvalFunction) wrapper_func;
        System.out.println("total function evaluations="+f.getEvalCount()+
                           " total SUCCESSFUL function evaluations="+
					                 f.getSucEvalCount());
      }
			
			// finally, if there exists a requirement for storing results, do so.
			String outputlabelsfile = (String) params.get("ffnn.outputlabelsfile");
			if (outputlabelsfile!=null) {
				// first, add the "hiddenws$i$ and "outputws" key-value pairs in params
				final FFNN4Train ft = (FFNN4Train) func;
				final boolean include_biases = ft instanceof FFNN4TrainB;
				final double[] all_weights = arg;
				final int num_hidden_layers = ft.getNumHiddenLayers();
				/*
				if (matrix==null || labels==null) {
					// see if data can be read from "ffnn.train[data|labels]file" param
					System.err.println("OptFFNNRun: matrix or labels is null, "+
						                 "trying to read from file");
					String traindatafile = (String) params.get("ffnn.traindatafile");
					if (traindatafile!=null) 
						matrix = DataMgr.readMatrixFromFile(traindatafile);
					String trainlabelsfile = (String) params.get("ffnn.trainlabelsfile");
					if (trainlabelsfile!=null) 
						labels = DataMgr.readDoubleLabelsFromFile(trainlabelsfile);
				}
				*/
				int num_data_features = matrix[0].length;
				for (int l=0; l<num_hidden_layers; l++) {
					double[][] wi = include_biases ?
						ft.getLayerWeightsWithBias(l, all_weights, num_data_features) :
						ft.getLayerWeights(l, all_weights, num_data_features);
					params.put("hiddenws"+l, wi);
				}
				double[] outws = include_biases ? 
					                 ft.getOutputWeightsWithBias(all_weights) :
					                 ft.getOutputWeights(all_weights);
				params.put("outputws", outws);
				PrintWriter pw = new PrintWriter(new FileWriter(outputlabelsfile));
				int num_errors = 0;
				for (int row=0; row<matrix.length; row++) {
					double c_label = ft.evalNetworkOnInputData(matrix[row], params);
					// let's calculate the number of errors as well, assuming labels are
					// integer -consecutive- numbers.
					int comp_lbl = (int)Math.round(c_label);
					if (comp_lbl != (int) labels[row]) ++num_errors;
					pw.println(c_label);
				}
				final double acc = 100*(1.0-((double)num_errors)/labels.length);
				System.out.println("accuracy on training set="+acc+"%");
				pw.flush();
				pw.close();
			}
			
      long end_time = System.currentTimeMillis();
      long dur = end_time-start_time;
			System.out.println("total time (msecs): "+dur);
			if (wrapper_func instanceof FunctionBase)
        System.out.println("VVV,"+val+",TTT,"+dur+",NNN,"+
				                   ((FunctionBase)wrapper_func).getEvalCount()+
				                   ",PPP,???,FFF,"+args[0]);  // for parser program to 
		                                                  // extract from output
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
