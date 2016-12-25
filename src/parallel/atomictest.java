package parallel;

/**
 * test-driver for the <CODE>atomic</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class atomictest {

	/**
	 * invoke as:
	 * <CODE>java -cp &lt;classpath&gt; parallel.atomictest</CODE>.
	 * @param args 
	 */
  public static void main(String[] args) {
    T t1 = new T("a", "i");
    T t2 = new T("b", "j");

    t1.start();
    t2.start();
  }
	
	/**
	 * auxiliary inner-class not part of the public API.
	 */
	static class T extends Thread {
		String _s1, _s2 = null;

		public T(String s1, String s2) {
			_s1 = s1; _s2 = s2;
		}

		public void run() {

			for (int i=0; i<200; i++) {
				System.out.println(_s2+" "+i);
			}

			atomic.start(1);
			for (int i=200; i<250; i++) {
				System.out.println(_s1+" "+ i);
			}
			atomic.end(1);

			for (int i=250; i<270; i++) {
				System.out.println(_s2+" "+i);
			}

			atomic.start(2);
			for (int i=270; i<370; i++) {
				System.out.println(_s1+" "+i);
			}
			atomic.end(2);
		}
	}
}
