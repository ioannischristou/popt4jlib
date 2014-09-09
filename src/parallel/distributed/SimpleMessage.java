package parallel.distributed;

import java.io.Serializable;

/**
 * encapsulates a single String object.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class SimpleMessage implements Serializable {
  private static final long serialVersionUID = -3512552556587721563L;
  private String _msg;

  /**
   * single public constructor
   * @param msg String
   */
  public SimpleMessage(String msg) {
    _msg = msg;
  }


  /**
   * returns the message contained in this object.
   * @return String
   */
  public String getMessage() { return _msg; }


  /**
   * overrides the Object.toString() method to print "SimpleMessage($this._msg$)".
   * @return String
   */
  public String toString() {
    return "SimpleMessage("+_msg+")";
  }

}
