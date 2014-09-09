package parallel;

import java.util.*;

class ST extends Thread {
  private int _id;
  private Random _r;
  private Semaphore _sem;

  public ST(int id, Semaphore sem) {
    _id = id;
    _sem = sem;
    _r = new java.util.Random();
  }

  public void run() {
    int pid = _sem.acquire();
    synchronized (SemaphoreTest.class) {
      System.out.println("Thread-id=" + _id + " has permit-id=" + pid);
    }
    SemaphoreTest.addToWorking(_id);
    try {
      Thread.sleep(_r.nextInt(1000));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    SemaphoreTest.removeFromWorking(_id);
    try {
      _sem.release();
      synchronized (SemaphoreTest.class) {
        System.out.println("Thread-id=" + _id + " released permit-id=" + pid);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}

public class SemaphoreTest {

  private static Set _permits = new TreeSet();

  public static synchronized void addToWorking(int id) {
    _permits.add(new Integer(id));
    System.out.print("Threads w/ permit: [");
    Iterator it = _permits.iterator();
    while (it.hasNext()) {
      Integer i = (Integer) it.next();
      System.out.print(i.intValue()+" ");
    }
    System.out.println("]");
  }

  public static synchronized void removeFromWorking(int id) {
    _permits.remove(new Integer(id));
  }

  public static void main(String[] args) {
    Semaphore sem1 = new Semaphore(10);

    for (int i=0; i<100; i++) {
      ST sti = new ST(i, sem1);
      sti.start();
    }
  }
}
