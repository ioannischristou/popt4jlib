package popt4jlib;

/**
 * interface to be implemented by those optimizers that can be queried at any 
 * point about their incumbent solution at that point in time.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface IncumbentProviderIntf {
	
	/**
	 * return the current incumbent solution.
	 * @return Object may be null  // usually a double[] or VectorIntf object
	 */
	public Object getIncumbent();
	
	
	/**
	 * the current incumbent value (or <CODE>Double.MAX_VALUE</CODE> if none such
	 * exists yet.)
	 * @return double
	 */
	public double getIncumbentValue();
}
