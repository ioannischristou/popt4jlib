package tests;

import popt4jlib.*;
import parallel.*;
import parallel.distributed.*;


/**
 * tests the popt4jlib.DblArray1SparseVector[MT/FE/...] class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class SparseVectorTest {

  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; tests.SparseVectorTest [num_dimensions(100)] [sparsity_factor(2)]</CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      main1(args);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }


  public static void main1(String[] args) throws ParallelException {
    int n = 100;
    if (args.length>0) n = Integer.parseInt(args[0]);
    int f = 2;
    if (args.length>1) f = Integer.parseInt(args[1]);
    if (f<=1) f = 2;  // minimum sparsity degree
    int dim = (int)Math.ceil((double)n/(double)f);
    System.err.println("dim="+dim);
    int[] inds = new int[dim];
    double[] vals = new double[dim];
    int j=0;
    for (int i=0; i<n; i++) {
      if (i%f==0) {
        inds[j] = i;
        vals[j++] = i + 1;
      }
    }
    try {
      DblArray1SparseVectorMT.setExecutor(4);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
    // set values for sparse vectors
    DblArray1SparseVectorMT sv = new DblArray1SparseVectorMT(inds, vals, n);
    DblArray1SparseVector sv1 = new DblArray1SparseVector(n);
    double[] x1 = new double[n];
    for (int i=0; i<inds.length; i++) {
      sv1.setCoord(inds[i],vals[i]);
      x1[inds[i]] = vals[i];
    }
    DblArray1Vector vs = new DblArray1Vector(x1);
    /*
    System.out.println(sv);
    for (int i=0; i<n; i++) {
      System.out.println("sv["+i+"]="+sv.getCoord(i));
    }
    */
    // tests for sv, sv1, x1
    if (sv.equals(sv1)==false) {
      System.err.println("sv!=sv1");
      System.exit(-1);
    }
    if (vs.equals(sv)==false) {
      System.err.println("vs!=sv");
      System.exit(-1);
    }
    double svn = sv.norm2();
    double sv1n = sv1.norm2();
    double svsv1diff = Math.abs(svn-sv1n);
    if (svsv1diff>1.e-50) {
      System.err.println("svsv1diff="+svsv1diff);
      System.exit(-1);
    }
    double vsn = popt4jlib.GradientDescent.VecUtil.norm2(vs);
    double sv1vsdiff = Math.abs(vsn-sv1n);
    if (svsv1diff>1.e-50) {
      System.err.println("sv1vsdiff="+sv1vsdiff);
      System.exit(-1);
    }
    // sanity test
    DblArray1SparseVector sv1k = new DblArray1SparseVector(153);
    for (int i=100; i>=5; i--) {
      if (i%3==0) sv1k.setCoord(i,2.0);
    }
    sv1k.setCoord(1,1.0);
    //sv1k.setCoord(152,1.0);
    // now set again the coords
    System.err.println("sv1k.nnz="+sv1k.getNumNonZeros());
    System.err.println("before sv1k="+sv1k);
    for (int i=0; i<sv1k.getNumNonZeros(); i++) {
      sv1k.setCoord(sv1k.getIthNonZeroPos(i),1.0);
    }
    System.err.println("sv1k.nnz="+sv1k.getNumNonZeros());
    System.err.println("after sv1k="+sv1k);

    DblArray1SparseVectorMT svcopy = (DblArray1SparseVectorMT) sv.newInstance();  // used to be sv.newCopy();
    if (svcopy.equals(sv)) System.err.println("DblArray1SparseVectorMT.newCopy() works");
    else System.err.println("insanity on newCopy() or equals() for DblArray1SparseVectorMT");
    svcopy.setCoord(0,-1);
    if (svcopy.equals(sv)==false) System.err.println("DblArray1SparseVectorMT.newCopy() works");
    else System.err.println("insanity on newCopy() or equals() for DblArray1SparseVectorMT");


    double[] x = new double[n];
    for (int i=0; i<n; i++) x[i] = 1;
    DblArray1Vector v = new DblArray1Vector(x);
    long start = System.currentTimeMillis();
    double ip = popt4jlib.GradientDescent.VecUtil.innerProduct(sv,v);
    long dur = System.currentTimeMillis()-start;
    System.out.println("normal inner-product="+ip+" took "+dur+" msecs");
    start = System.currentTimeMillis();
    ip = sv.innerProduct(v);
    dur = System.currentTimeMillis()-start;
    System.out.println("sparse-vector inner-product="+ip+" took "+dur+" msecs");
    for (int i=0; i<n; i++) sv.setCoord(i,i);
    System.out.println("sv="+sv);
  }
}
