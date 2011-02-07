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
import edu.kaist.uilab.plda.file.TextFiles;

/**
 * Implementation of the entity lda Gibbs sampler.
 * 
 * <p> This sampler uses the beta distribution for the switch (choosing
 * between document and entity topic). It also uses different topic-word
 * distribution for documents and entities.
 * 
 * TODO(trung): test without a switch (does not make much sense)
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class EntityLdaGibbsSampler3 {
  private static final int DOCUMENT = 0;
  private static final int ENTITY = 1;

  private int numDocumentTopics; // number of document topics
  private int numEntityTopics; // number of entity topics
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
  private double alpha_d;
  private double alpha_e;
  private double beta_d;
  private double beta_e;
  private double eta_d, eta_e;

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
    int[][] rho; // author assignment for each word i (rho[i])
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
    // H X T: entity-topic count
    // cet[h][k] = # times that a word "of" entity h is assigned topic k
    // (OF ENTITY h)
    int cet[][];

    // cd[m] = # times the switch of a word in document m equals DOCUMENT
    // ce[m] = documents[m].length - cd[m]
    int cd[];
    int ce[];
    // cwtsum[k] = # words assigned to topic k
    int cwdtsum[];
    int cwetsum[];
    // cdtsum[m] = # words assigned to a topic of the document m
    int cdtsum[];
    // cetsum[h] = # words assigned to a topic of the entity h
    int cetsum[];

    // K X V : phi matrix of the current sample
    double phi_d[][];
    double phi_e[][];
    // D x K : thetad matrix of the current sample
    double thetad[][];
    // H x K : thetap matrix of the current sample
    double thetae[][];
  }

  /**
   * Default constructor -- for testing purpose only.
   */
  public EntityLdaGibbsSampler3() {
  }

  /**
   * Constructs a new GibbsSampler with given model parameters.
   * 
   * @param numDocumentTopics
   * @param numEntityTopics
   * @param vocabularySize
   * @param numEntities
   * @param documents
   * @param documentEntities
   * @param corpusEntitySet
   */
  public EntityLdaGibbsSampler3(int numDocumentTopics, int numEntityTopics,
      int vocabularySize, int numEntities, int[][] documents,
      Entity[][] documentEntities, CorpusEntitySet corpusEntitySet) {
    this.numDocumentTopics = numDocumentTopics;
    this.numEntityTopics = numEntityTopics;
    this.vocabularySize = vocabularySize;
    this.numEntities = numEntities;
    this.documents = documents;
    this.documentEntities = documentEntities;
    this.corpusEntitySet = corpusEntitySet;
    this.numDocuments = documents.length;
    initDocEntityCount();
  }

  /**
   * Sets the priors for this sampler.
   * 
   * @param alpha_d
   * @param alpha_e
   * @param beta_d
   * @param beta_e
   * @param eta_d
   * @param eta_e
   */
  public void setPriors(double alpha_d, double alpha_e, double beta_d,
      double beta_e, double eta_d, double eta_e) {
    this.alpha_d = alpha_d;
    this.alpha_e = alpha_e;
    this.beta_d = beta_d;
    this.beta_e = beta_e;
    this.eta_d = eta_d;
    this.eta_e = eta_e;
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
          model.rho[m][n] = sample.rho;
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

  public void printDocumentTopicTerms(double[][] phi, String file)
      throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int k = 0; k < numDocumentTopics; k++) {
      for (int i = 0; i < vocabularySize; i++) {
        out.printf("%.10f,", phi[k][i]);
      }
      out.println();
    }
    out.close();
  }

  public void printEntityTopicTerms(double[][] phi, String file)
      throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int k = 0; k < numEntityTopics; k++) {
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
      for (int k = 0; k < numDocumentTopics; k++) {
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
      for (int k = 0; k < numEntityTopics; k++) {
        out.printf("%.10f,", thetap[h][k]);
      }
      out.println();
    }
    out.close();
  }

  /**
   * Prints top words assigned to a document topic to the file.
   * 
   * @param thetad
   * @param file
   * @throws IOException
   */
  public void printDocumentTopicWords(String file) throws IOException {
    PrintWriter writer = new PrintWriter(file);
    for (int topic = 0; topic < numDocumentTopics; ++topic) {
      ObjectToCounterMap<Integer> docCounter = new ObjectToCounterMap<Integer>();
      for (int word = 0; word < vocabularySize; ++word) {
        docCounter.set(word, model.cwdt[word][topic]);
      }  
      List<Integer> top = docCounter.keysOrderedByCountList();
      writer.printf("\nTOPIC %d (total count=%d)\n", topic, model.cwdtsum[topic]);
      int id;
      for (int rank = 0; rank < maxWordsPerTopic && rank < top.size(); rank++) {
        id = top.get(rank);
        writer.printf("%15s(%d)\n", symbolTable.idToSymbol(id),
            model.cwdt[id][topic]);
      }
    }
    writer.close();
  }

  /**
   * Prints top words assigned to an entity topic to the file.
   * 
   * @param thetad
   * @param file
   * @throws IOException
   */
  public void printEntityTopicWords(String file) throws IOException {
    PrintWriter writer = new PrintWriter(file);
    for (int topic = 0; topic < numEntityTopics; ++topic) {
      ObjectToCounterMap<Integer> counter = new ObjectToCounterMap<Integer>();
      for (int word = 0; word < vocabularySize; ++word) {
        counter.set(word, model.cwet[word][topic]);
      }  
      List<Integer> top = counter.keysOrderedByCountList();
      writer.printf("\nTOPIC %d (total count=%d)\n", topic, model.cwetsum[topic]);
      int id;
      for (int rank = 0; rank < maxWordsPerTopic && rank < top.size(); rank++) {
        id = top.get(rank);
        writer.printf("%15s(%d)\n", symbolTable.idToSymbol(id),
            model.cwet[id][topic]);
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
      for (int topic = 0; topic < numDocumentTopics; ++topic)
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
      for (int topic = 0; topic < numEntityTopics; topic++) {
        counter.set(topic, model.cet[ent][topic]);
      }
      List<Integer> topTopics = counter.keysOrderedByCountList();
      if (topTopics.size() > 0) {
        writer.println("\nENTITY " + ent);
        writer.println("TOPIC    COUNT    PROB");
        writer.println("----------------------");
        for (int rank = 0; rank < topTopics.size() && rank < maxTopicsPerEntity; rank++) {
          int topic = topTopics.get(rank);
          writer.printf("%5d  %7d   %4.3f\n", topic, model.cet[ent][topic],
              model.thetae[ent][topic]);
        }
        writer.println();
      }
    }
    writer.close();
  }

//  static double binomialZ(double wordCountInDoc, double wordsInDoc,
//      double wordCountinCorpus, double wordsInCorpus) {
//    double pCorpus = wordCountinCorpus / wordsInCorpus;
//    double var = wordsInCorpus * pCorpus * (1 - pCorpus);
//    double dev = Math.sqrt(var);
//    double expected = wordsInDoc * pCorpus;
//    double z = (wordCountInDoc - expected) / dev;
//    return z;
//  }

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
    printEntityTopics(model.thetae, outputDir + "/" + iter
        + "/entityTopics.csv");
    printDocumentTopicTerms(model.phi_d, outputDir + "/" + iter + "/documentTopicsTerms.csv");
    printDocumentTopicWords(outputDir + "/" + iter + "/topDocumentTopicWords.txt");
    printEntityTopicWords(outputDir + "/" + iter + "/topEntityTopicWords.txt");
    printTopDocTopics(outputDir + "/" + iter + "/topDocTopics.txt");
    printTopEntityTopics(outputDir + "/" + iter + "/topEntityTopics.txt");
    TextFiles.writeFile(outputDir + "/" + iter + "/loglikelihood.txt",
        String.valueOf(corpusLog2Likelihood()));
    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(
        outputDir + "/" + iter + "/model.gz"));
    out.writeObject(model);
    out.close();
  }

  /**
   * Returns the log likelihood of the trained model (using point estimate).
   * 
   * @return
   */
  public double corpusLog2Likelihood() {
    double log = 0.0;
    for (int m = 0; m < numDocuments; m++) {
      for (int n = 0; n < documents[m].length; n++) {
        // document topic
        double wordProb1 = 0.0;
        for (int z = 0; z < numDocumentTopics; z++) {
          wordProb1 += model.phi_d[z][documents[m][n]] * model.thetad[m][z];
        }
        // entity topic (s_i = ent)
        double wordProb2 = 0.0;
        if (docEntityCount[m] > 0) {
          for (int z = 0; z < numEntityTopics; z++) {
            double entProb = 0.0;
            for (int h = 0; h < documentEntities[m].length; h++) {
              entProb += model.thetae[h][z];
            }
            wordProb2 += entProb * model.phi_e[z][documents[m][n]];
          }
          wordProb2 /= docEntityCount[m]; // entity prob
        }  
        double piDoc = (model.cd[m] + eta_d) / (documents[m].length + eta_d + eta_e);
        log += com.aliasi.util.Math.log2(piDoc * wordProb1 + (1 - piDoc) * wordProb2);
      }
    }
    System.out.println("\n" + log);
    
    return log;
  }
  
  /**
   * Initializes the model (assign random values to hidden variables -- the
   * topics z).
   */
  private void initialize() {
    // initialize count variables
    model = new Model();
    model.cwdt = new int[vocabularySize][numDocumentTopics];
    model.cwet = new int[vocabularySize][numEntityTopics];
    model.cwdtsum = new int[numDocumentTopics];
    model.cwetsum = new int[numEntityTopics];
    model.cdt = new int[numDocuments][numDocumentTopics];
    model.cdtsum = new int[numDocuments];
    model.cet = new int[numEntities][numEntityTopics];
    model.cetsum = new int[numEntities];
    model.cd = new int[numDocuments];
    model.ce = new int[numDocuments];

    // sample values of z[i], rho[i], s[i] randomly ([1..numTopics] as the
    // initial state of the Markov chain
    model.z = new int[numDocuments][];
    model.rho = new int[numDocuments][];
    model.s = new int[numDocuments][];
    for (int m = 0; m < numDocuments; m++) {
      int N = documents[m].length;
      model.z[m] = new int[N];
      model.rho[m] = new int[N];
      model.s[m] = new int[N];
      int randZ, randRho; // the sample topic
      for (int n = 0; n < N; n++) {
        // it seems that first sample can be initialized randomly
        if (documentEntities[m].length == 0) {
          model.s[m][n] = DOCUMENT;
        } else {
          // if document has entities, randS is one of DOCUMENT or ENTITY
          model.s[m][n] = (int) (Math.random() * 2);
        }
        if (model.s[m][n] == DOCUMENT) {
          randZ = (int) (Math.random() * numDocumentTopics);
          model.z[m][n] = randZ;
          // word i assigned to topic randZ
          model.cwdt[documents[m][n]][randZ]++;
          // total number of words assigned to topic randZ
          model.cwdtsum[randZ]++;
          // a word in document m assigned to topic k of document m
          model.cdt[m][randZ]++;
          model.cdtsum[m]++;
          model.cd[m]++;
        } else {
          randZ = (int) (Math.random() * numEntityTopics);
          model.z[m][n] = randZ;
          model.cwet[documents[m][n]][randZ]++;
          model.cwetsum[randZ]++;
          // the word is assigned to topic k of an entity randRo
          randRho = getRandEntity(m);
          model.rho[m][n] = randRho;
          model.cet[randRho][randZ]++;
          model.cetsum[randRho]++;
          model.ce[m]++;
        }
      }
    }
    
    model.thetad = new double[numDocuments][numDocumentTopics];
    model.thetae = new double[numEntities][numEntityTopics];
    model.phi_d = new double[numDocumentTopics][vocabularySize];
    model.phi_e = new double[numEntityTopics][vocabularySize];
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
    // thetad[][] (D x K_d)
    double tAlpha = numDocumentTopics * alpha_d;
    for (int m = 0; m < numDocuments; m++) {
      for (int k = 0; k < numDocumentTopics; k++) {
        model.thetad[m][k] = (model.cdt[m][k] + alpha_d) / (model.cdtsum[m] + tAlpha);
      }
    }

    // thetae[][] (H x K_e)
    double tGamma = numEntityTopics * alpha_e;
    for (int h = 0; h < numEntities; h++) {
      for (int k = 0; k < numEntityTopics; k++) {
        model.thetae[h][k] = (model.cet[h][k] + alpha_e) / (model.cetsum[h] + tGamma);
      }
    }

    // phi_d[][] (K_d X V)
    double vBeta = vocabularySize * beta_d;
    for (int k = 0; k < numDocumentTopics; k++) {
      for (int i = 0; i < vocabularySize; i++) {
        model.phi_d[k][i] = (model.cwdt[i][k] + beta_d) / (model.cwdtsum[k] + vBeta);
      }
    }

    // phi_e[][] (K_e X V)
    vBeta = vocabularySize * beta_e;
    for (int k = 0; k < numEntityTopics; k++) {
      for (int i = 0; i < vocabularySize; i++) {
        model.phi_e[k][i] = (model.cwet[i][k] + beta_e) / (model.cwetsum[k] + vBeta);
      }
    }
  }

  /**
   * Samples a set of hidden variables (z, rho, s) for the n_th word in document
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
    // TODO(TRUNG): verify this
    int i = documents[m][n];
    int topic = model.z[m][n];
    int entity = model.rho[m][n];
    // the i_th word was assigned a topic of document m
    if (model.s[m][n] == DOCUMENT) {
      // not counting the i_th word
      model.cwdt[i][topic]--;
      model.cwdtsum[topic]--;
      model.cdt[m][topic]--;
      model.cdtsum[m]--;
      model.cd[m]--;
    } else {
      model.cwet[i][topic]--;
      model.cwetsum[topic]--;
      model.cet[entity][topic]--;
      model.cetsum[entity]--;
      model.ce[m]--;
    }

    double vBetad = vocabularySize * beta_d;
    double vBetae = vocabularySize * beta_e;
    double tAlphad = numDocumentTopics * alpha_d;
    double tAlphae = numEntityTopics * alpha_e;
    ArrayList<SamplingSet> list = new ArrayList<SamplingSet>();
    double p;
    // add all p(z_i, s_i = doc)
    for (int k = 0; k < numDocumentTopics; k++) {
      p = ((model.cwdt[i][k] + beta_d) / (model.cwdtsum[k] + vBetad))
                * ((model.cdt[m][k] + alpha_d) / (model.cdtsum[m] + tAlphad))
                * (model.cd[m] + eta_d);
      list.add(new SamplingSet(k, -1, DOCUMENT, p));
    }
    // add all p(z_i, e_i, s_i = ent)
    Entity ent = null;
    for (int k = 0; k < numEntityTopics; k++) {
      for (int e = 0; e < documentEntities[m].length; e++) {
        ent = documentEntities[m][e];
        /**
         * We use "uniform dist", i.e., equal probability for each entity that
         * appears in a document. So, if an entity appears ent.getCount()
         * times, its probability is multiplied by that amount. But in theory,
         * this is still uniform for each entity that appears in the document.
         */
        p = (model.cwet[i][k] + beta_e) / (model.cwetsum[k] + vBetae)
            * ((model.cet[e][k] + alpha_e) / (model.cetsum[e] + tAlphae))
            * (model.ce[m] + eta_e)
            / docEntityCount[m]
            * ent.getCount();
        list.add(new SamplingSet(k, corpusEntitySet.toId(ent), ENTITY, p));
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
      model.cd[m]++;
    } else {
      model.cwet[i][topic]++;
      model.cwetsum[topic]++;
      model.cet[sample.rho][topic]++;
      model.cetsum[sample.rho]++;
      model.ce[m]++;
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
