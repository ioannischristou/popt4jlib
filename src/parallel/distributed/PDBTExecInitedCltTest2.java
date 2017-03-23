package parallel.distributed;

import parallel.TaskObject;
import tests.TridNonlinearFunction;
import utils.RndUtil;
import java.io.*;

/**
 * test-driver for PDBTExecInitedClt and networks of PDBTExecInitedWrk
 * objects in general. Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBTExecInitedCltTest2 implements Serializable {
	// private static final long serialVersionUID = -5971124461081483543L;
	// PDBTExecInitedCltTest2 needs to be serializable as it is the outer class
	// of the IdEvalTask class, objects of which we submit via our clients to 
	// the servers for distribution to the workers to work them out...

  private PDBTExecInitedCltTest2() {
    // no-op
  }


  /**
   * auxiliary method, called by main().
   */
  private void run() {
    long start = System.currentTimeMillis();
    final int numtasks = 20;  // should reach 10000 at least
		PDBTExecInitedCltTestInitCommand2 init_cmd = 
			new PDBTExecInitedCltTestInitCommand2();
    PDBTExecInitedClt client1 = new PDBTExecInitedClt();
    PDBTExecInitedClt client2 = new PDBTExecInitedClt();
		try {
			client1.submitInitCmd(init_cmd);
			client2.submitInitCmd(init_cmd);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
    // client-1 tasks
		int validation_sum=0;
    IdEvalTask[] tasks1 = new IdEvalTask[numtasks];
    for (int i=0; i<tasks1.length; i++) {
      tasks1[i] = new IdEvalTask(i);
			validation_sum += tasks1[i]._x;
    }
    // client-2 tasks
    IdEvalTask[] tasks2 = new IdEvalTask[numtasks];
    for (int i=0; i<tasks2.length; i++) {
      tasks2[i] = new IdEvalTask(i);
			validation_sum += tasks2[i]._x;
    }
    PDBTECTThread t1 = new PDBTECTThread(client1, tasks1,1);
    t1.start();
    PDBTECTThread t2 = new PDBTECTThread(client2, tasks2,2);
    t2.start();
    try {
      t1.join();
      t2.join();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
    // now find best number
    int sum1=0;
    if (t1._results!=null) {
      System.err.println("adding through t1 results...");
      for (int i=0; i<t1._results.length; i++) {
        int y = (int) t1._results[i];
        sum1 += y;
      }
      System.err.println("t1 sum="+sum1);
    }
		int sum2=0;
    if (t2._results!=null) {
      System.err.println("adding through t2 results...");
      for (int i=0; i<t2._results.length; i++) {
        int y = (int) t2._results[i];
        sum2 += y;
      }
    }
    long dur = System.currentTimeMillis()-start;
    System.out.println("Overall sum="+(sum1+sum2)+" (duration="+dur+" msecs)");
		System.out.println("validation_sum="+validation_sum);
  }


  /**
   * invoke with no args as:
   * <CODE>java -cp &lt;classpath&gt; 
	 * parallel.distributed.PDBTExecInitedCltTest2</CODE>.
	 * A PDBTExecInitedSrv server must be running on localhost accepting client
	 * connections on default port 7891.
   * @param args String[]
   */
  public static void main(String[] args) {
    PDBTExecInitedCltTest2 test = new PDBTExecInitedCltTest2();
    test.run();
  }


  /**
	 * auxiliary nested class, not part of the public API.
	 */
  class PDBTECTThread extends Thread {
    PDBTExecInitedClt _client;
    IdEvalTask[] _tasks;
		int _id;
    int[] _results = new int[10];
    PDBTECTThread(PDBTExecInitedClt client, IdEvalTask[] tasks, int id) {
      _client = client;
      _tasks = tasks;
			_id=id;
    }
		public void run() {
			IdEvalTask[] tasks = new IdEvalTask[10];  // send 10 tasks at a time
			int j=0;
			for (int i=0; i<_tasks.length; i++) {
				if (j==10) {
					run(tasks);
					j=0;
				}
				tasks[j++] = _tasks[i];
				if (i==_tasks.length-1) {
					for(int k=j;k<tasks.length;k++) tasks[k]=new IdEvalTask(0);
					run(tasks);
				}
			}
			if (_client!=null) {
				try {
					_client.terminateConnection();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
    private void run(IdEvalTask[] tasks) {
      try {
				System.out.println("CltTest:T-"+_id+" submitting tasks: "+
					                 toString(tasks));
        Object[] results = _client.submitWorkFromSameHost(tasks,1);
        System.out.println("CltTest: T-"+_id+" got results: "+
					                 toString(results));
				setResults(results);
      }
      catch (PDBatchTaskExecutorException e) {
        e.printStackTrace();
        // attempt to re-submit for 10 seconds, then exit
        long now = System.currentTimeMillis();
        long max_duration = 10*1000L;
        while (System.currentTimeMillis()<now+max_duration) {
          try {
            Object[] results = _client.submitWorkFromSameHost(tasks,1);
						setResults(results);
            return;
          }
          catch (PDBatchTaskExecutorException e2) {
            System.err.println("oops, failed again with exception "+
							                 e2.getMessage());
          }
          catch (Exception e3) {
            e3.printStackTrace();
            System.err.println("PDBTECTThread.run(): exits due to exception");
            return;
          }
        }
        System.err.println("PDBTECTThread.run(): exits due to exception");
      }
      catch (Exception e) {
        e.printStackTrace();
        System.err.println("PDBTECTThread.run(): exits due to exception");
      }
    }
		private void setResults(Object[] results) {
			for (int i=0; i<results.length; i++) {
				Integer ri = (Integer) results[i];
				_results[i] += ri.intValue();
			}
		}
		private String toString(Object[] arr) {
			String ret="[";
			for (int i=0; i<arr.length; i++) {
				ret += arr[i];
				if (i<arr.length-1) ret += ",";
			}
			ret += "]";
			return ret;
		}
  }

	
	/**
	 * auxiliary inner class implementing TaskObject interface, by means of 
	 * returning its constructor argument.
	 * Not part of the public API.
	 * <p>Title: popt4jlib</p>
	 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
	 * <p>Copyright: Copyright (c) 2017</p>
	 * <p>Company: </p>
	 * @author Ioannis T. Christou
	 * @version 1.0
	 */
	class IdEvalTask implements TaskObject {
		// private static final long serialVersionUID = -5971124461081483543L;
		private int _x;
		private boolean _isDone=false;

		IdEvalTask(int x) {
			_x = x;
		}

		public Serializable run() {
			_isDone = true;
			return new Integer(_x);
		}


		public boolean isDone() {
			return _isDone;
		}


		public void copyFrom(TaskObject obj) throws IllegalArgumentException {
			throw new UnsupportedOperationException("unsupported");
		}
		
		
		public String toString() {
			return new Integer(_x).toString();
		}
	}	
}


/**
 * auxiliary no-op class, not part of the public API.
 */
class PDBTExecInitedCltTestInitCommand2 extends RRObject {
	//private final static long serialVersionUID = 0L;
	
	
	/**
	 * sole constructor is a no-op.
	 */
	public PDBTExecInitedCltTestInitCommand2() {
		// no-op
	}
	
	
	/**
	 * no-op.
	 * @param srv PDBatchTaskExecutorSrv
	 * @param ois ObjectInputStream
	 * @param oos ObjectOutputStream
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws PDBatchTaskExecutorException 
	 */
	public void runProtocol(PDBatchTaskExecutorSrv srv, 
		                      ObjectInputStream ois, ObjectOutputStream oos) 
		throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
		// no-op
	}
}

