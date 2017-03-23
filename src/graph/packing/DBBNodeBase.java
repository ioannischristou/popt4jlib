/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package graph.packing;

import parallel.TaskObject;
import java.util.Set;


/**
 * represents a node in the distributed (cluster parallel) version of the 
 * B&amp;B tree of the hybrid B&amp;B - GASP scheme for packing problems. Not 
 * part of the public API. Essentially, this class factors-out the functionality
 * required of <CODE>DBBNode*</CODE> objects by the "main" classes 
 * <CODE>DBBTree</CODE> and <CODE>DBBGASPPacker</CODE> in this package, and lets
 * sub-classes implement the way the node-ids will be stored/retrieved/edited.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0 
 */
public abstract class DBBNodeBase implements Comparable, TaskObject {
	/**
	 * defines a multiplicative fudge factor by which the "best cost" of
	 * graph-nodes is allowed to be "over" so as to still be considered "best" for
	 * inclusion in the <CODE>getBestNodes2Add(DBBNode)</CODE> method and further
	 * consideration. This value is allowed to change until a call to
	 * <CODE>disallowFFChanges()</CODE> is made by any thread.
	 */
	private static double _ff = 0.85;
	/**
	 * guard member to ensure the value of _ff doesn't change after DBBNode
	 * objects are created.
	 */
	private static boolean _ffAllowed2Change = true;

	
	/**
	 * compute the cost of this DBBNode object.
	 * @return double
	 */
	protected abstract double getCost();
	
	
	/**
	 * sub-classes such as <CODE>DBBNode1</CODE> return their corresponding data
	 * member. Other sub-classes however that implement the store the node-ids as
	 * bit-vectors, need to construct a Set of Integers and return it.
	 * @return Set
	 */
	protected abstract Set getNodeIdsAsSet();
	
	
	/**
	 * compute a valid bound on the cost (really, the profit) of this solution, ie
	 * a number above which any complete solution to MWIS containing all the nodes
	 * that this solution contains, cannot go.
	 * @return double
	 */
	protected abstract double getBound();
	
	
	/**
	 * disallow changes to <CODE>_ff</CODE>. Called only from the
	 * <CODE>DBBGASPAcker</CODE> class.
	 */
	static synchronized void disallowFFChanges() {
		_ffAllowed2Change = false;
	}


	/**
	 * set the value of <CODE>_ff</CODE> field. Called only from the
	 * <CODE>DBBGASPPacker</CODE> class before any DBBNode objects are constructed
	 * or executed on threads.
	 * @param val double
	 */
	static synchronized void setFF(double val) {
		if (_ffAllowed2Change) _ff = val;
	}
	
	
	/**
	 * get the value of the fudge factor _ff.
	 * @return double
	 */
	static double getFF() {
		return _ff;
	}

}
