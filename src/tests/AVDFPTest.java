package tests;

import popt4jlib.*;
import popt4jlib.GradientDescent.*;
import java.util.*;
import utils.RndUtil;

/**
 * Test-driver class for optimizing the Fletcher-Powell Function (defined and
 * documented in <CODE>tests.FletcherPowellFunction</CODE>) using the AVD 
 * process.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class AVDFPTest {

  /**
   * public no-arg constructor
   */
  public AVDFPTest() {
  }


  /**
   * invoke as 
	 * <CODE>java -&lt;classpath&gt; tests.AVDFPTest &lt;params_file&gt;</CODE>.
   * The params_file must have the lines described in the documentation of
   * the class 
	 * <CODE>popt4jlib.GradientDescent.AlternatingVariablesDescent</CODE>.
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      long start_time = System.currentTimeMillis();
      HashMap params = utils.DataMgr.readPropsFromFile(args[0]);
      // simply add the parameters A, B and alpha
      int n = ((Integer) params.get("avd.numdimensions")).intValue();
      double maxargval = ((Double) params.get("avd.maxargval")).doubleValue();
      double minargval = ((Double) params.get("avd.minargval")).doubleValue();
      // add the initial point
      VectorIntf x0 = new DblArray1Vector(new double[n]);
			Random rnd = RndUtil.getInstance().getRandom();
      for (int j=0; j<n; j++) {
        double val = minargval + rnd.nextDouble()*(maxargval-minargval);
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
      params.put("avd.x0", x0);
      // prepare the params A,B,alpha.
      Vector[] A = new Vector[n];
      Vector[] B = new Vector[n];
      double[] alpha = new double[n];
      for (int i=0; i<n; i++) {
        A[i] = new Vector();
        B[i] = new Vector();
        alpha[i] = Math.PI*Math.sin(Math.PI/(i+1));
        for (int j=0; j<n; j++) {
          A[i].addElement(new Integer(rnd.nextInt(200)-100));
          B[i].addElement(new Integer(rnd.nextInt(200)-100));
        }
      }
      params.put("A",A);
      params.put("B",B);
      params.put("alpha",alpha);
      FunctionIntf func = (FunctionIntf) params.get("avd.function");
      FunctionBase wrapper_func = new FunctionBase(func);
      params.put("avd.function",wrapper_func);
      AlternatingVariablesDescent opter = 
				new AlternatingVariablesDescent(params);
      utils.PairObjDouble p = opter.minimize(wrapper_func);
      VectorIntf arg = (VectorIntf) p.getArg();
      System.out.print("best soln found:[");
      for (int i=0; i<arg.getNumCoords(); i++) 
				System.out.print(arg.getCoord(i)+" ");
      System.out.println("] VAL="+p.getDouble());
      System.out.println("total function evaluations="+
				                 wrapper_func.getEvalCount());
      long end_time = System.currentTimeMillis();
      System.out.println("total time (msecs): "+(end_time-start_time));
      System.out.print("true optimal solution is: alpha=[ ");
      for (int i=0; i<n; i++) System.out.print(alpha[i]+" ");
      System.out.println("] FP(alpha)=0");
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
