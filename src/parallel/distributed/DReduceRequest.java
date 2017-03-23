package parallel.distributed;

import parallel.*;
import java.io.*;


/**
 * implements a request to enter a distributed reduce operation.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DReduceRequest implements DMsgIntf {
  //private static final long serialVersionUID = 987654333339L;
  private String _bName;
	private Serializable _data;
	private ReduceOperator _op;


  /**
   * public constructor.
   * @param reducername String the name of the DReducer object to be used.
	 * @param data Serializable the data to reduce
	 * @param op ReduceOperator the reduction operator
   */
  public DReduceRequest(String reducername, Serializable data, ReduceOperator op) {
    _bName = reducername;
		_data = data;
		_op = op;
  }


  /**
   * reads the data and sends them back to the JVM's thread that connected to
   * the server, via the same socket.
   * @param oos ObjectOutputStream
   * @throws ParallelException
   * @throws IOException
   */
  public void execute(ObjectOutputStream oos) 
		throws ParallelException, IOException {
    try {
      Serializable result = (Serializable) ReduceOpBase.getInstance(_bName).reduce(_data, _op);
      // ok, reduction done
			oos.reset();  // force object to be written anew
      oos.writeObject(new OKReplyData(result));
      oos.flush();
    }
    catch (ParallelException e) {
      oos.writeObject(new FailedReply());  // no need for oos.reset() here
      oos.flush();
      throw e;
    }
  }

	
	/**
	 * return string repr.
	 * @return String
	 */
  public String toString() {
    return "DReduceRequest(_bName="+_bName+",_data="+_data+",op="+_op+")";
  }

}
