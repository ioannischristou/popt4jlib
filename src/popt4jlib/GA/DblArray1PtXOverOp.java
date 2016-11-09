package popt4jlib.GA;

import popt4jlib.OptimizerException;
import utils.*;
import java.util.*;


/**
 * implements standard 1-point crossover over fixed-size arrays of doubles
 * (<CODE>double[]</CODE> data structures).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1PtXOverOp implements XoverOpIntf {

  /**
   * sole public no-arg constructor (no-op body)
   */
  public DblArray1PtXOverOp() {
  }


  /**
   * implements standard 1-point crossover between two fixed (and same) length
   * arrays of doubles.
   * @param c1 Object (a double[])
   * @param c2 Object (a double[])
   * @param params HashMap must contain a pair &lt;"thread.id", $integer_value$&gt;
   * @throws OptimizerException
   * @return Pair containing two new <CODE>double[]</CODE> objects.
   */
  public Pair doXover(Object c1, Object c2, HashMap params) throws OptimizerException {
    try {
      final int id = ( (Integer) params.get("thread.id")).intValue();
      double[] a1 = (double[]) c1;
      double[] a2 = (double[]) c2;
      int xoverind = RndUtil.getInstance(id).getRandom().nextInt(a1.length);
      double[] off1 = new double[a1.length];
      double[] off2 = new double[a1.length];
      for (int i = 0; i < a1.length; i++) {
        if (i <= xoverind) {
          off1[i] = a1[i];
          off2[i] = a2[i];
        }
        else {
          off1[i] = a2[i];
          off2[i] = a1[i];
        }
      }
      Pair p = new Pair(off1, off2);
      return p;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("doXover(): failed");
    }
  }

}

