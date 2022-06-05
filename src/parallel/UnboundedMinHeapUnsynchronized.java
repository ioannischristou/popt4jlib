package parallel;

import java.util.*;


/**
 * Unbounded version of the class <CODE>BoundedMinHeapUnsynchronized</CODE>,
 * offering the essential needed API of a min-heap data structure. Its amortized
 * costs are the same as for the bounded min-heap data structure.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2017-2022</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class UnboundedMinHeapUnsynchronized {
	private BoundedMinHeapUnsynchronized _minHeap;

	
	/**
	 * public constructor assumes that the objects to be passed in will be
	 * pairs of the form <CODE>utils.Pair(Object first, Double second)</CODE> and
	 * the comparison between objects will be on the basis of the second argument
	 * of the pair (unless second arguments are equal).
	 * @param init_size initial heap-size estimate
	 */
	public UnboundedMinHeapUnsynchronized(int init_size) {
		_minHeap = new BoundedMinHeapUnsynchronized(init_size);
	}
	
	
	/**
	 * public constructor requires besides an initial size estimate, a comparator
	 * object to compare any two objects in the heap.
	 * @param init_size
	 * @param comp 
	 */
	public UnboundedMinHeapUnsynchronized(int init_size, Comparator comp) {
		_minHeap = new BoundedMinHeapUnsynchronized(init_size, comp);
	}
	
	
	/**
	 * adds an element to this heap. Duplicates are not allowed. Notice that this
	 * implementation never throws IndexOutOfBoundsException; instead it will 
	 * eventually throw OutOfMemoryException when the heap memory runs out.
	 * Time complexity is O(logN) where N is the size of this heap (assuming O(1)
	 * complexity for hash-map operations <CODE>containsKey(),put()</CODE>).
	 * @param o Object must be comparable via this object's comparator member
	 * (<CODE>_comp</CODE>)
	 * @throws IllegalArgumentException if o already exists in min-heap
	 */	
	public void addElement(Object o) {
		try {
			_minHeap.addElement(o);
		}
		catch (IndexOutOfBoundsException e) {  // min-heap full, resize
			_minHeap = _minHeap.newCopyDoubleCapacity();
			_minHeap.addElement(o);
		}
	}
	
	
	/**
	 * remove the min-element from this heap. Time complexity is O(logN) where N
	 * is the size of this heap.
	 * @return Object the min-element in this heap
	 * @throws IndexOutOfBoundsException if min-heap is empty
	 */
	public Object remove() {
		return _minHeap.remove();
	}

	
	/**
	 * return the current number of elements in this min-heap. Obviously, O(1)
	 * operation.
	 * @return int
	 */
	public int size() {
		return _minHeap.size();
	}
	
	
	/**
	 * return the current capacity of this min-heap. Obviously, O(1) operation.
	 * @return int
	 */
	public int getMaxSize() {
		return _minHeap.getMaxSize();
	}


	/**
	 * resets this heap, letting the comparator as is.
	 */
	public void reset() {
		_minHeap.reset();
	}
	

	/**
	 * find the object key in the min-heap, and move it up so that its position
	 * matches the position for the value of the update object. Time complexity is
	 * O(logN) where N is the size of this heap, assuming O(1) complexity for
	 * hash-map operations <CODE>remove(),get(),put()</CODE>.
	 * @param key Object the object to be updated in this min-heap
	 * @param update Object the updated object that replaces the key above
	 * @throws IllegalArgumentException if key doesn't exist in the min-heap
	 */
	public void decreaseKey(Object key, Object update) {
		_minHeap.decreaseKey(key, update);
	}
}
