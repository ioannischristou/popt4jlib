package graph.packing;

import java.io.*;
import java.util.*;
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
public class PDAsynchInitDBBTreeCmd extends PDAsynchInitCmd {
	private String _graphfile;
	private String _paramsfile;
	private int _dbglvl;  // init to zero
	
	public PDAsynchInitDBBTreeCmd(String graphfile, String paramsfile) {
		_graphfile=graphfile;
		_paramsfile=paramsfile;
		// read params from the params-file residing on the client, for the shake of 
		// setting the debug-level for the server (not the worker) that will get 
		// this cmd only
		try {
			HashMap params = DataMgr.readPropsFromFile(paramsfile);
			_dbglvl = utils.Messenger.getInstance().getDebugLvl();
		}
		catch (Exception e) {
			utils.Messenger.getInstance().msg("reading params from "+_paramsfile+
				                                " failed.", 0);
		}
	}
	
	
	/**
	 * sets the debug level for <CODE>utils.Messenger</CODE> class, as it was
	 * read from the params at the client computer.
	 */
	public void applyOnServer() {
		utils.Messenger.getInstance().setDebugLevel(_dbglvl);
	}
	
	
	/**
	 * reads the params from the paramsfile passed in the constructor, and
	 * calls the <CODE>DBBTree.init()</CODE> method to initialize the 
	 * <CODE>DBBTree</CODE> data structure before the thread-pool of the executor
	 * is created and started.
	 * @param srv PDBatchTaskExecutorSrv unused
	 * @param ois ObjectInputStream unused
	 * @param oos ObjectOutputStream unused
	 */
	public void runProtocol(PDBatchTaskExecutorSrv srv, 
		                      ObjectInputStream ois, ObjectOutputStream oos) {
		try {
			// 1. _graphfile
			HashMap params = DataMgr.readPropsFromFile(_paramsfile);
			// 2.
			String pdahost = "localhost";
			if (params.containsKey("pdahost")) 
				pdahost = (String) params.get("pdahost");
			// 3.
			int pdaport = 7981;
			if (params.containsKey("pdaport")) 
				pdaport = ((Integer) params.get("pdaport")).intValue();
			// 4.
			String cchost = "localhost";
			if (params.containsKey("cchost")) cchost = (String) params.get("cchost");
			// 5.
			int ccport = 7899;
			if (params.containsKey("ccport")) 
				ccport = ((Integer) params.get("ccport")).intValue();
			// 6.
			String acchost = "localhost";
			if (params.containsKey("acchost")) 
				acchost = (String) params.get("acchost");
			// 7.
			int accport = 7900;
			if (params.containsKey("accport")) 
				accport = ((Integer) params.get("accport")).intValue();
			// 8.		
			String accnotificationshost = "localhost";
			if (params.containsKey("accnotificationshost")) 
				accnotificationshost = (String) params.get("accnotificationshost");				
			// 9.		
			int accnotificationsport = 9900;
			if (params.containsKey("accnotificationsport")) 
				accnotificationsport = 
					((Integer) params.get("accnotificationsport")).intValue();				
			// 10.
			Boolean localSearchB = (Boolean) params.get("localsearch");
			boolean localsearch = false;
			if (localSearchB!=null) localsearch = localSearchB.booleanValue();
			// 11.
			AllChromosomeMakerClonableIntf localsearchtype = 
				(AllChromosomeMakerClonableIntf) params.get("localsearchtype");
			// 12.
			Double ffD = (Double) params.get("ff");
			double ff = 0.85;
			if (ffD!=null) ff = ffD.doubleValue();
			// 13.
			Integer tlvlI = (Integer) params.get("tightenboundlevel");
			int tightenboundlevel = Integer.MAX_VALUE;
			if (tlvlI!=null && tlvlI.intValue()>=1) 
				tightenboundlevel = tlvlI.intValue();
			// 14.
			int maxitersinGBNS2A = 100000;
			Integer kmaxI = (Integer) params.get("maxitersinGBNS2A");
			if (kmaxI!=null && kmaxI.intValue()>0)
				maxitersinGBNS2A = kmaxI.intValue();
			// 15. 
			Boolean sortmaxsubsetsB = (Boolean) params.get("sortmaxsubsets");
			boolean sortmaxsubsets = false;
			if (sortmaxsubsetsB!=null)
				sortmaxsubsets = sortmaxsubsetsB.booleanValue();
			// 16.
			double avgpercextranodes2add = 0.0;
			Double avgpercextranodes2addD = 
				(Double) params.get("avgpercextranodes2add");
			if (avgpercextranodes2addD!=null)
				avgpercextranodes2add = avgpercextranodes2addD.doubleValue();
			// 17.
			Boolean useGWMIN24BN2AB = (Boolean) params.get("useGWMIN2criterion");
			boolean useGWMIN2criterion = false;
			if (useGWMIN24BN2AB!=null)
				useGWMIN2criterion = useGWMIN24BN2AB.booleanValue();
			// 18.
			Double expandlocalsearchfactorD = 
				(Double) params.get("expandlocalsearchfactor");
			double expandlocalsearchfactor = 1.0;
			if (expandlocalsearchfactorD!=null)
				expandlocalsearchfactor = expandlocalsearchfactorD.doubleValue();
			// 19.
			double minknownbound = Double.NEGATIVE_INFINITY;
			Double minknownboundD = (Double) params.get("minknownbound");
			if (minknownboundD!=null) 
				minknownbound = minknownboundD.doubleValue();
			// 20.
			int maxnodechildren = Integer.MAX_VALUE;
			Integer maxchildrenI = (Integer) params.get("maxnodechildren");
			if (maxchildrenI != null && maxchildrenI.intValue() > 0)
				maxnodechildren = maxchildrenI.intValue();
			// 21.
			DBBNodeComparatorIntf dbbnodecomparator = 
				(DBBNodeComparatorIntf) params.get("dbbnodecomparator");
			if (dbbnodecomparator == null) 
				dbbnodecomparator = new DefDBBNodeComparator();
			// 22.
			long seed = RndUtil.getInstance().getSeed();  
      // get whatever seed was set when props were read-in.
			// 23.
			int maxNodesAllowed = Integer.MAX_VALUE;
			Integer mnaI = (Integer) params.get("maxnodesallowed");
			if (mnaI!=null && mnaI.intValue()>0)
				maxNodesAllowed = mnaI.intValue();
			// 24.
			double initbound = 0.0;
			// 25. 
			_dbglvl = utils.Messenger.getInstance().getDebugLvl();

			DBBTree.init(_graphfile, null, initbound, 
									 pdahost, pdaport, cchost, ccport, 
									 acchost, accport, 
									 accnotificationshost, accnotificationsport,
									 localsearch, localsearchtype, ff, tightenboundlevel,
									 maxitersinGBNS2A, sortmaxsubsets,
									 avgpercextranodes2add, useGWMIN2criterion, 
									 expandlocalsearchfactor, minknownbound, maxnodechildren,
									 dbbnodecomparator, seed, false, 
									 maxNodesAllowed, _dbglvl);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
