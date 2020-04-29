package popt4jlib.neural;

import java.io.FileWriter;
import java.io.PrintWriter;
import popt4jlib.LocalOptimizerIntf;
import popt4jlib.*;
import popt4jlib.GA.*;
import java.util.*;
import parallel.distributed.PDBatchTaskExecutor;
import utils.RndUtil;

/**
 * Test-driver program for optimizing feed-forward neural networks via the 
 * Distributed Genetic Algorithm meta-heuristic optimizer. Post-processing local
 * optimization is possible via the optional "dga.localoptimizer" parameter.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DGAFFNNRun {

  /**
   * public no-arg (no-op) constructor
   */
  public DGAFFNNRun() {

  }


  /**
   * run as 
	 * <CODE> 
	 * java -cp &lt;classpath&gt; popt4jlib.neural.DGAFFNNRun &lt;params_file&gt; 
	 * [random_seed] [maxfuncevalslimit]
	 * </CODE>.
   * 
	 * The params_file must contain lines of the following form:
	 * 
	 * <ul>
   * <li> class,dga.function, &lt;fullclassnameoffunction&gt; mandatory, the 
	 * name of the java class defining the function to be optimized
   * <li> function.maxevaluationtime, $num$ optional, maximum wall-clock time
   * allowed for any function evaluation to complete (in milliseconds).
   * <li> function.reentrantMT, $bool_val$ optional, if present and $bool_val$
   * evaluates to true, indicates that the function to be optimized is not
   * thread-safe, but does not contain any static data members that can destroy
   * the evaluations of two different function objects, so different threads
   * evaluating the function on different arguments using different function
   * objects are safe.
   * <li> dga.numthreads, $num$ optional, indicates how many islands (each on
   * its own thread) will be created for this DGA run. Default is 1.
   * <li> rndgen,$num$,$num2$ mandatory, specifies the starting random
   * seed to use for each of the $num2$ threads to use (the value num2 must
   * equal the number given for the dde.numthreads, or 1 if no such line is
   * present). The value of num should be a positive integer.
   * <li> class,dga.observerlocaloptimizer, &lt;fullclassname&gt; optional, the
   * full class name of the class implementing the <CODE>ObserverIntf</CODE>
   * in the popt4jlib pacakge as well as the <CODE>LocalOptimizerIntf</CODE> in
   * the popt4jlib package. Used for constructing ensembles of
   * optimizers exchanging (sub-)optimal incumbent solutions as they are found.
   * Can only be used with functions accepting as args <CODE>double[]</CODE>
   * or <CODE>VectorIntf</CODE> (satisfied by <CODE>popt4jlib.neural.FFNN</CODE>
	 * type functions for which this class is designed.)
   * <li> class,dga.localoptimizer, &lt;fullclassname&gt; optional, full class
   * name of the java class implementing the <CODE>LocalOptimizerIntf</CODE> in
   * the popt4jlib package. Used as a final post-optimization step.
   * Can only be used with functions accepting as args <CODE>double[]</CODE>
   * or <CODE>VectorIntf</CODE> (satisfied by <CODE>popt4jlib.neural.FFNN</CODE>
	 * type functions for which this class is designed.)
   * <li> class,dga.randomchromosomemaker,&lt;fullclassname&gt; mandatory, the
   * full class name of the java class implementing the 
	 * <CODE>popt4jlib.RandomChromosomeMakerIntf</CODE> interface in the popt4jlib 
	 * package, responsible for creating valid random chromosome Objects to 
	 * populate the islands.
   * <li> class,dga.xoverop, &lt;fullclassname&gt; mandatory, full class name
   * of the java class implementing the <CODE>popt4jlib.GA.XoverOpIntf</CODE> 
	 * interface in the popt4jlib.GA package that produces two new chromosome 
	 * Objects from two old chromosome Objects. It is the responsibility of the 
	 * operator to always return NEW Objects.
   * <li> class,dga.mutationop, &lt;fullclassname&gt; optional, full class name
   * of the class implementing the <CODE>popt4jlib.GA.MutationOpIntf</CODE> 
	 * interface in the popt4jlib.GA package. If present, the operator will always 
	 * be applied to the resulting Objects that the XoverOpIntf will produce.
   * <li> class,dga.c2amaker, &lt;fullclassname&gt; optional, full class name
   * of the class implementing <CODE>popt4jlib.Chromosome2ArgMakerIntf</CODE> 
	 * interface in package popt4jlib that is responsible for tranforming a 
	 * chromosome Object to a function argument Object. If not present, the 
	 * default identity transformation is assumed.
   * <li> class,dga.a2cmaker, &lt;fullclassname&gt; optional, full class name
   * of the class implementing <CODE>popt4jlib.Arg2ChromosomeMakerIntf</CODE> in 
	 * the popt4jlib package responsible for transforming a 
	 * <CODE>popt4jlib.FunctionIntf</CODE> argument Object to a chromosome Object.
   * If not present, the default identity transformation is assumed. The a2c 
	 * object is only useful when other ObserverIntf objects register for this
   * SubjectIntf object and also add back solutions to it (as FunctionIntf args)
   * <li> dga.numgens, $num$ optional, the number of generations to run the
   * GA, default is 1.
   * <li> dga.numinitpop, $num$ optional, the initial population number for
   * each island, default is 10.
   * <li> dga.poplimit, $num$ optional, the maximum population for each
   * island, default is 100.
   * <li> dga.xoverprob, $num$ optional, the square of the expectation
   * of the number of times cross-over will occur divided by island population
   * size, default is 0.7.
   * <li> dga.cutoffage, $num$ optional, the number of generations an
   * individual is expected to live before being removed from the population,
   * default is 5.
   * <li> dga.varage, $num$ optional, the variance in the number of
   * generations an individual will remain in the population before being
   * removed, default is 0.9.
   * <li> maxfuncevalslimit, $num$ optional, the max. number of function
   * evaluations allowed in the process.
	 * <li> class,dga.pdbtexecinitedwrkcmd,&lt;fullclassname&gt;[args] optional, 
	 * if present, the full class name of the initialization command to send to 
	 * the network of workers to run function evaluation tasks, followed by the 
	 * constructor's arguments if any; default is null, indicating no distributed 
	 * computation.
	 * <li> dga.pdbthost, &lt;hostname&gt; optional, the name
	 * of the server to send function evaluation requests, default is localhost.
	 * <li> dga.pdbtport, $num$ optional, the port the above server listens to 
	 * for client requests, default is 7891.
   * <li> In addition, the various operators (xover, mutation, a2cmaker,
   * c2amaker, randomchromosomemaker etc.) may require additional parameters as
   * they are defined and documented in their respective class file definitions.
	 * For example, usually, the line
	 * &lt;dga.chromosomelength, $num$&gt; is required for the chromosome makers 
	 * to know what chromosome lengths to produce.
	 * <li> Further, if a key-value pair of the form:
	 * &lt;"ffnn.outputlabelsfile",&lt;filename&gt;&gt; is in the parameters, that
	 * file will be used to write the outputs of the neural network for each 
	 * training instance passed in the params for key "ffnn.traindata".
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
      FunctionIntf func = (FunctionIntf) params.get("dga.function");
      Integer maxfuncevalI = (Integer) params.get("function.maxevaluationtime");
      Boolean doreentrantMT = (Boolean) params.get("function.reentrantMT");
      int nt = 1;
      Integer ntI = (Integer) params.get("dga.numthreads");
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
      else wrapper_func = new FunctionBase(func);
      params.put("dga.function",wrapper_func);
      DGA opter = new DGA(params);
      // check for an ObserverIntf
      ObserverIntf obs=(ObserverIntf) params.get("dga.observerlocaloptimizer");
      if (obs!=null) {
        opter.registerObserver(obs);
        ((LocalOptimizerIntf) obs).setParams(params);
      }
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
/*
        // finally see how mutation went
        MutationOpIntf mop = (MutationOpIntf) params.get("dga.mutationop");
        if (mop!=null && mop instanceof DblArray1GradientDescentMutationOp) {
          int s = ((DblArray1GradientDescentMutationOp) mop).getSuccessfulMutations();
          int f = ((DblArray1GradientDescentMutationOp) mop).getFailedMutations();
          System.err.println("successful mutations="+s+" failed mutations="+f);
        }
*/
      // final local optimization
        LocalOptimizerIntf lasdst = 
					(LocalOptimizerIntf) params.get("dga.localoptimizer");
        if (lasdst!=null) {
          VectorIntf x0 = new popt4jlib.DblArray1Vector(arg);
          params.put("gradientdescent.x0", x0);
					
					if (nt>1) {  
            // add an executor to the params to allow for parallel evaluation of 
						// the FFNN4Train function on the training dataset! Use as many 
						// threads as there were islands on the DGA.
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
      } else System.err.println("DGA did not find any solution.");
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
      else {
        LimitedTimeEvalFunction f = (LimitedTimeEvalFunction) wrapper_func;
        System.out.println("total function evaluations="+f.getEvalCount()+
                           " total SUCCESSFUL function evaluations="+
					                 f.getSucEvalCount());
      }
			
			// finally, if there exists a requirement for storing results, do so.
			String outputlabelsfile = (String) params.get("ffnn.outputlabelsfile");
			if (outputlabelsfile!=null) {
				// first, add the "hiddenws$i$ and "outputws" key-value pairs in params
				FFNN4Train ft = (FFNN4Train) func;
				double[] all_weights = arg;
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
      /*
      System.err.println("Total #DblArray1Vector objects created="+DblArray1Vector.getTotalNumObjs());
      */
     System.out.println("total time (msecs): "+dur);
     if (wrapper_func instanceof FunctionBase)
       System.out.println("VVV,"+val+",TTT,"+dur+",NNN,"+
				                  ((FunctionBase)wrapper_func).getEvalCount()+
				                  ",PPP,DGA,FFF,"+args[0]);  // for parser program to 
		                                                 // extract from output
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
