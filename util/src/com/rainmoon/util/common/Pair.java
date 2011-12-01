package com.rainmoon.util.common;
/**
 * Class that represents a pair of integers.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class Pair implements Comparable<Pair> {
  public Pair(int u, int v) {
    this.u = u;
    this.v = v;
  }

  /**
   * Compares this pair with another pair.
   */
  @Override
  public int compareTo(Pair p) {
    if (u == p.u && v == p.v) {
      return 0;
    }
    
    return u - p.u;
  }

  @Override
  public int hashCode() {
    return String.format("%d,%d", u, v).hashCode();
  }
  
  @Override
  public String toString() {
    return "( " + u + "," + v + ")";
  }
  
  private int u, v;
}
