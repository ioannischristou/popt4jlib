/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel;

/**
 * interface for use with dynamic, priority-based task msg-passing
 * coordinators and executors. Indicates a task that must be run on a specific 
 * thread.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface ThreadSpecificComparableTaskObject extends ComparableTaskObject {
	public int getThreadIdToRunOn();
}
