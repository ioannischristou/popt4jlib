package popt4jlib.GA;

import popt4jlib.*;
import utils.*;
import java.util.*;


/**
 * implements standard 1-point crossover over fixed-size DblArray1Vector
 * objects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1Vector1PtXOverOp implements XoverOpIntf {

  /**
   * sole public no-arg constructor (no-op body)
   */
  public DblArray1Vector1PtXOverOp() {
  }


  /**
   * implements standard 1-point crossover between two fixed (and same) length
   * arrays of doubles represented as DblArray1Vector objects
   * @param c1 Object (a DblArray1Vector)
   * @param c2 Object (a DblArray1Vector)
   * @param params Hashtable must contain a pair <"thread.id", $integer_value$>
   * @throws OptimizerException
   * @return Pair containing two new <CODE>DblArray1Vector</CODE> objects.
   */
  public Pair doXover(Object c1, Object c2, Hashtable params) throws OptimizerException {
    try {
      final int id = ( (Integer) params.get("thread.id")).intValue();
      DblArray1Vector a1 = (DblArray1Vector) c1;
      DblArray1Vector a2 = (DblArray1Vector) c2;
			final int a1len = a1.getNumCoords();
      int xoverind = RndUtil.getInstance(id).getRandom().nextInt(a1len);
      DblArray1Vector off1 = DblArray1Vector.newInstance(a1len);
      DblArray1Vector off2 = DblArray1Vector.newInstance(a1len);
      for (int i = 0; i < a1len; i++) {
        if (i <= xoverind) {
          off1.setCoord(i, a1.getCoord(i));  // off1[i] = a1[i];
          off2.setCoord(i, a2.getCoord(i));  // off2[i] = a2[i];
        }
        else {
          off1.setCoord(i, a2.getCoord(i));  // off1[i] = a2[i];
          off2.setCoord(i, a1.getCoord(i));  // off2[i] = a1[i];
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

