package tests;

import popt4jlib.*;
import popt4jlib.GA.*;
import java.util.*;

/**
 * test-driver class for the DGA process for optimizing the AlternateFunction.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DGAAFTest {
  /**
   * not needed
   */
  public DGAAFTest() {
  }


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; tests.DGAAFTest &lt;params_file&gt;</CODE>
   * where the format of the params_file is described in the documentation of the
   * <CODE>main(String[] args)</CODE> method of the tests.DGATest class.
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      HashMap params = utils.DataMgr.readPropsFromFile(args[0]);
      DGA opter = new DGA(params);
      utils.PairObjDouble p = opter.minimize(new AlternateFunction());
      double[] arg = (double[]) p.getArg();
      System.out.print("best soln found:[");
      for (int i=0;i<arg.length;i++) System.out.print(arg[i]+" ");
      System.out.println("] VAL="+p.getDouble());
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
