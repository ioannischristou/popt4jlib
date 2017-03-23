package parallel.distributed;

import parallel.TaskObject;
import tests.TridNonlinearFunction;
import utils.RndUtil;
import java.io.Serializable;
import java.io.IOException;


/**
 * test-driver for PDAsynchBatchTaskExecutorClt and networks of 
 * PDAsynchBatchTaskExecutorWrk objects in general.
 * For this test to run, a DConditionCounterSrv must run on the localhost in the
 * default port, as well as a DAccumulatorSrv (also on localhost/default port).
 * Then, a PDAsynchBatchTaskExecutorSrv together with a registered 
 * PDAsynchBatchTaskExecutorWrk must be running also on the localhost and 
 * default ports.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDAsynchBatchTaskExecutorCltTest {

	/**
	 * sole constructor is a no-op.
	 */
  private PDAsynchBatchTaskExecutorCltTest() {
    // no-op
  }


  /**
   * auxiliary method, called by main().
   */
  private void run() throws IOException {
    long start = System.currentTimeMillis();
    final int numdims = 100;
    final int numtasks = 200;
    PDAsynchBatchTaskExecutorClt client = 
			PDAsynchBatchTaskExecutorClt.getInstance();
    // thread-1 tasks
    TNLAsynchEvalTask[] tasks1 = new TNLAsynchEvalTask[numtasks];
		for (int i=0; i<tasks1.length; i++) {
	    double[][] x10 = new double[10][];
			for (int j=0; j<10; j++) {
				double[] x = getRandomArray(numdims);
				x10[j] = x;
			}
      tasks1[i] = new TNLAsynchEvalTask(x10);
    }
    // thread-2 tasks
    TNLAsynchEvalTask[] tasks2 = new TNLAsynchEvalTask[numtasks];
		for (int i=0; i<tasks2.length; i++) {
	    double[][] x10 = new double[10][];
			for (int j=0; j<10; j++) {
				double[] x = getRandomArray(numdims);
				x10[j] = x;
			}
      tasks2[i] = new TNLAsynchEvalTask(x10);
    }
		// set the number of tasks to the distributed condition counter
		DConditionCounterClt cond_counter = new DConditionCounterClt();
		try {
			cond_counter.increment(tasks1.length+tasks2.length);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
    PDABTECTThread t1 = new PDABTECTThread(client, tasks1);
    t1.start();
    PDABTECTThread t2 = new PDABTECTThread(client, tasks2);
    t2.start();
    try {
      t1.join();
      t2.join();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
		// await on the cond-counter
		try {
			cond_counter.await();
			cond_counter.shutDownSrv();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
    // ok, tasks are all done, now find best number
    double best = Double.NaN;
		try {
			best = DAccumulatorClt.getMinNumber();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
    long dur = System.currentTimeMillis()-start;
    System.out.println("Overall best="+best+" (duration="+dur+" msecs)");
		try {
			DAccumulatorClt.shutDownSrv();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		PDAsynchBatchTaskExecutorClt.disconnect();
  }


  /**
   * invoke with no args as:
   * <CODE>java -cp &lt;classpath&gt; 
	 * parallel.distributed.PDAsynchBatchTaskExecutorCltTest</CODE>.
   * @param args String[]
   */
  public static void main(String[] args) {
    PDAsynchBatchTaskExecutorCltTest test = 
			new PDAsynchBatchTaskExecutorCltTest();
		try {
			test.run();
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
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
  class PDABTECTThread extends Thread {
    PDAsynchBatchTaskExecutorClt _client;
    TNLAsynchEvalTask[] _tasks;
    PDABTECTThread(PDAsynchBatchTaskExecutorClt client, 
			             TNLAsynchEvalTask[] tasks) {
      _client = client;
      _tasks = tasks;
    }
		public void run() {
			TNLAsynchEvalTask[] tasks = new TNLAsynchEvalTask[100];  
      // send 100 tasks at a time
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
    private void run(TNLAsynchEvalTask[] tasks) {
      try {
        System.out.println("CltTest: submitting tasks of size "+tasks.length);
        _client.submitWorkFromSameHost(tasks);
				//Thread.sleep(1000);  // itc: HERE rm asap to prevent socket problems due to the submitXXX() call opening/closing sockets
        System.out.println("CltTest: submitted tasks.");
      }
      catch (PDAsynchBatchTaskExecutorException e) {
        e.printStackTrace();
        // attempt to re-submit for 10 seconds, then exit
        long now = System.currentTimeMillis();
        long max_duration = 10*1000L;
        while (System.currentTimeMillis()<now+max_duration) {
          try {
            _client.submitWorkFromSameHost(tasks);
            return;
          }
          catch (PDAsynchBatchTaskExecutorException e2) {
            System.err.println("oops, failed again with exception msg \""+
							                 e2.getMessage()+"\"");
          }
          catch (Exception e3) {
            e3.printStackTrace();
            System.err.println("PDABTECTThread.run(): exits due to exception");
            return;
          }
        }
        System.err.println("PDABTECTThread.run(): exits due to exception");
      }
      catch (Exception e) {
        e.printStackTrace();
        System.err.println("PDABTECTThread.run(): exits due to exception");
      }
    }
	}
}


/**
 * auxiliary class implementing TaskObject interface, by means of evaluating
 * the <CODE>tests.TridNonlinearFunction</CODE> at a given vector.
 * Each task upon generation, increments the DConditionCounter found on the 
 * localhost, and upon finishing its computation in the run() method decrements
 * that counter. It also sends its value to the DAccumulatorSrv running on the
 * localhost.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class TNLAsynchEvalTask implements TaskObject {
  //private static final long serialVersionUID = -5971124461081483543L;
  private double[][] _x;
  private boolean _isDone=false;

  TNLAsynchEvalTask(double[][] x) throws IllegalArgumentException {
    if (x==null || x.length==0)
      throw new IllegalArgumentException("null array passed in");
    _x = x;
		/*
		// the following increment is better placed as a single "batch increment"
		// at the code that creates these tasks.
		try {
			DConditionCounterClt condCounter = new DConditionCounterClt();
			condCounter.increment();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		*/
  }

  public Serializable run() {
    TridNonlinearFunction f = new TridNonlinearFunction();
		double best=Double.MAX_VALUE;
		for (int i=0; i<_x.length; i++) {
			double y = f.eval(_x[i], null);
			if (y<best) {
				best=y;
			}
		}
		// print best value among the 10 vectors of the task
		System.err.println("TNLAsynchEvalTask: best among 10 vectors val="+best);  // itc: HERE rm asap
    _isDone = true;
		try {
			DAccumulatorClt.addNumber(best);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		try {
			DConditionCounterClt cond_counter=new DConditionCounterClt();
			cond_counter.decrement();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
    return null;
  }


  public boolean isDone() {
    return _isDone;
  }


  public void copyFrom(TaskObject obj) throws IllegalArgumentException {
		throw new IllegalArgumentException("not supported");
  }
}

