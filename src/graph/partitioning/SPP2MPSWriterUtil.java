package graph.partitioning;

import cern.colt.matrix.*;
import java.io.*;
import java.util.*;
import java.text.*;
import java.lang.reflect.*;


public class SPP2MPSWriterUtil {
  static int pos[] = {2, 5, 15, 25, 40, 50};
  static int poslen[] = {2, 8, 8, 12, 8, 12};


  public static void writeSPP2MPSFileFast(DoubleMatrix2D A, DoubleMatrix1D c, int k, String filename) throws IOException {
    PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
    // write problem name
    pw.println("NAME           SETPARPROB");
    // write objsense
    pw.println("OBJSENSE");
    pw.println(" MIN");
    pw.println("ROWS");
    pw.println(" N  COST");
    int num_vars = A.columns();
    int num_constrs = A.rows()+1;
    // declare the A rows
    for (int i=0; i<num_constrs-1; i++) {
      StringBuffer l = newMPSLineBuffer();
      writeMPSFieldFast(l, 0, "E");
      String ci = "CON"+i;
      writeMPSFieldFast(l, 1, ci);
      pw.println(l);
    }
    pw.println(" E  PARCTR");  // the c'x = k
    // now the vars
    pw.println("COLUMNS");
    pw.flush();
    // NumberFormat nf = new DecimalFormat("0.#######E0");
    NumberFormat nf = new DecimalFormat("00000000E0");
    for (int i=0; i<num_vars; i++) {
      if (i%1000==0) System.out.println("Now writing column "+i);  // itc: HERE rm asap
      StringBuffer l = newMPSLineBuffer();
      writeMPSFieldFast(l, 1, "X"+i);
      writeMPSFieldFast(l, 2, "COST");
      double ci = c.getQuick(i);
      String cival = nf.format(ci);
      writeMPSFieldFast(l, 3, cival);
      writeMPSFieldFast(l, 4, "CON0");
      String ai1val = nf.format(A.getQuick(0, i));
      writeMPSFieldFast(l, 5, ai1val);
      pw.println(l);
      pw.flush();
      // print the A coeff for x_i var
      for (int j=1; j<num_constrs-1; j++) {
        l = newMPSLineBuffer();
        writeMPSFieldFast(l, 1, "X"+i);
        writeMPSFieldFast(l, 2, "CON"+j);
        String aijval = nf.format(A.getQuick(j,i));
        writeMPSFieldFast(l, 3, aijval);
        if (j==num_constrs-2) {
          if (Math.abs(A.getQuick(j,i))>1.e-8) {  // print only nonzero values
            pw.println(l);
            pw.flush();
          }
          break;
        }
        j++;
        writeMPSFieldFast(l, 4, "CON"+j);
        aijval = nf.format(A.getQuick(j,i));
        writeMPSFieldFast(l, 5, aijval);
        if (Math.abs(A.getQuick(j,i))>1.e-8 || Math.abs(A.getQuick(j-1,i))>1.e-8) {
          // only print a line having at least one non-zero element
          pw.println(l);
          pw.flush();
        }
      }
      // last constraint c'x=k
      l = newMPSLineBuffer();
      writeMPSFieldFast(l, 1, "X"+i);
      writeMPSFieldFast(l, 2, "PARCTR");
      writeMPSFieldFast(l, 3, "1");
      pw.println(l);
      pw.flush();
    }
    // RHS
    pw.println("RHS");
    for (int i=0; i<num_constrs-1; i++) {
      StringBuffer l = newMPSLineBuffer();
      writeMPSFieldFast(l, 1, "RHS1");
      writeMPSFieldFast(l, 2, "CON"+i);
      writeMPSFieldFast(l, 3, "1");
      pw.println(l);
    }
    pw.println("    RHS1      PARCTR    "+k);
    pw.flush();
    // BOUNDS
    pw.println("BOUNDS");
    for (int i=0; i<num_vars; i++) {
      StringBuffer l = newMPSLineBuffer();
      writeMPSFieldFast(l, 0, "UI");
      writeMPSFieldFast(l, 1, "BOUND");
      writeMPSFieldFast(l, 2, "X"+i);
      writeMPSFieldFast(l, 3, "1");
      pw.println(l);
    }
    // end
    pw.println("ENDATA");
    pw.flush();
    pw.close();
  }
  public static void writeLP2MPSFileFast(DoubleMatrix2D A, DoubleMatrix1D c, int k, String filename) throws IOException {
    PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
    // write problem name
    pw.println("NAME           SETCOVPROB");
    pw.println("ROWS");
    pw.println(" N  COST");
    int num_vars = A.columns();
    int num_constrs = A.rows()+1;
    // declare the A rows
    for (int i=0; i<num_constrs-1; i++) {
      StringBuffer l = newMPSLineBuffer();
      writeMPSFieldFast(l, 0, "G");
      String ci = "CON"+i;
      writeMPSFieldFast(l, 1, ci);
      pw.println(l);
    }
    pw.println(" E  COVCTR");  // the c'x = k
    // now the vars
    pw.println("COLUMNS");
    pw.flush();
    // NumberFormat nf = new DecimalFormat("0.#######E0");
    NumberFormat nf = new DecimalFormat("00000000E0");
    for (int i=0; i<num_vars; i++) {
      if (i%1000==0) System.out.println("Now writing column "+i);  // itc: HERE rm asap
      StringBuffer l = newMPSLineBuffer();
      writeMPSFieldFast(l, 1, "X"+i);
      writeMPSFieldFast(l, 2, "COST");
      double ci = c.getQuick(i);
      String cival = nf.format(ci);
      writeMPSFieldFast(l, 3, cival);
      writeMPSFieldFast(l, 4, "CON0");
      String ai1val = nf.format(A.getQuick(0, i));
      writeMPSFieldFast(l, 5, ai1val);
      pw.println(l);
      pw.flush();
      // print the A coeff for x_i var
      for (int j=1; j<num_constrs-1; j++) {
        l = newMPSLineBuffer();
        writeMPSFieldFast(l, 1, "X"+i);
        writeMPSFieldFast(l, 2, "CON"+j);
        String aijval = nf.format(A.getQuick(j,i));
        writeMPSFieldFast(l, 3, aijval);
        if (j==num_constrs-2) {
          if (Math.abs(A.getQuick(j,i))>1.e-8) {  // print only nonzero values
            pw.println(l);
            pw.flush();
          }
          break;
        }
        j++;
        writeMPSFieldFast(l, 4, "CON"+j);
        aijval = nf.format(A.getQuick(j,i));
        writeMPSFieldFast(l, 5, aijval);
        if (Math.abs(A.getQuick(j,i))>1.e-8 || Math.abs(A.getQuick(j-1,i))>1.e-8) {
          // only print a line having at least one non-zero element
          pw.println(l);
          pw.flush();
        }
      }
      // last constraint c'x=k
      l = newMPSLineBuffer();
      writeMPSFieldFast(l, 1, "X"+i);
      writeMPSFieldFast(l, 2, "COVCTR");
      writeMPSFieldFast(l, 3, "1");
      pw.println(l);
      pw.flush();
    }
    // RHS
    pw.println("RHS");
    for (int i=0; i<num_constrs-1; i++) {
      StringBuffer l = newMPSLineBuffer();
      writeMPSFieldFast(l, 1, "RHS1");
      writeMPSFieldFast(l, 2, "CON"+i);
      writeMPSFieldFast(l, 3, "1");
      pw.println(l);
    }
    pw.println("    RHS1      COVCTR    "+k);
    pw.flush();
    // BOUNDS
    pw.println("BOUNDS");
    for (int i=0; i<num_vars; i++) {
      StringBuffer l = newMPSLineBuffer();
      writeMPSFieldFast(l, 0, "UP");
      writeMPSFieldFast(l, 1, "BOUND");
      writeMPSFieldFast(l, 2, "X"+i);
      writeMPSFieldFast(l, 3, "1");
      pw.println(l);
    }
    // end
    pw.println("ENDATA");
    pw.flush();
    pw.close();
  }

/*
  private static String writeMPSField(String line, int fieldnum, String val) throws IOException {
    // line must be of length 61
    // assert
    if (line==null || line.length()!=61) throw new IOException("incorrect line");
    String newline = line.substring(0, pos[fieldnum]-1);
    int val_len = val.length()<=poslen[fieldnum] ? val.length() : poslen[fieldnum];
    String new_val = val.substring(0, val_len);  // cut-off the value of val if it's too big
    newline += new_val;
    for(int i=0; i<poslen[fieldnum]-val_len; i++) newline+=" ";
    if (line.length()>newline.length()) newline += line.substring(newline.length());
    return newline;
  }
*/

  private static void writeMPSFieldFast(StringBuffer line, int fieldnum, String val) throws IOException {
    // line must be of length 61
    // assert
    if (line==null || line.length()!=61) throw new IOException("incorrect line: ["+line+"]");
    int val_len = val.length()<=poslen[fieldnum] ? val.length() : poslen[fieldnum];
    int endpos = pos[fieldnum]-1+val_len;
    line.replace(pos[fieldnum]-1, endpos, val);
    return;
  }


  private static String newMPSLine() {
    return "                                                             ";
  }


  private static StringBuffer newMPSLineBuffer() {
    return new StringBuffer("                                                             ");
  }


  private static void writeSpaces(PrintWriter pw, int numspaces) {
    for (int i=0; i<numspaces; i++) pw.print(" ");
  }

}

