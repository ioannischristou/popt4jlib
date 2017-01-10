package parallel.distributed;

import parallel.*;
import popt4jlib.ObserverIntf;
import popt4jlib.SubjectIntf;
import java.io.*;
import java.net.*;
import java.util.HashMap;


/**
 * class implementing clients for sending accumulating numbers between threads
 * living in distributed JVMs (or for asking the current min/max/sum value among
 * the received numbers). A thread wishing to send a number(s) invokes the
 * <CODE>addNumber[s](data)</CODE> method of the client. A client wishing to
 * receive the minimum value currently stored on the accumulating server, calls
 * the <CODE>getMinValue()</CODE>. 
 * <p>Clients may also register their interest in receiving updates on new min 
 * or max values accumulated in the server by calling the method 
 * <CODE>registerListener(popt4jlib.ObserverIntf obs, int not_type)</CODE>;
 * in such a case, the obs object's <CODE>notifyChange(SubjectIntf)</CODE> 
 * method will be invoked whenever a new min or max value is accumulated on the 
 * server, with parameter the updating thread of this client.
 * The client is thread-safe (essentially serialized).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DAccumulatorClt {
  private static String _host="localhost";
  private static int _port = 7900;
	private static String _notificationsHost="localhost";
	private static int _notificationsPort = 9900;
	private static AsynchUpdateThread _updaterThread=null;


  /**
   * method specifies the host and port where the accumulator server lives, as
	 * well as the accumulator notifications server.
   * @param host String the address of the <CODE>DAccumulatorSrv</CODE> or any
	 * other <CODE>FwdSrv</CODE> connected to the accumulator server
   * @param port int
	 * @param notificationshost String may be the <CODE>DAccumulatorSrv</CODE>
	 * or any other <CODE>BCastSrv</CODE> connected to the accumulator server
	 * @param notificationsport int if &lt; 0, the client does not register an 
	 * interest in notifications, otherwise, if &lt; 1024 the default port is 
	 * used.
   * @throws UnknownHostException
   */
  public synchronized static void setHostPort(String host, int port, 
		                                          String notificationshost, 
																							int notificationsport) 
		throws UnknownHostException {
    _host = InetAddress.getByName(host).getHostAddress();
    _port = port;
		_notificationsHost = InetAddress.getByName(notificationshost).getHostAddress();
		if (notificationsport>=1024 || notificationsport<0)
			_notificationsPort = notificationsport;
  }
	
	
	/**
	 * method must be called only once; it starts the 
	 * <CODE>DAccumulatorClt.AsynchUpdateThread</CODE> thread that will first send
	 * to the notifications server the type of notifications this client is 
	 * interested in, and then will keep listening for double values representing
	 * new accumulated incumbents (new min or max values).
	 * @param observer ObserverIntf the object that will be receiving the 
	 * notifications, ie this object's <CODE>notifyChange()</CODE> method will be
	 * called each time a new value arrives from the socket
	 * @param notification_type int allowed values are the declared 
	 * <CODE>DAccumulatorNotificationType</CODE> constants
	 * @throws IllegalStateException if called more than once
	 */
	public static synchronized void registerListener(ObserverIntf observer, int notification_type) {
		if (_updaterThread!=null) {
			throw new IllegalStateException("DAccumulatorClt.registerListener(): method already called.");
		}
		_updaterThread = new AsynchUpdateThread(observer, notification_type);
		_updaterThread.setDaemon(true);
		_updaterThread.start();
	}


	/**
	 * sends the argument to be accumulated to the server.
	 * @param num double
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException
	 */
  public static synchronized void addNumber(double num)
		throws IOException, ClassNotFoundException, ParallelException {
    Socket s = new Socket(_host, _port);
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    try {
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.flush();
      ois = new ObjectInputStream(s.getInputStream());
      oos.writeObject(new DAccNumberRequest(num));
      oos.flush();
      Object reply = ois.readObject();
      if (reply instanceof OKReply) {
        return;
      }
      else {
        throw new ParallelException("addNumber("+num+") failed");
      }
    }
    finally {
      if (oos!=null) oos.close();
      if (ois!=null) ois.close();
      s.close();
    }
  }


	/**
	 * sends the numbers in the argument array to be accumulated to the server.
	 * @param nums double[]
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException
	 */
  public static synchronized void addNumbers(double[] nums)
		throws IOException, ClassNotFoundException, ParallelException {
    Socket s = new Socket(_host, _port);
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    try {
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.flush();
      ois = new ObjectInputStream(s.getInputStream());
      oos.writeObject(new DAccNumbersRequest(nums));
      oos.flush();
      Object reply = ois.readObject();
      if (reply instanceof OKReply) {
        return;
      }
      else {
        throw new ParallelException("addNumbers(nums) failed");
      }
    }
    finally {
      if (oos!=null) oos.close();
      if (ois!=null) ois.close();
      s.close();
    }
  }


	/**
	 * sends the arguments to be accumulated to the server.
	 * @param arg Serializable
	 * @param num double
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException
	 */
  public static synchronized void addArgDblPair(Serializable arg, double num)
		throws IOException, ClassNotFoundException, ParallelException {
    Socket s = new Socket(_host, _port);
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    try {
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.flush();
      ois = new ObjectInputStream(s.getInputStream());
      oos.writeObject(new DAccSerDblPairRequest(arg, num));
      oos.flush();
      Object reply = ois.readObject();
      if (reply instanceof OKReply) {
        return;
      }
      else {
        throw new ParallelException("addArgDblPair("+arg+","+num+") failed");
      }
    }
    finally {
      if (oos!=null) oos.close();
      if (ois!=null) ois.close();
      s.close();
    }
  }


	/**
	 * gets back the minimum number accumulated so far in the server.
	 * @return double
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException
	 */
	public static synchronized double getMinNumber()
		throws IOException, ClassNotFoundException, ParallelException {
    Socket s = new Socket(_host, _port);
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    try {
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.flush();
      ois = new ObjectInputStream(s.getInputStream());
      oos.writeObject(new DGetMinNumberRequest());
      oos.flush();
      Object reply = ois.readObject();
      if (reply instanceof OKReplyData) {
        return ((Double) ((OKReplyData) reply).getData()).doubleValue();
      }
      else {
        throw new ParallelException("getMinNumber() failed");
      }
    }
    finally {
      if (oos!=null) oos.close();
      if (ois!=null) ois.close();
      s.close();
    }

	}


	/**
	 * gets back the maximum number accumulated so far in the server.
	 * @return double
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException
	 */
	public static synchronized double getMaxNumber()
		throws IOException, ClassNotFoundException, ParallelException {
    Socket s = new Socket(_host, _port);
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    try {
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.flush();
      ois = new ObjectInputStream(s.getInputStream());
      oos.writeObject(new DGetMaxNumberRequest());
      oos.flush();
      Object reply = ois.readObject();
      if (reply instanceof OKReplyData) {
        return ((Double) ((OKReplyData) reply).getData()).doubleValue();
      }
      else {
        throw new ParallelException("getMaxNumber() failed");
      }
    }
    finally {
      if (oos!=null) oos.close();
      if (ois!=null) ois.close();
      s.close();
    }
	}


	/**
	 * gets back the sum of numbers accumulated so far in the server.
	 * @return double
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException
	 */
	public static synchronized double getSumNumber()
		throws IOException, ClassNotFoundException, ParallelException {
    Socket s = new Socket(_host, _port);
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    try {
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.flush();
      ois = new ObjectInputStream(s.getInputStream());
      oos.writeObject(new DGetSumOfNumbersRequest());
      oos.flush();
      Object reply = ois.readObject();
      if (reply instanceof OKReplyData) {
        return ((Double) ((OKReplyData) reply).getData()).doubleValue();
      }
      else {
        throw new ParallelException("getSumNumber() failed");
      }
    }
    finally {
      if (oos!=null) oos.close();
      if (ois!=null) ois.close();
      s.close();
    }
	}


	/**
	 * gets back the arg-min object accumulated so far in the server.
	 * @return Serializable
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException
	 */
	public static synchronized Serializable getArgMin()
		throws IOException, ClassNotFoundException, ParallelException {
    Socket s = new Socket(_host, _port);
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    try {
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.flush();
      ois = new ObjectInputStream(s.getInputStream());
      oos.writeObject(new DGetArgMinObjectRequest());
      oos.flush();
      Object reply = ois.readObject();
      if (reply instanceof OKReplyData) {
        return (Double) ((OKReplyData) reply).getData();
      }
      else {
        throw new ParallelException("getArgMin() failed");
      }
    }
    finally {
      if (oos!=null) oos.close();
      if (ois!=null) ois.close();
      s.close();
    }
	}


	/**
	 * gets back the arg-max object accumulated so far in the server.
	 * @return Serializable
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException
	 */
	public static synchronized Serializable getArgMax()
		throws IOException, ClassNotFoundException, ParallelException {
    Socket s = new Socket(_host, _port);
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    try {
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.flush();
      ois = new ObjectInputStream(s.getInputStream());
      oos.writeObject(new DGetArgMaxObjectRequest());
      oos.flush();
      Object reply = ois.readObject();
      if (reply instanceof OKReplyData) {
        return ((OKReplyData) reply).getData();
      }
      else {
        throw new ParallelException("getArgMax() failed");
      }
    }
    finally {
      if (oos!=null) oos.close();
      if (ois!=null) ois.close();
      s.close();
    }
	}
	
	
	public static synchronized void disconnect() {
		if (_updaterThread!=null) {
			utils.Messenger.getInstance().msg("DAccumulatorClt.disconnect(): enter", 0);
			_updaterThread.setCont(false);
			try {
				if (_updaterThread._s!=null) {
					_updaterThread._s.shutdownInput();
					_updaterThread._s.close();
				}
			}
			catch (IOException e) {
				utils.Messenger.getInstance().msg("DAccumulatorClt.disconnect(): disconnected notification-updates Thread", 0);
			}
			finally {
				_updaterThread=null;
				utils.Messenger.getInstance().msg("DAccumulatorClt.disconnect(): done", 0);
			}
		}
	}


	/**
	 * sends a shut-down request to the server.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException
	 */
  public static synchronized void shutDownSrv()
		throws IOException, ClassNotFoundException, ParallelException {
    Socket s = new Socket(_host, _port);
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    try {
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.flush();
      ois = new ObjectInputStream(s.getInputStream());
      oos.writeObject(new DShutDownSrvRequest());
      oos.flush();
      Object reply = ois.readObject();
      if (reply instanceof OKReply) {
        return;
      }
      else {
        throw new ParallelException("shutDownSrv() failed");
      }
    }
    finally {
      if (oos!=null) oos.close();
      if (ois!=null) ois.close();
      s.close();
    }
  }
	
	
	/**
	 * auxiliary thread class is responsible for opening a connection to the 
	 * DAccumulatorSrv's notification-port, and listening for incoming numbers 
	 * representing new (min or max) incumbents. Whenever such a new incumbent 
	 * arrives, the thread calls its unique observer's notifyChange(this) method.
	 * Not part of the public API.
	 */
	static class AsynchUpdateThread extends Thread implements SubjectIntf {
		private final int _type;
		private final ObserverIntf _observer;
		private Double _inc = Double.NaN;
		private Socket _s = null;
		private boolean _cont = true;
		
		
		/**
		 * sole constructor.
		 * @param observer ObserverIntf
		 * @param type int must only be one of DAccumulatorNotificationType.[_MIN|_MAX]
		 */
		AsynchUpdateThread(ObserverIntf observer, int type) {
			_type = type;
			_observer = observer;
		}
		
		
		/**
		 * opens a socket to the accumulator's server notifications-port (default 
		 * 9990) and then enters an infinite loop listening for Double objects which 
		 * are new incumbents (min or max, according to constructor's 2nd argument).
		 * Notice that it is also possible that instead of connecting directly to 
		 * the accumulator server's notifications-port, the client may connect to 
		 * a broadcast server that is connected to the accumulator server; the 
		 * effect would be the same, and in this way, it is possible to have many
		 * more accumulator clients connected to the accumulator server than the 
		 * O/S would allow (limit on number on sockets or threads etc.)
		 */
		public void run() {
			try {
				synchronized (DAccumulatorClt.class) {
					_s = new Socket(_notificationsHost, _notificationsPort);
				}
				ObjectOutputStream oos = new ObjectOutputStream(_s.getOutputStream());
				oos.flush();
				ObjectInputStream ois = new ObjectInputStream(_s.getInputStream());
				oos.writeObject(new DAccumulatorNotificationType(_type));
				oos.flush();
				while (getCont()) {
					Double valD = (Double) ois.readObject();
					synchronized (this) {
						_inc = valD;
					}
					notifyObservers();
				}
			}
			catch (Exception e) {
				utils.Messenger mger = utils.Messenger.getInstance();
				mger.msg("DAccumulatorClt.AsynchUpdateThread.run(): received exception '"+
					       e+"': thread exits.", 0);
			}
		}
			
		
		/**
		 * get the best Double object found so far.
		 * @return Object  // Double 
		 */
	  public synchronized Object getIncumbent() {
			return _inc;
		}
		
		
		/**
		 * calls the associated observer's <CODE>notifyChange(this)</CODE> method.
		 */
		public void notifyObservers() {
			try {
				_observer.notifyChange(this);
			}
			catch (Exception e) {
				e.printStackTrace();  // ignore for now
			}
		}

		
		// below methods are so that AsynchUpdateThread implements the SubjectIntf
		// interface; they all throw UnsupportedOperationException exception.
		
		public boolean registerObserver(ObserverIntf o) {
			throw new UnsupportedOperationException("registerObserver() not supported");
		}
		public boolean removeObserver(ObserverIntf o) {
			throw new UnsupportedOperationException("removeObserver() not supported");
		}
		public popt4jlib.FunctionIntf getFunction() {
			throw new UnsupportedOperationException("getFunction() not supported");
		}
		public HashMap getParams() {
			throw new UnsupportedOperationException("getParams() not supported");			
		}
		public void addIncumbent(ObserverIntf obs, Object soln) {
			throw new UnsupportedOperationException("addIncumbent() not supported");						
		}
		
		synchronized boolean getCont() {
			return _cont;
		}
		synchronized void setCont(boolean c) {
			_cont = c;
		}

	}

}

