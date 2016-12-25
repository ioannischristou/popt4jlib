/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel.distributed;
import java.io.Serializable;


/**
 * Helper class that <CODE>DAccumulatorClt</CODE> clients use to subscribe their
 * interest to a <CODE>DAccumulatorSrv</CODE> object about receiving updates
 * when new max or min values are sent to the server.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DAccumulatorNotificationType implements Serializable {
	// public static final long serialVersionUID=-1L;
	
	public static final int _MAX=1;
	public static final int _MIN=2;
	
	private int _type;
	
	/**
	 * sole constructor.
	 * @param type int
	 */
	public DAccumulatorNotificationType(int type) {
		_type = type;
	}
	
	
	/**
	 * return the notification type.
	 * @return int
	 */
	public int getType() {
		return _type;
	}	
}


/**
 * class defines DAccumulatorNotificationType=MAX.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DAccNotTypeMAX extends DAccumulatorNotificationType {
	public DAccNotTypeMAX() {
		super(_MAX);
	}
}


/**
 * class defines DAccumulatorNotificationType=MIN.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DAccNotTypeMIN extends DAccumulatorNotificationType {
	public DAccNotTypeMIN() {
		super(_MIN);
	}
}

