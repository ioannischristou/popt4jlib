package parallel.distributed;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * class provides client functionality to <CODE>DObjectsCacheSrv</CODE> server.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DObjectsCacheClt {
	private String _srvAddr;
	private int _srvPort;
	private Socket _s;
	
	public DObjectsCacheClt(String host, int port) {
		_srvAddr = host;
		_srvPort = port;
		try {
			_s = new Socket(_srvAddr,_srvPort);
		}
		catch (Exception e) {
			e.printStackTrace();
			_s = null;
		}
	}
	
	public synchronized void addObjects(Collection objects) 
		throws IOException, ClassNotFoundException {
		if (_s==null) {
			throw new IllegalStateException("no socket connection to server");
		}
		ObjectOutputStream oos = new ObjectOutputStream(_s.getOutputStream());
		oos.flush();
		ObjectInputStream ois = new ObjectInputStream(_s.getInputStream());
		oos.reset();
		oos.writeObject(new DObjCacheAddReq(objects));
		oos.flush();
		Object reply = ois.readObject();
		if (reply instanceof OKReply) {
			return;
		}
		else throw new IllegalStateException("failed to receive OKReply from Srv");
	}
	
	
	public synchronized Set getObjectsInServerCache() 
		throws IOException, ClassNotFoundException {
		if (_s==null) {
			throw new IllegalStateException("no socket connection to server");
		}
		ObjectOutputStream oos = new ObjectOutputStream(_s.getOutputStream());
		oos.flush();
		ObjectInputStream ois = new ObjectInputStream(_s.getInputStream());
		oos.reset();
		oos.writeObject(new DObjCacheGetAllReq());
		oos.flush();
		Set objects = (Set) ois.readObject();
		return objects;
	}
	
	
	public synchronized void clearServerCache() 
		throws IOException, ClassNotFoundException {
		if (_s==null) {
			throw new IllegalStateException("no socket connection to server");
		}
		ObjectOutputStream oos = new ObjectOutputStream(_s.getOutputStream());
		oos.flush();
		ObjectInputStream ois = new ObjectInputStream(_s.getInputStream());
		oos.reset();
		oos.writeObject(new DObjCacheClearReq());
		oos.flush();
		Object reply = ois.readObject();
		if (reply instanceof OKReply) {
			return;
		}
		else throw new IllegalStateException("failed to receive OKReply from Srv");		
	}
}

