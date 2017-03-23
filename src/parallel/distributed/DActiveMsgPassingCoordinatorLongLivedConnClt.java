package parallel.distributed;

import parallel.*;
import java.io.*;
import java.net.*;

/**
 * class implementing clients for message-passing between threads living in
 * distributed JVMs. A thread wishing to send or receive a message to another
 * thread (in the same or different JVM), invokes the sendData() or recvData()
 * methods of this class which behave exactly as the shared-memory analogue in
 * class <CODE>parallel.MsgPassingCoordinator</CODE>. This class maintains one
 * socket per object, so multiple threads requiring simultaneous access to the
 * server should create their own client objects to avoid having to lock
 * waiting for others to finish (all methods in this class are synchronized).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DActiveMsgPassingCoordinatorLongLivedConnClt {
  private String _host="localhost";
  private int _port = 7895;
  private String _coordName="DMsgPassingCoordinator_"+_port;
  private Socket _s=null;
  ObjectOutputStream _oos = null;
  ObjectInputStream _ois = null;


  /**
   * default constructor assumes the coordinator server lives in the same host
   * (localhost) listening in port 7895, and the name of the coordinator is
   * "DMsgPassingCoordinator_7895".
   * @throws UnknownHostException
   * @throws IOException
   */
  public DActiveMsgPassingCoordinatorLongLivedConnClt() throws UnknownHostException, IOException {
    InetAddress ia = InetAddress.getLocalHost();
    _host = ia.getHostAddress();
    _s = new Socket(_host, _port);
    _oos = new ObjectOutputStream(_s.getOutputStream());
    _oos.flush();
    _ois = new ObjectInputStream(_s.getInputStream());
  }


  /**
   * constructor specifies the host and port where the msg passing coordinator
   * server lives, and also the name of the coordinator with whom all sends and
   * receives will coordinate.
   * @param host String
   * @param port int
   * @param coordName String name of the coordinator instance in the server,
   * to be used by threads sending and receiving msgs. Notice that it's possible
   * for the same server to coordinate many different threads and processes via
   * different names.
   * @throws UnknownHostException
   * @throws IOException
   */
  public DActiveMsgPassingCoordinatorLongLivedConnClt(String host, int port,
                                                String coordName) throws UnknownHostException, IOException {
    _host = InetAddress.getByName(host).getHostAddress();
    _port = port;
    _coordName = coordName;
    _s = new Socket(_host, _port);
    _oos = new ObjectOutputStream(_s.getOutputStream());
    _oos.flush();
    _ois = new ObjectInputStream(_s.getInputStream());
  }


  /**
   * works exactly as the corresponding method with same signature in
   * <CODE>parallel.MsgPassingCoordinator</CODE> class, except that the data
   * must be serializable. If the data is an instance of DMsgIntf, the data
   * itself is transmitted, rather than a DSendMsg object.
   * @param myid int
   * @param toid int
   * @param data Serializable
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws ParallelException
   */
  public synchronized void sendData(int myid, int toid, Serializable data) throws IOException, ClassNotFoundException, ParallelException {
    /*
    if (_s==null || _s.isClosed()) {
      if (_oos!=null) {
        _oos.flush();
        _oos.close();
      }
      if (_ois!=null) _ois.close();
      _s = new Socket(_host, _port);
      _oos = new ObjectOutputStream(_s.getOutputStream());
      _oos.flush();
      _ois = new ObjectInputStream(_s.getInputStream());
    }
    */
    _oos.reset();  // force object to be written anew
    if (data instanceof DMsgIntf) _oos.writeObject(data);
    else _oos.writeObject(new DSendMsg(myid, toid, data, _coordName));
    _oos.flush();
    Object reply = _ois.readObject();
    if (reply instanceof OKReply) {
      return;
    }
    else {
      throw new ParallelException("sendData(myid, toid, data) failed");
    }
  }


  /**
   * works exactly as the corresponding method with same signature in
   * <CODE>parallel.MsgPassingCoordinator</CODE> class, except that the data
   * must be serializable. Also, if the data is a DMsgIntf object, the object
   * itself is sent (so as to be executed) rather than a DSendMsg wrapper object.
   * @param myid int
   * @param data Serializable
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws ParallelException
   */
  public synchronized void sendData(int myid, Serializable data) throws IOException, ClassNotFoundException, ParallelException {
    /*
    if (_s==null || _s.isClosed()) {
      if (_oos!=null) {
        _oos.flush();
        _oos.close();
      }
      if (_ois!=null) _ois.close();
      _s = new Socket(_host, _port);
      _oos = new ObjectOutputStream(_s.getOutputStream());
      _oos.flush();
      _ois = new ObjectInputStream(_s.getInputStream());
    }
    */
    _oos.reset();  // force object to be written anew to the stream
    if (data instanceof DMsgIntf) _oos.writeObject(data);
    else _oos.writeObject(new DSendMsg(myid, data, _coordName));
    _oos.flush();
    Object reply = _ois.readObject();
    if (reply instanceof OKReply) {
      return;
    }
    else {
      throw new ParallelException("sendData(myid, data) failed");
    }
  }

	
	/**
	 * send data, and receive reply back. Useful when a msg must send back some
	 * data as a result.
	 * @param myid int
	 * @param data DMsgIntf
	 * @return Object  // Serializable
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException 
	 */
	public synchronized Object sendAndRecvData(int myid, DMsgIntf data) throws IOException, ClassNotFoundException, ParallelException {
    _oos.reset();  // force object to be written anew to the stream
    _oos.writeObject(data);
    _oos.flush();
    Object reply = _ois.readObject();
    if (reply instanceof OKReplyData) {
      return ((OKReplyData) reply).getData();
    }
    else {
      throw new ParallelException("sendDataAndRecv(myid, data) failed");
    }
	}
	
	
  /**
   * works exactly as the corresponding method with same signature in
   * <CODE>parallel.MsgPassingCoordinator</CODE> class, except that the data
   * must be serializable.
   * @param myid int
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws ParallelException
   * @return Object // Serializable
   */
  public synchronized Object recvData(int myid) throws IOException, ClassNotFoundException, ParallelException {
    /*
    if (_s==null || _s.isClosed()) {
      if (_oos!=null) {
        _oos.flush();
        _oos.close();
      }
      if (_ois!=null) _ois.close();
      _s = new Socket(_host, _port);
      _oos = new ObjectOutputStream(_s.getOutputStream());
      _oos.flush();
      _ois = new ObjectInputStream(_s.getInputStream());
    }
    */
    _oos.reset();  // force object to be written anew to the stream
    _oos.writeObject(new DRecvMsg(myid, _coordName));
    _oos.flush();
    Object reply = _ois.readObject();
    if (reply instanceof FailedReply)
      throw new ParallelException("recvData(myid="+myid+"): server failed to recv msg");
    else return reply;
  }


  /**
   * works exactly as the corresponding method with same signature in
   * <CODE>parallel.MsgPassingCoordinator</CODE> class, except that the data
   * must be serializable.
   * @param myid int
   * @param fromid int
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws ParallelException
   * @return Object // Serializable
   */
  public synchronized Object recvData(int myid, int fromid) throws IOException, ClassNotFoundException, ParallelException {
    /*
    if (_s==null || _s.isClosed()) {
      if (_oos!=null) {
        _oos.flush();
        _oos.close();
      }
      if (_ois!=null) _ois.close();
      _s = new Socket(_host, _port);
      _oos = new ObjectOutputStream(_s.getOutputStream());
      _oos.flush();
      _ois = new ObjectInputStream(_s.getInputStream());
    }
    */
    _oos.reset();  // force object to be written anew to the stream
    _oos.writeObject(new DRecvMsg(myid, fromid, _coordName));
    _oos.flush();
    Object reply = _ois.readObject();
    if (reply instanceof FailedReply)
      throw new ParallelException("recvData(myid="+myid+", fromid="+fromid+"): server failed to recv msg");
    else return reply;
  }


  /**
   * close the (long-lived) socket connection.
   * @throws IOException
   */
  public synchronized void closeConnection() throws IOException {
    if (_s!=null && _s.isClosed()==false) {
      _s.close();
      if (_ois!=null) _ois.close();
      if (_oos!=null) {
        _oos.flush();
        _oos.close();
      }
    }
  }
}

