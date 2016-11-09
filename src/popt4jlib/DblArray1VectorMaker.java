package popt4jlib;

import java.util.*;

/**
 * class responsible for converting double[] chromosome objects into
 * DblArray1Vector arguments for functions having as domain VectorIntf objects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1VectorMaker implements Chromosome2ArgMakerIntf {

  /**
   * no-op constructor
   */
  public DblArray1VectorMaker() {
    // no-op
  }


  /**
   * Chromosome --&gt; FunctionArgument Map.
   * @param chromosome Object must be double[]
   * @param params HashMap
   * @throws OptimizerException
   * @throws IllegalArgumentException if chromosome is not a double[]
   * @return Object DblArray1Vector having as data the double[] ref. passed in
   */
  public Object getArg(Object chromosome, HashMap params) throws OptimizerException, IllegalArgumentException {
    try {
      double[] arr = (double[]) chromosome;
      // DblArray1Vector v = new DblArray1Vector(arr);
      DblArray1Vector v = DblArray1Vector.newInstance(arr.length);
			for (int i=0; i<arr.length; i++) {
				v.setCoord(i, arr[i]);
			}
			return v;
    }
    catch (ClassCastException e) {
      throw new IllegalArgumentException("getArg(): chromosome argument is not a double[] array");
    }
  }
}
