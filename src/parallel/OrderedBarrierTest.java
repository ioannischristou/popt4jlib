package parallel;

import java.io.Serializable;

/**
 * test-driver for <CODE>OrderedBarrier</CODE>. The test spawns a number of 
 * threads and for a number of times, each thread must print its id, in the 
 * same order it was registered in the OrderedBarrier, which is in increasing
 * order from 0 to numthreads-1.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class OrderedBarrierTest {
	/**
	 * single no-op constructor.
	 */
  public OrderedBarrierTest() {
  }

	/**
	 * invoke as:
	 * <CODE>java -cp &lt;classpath&gt; parallel.OrderedBarrierTest [numthreads(10)] [numiters(10)]</CODE>.
	 * @param args 
	 */
  public static void main(String[] args) {
		int nt = 10;
		if (args.length>0) nt = Integer.parseInt(args[0]);
		int niters=10;
		if (args.length>1) niters = Integer.parseInt(args[1]);
    OBThread arr[] = new OBThread[nt];
    for (int i=0; i<arr.length; i++) {
      arr[i] = new OBThread(i,niters);
      OrderedBarrier.addThread(arr[i], "mitsos");
    }
    for (int i=0; i<arr.length; i++) {
      arr[i].start();
    }
  }
	
	
	/**
	 * auxiliary inner-class, not part of the public API.
	 */
	static class OBThread extends Thread {
		private int _id;
		private int _niters;
		public OBThread(int id, int niters) { _id = id; _niters=niters; }
		public void run() {
			for (int i=0; i<_niters; i++) {
				//System.out.println("t-id="+_id+" i="+i);
				try {
					OrderedBarrier.getInstance("mitsos").orderedBarrier(new TO(_id, i));
				}
				catch (ParallelException e) {
					e.printStackTrace();
				}
			}
		}
	}

	
	/**
	 * auxiliary inner-class, not part of the public API.
	 */
	static class TO implements TaskObject {
		private final static long serialVersionUID = 4600365979228646415L;
		int _id;
		int _i;
		boolean _done;
		public TO(int id, int i) {
			_id = id;
			_i = i;
			_done=false;
		}
		public Serializable run() {
			System.out.println("t-id="+_id+" i="+_i);
			synchronized (this) {  // the synchronization is not necessary, only done
														 // for illustration purposes.
				_done = true;
				return null;
			}
		}
		public synchronized boolean isDone() {  // synchronization not necessary
			return _done;
		}
		public synchronized void copyFrom(TaskObject t) throws IllegalArgumentException {
			throw new IllegalArgumentException("copyFrom(t) method not supported");
		}
	}

}
