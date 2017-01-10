package parallel;

/**
 * an unbounded-length FIFO buffer implemented as an Object Array that's totally
 * unsynchronized. It's exactly the same as the 
 * <CODE>BoundedBufferArrayUnsynchronized</CODE> class, only that's completely 
 * unbounded (until out-of-memory-exceptions are thrown), therefore certain 
 * exceptions are never thrown.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class UnboundedBufferArrayUnsynchronized {
  private Object[] _buffer;
  private int _head;  // points to first element, remove from head (-1 if no elems in buffer)
  private int _tail;  // points to last element, add into tail (-1 if no elems in buffer)

  /**
   * public constructor.
   * @param initlen int must be greater than zero
   */
  public UnboundedBufferArrayUnsynchronized(int initlen) {
    _buffer = new Object[initlen];
    _head = -1;
    _tail = -1;
  }


  /**
   * add an object in the buffer. If the current buffer is full, it is resized
	 * by getting a buffer of size twice the current size.
   * @param obj Object
	 * @throws IllegalArgumentException if argument is null
   */
  public void addElement(Object obj) throws IllegalArgumentException {
		if (obj==null) throw new IllegalArgumentException("null argument cannot be passed in");
    if (_head!=-1 && prev(_head)==_tail) {  // buffer full, resize and copy
			final int cur_buf_len = _buffer.length;
			Object[] buf = new Object[2*cur_buf_len];
			for (int i=_head, j=0; j<cur_buf_len; i=next(i), j++) {
				buf[j] = _buffer[i];
			}
			_head = 0;
			_tail = cur_buf_len-1;
			_buffer = buf;
		}
    if (_head==-1) {
      _head = 0;
      _tail = _head;
      _buffer[0] = obj;
      return;
    } else {
      _tail = next(_tail);
      _buffer[_tail] = obj;
      return;
    }
  }


  /**
   * remove the first object that was inserted in the buffer.
   * @throws IllegalStateException if the buffer is empty.
   * @return Object
   */
  public Object remove() throws IllegalStateException {
    if (_head==-1)
      throw new IllegalStateException("buffer is empty");
    Object result = _buffer[_head];
		_buffer[_head] = null;  // avoid memory leak
    if (_head==_tail) {  // buffer is now empty
      _head = -1;
      _tail = -1;
      return result;
    }
    _head = next(_head);
    return result;
  }


	/**
	 * removes the <CODE>i</CODE>-th element in this array and returns it. Much 
	 * slower than the other operations.
	 * @param i int 
	 * @return Object the element previously in the <CODE>i</CODE>-th position.
	 * @throws IndexOutOfBoundsException if the index is out of the range {0,...size()-1} 
	 */
	public Object remove(int i) throws IndexOutOfBoundsException {
		if (i==0) return remove();  // remove the first element (pointed by _head)
		if (i<0 || i>=size()) throw new IndexOutOfBoundsException("index "+i+" out of range");
		Object res=null;
		int opos = _head+i;
		if (opos>=_buffer.length) opos -= _buffer.length;
		res = _buffer[opos];
		for (; opos!=_tail; opos = next(opos)) {
			_buffer[opos] = _buffer[next(opos)];
		}
		_buffer[_tail] = null;  // avoid memory leaks
		_tail = prev(_tail);
		return res;
	}


  /**
   * return the datum at position i.
   * @param i int
   * @throws IndexOutOfBoundsException if the index is out of range.
   * @return Object
   */
  public Object elementAt(int i) throws IndexOutOfBoundsException {
    if (i<0 || i>= size()) throw new IndexOutOfBoundsException("index "+i+"out of range");
    int opos = _head+i;
    if (opos<_buffer.length) return _buffer[opos];
    else return _buffer[i - (_buffer.length - _head)];
  }


  /**
   * returns the current total number of objects in the buffer.
   * @return int
   */
  public int size() {
    if (_head==-1) return 0;
    if (_head<=_tail) return _tail - _head + 1;
    else return _buffer.length - (_head - _tail) + 1;
  }

	
	/**
	 * return the buffer array's length.
	 * @return int
	 */
	public int getCurrentMaxSize() {
		return _buffer.length;
	}

	
	/**
	 * print a String representation of this object. Used for debugging.
	 * @return String
	 */
	public String toString() {
		String arr = "UBAU(buf_length="+_buffer.length+
								 " .size()="+size()+
								 " ._head="+_head+
								 " ._tail="+_tail;
		arr += ": data=[\n";
		// now print the elems
		for (int i=_head; i!=_tail; i=next(i)) {
			arr += i+"->"+_buffer[i];
			arr += "\n";
		}
		arr += "])";
		return arr;
	}
	
	
  private int prev(int head) {
    return head>0 ? head-1 : _buffer.length - 1;
  }


  private int next(int tail) {
    return tail < _buffer.length-1 ? tail+1 : 0;
  }
}

