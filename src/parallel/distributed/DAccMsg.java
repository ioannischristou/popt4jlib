package parallel.distributed;

import java.io.*;
import parallel.*;

/**
 * DAccMsg objects are meant to transport over Socket wires number accumulation
 * requests (accumulate-data or get[Min/Max/Sum...] requests).
 * Not part of the public API (despite the public status).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public abstract class DAccMsg implements Serializable {
  /**
   * if the object is a <CODE>DAccNumber[s]Request</CODE>, it instructs the 
	 * server to add the data in its cache; else if it's a 
	 * <CODE>DGetMinRequest</CODE>, it instructs the server to find the min value
	 * it holds and send it to the other end of the socket.
   * @param oos ObjectOutputStream
   * @throws ParallelException
   * @throws IOException
   */
  public abstract void execute(DAccumulatorSrv srv, ObjectOutputStream oos) throws ParallelException, IOException;
}


/**
 * encapsulates a request to accumulate a number in the server. 
 * Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DAccNumberRequest extends DAccMsg {
	// private final static long serialVersionUID = 5801899648236803371L;
	private double _num;
	
	
	/**
	 * sole constructor.
	 * @param num double
	 */
	DAccNumberRequest(double num) {
		_num = num;
	}
	
	
	/**
	 * sends data field to the server.
	 * @param srv DAccumulatorSrv
	 * @param oos ObjectOutputStream
	 * @throws ParallelException
	 * @throws IOException 
	 */
	public void execute(DAccumulatorSrv srv, ObjectOutputStream oos) throws ParallelException, IOException {
		srv.addNumber(_num);
    oos.writeObject(new OKReply());
    oos.flush();
		// no need for reset
	}
}


/**
 * encapsulates a request to accumulate a (Serializable,double) pair in the 
 * server. 
 * Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DAccSerDblPairRequest extends DAccMsg {
	// private final static long serialVersionUID = 5801899648236803371L;
	private Serializable _arg;  // object usually represents the argument to a
	                            // function evaluation that yields the number _num
	                            // below
	private double _num;
	
	
	/**
	 * sole constructor.
	 * @param arg Serializable
	 * @param num double
	 */
	DAccSerDblPairRequest(Serializable arg, double num) {
		_arg = arg;
		_num = num;
	}
	
	
	/**
	 * adds data fields to the server and sends back an <CODE>OKReply</CODE>.
	 * @param srv DAccumulatorSrv
	 * @param oos ObjectOutputStream
	 * @throws ParallelException
	 * @throws IOException 
	 */
	public void execute(DAccumulatorSrv srv, ObjectOutputStream oos) 
		throws ParallelException, IOException {
		srv.addArgDblPair(_arg,_num);
    oos.writeObject(new OKReply());
    oos.flush();
		// no need for reset here
	}
}


/**
 * encapsulates a request to accumulate an array of numbers in the server. 
 * Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DAccNumbersRequest extends DAccMsg {
	// private final static long serialVersionUID = 5801899648236803371L;
	private double[] _numbers;
	
	
	/**
	 * sole constructor.
	 * @param nums double[]
	 */
	DAccNumbersRequest(double[] nums) {
		_numbers = nums;
	}
	
	
	/**
	 * sends the data to the server for "accumulation".
	 * @param srv DAccumulatorSrv
	 * @param oos ObjectOutputStream
	 * @throws ParallelException
	 * @throws IOException 
	 */
	public void execute(DAccumulatorSrv srv, ObjectOutputStream oos) throws ParallelException, IOException {
		srv.addNumbers(_numbers);
    oos.writeObject(new OKReply());
    oos.flush();
		// no need for reset
	}
}


/**
 * encapsulates a request to get the min of values sent to a server. 
 * Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DGetMinNumberRequest extends DAccMsg {
	// private final static long serialVersionUID = 5801899648236803371L;
	
	
	/**
	 * sole constructor.
	 */
	DGetMinNumberRequest() {
		// no-op
	}
	
	
	/**
	 * calls the server's <CODE>getMinValue()</CODE> and writes it to the second
	 * argument that is the output stream of the socket to the client.
	 * @param srv DAccumulatorSrv
	 * @param oos ObjectOutputStream
	 * @throws ParallelException
	 * @throws IOException 
	 */
	public void execute(DAccumulatorSrv srv, ObjectOutputStream oos) throws ParallelException, IOException {
		double val = srv.getMinValue();
    oos.writeObject(new OKReplyData(new Double(val)));
    oos.flush();
		// no need for reset here
	}
}


/**
 * encapsulates a request to get the argument corresponding to the min value 
 * sent in a (Serializable,double) pair accumulation request to a server. 
 * Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DGetArgMinObjectRequest extends DAccMsg {
	// private final static long serialVersionUID = 5801899648236803371L;
	
	
	/**
	 * sole constructor.
	 */
	DGetArgMinObjectRequest() {
		// no-op
	}
	
	
	/**
	 * calls the server's <CODE>getArgMin()</CODE> and writes it to the 2nd
	 * argument that is the output stream of the socket to the client.
	 * @param srv DAccumulatorSrv
	 * @param oos ObjectOutputStream
	 * @throws ParallelException
	 * @throws IOException 
	 */
	public void execute(DAccumulatorSrv srv, ObjectOutputStream oos) throws ParallelException, IOException {
		Serializable arg = srv.getArgMin();
    oos.reset();  // force object to be written anew
		oos.writeObject(new OKReplyData(arg));
    oos.flush();
	}
}


/**
 * encapsulates a request to get the max value sent to a server. 
 * Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DGetMaxNumberRequest extends DAccMsg {
	// private final static long serialVersionUID = 5801899648236803371L;
	
	/**
	 * sole constructor.
	 */
	DGetMaxNumberRequest() {
		
	}
	
	
	/**
	 * calls the server's <CODE>getMaxValue()</CODE> method and writes it to the 
	 * second argument that is the output stream of the socket to the client.
	 * @param srv DAccumulatorSrv
	 * @param oos ObjectOutputStream
	 * @throws ParallelException
	 * @throws IOException 
	 */
	public void execute(DAccumulatorSrv srv, ObjectOutputStream oos) throws ParallelException, IOException {
		double val = srv.getMaxValue();
    oos.reset();  // force object to be written anew
		oos.writeObject(new OKReplyData(new Double(val)));
    oos.flush();
	}
}


/**
 * encapsulates a request to get the argument corresponding to the max value 
 * sent in a (Serializable,double) pair accumulation request to a server. 
 * Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DGetArgMaxObjectRequest extends DAccMsg {
	// private final static long serialVersionUID = 5801899648236803371L;
	
	
	/**
	 * sole constructor.
	 */
	DGetArgMaxObjectRequest() {
		// no-op
	}
	
	
	/**
	 * calls the server's <CODE>getArgMax()</CODE> and writes it to the 2nd
	 * argument that is the output stream of the socket to the client.
	 * @param srv DAccumulatorSrv
	 * @param oos ObjectOutputStream
	 * @throws ParallelException
	 * @throws IOException 
	 */
	public void execute(DAccumulatorSrv srv, ObjectOutputStream oos) throws ParallelException, IOException {
		Serializable arg = srv.getArgMax();
		oos.reset();  // force object to be written anew
    oos.writeObject(new OKReplyData(arg));
    oos.flush();
	}
}


/**
 * encapsulates a request to get the sum of all values sent to a server. 
 * Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DGetSumOfNumbersRequest extends DAccMsg {
	// private final static long serialVersionUID = 5801899648236803371L;
	
	
	/**
	 * sole constructor.
	 */
	DGetSumOfNumbersRequest() {
		// no-op
	}
	
	
	/**
	 * calls the server's <CODE>getSumValue()</CODE> method and writes it to the 
	 * second argument that is the output stream of the socket to the client.
	 * @param srv DAccumulatorSrv
	 * @param oos ObjectOutputStream
	 * @throws ParallelException
	 * @throws IOException 
	 */
	public void execute(DAccumulatorSrv srv, ObjectOutputStream oos) throws ParallelException, IOException {
		double val = srv.getSumValue();
		oos.reset();  // force object to be written anew
    oos.writeObject(new OKReplyData(new Double(val)));
    oos.flush();
	}
}


/**
 * encapsulates a request to shut-down a server. 
 * Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DShutDownSrvRequest extends DAccMsg {
	// private final static long serialVersionUID = 5801899648236803371L;
	
	
	/**
	 * sole constructor.
	 */
	DShutDownSrvRequest() {
	}
	
	
	/**
	 * shuts-down the server.
	 * @param srv DAccumulatorSrv
	 * @param oos ObjectOutputStream
	 * @throws ParallelException
	 * @throws IOException 
	 */
	public void execute(DAccumulatorSrv srv, ObjectOutputStream oos) throws ParallelException, IOException {
    // no need for reset here
		oos.writeObject(new OKReply());
    oos.flush();
		System.exit(0);
	}
}
