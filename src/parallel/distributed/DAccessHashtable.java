package parallel.distributed;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.Iterator;


/**
 * Implements a shared-memory-type hash-table that allows for distributed JVM
 * process clients to access a hash-table for get/put functionality.
 * The class supports multiple hash-tables identified by name.
 * When key-value pairs are put in a particular hash-table, the key acquires a
 * unique id, that can also be retrieved, but also serves to lock multiple
 * keys at once. When a hash-table is created, its maximum size must be declared
 * for efficiency purposes.
 * <p>Note: The class should alternatively be called
 * <CODE>DLongJohnTupleSpace</CODE> in honor of Linda's movie partner John
 * Holmes.</p>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DAccessHashtable {
	private final int _maxSize;
	private final String _name;
	private int _nextId;
	private final HashMap _keyIds = new HashMap();  // map<Object key, Integer id>
	// the key-ids is the reason that the lockAndGet(Object[] keys) method cannot
	// hang: keys are locked in ascending order of their ids, and thus there is
	// no way for starvation to occur
	private final Object[] _values;  // values store
	private final boolean[] _locked;  // values locks
	private final boolean[] _rmAllowed;  // auxiliary variable that controls when
	                                     // invoking remove() method may succeed
	private final TreeSet _purgedIds = new TreeSet();  // TreeSet<Integer> purged
	                                                   // ids that can be re-used

	private static HashMap _instances = new HashMap();  // map<String name,
	                                                    //  DAccessHashtable daht>


	/**
	 * sole constructor (private) declares the maximum size of the hashtable to
	 * be created in terms of number of keys it can hold.
	 * @param name String the name of this hash-table
	 * @param maxsize int (not in inches :-))
	 */
	private DAccessHashtable(String name, int maxsize) {
		_name=name;
		_maxSize=maxsize;
		_values = new Object[maxsize];
		_locked = new boolean[maxsize];  // init to false
		_rmAllowed = new boolean[maxsize];
		for (int i=0; i<_rmAllowed.length; i++) _rmAllowed[i]=true;
	}


	/**
	 * creates a new named instance.
	 * @param name String
	 * @param maxnumkeys int
	 * @return DAccessHashtable
	 * @throws IllegalArgumentException if a hash-table with the given name has
	 * already been created
	 */
	public static synchronized DAccessHashtable createInstance(String name,
		                                                         int maxnumkeys) {
		if (_instances.containsKey(name))
			throw new IllegalArgumentException("hash-table with name="+name+
				                                 " already exists.");
		DAccessHashtable daht = new DAccessHashtable(name,maxnumkeys);
		_instances.put(name, daht);
		return daht;
	}


	/**
	 * returns the DAccessHashtable with the given name.
	 * @param name String
	 * @return DAccessHashtable null if no object with this name has been created
	 */
	public static synchronized DAccessHashtable getInstance(String name) {
		return (DAccessHashtable) _instances.get(name);
	}


	/**
	 * standard <CODE>get()</CODE> operation.
	 * @param key Object must be Serializable and obey the rules for keys in the
	 * standard Collections framework
	 * @return Object value may be null if no such key exists in data structure
	 */
	public synchronized Object get(Object key) {
		Integer keyid = (Integer) _keyIds.get(key);
		if (keyid==null) return null;
		_rmAllowed[keyid.intValue()]=false;
		while (_locked[keyid.intValue()]) {
			try {
				wait();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		// ok, is now free to get its value
		_rmAllowed[keyid.intValue()]=true;
		return _values[keyid.intValue()];
	}


	/**
	 * implements the classical operation <CODE>put(key,value)</CODE>, with the
	 * twist that in case the particular key is locked, the operation will throw.
	 * @param key Object must be Serializable and obey the rules for keys in the
	 * standard Collections framework
	 * @param value Object must be Serializable
	 * @throws IllegalStateException if key is locked when attempting to put new
	 * value for it
	 */
	public synchronized void put(Object key, Object value)
		throws IllegalStateException {
		Integer keyid;
		keyid = (Integer) _keyIds.get(key);
		if (keyid==null) {
			if (_purgedIds.size()>0) {  // get keyid from re-cycled ids
				keyid = (Integer) _purgedIds.first();
				_purgedIds.remove(keyid);
			}
			else {
				if (_nextId==_maxSize)
					throw new IllegalStateException("no more slots available "+
						                              "to put this (key,value) pair");
				keyid = new Integer(_nextId++);
			}
		}
		if (_locked[keyid.intValue()])
			throw new IllegalStateException("key "+key+" is currently locked");
		// put the value in the object array
		_values[keyid.intValue()]=value;
	}


	/**
	 * get the unique integer identifier associated with this key.
	 * @param key Object
	 * @return Integer may be null if the passed key does not exist in the
	 * data structure
	 */
	public synchronized Integer getKeyId(Object key) {
		return (Integer) _keyIds.get(key);
	}


	/**
	 * first locks the given key and then returns the associated value. The lock
	 * is not released, until the method <CODE>putAndUnlock()</CODE> is invoked
	 * with the same key. The locks are NOT re-entrant.
	 * @param key Object
	 * @return Object value
	 * @throws IllegalArgumentException if key does not exist
	 */
	public synchronized Object lockAndGet(Object key) {
		Integer keyid = (Integer) _keyIds.get(key);
		if (keyid==null) throw new IllegalArgumentException("key does not exist");
		_rmAllowed[keyid.intValue()]=false;
		while (_locked[keyid.intValue()]) {
			try {
				wait();
			}
			catch(InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		_locked[keyid.intValue()]=true;
		_rmAllowed[keyid.intValue()]=true;
    // notice that though _rmAllowed[kid] is now true
		// the method remove(key) will still throw until
		// the putAndUnlock(key,value) method is also called.
		return _values[keyid.intValue()];
	}


	/**
	 * puts the related (key,value) pairs in the data-structure, and releases the
	 * locks associated with each key. If the key doesn't exist it simply returns.
	 * Notice that no check is made to ensure that the lock on the key was
	 * obtained before. The user of this class has the responsibility to make
	 * sure that this method is only invoked after a successful
	 * <CODE>lockAndGet()</CODE> operation.
	 * @param key Object
	 * @param value Object
	 * @throws IllegalArgumentException if key is null
	 */
	public synchronized void putAndUnlock(Object key, Object value) {
		if (key==null) throw new IllegalArgumentException("null key");
		Integer keyid = (Integer) _keyIds.get(key);
		if (keyid==null) return;
		_values[keyid.intValue()] = value;
		_locked[keyid.intValue()] = false;
		notifyAll();
	}


	/**
	 * same as <CODE>lockAndGet(key)</CODE> but works for multiple keys, which are
	 * locked in ascending order of their unique id, and then their associated
	 * value is added to the resulting array. The locks are NOT re-entrant.
	 * @param keys Object[]
	 * @return Object[] values for each of the keys
	 * @throws IllegalArgumentException if key doesn't exist
	 */
	public synchronized Object[] lockAndGet(Object[] keys) {
		if (keys==null || keys.length==0) {
			throw new IllegalArgumentException("null or empty argument passed id");
		}
		Object[] result = new Object[keys.length];
		TreeSet keyids = new TreeSet();
		HashMap id2keyspos = new HashMap();  // map<Integer keyid, Integer pos>
		for (int i=0; i<keys.length; i++) {
			Integer ki = (Integer) _keyIds.get(keys[i]);
			if (ki==null)
				throw new IllegalArgumentException("key "+keys[i]+" does not exist");
			_rmAllowed[ki.intValue()]=false;
			keyids.add(ki);
			id2keyspos.put(ki, new Integer(i));
		}
		Iterator it = keyids.iterator();
		while (it.hasNext()) {
			Integer kid = (Integer) it.next();
			while (_locked[kid.intValue()]) {
				try {
					wait();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			_locked[kid.intValue()]=true;
			_rmAllowed[kid.intValue()] = true;
      // notice that though _rmAllowed[kid] is now true
			// the method remove(key) will still throw until
			// the putAndUnlock(key,value) method is also called.
			int pos = ((Integer) id2keyspos.get(kid)).intValue();
			result[pos] = _values[kid.intValue()];
		}
		return result;
	}


	/**
	 * same as <CODE>putAndUnlock(key,value)</CODE> method, but for multiple
	 * (key,value) pairs.
	 * @param keys Object[]
	 * @param values Object[]
	 * @throws IllegalArgumentException if keys and/or values arrays lengths don't
	 * match
	 */
	public synchronized void putAndUnlock(Object[] keys, Object[] values) {
		if (keys==null || values==null || keys.length!=values.length)
			throw new IllegalArgumentException("keys and/or values arguments wrong");
		for (int i=0; i<keys.length; i++) {
			Object key = keys[i];
			Object val = values[i];
			Integer keyid = (Integer) _keyIds.get(key);
			if (keyid==null) {
				utils.Messenger.getInstance().msg("DAccessHashtable["+_name+
					                                "].putAndUnlock(): "+
					                                "key "+key+" does not exist.", 0);
				continue;
			}
			_values[keyid.intValue()] = val;
			_locked[keyid.intValue()] = false;
			notifyAll();
		}
	}


	/**
	 * removes the given key from this map.
	 * @param key Object
	 * @throws IllegalStateException if currently there is a lock on the given key
	 * or if there is a <CODE>*get()</CODE> operation under way, prohibiting
	 * the removal
	 */
	public synchronized void remove(Object key) throws IllegalStateException {
		if (key==null) return;
		Integer keyid = (Integer) _keyIds.get(key);
		if (keyid==null) return;
		if (_locked[keyid.intValue()] || !_rmAllowed[keyid.intValue()])
			throw new IllegalStateException("key "+key+
				                              " cannot be currently removed.");
		_keyIds.remove(key);
		_purgedIds.add(keyid);
		_values[keyid.intValue()]=null;
	}

}

