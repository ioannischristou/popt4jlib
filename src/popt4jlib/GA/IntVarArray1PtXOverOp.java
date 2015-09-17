package popt4jlib.GA;

import popt4jlib.OptimizerException;
import utils.*;
import java.util.*;

/**
 * implements standard 1-point crossover over variable-size arrays of integers
 * (<CODE>int[]</CODE> data structures).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntVarArray1PtXOverOp implements XoverOpIntf {

  /**
   * public constructor (no-op body)
   */
  public IntVarArray1PtXOverOp() {
  }


  /**
   * implements standard 1-point crossover between two variable length
   * arrays of integers (<CODE>int[]</CODE> objects).
   * @param c1 Object (a double[])
   * @param c2 Object (a double[])
   * @param params HashMap must contain a pair <"thread.id", $integer_value$>
   * @throws OptimizerException if the params are incorrectly set
   * @return Pair containing two new <CODE>int[]</CODE> objects, the first
	 * having dimension equal to c1' dimension, the second having dimension equal
	 * to c2's dimension
   */
  public Pair doXover(Object c1, Object c2, HashMap params) throws OptimizerException {
    try {
      final int id = ( (Integer) params.get("thread.id")).intValue();
      int[] a1 = (int[]) c1;
      int[] a2 = (int[]) c2;
      int min = a1.length < a2.length ? a1.length : a2.length;
      int max = a1.length > a2.length ? a1.length : a2.length;
      int xoverind = RndUtil.getInstance(id).getRandom().nextInt(min);
      int[] off1 = new int[a1.length];
      int[] off2 = new int[a2.length];
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

