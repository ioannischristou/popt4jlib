package parallel;

import java.io.Serializable;


/**
 * Interface for classes that implement bounded or unbounded, synchronized or 
 * unsynchronized buffers operating in FIFO order.
 * <p>Notes:
 * <ul>
 * <li>20211009: the interface now extends <CODE>Serializable</CODE> so that all
 * implementing classes are serializable. This means that all objects to be 
 * added to such buffers must also be serializable, otherwise the underlying 
 * buffer data structure must be declared transient.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2020-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 2.0
 */
public interface BufferIntf extends Serializable {
	
	/**
	 * adds an element at the end of this buffer.
	 * @param o Object
	 * @throws Exception if this is an unsynchronized bounded buffer 
	 * and it is full
	 */
	public void addElement(Object o) throws Exception;
	
	
	/**
	 * return the element at position pos.
	 * @param pos int
	 * @return Object
	 * @throws Exception if the index is out of range
	 */
	public Object elementAt(int pos) throws Exception;
	
	
	/**
	 * removes the "oldest" element in this buffer and returns it to the caller.
	 * @return Object
	 * @throws Exception 
	 */
	public Object remove() throws Exception;
	
	
	/**
	 * return the current number of objects in this buffer.
	 * @return int
	 */
	public int size();
	
	
	/**
	 * reset this buffer.
	 */
	public void reset();
	
}
