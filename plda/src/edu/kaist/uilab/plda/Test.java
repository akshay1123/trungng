package edu.kaist.uilab.plda;

public class Test {
  public static void main(String args[]) {
    long start = System.currentTimeMillis();
    for (int i = 0; i < 300000000; i++) {
      int x = 3;
      int y = 4;
      int z = x * y + x - y + (x / y) + (x * y * y);
    }
    System.out.println(System.currentTimeMillis() - start);
  }
}
