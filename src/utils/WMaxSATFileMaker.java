package utils;

import graph.*;
import java.io.*;
import java.util.*;


/**
 * utility class that takes as argument a graph file and converts it into a
 * file whose format is accepted as an input file for a weighted MAX-SAT problem
 * solver.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class WMaxSATFileMaker {
  private WMaxSATFileMaker() {
  }

  private static void convert2WMaxSATFormat(String graphfile, String wcnffile) throws GraphException, IOException {
    Graph g = utils.DataMgr.readGraphFromFile2(graphfile);
    // compute num-clauses
    final int n = g.getNumNodes();
    Set clauses = new HashSet();  // Set<IntSet>
    for (int i=0; i<n; i++) {
      Node ni = g.getNode(i);
      Set nsi = ni.getNbors();
      int[] nborsi = new int[nsi.size()+1];
      Iterator iter = nsi.iterator();
      int j=0;
      while (iter.hasNext()) {
        Node nn = (Node) iter.next();
        nborsi[j++] = nn.getId()+1;
      }
      nborsi[j] = i+1;
      for (int j1=0; j1<nborsi.length; j1++) {
        for (int j2=j1+1; j2<nborsi.length; j2++) {
          Set s12 = new TreeSet();
          s12.add(new Integer(nborsi[j1]));
          s12.add(new Integer(nborsi[j2]));
          clauses.add(s12);
        }
      }
    }
    PrintWriter pw = new PrintWriter(new FileWriter(wcnffile));
    pw.println("c Weighted MAXSAT formulation of graph "+graphfile);
    pw.println("p wcnf "+n+" "+(clauses.size()+n)+" "+(clauses.size()*2));
    Iterator it = clauses.iterator();
    while (it.hasNext()) {
      Set is = (TreeSet) it.next();
      pw.print((clauses.size()*2)+" ");
      Iterator it2 = is.iterator();
      while (it2.hasNext()) {
        Integer ii = (Integer) it2.next();
        pw.print(-ii.intValue()+" ");
      }
      pw.println("0");
    }
    for (int i=1; i<=n; i++) {
      pw.println("1 "+i+ " 0");
    }
    pw.flush();
    pw.close();
  }

  /**
   * invoke as:
   * <CODE>java -cp utils.WMaxSATFileMaker &lt;inputgraphfilename&gt;
   * &lt;outputwmaxsatfilename&gt;</CODE>.
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      convert2WMaxSATFormat(args[0], args[1]);
      System.out.println("Done.");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}

