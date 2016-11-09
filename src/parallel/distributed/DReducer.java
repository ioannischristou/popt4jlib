package parallel.distributed;

import parallel.*;
import java.net.*;
import java.io.*;

/**
 * class implements a distributed reducer object across many threads living
 * in many (remote) JVMs. The following constraint is imposed on DReducer
 * objects use: the <CODE>reduce(data,op)</CODE> method can only be called from the
 * same thread that created this DReducer object (otherwise an exception is
 * thrown).
 * Notice that this implementation is modeled after the DBarrier class in this
 * package.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DReducer {
  private String _host = "localhost";
  private int _port = 7901;
  private String _coordname = "DReduceCoord_"+_host+"_"+_port;  // coordname and reducer name are the same
  private DActiveMsgPassingCoordinatorLongLivedConnClt _coordclt=null;
  private Thread _originatingThread = null;


  /**
   * no-arg constructor assumes the following defaults:
	 * <ul>
   * <li> host="localhost"
   * <li> port=7901
   * <li> reducer/coord name = "DReduceCoord_localhost_7901"
	 * </ul>
   * The constructor will actually register the current thread with the reducer
   * object of the server, so that later invocations of the <CODE>reduce(.,.)</CODE>
   * method of this object will synchronize the current thread with all other
   * threads in all JVMs having constructed DReducer objects connected to the
   * default server, default port, and default reducer name.
   * @throws UnknownHostException
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws ParallelException
   */
  public DReducer() throws UnknownHostException, IOException, ClassNotFoundException, ParallelException {
    _coordclt = new DActiveMsgPassingCoordinatorLongLivedConnClt(_host, _port, _coordname);
    _originatingThread = Thread.currentThread();
    DReduceAddRequest addreq = new DReduceAddRequest(_coordname);
    _coordclt.sendData(-1, addreq);
  }


  /**
   * constructor adds current thread to the reducer object of the server found
   * in IP address specified by first two argument parameters with the name
   * specified in the third parameter, so that later invocations of the
   * <CODE>reduce(.,.)</CODE> method of this object will synchronize the current
   * thread with all other threads in all JVMs having constructed DReducer
   * objects connected to the same server-port, and same reducer name.
   * @param host String
   * @param port int
   * @param coordname String
   * @throws UnknownHostException
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws ParallelException
   */
  public DReducer(String host, int port, String coordname)
      throws UnknownHostException, IOException, ClassNotFoundException,
             ParallelException {
    // 0. initialization
    _host = host;
    _port = port;
    _coordname = coordname;
    _originatingThread = Thread.currentThread();
    _coordclt = new DActiveMsgPassingCoordinatorLongLivedConnClt(_host, _port, _coordname);
    // 1. send a Reduce Add Thread request: when the method terminates, done.
    DReduceAddRequest addreq = new DReduceAddRequest(_coordname);
    _coordclt.sendData(-1, addreq);
  }


  /**
   * the main class method. Blocks until all other threads (from same and/or
   * remote JVMs) that have registered with the same coordname as this one
   * (in object construction time) at the same host/port address, have also
   * called this method (on their respective DReducer objects). The method can
   * be called multiple times, even after some other threads in same or remote
   * JVM have called <CODE>removeCurrentThread()</CODE>. The result of the 
	 * reduce operation is returned.
	 * @param data Serializable
	 * @param op ReduceOperator
	 * @return Object // Serializable
   * @throws IOException if a network error occurs
   * @throws ClassNotFoundException if the server JVM is not in synch with
   * this JVM's classes.
   * @throws ParallelException if this DReducer object was constructed by a
   * different thread than the one calling this method.
   */
  public Object reduce(Serializable data, ReduceOperator op) throws IOException, ClassNotFoundException, ParallelException {
    // 0. check if thread is ok
    if (Thread.currentThread()!=_originatingThread) {
      throw new ParallelException("reduce(data,op): method called from thread different than the one originating the object");
    }
    // 1. send a reduce request: when the method terminates, we're done
    Object result = _coordclt.sendAndRecvData(-1, new DReduceRequest(_coordname, data, op));  // reducer and coord names are the same
		return result;
  }


  /**
   * removes the current thread from this DReducer object, and closes the
   * connection.
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws ParallelException if calling thread is not the same as creation
   * thread.
   */
  public void removeCurrentThread() throws IOException, ClassNotFoundException, ParallelException {
    // 0. check if thread is ok
    if (Thread.currentThread()!=_originatingThread) {
      throw new ParallelException("removeCurrentThread(): method called from thread different than the one originating the object");
    }
    // 1. send a DReducerRmRequest request: when the method terminates, we're ok
    _coordclt.sendData(-1, new DReduceRmRequest(_coordname));  // Reducer and coord names are the same
    // 2. finally, close the connection.
    _coordclt.closeConnection();
  }

}

