/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel.distributed;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * no-op initialization command for <CODE>PDBTExecInited[Clt|Srv|Wrk]</CODE>
 * objects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBTExecInitNoOpCmd extends RRObject {
	
  public void runProtocol(PDBatchTaskExecutorSrv srv,
                                   ObjectInputStream ois,
                                   ObjectOutputStream oos) {
		// no-op
	}

}
