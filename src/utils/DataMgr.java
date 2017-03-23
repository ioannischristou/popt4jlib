package utils;

import graph.*;
import parallel.*;
import popt4jlib.VectorIntf;
import popt4jlib.SparseVectorIntf;
import popt4jlib.DblArray1Vector;
import popt4jlib.DblArray1SparseVector;
import popt4jlib.DblArray1SparseVectorMT;
import popt4jlib.DblArray1SparseVectorFE;
import popt4jlib.GradientDescent.VecUtil;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;


/**
 * The class acts as an I/O Manager class for saving and loading data for
 * complex objects in the library such as Graph, HGraph, and properties
 * HashMap objects.
 * The class is thread-safe (reentrant).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DataMgr {

  private static long _uid=0;


  /**
   * return a globally unique identifier, in a thread-safe manner.
   * @return long
   */
  public static synchronized long getUniqueId() {
    return _uid++;
  }


  /**
   * Reads properties from the given file and stores them as &lt;key,value&gt;
	 * pairs in the returned <CODE>HashMap</CODE>.
	 * Unless the key string is a special keyword (see below), the value is 
	 * assumed first to be an int value, and if value does not represent an int
	 * it is then assumed to be a long value; if value does not represent a long
	 * it is then assumed to be a double value; if value does not represent a 
	 * double either, value is assumed to be representing a boolean value (the 
	 * strings "true" or "false" in all lower- or upper-case letters); if this 
	 * also fails, value is kept in the hash-table as-is, that is, as a 
	 * <CODE>String</CODE> type.
	 * <p>In case key is the keyword "graph", the key value is the next token in
	 * the line, and the full filename of the file containing the graph to be 
	 * constructed using the method <CODE>readGraphFromFile2(String)</CODE> is the 
	 * next token.
	 * <p>In case key is the keyword "class", the key value is the next token in
   * the line, and the object to be constructed together with its string
   * arguments is given in the rest of the line.
   * <p>In case key is the keyword "rndgen", the <CODE>RndUtil</CODE> class's 
	 * seed is populated with the value of this line (long int). The seed does not 
	 * need to be stored in the <CODE>HashMap</CODE> returned. Also, if a 3rd 
	 * argument is provided, it indicates the number of distinct threads to 
	 * require access to RndUtil, and thus it sets the required extra instances of 
	 * Random objects needed.
	 * <p>In case key is the keyword "dbglvl", the method 
	 * <CODE>utils.Messenger.getInstance(classname).setDebugLevel(lvl)</CODE> is 
	 * invoked where the value of the string classname is the next comma separated
	 * value in the line, and the lvl value is the value after that in the same
	 * line. If the classname is omitted in the line, the value "default" is 
	 * assumed.
	 * <p>In case key is the keyword "dbgclasses" then value is assumed to be 
	 * an integer, and the method <CODE>utils.Debug.setDebugBit(val)</CODE> is 
	 * called with the value represented by the value string.
   * <p>In case key is the keyword "mpc.maxsize", the 
	 * <CODE>parallel.MsgPassingCoordinator</CODE> class's max data size is set to 
	 * the value of this line (int), via a call to 
	 * <CODE>MsgPassingCoordinator.setMaxSize(val)</CODE>.
   * <p>In case key is the keyword "rpp.poolsize", the 
	 * <CODE>parallel.RegisteredParcelPool</CODE> 
	 * class's pool size is set to the value of this line (int), via a call to 
	 * <CODE>RegisteredParcelThreadLocalPools.setPoolSize(val)</CODE>.
	 * 
   * <p>File format is:
	 * <PRE>
   * key,value[,classname|graphfilename][,opts]
   * [...]
	 * </PRE>.
   * <p>Empty lines and lines starting with # are ignored.</p>
   * @param filename String
   * @throws IOException
   * @return HashMap
   */
  public synchronized static HashMap readPropsFromFile(String filename)
      throws IOException {
    HashMap props = new HashMap();
    BufferedReader br=null;
    try {
      br = new BufferedReader(new FileReader(filename));
      String line = null;
      StringTokenizer st = null;
      if (br.ready()) {
        while (true) {
          line = br.readLine();
          if (line == null) break;
          else if (line.length()==0 || line.startsWith("#")) continue;
          st = new StringTokenizer(line, ",");
          String key = st.nextToken();
          String strval = st.nextToken();
          if ("class".equals(key)) {
            String classname = st.nextToken();
            String ctrargs = "";
            while (st.hasMoreTokens()) {
              // ctrargs = st.nextToken().trim();
							ctrargs += st.nextToken();
							if (st.hasMoreTokens()) ctrargs+=",";
            }
            try {
              if (ctrargs.length() > 0) {
                Class cl = Class.forName(classname);
                Pair p = getArgTypesAndObjs(ctrargs, props);
                Class[] argtypes = (Class[]) p.getFirst();
                Object[] args = (Object[]) p.getSecond();
                Constructor ctor = cl.getConstructor(argtypes);
                Object obj = ctor.newInstance(args);
                props.put(strval, obj);
                continue;
              }
              else {
                Class cl = Class.forName(classname);
                Object obj = cl.newInstance();
                props.put(strval, obj);
                continue;
              }
            }
            catch (Exception e) {
              e.printStackTrace();
              continue;
            }
          }
          else if ("rndgen".equals(key)) {
            long seed = Long.parseLong(strval);
            RndUtil.getInstance().setSeed(seed);
            if (st.hasMoreTokens()) {  // next token indicates num-threads
              int nt = Integer.parseInt(st.nextToken());
              RndUtil.addExtraInstances(nt);
            }
            continue;
          }
					else if ("mpc.maxsize".equals(key)) {
						try {
							int maxsize = Integer.parseInt(strval);
							parallel.MsgPassingCoordinator.setMaxSize(maxsize);
						}
						catch (Exception e) {
							e.printStackTrace();  // ignore
						}
					}
					else if ("rpp.poolsize".equals(key)) {
						try {
							int maxsize = Integer.parseInt(strval);
							parallel.RegisteredParcelThreadLocalPools.setPoolSize(maxsize);
						}
						catch (Exception e) {
							e.printStackTrace();  // ignore
						}
					}
					else if ("graph".equals(key)) {
						try {
							String graphfilename = st.nextToken();
							Graph g = readGraphFromFile2(graphfilename);
							props.put(strval, g);
						}
						catch (Exception e) {
							e.printStackTrace();  // ignore
						}
					}
          else if ("dbglvl".equals(key)) {
            String msgername=null;
            int dbglvl=0;
            if (st.countTokens()>=1) {
              msgername = strval;
              dbglvl = Integer.parseInt(st.nextToken());
              Messenger mger = Messenger.getInstance(msgername);
              OutputStream stream = null;
              if (st.countTokens()>=1) {
                stream = new FileOutputStream(st.nextToken());
              }
              if (mger==null || stream!=null) Messenger.setInstance(msgername, stream);
              else if (mger==null) Messenger.setInstance(msgername,System.err);
              Messenger.getInstance(msgername).setDebugLevel(dbglvl);
            } else {
              dbglvl = Integer.parseInt(strval);
              Messenger.getInstance().setDebugLevel(dbglvl);
            }
            continue;
          }
          else if ("dbgclasses".equals(key)) {
            int val = Integer.parseInt(strval);
            Debug.setDebugBit(val);
            continue;
          }
          else if ("function.notreentrant".equals(key)) {
            // this prop must be placed after any <"*.numthreads",xxx>
            // or <"*.function", xxx> property lines in the props file.
            int val = Integer.parseInt(strval);  // used to be st.nextToken()
            if (val==1) {
              int nthreads=1;
              Iterator it = props.keySet().iterator();
              while (it.hasNext()) {
                String k = (String) it.next();
                if (k.endsWith("numthreads")) {
                  Integer ntI = (Integer) props.get(key);
                  nthreads += ntI.intValue();
                }
              }
              it = props.keySet().iterator();
              popt4jlib.ReentrantFunctionBaseMT rfb=null;
              while (it.hasNext()) {
                String k = (String) it.next();
                if (k.endsWith(".function")) {
                  popt4jlib.FunctionIntf f = (popt4jlib.FunctionIntf) props.get(k);
                  if (rfb==null) rfb = new popt4jlib.ReentrantFunctionBaseMT(f,nthreads);
                  props.put(k, rfb);
                  break;
                }
              }
            }
            else if (val==2) {
              Iterator it = props.keySet().iterator();
              while (it.hasNext()) {
                String k = (String) it.next();
                if (k.endsWith(".function")) {
                  popt4jlib.FunctionIntf f = (popt4jlib.FunctionIntf) props.get(k);
                  popt4jlib.ReentrantFunctionBase rfb = new popt4jlib.ReentrantFunctionBase(f);
                  props.put(k, rfb);
                  break;
                }
              }
            }
            continue;
          }
          // figure out what is strval: try int, long, double, boolean in that 
					// order.
					// as catch-all, keep as String.
          try {
            Integer v = new Integer(strval);
            props.put(key, v);
          }
          catch (NumberFormatException e) {
            try {
							Long v = new Long(strval);
							props.put(key, v);
						}
						catch (NumberFormatException e1) {
							try {
								Double v = new Double(strval);
								props.put(key, v);
							}
							catch (NumberFormatException e2) {
								// strval cannot be interpreted as a number.
								// check out if it is boolean
								if ("true".equals(strval) || "TRUE".equals(strval) ||
									  "false".equals(strval) || "FALSE".equals(strval)) {
									Boolean b = new Boolean(strval);
									props.put(key, b);
								}
								// finally, cannot represent as anything else, store as string
								else props.put(key, strval);
							}
						}
          }
        }
      }
      return props;
    }
    finally {
      if (br!=null) br.close();
    }
  }


  private static Pair getArgTypesAndObjs(String line, HashMap currentProps) {
    if (line == null || line.length() == 0) return null;
    StringTokenizer st = new StringTokenizer(line, ",");
    int numargs = st.countTokens();  // used to be st.countTokens()/2;
    Class[] argtypes = new Class[numargs];
    Object[] argvals = new Object[numargs];
    Pair result = new Pair(argtypes, argvals);
    int i=0;
    while (st.hasMoreTokens()) {
      //String objname = st.nextToken();
      String objval = st.nextToken();
      // figure out what is strval: try int, long, double, boolean, already-read-in-prop, string
      try {
        Integer v = new Integer(objval);
        argtypes[i] = int.class;  // used to be v.getClass();
        argvals[i++] = v;
      }
      catch (NumberFormatException e) {
        try {
					Long v = new Long(objval);
          argtypes[i] = long.class;  // used to be v.getClass();
          argvals[i++] = v;
				}
				catch (NumberFormatException e1) {
					try {
	          Double v = new Double(objval);
		        argtypes[i] = double.class;  // used to be v.getClass();
			      argvals[i++] = v;
				  }
					catch (NumberFormatException e2) {
						// strval cannot be interpreted as a number.
						// check out if it is boolean
						if ("true".equals(objval) || "TRUE".equals(objval) ||
							  "false".equals(objval) || "FALSE".equals(objval)) {
							Boolean b = new Boolean(objval);
							argtypes[i] = boolean.class;  // used to be b.getClass();
							argvals[i++] = b;
						}
						else {
							// check if it is already in currentProps
							Object v = currentProps.get(objval);  // used to be objname
							if (v!=null) {
								argtypes[i] = v.getClass();
								argvals[i++] = v;
							}
							else {
								// finally, cannot represent as anything else, must be a string
								argtypes[i] = "".getClass();  // String.class ok too
								argvals[i++] = objval;
							}
						}
					}
				}
      }
    }
    return result;
  }


  /**
   * reads the vectors from a file of the form
	 * <PRE>
   * [
   * numdocs totaldimensions
   * dim,val [ dim,val]*
   * [...]
   * ]
	 * </PRE>
   * dim is in [1...totaldimensions]
   * the documents are represented as a vector representation of
   * a vector in a vector space of dimension totaldimensions. If a &lt;dim,val&gt;
   * pair does not appear for a vector, the value for this coordinate is assumed
   * zero.
   * @param filename String
   * @return Vector // Vector&lt;VectorIntf&gt;
   * @throws IOException
   */
  public static Vector readVectorsFromFile(String filename)
      throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    try {
      Vector v = new Vector();
      if (br.ready()) {
        String line = br.readLine();
        StringTokenizer st = new StringTokenizer(line, " ");
        int numdocs = Integer.parseInt(st.nextToken());  // unused
        int totaldims = Integer.parseInt(st.nextToken());
        Integer dim = null;
        double val = 0.0;
        while (true) {
          line = br.readLine();
          if (line == null) break;  // end-of-file
          DblArray1Vector d = new DblArray1Vector(new double[totaldims]);
          st = new StringTokenizer(line, " ");
          while (st.hasMoreTokens()) {
            String pair = st.nextToken();
            StringTokenizer st2 = new StringTokenizer(pair, ",");
            dim = new Integer(Integer.parseInt(st2.nextToken()) - 1);
            // dimension value is from 1...totdims
            val = Double.parseDouble(st2.nextToken());
            d.setCoord(dim.intValue(), val);
          }
          v.addElement(d);
        }
      }
      return v;
    }
    finally {
      if (br!=null) br.close();
    }
  }

	
	/**
	 * same functionality as in <CODE>readVectorsFromFile(String)</CODE> method,
	 * except that it only reads and returns the vectors in the lines in the range
	 * [fromIndex, toIndex] (fromIndex and toIndex take values in 
	 * {0,...,<CODE>readNumVectorsInFile(String)</CODE>-1})
	 * @param filename String 
	 * @param fromIndex int 
	 * @param toIndex int
	 * @return Vector // Vector&lt;DblArray1Vector&gt;
	 * @throws IOException 
	 * @throws IllegalArgumentException if fromIndex &gt; toIndex or any of the two
	 * is outside the valid range of indices.
	 * @throws IndexOutOfBoundsException if fromIndex or toIndex are out of range.
	 */
	public static Vector readVectorsFromFileInRange(String filename, int fromIndex, int toIndex)
	    throws IOException, IllegalArgumentException, IndexOutOfBoundsException {
		if (fromIndex>toIndex) 
			throw new IllegalArgumentException("fromIndex("+fromIndex+
							                           ") cannot be > toIndex("+toIndex+")");
		if (fromIndex < 0 || toIndex < 0)
			throw new IndexOutOfBoundsException("fromIndex or toIndex is less than zero");
    BufferedReader br = new BufferedReader(new FileReader(filename));
    try {
      Vector v = new Vector();
      if (br.ready()) {
        String line = br.readLine();
        StringTokenizer st = new StringTokenizer(line, " ");
        int numdocs = Integer.parseInt(st.nextToken());
				if (toIndex>=numdocs) throw new IndexOutOfBoundsException("toIndex is >= #docs in file "+filename);
        int totaldims = Integer.parseInt(st.nextToken());
        Integer dim = null;
        double val = 0.0;
				int lcnt = 0;  // line counter
        while (true) {
          line = br.readLine();
          if (line == null) break;  // end-of-file
					lcnt++;
					int lcntm1 = lcnt-1;
					if (lcntm1<fromIndex) continue;  // skip
					else if (lcntm1>toIndex) break;  // done
          DblArray1Vector d = new DblArray1Vector(new double[totaldims]);
          st = new StringTokenizer(line, " ");
          while (st.hasMoreTokens()) {
            String pair = st.nextToken();
            StringTokenizer st2 = new StringTokenizer(pair, ",");
            dim = new Integer(Integer.parseInt(st2.nextToken()) - 1);
            // dimension value is from 1...totdims
            val = Double.parseDouble(st2.nextToken());
            d.setCoord(dim.intValue(), val);
          }
          v.addElement(d);
        }
				if (lcnt!=numdocs && toIndex > lcnt) 
					throw new IOException("bad file header (numdocs="+numdocs+" but there are only "+lcnt+" data lines.");
      }
      return v;
    }
    finally {
      if (br!=null) br.close();
    }		
	}
	

  /**
   * reads the vectors from a file of the form
   * <PRE>
   * numdocs totaldimensions
   * dim,val [dim,val]
   * [...]
   * </PRE>
   * dim is in [1...totaldimensions]
   * the documents are represented as a sparse vector representation of
   * a vector in a vector space of dimension totaldimensions
   * @param filename String
   * @return Vector // Vector&lt;DblArray1SparseVector&gt;
   * @throws IOException
   */
  public static Vector readSparseVectorsFromFile(String filename)
      throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    try {
      Vector v = new Vector();
      if (br.ready()) {
        String line = br.readLine();
        StringTokenizer st = new StringTokenizer(line, " ");
        int numdocs = Integer.parseInt(st.nextToken());  // unused
        int totaldims = Integer.parseInt(st.nextToken());
        Integer dim = null;
        double val = 0.0;
        while (true) {
          line = br.readLine();
          if (line == null) break;  // end-of-file
          VectorIntf d = null;
          st = new StringTokenizer(line, " ");
          while (st.hasMoreTokens()) {
            String pair = st.nextToken();
            StringTokenizer st2 = new StringTokenizer(pair, ",");
            dim = new Integer(Integer.parseInt(st2.nextToken()) - 1);
            // dimension value is from 1...totdims
            val = Double.parseDouble(st2.nextToken());
            if (d==null) {
              int[] inds = new int[1];
              double[] vals = new double[1];
              inds[0] = dim.intValue();
              vals[0] = val;
              d = new DblArray1SparseVector(inds, vals, totaldims);
            }
            else d.setCoord(dim.intValue(), val);
          }
          if (d==null) d = new DblArray1SparseVector(totaldims);  // empty vector
          v.addElement(d);
        }
      } else throw new IOException("readSparseVectorsFromFile("+filename+"): failed");
      return v;
    }
    catch (ParallelException e) {  // can never get here
      e.printStackTrace();
      return null;
    }
    finally {
      if (br!=null) br.close();
    }
  }


  /**
   * reads the vectors from a file of the form
   * <PRE>
   * numdocs totaldimensions
   * dim,val [dim,val]
   * [...]
   * </PRE>
   * dim is in [1...totaldimensions]
   * the documents are represented using full storage requirements (as a normal
   * double[]) taking up a lot of space, but also maintaining an index of the
   * non-zero positions for very fast <CODE>getQuick(i)</CODE> and
   * <CODE>getIthNonnZeroPos(i)</CODE> operations.
   * @param filename String
   * @return Vector // Vector&lt;DblArray1SparseVectorFE&gt;
   * @throws IOException
   */
  public static Vector readSparseVectorsFEFromFile(String filename)
      throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    try {
      Vector v = new Vector();
      if (br.ready()) {
        String line = br.readLine();
        StringTokenizer st = new StringTokenizer(line, " ");
        int numdocs = Integer.parseInt(st.nextToken());  // unused
        int totaldims = Integer.parseInt(st.nextToken());
        Integer dim = null;
        double val = 0.0;
        while (true) {
          line = br.readLine();
          if (line == null) break;  // end-of-file
          VectorIntf d = null;
          st = new StringTokenizer(line, " ");
          while (st.hasMoreTokens()) {
            String pair = st.nextToken();
            StringTokenizer st2 = new StringTokenizer(pair, ",");
            dim = new Integer(Integer.parseInt(st2.nextToken()) - 1);
            // dimension value is from 1...totdims
            val = Double.parseDouble(st2.nextToken());
            if (d==null) {
              int[] inds = new int[1];
              double[] vals = new double[1];
              inds[0] = dim.intValue();
              vals[0] = val;
              d = new DblArray1SparseVectorFE(inds, vals, totaldims);
            }
            else d.setCoord(dim.intValue(), val);
          }
          if (d==null) d = new DblArray1SparseVectorFE(totaldims);  // empty vector
          v.addElement(d);
        }
      } else throw new IOException("readSparseVectorsFromFile("+filename+"): failed");
      return v;
    }
    catch (ParallelException e) {  // can never get here
      e.printStackTrace();
      return null;
    }
    finally {
      if (br!=null) br.close();
    }
  }


  /**
   * reads the vectors from a file of the form
   * <PRE>
   * numdocs totaldimensions
   * dim,val [dim,val]
   * [...]
   * </PRE>
   * dim is in [1...totaldimensions]
   * the documents are represented as a sparse vector representation of
   * a vector in a vector space of dimension totaldimensions
   * @param filename String
   * @return Vector // Vector&lt;DblArray1SparseVectorMT&gt;
   * @throws IOException
   */
  public static Vector readSparseVectorsMTFromFile(String filename)
      throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    try {
      Vector v = new Vector();
      if (br.ready()) {
        String line = br.readLine();
        StringTokenizer st = new StringTokenizer(line, " ");
        int numdocs = Integer.parseInt(st.nextToken());  // unused
        int totaldims = Integer.parseInt(st.nextToken());
        Integer dim = null;
        double val = 0.0;
        while (true) {
          line = br.readLine();
          if (line == null) break;  // end-of-file
          VectorIntf d = null;
          st = new StringTokenizer(line, " ");
          while (st.hasMoreTokens()) {
            String pair = st.nextToken();
            StringTokenizer st2 = new StringTokenizer(pair, ",");
            dim = new Integer(Integer.parseInt(st2.nextToken()) - 1);
            // dimension value is from 1...totdims
            val = Double.parseDouble(st2.nextToken());
            if (d==null) {
              int[] inds = new int[1];
              double[] vals = new double[1];
              inds[0] = dim.intValue();
              vals[0] = val;
              d = new DblArray1SparseVectorMT(inds, vals, totaldims);
            }
            else d.setCoord(dim.intValue(), val);
          }
          if (d==null) d = new DblArray1SparseVectorMT(totaldims);  // empty vector
          v.addElement(d);
        }
      } else throw new IOException("readSparseVectorsFromFile("+filename+"): failed");
      return v;
    }
    catch (ParallelException e) {  // can never get here
      e.printStackTrace();
      return null;
    }
    finally {
      if (br!=null) br.close();
    }
  }


  /**
   * same as the method <CODE>readVectorsFromFile(String filename)</CODE>
   * except that each <CODE>VectorIntf</CODE> object is normalized (in L2 norm)
   * to unity.
   * @param filename String
   * @throws IOException
   * @return Vector // Vector&lt;VectorIntf&gt;
   */
  public static Vector readVectorsFromFileAndNormalize(String filename)
      throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    try {
      Vector v = new Vector();
      if (br.ready()) {
        String line = br.readLine();
        StringTokenizer st = new StringTokenizer(line, " ");
        int numdocs = Integer.parseInt(st.nextToken());
        int totaldims = Integer.parseInt(st.nextToken());
        Integer dim = null;
        double val = 0.0;
        while (true) {
          line = br.readLine();
          if (line == null) break;
          VectorIntf d = new DblArray1Vector(new double[totaldims]);
          st = new StringTokenizer(line, " ");
          while (st.hasMoreTokens()) {
            String pair = st.nextToken();
            StringTokenizer st2 = new StringTokenizer(pair, ",");
            dim = new Integer(Integer.parseInt(st2.nextToken()) - 1);
            // dimension value is from 1...totdims
            val = Double.parseDouble(st2.nextToken());
            d.setCoord(dim.intValue(), val);
          }
          // normalize
          double norm = VecUtil.norm2(d);
          if (norm>0) d.div(norm);
          v.addElement(d);
        }
      }
      return v;
    }
    catch (ParallelException e) {  // can never get here
      e.printStackTrace();
      return null;
    }
    finally {
      if (br!=null) br.close();
    }
  }


  /**
   * same as the method <CODE>readSparseVectorsFromFile(String filename)</CODE>
   * except that each <CODE>VectorIntf</CODE> object is normalized (in L2 norm)
   * to unity.
   * @param filename String
   * @throws IOException
   * @return Vector // Vector&lt;VectorIntf&gt;
   */
  public static Vector readSparseVectorsMTFromFileAndNormalize(String filename)
      throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    try {
      Vector v = new Vector();
      if (br.ready()) {
        String line = br.readLine();
        StringTokenizer st = new StringTokenizer(line, " ");
        int numdocs = Integer.parseInt(st.nextToken());
        int totaldims = Integer.parseInt(st.nextToken());
        Integer dim = null;
        double val = 0.0;
        while (true) {
          line = br.readLine();
          if (line == null) break;
          VectorIntf d = null;
          st = new StringTokenizer(line, " ");
          while (st.hasMoreTokens()) {
            String pair = st.nextToken();
            StringTokenizer st2 = new StringTokenizer(pair, ",");
            dim = new Integer(Integer.parseInt(st2.nextToken()) - 1);
            // dimension value is from 1...totdims
            val = Double.parseDouble(st2.nextToken());
            if (d==null) {
              int[] inds = new int[1];
              double[] vals = new double[1];
              inds[0] = dim.intValue();
              vals[0] = val;
              d = new DblArray1SparseVectorMT(inds, vals, totaldims);
            }
            else d.setCoord(dim.intValue(), val);
          }
          // normalize
          double norm = VecUtil.norm2(d);
          if (norm>0) d.div(norm);
          v.addElement(d);
        }
      }
      return v;
    }
    catch (ParallelException e) {  // can never get here
      e.printStackTrace();
      return null;
    }
    finally {
      if (br!=null) br.close();
    }
  }
	
	
	/**
	 * returns the number in the header (first line) of the file stating the 
	 * number of vectors in the file.
	 * @param filename String name of file containing the vectors
	 * @return number of vectors in file
	 * @throws IOException 
	 */
	public static int readNumVectorsInFile(String filename) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
		int res = -1;
    try {
      if (br.ready()) {
				String line = br.readLine();
				StringTokenizer st = new StringTokenizer(line, " ");
				String numdocs = st.nextToken();
				res = Integer.parseInt(numdocs);
			}
			return res;
		}
		finally {
			if (br!=null) br.close();
		}
	}


  /**
   * reads a file of exactly the same format as the one described in
   * <CODE>readVectorsFromFile(String)</CODE> and interprets it as a 2D matrix
   * of double elements.
   * @param filename String
   * @throws IOException
   * @return double[][]
   */
  public static double[][] readMatrixFromFile(String filename) throws IOException, IllegalArgumentException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    try {
      double[][] v = null;
      if (br.ready()) {
        String line = br.readLine();
        StringTokenizer st = new StringTokenizer(line, " ");
        int numdocs = Integer.parseInt(st.nextToken());
        int totaldims = Integer.parseInt(st.nextToken());
        v = new double[numdocs][totaldims];
        double val = 0.0;
        int i=0;
        while (true) {
          line = br.readLine();
          if (line == null) break;
          st = new StringTokenizer(line, " ");
          while (st.hasMoreTokens()) {
            String pair = st.nextToken();
            StringTokenizer st2 = new StringTokenizer(pair, ",");
            int j = Integer.parseInt(st2.nextToken()) - 1;
            // dimension value is from 1...totdims
            val = Double.parseDouble(st2.nextToken());
            v[i][j] = val;//d.setCoord(dim.intValue(), val);
          }
          ++i;
        }
      }
      return v;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("wrong file format or data");
    }
    finally {
      if (br!=null) br.close();
    }
  }


  /**
   * reads numbers from a file (one integer in each consecutive line) and
   * returns them as an int[].
   * @param filename String
   * @throws IOException
   * @return int[]
   */
  public static int[] readNumericLabelsFromFile(String filename) throws IOException {
    BufferedReader br = null;
    Vector ls=new Vector();
    try {
      br = new BufferedReader(new FileReader(filename));
      if (br.ready()) {
        while (true) {
          String line=br.readLine();
          if (line==null) break;  // EOF
          int li = Integer.parseInt(line);
          ls.addElement(new Integer(li));
        }
      }
    }
    finally {
      if (br!=null) br.close();
    }
    int numdocs = ls.size();
    int labels[] = new int[numdocs];
    for (int i=0; i<numdocs; i++)
      labels[i]=((Integer) ls.elementAt(i)).intValue();
    return labels;
  }


  /**
   * read a set of center vectors from a file. The first line contains the
   * number of dimensions of the vector space. The rest of the lines are the
   * centers in space separated coordinates.
   * @param filename String
   * @throws IOException
   * @return Vector // Vector&lt;VectorIntf&gt;
   */
  public static Vector readCentersFromFile(String filename) throws IOException {
    BufferedReader br=null;
    try {
      br = new BufferedReader(new FileReader(filename));
      Vector v = new Vector();
      if (br.ready()) {
        String line = br.readLine();
        int dims = Integer.parseInt(line);
        Integer dim = null;
        double val = 0.0;
        while (true) {
          line = br.readLine();
          if (line == null || line.length() == 0)break;
          VectorIntf d = new DblArray1Vector(new double[dims]);
          StringTokenizer st = new StringTokenizer(line, ", ");
          int i = 0;
          while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            val = Double.parseDouble(tok);
            d.setCoord(i, val);
            ++i;
          }
          v.addElement(d);
        }
      }
      return v;
    }
    catch (ParallelException e) {  // can never get here
      e.printStackTrace();
      return null;
    }
    finally {
      if (br!=null) br.close();
    }
  }

	
	/**
	 * read a set of null-space separated integer values in a text file.
	 * @param filename String
	 * @return IntSet
	 * @throws IOException 
	 */
	public static IntSet readIntegersFromFile(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));
		if (br.ready()) {
			IntSet set = new IntSet();
			while (true) {
				String line = br.readLine();
				if (line==null) break;
				if (line.length()==0) continue;
				StringTokenizer st = new StringTokenizer(line);  // uses default delimiter set
				while (st.hasMoreTokens()) {
					String n = st.nextToken();
					int num = Integer.parseInt(n);
					set.add(new Integer(num));
				}
			}
			return set;
		}
		return null;
	}
	

  /**
   * filename format is of the form:
   * <PRE>
   * numnodes numarcs
   * starta enda [weighta]
   * [...]
   * </PRE>
   * The starta, enda are in [0...num_nodes-1]
   * weighta is double (non-negative)
   * @param filename String
   * @throws IOException
   * @throws GraphException
   * @return Graph
   */
  public synchronized static Graph readGraphFromFile(String filename)
      throws IOException, GraphException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    try {
      Graph g = null;
      if (br.ready()) {
        // read first line
        String line = br.readLine();
        if (line==null) throw new GraphException("null graph file");
        StringTokenizer st = new StringTokenizer(line, " ");
        int numnodes = Integer.parseInt(st.nextToken());
        int numarcs = Integer.parseInt(st.nextToken());
        g = Graph.newGraph(numnodes, numarcs);
        int starta, enda;
        double weighta;
        while (true) {
          line = br.readLine();
          if (line == null) break;
          st = new StringTokenizer(line, " ");
          starta = Integer.parseInt(st.nextToken());
          enda = Integer.parseInt(st.nextToken());
          if (st.hasMoreTokens()) weighta = Double.parseDouble(st.nextToken());
          else weighta = 1.0;
          try {
            g.addLink(starta, enda, weighta);
          }
          catch (ParallelException e) {
            // cannot get here
            e.printStackTrace();
          }
        }
      }
      // set cardinality values
      for (int i = 0; i < g.getNumNodes(); i++) {
        Node ni = g.getNode(i);
        try {
          ni.setWeight("cardinality", new Double(1.0));
        }
        catch (ParallelException e) { e.printStackTrace(); } // never gets here
      }
      return g;
    }
    finally {
      if (br!=null) br.close();
    }
  }


  /**
   * filename format is of the form:
   * <PRE>
   * numarcs numnodes
   * starta enda [weighta]
   * [...]
   * [weight_node1...]
   * </PRE>
   * The starta, enda are in [1...num_nodes]
   * weighta is double (non-negative)
   * node weights after arcs listing, if they exist, are double (non-negative)
   * @param filename String
   * @throws IOException
   * @throws GraphException
   * @return Graph
   */
  public synchronized static Graph readGraphFromFile2(String filename)
      throws IOException, GraphException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    try {
      Graph g = null;
      if (br.ready()) {
        // read first line
        String line = br.readLine();
        if (line==null) throw new GraphException("null graph file");
        StringTokenizer st = new StringTokenizer(line, " ");
        int numarcs = Integer.parseInt(st.nextToken());
        int numnodes = Integer.parseInt(st.nextToken());
        g = Graph.newGraph(numnodes, numarcs);
        int starta, enda;
        double weighta;
        int arccnt=0;
        while (++arccnt<=numarcs) {
          line = br.readLine();
          if (line == null) break;
					if (line.trim().length()==0) continue;  // ignore empty lines
          st = new StringTokenizer(line, " ");
          starta = Integer.parseInt(st.nextToken());
          enda = Integer.parseInt(st.nextToken());
          if (st.hasMoreTokens()) weighta = Double.parseDouble(st.nextToken());
          else weighta = 1.0;
          try {
            g.addLink(starta - 1, enda - 1, weighta);
          }
          catch (ParallelException e) {
            e.printStackTrace();  // cannot get here
          }
        }
      }
      int nid = 0;
      while (true) {
        String line = br.readLine();
        if (line==null) break;
				if (line.trim().length()==0) continue;  // ignore empty lines
        double val = Double.parseDouble(line);
        try {
          g.getNode(nid++).setWeight("value", new Double(val));
        }
        catch (ParallelException e) { e.printStackTrace(); } // never gets here
      }
      // set cardinality values
      for (int i = 0; i < g.getNumNodes(); i++) {
        Node ni = g.getNode(i);
        try {
          ni.setWeight("cardinality", new Double(1.0));
        }
        catch (ParallelException e) { e.printStackTrace(); } // never gets here
      }
      return g;
    }
    finally {
      if (br!=null) br.close();
    }
  }


  /**
   * filename format is of the form:
	 * <PRE>
   * numarcs numnodes 1
   * weighta starta enda
   * [...]
	 * </PRE>
   * The starta, enda are in [1...num_nodes]
   * weighta is int (non-negative)
   * @param filename String
   * @throws IOException
   * @throws GraphException
   * @return Graph
   */
  public synchronized static Graph readGraphFromhMeTiSFile(String filename)
      throws IOException, GraphException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    try {
      Graph g = null;
      if (br.ready()) {
        // read first line
        String line = br.readLine();
        if (line==null) throw new GraphException("null graph file");
        StringTokenizer st = new StringTokenizer(line, " ");
        int numarcs = Integer.parseInt(st.nextToken());
        int numnodes = Integer.parseInt(st.nextToken());
        g = Graph.newGraph(numnodes, numarcs);
        int starta, enda;
        double weighta;
        while (true) {
          line = br.readLine();
          if (line == null) break;
          st = new StringTokenizer(line, " ");
          weighta = Integer.parseInt(st.nextToken());
          starta = Integer.parseInt(st.nextToken());
          enda = Integer.parseInt(st.nextToken());
          try {
            g.addLink(starta - 1, enda - 1, weighta);
          }
          catch (ParallelException e) {
            e.printStackTrace();  // cannot get here
          }
        }
      }
      // set cardinality values
      for (int i = 0; i < g.getNumNodes(); i++) {
        Node ni = g.getNode(i);
        try {
          ni.setWeight("cardinality", new Double(1.0));
        }
        catch (ParallelException e) { e.printStackTrace(); } // never gets here
      }
      return g;
    }
    finally {
      if (br!=null) br.close();
    }
  }


  /**
   * same as above method, only that the labelfile serves to read the labels
   * for each of the nodes in the Graph being created
   * @param filename String
   * @param labelfile String
   * @throws IOException
   * @throws GraphException
   * @return Graph
   */
  public synchronized static Graph readGraphFromhMeTiSFile(String filename,
                                                           String labelfile)
      throws IOException, GraphException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    BufferedReader br2 = new BufferedReader(new FileReader(labelfile));
    try {
      Graph g = null;
      if (br.ready() && br2.ready()) {
        // read first line
        String line = br.readLine();
        if (line==null) throw new GraphException("null graph file");
        StringTokenizer st = new StringTokenizer(line, " ");
        int numarcs = Integer.parseInt(st.nextToken());
        int numnodes = Integer.parseInt(st.nextToken());
        // now read the 2nd file and create the labels array
        Integer[] labels = new Integer[numnodes];
        for (int j=0; j<numnodes; j++) {
          String l2 = br2.readLine();
          if (l2==null) throw new GraphException("filename and labelfile don't agree in dimensions");
          int labelj = Integer.parseInt(l2);
          labels[j] = new Integer(labelj);
        }
        br2.close();
        br2 = null;
        g = Graph.newGraph(numnodes, numarcs, labels);
        int starta, enda;
        double weighta;
        while (true) {
          line = br.readLine();
          if (line == null) break;
          st = new StringTokenizer(line, " ");
          weighta = Integer.parseInt(st.nextToken());
          starta = Integer.parseInt(st.nextToken());
          enda = Integer.parseInt(st.nextToken());
          try {
            g.addLink(starta - 1, enda - 1, weighta);
          }
          catch (ParallelException e) { e.printStackTrace(); }  // never here
        }
      }
      // set cardinality values
      for (int i = 0; i < g.getNumNodes(); i++) {
        Node ni = g.getNode(i);
        try {
          ni.setWeight("cardinality", new Double(1.0));
        }
        catch (ParallelException e) { e.printStackTrace(); } // never gets here
      }
      return g;
    }
    finally {
      if (br!=null) br.close();
      if (br2!=null) br2.close();
    }
  }


  /**
   * filename format is of the form:
   * <PRE>
   * numarcs numnodes [1]
   * [weighta] node1 node2 ... nodek
   * [...]
   * </PRE>
   * The nodei are in [1...numnodes]
   * weighta is int (non-negative). If it does not exist (the third value in the
   * first line does not exist), then its value is 1.
   * @param filename String
   * @throws IOException
   * @throws GraphException
   * @return HGraph
   */
  public synchronized static HGraph readHGraphFromhMeTiSFile(String filename)
      throws IOException, GraphException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    try {
      HGraph g = null;
      boolean no_weight=false;
      if (br.ready()) {
        // read first line
        String line = br.readLine();
        if (line==null) throw new GraphException("empty graph file");
        StringTokenizer st = new StringTokenizer(line, " ");
        int numarcs = Integer.parseInt(st.nextToken());
        int numnodes = Integer.parseInt(st.nextToken());
        if (st.hasMoreTokens()==false) no_weight=true;
        g = new HGraph(numnodes, numarcs);
        Set nodes = new HashSet();  // Set<Integer id>
        double weighta=1.0;
        while (true) {
          line = br.readLine();
          if (line == null) break;
          st = new StringTokenizer(line, " ");
          if (no_weight==false)
            weighta = Integer.parseInt(st.nextToken());
          nodes.clear();
          while (st.hasMoreTokens()) {
            int nid = Integer.parseInt(st.nextToken());
            nodes.add(new Integer(nid-1));
          }
          g.addHLink(nodes, weighta);
        }
      }
      // set cardinality values
      for (int i = 0; i < g.getNumNodes(); i++) {
        HNode ni = g.getHNode(i);
        ni.setWeight("cardinality", new Double(1.0));
      }
      return g;
    }
    finally {
      if (br!=null) br.close();
    }
  }


  /**
   * takes as input an already constructed graph, and prints it in MeTiS format
   * in the file specified in the second argument. The format of this output
   * file is as follows:
	 * <PRE>
   * numnodes numarcs
   * starta enda weight
	 * ...
	 * </PRE>
   * starta, enda are in [0,...,numnodes-1]
   * @param g Graph
   * @param filename String
   * @throws IOException
   */
  public synchronized static void writeGraphToFile(Graph g, String filename)
      throws IOException {
    final int numarcs = g.getNumArcs();
    final int numnodes = g.getNumNodes();
    PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
    pw.println(numnodes + " " + numarcs);
    for (int i=0; i<numarcs; i++) {
      Link arci = g.getLink(i);
      pw.println(arci.getStart() + " " +arci.getEnd() + " " + arci.getWeight());
    }
    pw.flush();
    pw.close();
  }


  /**
   * takes as input an already constructed graph, and prints it in MeTiS format
   * in the file specified in the second argument. The format of this output
   * file is as follows:
   * <PRE>
   * numarcs numnodes
   * ...
   * starta enda weight
   * ...
   * nodeweight
   * ...
   * </PRE>
   * starta, enda are in [1,...,numnodes]
   * nodeweight are double values, the first corresponds to the 1st node etc.
   * @param g Graph
   * @param filename String
   * @throws IOException
   */
  public synchronized static void writeGraphToFile2(Graph g, String filename)
      throws IOException {
    final int numarcs = g.getNumArcs();
    final int numnodes = g.getNumNodes();
    PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
    pw.println(numarcs + " " + numnodes);
    for (int i=0; i<numarcs; i++) {
      Link arci = g.getLink(i);
      pw.println((arci.getStart()+1) + " " +(arci.getEnd()+1) + " " + arci.getWeight());
    }
    for (int i=0; i<numnodes; i++) {
      Double vi = g.getNode(i).getWeightValue("value");
      if (vi!=null) pw.println(vi.doubleValue());
    }
    pw.flush();
    pw.close();
  }


  /**
   * takes as input an already constructed graph, and prints it in HMeTiS format
   * in the file specified in the second argument. The format of this output
   * file is specified in the comments for method writeClusterEnsembleToHGRFile()
   * @param g Graph
   * @param filename String
   * @throws IOException
   */
  public synchronized static void writeGraphToHGRFile(Graph g, String filename)
      throws IOException {
    final int numarcs = g.getNumArcs();
    final int numnodes = g.getNumNodes();
    PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
    pw.println(numarcs/2+" "+ numnodes+" 1");
    for (int i=0; i<numarcs; i++) {
      Link l = g.getLink(i);
      Node startn = g.getNode(l.getStart());
      Node endn = g.getNode(l.getEnd());
      if (startn.getId()<endn.getId()) {
        long w = (long) (2.0*Math.ceil(l.getWeight()));  // *MeTiS accepts int weights
        pw.println(w+" "+(startn.getId()+1)+" "+(endn.getId()+1));
      }
    }
    pw.flush();
    pw.close();
  }


  /**
   * write Graph in .hgr (?MeTiS) format
   * @param g Graph
   * @param filename String
   * @throws IOException
   */
  public static void writeGraphDirectToHGRFile(Graph g, String filename) throws IOException {
    final int numarcs = g.getNumArcs();
    final int numnodes = g.getNumNodes();
    PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
    pw.println(numarcs+" "+ numnodes+" 1");
    for (int i=0; i<numarcs; i++) {
      Link l = g.getLink(i);
      Node startn = g.getNode(l.getStart());
      Node endn = g.getNode(l.getEnd());
      if (startn.getId()<endn.getId()) {
        long w = (long) (Math.ceil(l.getWeight()));  // *MeTiS accepts int weights
        pw.println(w+" "+(startn.getId()+1)+" "+(endn.getId()+1));
      }
    }
    pw.flush();
    pw.close();
  }


  /**
   * write HGraph in .hgr (?MeTiS) format
   * @param g HGraph
   * @param filename String
   * @throws IOException
   */
  public synchronized static void writeHGraphDirectToHGRFile(HGraph g, String filename) throws IOException {
    final int numarcs = g.getNumArcs();
    final int numnodes = g.getNumNodes();
    PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
    pw.println(numarcs+" "+ numnodes+" 1");
    for (int i=0; i<numarcs; i++) {
      HLink l = g.getHLink(i);
      long w = (long) (Math.ceil(l.getWeight()));  // *MeTiS accepts int weights
      pw.print(w);
      Iterator nids = l.getHNodeIds();
      while (nids.hasNext()) {
        int nid = ((Integer) nids.next()).intValue()+1;
        pw.print(" "+nid);
      }
      pw.println("");
    }
    pw.flush();
    pw.close();
  }


  /**
   * write HGraph in .hgr (?MeTiS) format
   * @param g HGraph
   * @param filename String
   * @throws IOException
   */
  public synchronized static void writeWeightedHGraphDirectToHGRFile(HGraph g, String filename) throws IOException {
    final int numarcs = g.getNumArcs();
    final int numnodes = g.getNumNodes();
    PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
    pw.println(numarcs+" "+ numnodes+" 11");
    for (int i=0; i<numarcs; i++) {
      HLink l = g.getHLink(i);
      long w = (long) (Math.ceil(l.getWeight()));  // *MeTiS accepts int weights
      pw.print(w);
      Iterator nids = l.getHNodeIds();
      while (nids.hasNext()) {
        int nid = ((Integer) nids.next()).intValue()+1;
        pw.print(" "+nid);
      }
      pw.println("");
    }
    for (int i=0; i<numnodes; i++) {
      HNode ni = g.getHNode(i);
      pw.println(ni.getWeightValue("cardinality"));
    }
    pw.flush();
    pw.close();
  }


  /**
   * takes as input an already constructed graph, and prints it in MeTiS format
   * in the file specified in the second argument. The format of this output
   * file is as follows:
	 * <PRE>
   * numnodes numarcs 1
   * ...
   * nbor_id1 edgeweight_1 nbor_id2 edgeweight_2 ...
	 * </PRE>
   * @param g Graph
   * @param filename String
   * @throws IOException
   */
  public synchronized static void writeGraphToGRFile(Graph g, String filename) throws IOException {
    final int numarcs = g.getNumArcs();
    final int numnodes = g.getNumNodes();
    PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
    pw.println(numnodes + " " + numarcs/2+" 1");
    for (int i=0; i<numnodes; i++) {
      pw.print((i+1)+" ");
      Set inlinks = g.getNode(i).getInLinks();
      Iterator init = inlinks.iterator();
      while (init.hasNext()) {
        Integer linkid = (Integer) init.next();
        Link l = g.getLink(linkid.intValue());
        int s = l.getStart();
        long w = (long) (2.0*Math.ceil(l.getWeight()));  // *MeTiS accepts int weights
        pw.print(s+" "+w+" ");
      }
      Set outlinks = g.getNode(i).getOutLinks();
      Iterator outit = outlinks.iterator();
      while (outit.hasNext()) {
        Integer linkid = (Integer) outit.next();
        Link l = g.getLink(linkid.intValue());
        int e = l.getEnd();
        long w = (long) (2.0*Math.ceil(l.getWeight()));  // *MeTiS accepts int weights
        pw.print(e+" "+w+" ");
      }
      pw.println("");
    }
    pw.flush();
    pw.close();
  }


  /**
   * writes the vectors in the 1st argument to file specified in 3rd arg.
   * according to the format specified in <CODE>readVectorsFromFile()</CODE>.
   * @param docs Vector // Vector&lt;VectorIntf&gt;
   * @param tot_dims int
   * @param filename String
   * @throws IOException
   */
  public static void writeVectorsToFile(Vector docs, int tot_dims, String filename) throws IOException {
    PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
    final int docs_size = docs.size();
    pw.println(docs_size+" "+tot_dims);
    for (int i=0; i<docs_size; i++) {
      VectorIntf di = (VectorIntf) docs.elementAt(i);
      StringBuffer lineb = new StringBuffer();
      for (int j=0; j<tot_dims; j++) {
        double val = di.getCoord(j);
        if (Double.compare(val,0.0)==0) continue;  // ignore zero values
        lineb.append((j+1));
        lineb.append(",");
        lineb.append(val);
        lineb.append(" ");
      }
      String line = lineb.toString();
      pw.println(line);
    }
    pw.flush();
    pw.close();
  }


  /**
   * writes the sparse vectors in the 1st argument to file specified in 3rd arg.
   * according to the format specified in <CODE>readSparseVectorsFromFile()</CODE>.
   * @param docs Vector // Vector&lt;SparseVectorIntf&gt;
   * @param tot_dims int
   * @param filename String
   * @throws IOException
   */
  public static void writeSparseVectorsToFile(Vector docs, int tot_dims, String filename) throws IOException {
    PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
    final int docs_size = docs.size();
    pw.println(docs_size+" "+tot_dims);
    for (int i=0; i<docs_size; i++) {
      SparseVectorIntf di = (SparseVectorIntf) docs.elementAt(i);
      StringBuffer lineb = new StringBuffer();
      for (int j=0; j<di.getNumNonZeros(); j++) {
        int posj = di.getIthNonZeroPos(j);
        double val = di.getCoord(posj);
        if (Double.compare(val,0.0)==0) continue;  // ignore zero values
        lineb.append((posj+1));
        lineb.append(",");
        lineb.append(val);
        lineb.append(" ");
      }
      String line = lineb.toString();
      pw.println(line);
    }
    pw.flush();
    pw.close();
  }


  /**
   * writes the int[] given in the first argument to the file given in the
   * second argument.
   * @param indices int[] the asgn to write
   * @param filename String
   * @throws IllegalArgumentException
   * @throws FileNotFoundException
   */
  public static void writeIntArrayToFile(int indices[], String filename) throws IllegalArgumentException, FileNotFoundException {
    //System.err.println("writing labels to file "+filename);
    PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
    if (indices==null) throw new IllegalArgumentException("no clustering soln available");
    for (int i=0; i<indices.length; i++) {
      pw.println(indices[i]);  // used to be pw.println(indices[i] - 1);
    }
    pw.flush();
    pw.close();
  }

}

