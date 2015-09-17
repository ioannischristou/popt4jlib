package tests;

import popt4jlib.*;
import popt4jlib.GradientDescent.ArmijoSteepestDescent;
import popt4jlib.GA.*;
import java.util.*;

/**
 * unimportant test for Hashtable functionality. Not intended to be part of the
 * final library.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class HashtableTest {
  public HashtableTest() {
  }

  public static void main(String[] args) {
    HashMap params = new HashMap();
    params.put("smth",new Integer(1));
    ArmijoSteepestDescent opter = new ArmijoSteepestDescent(params);
    HashMap paramscopy = opter.getParams();
    paramscopy.put("smth", new Integer(2));
    Integer origval = (Integer) opter.getParams().get("smth");
    System.out.println("params.get(smth)="+origval.intValue());
    Integer newval = (Integer) paramscopy.get("smth");
    System.out.println("paramscopy.get(smth)="+newval.intValue());
  }
}
