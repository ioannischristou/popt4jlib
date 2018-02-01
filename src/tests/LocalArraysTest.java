/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tests;

import parallel.*;

/**
 * test-driver for the concept used in <CODE>popt4jlib.DE.DDE2</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LocalArraysTest {
	double [] _arr;
	
	public LocalArraysTest(int n) {
		_arr = new double[n];
	}
	
	public static void main(String[] args) {
		int n = args.length > 0 ? Integer.parseInt(args[0]) : 8000;
		LocalArraysTest lat = new LocalArraysTest(n);
		//for (int i=0; i<n; i++) lat._arr[i] = -1.0;

		int nt = args.length > 1 ? Integer.parseInt(args[1]) : 8;
		try {
			Barrier.setNumThreads(nt);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		int numtries = 1000;
		int work = n/nt;
		Thread[] ts = new TestThread[nt];
		int from = 0;
		int to = work-1;
		for (int i=0; i<nt-1; i++) {
			ts[i] = new TestThread(lat, numtries, from, to);
			from = to+1;
			to += work;
		}
		ts[nt-1] = new TestThread(lat, numtries, from, n-1);
		for (int i=0; i<nt; i++) {
			ts[i].start();
		}
		for (int i=0; i<nt; i++) {
			try {
				ts[i].join();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("done.");
		
	}
	
}


class TestThread extends Thread {
	int _numTries;
	LocalArraysTest _master;
	int _from;
	int _to;
	
	public TestThread(LocalArraysTest master, int numtries, int from, int to) {
		_master = master;
		_numTries = numtries;
		_from = from;
		_to = to;
	}
	
	public void run() {
		Barrier b = Barrier.getInstance();
		double[] copy_arr = new double[_master._arr.length];
		// init
		for (int i=_from; i<=_to; i++) {
			_master._arr[i] = -1.0;
			copy_arr[i] = -1.0;
		}
		b.barrier();
		for (int i=0; i<copy_arr.length; i++) {
			copy_arr[i] = _master._arr[i];
			if (Double.compare(copy_arr[i],-1.0)!=0) {
				System.err.println("init error");
				System.exit(-1);
			}
		}
		for (int i=0; i<_numTries; i++) {
			// work on your part, on the local copy of the array
			for (int j=_from; j<=_to; j++) {
				copy_arr[j] = (double)i;
			}
			b.barrier();
			// write your part to the master
			synchronized (_master) {
				for (int j=_from; j<=_to; j++) {
					_master._arr[j] = copy_arr[j];
				}
			}
			b.barrier();
			// read everyone's updates
			synchronized (_master) {
				for (int j=0; j<_master._arr.length; j++) {
					copy_arr[j] = _master._arr[j];
					if (Double.compare(copy_arr[j], (double)i)!=0) {
						System.err.println("Iteration="+i+" copy_arr["+j+"]="+copy_arr[j]);
						System.exit(-1);
					}
				}
				System.err.println("Thread-["+_from+","+_to+"] OK");
			}
			
		}
	}
}