package popt4jlib.neural;

import utils.DataMgr;
import utils.Messenger;

/**
 * Auxiliary class that serves as a place-holder for caching data for training
 * neural networks, and making them available via static method calls. Not part
 * of the public API.
 * <p>Notes:
 * <ul>
 * <li>2021-09-24: added one more synchronized method to read training data only
 * if at least one training cache is null.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class TrainData {
	private static double[][] _trainData = null;
	private static double[] _trainLabels = null;
	

	/**
	 * read train data and labels from respective files.
	 * @param datafile String
	 * @param labelsfile String
	 * @throws java.io.IOException 
	 */
	static synchronized void readTrainingDataFromFiles(String datafile, 
		                                                 String labelsfile) 
		throws java.io.IOException {
		Messenger mger = Messenger.getInstance();
		mger.msg("TrainData.readTrainingDataFromFiles("+datafile+","+labelsfile+
			       "): started reading data", 2);
		long st = System.currentTimeMillis();
		double[][] matrix = DataMgr.readMatrixFromFile(datafile);
		double[] labels = DataMgr.readDoubleLabelsFromFile(labelsfile);
		_trainData = matrix;
		_trainLabels = labels;
		long dur = System.currentTimeMillis() - st;
		mger.msg("TrainData.readTrainingDataFromFiles("+datafile+","+labelsfile+
			       "): finished reading data in "+dur+" msecs.", 2);
	}
	
	
	/**
	 * calls the <CODE>readTrainingDataFromFiles()</CODE> method only if at least 
	 * one of the <CODE>_trainData,_trainLabels</CODE> caches is null.
	 * @param datafile String
	 * @param labelsfile String
	 * @throws java.io.IOException 
	 */
	static synchronized void readTrainingDataFromFilesIfNull(String datafile,
		                                                       String labelsfile) 
		throws java.io.IOException {
		Messenger mger = Messenger.getInstance();		
		if (_trainData==null || _trainLabels==null) { 
			mger.msg("TrainData.readTrainingDataFromFilesIfNull(): starting read",0);
			readTrainingDataFromFiles(datafile, labelsfile);
			mger.msg("TrainData.readTrainingDataFromFilesIfNull(): done reading",0);
		}
		else {
			mger.msg("TrainData.readTrainingDataFromFilesIfNull(): caches exist",0);			
		}
	}
	
	
	/**
	 * get the training data as 2D matrix.
	 * @return double[][]
	 */
	static synchronized double[][] getTrainingVectors() {
		return _trainData;
	}
	
	
	/**
	 * get the training labels as 1D vector.
	 * @return double[]
	 */
	static synchronized double[] getTrainingLabels() {
		return _trainLabels;
	}
	
	
	/**
	 * set the training data.
	 * @param data double[][]
	 */
	static synchronized void setTrainingVectors(double[][] data) {
		_trainData = data;
	}
	
	
	/**
	 * set the training labels.
	 * @param labels double[]
	 */
	static synchronized void setTrainingLabels(double[] labels) {
		_trainLabels = labels;
	}
}

