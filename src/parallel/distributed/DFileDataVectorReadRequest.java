package parallel.distributed;

import utils.DataMgr;
import java.io.*;
import java.util.List;
import popt4jlib.DblArray1Vector;
import popt4jlib.VectorIntf;


/**
 * class encapsulates request to read from a file residing on a machine running 
 * a msg-passing coordinator server, a certain range of vectors. Not part of the
 * public API (despite the public status of the class).
 * <p>Notes:
 * <ul>
 * <li>2019-02-09: reads the compile-time flag
 * <CODE>utils.DataFileAccessSrv._SEND_COMPACT_ARRAY</CODE> to determine how to
 * send data over the network, as an optimization for the otherwise slow
 * <CODE>writeObject()</CODE> method for object serialization.
 * <li>2019-02-08: modified the <CODE>_vectors</CODE> data member to be a 
 * generic <CODE>List</CODE> object to avoid the synchronization costs of old
 * <CODE>Vector</CODE> objects.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014-2019</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 2.0
 */
public class DFileDataVectorReadRequest implements DMsgIntf {
	// private static final long serialVersionUID = ...L;
	private String _filename;
	private int _fromIndex;
	private int _toIndex;
	private List _vectors;  // so that server can ask for the data to cache them
	                        // in a weak reference
	
	private static utils.Messenger _mger = utils.Messenger.getInstance();

	
	/**
	 * public constructor.
	 * @param filename String the name of the file to read from
	 * @param fromind int the starting index to include vectors from
	 * @param toind  int the last index to include vectors up to
	 */
	public DFileDataVectorReadRequest(String filename, int fromind, int toind) {
		_filename = filename;
		_fromIndex = fromind;
		_toIndex = toind;
	}
	
	
	/**
	 * reads the data from the file specified in the constructor in the range
	 * specified in the constructor, and sends them back via the object output 
	 * stream argument. If an exception occurs while attempting to read the data
	 * from the method <CODE>DataMgr.readVectorsFromFileInRange()</CODE>, a 
	 * <CODE>SimpleMessage</CODE> is sent back, with the message of the exception
	 * raised.
	 * @param oos ObjectOutputStream the socket output stream to send data via
	 * @throws IOException 
	 */
	public void execute(ObjectOutputStream oos) throws IOException {
		try {
			List vectors = DataMgr.readVectorsFromFileInRange(_filename, 
				                                                _fromIndex, _toIndex);
			long start = System.currentTimeMillis();
			if (!utils.DataFileAccessSrv._SEND_COMPACT_ARRAY) {
				oos.reset();  // force object to be written anew
				oos.writeObject(vectors);
			} else {
				// compact all data to a single 1-D double array
				long start_mem_copy = System.currentTimeMillis();
				final int k = vectors.size();
				final int n = ((VectorIntf)vectors.get(0)).getNumCoords();
				double[] data_arr = new double[k*n+1];
				data_arr[0] = n;  // first position indicates vector length
				int pos = 1;
				for (int i=0; i<k; i++) {
					double[] di = popt4jlib.DblArray1VectorAccess.get_x(
																		(DblArray1Vector)vectors.get(i));
					System.arraycopy(di, 0, data_arr, pos, n);
					pos += n;
				}
				long num_bytes = 8*data_arr.length;
				long dur_mem_copy = System.currentTimeMillis()-start_mem_copy;
				_mger.msg("parallel.distributed.DFileDataVectorReadRequest.execute(): "+
					        "data mem copy of "+num_bytes+" bytes took "+dur_mem_copy+
					        " msecs",2);
				oos.reset();
				oos.writeObject(data_arr);				
			}
			long dur = System.currentTimeMillis()-start;
			_mger.msg("parallel.distributed.DFileDataVectorReadRequest.execute(): "+
				        "writing objects took totally "+dur+" msecs", 2);
			_vectors = vectors;
		}
		catch (IOException e) {
			e.printStackTrace();
			oos.writeObject(new SimpleMessage(e.toString()));
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
			oos.writeObject(new SimpleMessage(e.toString()));
		}
		catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
			oos.writeObject(new SimpleMessage(e.toString()));			
		}
		oos.flush();
	}
	
	
	public String getFileName() { return _filename; }
	public int getFromIndex() { return _fromIndex; }
	public int getToIndex() { return _toIndex; }
	public List getData() { return _vectors; }
	
}

