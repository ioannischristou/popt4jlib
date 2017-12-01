/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tests;

/**
 * tests whether an object constructed in one thread is visible (as valid ref)
 * to another if synchronization only happens within the object's methods.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ThreadAccessTest {
	static Helper _h;
	public static void main(String[] args) {
		TestThread t1 = new TestThread(0);
		TestThread t2 = new TestThread(1);
		t1.start();
		t2.start();
		try {
			t1.join();
			t2.join();
		}
		catch (Exception e) { e.printStackTrace(); }
		System.err.println("T1 output:"+t1._out);
		System.err.println("T2 output:"+t2._out);
		System.err.println("final output:"+_h._x);
	}

	static class TestThread extends Thread {
		private long _st;
		private  String _out = "";
		private long _id;  // needed for jdk 1.4
		private static long _nextId=0;  // for jdk1.4

		public TestThread(long sleepTime) {
			_st = sleepTime;
			synchronized (TestThread.class) {
				_id = ++_nextId;
			}
		}

		
    public long getId() {
      return _id;
    }

		
		public void run() {
			try {
				Thread.sleep(_st);
				if (_h==null) {
					_out += "Thread-"+getId()+" sees null _h\n";
					_h = new Helper((int)(100000*(1-_st)));
					_out += "Thread-"+getId()+" set _h._x to "+_st+"\n";
				}
				else {
					_out += "Thread-"+getId()+" sees _h._x="+_h.getX()+"\n";
				}
			}
			catch (Exception e) { e.printStackTrace(); }
		}
	}

	static class Helper {
		private int _x;
		private double[] _arr;
		public Helper(int x) {
			_x = x;
			_arr = new double[_x];
		}
		public synchronized int getX() { return _x; }
	}
}
