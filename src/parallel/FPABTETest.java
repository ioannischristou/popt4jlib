package parallel;

import java.io.Serializable;
import java.util.Vector;

/**
 * test-driver for the <CODE>FasterParallelAsynchBatchTaskExecutor</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FPABTETest {
	/**
	 * sole constructor.
	 */
  public FPABTETest() {
  }

	
	/**
	 * invoke as:
	 * <CODE>java -cp &lt;classpath&gt; parallel.FPABTETest &lt;j0&gt; &lt;numtasks&gt; &lt;numthreads&gt; [range(1000)]</CODE>.
	 * @param args 
	 */
  public static void main(String[] args) {
    if (args.length<3) {
      System.err.println("usage: java -cp <classpath> parallel.FPABTETest <j0> <numtasks> <numthreads> [range](def: 1000)");
      System.exit(-1);
    }
    try {
      long start = System.currentTimeMillis();
      Vector tasks = new Vector();
      java.util.Random r = new java.util.Random(7);
      final long j0 = Long.parseLong(args[0]);
      final int numtries = Integer.parseInt(args[1]);
      final int numthreads = Integer.parseInt(args[2]);
      int range = 1000;
      if (args.length>3) range = Integer.parseInt(args[3]);
      FasterParallelAsynchBatchTaskExecutor fpabte = 
							FasterParallelAsynchBatchTaskExecutor.
											newFasterParallelAsynchBatchTaskExecutor(numthreads);
      for (int i=0; i<numtries; i++) {
        long j1 = j0*r.nextInt();
        if (j1<0) j1 = -j1;
        long j2 = j1 + range;
        TestTask tti = new TestTask(i, j1, j2);
        tasks.add(tti);
      }
      fpabte.executeBatch(tasks);
      fpabte.shutDownAndWait4Threads2Finish();
      long dur = System.currentTimeMillis()-start;
      System.out.println("Executed in "+dur+" msecs.");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

	
	/**
	 * auxiliary inner-class, not part of the public API.
	 */
	static class TestTask implements TaskObject {
		//private final static long serialVersionUID = 3529832591673689027L;
		int _id;
		long _i;
		long _j;
		Vector _factors;
		boolean _done = false;

		public TestTask(int id, long i, long j) {
			_id = id; _i = i; _j = j;
		}

		public Serializable run() {
			// compute all prime factors for all numbers between _i and _j
			_factors = new Vector();
			for (long k = _i; k<=_j; k++) {
				boolean nofactors=true;
				long c = k;
				long sqrti = (long) Math.ceil(Math.sqrt(k));
				for (long j = 2; j <= sqrti; j++) {
					if (c % j == 0) {
						_factors.add(new Long(j));
						c /= j;
						j--;
						nofactors=false;
					}
				}
				if (nofactors) _factors.add(new Long(k));
			}
			System.out.println("Task "+_id+" executed and found "+_factors.size()+
												 " factors in ["+_i+", "+_j+"]");
			_done=true;
			return this;
		}

		public boolean isDone() {
			return _done;
		}

		public void copyFrom(TaskObject t) throws IllegalArgumentException {
			throw new IllegalArgumentException("copyFrom(t) method not supported");
		}

		public String toString() {
			return "(Tid: "+_id+" _i="+_i+" _j="+_j+")";
		}
	}

}

