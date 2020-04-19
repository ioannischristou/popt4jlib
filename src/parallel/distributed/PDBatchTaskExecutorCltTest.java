package parallel.distributed;

import parallel.TaskObject;
import tests.TridNonlinearFunction;
import utils.RndUtil;
import java.io.Serializable;

/**
 * test-driver for PDBatchTaskExecutorClt and networks of PDBatchTaskExecutorWrk
 * objects in general. Default PDBatchTaskExecutorSrv must be running on 
 * (localhost,7890).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBatchTaskExecutorCltTest {

  private PDBatchTaskExecutorCltTest() {
    // no-op
  }


  /**
   * auxiliary method, called by main().
   */
  private void run() {
    long start = System.currentTimeMillis();
    final int numdims = 100;
    final int numtasks = 10000;
    PDBatchTaskExecutorClt client1 = new PDBatchTaskExecutorClt();
    PDBatchTaskExecutorClt client2 = new PDBatchTaskExecutorClt();
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
   * <CODE>
	 * java -cp &lt;classpath&gt; parallel.distributed.PDBatchTaskExecutorCltTest
	 * </CODE>.
   * @param args String[]
   */
  public static void main(String[] args) {
    PDBatchTaskExecutorCltTest test = new PDBatchTaskExecutorCltTest();
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


  // inner class
  class PDBTECTThread extends Thread {
    PDBatchTaskExecutorClt _client;
    TNLEvalTask[] _tasks;
    Double[] _results = new Double[100];  // must be Serializable
    PDBTECTThread(PDBatchTaskExecutorClt client, TNLEvalTask[] tasks) {
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
		}
    private void run(TNLEvalTask[] tasks) {
      try {
        System.out.println("CltTest: submitting tasks of size "+tasks.length);
        Object[] results = _client.submitWorkFromSameHost(tasks);
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
            Object[] results = _client.submitWorkFromSameHost(tasks);
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

}


/**
 * auxiliary class implementing TaskObject interface, by means of evaluating
 * the <CODE>tests.TridNonlinearFunction</CODE> at a given vector.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class TNLEvalTask implements TaskObject {
  private static final long serialVersionUID = -5971124461081483543L;
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

