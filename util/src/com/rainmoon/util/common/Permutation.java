package com.rainmoon.util.common;
import java.util.Random;

/**
 * Class that provide utilities function for dealing with permutation.
 * 
 * @author Trung
 * 
 */
public class Permutation {

  /**
   * Returns true if the given array is a permutation of numbers.
   * 
   * @param p
   * @return
   */
  public boolean isPermutation(int[] p) {
    int count[] = new int[p.length];
    for (int i = 0; i < count.length; i++) {
      count[i] = 0;
    }
    for (int i = 0; i < p.length; i++) {
      count[p[i]]++;
      if (count[p[i]] > 1) {
        return false;
      }
    }
    
    return true;
  }

  /**
   * Generates a permutation of the given list.
   * 
   * @param p
   */
  public void generateNewPermutation(int[] p) {
    int max = p.length;
    Random rand = new Random();
    // shuffle inputs 300 times
    int x, y, swap;
    for (int i = 0; i < 300; i++) {
      x = rand.nextInt(max);
      y = rand.nextInt(max);
      swap = p[x];
      p[x] = p[y];
      p[y] = swap;
    }

    System.out.println("Permutation: ");
    for (int i = 0; i < max; i++) {
      System.out.print(p[i] + " ");
    }
  }
}
