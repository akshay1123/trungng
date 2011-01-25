package edu.kaist.uilab.plda;

/**
 * A set of hidden variables that can be used with the full conditional sampling process.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class SamplingSet {
  int z; // topic z[i]
  int ro; // author ro[i]
  int s; // switch s[i]
  // full conditional probability of this sampling set p(z, ro, s|.)
  double p;
  
  public SamplingSet(int z, int ro, int s, double p) {
    this.z = z;
    this.ro = ro;
    this.s = s;
    this.p = p;
  }
}
