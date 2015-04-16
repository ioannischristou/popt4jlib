package popt4jlib.GA;

import popt4jlib.OptimizerException;
import utils.*;
import java.util.*;

/**
 * class implements the standard N-point crossover operator on chromosomes
 * represented as <CODE>int[]</CODE> objects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntVarArrayNPtXOverOp implements XoverOpIntf {

  private int _n=1;

  /**
   * public constructor accepts as sole argument the number of points the
   * crossover operator must use.
   * @param n int
   */
  public IntVarArrayNPtXOverOp(int n) {
    if (n>1) _n = n;
  }

  /**
   * the method implements the standard N-point crossover operator operating on
   * <CODE>int[]</CODE> chromosomes of varying length!.
   * @param c1 Object int[]
   * @param c2 Object int[]
   * @param params Hashtable must contain the following key,value pair:
   * <li> &lt;"thread.id",$integer_value"&gt; mandatory, the (internal) id of the
   * thread invoking this method; this number is used so as to look-up the right
   * random-number generator associated with the current thread
   * @throws OptimizerException if the params are not correctly set
   * @return Pair Pair&lt;int[], int[]&gt; with the first array of the pair having
	 * dimension equal to c1's dimension, and the second having dimension equal to
	 * c2's dimension
   */
  public Pair doXover(Object c1, Object c2, Hashtable params) throws OptimizerException {
    try {
      final int id = ( (Integer) params.get("thread.id")).intValue();
      int[] a1 = (int[]) c1;
      int[] a2 = (int[]) c2;
      int min = a1.length < a2.length ? a1.length : a2.length;
      int max = a1.length > a2.length ? a1.length : a2.length;
      int[] off1 = new int[a1.length];
      int[] off2 = new int[a2.length];
      int offset = 0;
      int[] ta = a2;
      int[] tb = a1;
      for (int n=1; n<=_n; n++) {
        int xoverind = offset + RndUtil.getInstance(id).getRandom().nextInt(min-offset);
        // switch arrays
        if (ta==a2) {
          ta = a1;
          tb = a2;
        }
        else {
          ta=a2;
          tb = a1;
        }
        for (int i = offset; i < max; i++) {
          if (i <= xoverind) {
            off1[i] = ta[i];
            off2[i] = tb[i];
          }
          else {
            if (i < min) off1[i] = tb[i];
            else if (off1.length > i) off1[i] = ta[i];
            if (i < min) off2[i] = ta[i];
            else if (off2.length > i) off2[i] = tb[i];
          }
        }
        offset += xoverind;
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
