package popt4jlib.LIP;

/**
 * AdditiveSolverMT factory class, creating objects of type 
 * <CODE>AdditiveSolverXXX</CODE>.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2022</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.1
 */
public class AddSlvrFac {
	/**
	 * returns a new AdditiveSolver3 object. Sub-classes may return any other
	 * sub-type.
	 * @return AdditiveSolverMT  // AdditiveSolver3 really
	 */
	public AdditiveSolverMT newInstance() {
		return new AdditiveSolver3();
	}
}
