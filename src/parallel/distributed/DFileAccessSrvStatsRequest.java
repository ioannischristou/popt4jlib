/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel.distributed;

import parallel.ParallelException;
import utils.DataMgr;
import java.io.*;
import java.util.Vector;


/**
 * class encapsulates request to return statistics about the server cache for a
 * specific file being read. Not part of the public API (despite the public 
 * status of the class).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DFileAccessSrvStatsRequest implements DMsgIntf {
	// private static final long serialVersionUID = ...L;
	private String _filename;
	private String _answer="N/A";

	/**
	 * public constructor.
	 * @param filename String the name of the file to read from
	 */
	public DFileAccessSrvStatsRequest(String filename) {
		_filename = filename;
	}
	
	
	/**
	 * @param oos ObjectOutputStream the socket output stream to send data via
	 * @throws IOException 
	 */
	public void execute(ObjectOutputStream oos) throws IOException {
		try {
			oos.writeObject(_answer);
		}
		catch (IOException e) {
			e.printStackTrace();
			oos.writeObject(new SimpleMessage(e.toString()));
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
			oos.writeObject(new SimpleMessage(e.toString()));
		}
		catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
			oos.writeObject(new SimpleMessage(e.toString()));			
		}
		oos.flush();
	}
	
	
	public String getFileName() { return _filename; }
	public void setAnswer(int[] data) {
		StringBuffer sb = new StringBuffer("");
		for (int i=0; i<data.length; i++) {
			sb.append("[").append(data[i]).append(",").append(data[i+1]).append("]");
			if (data[i+2]>0) sb.append("OK ");
			else sb.append("N/A ");
			i+=2;
		}
		_answer = sb.toString();
	}
}

