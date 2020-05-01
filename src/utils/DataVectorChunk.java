package utils;

import java.lang.ref.SoftReference;
import java.util.List;

/**
 * implements a place-holder for ranges of <CODE>VectorIntf</CODE> objects, that
 * are chunks of vectors specified in a file, with the optimization that the 
 * place-holder holds only soft references to those chunks, for saving memory if
 * needed. Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DataVectorChunk {
	private int _fromIndex;
	private int _toIndex;
	private SoftReference _data;  // _data is List<VectorIntf>

	
	/**
	 * sole constuctor is not public, allowing only package classes to construct
	 * such objects.
	 * @param data List  // List&lt;VectorIntf&gt;
	 * @param from int
	 * @param to int
	 */
	DataVectorChunk(List data, int from, int to) {
		_fromIndex = from;
		_toIndex = to;
		_data = new SoftReference(data);
	}
	
	
	/**
	 * return the value the 2nd argument to the constructor of this object.
	 * @return int
	 */
	int getFromIndex() { return _fromIndex; }
	/**
	 * return the value of the 3rd argument to the constructor of this object.
	 * @return int
	 */
	int getToIndex() { return _toIndex; }
	/**
	 * return the value of the 1st argument to the constructor of this object 
	 * unless it's been evicted from memory.
	 * @return List may be null
	 */
	List getData() {
		return (List) _data.get();  // may be null
	}
	
}

