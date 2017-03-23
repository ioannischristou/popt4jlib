package graph;

import utils.DataMgr;
import java.util.HashMap;
import java.lang.ref.SoftReference;

/**
 * Graph object (memory) manager.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class GraphCacheMgr {
	private static HashMap _graphs = new HashMap();  // map<String filename, 
	                                                 //     SoftReference<Graph>g>
	
	/**
	 * get the <CODE>graph.Graph</CODE> object that the given file represents. The
	 * graph is loaded in main memory as a <CODE>SoftReference</CODE> for memory
	 * management efficiency purposes. This however implies that modifications to
	 * the returned object may not persist on subsequent invocations of this 
	 * method with the same argument.
	 * @param filename String
	 * @return Graph
	 */
	public static synchronized Graph getGraph(String filename) {
		if (_graphs.containsKey(filename)) {
			SoftReference gref = (SoftReference) _graphs.get(filename);
			Graph g = (Graph) gref.get();
			if (g!=null) return g;
		}
		try {
			Graph g = DataMgr.readGraphFromFile2(filename);
			_graphs.put(filename, new SoftReference(g));
			return g;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * same as the <CODE>getGraph(filename)</CODE> method, except that when this
	 * method is invoked, the Graph object will not be stored in memory as a 
	 * <CODE>SoftReference</CODE> but will instead be stored as a regular object,
	 * thus enabling modifications to it to persist in memory and be present when
	 * subsequent calls of this method with the same argument are made.
	 * @param filename String
	 * @return Graph
	 */
	public static synchronized Graph getGraphNoRef(String filename) {
		if (_graphs.containsKey(filename)) {
			Object gr = _graphs.get(filename);
			if (gr instanceof SoftReference) {
				SoftReference gref = (SoftReference) _graphs.get(filename);
				Graph g = (Graph) gref.get();
				if (g!=null) {
					_graphs.put(filename, g);
					return g;
				}
			}
			else return (Graph) gr;
		}
		try {
			Graph g = DataMgr.readGraphFromFile2(filename);
			_graphs.put(filename, g);
			return g;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
