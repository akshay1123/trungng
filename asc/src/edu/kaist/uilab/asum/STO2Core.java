/*
 * Implementation of Sentence Topic/Opinion
 *   - Different THETAs for different sentiments: THETA[S]
 *   - Positive/Negative
 * Author: Yohan Jo
 */
package edu.kaist.uilab.asum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import edu.kaist.uilab.asc.Application;
import edu.kaist.uilab.asc.data.Document;
import edu.kaist.uilab.asc.data.SamplingWord;
import edu.kaist.uilab.asc.data.Sentence;
import edu.kaist.uilab.asc.data.SentiWord;
import edu.kaist.uilab.asc.util.DoubleMatrix;
import edu.kaist.uilab.asc.util.IntegerMatrix;
import edu.kaist.uilab.asc.util.Utils;

public class STO2Core {
  private int numUniqueWords; // vocabulary size
  private int numTopics; // K
  private int numSenti; // S
  private int numRealIterations;
  private int numDocuments;
  private List<String> wordList = null;
  private int numProbWords = 100;

  public String inputDir = null;
  public String outputDir = null;
  private Integer intvalTmpOutput = null;

  private double alpha;
  private double sumAlpha;
  // betas[3]: Common Words, Corresponding Lexicon, The other lexicons
  private double[] betas;
  private double[] sumBeta; // sumBeta[senti]
  private double[] gammas;
  private double sumGamma;

  public DoubleMatrix[] Phi; // Phi[senti][word][topic]
  public DoubleMatrix[] Theta; // Theta[senti][document][topic]
  public DoubleMatrix Pi;

  public List<TreeSet<Integer>> sentiWordsList;

  private IntegerMatrix[] matrixSWT;
  private IntegerMatrix[] matrixSDT;
  private IntegerMatrix matrixDS;

  private int[][] sumSTW; // sumSTW[S][T]
  private int[][] sumDST; // sumDST[D][S]
  private int[] sumDS; // sumDS[D]

  private double[][] probTable;

  private List<Document> documents;
  final private int maxSentenceLength = 50;

  public static void main(String[] args) throws Exception {
    int numTopics = 50;
    int numIterations = 2000;
    int numSenti = 2;
    int numThreads = 1;
    String inputDir = "C:/datasets/asc/reviews/ASUM";
    String outputDir = "C:/datasets/asc/reviews/ASUM/50topics";
    String dicDir = inputDir;
    double alpha = 0.1;
    double[] betas = new double[] { 0.001, 0.001, 0.000001 };
    double[] gammas = new double[] { 1.0, 1.0 };
    boolean randomInit = false;

    // String sentiFilePrefix = "SentiWords-";
    String sentiFilePrefix = "SentiStems-";
    String wordListFileName = "WordList.txt";
    String wordDocFileName = "BagOfSentences_en.txt";
    String line;
    Vector<String> wordList = new Vector<String>();
    BufferedReader wordListFile = new BufferedReader(new FileReader(new File(
        inputDir + "/" + wordListFileName)));
    while ((line = wordListFile.readLine()) != null)
      if (line != "")
        wordList.add(line);
    wordListFile.close();

    // Vector<String> docList = new Vector<String>();
    // BufferedReader docListFile = new BufferedReader(new FileReader(new
    // File(inputDir+"/"+docListFileName)));
    // while ((line = docListFile.readLine()) != null)
    // if (line != "") docList.add(line);
    // docListFile.close();

    Vector<Document> documents = new Vector<Document>();
    Application.readDocuments(documents, inputDir + "/" + wordDocFileName);

    System.out.println("Documents: " + documents.size());
    System.out.println("Unique Words: " + wordList.size());

    ArrayList<TreeSet<String>> sentiWordsStrList = new ArrayList<TreeSet<String>>();
    for (int s = 0; s < numSenti; s++) {
      String dicFilePath = dicDir + "/" + sentiFilePrefix + s + ".txt";
      if (new File(dicFilePath).exists()) {
        sentiWordsStrList.add(Utils.readWords(dicFilePath, "utf-8"));
      }
    }

    ArrayList<TreeSet<Integer>> sentiWordsList = new ArrayList<TreeSet<Integer>>(
        sentiWordsStrList.size());
    for (Set<String> sentiWordsStr : sentiWordsStrList) {
      TreeSet<Integer> sentiWords = new TreeSet<Integer>();
      for (String word : sentiWordsStr)
        sentiWords.add(wordList.indexOf(word));
      sentiWordsList.add(sentiWords);
    }

    // Print the configuration
    System.out.println("Documents: " + documents.size());
    System.out.println("Unique Words: " + wordList.size());
    System.out.println("Topics: " + numTopics);
    System.out.println("Sentiments: " + numSenti + " (dictionary: "
        + sentiWordsList.size() + ")");
    System.out.println("Alpha: " + alpha);
    System.out.println();
    System.out.println("Iterations: " + numIterations);
    System.out.println("Threads: " + numThreads);
    System.out.println("Input Dir: " + inputDir);
    System.out.println("Dictionary Dir: " + dicDir);
    System.out.println("Output Dir: " + outputDir);

    STO2Core core = new STO2Core(numTopics, numSenti, wordList, documents,
        sentiWordsList, alpha, betas, gammas);
    core.setTmpOutputFiles(inputDir, outputDir, 1000);
    core.initialization(randomInit);
    core.gibbsSampling(numIterations, numThreads);
    core.generateOutputFiles(outputDir);
  }

  public STO2Core(int numTopics, int numSenti, List<String> wordList,
      List<Document> documents, List<TreeSet<Integer>> sentiWordsList,
      double alpha, double[] betas, double[] gammas) {
    this.numTopics = numTopics;
    this.numSenti = numSenti;
    this.numUniqueWords = wordList.size();
    this.numDocuments = documents.size();
    this.documents = documents;
    this.wordList = wordList;
    this.sentiWordsList = sentiWordsList;
    this.alpha = alpha;
    this.betas = betas;
    this.gammas = gammas;
    this.sumBeta = new double[numSenti];
    probTable = new double[numTopics][numSenti];
  }

  public void initialization(boolean randomInit) {
    sumSTW = new int[numSenti][numTopics];
    sumDST = new int[numDocuments][numSenti];
    sumDS = new int[numDocuments];

    matrixSWT = new IntegerMatrix[numSenti];
    for (int i = 0; i < numSenti; i++)
      matrixSWT[i] = new IntegerMatrix(numUniqueWords, numTopics);
    matrixSDT = new IntegerMatrix[numSenti];
    for (int i = 0; i < numSenti; i++)
      matrixSDT[i] = new IntegerMatrix(numDocuments, numTopics);
    matrixDS = new IntegerMatrix(numDocuments, numSenti);

    int numTooLongSentences = 0;

    for (Document currentDoc : documents) {
      int docNo = currentDoc.getDocNo();

      for (Sentence sentence : currentDoc.getSentences()) {
        int newSenti = -1;
        int numSentenceSenti = 0;
        for (SamplingWord sWord : sentence.getWords()) {
          SentiWord word = (SentiWord) sWord;

          int wordNo = word.getWordNo();
          for (int s = 0; s < sentiWordsList.size(); s++) {
            if (sentiWordsList.get(s).contains(wordNo)) {
              if (numSentenceSenti == 0 || s != newSenti)
                numSentenceSenti++;
              word.priorSentiment = s;
              newSenti = s;
            }
          }
        }
        if (randomInit || numSentenceSenti != 1)
          newSenti = (int) (Math.random() * numSenti);
        int newTopic = (int) (Math.random() * numTopics);

        if (sentence.getWords().size() > this.maxSentenceLength)
          numTooLongSentences++;

        if (!(numSentenceSenti > 1 || sentence.getWords().size() > this.maxSentenceLength)) {
          sentence.setTopic(newTopic);
          sentence.setSenti(newSenti);

          for (SamplingWord sWord : sentence.getWords()) {
            ((SentiWord) sWord).setSentiment(newSenti);
            sWord.setTopic(newTopic);
            matrixSWT[newSenti].incValue(sWord.getWordNo(), newTopic);
            sumSTW[newSenti][newTopic]++;
          }
          matrixSDT[newSenti].incValue(docNo, newTopic);
          matrixDS.incValue(docNo, newSenti);

          sumDST[docNo][newSenti]++;
          sumDS[docNo]++;
        }
      }
    }

    System.out.println("Too Long Sentences: " + numTooLongSentences);
  }

  public void gibbsSampling(int numIterations, int numThreads) throws Exception {
    this.sumAlpha = this.alpha * this.numTopics;
    int numSentiWords = 0;
    for (Set<Integer> sentiWords : sentiWordsList)
      numSentiWords += sentiWords.size();
    double sumBetaCommon = this.betas[0]
        * (this.numUniqueWords - numSentiWords);
    for (int s = 0; s < numSenti; s++) {
      int numLexiconWords = 0;
      if (this.sentiWordsList.size() > s)
        numLexiconWords = this.sentiWordsList.get(s).size();
      this.sumBeta[s] = sumBetaCommon + this.betas[1] * numLexiconWords
          + this.betas[2] * (numSentiWords - numLexiconWords);
    }
    this.sumGamma = 0;
    for (double gamma : this.gammas)
      this.sumGamma += gamma;

    System.out.println("Gibbs sampling started (Iterations: " + numIterations
        + ", Threads: " + numThreads + ")");

    long startTime = System.currentTimeMillis();
    for (int i = 0; i < numIterations; i++) {
      System.out.print(i + " ");
      if (i % 100 == 0) {
        System.out.println();
      }
      for (Document currentDoc : documents)
        sampleForDoc(currentDoc);

      this.numRealIterations = i + 1;
      if (this.intvalTmpOutput != null
          && this.numRealIterations % this.intvalTmpOutput == 0
          && this.numRealIterations < numIterations) {
        this.Phi = STO2Util.calculatePhi(matrixSWT, sumSTW, this.betas,
            this.sumBeta, this.sentiWordsList);
        this.Theta = STO2Util.calculateTheta(matrixSDT, sumDST, this.alpha,
            this.sumAlpha);
        this.Pi = STO2Util.calculatePi(matrixDS, sumDS, this.gammas,
            this.sumGamma);
        generateOutputFiles(this.outputDir);
      }
    }
    System.out.printf("\nGibbs sampling terminated (%d).",
        (System.currentTimeMillis() - startTime) / 1000);
    this.Phi = STO2Util.calculatePhi(matrixSWT, sumSTW, this.betas,
        this.sumBeta, this.sentiWordsList);
    this.Theta = STO2Util.calculateTheta(matrixSDT, sumDST, this.alpha,
        this.sumAlpha);
    this.Pi = STO2Util.calculatePi(matrixDS, sumDS, this.gammas, this.sumGamma);
  }

  private void sampleForDoc(Document currentDoc) {
    int docNo = currentDoc.getDocNo();
    for (Sentence sentence : currentDoc.getSentences()) {
      if (sentence.getSenti() == -1
          || sentence.getWords().size() > this.maxSentenceLength)
        continue;

      Map<SamplingWord, Integer> wordCnt = sentence.getWordCnt();

      double sumProb = 0;

      int oldTopic = sentence.getTopic();
      int oldSenti = sentence.getSenti();

      matrixSDT[oldSenti].decValue(docNo, oldTopic);
      matrixDS.decValue(docNo, oldSenti);

      sumDST[docNo][oldSenti]--;
      sumDS[docNo]--;

      for (SamplingWord sWord : sentence.getWords()) {
        matrixSWT[oldSenti].decValue(sWord.getWordNo(), oldTopic);
        sumSTW[oldSenti][oldTopic]--;
      }

      // Sampling
      for (int si = 0; si < numSenti; si++) {
        boolean trim = false;

        // Fast Trimming
        for (SamplingWord sWord : wordCnt.keySet()) {
          SentiWord word = (SentiWord) sWord;
          if (word.priorSentiment != null && word.priorSentiment != si) {
            trim = true;
            break;
          }
        }
        if (trim) {
          for (int ti = 0; ti < numTopics; ti++)
            probTable[ti][si] = 0;
        } else {
          for (int ti = 0; ti < numTopics; ti++) {
            double beta0 = sumSTW[si][ti] + sumBeta[si];
            int m0 = 0;
            double expectTSW = 1;

            for (SamplingWord sWord : wordCnt.keySet()) {
              SentiWord word = (SentiWord) sWord;

              double beta;
              if (word.priorSentiment == null)
                beta = this.betas[0];
              else if (word.priorSentiment == si)
                beta = this.betas[1];
              else
                beta = this.betas[2];

              double betaw = matrixSWT[si].getValue(word.getWordNo(), ti)
                  + beta;

              int cnt = wordCnt.get(word);
              for (int m = 0; m < cnt; m++) {
                expectTSW *= (betaw + m) / (beta0 + m0);
                m0++;
              }

              // if (word.lexicon != null && word.lexicon != si && expectTSW >
              // 0) {
              // System.err.println(this.wordList.get(word.wordNo)+": "+ti+", "+si+", "+matrixTWS[ti].getValue(word.wordNo,
              // si)+", "+beta);
              // }
            }
            // probTable[ti][si] = (matrixSDT[si].getValue(docNo, ti) +
            // this.alpha) / (sumDST[docNo][si] + this.sumAlpha)
            // * (matrixDS.getValue(docNo, si) + this.gammas[si]) /
            // (sumDS[docNo] + this.sumGamma)
            // * expectTSW;
            // Fast version
            probTable[ti][si] = (matrixSDT[si].getValue(docNo, ti) + this.alpha)
                / (sumDST[docNo][si] + this.sumAlpha)
                * (matrixDS.getValue(docNo, si) + this.gammas[si]) * expectTSW;

            sumProb += probTable[ti][si];
          }
        }
      }

      int newTopic = -1, newSenti = -1;
      double randNo = Math.random() * sumProb;
      double tmpSumProb = 0;
      boolean found = false;
      for (int ti = 0; ti < numTopics; ti++) {
        for (int si = 0; si < numSenti; si++) {
          tmpSumProb += probTable[ti][si];
          if (randNo < tmpSumProb) {
            newTopic = ti;
            newSenti = si;
            found = true;
          }
          if (found)
            break;
        }
        if (found)
          break;
      }

      sentence.setTopic(newTopic);
      sentence.setSenti(newSenti);

      for (SamplingWord sWord : sentence.getWords()) {
        SentiWord word = (SentiWord) sWord;
        word.setTopic(newTopic);
        word.setSentiment(newSenti);
        matrixSWT[newSenti].incValue(word.getWordNo(), newTopic);
        sumSTW[newSenti][newTopic]++;
      }
      matrixSDT[newSenti].incValue(docNo, newTopic);
      matrixDS.incValue(docNo, newSenti);

      sumDST[docNo][newSenti]++;
      sumDS[docNo]++;
    }
  }

  public void setTmpOutputFiles(String inputDir, String outputDir, int interval)
      throws Exception {
    if (inputDir == null || outputDir == null)
      throw new Exception(
          "Should specify the input and output dirs for tmp output files");
    if (interval <= 0)
      throw new Exception(
          "The interval of writing tmp output files should be greater than 0");
    this.inputDir = inputDir;
    this.outputDir = outputDir;
    this.intvalTmpOutput = interval;
  }

  void writeClassificationSummary(DoubleMatrix pi, String file)
      throws IOException {
    // get classification accuracy for english documents
    int observedSenti, inferedSenti, numCorrect = 0;
    int numNotRated = 0, numNeutral = 0, numPos = 0, numSubjective = 0;
    for (int i = 0; i < numDocuments; i++) {
      Document document = documents.get(i);
      double rating = document.getRating();
      if (rating != 3.0 && rating != -1.0) {
        numSubjective++;
        observedSenti = rating > 3.0 ? 0 : 1;
        inferedSenti = pi.getValue(i, 0) > pi.getValue(i, 1) ? 0 : 1;
        if (observedSenti == inferedSenti) {
          numCorrect++;
        }
        if (observedSenti == 0) {
          numPos++;
        }
      } else {
        if (rating == 3.0) {
          numNeutral++;
        } else {
          numNotRated++;
        }
      }
    }
    PrintWriter out = new PrintWriter(file);
    out.println("English reviews:");
    out.printf("\tSubjective:\t%d\tpos = %d(%.2f)\n", numSubjective, numPos,
        ((double) numPos) / numSubjective);
    out.printf("\tNeutral:\t%d\n", numNeutral);
    out.printf("\tNot rated:\t%d\n", numNotRated);
    out.printf("\tAccuracy:\t%.5f\n", ((double) numCorrect) / numSubjective);
    out.println("-------------------");
    out.close();
  }

  public void generateOutputFiles(String dir) throws Exception {
    String prefix = "STO2-T" + numTopics + "-S" + numSenti + "("
        + sentiWordsList.size() + ")-A" + alpha + "-B" + betas[0];
    for (int i = 1; i < betas.length; i++)
      prefix += "," + betas[i];
    prefix += "-G" + gammas[0];
    for (int i = 1; i < numSenti; i++)
      prefix += "," + gammas[i];
    prefix += "-I" + numRealIterations;

    // Phi
    System.out.println("Writing Phi...");
    PrintWriter out = new PrintWriter(new FileWriter(new File(dir + "/"
        + prefix + "-Phi.csv")));
    for (int s = 0; s < this.numSenti; s++)
      for (int t = 0; t < this.numTopics; t++)
        out.print(",S" + s + "-T" + t);
    out.println();
    for (int w = 0; w < this.wordList.size(); w++) {
      out.print(this.wordList.get(w));
      for (int s = 0; s < this.numSenti; s++) {
        for (int t = 0; t < this.numTopics; t++) {
          out.print("," + this.Phi[s].getValue(w, t));
        }
      }
      out.println();
    }
    out.close();

    // Theta
    System.out.println("Writing Theta...");
    out = new PrintWriter(new FileWriter(new File(dir + "/" + prefix
        + "-Theta.csv")));
    for (int s = 0; s < this.numSenti; s++)
      for (int t = 0; t < this.numTopics; t++)
        out.print("S" + s + "-T" + t + ",");
    out.println();
    for (int d = 0; d < this.numDocuments; d++) {
      for (int s = 0; s < this.numSenti; s++) {
        for (int t = 0; t < this.numTopics; t++) {
          out.print(this.Theta[s].getValue(d, t) + ",");
        }
      }
      out.println();
    }
    out.close();

    // Pi
    System.out.println("Writing Pi...");
    Pi.writeMatrixToCSVFile(dir + "/" + prefix + "-Pi.csv");
    writeClassificationSummary(Pi, dir + "/classification.txt");

    // Most probable words
    System.out.println("Writing the most probable words...");
    out = new PrintWriter(new FileWriter(new File(dir + "/" + prefix
        + "-ProbWords.csv")));
    for (int s = 0; s < this.numSenti; s++)
      for (int t = 0; t < this.numTopics; t++)
        out.print("S" + s + "-T" + t + ",");
    out.println();
    int[][][] wordIndices = new int[this.numSenti][this.numTopics][this.numProbWords];
    for (int s = 0; s < this.numSenti; s++) {
      for (int t = 0; t < this.numTopics; t++) {
        Vector<Integer> sortedIndexList = this.Phi[s].getSortedColIndex(t,
            this.numProbWords);
        for (int w = 0; w < sortedIndexList.size(); w++)
          wordIndices[s][t][w] = sortedIndexList.get(w);
      }
    }
    for (int w = 0; w < this.numProbWords; w++) {
      for (int s = 0; s < this.numSenti; s++) {
        for (int t = 0; t < this.numTopics; t++) {
          int index = wordIndices[s][t][w];
          out.print(this.wordList.get(index) + " ("
              + String.format("%.3f", Phi[s].getValue(index, t)) + "),");
        }
      }
      out.println();
    }
    out.close();

    // Most probable words by term-score
    System.out.println("Writing the most probable words by termscores...");
    out = new PrintWriter(new FileWriter(new File(dir + "/" + prefix
        + "-ProbWordsByTermScore.csv")));
    for (int s = 0; s < this.numSenti; s++)
      for (int t = 0; t < this.numTopics; t++)
        out.print("S" + s + "-T" + t + ",");
    out.println();
    wordIndices = new int[this.numSenti][this.numTopics][this.numProbWords];
    DoubleMatrix[] ts = buildTermScoreMatrix(this.Phi);
    for (int s = 0; s < numSenti; s++) {
      for (int t = 0; t < numTopics; t++) {
        Vector<Integer> sortedIndexList = ts[s].getSortedColIndex(t,
            numProbWords);
        for (int w = 0; w < sortedIndexList.size(); w++)
          wordIndices[s][t][w] = sortedIndexList.get(w);
      }
    }
    for (int w = 0; w < this.numProbWords; w++) {
      for (int s = 0; s < this.numSenti; s++) {
        for (int t = 0; t < this.numTopics; t++) {
          int index = wordIndices[s][t][w];
          out.print(this.wordList.get(index) + " ("
              + String.format("%.3f", ts[s].getValue(index, t)) + "),");
        }
      }
      out.println();
    }
    out.close();

    /*
     * // Result reviews System.out.println("Visualizing reviews..."); String []
     * sentiColors = {"green","red","black"}; out = new PrintWriter(new
     * FileWriter(new File(dir + "/" + prefix + "-VisReviews.html"))); for
     * (OrderedDocument doc : this.documents) {
     * out.println("<h3>Document "+doc.getDocNo()+"</h3>"); for (Sentence
     * sentence : doc.getSentences()) { if (sentence.getSenti() < 0 ||
     * sentence.getSenti() >= this.numSenti || sentence.getWords().size() >
     * this.maxSentenceLength) continue;
     * out.print("<p style=\"color:"+sentiColors
     * [sentence.getSenti()]+";\">T"+sentence.getTopic()+":"); for (Word word :
     * sentence.getWords()) out.print(" "+this.wordList.get(word.wordNo));
     * out.println("</p>"); } } out.close(); // Sentence probabilities
     * System.out.println("Calculating sentence probabilities..."); out = new
     * PrintWriter(new FileWriter(new File(dir + "/" + prefix +
     * "-SentenceProb.csv"))); out.print("Document,Sentence,Length"); for (int s
     * = 0; s < this.numSenti; s++) for (int t = 0; t < this.numTopics; t++)
     * out.print(",S"+s+"-T"+t); out.println(); for (int d = 0; d <
     * this.documents.size(); d++) { OrderedDocument doc =
     * this.documents.get(d); for (int sen = 0; sen < doc.getSentences().size();
     * sen++) { Sentence sentence = doc.getSentences().get(sen); if
     * (sentence.numSenti > 1 || sentence.getWords().size() > 50) continue; if
     * (sentence.getWords().size() == 0) throw new Exception("WHAT???");
     * out.print(d+",\""); for (Word word : sentence.getWords())
     * out.print(this.wordList.get(word.wordNo)+" ");
     * out.print("\","+sentence.getWords().size()); double [][] prod = new
     * double[this.numSenti][this.numTopics]; double sum = 0; for (int s = 0; s
     * < this.numSenti; s++) { for (int t = 0; t < this.numTopics; t++) {
     * prod[s][t] = 1; for (Word word : sentence.getWords()) prod[s][t] *=
     * this.Phi[s].getValue(word.wordNo, t); sum += prod[s][t]; } } for (int s =
     * 0; s < this.numSenti; s++) { for (int t = 0; t < this.numTopics; t++) {
     * out.print("," + (prod[s][t] / sum)); } } out.println(); } } out.close();
     * // Sentiment lexicon words distribution
     * System.out.println("Calculating sentiment lexicon words distributions..."
     * ); out = new PrintWriter(new FileWriter(new File(dir + "/" + prefix +
     * "-SentiLexiWords.csv"))); for (Set<Integer> sentiWords :
     * this.sentiWordsList) { for (int wordNo : sentiWords) { if (wordNo < 0 ||
     * wordNo >= this.wordList.size()) continue;
     * out.print(this.wordList.get(wordNo)); for (int s = 0; s < numSenti; s++)
     * { int sum = 0; for (int t = 0; t < numTopics; t++) sum +=
     * matrixSWT[s].getValue(wordNo, t); out.print(","+sum); } out.println(); }
     * out.println(); } out.close();
     */
  }

  /**
   * Builds the term-score matrix from the inferred values of Phi.
   * 
   * @return
   */
  private DoubleMatrix[] buildTermScoreMatrix(DoubleMatrix[] phi) {
    DoubleMatrix[] termScore = new DoubleMatrix[phi.length];
    double sumOfLogs[] = new double[numUniqueWords];
    // compute the sum of logs for each word
    for (int w = 0; w < numUniqueWords; w++) {
      sumOfLogs[w] = 0.0;
      for (int s = 0; s < numSenti; s++) {
        for (int t = 0; t < numTopics; t++) {
          sumOfLogs[w] += Math.log(phi[s].getValue(w, t));
        }
      }
    }
    double score, prob;
    // int topics = numTopics * numSenti;
    // TODO(trung): this is a different from the term-score formula (with the
    // assumption that a senti-word has only one senti -> only numTopics)
    int topics = numTopics;
    for (int s = 0; s < numSenti; s++) {
      termScore[s] = new DoubleMatrix(numUniqueWords, numTopics);
      for (int t = 0; t < numTopics; t++) {
        for (int w = 0; w < numUniqueWords; w++) {
          prob = phi[s].getValue(w, t);
          score = prob * (Math.log(prob) - sumOfLogs[w] / topics);
          termScore[s].setValue(w, t, score);
        }
      }
    }
    return termScore;
  }

}