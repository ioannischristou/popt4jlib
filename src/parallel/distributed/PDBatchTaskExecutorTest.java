package parallel.distributed;

import parallel.*;
import java.io.*;

/**
 * test-driver class for method <CODE>executeTaskOnAllThreads(cmd)</CODE> of 
 * class <CODE>PDBatchTaskExecutor</CODE>.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBatchTaskExecutorTest {
	/**
	 * must print 100 times some msg w/ a count incrementing correctly each time.
	 * @param args 
	 */
	public static void main(String[] args) {
		try {
			PDBatchTaskExecutor pdextor = 
				PDBatchTaskExecutor.newPDBatchTaskExecutor(100);
			TaskObject task = new TaskObject() {
				private int _cnt=0;
				public synchronized Serializable run() {
					++_cnt;
					System.err.println("Thread executes task and _cnt="+_cnt);
					return null;
				}
				public boolean isDone() {
					throw new UnsupportedOperationException();
				}
				public void copyFrom(TaskObject to) {
					throw new UnsupportedOperationException();
				}
			};
			pdextor.executeTaskOnAllThreads(task);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
