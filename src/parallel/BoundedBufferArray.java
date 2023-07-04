package parallel;

/**
 * a bounded-length FIFO buffer implemented as an Object Array. It's
 * meant to be used as a (faster) replacement for Vector structures in
 * XXXMsgPassingCoordinator family of classes.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class BoundedBufferArray implements BufferIntf {
  private Object[] _buffer;
  private int _head;
  private int _tail;

	
  /**
   * public constructor.
   * @param maxlen int
   */
  public BoundedBufferArray(int maxlen) {
    _buffer = new Object[maxlen];
    _head = -1;
    _tail = -1;
  }


  /**
   * add an object in the buffer (if there is space).
   * @param obj Object
   * @throws ParallelException if the buffer is full.
   */
  public synchronized void addElement(Object obj) throws ParallelException {
    if (_head!=-1 && prev(_head)==_tail)
      throw new ParallelException("cannot add any more data to this buffer");
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
   * @throws ParallelException if the buffer is empty.
   * @return Object
   */
  public synchronized Object remove() throws ParallelException {
    if (_head==-1)
      throw new ParallelException("buffer is empty");
    Object result = _buffer[_head];
		_buffer[_head] = null;  // itc 2014_09_07: avoid memory leak
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
	 * @throws ParallelException if the index is out of the range {0,...size()-1} 
	 */
	public synchronized Object remove(int i) throws ParallelException {
		if (i==0) return remove();  // remove the first element (pointed by _head)
		if (i<0 || i>=size()) throw new ParallelException("index "+i+" out of range");
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
   * @throws ParallelException if the index is out of range.
   * @return Object
   */
  public synchronized Object elementAt(int i) throws ParallelException {
    if (i<0 || i>=size()) 
			throw new ParallelException("index "+i+" out of range");
    int opos = _head+i;
    if (opos<_buffer.length) return _buffer[opos];
    else return _buffer[i - (_buffer.length - _head)];
  }
	
	
	/**
	 * check if the object o is contained in this buffer. Linear-time operation in 
	 * the number of objects in the buffer.
	 * @param o Object
	 * @return boolean true iff there exists an object in the buffer that equals
	 * the object o
	 */
	public synchronized boolean contains(Object o) {
		final int sz = size();
		for (int i=0; i<sz; i++) {
			try {
				if (elementAt(i).equals(o)) return true;
			}
			catch (ParallelException e) {
				// can never get here
			}
		}
		return false;
	}


  /**
   * returns the current total number of objects in the buffer.
   * @return int
   */
  public synchronized int size() {
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

	
	/**
	 * reset this buffer.
	 */
	public synchronized void reset() {
		for (int i=0; i<_buffer.length; i++) _buffer[i] = null;
		_head = -1;
		_tail = -1;
	}


  private int prev(int head) {
    return head>0 ? head-1 : _buffer.length - 1;
  }


  private int next(int tail) {
    return tail < _buffer.length-1 ? tail+1 : 0;
  }
}

