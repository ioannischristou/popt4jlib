package graph;

public class LinkPair {
  private int _start;
  private int _end;
  private double _w;

  public LinkPair(int start, int end, double w) {
    _start = start; _end = end; _w = w;
  }


  public boolean equals(Object o) {
    if (o==null) return false;
    try {
      LinkPair l = (LinkPair) o;
      return (_start == l._start && _end == l._end);
    }
    catch (ClassCastException e) {
      return false;
    }
  }


  public int hashCode() {
    return _start + _end;
  }


  public int getStart() { return _start; }
  public int getEnd() { return _end; }
  public double getWeight() { return _w; }


  public double addWeight(double w) {
    _w += w;
    return _w;
  }
}
