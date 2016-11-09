package parallel.distributed;

import parallel.*;
import java.io.*;
import java.net.*;

/**
 * class implementing clients for sending accumulating numbers between threads 
 * living in distributed JVMs (or for asking the current min/max/sum value among 
 * the received numbers). A thread wishing to send a number(s) invokes the 
 * <CODE>addNumber[s](data)</CODE> method of the client. A client wishing to
 * receive the minimum value currently stored on the accumulating server, calls
 * the <CODE>getMinValue()</CODE>. The client is thread-safe (essentially 
 * serialized).
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


  /**
   * method specifies the host and port where the accumulator server lives.
   * @param host String
   * @param port int
   * @throws UnknownHostException
   */
  public synchronized static void setHostPort(String host, int port) throws UnknownHostException {
    _host = InetAddress.getByName(host).getHostAddress();
    _port = port;
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
        return ((Double) ((OKReplyData) reply).getData()).doubleValue();
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
        return ((Double) ((OKReplyData) reply).getData()).doubleValue();
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
	
}

