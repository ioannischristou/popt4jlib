package parallel;

import java.util.HashMap;


/**
 * class simulates the data access synchronization processes in AMORE (AIT
 * MOvie Recommendation Engine).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DMCTest2 {
  private long _lastAccess2FetchedUpdateTime = 0;
  private long _lastFetchedUpdateTime = 0;
  long _lastLoadingTime = 0;
  private HashMap _items = null;
  private DMCT2LThread _thread = null;

	/**
	 * sole constructor.
	 */
  public DMCTest2() {
  }


	/**
	 * the main method of the class.
	 * @throws ParallelException
	 * @throws InterruptedException 
	 */
  public void run() throws ParallelException, InterruptedException {
    while (true) {
      DMCoordinator.getInstance("DMCTest2").getReadAccess();
      doWork();
      DMCoordinator.getInstance("DMCTest2").releaseReadAccess();
      doWork2();
      load();
    }
  }


	/**
	 * invoke as:
	 * <CODE>java -cp &lt;classpath&gt; parallel.DMCTest2</CODE>.
	 * @param args 
	 */
  public static void main(String[] args) {
    DMCTest2 t = new DMCTest2();
    try {
      t.run();
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }


  void setLoaderThreadNull() { _thread = null; }
  void setItems(HashMap t) {
    _items = t;
    _lastLoadingTime = System.currentTimeMillis();
  }


  private void doWork() throws InterruptedException {
    System.out.println("doWork(): working...");
    Thread.sleep(10);
  }


  private void doWork2() throws InterruptedException, ParallelException {
    loadIfNeeded();
    DMCoordinator.getInstance("DMCTest2").getReadAccess();
    System.out.println("doWork2(): working...");
    Thread.sleep(100);
    DMCoordinator.getInstance("DMCTest2").releaseReadAccess();
  }


  private synchronized void load() {
    try {
      if (_thread == null) {
        _thread = new DMCT2LThread(this);
        _thread.start();
        _thread.join();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }


  private synchronized void loadIfNeeded() {
    try {
      if (mustLoad() && _thread == null) {
        _thread = new DMCT2LThread(this);
        _thread.start();
        if (_items == null)  // this is ugly, is it ok ?
          _thread.join();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }


  private boolean mustLoad() {
    long now = System.currentTimeMillis();
    if (now - _lastAccess2FetchedUpdateTime > 1000) {  // no check for more than 1 second
      _lastAccess2FetchedUpdateTime = now;
      _lastFetchedUpdateTime = now;
    }
    return (_items==null || _lastLoadingTime < _lastFetchedUpdateTime);
  }

	
	/**
	 * auxiliary inner-class not part of the public API.
	 */
	class DMCT2LThread extends Thread {
		private DMCTest2 _testObj;

		public DMCT2LThread(DMCTest2 t) {
			_testObj = t;
		}

		public void run() {
			try {
				DMCoordinator.getInstance("DMCTest2").getWriteAccess();
				System.out.print("DMCT2LThread.run(): creating items...");
				Thread.sleep(1000*utils.RndUtil.getInstance().getRandom().nextInt(10));  // sleep for at most 9 seconds
				System.out.println("Done!");
				_testObj.setItems(new HashMap());
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			finally {
				try {
					_testObj.setLoaderThreadNull();
					DMCoordinator.getInstance("DMCTest2").releaseWriteAccess();
				}
				catch (ParallelException e) {
					e.printStackTrace();  // can never get here
				}
			}
		}
	}

}

