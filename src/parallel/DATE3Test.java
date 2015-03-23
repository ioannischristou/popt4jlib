/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import popt4jlib.IdentifiableIntf;

/**
 * class exercises <CODE>DynamicAsynchTaskExecutor3</CODE> functionality by
 * utilizing the <CODE>Fib</CODE> task: computing recursively in parallel the
 * Fibonacci sequence using essentially a Fork-Join pattern (originally proposed
 * for java by Doug Lea in his util.concurrent java package, based on the Cilk
 * framework.)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DATE3Test {
	private static DynamicAsynchTaskExecutor3 _extor;
	private static int _initN;
	
	/**
	 * invoke with params: [n(50)] [num_threads(8)] [seq_threshold(20)]
	 * @param args 
	 */
	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		int n = 50;
		int num_threads = 2;
		int threshold = 20;
		if (args.length>0) n = Integer.parseInt(args[0]);
		if (args.length>1) num_threads = Integer.parseInt(args[1]);
		if (args.length>2) threshold = Integer.parseInt(args[2]);
		Fib._threshold = threshold;
		_initN = n;
		Fib fn = new Fib(n);
		try {
			_extor = DynamicAsynchTaskExecutor3.newDynamicAsynchTaskExecutor3(2, num_threads);  
			_extor.execute(fn);
			while (!fn.isDone()) {
				try {
					Thread.sleep(10);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
					Thread.currentThread().interrupt();
				}
			}
			_extor.shutDownAndWait4Threads();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		long dur = System.currentTimeMillis()-start;
		System.out.println("result="+fn._result+" in "+dur+" msecs.");
	}
	
	
  static class Fib implements Runnable {
    private static int _threshold = 20;
    private boolean _isDone;  // false
    protected int _threadid = Integer.MAX_VALUE;  // MAX_VALUE specifies "N/A". 
    // Any other number specifies the thread-id in which this object needs to be
    // run.
    protected int _n;  // zero
    protected volatile long _result = -1;
    protected Fib _fnm1;  // null
    protected Fib _fnm2;  // null
    protected volatile long _exiters = 0;  // debugging
    
    public Fib(int n) {
			_n = n;
    }
    public void setPData(int n, String path) {
			_n = n;
      _isDone = false;
      _fnm1 = null;
      _fnm2 = null;
      _result = -1;
      _exiters = 0;
      _threadid = Integer.MAX_VALUE;
    }
    
    public synchronized boolean isDone() {
      return _isDone;
    }
    public int getThreadIdToRunOn() {
      return _threadid;
    }
    public void run() {
			/*
      ++_exiters;
			if (_exiters>=1000000) {  // too much, print and reset
        System.err.println("large _exiters: _n="+_n+" data queue.size()="+_extor.getNumTasksInQueue()+" hotlocal="+_extor.hotLocalQueueSize()+
								" coldlocal="+_extor.coldLocalQueueSize());
				_exiters=0;  // reset
      }
			*/
      if (_fnm1!=null) {  // has been submitted before
        if (_fnm1.isDone() && _fnm2.isDone()) {
          _result = _fnm1._result + _fnm2._result;
          synchronized (this) {
            _isDone = true;
            //notify();
          }
          return;
        } else {  
					_extor.resubmitToSameThread(this);  // go to cold queue
					return;
        }
      }
      if (_n<=_threshold) {
        _result = seqFib(_n);
        synchronized (this) {
					_isDone = true;
          //notify();
        }
        return;
      } else {
        try {
					final int mythreadid = (int)((IdentifiableIntf)Thread.currentThread()).getId();
					if (_initN-(_n-1) < 8) {
		        _fnm1 = new Fib(_n-1);
						_extor.execute(_fnm1);
					}
					else {  // execute locally
						if (_n % 2 == 0) {
							_fnm1 = new TSFib(_n-1);
							_fnm1._threadid = mythreadid;							
							_extor.submitToSameThread((ThreadSpecificTaskObject) _fnm1);
						} else {  // go global
							_fnm1 = new Fib(_n-1);
							_extor.execute(_fnm1);							
						}
					}
					if (_initN-(_n-2) < 8) {
		        _fnm2 = new Fib(_n-2);
						_extor.execute(_fnm2);
					}
					else {  // execute locally
						if (_n % 2 == 0) {  // go to hot-local
							_fnm2 = new TSFib(_n-2);
							_fnm2._threadid = mythreadid;
							_extor.submitToSameThread((ThreadSpecificTaskObject) _fnm2);
						} else {  // go global
							_fnm2 = new Fib(_n-2);
							_extor.execute(_fnm2);
						}
					}
					// finally-finally, re-submit me to cold queue!
					_extor.resubmitToSameThread(this);
        }
        catch (Exception e) {
          e.printStackTrace();
          System.exit(-1);
        }
        return;
      }
    }
    public String toString() {
      String fib="Fib[";
      fib += "_n="+_n+",_result="+_result+",_isDone="+_isDone;
      fib += _fnm1!=null ? ", _fnm1._isDone="+_fnm1.isDone() : "";
      fib += _fnm2!=null ? ", _fnm2._isDone="+_fnm2.isDone() : "";
      fib += ",_exiters="+_exiters+"]";
      return fib;
    }
    
    
    private static long seqFib(int n) {
      if (n<=1) return n;
      else {
        long res = seqFib(n-1)+seqFib(n-2);
        return res;
      }
    }
        
	}
	
	static class TSFib extends Fib implements ThreadSpecificTaskObject {
		TSFib(Fib f) {
			super(f._n);
			_threadid = f._threadid;
			_exiters = f._exiters;
			_fnm1 = f._fnm1;
			_fnm2 = f._fnm2;
			_result = f._result;
		}
		
		TSFib(int n) {
			super(n);
		}
	}

}

