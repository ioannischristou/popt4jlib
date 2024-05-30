package parallel;

/**
 * auxiliary class allows some TaskObject's to express the fact that they can 
 * be cancelled even during their execution (how is up to the implementation of
 * their <CODE>run()</CODE> method). This allows the 
 * <CODE>FasterParallelAsynchBatchTaskExecutor</CODE> to cancel all currently
 * running cancelable objects in its threads.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2020-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface CancelableTaskObject extends TaskObject {
	/**
	 * cancels the execution of the underlying object's <CODE>run()</CODE> method;
	 * this is usually achieved by having the run method implement a while loop
	 * that checks very often a cancel flag to make sure it's not set in order to
	 * continue executing.
	 */
	public void cancel();
}
