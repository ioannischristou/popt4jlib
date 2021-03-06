package parallel;

import java.util.Vector;

/**
 * A class that allows its users to execute tasks (object that describe some
 * computation that must implement the TaskObject interface) for a limited time,
 * starting them in different threads than the thread that called the
 * execute(task) method. Thread-pooling is implemented so that if a thread
 * previously created is available, it is re-used rather than starting a new
 * thread for each call of the execute(task) method. Long-running tasks are
 * given lower and lower priority (but cannot be abruptly ended as the destroy()
 * family of thread methods has been deprecated since jdk 1.2.)
 * The class provides a useful tool for limiting function evaluations in the
 * popt4jlib library; see the source and documentation of the classes
 * popt4jlib.LimitedTimeEvalFunction and popt4jlib.FunctionEvaluationTask
 * The tasks must of course be thread-safe, and must not be manipulated in any
 * way from another thread after they have been submitted to the
 * LimitedTimeTaskExecutor class for execution prior to finishing execution. The
 * class itself is also thread-safe (per-se).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LimitedTimeTaskExecutor {
  private long _msecs2wait;  // milliseconds to wait for before proceeding
  private Vector _threads;  // Vector<LTEThread t>

  /**
   * constructor for the LimitedTimeTaskExecutor class.
   * @param maxresponsetime2wait long the maximum time allowed to pass for the
   * task.run() method of any TaskObject object to finish (in milliseconds).
   */
  public LimitedTimeTaskExecutor(long maxresponsetime2wait) {
    _msecs2wait = maxresponsetime2wait;
    _threads = new Vector();
  }


  /**
   * return the total number of threads created by this LimitedTimeTaskExecutor.
   * @return int
   */
  public synchronized int getNumThreads() { return _threads.size(); }


  /**
   * executes the TaskObject argument in a different thread, and waits for
   * _msecs2wait milliseconds for the run() method of the task object to
   * finish. If it does, then it returns true, otherwise it returns false.
   * The TaskObject task must be such so that its method isDone() does not
   * return true until the computation of run() has been completed.
   * @param task TaskObject
   * @return boolean
   */
  public boolean execute(TaskObject task) {
    synchronized (this) {
      final int n = _threads.size();
      ExecutorThread t = null;
      for (int i = 0; i < n && t == null; i++) {
        ExecutorThread ti = (ExecutorThread) _threads.elementAt(i);
        if (ti.isAvailable()) t = ti;
      }
      if (t == null) {
        // update thread priorities
        for (int i = 0; i < n; i++) {
          ExecutorThread ti = (ExecutorThread) _threads.elementAt(i);
          ti.updatePriority(_msecs2wait);
        }
        t = new ExecutorThread();
        t.start();
        _threads.addElement(t); // add the new thread in the vector of threads
      }
      // now we have a Thread that is free
      t.runTask(task); // give the task to the thread t to execute.
    }
    try {
      synchronized (task) {
        long startwait = System.currentTimeMillis();
        long endwait = startwait + _msecs2wait;
        long remaining = _msecs2wait;
        if (task.isDone()==false) {  // is task is done already, no need to wait
          while (System.currentTimeMillis()<endwait && task.isDone()==false) {
            task.wait(remaining); // wait up to _msecs2wait, then return
            remaining = endwait - System.currentTimeMillis();
            // while-loop above protects against "spurious waits", but renders
            // wait-times etc. possibly inaccurate, although it is guaranteed
            // that the thread will wait for at least the _msecs2wait specified
            // interval amount before returning.
          }
        }
        long dur = System.currentTimeMillis()-startwait;
        if (dur<_msecs2wait)
          return true;
        else return false;
      }
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

	
	/**
	 * auxiliary inner-class, not part of the public API.
	 */
	class ExecutorThread extends Thread {
		private SingleQueue _q;
		private boolean _isRunning;
		private long _currentTaskStartTime;
		private long _lastFactor=0;

		public ExecutorThread() {
			setDaemon(true);  // all threads are deamon and will end when the main
												// thread ends
			_q = new SingleQueue();
			_isRunning=false;
			// _lte = lte;
		}
		public void run() {
			while (true) {
				TaskObject o = _q.getTaskObject();
				setIsRunning(true);
				try {
					o.run();
				}
				catch (Exception e) {  // TaskObject threw exception: ignore & continue
					e.printStackTrace();  // no-op
				}
				setIsRunning(false);
				synchronized (o) {
					o.notifyAll();
				}
			}
		}
		synchronized void updatePriority(long msecs) {
			if (msecs<=0) return;
			if (_currentTaskStartTime>0) {
				long now = System.currentTimeMillis();
				long div = (now-_currentTaskStartTime)/msecs;
				if (div>_lastFactor) {
					_lastFactor = div;
					int prior = getPriority()-1;
					if (prior > Thread.MIN_PRIORITY) 
						setPriority(prior);  // lower priority
				}
			}
			else _lastFactor = 0;
		}

		protected synchronized boolean isAvailable() {
			boolean res =  !_isRunning && _q.isEmpty();
			return res;
		}
		protected void runTask(TaskObject to) {
			_q.putTaskObject(to);
		}

		private synchronized void setIsRunning(boolean v) {
			_isRunning = v;
			if (v) _currentTaskStartTime = System.currentTimeMillis();
			else _currentTaskStartTime=-1;
			_lastFactor = 0;
			setPriority(Thread.NORM_PRIORITY);  // reset thread priority
		}
	}


	/**
	 * auxiliary inner-class, not part of the public API.
	 */
	class SingleQueue {
		private TaskObject _taskObj;

		SingleQueue() {
			// no-op
		}
		synchronized TaskObject getTaskObject() {
			while (_taskObj==null) {
				try {
					wait();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			TaskObject res = _taskObj;
			_taskObj=null;
			notifyAll();
			return res;
		}
		synchronized void putTaskObject(TaskObject to) {
			while (_taskObj!=null) {
				try {
					wait();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			_taskObj = to;
			notifyAll();
		}
		synchronized boolean isEmpty() { return _taskObj==null; }
	}
	
}
