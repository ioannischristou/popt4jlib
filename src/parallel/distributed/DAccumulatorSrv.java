package parallel.distributed;

import parallel.*;
import java.net.*;
import java.io.*;


/**
 * class implements a server for distributed data accumulation functionality. 
 * Multiple JVMs can send numerical data to this server, or ask for the 
 * min/max/sum/etc. value the server currently holds.
 * Each client may as well be multi-threaded.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DAccumulatorSrv {
  private int _port = 7900;  // default port
  private DynamicAsynchTaskExecutor _pool;
  private long _countConns;  // defaults to 0
	private double _minValue = Double.POSITIVE_INFINITY;
	private double _maxValue = Double.NEGATIVE_INFINITY;
	private double _sumValue;  // defaults to 0
	private double _minValueWithArg = Double.POSITIVE_INFINITY;
	private double _maxValueWithArg = Double.NEGATIVE_INFINITY;
	private double _sumValuesWithArg;  // defaults to 0
	private Serializable _minArg;  // defaults to null
	private Serializable _maxArg;  // defaults to null
	
	
  /**
   * constructor specifying the port the server will listen to, and
   * the maximum number of threads in the thread-pool.
   * @param port int if &lt; 1024, the number 7900 is used.
   * @param maxthreads int if &lt; 10000, the number 9999 is used.
   */
  DAccumulatorSrv(int port, int maxthreads) {
    if (port >= 1024)
      _port = port;
    try {
      if (maxthreads<10000)
        maxthreads = 9999;  // the max. number of threads
                            // cannot be restricted as it
                            // may introduce starvation locks.
      _pool = DynamicAsynchTaskExecutor.
								newDynamicAsynchTaskExecutor(2, maxthreads);
    }
    catch (ParallelException e) {
      e.printStackTrace();  // cannot get here
    }
  }


  /**
   * enters an infinite loop, listening for socket connections, and handling
   * the incoming sockets as new Runnable tasks (DAccTask objects) that are
   * given to the associated thread-pool.
   * @throws IOException
   * @throws ParallelException
   */
  void run() throws IOException, ParallelException {
    ServerSocket ss = new ServerSocket(_port);
    while (true) {
      utils.Messenger.getInstance().msg("DAccumulatorSrv: waiting for socket connection",0);
      Socket s = ss.accept();
      ++_countConns;
      System.err.println("Total "+_countConns+" socket connections arrived.");
      handle(s, _countConns);
    }
  }
	
	
	/**
	 * updates this server's statistics when this number is &quot;added&quot; to
	 * the server.
	 * @param num double
	 */
	synchronized void addNumber(double num) {
		if (Double.isNaN(num)) return;  // ignore
		_sumValue += num;
		if (_minValue > num) _minValue = num;
		if (_maxValue < num) _maxValue = num;
	}
	
	
	/**
	 * updates this server's statistics when these numbers are &quot;added&quot; 
	 * to the server.
	 * @param numbers double[]
	 */	
	synchronized void addNumbers(double[] numbers) {
		if (numbers==null) return;
		for (int i=0; i<numbers.length; i++) addNumber(numbers[i]);
	}
	
	
	/**
	 * updates the server statistics according to the (arg,val) pair in the 
	 * parameters. Notice that this method is synchronized but theoretically it 
	 * could synchronize on a different object since the methods that simply add
	 * numbers to the accumulator do not interfere with the (arg,value) pair 
	 * methods.
	 * @param arg Serializable
	 * @param val double
	 */
	synchronized void addArgDblPair(Serializable arg, double val) {
		if (Double.isNaN(val)) return;  // ignore
		_sumValuesWithArg += val;
		if (_minValueWithArg > val) {
			_minValueWithArg = val;
			_minArg = arg;
		}
		if (_maxValueWithArg < val) {
			_maxValueWithArg = val;
			_maxArg = arg;
		}
	}

	
	/**
	 * returns the min value seen so far.
	 * @return double
	 */
	synchronized double getMinValue() {
		return _minValue;
	}
	
	
	/**
	 * returns the argument object with the min value that arrived in a 
	 * (Serializable,double) pair so far.
	 * @return Serializable
	 */
	synchronized Serializable getArgMin() {
		return _minArg;
	}
	
	
	/**
	 * returns the max value seen so far.
	 * @return double
	 */
	synchronized double getMaxValue() {
		return _maxValue;
	}

	
	/**
	 * returns the argument object with the max value that arrived in a 
	 * (Serializable,double) pair so far.
	 * @return Serializable
	 */
	synchronized Serializable getArgMax() {
		return _maxArg;
	}

	
	/**
	 * returns the sum of all values seen so far.
	 * @return double
	 */
	synchronized double getSumValue() {
		return _sumValue;
	}

	
  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; parallel.distributed.DAccumulatorSrv [port(7900)] [maxthreads(10000)]</CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    int port = -1;
    int maxthreads = 10000;
    DAccumulatorSrv srv=null;
    if (args.length>0) {
      port = Integer.parseInt(args[0]);
      if (args.length>1)
        maxthreads = Integer.parseInt(args[1]);
    }
    srv = new DAccumulatorSrv(port, maxthreads);
    try {
      srv.run();
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }


  private void handle(Socket s, long connum) throws IOException, ParallelException {
    DAccTask t = new DAccTask(s, connum);
    _pool.execute(t);
  }


  /**
   * inner helper class.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011-2016</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class DAccTask implements Runnable {
    private Socket _s;
    private long _conId;

    DAccTask(Socket s, long conid) {
			_s = s;
      _conId = conid;
    }

    public void run() {
      utils.Messenger.getInstance().msg("DAccTask with id="+_conId+" running...",1);
      ObjectInputStream ois = null;
      ObjectOutputStream oos = null;
      try {
        oos = new ObjectOutputStream(_s.getOutputStream());
        oos.flush();
        ois = new ObjectInputStream(_s.getInputStream());
        DAccMsg msg = (DAccMsg) ois.readObject();  // read a msg (accumulate-numbers or get[Min/Max/Sum...])
        msg.execute(DAccumulatorSrv.this, oos);
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
      finally {
        try {
          if (oos != null) {
            oos.flush();
            oos.close();
          }
          if (ois != null) ois.close();
          _s.close();
        }
        catch (IOException e) {
          e.printStackTrace();  // shouldn't get here
        }
      }
    }
  }

}

