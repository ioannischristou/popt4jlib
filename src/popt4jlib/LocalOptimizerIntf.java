package popt4jlib;

import java.util.HashMap;
import java.io.Serializable;

/**
 * interface for local-optimization methods (methods seeking a local minimum
 * in the neighborhood of a "starting point" solution).
 * Extends the <CODE>java.io.Serializable</CODE> interface so that implementing 
 * objects can be transported across JVMs in distributed computation.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 2.0
 */
public interface LocalOptimizerIntf extends OptimizerIntf, Serializable {
	/**
	 * create and return a new (empty) instance of the implementing object, that
	 * will need to set its parameters first before invoking the 
	 * <CODE>OptimizerIntf</CODE>'s <CODE>minimize(f)</CODE> method.
	 * @return LocalOptimizerIntf
	 */
  public LocalOptimizerIntf newInstance();
	
	
	/**
	 * get a copy of this object's parameters.
	 * @return HashMap may be null
	 */
  public HashMap getParams();
		
}

