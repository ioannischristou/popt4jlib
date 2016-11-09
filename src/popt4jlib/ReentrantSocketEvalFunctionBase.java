package popt4jlib;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 * The class is a wrapper for functions that are computed in a different
 * process and communicate with the optimizer through sockets. The process that
 * evaluates the function must receive its arguments via a socket in a known
 * host/port address, and will return the double value of the evaluation back
 * to the popt4jlib client through the same socket.
 * The class also keeps track of how many times a function has been evaluated,
 * plus it forces threads asking for function evaluation to execute sequentially.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ReentrantSocketEvalFunctionBase implements FunctionIntf {
  private Socket _s;
  private ObjectInputStream _ois;
  private ObjectOutputStream _oos;
  private long _evalCount=0;
  private boolean _sendParams=false;


  /**
   * public constructor, setting up the evaluation count to zero.
	 * @param host String
	 * @param port int
	 * @throws IOException
	 * @throws SocketException
   */
  public ReentrantSocketEvalFunctionBase(String host, int port) throws IOException, SocketException {
    _s = new Socket(host, port);
    _oos = new ObjectOutputStream(_s.getOutputStream());
    _oos.flush();
    _ois = new ObjectInputStream(_s.getInputStream());
    _evalCount=0;
    utils.Messenger.getInstance().msg("ReentrantSocketEvalFunctionBase connected at <"+host+","+port+">",0);
  }


  /**
   * same as two-arg constructor, but also specifies whether to send function
   * params (in the HashMap, not the argument of the function) or not via the
   * socket.
   * @param host String
   * @param port int
   * @param sendFunctionParams boolean
   * @throws IOException
   * @throws SocketException
   */
  public ReentrantSocketEvalFunctionBase(String host, int port, boolean sendFunctionParams)
      throws IOException, SocketException {
    this(host,port);
    _sendParams=sendFunctionParams;
  }


  /**
   * force function to be reentrant as no two threads can enter the eval()
   * method simultaneously
   * @param arg Object
   * @param params HashMap
   * @return double
   */
  public synchronized double eval(Object arg, HashMap params) throws IllegalArgumentException {
    ++_evalCount;
    // send params
    if (arg!=null) {
      try {
        _oos.writeObject(arg);
        _oos.flush();
        if (params != null && _sendParams) _oos.writeObject(params);
        _oos.flush();
        // get back the value
        Double val = (Double) _ois.readObject();
        return val.doubleValue();
      }
      catch (Exception e) {
        e.printStackTrace();
        throw new IllegalArgumentException("evaluation failed...");
      }
    }
    else {
      throw new IllegalArgumentException("cannot evaluate null arg");
    }
  }



  /**
   * get the total number of times the <CODE>eval(arg,params)</CODE> method was
   * called so far.
   * @return long
   */
  synchronized public long getEvalCount() { return _evalCount; }


  /**
   * shut-down the associated socket. After this operation, this object cannot
   * be used any more to evaluate any argument.
   * @throws IOException
   * @throws SocketException
   */
  public synchronized void shutDownSocket() throws IOException, SocketException {
    if (_ois != null) {
      _ois.close();
      _ois = null;
    }
    if (_oos != null) {
      _oos.close();
      _oos=null;
    }
    if (_s!=null) {
      _s.close();
      _s=null;
    }
  }
}

