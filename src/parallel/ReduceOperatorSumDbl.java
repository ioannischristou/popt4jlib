/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel;

import java.util.*;

/**
 * find the sum of double values in the data values of the threadsData table.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class ReduceOperatorSumDbl extends ReduceOperator {
	private final static ReduceOperatorSumDbl _instance = new ReduceOperatorSumDbl();  // singleton design pattern
	
	
	public static ReduceOperatorSumDbl getInstance() {
		return _instance;
	}

	
	/**
	 * return the sum of all double values in the argument. 
	 * @param threadsData HashMap // map&lt;Thread,Object val&gt;
	 * @return Object // Double
	 */
	public Object reduce(HashMap threadsData) {
		if (threadsData==null) return null;
		double val = 0.0;
		Iterator it = threadsData.values().iterator();
		while (it.hasNext()) {
			Object v = it.next();
			if (v instanceof Double) {
				double vd = ((Double) v).doubleValue();
				val += vd;
			} else if (v instanceof double[]) {
				double[] va = (double[]) v;
				for (int i=0; i<va.length; i++) 
					val += va[i];
			}
			// else ignore
		}
		return new Double(val);
	}
	
	
	private ReduceOperatorSumDbl() {
		super();
	}

}
