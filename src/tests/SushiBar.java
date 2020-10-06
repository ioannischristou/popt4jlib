package tests;

import parallel.Lock;
import java.util.Random;

/**
 * An implementation of the solution for the "Sushi Bar" problem in the
 * "Little Book of Semaphores" that uses Reek's pattern "Pass the Baton", which
 * basically has threads other than the one that acquire a lock to release it.
 * The simulation cheks whether the pattern is broken due to memory
 * visibility issues: semaphores and synchronization is used not only for 
 * coordination but for communication as well, which the "Pass the Baton" 
 * pattern fails to take into account, as it seems to be based on the implicit
 * assumption that all shared variable reads and writes are directly done from
 * the single main memory of the machine. However, running the program shows 
 * that the invariant (number-of-eating-customers &le; 5) never fails, so the 
 * algorithm seems to be working OK.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class SushiBar {
	static private final int _tableSize = 10;  // compile-time constant
	static private Lock _mutex = new Lock();
	
	static private Lock _block = new Lock();  // must start out as locked
	static private boolean _mustWait = false;
	static private long _waiting = 0;
	static private long _eating = 0;
	
	
	/**
	 * invoke as 
	 * <CODE>java -cp &lt;classpath&gt; tests.SushiBar</CODE>.
	 * The program doesn't seem to fail to maintain the invariant of the number of
	 * people eating (_eating) exceeding the table size (_tableSize).
	 * @param args 
	 */
	public static void main(String[] args) {
		Random r = new Random(3);
		ClientThread[] clients = new ClientThread[50];  // even 10000, no problem
		_block.getLock();  // start it out locked!
		for (int i=0; i<clients.length; i++) {
			clients[i] = new ClientThread(i);
		}
		for (int i=0; i<clients.length; i++) {
			clients[i].start();
			/*
			try {
				int sd = r.nextInt(1);
				Thread.sleep(sd);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			*/
		}
		for (int i=0; i<clients.length; i++) {
			try {
				clients[i].join();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("ALL Done.");
	}
	
	
	/**
	 * auxiliary inner class, not part of the public API.
	 */
	static class ClientThread extends Thread {
		private int _id;
		
		public ClientThread(int i) {
			_id = i;
		}
		
		/**
		 * each thread goes to the sushi-bar, eats, leaves, then comes back. Never
		 * finishes.
		 */
		public void run() {
			//boolean have_mutex = false;
			//boolean have_block = false;
			while (true) {
				//Random r = new Random(_id);
				//System.out.println("Customer "+_id+" has entered the building");
				_mutex.getLock();
				//have_mutex = true;
				if (_mustWait) {
					++_waiting;
					_mutex.releaseLock();
					//have_mutex = false;
					_block.getLock();
					//have_block = true;
					--_waiting;
				}

				++_eating;
				_mustWait = (_eating==_tableSize);
				if (_waiting>0 && !_mustWait) {
					_block.releaseLock();
					//if (!have_block) {
					//	System.err.println("Customer-"+_id+" released block w/o owning");
					//}
					//have_block = false;
				}
				else {
					_mutex.releaseLock();
					//if (!have_mutex) {
					//	System.err.println("Customer-"+_id+" released mutex w/o owning");
					//}
					//have_mutex = false;
				}

				// eat sushi
				/*
				System.out.println("Customer "+_id+" is eating delicious sushi with "+
													 (_eating-1)+" other customers; "+
													 _waiting+" others are waiting");
				try {
					Thread.sleep(r.nextInt(5));  // scrantch, scrontch...
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				*/

				if (_eating>_tableSize) {
					System.err.println("broken");
					System.exit(-1);
				}

				_mutex.getLock();
				//have_mutex = true;
				--_eating;
				if (_eating==0) _mustWait = false;

				if (_waiting>0 && !_mustWait) {
					_block.releaseLock();
					//if (!have_block) {
					//	System.err.println("Customer-"+_id+" released block w/o having");
					//}
					//have_block = false;
				}
				else {
					_mutex.releaseLock();
					//if (!have_mutex) {
					//	System.err.println("Customer-"+_id+" released mutex w/o having");
					//}
					//have_mutex = false;
				}
				//System.out.println("Customer "+_id+" has left the building");
			}
		}
	}
}
