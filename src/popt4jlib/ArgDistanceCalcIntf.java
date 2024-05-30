package popt4jlib;

import java.io.Serializable;

/**
 * The interface is a functional interface defining a calculator allowing two 
 * arguments to the <CODE>FunctionIntf</CODE> interface, to compute their 
 * distance from each other. For example, when the arguments are vectors 
 * (implementing the <CODE>VectorIntf</CODE>), an appropriate corresponding 
 * calculation is the Euclidean norm of the difference between them. 
 * The result must always be a non-negative number.
 * Extends the <CODE>java.io.Serializable</CODE> interface so that implementing 
 * objects can be transported across JVMs in distributed computation.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface ArgDistanceCalcIntf extends Serializable {
	
	/**
	 * returns a notion of the distance between the two argument objects.
	 * @param arg1 Object
	 * @param arg2 Object
	 * @return double must be non-negative
	 */
	public double dist(Object arg1, Object arg2);
}
