package popt4jlib.MSSC1D;

import java.util.Vector;
import java.util.List;

/**
 * web-service main class for DP-based parallel clustering.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class SolverWSvc {
  public SolverWSvc() {
  }


  /**
   * the method solves the number clustering problem and returns
   * a List&lt;Integer&gt; result where result[i] indicates to which
   * cluster from [1...k] the numbers[i] element belongs to.
   * Last element of result vector is value of partition obtained.
   * If the problem is infeasible, null is returned.
   * @param numbers Vector&lt;Double&gt; not necessarily sorted, size let's say n
   * @param k int
   * @param p double
   * @return List&lt;Integer&gt;, size n+1, last element is Double, the value of 
	 * the partition
   */
  public List solve(Vector numbers, int k, double p) {
    if (numbers==null || numbers.size()==0) return null;
    double[] arr = new double[numbers.size()];
    for (int i=0; i<arr.length; i++) arr[i] = ((Double) numbers.elementAt(i)).doubleValue();
    Params params = new Params(arr, p, k);
    List result;
    Solver s = new Solver(params);
    try {
      double v = s.solveDP2ParallelMat(2);  // use 2 threads
      result = s.getSolutionIndices();
      result.add(new Double(v));  // last element is value of partition
    }
    catch (CException e) {
      e.printStackTrace();
      return null;
    }
    return result;
  }
	
}

