package parallel;

/**
 * a bounded-length FIFO buffer implemented as an Object Array that is totally
 * unsynchronized. It's exactly the same as the <CODE>BoundedBufferArray</CODE>
 * class, only that's completely unsynchronized (and the exceptions thrown are 
 * now IllegalStateException or IndexOutOfBoundsException.)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class BoundedBufferArrayUnsynchronized {
  private Object[] _buffer;
  private int _head;
  private int _tail;
	
	
  /**
   * public constructor.
   * @param maxlen int
   */
  public BoundedBufferArrayUnsynchronized(int maxlen) {
    _buffer = new Object[maxlen];
    _head = -1;
    _tail = -1;
  }


  /**
   * add an object in the buffer (if there is space).
   * @param obj Object
   * @throws IllegalStateException if the buffer is full.
   */
  public void addElement(Object obj) throws IllegalStateException {
    if (_head!=-1 && prev(_head)==_tail)
      throw new IllegalStateException("cannot add any more data to this buffer");
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
	 * removes the <CODE>i</CODE>-th element in this array. Much slower than the
	 * other operations.
	 * @param i int 
	 * @return Object 
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
	 * removes the first argument from this buffer, starting the search from the 
	 * index provided by the 2nd argument (which can possibly be greater than
	 * the current size of the buffer, in which case the search goes from the end
	 * of the buffer backwards).
	 * @param p Object the object reference to be removed must exist in the buffer
	 * for the search employs the reference equality test (==)
	 * @param hintPos int a hint on the index position to start the search
	 * @return int the position where the object was found
	 * @throws IllegalArgumentException if the object is not found or 
	 * if hintPos &le 0 or if p is null
	 */
	public int remove(Object p, int hintPos) throws IllegalArgumentException {
		if (hintPos<0) 
			throw new IllegalArgumentException("2nd arg. cannot be less than zero");
		if (p==null) throw new IllegalArgumentException("1st arg. cannot be null");
		if (size()==0) throw new IllegalArgumentException("buffer is empty");
		final int sz = size();
		final boolean LEFT = false;
		final boolean RIGHT = true;
		boolean last = RIGHT;  // start going left
		if (hintPos>=sz) hintPos = sz-1;
		int offset = 1;
		int pos = hintPos;
		boolean cont = true;
		while (cont) {
			if (elementAt(pos)==p) {  // found it
				remove(pos);
				return pos;
			}
			// update pos to next position to examine
			if (last==LEFT) {
				pos = hintPos + offset;
				if (pos<=sz-1) {
					last = RIGHT;
					offset++;
				} else {  // reached right end
					if (hintPos<offset) cont = false;  // break;
					else {  // keep going left
						pos = hintPos-offset;
						last = RIGHT;
						offset++;
					}
				}
			} else {  // last==RIGHT
				pos = hintPos - offset;
				if (pos>=0) {
					last = LEFT;
					if (hintPos+offset>=sz) offset++;  // normally when going left, don't increment offset
				} else {  // reached left end
					if (hintPos+offset>=sz) cont = false;  // break;
					else {  // keep going right
						pos = hintPos+offset;
						last = LEFT;
						offset++;
					}
				}
			}
		}
		throw new IllegalArgumentException("Object not found");
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
	public int getMaxSize() {
		return _buffer.length;
	}

	
  private int prev(int head) {
    return head>0 ? head-1 : _buffer.length - 1;
  }


  private int next(int tail) {
    return tail < _buffer.length-1 ? tail+1 : 0;
  }
}

