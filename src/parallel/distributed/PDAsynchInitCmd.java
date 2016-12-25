/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel.distributed;

/**
 * empty abstract class extends RRObject. Its purpose is to differentiate 
 * between other <CODE>RRObject</CODE>s and initialization command objects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public abstract class PDAsynchInitCmd extends RRObject {
	/**
	 * method specifies how, if anything, is to apply on the server before 
	 * sending to workers. Standard example is to set debug-level.
	 */
	public abstract void applyOnServer();
}
