package popt4jlib.LIP;

/**
 * AdditiveSolver2 factory class, creating objects of type 
 * <CODE>AdditiveSolver3</CODE>.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class AddSlvrFac {
	/**
	 * returns a new AdditiveSolver3 object. Sub-classes may return any other
	 * sub-type.
	 * @return AdditiveSolver2  // AdditiveSolver3 really
	 */
	public AdditiveSolver2 newInstance() {
		return new AdditiveSolver3();
	}
}
