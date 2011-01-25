package edu.kaist.uilab.plda;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.aliasi.symbol.SymbolTable;
import com.aliasi.util.ObjectToCounterMap;

import edu.kaist.uilab.plda.data.CorpusEntitySet;
import edu.kaist.uilab.plda.data.Entity;

/**
 * Implementation of the entity lda Gibbs sampler.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class EntityLdaGibbsSampler {
  private static final int DOCUMENT = 0;
  private static final int ENTITY = 1;

  private int numTopics; // T = numTopics
  private int vocabularySize; // V = vocabularySize
  private int numDocuments; // D = number of documents
  private int numEntities; // H = number of entities
  // documents[m][n] = (index of the n_th word in document m) = i  
  private int[][] documents;
  // documentEntities[m] = all entities of the m_th document
  private Entity[][] documentEntities;
  private SymbolTable symbolTable;
  private CorpusEntitySet corpusEntitySet;

  // hyper-parameters
  private double alpha = 0.1;
  private double beta = 0.01;
  private double gamma = 0.1;

  // sampling parameters and variables
  private int numIterations = 1000;
  private int burnIn = 200;
  private int sampleLags = 20; // number of sample lags (to prevent correlation)
  // (default = 50)
  private int numSamples = 1; // number of samples to take (default = 1)
  private int[][] z; // topic assignment for each word z[m][n] (z[i])
  private int[][] ro; // author assignment for each word i (ro[i])
  // switch for each word i :s[i]
  // s[i] = DOCUMENT: topic of assignment for this word is that of a document
  // s[i] = ENTITY: topic of assignment for this word is that of an entity
  private int[][] s;
  // V X T: word-topic count
  // cwt[i][k] = # times word i is assigned topic k
  private int cwt[][];
  // D x T: document-topic count
  // cdt[m][k] = # times that a word in document m is assigned topic k (OF
  // DOCUMENT m)
  private int cdt[][];
  // H X T: author-topic count
  // cpt[h][k] = # times that a word "of" person/entity h is assigned topic k
  // (OF ENTITY h)
  private int cpt[][];

  // cwtsum[k] = # words assigned to topic k
  private int cwtsum[];
  // cdtsum[m] = # words assigned to a topic of the document m
  private int cdtsum[];
  // cptsum[h] = # words assigned to a topic of the person/entity h
  private int cptsum[];

  // K X V : phi matrix of the current sample
  private double phi[][];
  // D x K : thetad matrix of the current sample
  private double thetad[][];
  // H x K : thetap matrix of the current sample
  private double thetap[][];
  
  // output parameters
  private String outputDir;
  private int maxWordsPerTopic;
  private int maxTopicsPerDoc;
  private int maxTopicsPerEntity;

  /**
   * Default constructor -- for testing purpose only.
   */
  public EntityLdaGibbsSampler() {
  }

  /**
   * Constructs a new GibbsSampler with given model parameters.
   * 
   * @param numTopics
   * @param vocabularySize
   * @param numEntities
   * @param documents
   * @param documentEntities
   * @param corpusEntitySet
   * @param alpha
   * @param beta
   * @param gamma
   */
  public EntityLdaGibbsSampler(int numTopics, int vocabularySize, int numEntities,
      int[][] documents, Entity[][] documentEntities, CorpusEntitySet corpusEntitySet,
      double alpha, double beta, double gamma) {
    this.numTopics = numTopics;
    this.vocabularySize = vocabularySize;
    this.numEntities = numEntities;
    this.documents = documents;
    this.documentEntities = documentEntities;
    this.corpusEntitySet = corpusEntitySet;
    this.alpha = alpha;
    this.beta = beta;
    this.gamma = gamma;
    this.numDocuments = documents.length;
  }

  /**
   * Sets the parameters of the sampler.
   * 
   * @param maxIterations
   *          the number of max iterations to run
   * @param burnIn
   *          the number of iterations to be counted as burn-in period
   * @param sampleLags
   *          the sample lags
   * @param numSamples
   *          the number of samples to be collected
   */
  public void setSamplerParameters(int maxIterations, int burnIn,
      int sampleLags, int numSamples) {
    this.numIterations = maxIterations;
    this.burnIn = burnIn;
    this.sampleLags = sampleLags;
    this.numSamples = numSamples;
  }

  /**
   * Sets parameters for reporting output.
   * 
   */
  public void setOutputParameters(SymbolTable symbolTable, String outputDir, int wordsPerTopic,
      int topicsPerDoc, int topicsPerEntity) {
    this.symbolTable = symbolTable;
    this.outputDir = outputDir;
    this.maxWordsPerTopic = wordsPerTopic;
    this.maxTopicsPerDoc = topicsPerDoc;
    this.maxTopicsPerEntity = topicsPerEntity;
  }
  
  /**
   * Runs the Gibbs sampler.
   */
  public void doGibbsSampling() throws IOException {
    System.out.print("Initializing parameters...");
    initialize();
    System.out.println("done");

    int samplesCollected = 0;
    System.out.println("Burning in period...");
    for (int iter = 0; iter < numIterations; iter++) {
      if (iter < burnIn) {
        System.out.print(iter + " ");
        if (iter % 100 == 99) {
          System.out.println();
        }
      } else if (iter == burnIn) {
        System.out.println("\nBurning in done");
      }

      // sampling each hidden variable z_i
      for (int m = 0; m < numDocuments; m++) {
        for (int n = 0; n < documents[m].length; n++) {
          SamplingSet sample = sampleFullConditional(m, n);
          z[m][n] = sample.z;
          ro[m][n] = sample.ro;
          s[m][n] = sample.s;
        }
      }

      // after burn-in & some sample lags we can collect a sample
      // note that we are not saving z[m][n] for now
      if (iter > burnIn && (iter - burnIn) % sampleLags == 0) {
        System.out.printf("\nCollected a sample at iteration %d", iter);
        updateParams();
        samplesCollected++;
        report(iter);
        if (samplesCollected > numSamples) {
          return; // enough samples has been collected
        }
      }
    }
  }

  /**
   * Returns the estimated theta_d values of the last sample collected.
   * 
   * <p>
   * If the number of samples was set to be greater than 0, this is the average
   * of the estimated value of each sample collected.
   * 
   * @return
   */
  public double[][] getThetad() {
    return thetad;
  }

  /**
   * Returns the estimated theta_p values of the last sample collected.
   * 
   * @return
   */
  public double[][] getThetap() {
    return thetap;
  }

  /**
   * Returns the estimated phi values of the last sample collected.
   * 
   * @return
   */
  public double[][] getPhi() {
    return phi;
  }
  
  public void printTopicTerms(double[][] phi, String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < vocabularySize; i++) {
        out.printf("%.10f,", phi[k][i]);
      }
      out.println();
    }
    out.close();
  }
  
  public void printDocumentTopics(double[][] thetad, String file)
      throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int m = 0; m < documents.length; m++) {
      for (int k = 0; k < numTopics; k++) {
        out.printf("%.10f,", thetad[m][k]);
      }
      out.println();
    }
    out.close();
  }
  
  public void printEntityTopics(double[][] thetap, String file)
      throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int h = 0; h < numEntities; h++) {
      for (int k = 0; k < numTopics; k++) {
        out.printf("%.10f,", thetap[h][k]);
      }
      out.println();
    }
    out.close();
  }

  /**
   * Prints top words assigned to a topic to the file.
   * 
   * @param thetad
   * @param file
   * @throws IOException
   */
  public void printTopWords(double[][] thetad, String file) throws IOException {
    PrintWriter writer = new PrintWriter(file);
    for (int topic = 0; topic < numTopics; ++topic) {
      ObjectToCounterMap<Integer> counter = new ObjectToCounterMap<Integer>();
      for (int word = 0; word < vocabularySize; ++word)
        counter.set(word, cwt[word][topic]);
      List<Integer> topWords = counter.keysOrderedByCountList();
      writer.println("\nTOPIC " + topic + "  (total count=" + cwtsum[topic]
          + ")");
      for (int rank = 0; rank < maxWordsPerTopic && rank < topWords.size(); ++rank) {
        int wordId = topWords.get(rank);
        String word = symbolTable.idToSymbol(wordId);
//        int wordCount = sample.wordCount(wordId);
//        int topicWordCount = sample.topicWordCount(topic, wordId);
//        double topicWordProb = sample.topicWordProb(topic, wordId);
//        double z = binomialZ(topicWordCount, topicCount, wordCount, numTokens);
//        writer.printf("%6d  %15s  %7d   %4.3f  %8.1f\n", wordId, word,
//            topicWordCount, topicWordProb, z);
        writer.println(word);
      }
    }
    writer.close();
  }

  /**
   * Prints top topics assigned to each document to the file.
   * 
   * @param file
   * @throws IOException
   */
  public void printTopDocTopics(String file) throws IOException {
    PrintWriter writer = new PrintWriter(file);
    for (int doc = 0; doc < numDocuments; ++doc) {
      ObjectToCounterMap<Integer> counter = new ObjectToCounterMap<Integer>();
      for (int topic = 0; topic < numTopics; ++topic)
        counter.set(topic, cdt[doc][topic]);
      List<Integer> topTopics = counter.keysOrderedByCountList();
      writer.println("\nDOC " + doc);
      writer.println("TOPIC    COUNT    PROB");
      writer.println("----------------------");
      for (int rank = 0; rank < topTopics.size() && rank < maxTopicsPerDoc; ++rank) {
        int topic = topTopics.get(rank);
        int docTopicCount = cdt[doc][topic];
        double docTopicProb = (cdt[doc][topic] + alpha) / (cdtsum[doc] + numTopics * alpha);
        writer.printf("%5d  %7d   %4.3f\n", topic, docTopicCount,
            docTopicProb);
      }
      writer.println();
    }
    writer.close();
  }

  /**
   * Prints top topics assigned to each entity.
   * 
   * @param file
   * @throws IOException
   */
  public void printTopEntityTopics(String file) throws IOException {
    PrintWriter writer = new PrintWriter(file);
    for (int ent = 0; ent < numEntities; ent++) {
      ObjectToCounterMap<Integer> counter = new ObjectToCounterMap<Integer>();
      for (int topic = 0; topic < numTopics; topic++) {
        counter.set(topic, cpt[ent][topic]);
      }
      List<Integer> topTopics = counter.keysOrderedByCountList();
      writer.println("\nENTITY " + ent);
      writer.println("TOPIC    COUNT    PROB");
      writer.println("----------------------");
      for (int rank = 0; rank < topTopics.size() && rank < maxTopicsPerEntity; rank++) {
        int topic = topTopics.get(rank);
        double entTopicProb = (cpt[ent][topic] + gamma) / (cptsum[ent] + numTopics * gamma);
        writer.printf("%5d  %7d   %4.3f\n", topic, cpt[ent][topic], entTopicProb);
      }
      writer.println();
    }
    writer.close();
  }
  
  static double binomialZ(double wordCountInDoc, double wordsInDoc,
      double wordCountinCorpus, double wordsInCorpus) {
    double pCorpus = wordCountinCorpus / wordsInCorpus;
    double var = wordsInCorpus * pCorpus * (1 - pCorpus);
    double dev = Math.sqrt(var);
    double expected = wordsInDoc * pCorpus;
    double z = (wordCountInDoc - expected) / dev;
    return z;
  }
  
  /**
   * Reports samples collected so far.
   * 
   * @param iter
   * @throws IOException
   */
  private void report(int iter) throws IOException {
    (new File(outputDir + "/" + iter)).mkdir();
    printDocumentTopics(thetad, outputDir + "/" + iter + "/documentTopics.csv");
    printEntityTopics(thetap, outputDir + "/" + iter + "/entityTopics.csv");
    printTopicTerms(phi, outputDir + "/" + iter + "/topicsTerms.csv");
    printTopWords(thetad, outputDir + "/" + iter + "/topTopicWords.txt");
    printTopDocTopics(outputDir + "/" + iter + "/topDocTopics.txt");
    printTopEntityTopics(outputDir + "/" + iter + "/topEntityTopics.txt");
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
    cpt = new int[numEntities][numTopics];
    cptsum = new int[numEntities];

    // sample values of z[i], ro[i], s[i] randomly ([1..numTopics] as the
    // initial state
    // of the Markov chain
    z = new int[numDocuments][];
    ro = new int[numDocuments][];
    s = new int[numDocuments][];
    for (int m = 0; m < numDocuments; m++) {
      int N = documents[m].length;
      z[m] = new int[N];
      ro[m] = new int[N];
      s[m] = new int[N];
      int randZ, randRo, randS; // the sample topic
      Entity entity;
      for (int n = 0; n < N; n++) {
        randZ = (int) (Math.random() * numTopics);
        entity = documentEntities[m][(int) (Math.random() * documentEntities[m].length)];
        randRo = corpusEntitySet.toId(entity);  
        randS = (int) (Math.random() * 2); // one of DOCUMENT or ENTITY
        z[m][n] = randZ;
        s[m][n] = randS;
        // in fact, if (s[i] = DOCUMENT) then this is not important
        ro[m][n] = randRo;
        cwt[documents[m][n]][randZ]++; // word i assigned to topic randZ
        cwtsum[randZ]++; // total number of words assigned to topic randZ
        if (s[m][n] == DOCUMENT) {
          // a word in document m assigned to topic k of document m
          cdt[m][randZ]++;
          cdtsum[m]++;
        } else {
          cpt[randRo][randZ]++;
          cptsum[randRo]++;
        }
      }
    }

    thetad = new double[numDocuments][numTopics];
    thetap = new double[numEntities][numTopics];
    phi = new double[numTopics][vocabularySize];
  }

  /**
   * Updates the parameters for the newly collected sample.
   */
  private void updateParams() {
    // thetadsum[][] (D x K) -- sum of samples (to return the average sample)
    double tAlpha = numTopics * alpha;
    for (int m = 0; m < numDocuments; m++) {
      for (int k = 0; k < numTopics; k++) {
        thetad[m][k] = (cdt[m][k] + alpha) / (cdtsum[m] + tAlpha);
      }
    }

    // thetapsum[][] (H x K) -- sum of sample parameters (to return the average
    // sample)
    double tGamma = numTopics * gamma;
    for (int h = 0; h < numEntities; h++) {
      for (int k = 0; k < numTopics; k++) {
        thetap[h][k] = (cpt[h][k] + gamma) / (cptsum[h] + tGamma);
      }
    }

    // phisum[][] (K X V) -- sum of samples (to return the average sample)
    double vBeta = vocabularySize * beta;
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < vocabularySize; i++) {
        phi[k][i] = (cwt[i][k] + beta) / (cwtsum[k] + vBeta);
      }
    }
  }

  /**
   * Samples a set of hidden variables (z, ro, s) for the n_th word in document
   * m.
   * 
   * @param m
   *          the document
   * @param n
   *          the index (position) of the word in this document
   * 
   * @return the sampling set
   */
  private SamplingSet sampleFullConditional(int m, int n) {
    int i = documents[m][n];
    int topic = z[m][n];
    int entity = ro[m][n];
    // not counting the i_th word
    cwt[i][topic]--;
    cwtsum[topic]--;
    if (s[m][n] == DOCUMENT) { // the i_th word was assigned a topic of document
                               // m
      cdt[m][topic]--;
      cdtsum[m]--;
    } else {
      cpt[entity][topic]--;
      cptsum[entity]--;
    }

    double vBeta = vocabularySize * beta;
    double tAlpha = numTopics * alpha;
    double tGamma = numTopics * gamma;
    double p, wordP;
    Entity ent = null;
    ArrayList<SamplingSet> list = new ArrayList<SamplingSet>();
    for (int k = 0; k < numTopics; k++) {
      for (int e = 0; e < documentEntities[m].length; e++) {
        wordP = (cwt[i][k] + beta) / (cwtsum[k] + vBeta);
        ent = documentEntities[m][e];
        /**
         * We use "uniform dist", i.e., equal probability for each entity that appears
         * in a document. So, if an entity appears ent.getCount() times, its probability
         * is multiplied by that amount. But in theory, this is still uniform for each
         * entity that appears in the document.
         */
        // s[i] = DOCUMENT
        p = wordP * ((cdt[m][k] + alpha) / (cdtsum[m] + tAlpha)) * ent.getCount();
        list.add(new SamplingSet(k, corpusEntitySet.toId(ent), DOCUMENT, p));
        // s[i] = ENTITY
        p = wordP * ((cpt[e][k] + gamma) / (cptsum[e] + tGamma)) * ent.getCount();
        list.add(new SamplingSet(k, corpusEntitySet.toId(ent), ENTITY, p));
      }
    }
    SamplingSet sample = sample(list);

    // assign new sample set to the i_th word
    topic = sample.z;
    z[m][n] = topic;
    cwt[i][topic]++;
    cwtsum[topic]++;
    if (sample.s == DOCUMENT) {
      cdt[m][topic]++;
      cdtsum[m]++;
    } else {
      cpt[sample.ro][topic]++;
      cptsum[sample.ro]++;
    }

    return sample;
  }

  /**
   * Samples a {@link SamplingSet} from the given discrete distribution.
   * 
   * @param list
   *          a list of {@link SamplingSet} elements.
   * @return
   */
  SamplingSet sample(ArrayList<SamplingSet> list) {
    int size = list.size();
    double p[] = new double[size];

    // turning the probability distribution from list into a cumulative
    // distribution
    p[0] = list.get(0).p;
    for (int i = 1; i < size; i++) {
      p[i] = p[i - 1] + list.get(i).p;
    }

    // scaled sample because of unnormalized p
    double u = Math.random() * p[size - 1];
    int selection;
    // find the interval which contains u
    for (selection = 0; selection < size; selection++) {
      if (u < p[selection]) {
        break;
      }
    }

    return list.get(selection);
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
