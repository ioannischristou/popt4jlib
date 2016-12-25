package parallel;

/**
 * minimal test-driver to test the <CODE>DMCoordinator</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DMCTest {

	/**
	 * invoke as:
	 * <CODE>java -cp &lt;classpath&gt; parallel.DMCTest</CODE>.
	 * @param args 
	 */
  public static void main(String[] args) {
    int n = 10;
    for (int i=0; i<n; i++) {
      DMCThread di;
      if (i%3==0)
        di = new DMCThread(1);  // write
      else di = new DMCThread(0);  // read
      di.start();
    }
  }

	
	/**
	 * auxiliary static inner-class not part of the public API.
	 */
	static class DMCThread extends Thread {

		int _i;

		public DMCThread(int i) {
			_i = i;
		}


		public void run() {
			for (int i=0; i<10; i++)
				runAux();
		}


		public void runAux() {
			long start_time = System.currentTimeMillis();
			boolean done = false;
			while (!done) {
				done = (System.currentTimeMillis() - start_time)/1000.0 >= 10;  // run for 10 secs
				if (_i==0) {
					// reader
					DMCoordinator.getInstance().getReadAccess();
					try {
						Thread.currentThread().sleep(5);  // sleep
					} catch (InterruptedException e) {}
					System.out.println(
							"Thread id="+Thread.currentThread().toString()+
							" writers="+DMCoordinator.getInstance().getNumWriters()+
							" readers="+DMCoordinator.getInstance().getNumReaders());

					// try to upgrade to Write Access
					java.util.Random r = new java.util.Random();
					double rn=r.nextDouble();
					if (rn<0.45) {
						// try to gain Write Access
						try {
							System.out.println("Thread-id="+Thread.currentThread().toString()+" Upgrading read->write status");
							DMCoordinator.getInstance().getWriteAccess();
							System.out.println("Thread-id="+Thread.currentThread().toString()+" Upgraded read->write status");
							System.out.println(
									"Thread id="+Thread.currentThread().toString()+
									" writers="+DMCoordinator.getInstance().getNumWriters()+
									" readers="+DMCoordinator.getInstance().getNumReaders());
							Thread.currentThread().sleep(5);  // sleep
							DMCoordinator.getInstance().releaseWriteAccess();
							System.out.println("Thread-id="+Thread.currentThread().toString()+" back from write->read status");
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						catch (ParallelException e) {
							System.err.println("Thread-id="+Thread.currentThread().toString()+"Upgrade failed********************************************");
						}
					}
					else if (rn < 0.8) {
						try {
							// try to get read access again
							DMCoordinator.getInstance().getReadAccess();
							System.err.println("got read lock inside");
							DMCoordinator.getInstance().releaseReadAccess();
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
					try {
						DMCoordinator.getInstance().releaseReadAccess();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					Thread.currentThread().yield();
					try { Thread.currentThread().sleep(3); } // sleep a bit
					catch (InterruptedException e) {}
				}
				else {
					// writer
					try {
						DMCoordinator.getInstance().getWriteAccess();
						Thread.currentThread().sleep(10);  // sleep
					}
					catch (InterruptedException e) {}
					catch (ParallelException e) {
						System.err.println("Thread-id="+Thread.currentThread().toString()+"getWriteAccess failed********************************************");
					}
					System.out.println(
							"Thread id="+Thread.currentThread().toString()+
							"writers="+DMCoordinator.getInstance().getNumWriters()+
							" readers="+DMCoordinator.getInstance().getNumReaders());
					// try to get the read lock too
					try {
						// get Read Access too
						java.util.Random r = new java.util.Random();
						double rn=r.nextDouble();
						if (rn<0.4) {
							DMCoordinator.getInstance().getReadAccess();
							System.err.println("Thread-id=" + Thread.currentThread().toString() +
																 " got read access too");
							Thread.sleep(10);
							DMCoordinator.getInstance().releaseReadAccess();
						}
					}
					catch (Exception e) {
						e.printStackTrace();
						System.exit(-1);
					}
					try {
						DMCoordinator.getInstance().releaseWriteAccess();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					Thread.currentThread().yield();
				}
			}
		}
	}

}
