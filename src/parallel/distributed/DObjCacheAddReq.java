package parallel.distributed;

import java.io.Serializable;
import java.util.Collection;

/**
 * helper class, not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class DObjCacheAddReq implements Serializable {
	// private static final long serialVersionUID=...L;
	private Collection _objects2Add;
	
	
	public DObjCacheAddReq(Collection objects) {
		_objects2Add = objects;
	}
	
	
	public Collection getObjects() {
		return _objects2Add;
	}
}
