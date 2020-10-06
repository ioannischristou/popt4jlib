package parallel;

import java.util.Random;

/**
 * test-driver for the <CODE>Lock2</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Lock2Test {
	/**
	 * invoke as:
	 * <CODE>
	 * java -cp &lt;classpath&gt; parallel.Lock2Test 
	 * [num_threads(100)] [num_iters(1000)] [sleep_time_in_nanos(0)]
	 * </CODE>.
	 * @param args 
	 */
	public static void main(String[] args) {
		int n = 100;
		if (args.length>0) {
			n = Integer.parseInt(args[0]);
		}
		int m = 1000;
		if (args.length>1) {
			m = Integer.parseInt(args[1]);
		}
		final int mf = m;
		int s = 0;
		if (args.length>2) {
			s = Integer.parseInt(args[2]);
		}
		final int sf = s;
		final Random r = new Random(5);
		final Lock2 lock = new Lock2();
		Thread[] threads = new Thread[n];
		for (int i=0; i<n; i++) {
			final int i_f = i;
			threads[i] = new Thread(new Runnable() {
				public void run() {
					System.out.println("Thread-"+i_f+" starting");
					for (int j=0; j<mf; j++) {
						//System.err.println("T"+i_f+" j="+j+" now trying lock");
						lock.getLock();
						// simulate some critical section work
						try {
							final int sd_in = r.nextInt(sf+1);
							Thread.sleep(0,sd_in);
						}
						catch (InterruptedException e) {
							e.printStackTrace();
						}
						//System.err.println("T"+i_f+" j="+j+" released lock");
						lock.releaseLock();
						// simulate some non-critical section work
						try {
							final int sd_out = r.nextInt(i_f*10+sf+1);
							Thread.sleep(0,sd_out);
						}
						catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					System.out.println("Thread-"+i_f+" done.");
				}
			});
		}
		long st = System.currentTimeMillis();
		for (int i=0; i<n; i++) {
			threads[i].start();
		}
		for (int i=0; i<n; i++) {
			try {
				threads[i].join();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}			
		}
		long d = System.currentTimeMillis()-st;
		System.out.println("ALL Done in "+d+" msecs");
	}
}
