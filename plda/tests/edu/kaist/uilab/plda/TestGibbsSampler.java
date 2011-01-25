package edu.kaist.uilab.plda;

import junit.framework.TestCase;

/**
 * Tests for {@link GibbsSampler}.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class TestGibbsSampler extends TestCase {
  
  /**
   * Tests if the values sampled have approximate probability.
   */
  public void testSample() {
    GibbsSampler sampler = new GibbsSampler();
    final double[] p = new double[] {0.45, 0.2, 0.35};
    final int[] sampleCounts = new int[p.length];
    final int numSamples = 2000;
    int sample;
    for (int i = 0; i < numSamples; i++) {
      sample = sampler.sample(new double[] {0.45, 0.2, 0.35});
      assertTrue(sample >= 0 && sample < p.length);
      sampleCounts[sample]++;
    }
    
    double delta = 0.02;
    double difference = 0.0;
    for (int i = 0; i < p.length; i++) {
      difference = Math.abs((((double) sampleCounts[i]) / numSamples) - p[i]);
      System.out.println(difference);
      assertTrue(difference < delta);
    }
  }
}
