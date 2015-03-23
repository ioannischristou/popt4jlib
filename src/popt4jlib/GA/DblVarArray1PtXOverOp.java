package popt4jlib.GA;

import popt4jlib.OptimizerException;
import utils.*;
import java.util.*;

/**
 * implements standard 1-point crossover over variable-size arrays of doubles
 * (<CODE>double[]</CODE> data structures).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblVarArray1PtXOverOp implements XoverOpIntf {

  /**
   * sole public constructor (no-op body)
   */
  public DblVarArray1PtXOverOp() {
  }


  /**
   * implements standard 1-point crossover between two variable length
   * arrays of doubles. 
   * @param c1 Object (a double[])
   * @param c2 Object (a double[])
   * @param params Hashtable must contain a pair &lt"thread.id", $integer_value$&gt
   * @throws OptimizerException if the params are not correctly set
   * @return Pair containing two new <CODE>double[]</CODE> objects, the first
	 * having the same dimension as the first argument c1, and the second having
	 * the same dimension as the second argument c2
   */
  public Pair doXover(Object c1, Object c2, Hashtable params) throws OptimizerException {
    try {
      final int id = ( (Integer) params.get("thread.id")).intValue();
      double[] a1 = (double[]) c1;
      double[] a2 = (double[]) c2;
      int min = a1.length < a2.length ? a1.length : a2.length;
      int max = a1.length > a2.length ? a1.length : a2.length;
      int xoverind = RndUtil.getInstance(id).getRandom().nextInt(min);
      double[] off1 = new double[a1.length];
      double[] off2 = new double[a2.length];
      for (int i = 0; i < max; i++) {
        if (i <= xoverind) {
          off1[i] = a1[i];
          off2[i] = a2[i];
        }
        else {
          if (i < min) off1[i] = a2[i];
          else if (off1.length > i) off1[i] = a1[i];
          if (i < min) off2[i] = a1[i];
          else if (off2.length > i) off2[i] = a2[i];
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

