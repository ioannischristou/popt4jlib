package parallel.distributed;

import parallel.*;
import java.net.*;
import java.io.*;

/**
 * class implements a distributed barrier object across many threads living
 * in many (remote) JVMs. The following constraint is imposed on DBarrier
 * objects use: the <CODE>barrier()</CODE> method can only be called from the
 * same thread that created this DBarrier object (otherwise an exception is
 * thrown).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DBarrier {
  private String _host = "localhost";
  private int _port = 7896;
  private String _coordname = "DBarrierCoord_"+_host+"_"+_port;  // coordname 
	                                                               // and barrier 
	                                                               // name are the 
	                                                               // same
  private DActiveMsgPassingCoordinatorLongLivedConnClt _coordclt=null;
  private Thread _originatingThread = null;


  /**
   * no-arg constructor assumes the following defaults:
	 * <ul>
   * <li> host="localhost"
   * <li> port=7896
   * <li> barrier/coord name = "DBarrierCoord_localhost_7896"
	 * </ul>.
   * The constructor will actually register the current thread with the barrier
   * object of the server, so later invocations of the <CODE>barrier()</CODE>
   * method of this object will synchronize the current thread with all other
   * threads in all JVMs having constructed DBarrier objects connected to the
   * default server, default port, and default barrier name.
   * @throws UnknownHostException
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws ParallelException
   */
  public DBarrier() throws UnknownHostException, IOException, 
		                       ClassNotFoundException, ParallelException {
    _coordclt = 
			new DActiveMsgPassingCoordinatorLongLivedConnClt(_host, _port, 
				                                               _coordname);
    _originatingThread = Thread.currentThread();
    DBarrierAddRequest addreq = new DBarrierAddRequest(_coordname);
    _coordclt.sendData(-1, addreq);
  }


  /**
   * constructor adds current thread to the barrier object of the server found
   * in IP address specified by first two argument parameters with the name
   * specified in the third parameter, so that later invocations of the
   * <CODE>barrier()</CODE> method of this object will synchronize the current
   * thread with all other threads in all JVMs having constructed DBarrier
   * objects connected to the same server-port, and same barrier name.
   * @param host String
   * @param port int
   * @param coordname String
   * @throws UnknownHostException
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws ParallelException
   */
  public DBarrier(String host, int port, String coordname)
      throws UnknownHostException, IOException, ClassNotFoundException,
             ParallelException {
    // 0. initialization
    _host = host;
    _port = port;
    _coordname = coordname;
    _originatingThread = Thread.currentThread();
    _coordclt = 
			new DActiveMsgPassingCoordinatorLongLivedConnClt(_host, _port, 
				                                               _coordname);
    // 1. send a Barrier Add Thread request: when the method terminates, done.
    DBarrierAddRequest addreq = new DBarrierAddRequest(_coordname);
    _coordclt.sendData(-1, addreq);
  }


  /**
   * the main class method. Blocks until all other threads (from same and/or
   * remote JVMs) that have registered with the same coordname as this one
   * (in object construction time) at the same host/port address, have also
   * called this method (on their respective DBarrier objects). The method can
   * be called multiple times, even after some other threads in same or remote
   * JVM have called <CODE>removeCurrentThread()</CODE>.
   * @throws IOException if a network error occurs
   * @throws ClassNotFoundException if the server JVM is not in synch with
   * this JVM's classes.
   * @throws ParallelException if this DBarrier object was constructed by a
   * different thread than the one calling this method.
   */
  public void barrier() throws IOException, ClassNotFoundException, 
		                           ParallelException {
    // 0. check if thread is ok
    if (Thread.currentThread()!=_originatingThread) {
      throw new ParallelException("barrier(): method called from thread "+
				                          "different than one originating the object");
    }
    // 1. send a Barrier request: when the method terminates, we're done
    _coordclt.sendData(-1, new DBarrierRequest(_coordname));  // barrier and 
		                                                          // coord names are 
		                                                          // the same
  }


  /**
   * removes the current thread from this DBarrier object, and closes the
   * connection.
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws ParallelException if calling thread is not the same as creation
   * thread.
   */
  public void removeCurrentThread() throws IOException, ClassNotFoundException, 
		                                       ParallelException {
    // 0. check if thread is ok
    if (Thread.currentThread()!=_originatingThread) {
      throw new ParallelException("removeCurrentThread(): method called from "+
				                          "thread different than the one originating "+
				                          "the object");
    }
    // 1. send a DBarrierRmRequest request: when the method terminates, we're ok
    _coordclt.sendData(-1, new DBarrierRmRequest(_coordname));  // barrier and 
		                                                            // coord names 
		                                                            // are the same
    // 2. finally, close the connection.
    _coordclt.closeConnection();
  }

}

