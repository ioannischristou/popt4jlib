/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel.distributed;

import java.io.Serializable;

/**
 * helper class not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class OKReplyData extends OKReply {
	private Serializable _data;
	
	
	/**
	 * sole constructor.
	 * @param data 
	 */
	public OKReplyData(Serializable data) {
		_data = data;
	}
	
	
	/**
	 * get the reply's data.
	 * @return Serializable
	 */
	public Serializable getData() {
		return _data;
	}
	
	
	/**
	 * returns string repr.
	 * @return String
	 */
	public String toString() {
		return "OKReplyData(data="+_data.toString()+")";
	}
}
