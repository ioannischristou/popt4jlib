/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel;

/**
 * Utility class, similar (but simpler) to the ones found in concurrent utils.
 * The class is useful if one wishes to poll whether enough time has passed 
 * but without forcing busy-waiting kind of polling or query system time calls
 * on their basic thread of computation. See <CODE>graph.GRASPPacker</CODE>
 * for an example of use.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class TimerThread extends Thread {
	private long _maxTimeAllowedMS;  // in milliseconds
	private boolean _cont;
	

	/**
	 * constructor specifies the amount of time in milliseconds for which, after 
	 * this thread has started, the <CODE>doContinue()</CODE> method will return 
	 * true. After that interval of time elapses, the thread will exit, and its
	 * <CODE>doContinue()</CODE> method will return false.
	 * @param time long (representing milliseconds)
	 */
	public TimerThread(long time) {
		this(time, true);
	}
	
	
	/**
	 * same as 1-arg constructor, except if the second argument is false, the 
	 * <CODE>doContinue()</CODE> method will always return false.
	 * @param time
	 * @param cont 
	 */
	public TimerThread(long time, boolean cont) {
		_maxTimeAllowedMS = time;
		_cont = cont;
		setDaemon(true);  // exit when the main thread exits.
	}
	
	
	/**
	 * the main method of the thread.
	 */
	public void run() {
		long start = System.currentTimeMillis();
		while (_cont) {
			long now = System.currentTimeMillis();
			if (now-start >= _maxTimeAllowedMS) {
				synchronized (this) {
					_cont = false;
				}
				return;
			}
			// sleep for a while
			try {
			Thread.currentThread().sleep(100);  // sleep for 1/10 of a second
			}
			catch (InterruptedException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
		}
	}
	
	
	/**
	 * query whether enough time has passed after the thread was started.
	 * @return boolean
	 */
	public synchronized boolean doContinue() {
		return _cont;
	}
	
}
