package graph;

import parallel.ParallelException;
import java.io.Serializable;

/**
 * represents links between nodes in Graph objects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Link implements Serializable {
  // private final static long serialVersionUID = -7776018308783216013L;
  private final int _id;
  private final int _starta;
  private final int _enda;
  private double _weight;
	private Graph _g;  // needed to check for order of edges in the graph


  /**
   * public constructor. Assigns a default weight of one to this Link object.
	 * @param g Graph the graph object for which this link is a member
   * @param id int
   * @param in Node the start-node of this link
   * @param out Node the end-node of this link
   * @throws ParallelException if the current thread has a read-lock on the
   * in Node and there exists another thread that also has a read-lock on the
   * in Node.
   */
  Link(Graph g, int id, Node in, Node out) throws ParallelException {
    _id = id;
    _starta = in.getId();
    _enda = out.getId();
    _weight = 1.0;
    in.addOutLink(out, new Integer(id));
		_g = g;
  }


  /**
   * public constructor.
   * @param g Graph
   * @param id int
   * @param starta int
   * @param enda int
   * @param weight double
   * @throws ParallelException if the current thread has a read-lock on the
   * Node with id starta, and there exists another thread that simultaneously
   * holds the same read-lock.
   */
  Link(Graph g, int id, int starta, int enda, double weight)
      throws ParallelException {
    _id = id;
    _starta = starta;
    _enda = enda;
    _weight = weight;
    Node s = g.getNode(starta);
    Node e = g.getNode(enda);
    s.addOutLink(e, new Integer(id));
		_g = g;
  }


  /**
   * return this Link's id in [0, g.getNumArcs()-1]
   * @return int
   */
  public int getId() { return _id; }


  /**
   * return this Link's weight
   * @return double
   */
  public double getWeight() { return _weight; }


  /**
   * return the id of the starting Node of this Link in [0, g.getNumNodes()-1].
	 * Notice that the return value will be <CODE>_enda</CODE> if the graph in 
	 * which this link belongs has its links in "reversed-order" (via appropriate
	 * call). This method accesses <CODE>_g._isDirectionReverted</CODE> without
	 * any synchronization, so care must be given not to create any race
	 * conditions.
   * @return int
   */
  public int getStart() { 
		return _g._isDirectionReverted ? _enda : _starta; 
	}


  /**
   * return the id of the ending Node of this Link in [0, g.getNumNodes()-1].
	 * Notice that the return value will be <CODE>_starta</CODE> if the graph in 
	 * which this link belongs has its links in "reversed-order" (via appropriate
	 * call). This method accesses <CODE>_g._isDirectionReverted</CODE> without
	 * any synchronization, so care must be given not to create any race
	 * conditions.
   * @return int
   */
  public int getEnd() { 
		return _g._isDirectionReverted ? _starta : _enda; 
	}

	
	/**
	 * package-private method used only by the Graph class to modify link weights.
	 * Not part of the public API.
	 * @param w double
	 */
	void setWeight(double w) {
		_weight = w;
	}
}

