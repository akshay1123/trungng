package edu.kaist.uilab.event;

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

/**
 * Implementation of the event model using Gibbs sampler.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class EventGibbsSampler {

  private int numEvents; // V = numEvents
  private int vocabularySize; // W = vocabularySize
  private int numDocuments; // D = number of documents
  private int numEntities; // E = number of entities
  // term[m][n] = (index of the n_th word in document m) = i
  private int[][] term;
  // entity[m] = all entities of the m_th document
  private Entity[][] entity;
  private SymbolTable symbolTable;
  private ArrayList<Entity> entityTable;
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
  private int maxWordsPerEvent;
  private int maxEventsPerDoc;
  private int maxEntitiesPerEvent;

  /**
   * Stores the current model parameters for sampling.
   */
  static class Model implements Serializable {
    private static final long serialVersionUID = 7107787670680110327L;

    // event assignment for each word    
    int[][] wordEvent;
    // word and event related counts
    // W x V: word-event count
    int wordEventCount[][];
    // D x V: document-event count (words of document m assigned to event v)
    int docEventByWordCount[][];
    // V: #words assigned to event v
    int wordEventSum[];
    
    // event assignment for each entity
    int[][] entityEvent;
    // E x V : entity-event count
    int entityEventCount[][];
    // TODO(trung): unequal weight b.w words and entities?
    // D x V : document-event count (entities of document m assigned to event v)
    int docEventByEntityCount[][];
    // V: #entities assigned to event v
    int entityEventSum[];
    
    // TODO(trung): # words of document & # entities of document are normalizing constant

    // K X V : topic-event distribution
    double phi[][];
    // D x V : document-event distribution
    double theta[][];
    // V x E : event-entity distribution
    double psi[][];
  }

  /**
   * Default constructor -- for testing purpose only.
   */
  public EventGibbsSampler() {
  }

  /**
   * Constructs a new GibbsSampler with given model parameters.
   * 
   * @param numEvents
   * @param vocabularySize
   * @param numEntities
   * @param term
   * @param entity
   * @param corpusEntitySet
   * @param alpha
   * @param beta
   * @param gamma
   */
  public EventGibbsSampler(int numEvents, int vocabularySize,
      int numEntities, int[][] term, Entity[][] entity) {
    this.numEvents = numEvents;
    this.vocabularySize = vocabularySize;
    this.numEntities = numEntities;
    this.term = term;
    this.entity = entity;
    this.numDocuments = term.length;
  }

  /**
   * Sets the model priors.
   */
  public void setPriors(double alpha, double beta, double gamma) {
    this.alpha = alpha;
    this.beta = beta;
    this.gamma = gamma;
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
  public void setOutputParameters(SymbolTable symbolTable,
      ArrayList<Entity> entityTable, String outputDir,
      int wordsPerEvent, int eventsPerDoc, int entitiesPerEvent) {
    this.symbolTable = symbolTable;
    this.entityTable = entityTable;
    this.outputDir = outputDir;
    this.maxWordsPerEvent = wordsPerEvent;
    this.maxEventsPerDoc = eventsPerDoc;
    this.maxEntitiesPerEvent = entitiesPerEvent;
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

      for (int m = 0; m < numDocuments; m++) {
        // sample event for word
        for (int n = 0; n < term[m].length; n++) {
          model.wordEvent[m][n] = sampleEventForWord(m, n);
        }
        // sample event for entity
        for (int e = 0; e < entity[m].length; e++) {
          model.entityEvent[m][e] = sampleEventForEntity(m, e);
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
   * Loads the last iteration to continue previous training.
   * 
   * @return
   * @throws IOException
   */
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
   * Prints the matrix of event-term association, i.e., the term distribution
   * for each event.
   * 
   * @param file
   * @throws IOException
   */
  void printEventTerms(String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int v = 0; v < numEvents; v++) {
      for (int i = 0; i < vocabularySize; i++) {
        out.printf("%.10f,", model.phi[v][i]);
      }
      out.println();
    }
    out.close();
  }
  
  /**
   * Prints the entity distribution of each event to the file.
   * 
   * @param file
   * @throws IOException
   */
  void printEventEntities(String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int v = 0; v < numEvents; v++) {
      for (int e = 0; e < numEntities; e++) {
        out.printf("%.10f,", model.psi[v][e]);
      }
      out.println();
    }
    out.close();
  }

  /**
   * Prints the event distribution of each document.
   * 
   * @param file
   * @throws IOException
   */
  void printDocumentEvents(String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int m = 0; m < term.length; m++) {
      for (int v = 0; v < numEvents; v++) {
        out.printf("%.10f,", model.theta[m][v]);
      }
      out.println();
    }
    out.close();
  }

  /**
   * Prints top words assigned to an event to the file.
   * 
   * @param file
   * @throws IOException
   */
  void printTopEventWords(String file) throws IOException {
    PrintWriter writer = new PrintWriter(file);
    for (int event = 0; event < numEvents; ++event) {
      ObjectToCounterMap<Integer> counter = new ObjectToCounterMap<Integer>();
      for (int i = 0; i < vocabularySize; ++i) {
        counter.set(i, model.wordEventCount[i][event]);
      }  
      List<Integer> top = counter.keysOrderedByCountList();
      writer.printf("\nEVENT %d (count=%d)\n", event, model.wordEventSum[event]);
      for (int rank = 0; rank < maxWordsPerEvent && rank < top.size(); rank++) {
        int i = top.get(rank);
        writer.printf("%15s(%d)\n", symbolTable.idToSymbol(i),
            model.wordEventCount[i][event]);
      }
    }
    writer.close();
  }

  void printTopEventEntities(String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int v = 0; v < numEvents; v++) {
      ObjectToCounterMap<Integer> counter = new ObjectToCounterMap<Integer>();
      for (int e = 0; e < numEntities; e++) {
        counter.set(e, model.entityEventCount[e][v]);
      }
      List<Integer> top = counter.keysOrderedByCountList();
      out.printf("\nEVENT %d (count=%d)\n", v, model.entityEventSum[v]);
      for (int rank = 0; rank < maxEntitiesPerEvent && rank < top.size(); rank++) {
        int e = top.get(rank);
        out.printf("%15s(%d)\n", entityTable.get(e).getValue(),
            model.entityEventCount[e][v]);
      }
    }
    out.close();
  }
  
  /**
   * Prints top events assigned to each document to the file.
   * 
   * @param file
   * @throws IOException
   */
  void printTopDocEvents(String file) throws IOException {
    PrintWriter writer = new PrintWriter(file);
    for (int doc = 0; doc < numDocuments; ++doc) {
      ObjectToCounterMap<Integer> counter = new ObjectToCounterMap<Integer>();
      for (int event = 0; event < numEvents; ++event)
        counter.set(event, model.docEventByEntityCount[doc][event] +
            model.docEventByWordCount[doc][event]);
      List<Integer> topEvents = counter.keysOrderedByCountList();
      writer.println("\nDOC " + doc);
      writer.println("EVENT    COUNT    PROB");
      writer.println("----------------------");
      for (int rank = 0; rank < topEvents.size() && rank < maxEventsPerDoc; ++rank) {
        int event = topEvents.get(rank);
        writer.printf("%5d  %7d   %4.3f\n", event, model.docEventByEntityCount[doc][event] +
            model.docEventByWordCount[doc][event]);
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
    printDocumentEvents(outputDir + "/" + iter + "/documentEvents.csv");
    printEventTerms(outputDir + "/" + iter + "/eventTerms.csv");
    printEventEntities(outputDir + "/" + iter + "/eventEntities.csv");
    printTopEventWords(outputDir + "/" + iter + "/topEventWords.txt");
    printTopEventEntities(outputDir + "/" + iter + "/topEventEntities.txt");
    printTopDocEvents(outputDir + "/" + iter + "/topDocEvents.txt");
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
    model.wordEventCount = new int[vocabularySize][numEvents];
    model.wordEventSum = new int[numEvents];
    model.docEventByWordCount = new int[numDocuments][];
    model.entityEventCount = new int[numEntities][numEvents];
    model.entityEventSum = new int[numEvents];
    model.docEventByEntityCount = new int[numDocuments][];

    model.wordEvent = new int[numDocuments][];
    model.entityEvent = new int[numDocuments][];
    for (int m = 0; m < numDocuments; m++) {
      model.wordEvent[m] = new int[term[m].length];
      model.entityEvent[m] = new int[entity[m].length];
      for (int n = 0; n < term[m].length; n++) {
        int event = (int) (Math.random() * numEvents);
        model.wordEvent[m][n] = event;
        model.wordEventCount[term[m][n]][event]++;
        model.wordEventSum[event]++;
        model.docEventByWordCount[m][event]++;
      }
      for (int e = 0; e < entity[m].length; e++) {
        int event = (int) (Math.random() * numEvents);
        model.entityEvent[m][e] = event;
        model.entityEventCount[entity[m][e].getId()][event]++;
        model.entityEventSum[event]++;
        model.docEventByEntityCount[m][event]++;
      }
    }

    model.phi = new double[numEvents][vocabularySize];
    model.theta = new double[numDocuments][numEvents];
    model.psi = new double[numEvents][numEntities];
  }

  /**
   * Updates the parameters for the newly collected sample.
   */
  private void updateParams() {
    // theta[][] (D x K)
    double vAlpha = numEvents * alpha;
    for (int m = 0; m < numDocuments; m++) {
      for (int v = 0; v < numEvents; v++) {
        model.theta[m][v] = (model.docEventByWordCount[m][v] + model.docEventByEntityCount[m][v] + alpha)
            / (term[m].length + entity[m].length + vAlpha);
      }
    }

    // psi[][] (V x E)
    double eGamma = numEntities * gamma;
    for (int v = 0; v < numEvents; v++) {
      for (int e = 0; e < numEntities; e++) {
        model.psi[v][e] = (model.entityEventCount[e][v] + gamma)
            / (model.entityEventSum[v] + eGamma);
      }
    }

    // phi[][] (V x W)
    double wBeta = vocabularySize * beta;
    for (int v = 0; v < numEvents; v++) {
      for (int i = 0; i < vocabularySize; i++) {
        model.phi[v][i] = (model.wordEventCount[i][v] + beta)
            / (model.wordEventSum[v] + wBeta);
      }
    }
  }

  /**
   * Samples an event for a word.
   * 
   * @param m
   *          the document
   * @param n
   *          the index (position) of the word in this document
   * 
   * @return the sampling set
   */
  private int sampleEventForWord(int m, int n) {
    int i = term[m][n];
    int event = model.wordEvent[m][n];
    // the i_th word was assigned a topic of document m
    model.wordEventCount[i][event]--;
    model.wordEventSum[event]--;
    model.docEventByWordCount[m][event]--;

    double wBeta = vocabularySize * beta;
    double[] p = new double[numEvents];
    for (int v = 0; v < numEvents; v++) {
      // p(word | event) * p(event | doc)
      p[v] = ((model.wordEventCount[i][v] + beta) / (model.wordEventSum[v] + wBeta))
          * (model.docEventByWordCount[m][v] + alpha);
    }
    event = sample(p);

    // assign new sample set to the i_th word
    model.wordEventCount[i][event]++;
    model.wordEventSum[event]++;
    model.docEventByWordCount[m][event]++;

    return event;
  }

  /**
   * Samples an event for an entity.
   * 
   * @param m
   *          the document
   * @param e
   *          the index (position) of the entity in this document
   * 
   * @return the sampling set
   */
  private int sampleEventForEntity(int m, int e) {
    int ent = entity[m][e].getId();
    int event = model.entityEvent[m][ent];
    model.entityEventCount[ent][event]--;
    model.entityEventSum[event]--;
    model.docEventByEntityCount[m][event]--;

    double eGamma = numEntities * gamma;
    double[] p = new double[numEvents];
    for (int v = 0; v < numEvents; v++) {
      // p(entity | event) * p(event | doc)
      p[v] = ((model.entityEventCount[ent][v] + gamma) / (model.entityEventSum[v] + eGamma))
          * (model.docEventByEntityCount[m][v] + alpha);
    }
    event = sample(p);

    model.entityEventCount[ent][event]++;
    model.entityEventSum[event]++;
    model.docEventByEntityCount[m][event]++;

    return event;
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
