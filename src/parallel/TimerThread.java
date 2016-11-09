/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel;

/**
 * Utility class, similar (but simpler) to the ones found in concurrent utils.
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
	
	
	public TimerThread(long time) {
		this(time, true);
	}
	
	
	public TimerThread(long time, boolean cont) {
		_maxTimeAllowedMS = time;
		_cont = cont;
		setDaemon(true);  // exit when the main thread exits.
	}
	
	
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
	
	public synchronized boolean doContinue() {
		return _cont;
	}
	
}
