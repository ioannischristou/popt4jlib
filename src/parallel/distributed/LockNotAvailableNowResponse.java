/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel.distributed;

import java.io.Serializable;

/**
 * indicates a response that a distributed lock canNOT be given immediately,
 * with no waiting.
 * Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LockNotAvailableNowResponse implements Serializable {
  //private final static long serialVersionUID = 3750098616479668171L;
	
	public LockNotAvailableNowResponse() {
		// no-op
	}
}
