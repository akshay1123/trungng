package com.rainmoon.util.common;
/**
 * Class that represents a pair of integers.
 * 
 * @author Trung
 *
 */
public class Pair {
  public Pair(int u, int v) {
    this.u = u;
    this.v = v;
  }
  
  @Override
  public String toString() {
    return u + " " + v;
  }
  
  private int u, v;
}
