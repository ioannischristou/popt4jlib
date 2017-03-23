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
public class PDBTExecInitedCltTest implements Serializable {
	// private static final long serialVersionUID = -5971124461081483543L;
	// PDBTExecInitedCltTest needs to be serializable as it is the outer class
	// of the TNLEvalTask class, objects of which we submit via our clients to 
	// the servers for distribution to the workers to work them out...

  private PDBTExecInitedCltTest() {
    // no-op
  }


  /**
   * auxiliary method, called by main().
   */
  private void run() {
    long start = System.currentTimeMillis();
    final int numdims = 100;
    final int numtasks = 10000;  // should reach 10000 at least
		PDBTExecInitedCltTestInitCommand init_cmd = 
			new PDBTExecInitedCltTestInitCommand();
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
    TNLEvalTask[] tasks1 = new TNLEvalTask[numtasks];
    for (int i=0; i<tasks1.length; i++) {
      double[] x = getRandomArray(numdims);
      tasks1[i] = new TNLEvalTask(x);
    }
    // client-2 tasks
    TNLEvalTask[] tasks2 = new TNLEvalTask[numtasks];
    for (int i=0; i<tasks2.length; i++) {
      double[] x = getRandomArray(numdims);
      tasks2[i] = new TNLEvalTask(x);
    }
    PDBTECTThread t1 = new PDBTECTThread(client1, tasks1);
    t1.start();
    PDBTECTThread t2 = new PDBTECTThread(client2, tasks2);
    t2.start();
    try {
      t1.join();
      t2.join();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
    // now find best number
    double best = Double.MAX_VALUE;
    if (t1._results!=null) {
      System.err.println("searching through t1 results...");
      for (int i=0; i<t1._results.length; i++) {
        Double y = (Double) t1._results[i];
        if (y!=null && y.doubleValue()<best)
          best = y.doubleValue();
      }
      System.err.println("t1 best="+best);
    }
    if (t2._results!=null) {
      System.err.println("searching through t2 results...");
      for (int i=0; i<t2._results.length; i++) {
        Double y = (Double) t2._results[i];
        if (y!=null && y.doubleValue()<best)
          best = y.doubleValue();
      }
    }
    long dur = System.currentTimeMillis()-start;
    System.out.println("Overall best="+best+" (duration="+dur+" msecs)");
  }


  /**
   * invoke with no args as:
   * <CODE>java -cp &lt;classpath&gt; 
	 * parallel.distributed.PDBTExecInitedCltTest</CODE>.
	 * A PDBTExecInitedSrv server must be running on localhost accepting client
	 * connections on default port 7891.
   * @param args String[]
   */
  public static void main(String[] args) {
    PDBTExecInitedCltTest test = new PDBTExecInitedCltTest();
    test.run();
  }


  /**
   * creates a double[] of dimension passed in the arg, whose components are
   * random numbers uniformly distributed in [0,1).
   * @param dims int
   * @return double[]
   */
  private static double[] getRandomArray(int dims) {
    double[] x = new double[dims];
    for (int i=0; i<x.length; i++) {
      x[i] = RndUtil.getInstance().getRandom().nextDouble();
    }
    return x;
  }


  /**
	 * auxiliary nested class, not part of the public API.
	 */
  class PDBTECTThread extends Thread {
    PDBTExecInitedClt _client;
    TNLEvalTask[] _tasks;
    Double[] _results = new Double[100];  // must be Serializable
    PDBTECTThread(PDBTExecInitedClt client, TNLEvalTask[] tasks) {
      _client = client;
      _tasks = tasks;
    }
		public void run() {
			TNLEvalTask[] tasks = new TNLEvalTask[100];  // send 100 tasks at a time
			int j=0;
			for (int i=0; i<_tasks.length; i++) {
				if (j==100) {
					run(tasks);
					j=0;
				}
				tasks[j++] = _tasks[i];
				if (i==_tasks.length-1) {
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
    private void run(TNLEvalTask[] tasks) {
      try {
        System.out.println("CltTest: submitting tasks of size "+tasks.length);
        Object[] results = _client.submitWorkFromSameHost(tasks,1);
				setResults(results);
        System.out.println("CltTest: got results, exiting.");
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
				Double ri = (Double) results[i];
				if (_results[i]==null || ri.doubleValue()<_results[i].doubleValue()) {
					_results[i] = ri;
				}
			}
		}
  }

	
	/**
	 * auxiliary inner class implementing TaskObject interface, by means of 
	 * evaluating the <CODE>tests.TridNonlinearFunction</CODE> at a given vector.
	 * Not part of the public API.
	 * <p>Title: popt4jlib</p>
	 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
	 * <p>Copyright: Copyright (c) 2011</p>
	 * <p>Company: </p>
	 * @author Ioannis T. Christou
	 * @version 1.0
	 */
	class TNLEvalTask implements TaskObject {
		// private static final long serialVersionUID = -5971124461081483543L;
		private double[] _x;
		private boolean _isDone=false;

		TNLEvalTask(double[] x) throws IllegalArgumentException {
			if (x==null || x.length==0)
				throw new IllegalArgumentException("null array passed in");
			_x = x;
		}

		public Serializable run() {
			TridNonlinearFunction f = new TridNonlinearFunction();
			double y = f.eval(_x, null);
			_isDone = true;
			return new Double(y);
		}


		public boolean isDone() {
			return _isDone;
		}


		public void copyFrom(TaskObject obj) throws IllegalArgumentException {
			if (obj instanceof TNLEvalTask == false)
				throw new IllegalArgumentException("obj not a TNLEvalTask");
			TNLEvalTask t = (TNLEvalTask) obj;
			_x = new double[t._x.length];
			for (int i=0; i<_x.length; i++) _x[i] = t._x[i];
			_isDone = t._isDone;
		}
	}	
}


/**
 * auxiliary no-op class, not part of the public API.
 */
class PDBTExecInitedCltTestInitCommand extends RRObject {
	//private final static long serialVersionUID = 0L;
	
	
	/**
	 * sole constructor is a no-op.
	 */
	public PDBTExecInitedCltTestInitCommand() {
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

