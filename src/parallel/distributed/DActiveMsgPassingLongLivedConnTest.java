package parallel.distributed;

import java.io.*;

/**
 * test for the DActiveMsgPassingCoordinatorLongLivedConn[Clt/Srv] classes.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DActiveMsgPassingLongLivedConnTest {

  private DActiveMsgPassingLongLivedConnTest() {
  }


  /**
   * invoke as
   * <CODE>java -cp &lt;classpath&gt; parallel.distributed.DMsgPassingLongLivedConnTest &lt;startid&gt; &lt;ndid&gt; [delay(-1)]</CODE>.
   * The test is set up so that the
   * <CODE>parallel.distributed.DActiveMsgPassingCoordinatorLongLivedConnSrv</CODE> must be up and
   * running at the default (host=localhost,port=7895) combination, and there
   * must be exactly two processes, one started with (0,9) args and the other
   * with (10,19) aguments.
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      int startid = Integer.parseInt(args[0]);
      int endid = Integer.parseInt(args[1]);
      long delay = -1;
      if (args.length>=3) delay = Long.parseLong(args[2]);
      DActiveMsgPassingCoordinatorLongLivedConnClt coordclt = new DActiveMsgPassingCoordinatorLongLivedConnClt();  // use localhost as server
      for (int i = startid; i <= endid; i++) {
        DMPThread2 ti = new DMPThread2(i);
        if (delay>0) Thread.sleep(delay);  // enter a delay
        ti.start();
      }
      if (startid==0) {
        coordclt.sendData( -1, 0, new DActiveMsgPassingLLCTestSerObject2()); // bootstrap the recv->send cycle
        System.out.println("Thread--1(main) sent data to TId=0");
        System.out.flush();
      }
    }
    catch (Exception e) { e.printStackTrace(); }
  }

	
	/**
	 * auxiliary inner class, not part of the public API.
	 */
	static class DMPThread2 extends Thread {
		private DActiveMsgPassingCoordinatorLongLivedConnClt _coordclt;
		private int _id;


		public DMPThread2(int i) throws IOException {
			_id = i;
			_coordclt = new DActiveMsgPassingCoordinatorLongLivedConnClt();  // use localhost as server
		}


		public void run() {
			try {
				int sendTo = _id + 1;
				if (sendTo >= 20) sendTo = 0; // there will be two JVMs running DMsgPassingTest
				// the first will start with args 0 9 and the second will start with args
				// 10 19
				for (int i = 0; i < 100; i++) {
					System.out.println("Thread-" + _id + " doing iter " + i +
														 " waiting to recv any data");
					boolean done=false;
					while (!done) {
						try {
							_coordclt.recvData(_id);
							done = true;
						}
						catch (Exception e) {
							e.printStackTrace();
							Thread.sleep(100);
						}
					}
					System.out.println("Thread-" + _id + " doing iter " + i +
										 " sending data to TId=" + sendTo);
					done=false;
					while (!done) {
						try {
							_coordclt.sendData(_id, sendTo, new DActiveMsgPassingLLCTestSerObject2());
							done=true;
						}
						catch (Exception e) {
							e.printStackTrace();
							Thread.sleep(100);
						}
					}
				}
				if (_id==0) _coordclt.recvData(_id);  // consume the last datum sent
				_coordclt.closeConnection();  // done!
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	
	/**
	 * auxiliary trivial inner class, not part of the public API.
	 */
	static class DActiveMsgPassingLLCTestSerObject2 implements Serializable {
		private static final long serialVersionUID = 6054062999479720730L;
	}

}



