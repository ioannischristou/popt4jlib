package popt4jlib;

/**
 * utility class that allows access to the <CODE>double[]</CODE> data of a 
 * <CODE>DblArray1Vector</CODE> object.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2019</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class DblArray1VectorAccess {
	public static double[] get_x(DblArray1Vector x) {
		return x.get_x();
	}
}
