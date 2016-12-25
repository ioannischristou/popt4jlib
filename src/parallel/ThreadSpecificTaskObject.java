/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel;

/**
 * interface indicates that a specific Runnable must be run on a thread with the
 * specified thread-id (or any thread, or any thread except a specific one).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface ThreadSpecificTaskObject extends Runnable {
	/**
	 * returns an integer value indicating the thread-id of the thread that this
	 * object must run on. If the value is <CODE>Integer.MAX_VALUE</CODE> then
	 * any thread will do. If the value has opposite sign from the current 
	 * thread's id value, then it means that the object should run on any thread
	 * other than the thread with id <CODE>-getThreadIdToRunOn()</CODE>.
	 * @return int
	 */
	public int getThreadIdToRunOn();
}
