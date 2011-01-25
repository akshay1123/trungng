package edu.kaist.uilab.plda;

/**
 * A naive implementation of Gibbs sampler for LDA.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class GibbsSampler {
  private int numTopics; // T = numTopics
  private int vocabularySize; // V = vocabularySize
  private int numDocuments; // D = number of documents
  private int[][] documents; // documents[m][n] = (index of the n_th word in document m) = i

  // hyper-parameters
  private double alpha = 0.1; // default value
  private double beta = 0.01; // default value

  // sampling parameters and variables
  private int numIterations = 1000;
  private int burnIn = 200;
  private int sampleLags = 20; // number of sample lags (to prevent correlation)
  // (default = 50)
  private int numSamples = 1; // number of samples to take (default = 1)
  private int[][] z; // topic assignment for each word z[document][word]
  private int cwt[][]; // word-topic count = cwt[i][k] word i to topic k
  private int cdt[][]; // document-topic count = cdt[m][k] words in document m
  // to topic k
  private int cwtsum[]; // cwtsum[k] = # words assigned to topic k
  private int cdtsum[]; // cdtsum[m] = # words in document m

  private double thetasum[][]; // D x K : sum of samples (to return the average
  // sample)
  private double phisum[][]; // K X V : sum of samples (to return the average

  // sample)

  /**
   * Default constructor -- for testing purpose only.
   */
  public GibbsSampler() {
  }

  /**
   * Constructs a new GibbsSampler with given model parameters.
   * 
   * @param numTopics
   *          the number of topics
   * @param vocabularySize
   *          the vocabulary size
   * @param documents
   *          the terms/words matrix
   * @param alpha
   *          the topic prior
   * @param beta
   *          the
   */
  public GibbsSampler(int numTopics, int vocabularySize, int[][] documents,
      double alpha, double beta) {
    this.numTopics = numTopics;
    this.vocabularySize = vocabularySize;
    this.documents = documents;
    this.alpha = alpha;
    this.beta = beta;
    this.numDocuments = documents.length;
  }

  public static void main(String args[]) {
    // words in documents
    int[][] documents = { { 1, 4, 3, 2, 3, 1, 4, 3, 2, 3, 1, 4, 3, 2, 3, 6 },
        { 2, 2, 4, 2, 4, 2, 2, 2, 2, 4, 2, 2 },
        { 1, 6, 5, 6, 0, 1, 6, 5, 6, 0, 1, 6, 5, 6, 0, 0 },
        { 5, 6, 6, 2, 3, 3, 6, 5, 6, 2, 2, 6, 5, 6, 6, 6, 0 },
        { 2, 2, 4, 4, 4, 4, 1, 5, 5, 5, 5, 5, 5, 1, 1, 1, 1, 0 },
        { 5, 4, 2, 3, 4, 5, 6, 6, 5, 4, 3, 2 } };
    int vocabularySize = 7;
    int numTopics = 2;
    double alpha = 2;
    double beta = .5;
    System.out.println("Latent Dirichlet Allocation using Gibbs Sampling.");
    GibbsSampler sampler = new GibbsSampler(numTopics, vocabularySize, documents, alpha, beta);
    sampler.setSamplerParameters(10000, 2000, 100, 10);
    sampler.doGibbsSampling();

    double[][] theta = sampler.getTheta();
    double[][] phi = sampler.getPhi();

    System.out.print("\n\n");
    System.out.println("Document--Topic Associations, Theta[d][k] (alpha=" + alpha + ")");
    for (int m = 0; m < documents.length; m++) {
      for (int k = 0; k < numTopics; k++) {
        System.out.print(String.format("%.4f  ", theta[m][k]));
      }
      System.out.println();
    }
    System.out.println("Topic--Term Associations, Phi[i][k] (beta=" + beta + ")");
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < vocabularySize; i++) {
        System.out.print(String.format("%.4f  ", phi[k][i]));
      }
      System.out.println();
    }
  }

  /**
   * Sets the parameters of the sampler.
   * 
   * @param maxIterations
   *       the number of max iterations to run (default value is 1000)
   * @param burnIn
   *       the number of iterations to be counted as burn-in period (must be at least 100,
   *       default value is 200)
   * @param numSamples
   *       the number of samples to be collected (must be at least than 1, default value is 1)
   * @param sampleLags
   *       the sample lags (must be at least 1, default value is 20)
   */
  public void setSamplerParameters(int maxIterations, int burnIn, int sampleLags, int numSamples) {
    this.numIterations = maxIterations > 150 ? maxIterations : 150;
    this.burnIn = burnIn > 100 ? burnIn : 100;
    this.sampleLags = sampleLags > 0 ? sampleLags : 1;
    this.numSamples = numSamples > 0 ? numSamples : 1;
  }
  
  /**
   * Runs the Gibbs sampler.
   */
  public void doGibbsSampling() {
    System.out.print("Initializing parameters...");
    initialize();
    System.out.println("done");

    int samplesCollected = 0;
    System.out.println("Burning in period...");
    for (int iter = 0; iter < numIterations; iter++) {
      if (iter < burnIn) {
        System.out.print(iter + " ");
        if (iter % 100 == 0) {
          System.out.println();
        }
      } else if (iter == burnIn) {
        System.out.println("\nBurning in done");
      } 

      // sampling each hidden variable z_i
      for (int m = 0; m < numDocuments; m++) {
        for (int n = 0; n < documents[m].length; n++) {
          z[m][n] = sampleFullConditional(m, n);
        }
      }

      // after burn-in & some sample lags we can collect a sample
      // note that we are not saving z[m][n] for now
      if (iter > burnIn && iter % sampleLags == 0) {
        System.out.println(String.format("Collected a sample at iteration %d",
            iter));
        updateParams();
        samplesCollected++;
        if (samplesCollected > numSamples) {
          return; // enough samples has been collected
        }
      }
    }
  }

  /**
   * Returns the estimated theta values of this sampler.
   * 
   * <p>
   * If the number of samples was set to be greater than 0, this is the average
   * of the estimated value of each sample collected.
   * 
   * @return
   */
  public double[][] getTheta() {
    double[][] theta = new double[numDocuments][numTopics];
    for (int m = 0; m < numDocuments; m++) {
      for (int k = 0; k < numTopics; k++) {
        theta[m][k] = thetasum[m][k] / numSamples;
      }
    }

    return theta;
  }

  /**
   * Returns the estimated phi values of this sampler.
   * 
   * <p>
   * If the number of samples was set to be greater than 0, this is the average
   * of the estimated value of each sample collected.
   * 
   * @return
   */
  public double[][] getPhi() {
    double[][] phi = new double[numTopics][vocabularySize];
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < vocabularySize; i++) {
        phi[k][i] = phisum[k][i] / numSamples;
      }
    }

    return phi;
  }

  /**
   * Initializes the model (assign random values to hidden variables -- the
   * topics z).
   */
  private void initialize() {
    // initialize count variables
    cwt = new int[vocabularySize][numTopics];
    cwtsum = new int[numTopics];
    cdt = new int[numDocuments][numTopics];
    cdtsum = new int[numDocuments];

    // sample values of z_i randomly ([1..numTopics] as the initial state of the
    // Markov chain
    z = new int[numDocuments][];
    for (int m = 0; m < numDocuments; m++) {
      int N = documents[m].length;
      int k; // the sample topic
      z[m] = new int[N];
      cdtsum[m] = N; // cdtsum[m] = number of words in document m
      for (int n = 0; n < N; n++) {
        k = (int) (Math.random() * numTopics);
        z[m][n] = k;
        cwt[documents[m][n]][k]++; // word i assigned to topic k
        cdt[m][k]++; // word i in document m assigned to topic k
        cwtsum[k]++; // total number of words assigned to topic k
      }
    }

    thetasum = new double[numDocuments][numTopics];
    phisum = new double[numTopics][vocabularySize];
  }

  /**
   * Updates the parameters when a new sample is collected.
   */
  private void updateParams() {
    // thetasum[][] (D x K) -- sum of samples (to return the average sample)
    double tAlpha = numTopics * alpha;
    for (int m = 0; m < numDocuments; m++) {
      for (int k = 0; k < numTopics; k++) {
        thetasum[m][k] += (cdt[m][k] + alpha) / (cdtsum[m] + tAlpha);
      }
    }

    // phisum[][] (K X V) -- sum of samples (to return the average sample)
    double vBeta = vocabularySize * beta;
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < vocabularySize; i++) {
        phisum[k][i] += (cwt[i][k] + beta) / (cwtsum[k] + vBeta);
      }
    }
  }

  /**
   * Samples a topic for the n_th word in document m.
   * 
   * @param m
   *          the document
   * @param n
   *          the index (position) of the word in this document
   * 
   * @return the topic
   */
  private int sampleFullConditional(int m, int n) {
    int i, topic = 0;
    i = documents[m][n];
    topic = z[m][n];
    // not counting i_th word
    cwt[i][topic]--;
    cdt[m][topic]--;
    cwtsum[topic]--;
    cdtsum[m]--;

    double[] p = new double[numTopics];
    double vBeta = vocabularySize * beta;
    double tAlpha = numTopics * alpha;
    for (int k = 0; k < numTopics; k++) {
      p[k] = (cwt[i][k] + beta) / (cwtsum[k] + vBeta) * (cdt[m][k] + alpha) / (cdtsum[m] + tAlpha);
    }
    topic = sample(p);

    // assign new topic to the i_th word
    z[m][n] = topic;
    cwt[i][topic]++;
    cdt[m][topic]++;
    cwtsum[topic]++;
    cdtsum[m]++;

    return topic;
  }

  /**
   * Samples a value from a discrete distribution.
   * 
   * <p>
   * The method can modify the parameter {@code p} as it wants because {@code p}
   * is not needed afterward in the calling method.
   * 
   * @param p
   *          the unnormalized distribution
   */
  int sample(double p[]) {
    int T = p.length;
    int topic; // the sample

    // turning p into a cumulative distribution
    for (int i = 1; i < T; i++) {
      p[i] += p[i - 1];
    }

    // scaled sample because of unnormalized p
    double u = Math.random() * p[T - 1];
    // find the interval which contains u
    for (topic = 0; topic < T; topic++) {
      if (u < p[topic]) {
        break;
      }
    }

    return topic;
  }
}
