
package popt4jlib.BH;

import popt4jlib.OptimizerException;
import java.util.HashMap;
import java.io.Serializable;

/**
 * interface for objects that can perturb a current chromosome to another 
 * variant of it.
 * Extends the <CODE>java.io.Serializable</CODE> interface so that implementing 
 * objects can be transported across JVMs in distributed computation (in case
 * the local search employed by DGABH, needs to execute the perturbations this
 * object can provide.)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface ChromosomePerturberIntf extends Serializable {
	/**
	 * returns a (presumably small) perturbation of the 1st argument passed in.
	 * @param chromosome Object
	 * @param params HashMap
	 * @return Object the new chromosome created from the passed in argument
	 * @throws OptimizerException if the perturbation process fails
	 * @throws IllegalArgumentException if any parameters in the params argument
	 * are not as expected
	 */
	public Object perturb(Object chromosome, HashMap params) 
		throws OptimizerException;
}
