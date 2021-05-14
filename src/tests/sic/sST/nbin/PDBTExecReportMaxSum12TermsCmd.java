package tests.sic.sST.nbin;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import parallel.distributed.PDBTExecCmd;
import parallel.distributed.PDBatchTaskExecutorSrv;
import utils.Messenger;


/**
 * Command class instructs workers to print out the maximum number of added 
 * terms in the <CODE>sSTCnbin.sum[1|2]()</CODE> methods.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBTExecReportMaxSum12TermsCmd extends PDBTExecCmd {
	/**
	 * print out the maximum number of added terms in the 
	 * <CODE>sSTCnbin.sum[1|2]()</CODE> methods.
	 * @param srv PDBatchTaskExecutorSrv
	 * @param ois ObjectInputStream
	 * @param oos ObjectOutputStream
	 */
	public void runProtocol(PDBatchTaskExecutorSrv srv,
                          ObjectInputStream ois,
                          ObjectOutputStream oos) {
		final Messenger mger = Messenger.getInstance();
		String str = "MAX #TERMS needed for sum[1|2]() in sSTCnbin.eval(): "+
			           sSTCnbin.getMaxAddedTermsInSum12();
		mger.msg(str, 0);
	}
}
