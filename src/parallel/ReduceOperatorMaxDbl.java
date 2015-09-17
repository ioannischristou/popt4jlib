/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel;

import java.util.*;

/**
 * find the maximum double value in the data values of the threadsData table.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class ReduceOperatorMaxDbl extends ReduceOperator {
	private final static ReduceOperatorMaxDbl _instance = new ReduceOperatorMaxDbl();  // singleton design pattern
	
	
	public static ReduceOperatorMaxDbl getInstance() {
		return _instance;
	}

	
	/**
	 * return the maximum among all values in the argument. 
	 * @param threadsData HashMap // map&lt;Thread,Object val&gt;
	 * @return Object // Double
	 */
	public Object reduce(HashMap threadsData) {
		if (threadsData==null) return null;
		double val = Double.NEGATIVE_INFINITY;
		Iterator it = threadsData.values().iterator();
		while (it.hasNext()) {
			Object v = it.next();
			if (v instanceof Double) {
				double vd = ((Double) v).doubleValue();
				if (vd>val) val = vd;
			} else if (v instanceof double[]) {
				double[] va = (double[]) v;
				for (int i=0; i<va.length; i++) 
					if (va[i]>val) val = va[i];
			}
			// else ignore
		}
		return new Double(val);
	}
	
	
	private ReduceOperatorMaxDbl() {
		super();
	}

}
