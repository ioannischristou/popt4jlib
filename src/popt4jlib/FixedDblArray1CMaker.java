package popt4jlib;

import java.util.HashMap;


/**
 * implements the operator for creating specific chromosome objects that are of
 * type <CODE>double[]</CODE>.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FixedDblArray1CMaker implements RandomChromosomeMakerIntf {
	private double[] _arr;
	
	
  /**
   * sole public constructor.
   */
  public FixedDblArray1CMaker(double[] arr) {
    _arr = arr;
  }
	

  /**
   * creates and returns copies of the array <CODE>_arr</CODE>.
   * @param params HashMap redundant.
   * @return Object // double[] copy of the _arr array 
   */
  public Object createRandomChromosome(HashMap params) {
		double[] res = new double[_arr.length];
		for (int i=0; i<_arr.length; i++) res[i] = _arr[i];
		return res;
  }
}

