/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel.distributed;

/**
 * indicates unavailability of capacity of all workers in a network of remote
 * PDAsynchBatchTaskExecutor[Wrk] workers.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDAsynchBatchTaskExecutorNWAException extends PDAsynchBatchTaskExecutorException {
	public PDAsynchBatchTaskExecutorNWAException(String msg) {
		super("PDAsynchBatchTaskExecutorNWAException:"+msg);
	}
}
