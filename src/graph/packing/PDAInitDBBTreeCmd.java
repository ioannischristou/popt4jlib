package graph.packing;

import java.io.*;
import java.util.*;
import graph.Graph;
import parallel.ParallelException;
import parallel.distributed.PDAsynchInitCmd;
import parallel.distributed.PDBatchTaskExecutorSrv;
import popt4jlib.AllChromosomeMakerClonableIntf;
import utils.DataMgr;
import utils.RndUtil;

/**
 * auxiliary class used for the initialization of distributed 
 * <CODE>PDAsynchBatchTaskExecutorWrk</CODE> objects running code for the 
 * distributed version of the BBGASPPacker. Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDAInitDBBTreeCmd extends PDAsynchInitCmd {
	private String _graphfile;
	private double _initbound;
	private String _pdahost;
	private int _pdaport;
	private String _cchost;
	private int _ccport;
	private String _acchost;
	private int _accport;
	private String _accnotificationshost;
	private int _accnotificationsport;
	private boolean _localsearch=false;
	private AllChromosomeMakerClonableIntf _localsearchtype=null;
	private double _ff=0.85;
	private int _tightenboundlevel=Integer.MAX_VALUE;
	private int _maxitersinGBNS2A=100000;
	private boolean _sortmaxsubsets=false;
	private double _avgpercextranodes2add=0.0;
	private boolean _useGWMIN2criterion=false;
	private double _expandlocalsearchfactor=1.0;
	private double _minknownbound=Double.NEGATIVE_INFINITY;
	private int _maxnodechildren=Integer.MAX_VALUE;
	private DBBNodeComparatorIntf _dbbnodecomparator=null;
	private long _seed=0;
	private int _maxNodesAllowed = Integer.MAX_VALUE;
	private int _dbglvl=Integer.MAX_VALUE;
	
	public PDAInitDBBTreeCmd(String graphfile, String paramsfile) throws IOException {
		// 1.
		_graphfile = graphfile;
		HashMap params = DataMgr.readPropsFromFile(paramsfile);
		// 2.
		_pdahost = "localhost";
		if (params.containsKey("pdahost")) _pdahost = (String) params.get("pdahost");
		// 3.
		_pdaport = 7981;
		if (params.containsKey("pdaport")) _pdaport = ((Integer) params.get("pdaport")).intValue();
		// 4.
		_cchost = "localhost";
		if (params.containsKey("cchost")) _cchost = (String) params.get("cchost");
		// 5.
		_ccport = 7899;
		if (params.containsKey("ccport")) _ccport = ((Integer) params.get("ccport")).intValue();
		// 6.
		_acchost = "localhost";
		if (params.containsKey("acchost")) _acchost = (String) params.get("acchost");
		// 7.
		_accport = 7900;
		if (params.containsKey("accport")) _accport = ((Integer) params.get("accport")).intValue();
		// 8.		
		_accnotificationshost = "localhost";
		if (params.containsKey("accnotificationshost")) _accnotificationshost = (String) params.get("accnotificationshost");				
		// 9.		
		_accnotificationsport = 9900;
		if (params.containsKey("accnotificationsport")) _accnotificationsport = ((Integer) params.get("accnotificationsport")).intValue();				
    // 10.
		Boolean localSearchB = (Boolean) params.get("localsearch");
		_localsearch = false;
		if (localSearchB!=null) _localsearch = localSearchB.booleanValue();
		// 11.
		_localsearchtype = (AllChromosomeMakerClonableIntf) params.get("localsearchtype");
		// 12.
		Double ffD = (Double) params.get("ff");
		_ff = 0.85;
		if (ffD!=null) _ff = ffD.doubleValue();
    // 13.
		Integer tlvlI = (Integer) params.get("tightenboundlevel");
		_tightenboundlevel = Integer.MAX_VALUE;
		if (tlvlI!=null && tlvlI.intValue()>=1) 
			_tightenboundlevel = tlvlI.intValue();
    // 14.
		// _maxitersinGBNS2A = 100000;
		Integer kmaxI = (Integer) params.get("maxitersinGBNS2A");
    if (kmaxI!=null && kmaxI.intValue()>0)
			_maxitersinGBNS2A = kmaxI.intValue();
		// 15. 
		Boolean sortmaxsubsetsB = (Boolean) params.get("sortmaxsubsets");
		_sortmaxsubsets = false;
		if (sortmaxsubsetsB!=null)
			_sortmaxsubsets = sortmaxsubsetsB.booleanValue();
    // 16.
		Double avgpercextranodes2addD = (Double) params.get("avgpercextranodes2add");
    if (avgpercextranodes2addD!=null)
			_avgpercextranodes2add = avgpercextranodes2addD.doubleValue();
		// 17.
		Boolean useGWMIN24BN2AB = (Boolean) params.get("useGWMIN2criterion");
		_useGWMIN2criterion = false;
		if (useGWMIN24BN2AB!=null)
			_useGWMIN2criterion = useGWMIN24BN2AB.booleanValue();
		// 18.
		Double expandlocalsearchfactorD = (Double) params.get("expandlocalsearchfactor");
		_expandlocalsearchfactor = 1.0;
		if (expandlocalsearchfactorD!=null)
			_expandlocalsearchfactor = expandlocalsearchfactorD.doubleValue();
		// 19.
		_minknownbound = Double.NEGATIVE_INFINITY;
		Double minknownboundD = (Double) params.get("minknownbound");
		if (minknownboundD!=null) 
			_minknownbound = minknownboundD.doubleValue();
		// 20.
		_maxnodechildren = Integer.MAX_VALUE;
    Integer maxchildrenI = (Integer) params.get("maxnodechildren");
    if (maxchildrenI != null && maxchildrenI.intValue() > 0)
			_maxnodechildren = maxchildrenI.intValue();
    // 21.
		_dbbnodecomparator = (DBBNodeComparatorIntf) params.get("dbbnodecomparator");
		if (_dbbnodecomparator == null) _dbbnodecomparator = new DefDBBNodeComparator();
		// 22.
		_seed = RndUtil.getInstance().getSeed();  // get whatever seed was set when props were read-in.
		// 23.
		_maxNodesAllowed = Integer.MAX_VALUE;
		Integer mnaI = (Integer) params.get("maxnodesallowed");
		if (mnaI!=null && mnaI.intValue()>0)
			_maxNodesAllowed = mnaI.intValue();
		// 24.
		_initbound = 0.0;
		// 25. 
		_dbglvl = utils.Messenger.getInstance().getDebugLvl();
	}
	
	
	/**
	 * many (25) argument constructor.
	 * @param graphfile String the path-name of the file containing the graph.
	 * @param initbound
	 * @param pdahost
	 * @param pdaport
	 * @param cchost
	 * @param ccport
	 * @param acchost
	 * @param accport
	 * @param accnotificationshost
	 * @param accnotificationsport
	 * @param localsearch
	 * @param localsearchtype
	 * @param ff
	 * @param tightenboundlevel
	 * @param maxitersinGBNS2A
	 * @param sortmaxsubsets
	 * @param avgpercextranodes2add
	 * @param useGWMIN2criterion
	 * @param expandlocalsearchfactor
	 * @param minknownbound
	 * @param maxnodechildren
	 * @param dbbnodecomparator
	 * @param seed
	 * @param maxnodesallowed
	 * @param dbglvl 
	 */
	public PDAInitDBBTreeCmd(String graphfile, double initbound, 
		                       String pdahost, int pdaport, 
													 String cchost, int ccport, 
													 String acchost, int accport, 
													 String accnotificationshost, int accnotificationsport,
													 boolean localsearch,
													 AllChromosomeMakerClonableIntf localsearchtype,
													 double ff,
													 int tightenboundlevel,
													 int maxitersinGBNS2A,
													 boolean sortmaxsubsets,
													 double avgpercextranodes2add,
													 boolean useGWMIN2criterion,
													 double expandlocalsearchfactor,
													 double minknownbound,
													 int maxnodechildren,
													 DBBNodeComparatorIntf dbbnodecomparator,
													 long seed,
													 int maxnodesallowed,
													 int dbglvl) {
		_graphfile = graphfile;
		_initbound = initbound;
		_pdahost = pdahost;
		_pdaport = pdaport;
		_cchost = cchost;
		_ccport = ccport;
		_acchost = acchost;
		_accport = accport;
		_accnotificationshost = accnotificationshost;
		_accnotificationsport = accnotificationsport;
		_localsearch = localsearch;
		_localsearchtype = localsearchtype;
		_ff = ff;
		_tightenboundlevel = tightenboundlevel;
		_maxitersinGBNS2A = maxitersinGBNS2A;
		_sortmaxsubsets = sortmaxsubsets;
		_avgpercextranodes2add = avgpercextranodes2add;
		_useGWMIN2criterion = useGWMIN2criterion;
		_expandlocalsearchfactor = expandlocalsearchfactor;
		_minknownbound = minknownbound;
		_maxnodechildren = maxnodechildren;
		_dbbnodecomparator = dbbnodecomparator;
		_seed = seed;
		_maxNodesAllowed = maxnodesallowed;
		_dbglvl = dbglvl;
	}
	
	
	/**
	 * sets the debug level for <CODE>utils.Messenger</CODE> class.
	 */
	public void applyOnServer() {
		utils.Messenger.getInstance().setDebugLevel(_dbglvl);
	}
	
	
	/**
	 * calls the <CODE>DBBTree.init()</CODE> method to initialize the 
	 * <CODE>DBBTree</CODE> data structure before the thread-pool of the executor
	 * is created and started.
	 * @param srv PDBatchTaskExecutorSrv unused
	 * @param ois ObjectInputStream unused
	 * @param oos ObjectOutputStream unused
	 */
	public void runProtocol(PDBatchTaskExecutorSrv srv, ObjectInputStream ois, ObjectOutputStream oos) {
		DBBTree.init(_graphfile, null, _initbound, 
			           _pdahost, _pdaport, _cchost, _ccport, 
								 _acchost, _accport, 
								 _accnotificationshost, _accnotificationsport,
								 _localsearch,_localsearchtype, _ff, _tightenboundlevel,
								 _maxitersinGBNS2A, _sortmaxsubsets,
								 _avgpercextranodes2add, _useGWMIN2criterion, 
								 _expandlocalsearchfactor, _minknownbound, _maxnodechildren,
								 _dbbnodecomparator, _seed, false, 
								 _maxNodesAllowed, _dbglvl);
	}
	
}
