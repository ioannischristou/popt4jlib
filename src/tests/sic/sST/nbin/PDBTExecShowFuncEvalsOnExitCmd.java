package tests.sic.sST.nbin;

import popt4jlib.FunctionBaseStatic;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import parallel.distributed.PDBatchTaskExecutorSrv;
import parallel.distributed.RRObject;
import utils.Messenger;


/**
 * Command class instructs workers to print out the total number of function 
 * evaluations of any function wrapped in the <CODE>FunctionBaseStatic</CODE>
 * wrapper at the end, when each worker in the network shuts-down its JVM.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2024</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBTExecShowFuncEvalsOnExitCmd extends RRObject {
	
	private final static Messenger _mger = Messenger.getInstance();
	
	
	public void runProtocol(PDBatchTaskExecutorSrv srv, ObjectInputStream ois,
		                      ObjectOutputStream oos) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
					long num_evals = FunctionBaseStatic.getEvalCount();
					_mger.msg("Total #Func-Evals="+num_evals,0);
				}});
	}
	
}
