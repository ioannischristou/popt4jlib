package graph.packing;

import popt4jlib.*;
import popt4jlib.LocalSearch.*;
import graph.*;
import utils.*;
import parallel.*;
import java.util.*;
import java.io.Serializable;

/**
 * Multi-threaded version of the 
 * <CODE>IntSetN1RXPFirstImprovingGraphAllMovesMaker</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntSetN1RXPFirstImprovingGraphAllMovesMakerMT  implements AllChromosomeMakerClonableIntf {
  private IntSet _soln=null;
	private int _k;
	
	
  /**
   * no-arg constructor sets problem type to 2-packing.
   */
  public IntSetN1RXPFirstImprovingGraphAllMovesMakerMT() {
    // default _k=2
		_k = 2;
  }
	
	
	/**
	 * set the k in the k-packing problem, to determine if it's for the 
	 * max weighted independent set problem or the 2-packing problem. 
	 * @param k 
	 */
  public IntSetN1RXPFirstImprovingGraphAllMovesMakerMT(int k) {
		_k = k;
  }
	
	
	/**
	 * cloning method.
	 * @return IntSetN1RXPFirstImprovingGraphAllMovesMakerMT
	 */
	public AllChromosomeMakerClonableIntf newInstance() {
		return new IntSetN1RXPFirstImprovingGraphAllMovesMakerMT(_k);
	}
	

  /**
   * implements the N_{-1+P} neighborhood for sets of integers.
   * @param chromosome Object Set&lt;Integer node-id&gt;
   * @param params HashMap must contain the following key-value pairs:
	 * <ul>
   * <li> &lt;"dls.intsetneighborhoodfilter", IntSetNeighborhoodFilterIntf filter&gt;
	 * <li> &lt;"dls.graph", Graph g&gt; the original problem graph
	 * </ul>
   * It may also optionally contain the following pairs:
	 * <ul>
   * <li> &lt;"dls.intsetneighborhoodmaxnodestotry", Integer max_nodes&gt; which 
	 * if present will indicate the maximum number of nodes to try to remove from 
	 * the current solution in the "1RXP" local search.
   * <li> &lt;"dls.numthreads", Integer num_threads&gt; which if present denotes 
	 * the number of threads that an a-synchronous batch-task executor will create 
	 * and use to execute the generated tasks, default is 1.
	 * <li> &lt;"dls.fpabte", FasterParallelAsynchronousBatchTaskExecutor extor&gt;
	 * which if present is the parallel executor to use, default is null. If such
	 * an executor is present, the "dls.numthreads" value is ignored.
	 * <li> &lt;"dls.lock_graph", Boolean val&gt; which if present and false, 
	 * indicates that the graph and its elements will be accessed without any
	 * synchronization. Default is true.
	 * </ul>
   * <br>The filter must specify what ints to be tried for addition to the set given
   * an int to be removed from the set.</br>
   * @throws OptimizerException if any of the parameters are incorrectly set
   * @return Vector Vector&lt;Set&lt;Integer nodeid&gt; &gt;
   */
  public Vector createAllChromosomes(Object chromosome, HashMap params) throws OptimizerException {
    if (chromosome==null) throw new OptimizerException("IntSetN1RXPFirstImprovingGraphAllMovesMaker.createAllChromosomes(): null chromosome");
    synchronized (this) {
      _soln=null;  // reset
    }
    boolean rlocked_graph = false;
    Graph g = null;
    FasterParallelAsynchBatchTaskExecutor executor = null;
    try {
			boolean do_rlock = true;
			try {
				Boolean drlB = (Boolean) params.get("dls.lock_graph");
				if (drlB!=null && drlB.booleanValue()==false) do_rlock=false;
			}
			catch (ClassCastException e) {
				e.printStackTrace();  // ignore
			}
      int numthreads = 1;
      try {
        Integer ntI = (Integer) params.get("dls.numthreads");
        if (ntI!=null && ntI.intValue()>0) numthreads = ntI.intValue();
      }
      catch (ClassCastException e) {
        e.printStackTrace();  // ignore
      }
			if (params.containsKey("dls.fpabte")) 
				executor = (FasterParallelAsynchBatchTaskExecutor) params.get("dls.fpabte");
			else executor = FasterParallelAsynchBatchTaskExecutor.
							newFasterParallelAsynchBatchTaskExecutor(numthreads);
      g = (Graph) params.get("dls.graph");
      if (do_rlock) {
				g.getReadAccess();
				rlocked_graph = true;
			}
      Set result = null;  // Set<IntSet>
      Set x0 = (Set) chromosome;
      //System.err.println("IntSetN1RXPFirstImprovingGraphAllMovesMakerMT.createAllChromosomes(): working w/ a soln of size="+x0.size());  
      IntSetNeighborhoodFilterIntf filter = (IntSetNeighborhoodFilterIntf)
          params.get("dls.intsetneighborhoodfilter");
      int max_nodes2try = Integer.MAX_VALUE;
      Integer mn2tI = null;
      try {
        mn2tI = (Integer) params.get("dls.intsetneighborhoodmaxnodestotry");
        if (mn2tI!=null) max_nodes2try = mn2tI.intValue();
      }
      catch (ClassCastException e) {
        e.printStackTrace();  // ignore
      }
      Iterator iter = x0.iterator();
      boolean cont=true;
      int max_batch_size = 10;
      int count = max_batch_size;
      Vector tasks = new Vector();
      while (iter.hasNext() && cont) {
        if (--max_nodes2try==0) return null;  // done with the search
        Integer id = (Integer) iter.next();
        //System.err.println("IntSetN1RXPFirstImprovingGraphAllMovesMakerMT.createAllChromosomes(): working w/ id="+id);
        Set rmids = new IntSet();
        rmids.add(id);
        List tryids = filter.filter(rmids, x0, params);  // List<Integer>
        if (tryids!=null) {
          IntSet xnew = new IntSet(x0);
          xnew.removeAll(rmids);
          if (!searchCompleted()) {
            SetCreatorTaskObject scto = new SetCreatorTaskObject(xnew, id.intValue(), tryids, 2, params);
            tasks.addElement(scto);
            if (--count==0) {  // batch is full, submit
              executor.executeBatch(tasks);
              tasks = new Vector();
            }
          }
          else {
            result = new TreeSet();
            result.add(_soln);
            cont = false;
          }
        }
      }
      if (cont==true && tasks.size()>0) {  // some tasks remain
        ConditionCounter cond = new ConditionCounter(tasks.size());
        for (int i=0; i<tasks.size(); i++) {
          SetCreatorTaskObject ti = (SetCreatorTaskObject) tasks.elementAt(i);
          ti.setCondCounter(cond);  // add the counter to each task to be executed
        }
        executor.executeBatch(tasks);
        // now wait until those tasks run
        cond.await();
        if (searchCompleted()) {
          result = new TreeSet();
          result.add(_soln);
        }
      }
      // convert Set<IntSet> to Vector<IntSet>
      Vector res = null;
      if (result!=null) {
        res = new Vector(result);
        //System.err.println("IntSetN1RXPAllFirstImprovingGraphMovesMaker.createAllChromosomes(): in total "+res.size()+" moves generated.");
      }
      return res;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("IntSetN1RXPFirstImprovingGraphAllMovesMaker.createAllChromosomes(): failed");
    }
    finally {
      if (rlocked_graph) {
        try {
          g.releaseReadAccess();
        }
        catch (ParallelException e) {  // can never get here
          e.printStackTrace();
          throw new OptimizerException("insanity: ParallelException should not have been thrown");
        }
      }
			if (params.get("dls.fpabte")==null) {
				try {
					executor.shutDown();  // executor was created inside this call
				}
				catch (ParallelException e) {  // can never get here
					e.printStackTrace();
					throw new OptimizerException("insanity: ParallelException should not have been thrown");
				}
	      catch (ParallelExceptionUnsubmittedTasks e) {  // can never get here
		      e.printStackTrace();
			    throw new OptimizerException("insanity: ParallelExceptionUnsubmittedTasks should not have been thrown:"+
				                               " total of "+e.getUnsubmittedTasks().size()+" pills reported failing?");
				}
			}
    }
  }


  /**
   * hook method in the context of the Template Method Design Pattern.
   * Sub-classes with more domain knowledge may override this method to modify
   * the behavior of this move-maker. This method implements a depth-first
   * search on the space of neighbors to find the first improving solution
   * which it returns immediately (the soln is a maximally-improving soln in the
   * DF fashion). The implementation is recursive.
   * @param res Set // IntSet
   * @param rmid Integer
   * @param tryids List // List&lt;Integer&gt;
   * @param maxcard int
   * @param params HashMap must contain the pair &lt;"dls.graph", Graph g&gt;, 
	 * and may optionally contain the pair &lt;"dls.lock_graph", Boolean val&gt; 
	 * which if present and val is false, indicates no read-locking of the graph 
	 * elements
   * @return Set // IntSet
   */
  protected Set createSet(Set res, int rmid, List tryids, int maxcard, HashMap params) {
    IntSet x = (IntSet) res;
    for (int i=0; i<tryids.size(); i++) {
      Integer tid = (Integer) tryids.get(i);
      if (tid.intValue()!=rmid) {
        if (x.contains(tid)==false && isOK2Add(tid, x, params)) {
          IntSet x2 = new IntSet(x);
          x2.add(tid);
          Vector tryids2 = new Vector(tryids);
          tryids2.remove(i);  // remove the i-th element
          Set res3 = createSet(x2, rmid, tryids2, maxcard-1, params);
          if (res3!=null) return res3;
        }
      }
    }
    if (maxcard<=0) return res;
    else return null;
  }


  /**
   * hook method in the context of the Template Method Design Pattern.
   * Overrides this method to modify the behavior of the base move-maker.
   * @param tid Integer
   * @param x IntSet
   * @param params HashMap must contain the pair &lt;"dls.graph, Graph g&gt;, 
	 * and may optionally contain the pair &lt;"dls.lock_graph, Boolean val&gt; 
	 * which if present and val is false, indicates no read-locking of the graph 
	 * elements.
   * @return boolean
   */
  protected boolean isOK2Add(Integer tid, IntSet x, HashMap params) {
    try {
      Graph g = (Graph) params.get("dls.graph");
			boolean do_rlock = true;
			Object drlO = params.get("dls.lock_graph");
			if (drlO!=null && drlO instanceof Boolean && ((Boolean) drlO).booleanValue()==false)
				do_rlock = false;
      Node n = do_rlock ? g.getNode(tid.intValue()) : g.getNodeUnsynchronized(tid.intValue());
      Set nodes = new TreeSet();
      Iterator xiter = x.iterator();
      while (xiter.hasNext()) {
				Node nx = do_rlock ? g.getNode(((Integer) xiter.next()).intValue()) :
								             g.getNodeUnsynchronized(((Integer) xiter.next()).intValue());
        nodes.add(nx);
      }
      return isFree2Cover(n, nodes, _k, do_rlock);
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }


  /**
   * check if node nj can be set to one when the nodes in active are also set.
   * @param nj Node
   * @param active Set  // Set&lt;Node&gt;
	 * @param k int
	 * @param lock boolean // if false then accesses to graph elements will be 
	 * unsynchronized
   * @return boolean // true iff nj can be added to active
   * @throws ParallelException
   */
  private static boolean isFree2Cover(Node nj, Set active, int k, boolean do_rlock) 
	    throws ParallelException {
    if (active==null) return true;
    if (active.contains(nj)) return false;
		/* slow
		Set activated = new HashSet(active);
    Iterator it = active.iterator();
    while (it.hasNext()) {
      Node ni = (Node) it.next();
      Set nnbors = k==2 ? 
							(do_rlock ? ni.getNNbors() : ni.getNNborsUnsynchronized()) : 
							(do_rlock ? ni.getNbors() : ni.getNborsUnsynchronized());
      activated.addAll(nnbors);
    }
    return !activated.contains(nj);
		*/
		// /* faster: no need for HashSet's creation
		Set nborsj = k==1 ? (do_rlock ? nj.getNbors() : nj.getNborsUnsynchronized()) :
						            (do_rlock ? nj.getNNbors() : nj.getNNborsUnsynchronized());
		Iterator itj = nborsj.iterator();
		while (itj.hasNext()) {
			Node nnj = (Node) itj.next();
			if (active.contains(nnj)) return false;
		}
		// */
    return true;
  }


  private synchronized boolean searchCompleted() {
    return _soln!=null;
  }


  private synchronized void setSoln(IntSet x) {
    if (_soln==null) _soln = x;
  }


	/**
	 * nested auxiliary class encapsulates the tasks to be sent to the executor 
	 * created in each call of the <CODE>createAllChromosomes()</CODE> method. 
	 * Not part of the public API.
	 */
  class SetCreatorTaskObject implements TaskObject {
    // private final static long serialVersionUID =  -1079591307175127226L;
    private Set _x0;
    private int _id;
    private List _tryids;
    private int _maxCard;
    private HashMap _params;
    private boolean _isDone=false;
    private ConditionCounter _cond = null;

    SetCreatorTaskObject(IntSet x, int id, List tryids, int maxcard, HashMap params) {
      _x0 = x;
      _id = id;
      _tryids = tryids;
      _maxCard = maxcard;
      _params = params;
    }

    public synchronized void setDone() { _isDone = true; }
    synchronized void setCondCounter(ConditionCounter c) { _cond = c; }

    public synchronized boolean isDone() { return _isDone; }
    public Serializable run() {
      Set res = createSet(_x0, _id, _tryids, _maxCard, _params);
      if (res!=null) setSoln((IntSet) res);
      setDone();
      if (_cond!=null) _cond.increment();
      return this;
    }
    public void copyFrom(TaskObject other) throws IllegalArgumentException {
      throw new IllegalArgumentException("copyFrom(other): operation not supported");
    }
  }

}

