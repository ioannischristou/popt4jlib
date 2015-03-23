package parallel;

import java.util.Vector;

public class FPABTETest {
  public FPABTETest() {
  }

  public static void main(String[] args) {
    if (args.length<3) {
      System.err.println("usage: java -cp <classpath> parallel.FPABTETest <j0> <numtasks> <numthreads> [range](def: 1000)");
      System.exit(-1);
    }
    try {
      long start = System.currentTimeMillis();
      Vector tasks = new Vector();
      java.util.Random r = new java.util.Random(7);
      final long j0 = Long.parseLong(args[0]);
      final int numtries = Integer.parseInt(args[1]);
      final int numthreads = Integer.parseInt(args[2]);
      int range = 1000;
      if (args.length>3) range = Integer.parseInt(args[3]);
      FasterParallelAsynchBatchTaskExecutor fpabte = 
							FasterParallelAsynchBatchTaskExecutor.
											newFasterParallelAsynchBatchTaskExecutor(numthreads);
      for (int i=0; i<numtries; i++) {
        long j1 = j0*r.nextInt();
        if (j1<0) j1 = -j1;
        long j2 = j1 + range;
        TestTask tti = new TestTask(i, j1, j2);
        tasks.add(tti);
      }
      fpabte.executeBatch(tasks);
      fpabte.shutDown();
      long dur = System.currentTimeMillis()-start;
      System.out.println("Executed in "+dur+" msecs.");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}

