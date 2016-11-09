/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel;

import java.util.HashMap;
import java.io.Serializable;


/**
 * Base abstract class for reduction operations (such as MIN/MAX/SUM/AGGR etc.)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 *
 */
public abstract class ReduceOperator implements Serializable {
	public ReduceOperator() {
		// no-op
	}
	
	/**
	 * the reduction operation.
	 * @param threadsData HashMap // map&lt;Thread, Object data&gt;
	 * @return Object
	 */
	public abstract Object reduce(HashMap threadsData);
}
