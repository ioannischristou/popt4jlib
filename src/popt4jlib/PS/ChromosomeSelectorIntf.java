package popt4jlib.PS;

import popt4jlib.OptimizerException;
import java.util.List;
import java.util.HashMap;

/**
 * class specifies the topology operator for selecting a particle among the
 * list of all particles in an island (sub-population). The interface is not
 * public as this functionality is not exposed as part of the public API.
 * Implementing classes must be in this package (popt4jlib.PS) as they must
 * have knowledge of non-public class <CODE>DPSOIndividual</CODE>.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
interface ChromosomeSelectorIntf {
	
	/**
	 * the method returns the <CODE>DPSOIndividual</CODE> object with the best
	 * value among those individuals that are in the "topological neighborhood"
	 * of the individual in the i-th position (specified in the 2nd argument) in 
	 * the list specified in the 1st argument that represents the entire island.
	 * @param individuals List  // List&lt;DPSOIndividual&gt
	 * @param i int the position of the chromosome for which we want the "guiding" 
	 * solution (found among its "topological neighbors" specified by the 
	 * implementing classes)
	 * @param gen int the iteration number that may be needed in some topological
	 * selectors that change their decision with number of iterations
	 * @param params HashMap  // HashMap&lt;String key, Object value&gt;
	 * @return DPSOIndividual
	 * @throws OptimizerException if method somehow fails
	 */
	public DPSOIndividual getBestIndividual(List individuals, int pos, int gen, HashMap params) 
		throws OptimizerException;
}
