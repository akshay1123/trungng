package edu.kaist.uilab.plda;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

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
  int[] docEntityCount;
  private SymbolTable symbolTable;
  private CorpusEntitySet corpusEntitySet;
  private Model model;

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

  // output parameters
  private String outputDir;
  private int maxWordsPerTopic;
  private int maxTopicsPerDoc;
  private int maxTopicsPerEntity;

  /**
   * Stores the current model parameters for sampling.
   */
  static class Model implements Serializable {
    private static final long serialVersionUID = 7107787670680110327L;

    int[][] z; // topic assignment for each word z[m][n] (z[i])
    int[][] ro; // author assignment for each word i (ro[i])
    // switch for each word i :s[i]
    // s[i] = DOCUMENT: topic of assignment for this word is that of a document
    // s[i] = ENTITY: topic of assignment for this word is that of an entity
    int[][] s;
    // V X T: word-topic count
    // cwdt[i][k] = # times word i is assigned to some document and its topic k
    int cwdt[][];
    // cwdt[i][k] = # times word i is assigned to some entity and its topic k
    int cwet[][];
    // D x T: document-topic count
    // cdt[m][k] = # times that a word in document m is assigned topic k (OF
    // DOCUMENT m)
    int cdt[][];
    // H X T: author-topic count
    // cpt[h][k] = # times that a word "of" person/entity h is assigned topic k
    // (OF ENTITY h)
    int cpt[][];

    // cwtsum[k] = # words assigned to topic k
    int cwdtsum[];
    int cwetsum[];
    // cdtsum[m] = # words assigned to a topic of the document m
    int cdtsum[];
    // cptsum[h] = # words assigned to a topic of the person/entity h
    int cptsum[];

    // K X V : phi matrix of the current sample
    double phi[][];
    // D x K : thetad matrix of the current sample
    double thetad[][];
    // H x K : thetap matrix of the current sample
    double thetap[][];
  }

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
  public EntityLdaGibbsSampler(int numTopics, int vocabularySize,
      int numEntities, int[][] documents, Entity[][] documentEntities,
      CorpusEntitySet corpusEntitySet, double alpha, double beta, double gamma) {
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
    initDocEntityCount();
  }

  /**
   * Inits entity count for each document.
   */
  private void initDocEntityCount() {
    docEntityCount = new int[numDocuments];
    for (int doc = 0; doc < numDocuments; doc++) {
      for (Entity e : documentEntities[doc]) {
        docEntityCount[doc] += e.getCount();
      }
    }
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
  public void setOutputParameters(SymbolTable symbolTable, String outputDir,
      int wordsPerTopic, int topicsPerDoc, int topicsPerEntity) {
    this.symbolTable = symbolTable;
    this.outputDir = outputDir;
    this.maxWordsPerTopic = wordsPerTopic;
    this.maxTopicsPerDoc = topicsPerDoc;
    this.maxTopicsPerEntity = topicsPerEntity;
  }

  private int loadLastIter() throws IOException {
    File dir = new File(outputDir);
    SortedSet<Integer> set = new TreeSet<Integer>();
    for (File file : dir.listFiles()) {
      if (file.isDirectory()) {
        set.add(Integer.parseInt(file.getName()));
      }
    }

    ObjectInputStream is = new ObjectInputStream(new FileInputStream(outputDir
        + "/" + set.last() + "/model.gz"));
    try {
      model = (Model) is.readObject();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    is.close();

    return set.last();
  }

  /**
   * Runs the Gibbs sampler.
   * 
   * @param loadPastTraining
   *          true to continue an existing training
   */
  public void doGibbsSampling(boolean loadPastTraining) throws IOException {
    int iter = 0;
    if (!loadPastTraining) {
      System.out.print("Initializing parameters...");
      initialize();
      System.out.println("done");
    } else {
      iter = loadLastIter();
    }

    int samplesCollected = 0;
    System.out.println("Burning in period...");
    for (; iter < numIterations; iter++) {
      if (iter < burnIn) {
        System.out.print(iter + " ");
        if (iter % 100 == 99) {
          System.out.println();
        }
      } else if (iter == burnIn) {
        System.out.println("\nBurning in done");
      }

      // sampling hidden variables z_i
      for (int m = 0; m < numDocuments; m++) {
        for (int n = 0; n < documents[m].length; n++) {
          SamplingSet sample = sampleFullConditional(m, n);
          model.z[m][n] = sample.z;
          model.ro[m][n] = sample.ro;
          model.s[m][n] = sample.s;
        }
      }

      // after burn-in & some sample lags we can collect a sample
      if (iter >= burnIn && (iter - burnIn) % sampleLags == 0) {
        System.out.printf("\nCollected a sample at iteration %d", iter);
        updateParams();
        samplesCollected++;
        report(iter);
        if (samplesCollected == numSamples) {
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
    return model.thetad;
  }

  /**
   * Returns the estimated theta_p values of the last sample collected.
   * 
   * @return
   */
  public double[][] getThetap() {
    return model.thetap;
  }

  /**
   * Returns the estimated phi values of the last sample collected.
   * 
   * @return
   */
  public double[][] getPhi() {
    return model.phi;
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
  public void printTopWords(String file) throws IOException {
    PrintWriter writer = new PrintWriter(file);
    for (int topic = 0; topic < numTopics; ++topic) {
      ObjectToCounterMap<Integer> counter = new ObjectToCounterMap<Integer>();
      for (int word = 0; word < vocabularySize; ++word)
        counter.set(word, model.cwdt[word][topic] + model.cwet[word][topic]);
      List<Integer> topWords = counter.keysOrderedByCountList();
      writer.printf("\nTOPIC %d (count: doc=%d  ent=%d   total=%d)", topic,
          model.cwdtsum[topic], model.cwetsum[topic], model.cwdtsum[topic] + model.cwetsum[topic]);
      for (int rank = 0; rank < maxWordsPerTopic && rank < topWords.size(); ++rank) {
        int wordId = topWords.get(rank);
        String word = symbolTable.idToSymbol(wordId);
        writer.printf("%6d  %15s  %7d   %4.3f\n", wordId, word,
            model.cwdt[wordId][topic] + model.cwet[wordId][topic], model.phi[topic][wordId]);
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
        counter.set(topic, model.cdt[doc][topic]);
      List<Integer> topTopics = counter.keysOrderedByCountList();
      writer.println("\nDOC " + doc);
      writer.println("TOPIC    COUNT    PROB");
      writer.println("----------------------");
      for (int rank = 0; rank < topTopics.size() && rank < maxTopicsPerDoc; ++rank) {
        int topic = topTopics.get(rank);
        writer.printf("%5d  %7d   %4.3f\n", topic, model.cdt[doc][topic],
            model.thetad[doc][topic]);
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
        counter.set(topic, model.cpt[ent][topic]);
      }
      List<Integer> topTopics = counter.keysOrderedByCountList();
      if (topTopics.size() > 0) {
        writer.println("\nENTITY " + ent);
        writer.println("TOPIC    COUNT    PROB");
        writer.println("----------------------");
        for (int rank = 0; rank < topTopics.size() && rank < maxTopicsPerEntity; rank++) {
          int topic = topTopics.get(rank);
          writer.printf("%5d  %7d   %4.3f\n", topic, model.cpt[ent][topic],
              model.thetap[ent][topic]);
        }
        writer.println();
      }
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
    printDocumentTopics(model.thetad, outputDir + "/" + iter
        + "/documentTopics.csv");
    printEntityTopics(model.thetap, outputDir + "/" + iter
        + "/entityTopics.csv");
    printTopicTerms(model.phi, outputDir + "/" + iter + "/topicsTerms.csv");
    printTopWords(outputDir + "/" + iter + "/topTopicWords.txt");
    printTopDocTopics(outputDir + "/" + iter + "/topDocTopics.txt");
    printTopEntityTopics(outputDir + "/" + iter + "/topEntityTopics.txt");
    // TODO(trung): refactor code
    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(
        outputDir + "/" + iter + "/model.gz"));
    out.writeObject(model);
    out.close();
  }

  /**
   * Initializes the model (assign random values to hidden variables -- the
   * topics z).
   */
  private void initialize() {
    // initialize count variables
    model = new Model();
    model.cwdt = new int[vocabularySize][numTopics];
    model.cwet = new int[vocabularySize][numTopics];
    model.cwdtsum = new int[numTopics];
    model.cwetsum = new int[numTopics];
    model.cdt = new int[numDocuments][numTopics];
    model.cdtsum = new int[numDocuments];
    model.cpt = new int[numEntities][numTopics];
    model.cptsum = new int[numEntities];

    // sample values of z[i], ro[i], s[i] randomly ([1..numTopics] as the
    // initial state of the Markov chain
    model.z = new int[numDocuments][];
    model.ro = new int[numDocuments][];
    model.s = new int[numDocuments][];
    for (int m = 0; m < numDocuments; m++) {
      int N = documents[m].length;
      model.z[m] = new int[N];
      model.ro[m] = new int[N];
      model.s[m] = new int[N];
      int randZ, randRo; // the sample topic
      for (int n = 0; n < N; n++) {
        randZ = (int) (Math.random() * numTopics);
        model.z[m][n] = randZ;
        if (documentEntities[m].length == 0) {
          model.s[m][n] = DOCUMENT;
        } else {
          // if document has entities, randS is one of DOCUMENT or ENTITY
          model.s[m][n] = (int) (Math.random() * 2);
        }
        if (model.s[m][n] == DOCUMENT) {
          // word i assigned to topic randZ
          model.cwdt[documents[m][n]][randZ]++;
          // total number of words assigned to topic randZ
          model.cwdtsum[randZ]++;
          // a word in document m assigned to topic k of document m
          model.cdt[m][randZ]++;
          model.cdtsum[m]++;
        } else {
          model.cwet[documents[m][n]][randZ]++;
          model.cwetsum[randZ]++;
          // the word is assigned to topic k of an entity randRo
          randRo = getRandEntity(m);
          model.ro[m][n] = randRo;
          model.cpt[randRo][randZ]++;
          model.cptsum[randRo]++;
        }
      }
    }

    model.thetad = new double[numDocuments][numTopics];
    model.thetap = new double[numEntities][numTopics];
    model.phi = new double[numTopics][vocabularySize];
  }

  /**
   * Returns a random entity id of a document m.
   */
  private int getRandEntity(int m) {
    ArrayList<Integer> list = new ArrayList<Integer>();
    int entityId;
    for (Entity e : documentEntities[m]) {
      entityId = corpusEntitySet.toId(e);
      for (int i = 0; i < e.getCount(); i++) {
        list.add(entityId);
      }
    }

    return list.get((int) (Math.random() * list.size()));
  }

  /**
   * Updates the parameters for the newly collected sample.
   */
  private void updateParams() {
    // thetad[][] (D x K)
    double tAlpha = numTopics * alpha;
    for (int m = 0; m < numDocuments; m++) {
      for (int k = 0; k < numTopics; k++) {
        model.thetad[m][k] = (model.cdt[m][k] + alpha) / (model.cdtsum[m] + tAlpha);
      }
    }

    // thetap[][] (H x K)
    double tGamma = numTopics * gamma;
    for (int h = 0; h < numEntities; h++) {
      for (int k = 0; k < numTopics; k++) {
        model.thetap[h][k] = (model.cpt[h][k] + gamma) / (model.cptsum[h] + tGamma);
      }
    }

    // phi[][]
    double vBeta = vocabularySize * beta;
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < vocabularySize; i++) {
        model.phi[k][i] = (model.cwdt[i][k] + model.cwet[i][k] + beta)
            / (model.cwdtsum[k] + model.cwetsum[k] + vBeta);
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
    int topic = model.z[m][n];
    int entity = model.ro[m][n];
    // the i_th word was assigned a topic of document m
    if (model.s[m][n] == DOCUMENT) {
      // not counting the i_th word
      model.cwdt[i][topic]--;
      model.cwdtsum[topic]--;
      model.cdt[m][topic]--;
      model.cdtsum[m]--;
    } else {
      model.cwet[i][topic]--;
      model.cwetsum[topic]--;
      model.cpt[entity][topic]--;
      model.cptsum[entity]--;
    }

    double vBeta = vocabularySize * beta;
    double tAlpha = numTopics * alpha;
    double tGamma = numTopics * gamma;
    Entity ent = null;
    ArrayList<SamplingSet> list = new ArrayList<SamplingSet>();
    // if document m has no entities, perform lda
    if (documentEntities[m].length == 0) {
      for (int k = 0; k < numTopics; k++) {
        list.add(new SamplingSet(k, -1, DOCUMENT, (model.cwdt[i][k] + beta)
            / (model.cwdtsum[k] + vBeta) * (model.cdt[m][k] + alpha)
            / (model.cdtsum[m] + tAlpha)));
      }
    } else {
      for (int k = 0; k < numTopics; k++) {
        // s[i] = DOCUMENT
        list.add(new SamplingSet(k, -1, DOCUMENT, (model.cwdt[i][k] + beta)
            / (model.cwdtsum[k] + vBeta) * (model.cdt[m][k] + alpha)
            / (model.cdtsum[m] + tAlpha)));
        for (int e = 0; e < documentEntities[m].length; e++) {
          ent = documentEntities[m][e];
          /**
           * We use "uniform dist", i.e., equal probability for each entity that
           * appears in a document. So, if an entity appears ent.getCount()
           * times, its probability is multiplied by that amount. But in theory,
           * this is still uniform for each entity that appears in the document.
           */
          // s[i] = ENTITY
          list.add(new SamplingSet(k, corpusEntitySet.toId(ent), ENTITY,
              (model.cwet[i][k] + beta) / (model.cwetsum[k] + vBeta)
                  * ((model.cpt[e][k] + gamma) / (model.cptsum[e] + tGamma))
                  / docEntityCount[m] * ent.getCount()));
        }
      }
    }
    SamplingSet sample = sample(list);

    // assign new sample set to the i_th word
    topic = sample.z;
    if (sample.s == DOCUMENT) {
      model.cwdt[i][topic]++;
      model.cwdtsum[topic]++;
      model.cdt[m][topic]++;
      model.cdtsum[m]++;
    } else {
      model.cwet[i][topic]++;
      model.cwetsum[topic]++;
      model.cpt[sample.ro][topic]++;
      model.cptsum[sample.ro]++;
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
