package graph.packing;

import java.util.*;

/**
 * BoundedQueue: This class maintains a queue of BBNode's of finite length.
 * In particular, the queue allows of up to _maxLen BBNode's containing any
 * particular size of graph nodes to be maintained in the data structure.
 * When a BBNode n must be inserted and there is not enough space in the queue,
 * the "oldest" BBNode (the one that was inserted the earliest with the same
 * size as n) is removed from the structure so as to make room for the BBNode n.
 * Two Hashtables are maintained for efficiency in maintaining order of insertion
 * as well as speeding up look-ups in the "contains()" method
 * <p>Title: popt4jlib</p>
 * <p>Description: Optimizing Capacity in MANET graphs</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: AIT</p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class BoundedQueue {
  private Hashtable _pqueues;  // map<int BBNodeSize, DLCList<BBNodeBase> >
  private TreeSet _cnodes;  // TreeSet<BBNodeBase>
  private int _maxLen=0;


  /**
   * construct a BoundedQueue of finite length.
   * @param maxqueuelength int the max. length of this queue.
   */
  BoundedQueue(int maxqueuelength) {
    if (maxqueuelength>0) _maxLen = maxqueuelength;
    _pqueues = new Hashtable();
    _cnodes = new TreeSet();
  }


  /**
   * insert a BBNodeBase object in the queue. May have to remove an old BBNode*
   * object if there is not enough space.
   * @param n BBNode2
   */
  synchronized void insert(BBNodeBase n) {
    final int nsz = n.getNodes().size();
    DLCList nlist = (DLCList) _pqueues.get(new Integer(nsz));
    if (nlist==null) {
      nlist = new DLCList(_maxLen);
      _pqueues.put(new Integer(nsz), nlist);
    }
    BBNodeBase removed = (BBNodeBase) nlist.insert(n);
    _cnodes.add(n);
    if (removed!=null) _cnodes.remove(removed);
  }


  /**
   * check if the argument already exists in the queue.
   * @param n BBNodeBase
   * @return boolean
   */
  synchronized boolean contains(BBNodeBase n) {
    boolean res = (_cnodes!=null && _cnodes.contains(n));
    return res;
  }


  /**
   * clears the queue.
   */
  synchronized void clear() {
    _pqueues.clear();
    _cnodes.clear();
  }
}


class DLCList {
  private DLCListNode _head;
  private int _maxLen;

  DLCList(int maxlen) {
    _maxLen = maxlen;
    _head = new DLCListNode(null, null, null);
    _head._prev = _head;
    _head._next = _head;
    DLCListNode prev = _head;
    DLCListNode next = _head;
    for (int i=1; i<_maxLen; i++) {
      next = new DLCListNode(null, null, prev);
      prev._next=next;
      prev = next;
    }
    _head._prev = next;
    next._next = _head;
  }

  synchronized Object insert(BBNodeBase n) {
    DLCListNode oldest = _head._prev;
    Object pdata = oldest._data;
    oldest._data = n;
    _head = oldest;
    return pdata;
  }
}


class DLCListNode {
  DLCListNode _next;
  DLCListNode _prev;
  Object _data;

  DLCListNode(Object data, DLCListNode next, DLCListNode previous) {
    _data = data;
    _next = next;
    _prev = previous;
  }
}

