package parallel;
import utils.Pair;
import java.util.HashMap;
import java.util.Comparator;


/**
 * a bounded-length Min-Heap data structure, supporting insert/remove-min/
 * decrease-key operations in O(logN) time. All methods are unsynchronized 
 * for efficiency reasons. The implementation uses standard array-based complete 
 * binary tree. For more information see ch. 6 in: 
 * "M.A. Weiss, Data Structures and Algorithm Analysis in C++, Addison-Wesley,
 * Upper Saddle River, NJ, 2014". This class is used in shortest-path computing
 * in the <CODE>graph.Graph</CODE> class. 
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class BoundedMinHeapUnsynchronized {
	private int _curSize;  // number of elements in array
	private final Object[] _heap;  // the object store, 
	                               // essentially starts at position 1
	private final HashMap _positions;  // needed to support decreaseKey() method.
	private Comparator _comp = new PairSecondArgDblComparator();
	
	
	/**
	 * public constructor will assume that the objects to be passed in will be 
	 * pairs of the form <CODE>utils.Pair(Object first, Double second)</CODE> and 
	 * the comparison between objects will be on the basis of the second argument 
	 * of the pair (unless second arguments are equal).
	 * @param maxsize int
	 * @throws IllegalArgumentException if maxsize &le; 1
	 */
	public BoundedMinHeapUnsynchronized(int maxsize) {
		if (maxsize<=1) throw new IllegalArgumentException("maxsize must be > 1");
		_heap = new Object[maxsize+1];
		_curSize = 0;
		_positions = new HashMap();
	}
	
	
	/**
	 * specifies also the Comparator for the objects to be maintained in this 
	 * min-heap.
	 * @param maxsize int
	 * @param comp Comparator
	 * @throws IllegalArgumentException if maxsize &le; 1
	 */
	public BoundedMinHeapUnsynchronized(int maxsize, Comparator comp) {
		this(maxsize);
		_comp = comp;
	}
	
	
	/**
	 * returns the current number of objects held in this min-heap. Obviously
	 * O(1) operation.
	 * @return int
	 */
	public int size() {
		return _curSize;
	}
	
	
	/**
	 * return the capacity of this min-heap. Obviously, O(1) operation.
	 * @return int
	 */
	public int getMaxSize() {
		return _heap.length-1;
	}
	
	
	/**
	 * adds an element to this heap. Duplicates are not allowed. 
	 * Time complexity is O(logN) where N is the size of this heap (assuming O(1)
	 * complexity for hash-map operations <CODE>containsKey(),put()</CODE>).
	 * @param o Object must be comparable via this object's comparator member 
	 * (<CODE>_comp</CODE>)
	 * @throws IndexOutOfBoundsException if min-heap is full
	 * @throws IllegalArgumentException if o already exists in min-heap
	 */
	public void addElement(Object o) {
		if (_curSize==_heap.length-1) 
			throw new IndexOutOfBoundsException("heap is full");
		if (_positions.containsKey(o)) 
			throw new IllegalArgumentException("Object "+o+
				                                 " already exists in this min-heap");
		int hole = ++_curSize;
		_heap[0] = o;
		for(; _comp.compare(o, _heap[hole/2])<0; hole /= 2) {
			_heap[hole] = _heap[hole/2];
			_positions.put(_heap[hole], new Integer(hole));
		}
		_heap[hole] = _heap[0];
		_positions.put(o, new Integer(hole));
		_heap[0] = null;  // 0 position is only used for temporary storage
	}
	
	
	/**
	 * remove the min-element from this heap. Time complexity is O(logN) where N 
	 * is the size of this heap.
	 * @return Object the min-element in this heap
	 * @throws IndexOutOfBoundsException if min-heap is empty
	 */
	public Object remove() {
		if (_curSize==0) throw new IndexOutOfBoundsException("heap is empty");
		Object result = _heap[1];
		_heap[1] = _heap[_curSize--];
		_positions.put(_heap[1], new Integer(1));
		percolateDown(1);
		_positions.remove(result);
		return result;
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
		int hole = findPos(key);
		// percolate up
		_heap[0] = update;
		for(; _comp.compare(update, _heap[hole/2])<0; hole /= 2) {
			_heap[hole] = _heap[hole/2];
			_positions.put(_heap[hole], new Integer(hole));
		}
		_heap[hole] = _heap[0];
		_positions.remove(key);
		_positions.put(update, new Integer(hole));  // used to be put(key,hole);
		_heap[0] = null;  // 0 position is only used for temporary storage
	}
	
	
	/**
	 * return a String representation of the elements in this min-heap.
	 * @return String
	 */
	public String toString() {
		String res = "";
		for (int i=0; i<_curSize; i++) {
			res += _heap[i];
			if (i<_curSize-1) res += ",";
		}
		return res;
	}
		
	
	/**
	 * find the index in the store where key is stored.
	 * @param key Object
	 * @return int the index where key is stored (in the range [1,_curSize]).
	 * @throws IllegalArgumentException if key is not found.
	 */
	private int findPos(Object key) throws IllegalArgumentException {
		Integer hole = (Integer) _positions.get(key);
		if (hole!=null) return hole.intValue();
		throw new IllegalArgumentException("key "+key+" not found in min-heap"+
			                                 "\nHeap is: "+toString());
	}
	
	
	private void percolateDown(int hole) {
		int child=0;
		Object tmp = _heap[hole];
		for(; hole*2 <= _curSize; hole = child) {
			child = hole*2;
			if (child!=_curSize && _comp.compare(_heap[child+1], _heap[child])<0)
				++child;
			if (_comp.compare(_heap[child], tmp)<0) {
				_heap[hole] = _heap[child];
				_positions.put(_heap[hole], new Integer(hole));
				_heap[child] = null;
			}
			else break;
		}
		_heap[hole] = tmp;
		_positions.put(_heap[hole], new Integer(hole));		
	}
	
	
	/**
	 * auxiliary static inner-class used as default comparator for heap. Not
	 * part of the public API.
	 */
	static class PairSecondArgDblComparator implements Comparator {
		/**
		 * assumes objects are of type <CODE>Pair&lt;Object f, Double s&gt;</CODE>
		 * and the comparison must be between the second member of each pair, with
		 * ties broken by first object comparison, if the first object is 
		 * <CODE>Comparable</CODE>.
		 * @param f Object  // Pair(Object,Double)
		 * @param s Object  // Pair(Object,Double)
		 * @return int the result of <CODE>Double.compare(f.second, s.second)</CODE>
		 * and in case of ties, if f.first is Comparable, the result of comparing
		 * the first member of the pairs
		 */
		public int compare(Object f, Object s) {
			Pair fp = (Pair) f;
			Pair sp = (Pair) s;
			int r =  Double.compare(((Double)fp.getSecond()).doubleValue(), 
				                     ((Double)sp.getSecond()).doubleValue());
			if (r==0) {
				if (fp.getFirst() instanceof Comparable) 
					return ((Comparable) fp.getFirst()).compareTo(sp.getFirst());
				else return 0;
			}
			else return r;
		}
	}
}
