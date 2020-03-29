package parallel;

/**
 * Interface for classes that implement bounded or unbounded, synchronized or 
 * unsynchronized buffers operating in FIFO order.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface BufferIntf {
	
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
