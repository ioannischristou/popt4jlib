
package popt4jlib.BH;

import java.util.HashMap;

/**
 * interface for objects that can perturb a current chromosome to another 
 * variant of it.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface ChromosomePerturberIntf {
	/**
	 * returns a (presumably small) perturbation of the 1st argument passed in.
	 * @param chromosome Object
	 * @param params HashMap
	 * @return Object the new chromosome created from the passed in argument
	 */
	public Object perturb(Object chromosome, HashMap params);
}
