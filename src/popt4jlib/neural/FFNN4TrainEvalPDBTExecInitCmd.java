package popt4jlib.neural;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import parallel.distributed.PDBatchTaskExecutorSrv;
import parallel.distributed.RRObject;


/**
 * auxiliary class used to initialize workers participating in a distributed 
 * network of machines evaluating the function <CODE>FFNN4Train</CODE>. NOT part 
 * of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class FFNN4TrainEvalPDBTExecInitCmd extends RRObject {
	private String _datafile;
	private String _labelsfile;

	
	/**
	 * sole constructor is not public.
	 * @param datafile String
	 * @param labelsfile String
	 */
	FFNN4TrainEvalPDBTExecInitCmd(String datafile, String labelsfile) {
		_datafile = datafile;
		_labelsfile = labelsfile;
	}
	
	
	/**
	 * read the training and label data from the files provided at construction 
	 * time.
	 * @param srv PDBatchTaskExecutorSrv unused
	 * @param ois ObjectInputStream unused
	 * @param oos ObjectOutputStream unused
	 * @throws IOException 
	 */
	public void runProtocol(PDBatchTaskExecutorSrv srv,
                          ObjectInputStream ois,
                          ObjectOutputStream oos) throws IOException {
		TrainData.readTrainingDataFromFiles(_datafile, _labelsfile);
	}
}

