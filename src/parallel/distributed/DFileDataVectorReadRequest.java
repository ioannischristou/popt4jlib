/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel.distributed;

//import parallel.ParallelException;
import utils.DataMgr;
import java.io.*;
import java.util.Vector;


/**
 * class encapsulates request to read from a file residing on a machine running 
 * a msg-passing coordinator server, a certain range of vectors. Not part of the
 * public API (despite the public status of the class).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DFileDataVectorReadRequest implements DMsgIntf {
	// private static final long serialVersionUID = ...L;
	private String _filename;
	private int _fromIndex;
	private int _toIndex;
	private Vector _vectors;  // so that server can ask for the data to cache them
	                          // in a weak reference

	/**
	 * public constructor.
	 * @param filename String the name of the file to read from
	 * @param fromind int the starting index to include vectors from
	 * @param toind  int the last index to include vectors up to
	 */
	public DFileDataVectorReadRequest(String filename, int fromind, int toind) {
		_filename = filename;
		_fromIndex = fromind;
		_toIndex = toind;
	}
	
	
	/**
	 * reads the data from the file specified in the constructor in the range
	 * specified in the constructor, and sends them back via the object output 
	 * stream argument. If an exception occurs while attempting to read the data
	 * from the method <CODE>DataMgr.readVectorsFromFileInRange()</CODE>, a 
	 * <CODE>SimpleMessage</CODE> is sent back, with the message of the exception
	 * raised.
	 * @param oos ObjectOutputStream the socket output stream to send data via
	 * @throws IOException 
	 */
	public void execute(ObjectOutputStream oos) throws IOException {
		try {
			Vector vectors = DataMgr.readVectorsFromFileInRange(_filename, _fromIndex, _toIndex);
			oos.reset();  // force object to be written anew
			oos.writeObject(vectors);
			_vectors = vectors;
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
	public int getFromIndex() { return _fromIndex; }
	public int getToIndex() { return _toIndex; }
	public Vector getData() { return _vectors; }
	
}

