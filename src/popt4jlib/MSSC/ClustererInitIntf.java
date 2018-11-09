/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib.MSSC;

import java.util.List;


/**
 * Interface for K-Means initial centers determination.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.1
 */
public interface ClustererInitIntf {
	/**
	 * the single method of the interface.
	 * @param k int the number of clusters sought
	 * @return List  // List&lt;VectorIntf&gt;
	 */
	public List getInitialCenters(int k);
}
