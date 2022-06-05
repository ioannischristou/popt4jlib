package tests;

/**
 * test of thread data modifications made visible to other threads under no 
 * synchronization.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ThreadVisibilityTest {
	private int[] _x;
	
	public ThreadVisibilityTest() {
		_x = new int[100000];
	}
	
	// unsynchronized
	public int getX() { return _x[_x.length-1]; }
	public void setX(int x) { 
		for (int i=0; i<_x.length; i++) _x[i] = x; 
	}
	
	public static void main(String[] args) {
		final long init_delay = 100;  // msecs
		final long tot_delay = 10000;  // msecs
		int nt = 2;
		if (args.length>0) nt = Integer.parseInt(args[0]);
		
		Thread[] tts = new Thread[nt];
		
		ThreadVisibilityTest t = new ThreadVisibilityTest();
		
		for (int i=0; i<tts.length; i++) {
			tts[i] = new TT(i+1, t, (i+1)*init_delay, tot_delay);
		}
		
		for (int i=0; i<tts.length; i++) {
			tts[i].start();
		}
		
		for (int i=0; i<tts.length; i++) {
			try {
				tts[i].join();
				System.out.println(((TT)tts[i])._out);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("x[last]="+t._x[t._x.length-1]);
		
	}
	
	
}

class TT extends Thread {
	private ThreadVisibilityTest _t;
	private int _id;
	private long _iDel;
	private long _tDel;
	String _out;
	
	public TT(int id, ThreadVisibilityTest t, long iDel, long tDel) {
		_t = t;
		_id = id;
		_iDel = iDel;
		_tDel = tDel;
		_out = "";
	}
	
	public void run() {
		final String prefix = "[TT-"+_id+"]: ";
		final long due_time = System.currentTimeMillis()+_tDel;
		try {
			//_out += prefix + "time="+System.currentTimeMillis()+" starting to sleep."+
			//	      "\n";
			Thread.sleep(_iDel);
			if (_t.getX()==0) {
				//_out += prefix + "time="+System.currentTimeMillis()+" sees zero x."+"\n";
				_t.setX(_id);
			}
			else {
				//_out += prefix + "time="+System.currentTimeMillis()+" sees non-zero x."+
				//	      "\n";
			}
			long rem_delay = due_time - System.currentTimeMillis();
			Thread.sleep(rem_delay);
			//_out += prefix+"time="+System.currentTimeMillis()+" end\n";
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}