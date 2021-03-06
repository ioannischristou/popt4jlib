package analysis;

import popt4jlib.*;
import java.util.*;

/**
 * The class implements the Richardson extrapolation method to evaluate the
 * derivative of a differentiable function.
 * Note1: unfortunately, this implementation does not have a way of checking
 * when an nmax value in the Richardson recursion is "too much" or "too little".
 * Note2: there is currently no way to check if the values returned are
 * "close" to the real derivative value, or even if the derivative exists.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class GradApproximator implements VecFunctionIntf {
  private FunctionIntf _f=null;

  /**
   * public constructor.
   * @param f FunctionIntf
   */
  public GradApproximator(FunctionIntf f) {
    _f = f;
  }


  /**
   * compute the gradient of the function f passed in the constructor, at the
   * point x using Richardson extrapolation. The params hash-table may contain
   * the following pairs:
	 * <ul>
   * <li> &lt;"gradapproximator.nmax", Integer n&gt; optional, the dimension of 
   * the Richardson extrapolation. Default is 15.
	 * </ul>
   * @param x VectorIntf the point at which the gradient must be evaluated.
   * @param params HashMap any parameters to be passed to the function f, may be
	 * null
   * @throws IllegalArgumentException if x is null
   * @return VectorIntf
   */
  public VectorIntf eval(VectorIntf x, HashMap params) 
		throws IllegalArgumentException {
    if (x==null) throw new IllegalArgumentException("null arg passed");
    final int n = x.getNumCoords();
    VectorIntf xc = x.newCopy();  // work with copy so as to avoid memory
    // corruption issues in multi-threaded accesses of the vector
    double h = 1.0;
    int nmax = 15;
    Integer nmI = params != null ? 
			              (Integer) params.get("gradapproximator.nmax") : null;
    if (nmI!=null && nmI.intValue()>0)
      nmax = nmI.intValue();
    double[][] d = new double[nmax][nmax];
    VectorIntf retval = x.newInstance();  // used to be x.newCopy();
    try {
      for (int k = 0; k < n; k++) {
        h = 1.0; // reset h value for each partial derivative
        for (int i = 0; i < nmax; i++) {
          double xk = xc.getCoord(k);
          xc.setCoord(k, xk + h);
          double fphk = _f.eval(xc, params);
          xc.setCoord(k, xk - h);
          double fmhk = _f.eval(xc, params);
          double gk = (fphk - fmhk) / (2.0 * h);
          xc.setCoord(k, xk); // reset to normal value
          d[i][0] = gk;
          for (int j = 0; j <= i - 1; j++) {
            d[i][j + 1] = d[i][j] +
                (d[i][j] - d[i - 1][j]) / (Math.pow(4.0, j + 1.0) - 1.0);
          }
          h = h / 2.0;
        }
        retval.setCoord(k, d[nmax - 1][nmax - 1]);
      }
			if (xc instanceof PoolableObjectIntf) {
				((PoolableObjectIntf) xc).release();
			}
    }
    catch (parallel.ParallelException e) {  // can never get here
      e.printStackTrace();
    }
    return retval;
  }


  /**
   * evaluate the "coordindex"-th partial derivative of the function.
   * @param x VectorIntf
   * @param params HashMap
   * @param coordindex int
   * @throws IllegalArgumentException
   * @return double
   */
  public double evalCoord(VectorIntf x, HashMap params, int coordindex) 
		throws IllegalArgumentException {
    if (x==null) throw new IllegalArgumentException("null arg passed");
    // final int n = x.getNumCoords();
    VectorIntf xc = x.newCopy();  // work with copy so as to avoid memory
    // corruption issues in multi-threaded accesses of the vector
    double h = 1.0;
    int nmax = 15;
    Integer nmI = (Integer) params.get("gradapproximator.nmax");
    if (nmI!=null && nmI.intValue()>0)
      nmax = nmI.intValue();
    double[][] d = new double[nmax][nmax];
    try {
      int k = coordindex;
      h = 1.0; // reset h value for each partial derivative
      for (int i = 0; i < nmax; i++) {
        double xk = xc.getCoord(k);
        xc.setCoord(k, xk + h);
        double fphk = _f.eval(xc, params);
        xc.setCoord(k, xk - h);
        double fmhk = _f.eval(xc, params);
        double gk = (fphk - fmhk) / (2.0 * h);
        xc.setCoord(k, xk); // reset to normal value
        d[i][0] = gk;
        for (int j = 0; j <= i - 1; j++) {
          d[i][j + 1] = d[i][j] +
              (d[i][j] - d[i - 1][j]) / (Math.pow(4.0, j + 1.0) - 1.0);
        }
        h = h / 2.0;
      }
    }
    catch (parallel.ParallelException e) {  // can never get here
      e.printStackTrace();
    }
    return d[nmax-1][nmax-1];
  }
	
	
	/** test program to test the gradient aproximation formula; invoke as 
	 * <CODE>
	 * java -cp &lt;classpath&gt; analysis.GradApproximator &lt;function_class&lt;
	 *  &lt;x_1[,x_2...]&gt; [nmax(15)]
	 * </CODE>. The function to be tested must have a public no-arg constructor.
	 * @param args 
	 */
	public static void main(String[] args) {
		try {
			String function_class_name = args[0];
			Class fc = Class.forName(function_class_name);
			FunctionIntf f = (FunctionIntf) fc.newInstance();
			String xstr = args[1];
			StringTokenizer st = new StringTokenizer(xstr,",");
			int n = st.countTokens();
			DblArray1Vector x = new DblArray1Vector(n);
			int i=0;
			while (st.hasMoreTokens()) {
				x.setCoord(i++, Double.parseDouble(st.nextToken()));
			}
			int nmax = 15;
			HashMap params = null;
			if (args.length>2) {
				nmax = Integer.parseInt(args[2]);
				params = new HashMap();
				params.put("gradapproximator.nmax", new Integer(nmax));
			}
			GradApproximator gapprox = new GradApproximator(f);
			VectorIntf gax = gapprox.eval(x, params);
			System.out.println("g-approx("+x+")="+gax);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}

