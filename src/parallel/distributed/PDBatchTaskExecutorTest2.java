package parallel.distributed;

import parallel.*;
import java.io.*;
import java.util.*;


/**
 * test-driver class for method <CODE>throttleDown(num)</CODE> of 
 * class <CODE>PDBatchTaskExecutor</CODE>.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBatchTaskExecutorTest2 {
	/**
	 * must print 100 times some msg w/ a count incrementing correctly each time.
	 * @param args 
	 */
	public static void main(String[] args) {
		try {
			PDBatchTaskExecutor pdextor = 
				PDBatchTaskExecutor.newPDBatchTaskExecutor(100);
			// start executing 100 tasks lasting 100 msecs each. 
			final int ntasks = 100;
			ArrayList tasks = new ArrayList(ntasks);
			for (int i=0; i<ntasks; i++) {
				tasks.add(new TaskObject() {
					public Serializable run() {
						try {
							Thread.sleep(100);
						}
						catch (InterruptedException e) {
							e.printStackTrace();
							Thread.currentThread().interrupt();
						}
						return new Integer(1);
					}
					public boolean isDone() {
						throw new UnsupportedOperationException();
					}
					public void copyFrom(TaskObject to) {
						throw new UnsupportedOperationException();
					}
				});
			}
			long start = System.currentTimeMillis();
			Vector res = pdextor.executeBatch(tasks);
			long fdur = System.currentTimeMillis()-start;
			// now throttle-down the executor by 95%
			pdextor.throttleDown(95);
			start = System.currentTimeMillis();
			res = pdextor.executeBatch(tasks);
			long tdur = System.currentTimeMillis()-start;
			
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
			pdextor.shutDown();
			System.out.println("first execution duration="+fdur+" msecs");
			System.out.println("second execution duration="+tdur+" msecs");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
