package parallel;

/**
 * a bounded-length FIFO buffer implemented as a cyclic doubly-linked list. It's
 * meant to be used as a (faster) replacement for Vector structures in
 * XXXMsgPassingCoordinator family of classes.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class BoundedBuffer implements BufferIntf {
  private DLCListNode _head;
  private DLCListNode _tail;
  private int _maxLen;
  private DLCListNode[] _buffer;

  /**
   * public constructor.
   * @param maxlen int
   */
  public BoundedBuffer(int maxlen) {
    _maxLen = maxlen;
    _buffer = new DLCListNode[maxlen];
    _buffer[0] = new DLCListNode(null, null, null, 0);
    _head = _buffer[0];
    _head._prev = _head;
    _head._next = _head;
    DLCListNode prev = _head;
    DLCListNode next = _head;
    for (int i=1; i<_maxLen; i++) {
      _buffer[i] = new DLCListNode(null, null, prev, i);
      next = _buffer[i];
      prev._next=next;
      prev = next;
    }
    _head._prev = next;
    next._next = _head;
    _head = null; _tail = null; // empty buffer
  }


  /**
   * add an object in the buffer (if there is space).
   * @param obj Object
   * @throws ParallelException if the buffer is full.
   */
  public synchronized void addElement(Object obj) throws ParallelException {
    if (_head != null && _head._prev == _tail)
      throw new ParallelException("cannot add any more data to this buffer");
    if (_head==null) {
      _head = _buffer[0];
      _tail = _head;
      _head._data = obj;
      return;
    } else {
      _tail = _tail._next;
      _tail._data = obj;
      return;
    }
  }


  /**
   * remove the first object that was inserted in the buffer.
   * @throws ParallelException if the buffer is empty.
   * @return Object
   */
  public synchronized Object remove() throws ParallelException {
    if (_head==null)
      throw new ParallelException("buffer is empty");
    Object result = _head._data;
		_head._data = null;  // itc 2014_09_07: avoid memory leak
    if (_head==_tail) {  // buffer is now empty
      _head = null;
      _tail = null;
      return result;
    }
    _head = _head._next;
    return result;
  }


  /**
   * return the datum at position i.
   * @param i int
   * @throws ParallelException if the index is out of range.
   * @return Object
   */
  public synchronized Object elementAt(int i) throws ParallelException {
    if (i<0 || i>= size()) throw new ParallelException("index "+i+"out of range");
    int opos = _head._pos+i;
    if (opos<_maxLen) return _buffer[opos]._data;
    else return _buffer[i - (_maxLen - _head._pos)]._data;
  }


  /**
   * returns the current total number of objects in the buffer.
   * @return int
   */
  public synchronized int size() {
    if (_head==null) return 0;
    if (_head._pos<=_tail._pos) return _tail._pos - _head._pos + 1;
    else return _maxLen - (_head._pos - _tail._pos) + 1;
  }
	
	
	/**
	 * reset this buffer.
	 */
	public synchronized void reset() {
		for (int i=0; i<_buffer.length; i++) {
			_buffer[i]._data = null;  // ensure no memory leaks
		}
		_head = null;
		_tail = null;
	}

	
	/**
	 * auxiliary inner-class, not part of the public API.
	 */
	class DLCListNode {
		DLCListNode _next;
		DLCListNode _prev;
		int _pos;
		Object _data;

		public DLCListNode(Object data, DLCListNode next, DLCListNode previous, int i) {
			_data = data;
			_next = next;
			_prev = previous;
			_pos = i;
		}
	}

}

