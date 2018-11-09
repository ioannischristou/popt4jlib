/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
	
	DataVectorChunk(List data, int from, int to) {
		_fromIndex = from;
		_toIndex = to;
		_data = new SoftReference(data);
	}
	
	int getFromIndex() { return _fromIndex; }
	int getToIndex() { return _toIndex; }
	List getData() {
		return (List) _data.get();  // may be null
	}
	
}
