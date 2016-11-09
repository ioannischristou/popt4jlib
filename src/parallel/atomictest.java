package parallel;

class T extends Thread {
  String _s1, _s2 = null;

  public T(String s1, String s2) {
    _s1 = s1; _s2 = s2;
  }

  public void run() {

    for (int i=0; i<200; i++) {
      System.out.println(_s2+" "+i);
    }

    atomic.start(1);
    for (int i=200; i<250; i++) {
      System.out.println(_s1+" "+ i);
    }
    atomic.end(1);

    for (int i=250; i<270; i++) {
      System.out.println(_s2+" "+i);
    }

    atomic.start(2);
    for (int i=270; i<370; i++) {
      System.out.println(_s1+" "+i);
    }
    atomic.end(2);
  }
}

public class atomictest {

  public static void main(String[] args) {
    T t1 = new T("a", "i");
    T t2 = new T("b", "j");

    t1.start();
    t2.start();
  }
}
