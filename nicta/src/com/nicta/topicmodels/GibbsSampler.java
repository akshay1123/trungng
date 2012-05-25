package com.nicta.topicmodels;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import com.aliasi.symbol.SymbolTable;

import edu.kaist.uilab.asc.util.TextFiles;
import edu.kaist.uilab.asc.util.Utils;

/**
 * Gibbs sampler for the Latent Dirichlet Allocation (LDA) model.
 * 
 * @author trung (trung.ngvan@gmail.com)
 * 
 */
public class GibbsSampler {
    private int numTopics; // T = numTopics
    private int vocabularySize; // V = vocabularySize
    private int numDocuments; // D = number of documents
    // documents[m][n] = (index of the n_th word in document m) = i
    int[][] documents;

    // hyper-parameters
    private double alpha = 0.1; // default value
    private double beta = 0.01; // default value

    // sampling parameters and variables
    private int numIterations = 1000;
    private int burnIn = 200;
    private int[][] z; // topic assignment for each word z[document][word]
    private int cwt[][]; // word-topic count = cwt[i][k] word i to topic k
    private int cdt[][]; // document-topic count = cdt[m][k] words in document m
    // to topic k
    private int cwtsum[]; // cwtsum[k] = # words assigned to topic k
    private int cdtsum[]; // cdtsum[m] = # words in document m
    // D x K
    private double theta[][];
    // K X V
    private double phi[][];
    SymbolTable symbolTable;
    String outputDir;

    /**
     * Constructs a new GibbsSampler with given model parameters.
     * 
     * @param documents
     *            the terms/words matrix
     * @param numTopics
     *            the number of topics
     * @param table
     *            symbol table mapping tokens and ids in the corpuso
     * @param alpha
     *            the topic prior
     * @param beta
     *            the
     */
    public GibbsSampler(int[][] documents, SymbolTable table, int numTopics,
            double alpha, double beta) {
        this.numTopics = numTopics;
        this.symbolTable = table;
        this.vocabularySize = table.numSymbols();
        this.documents = documents;
        this.alpha = alpha;
        this.beta = beta;
        this.numDocuments = documents.length;
    }

    public static void main(String args[]) throws IOException {
        String dir = "/home/trung/output/topicmodels";
        String outputDir = "/home/trung/output/topicmodels/experiments";
        int numTopics = 100;
        double alpha = 0.5;
        double beta = .001;
        boolean useAbstract = false;

        int minTokenCount = 3;
        int topStopWords = 0;
        
        List<String> stopStems = TextFiles.readLines("stopstems.txt");
        CorpusParser parser = new CorpusParser(dir + "/pdffiles",
                minTokenCount, topStopWords, stopStems, useAbstract);
        parser.parse();
        parser.reportCorpus(outputDir + "/corpus.txt");
        GibbsSampler sampler = new GibbsSampler(parser.getDocumentsAsArray(),
                parser.getSymbolTable(), numTopics, alpha, beta);
        String experimentOutput = String.format("%s/T%d-Alpha%.3f-Beta%.3f-useAbstract=%s",
                outputDir, numTopics, alpha, beta, useAbstract);
        sampler.setSamplerSettings(3000, 500, experimentOutput);
        sampler.run(200);
    }

    /**
     * Sets the parameters of the sampler.
     * 
     * @param maxIterations
     *            the number of max iterations to run (default value is 1000)
     * @param burnIn
     *            the number of iterations to be counted as burn-in period (must
     *            be at least 100, default value is 200)
     */
    public void setSamplerSettings(int maxIterations, int burnIn,
            String outputDir) {
        this.numIterations = maxIterations > 150 ? maxIterations : 150;
        this.burnIn = burnIn > 100 ? burnIn : 100;
        this.outputDir = outputDir;
    }

    /**
     * Runs the Gibbs sampler and report every <code>reportPeriod</code>
     * iterations (after burn-in).
     */
    public void run(int reportPeriod) {
        initialize();
        for (int iter = 0; iter <= numIterations; iter++) {
            System.out.print(iter + " ");
            if (iter % 100 == 0) {
                System.out.println();
            }

            // sampling each hidden variable z_i
            for (int m = 0; m < numDocuments; m++) {
                for (int n = 0; n < documents[m].length; n++) {
                    z[m][n] = sampleTopicForWord(m, n);
                }
            }

            // after burn-in & some sample lags we can collect a sample
            // note that we are not saving z[m][n] for now
            if (iter > burnIn && iter % reportPeriod == 0) {
                updateParams();
                System.err.printf("Periodic report written to %s/%d.\n",
                        outputDir, iter);
                report(iter);
            }
        }

    }

    /**
     * Writes full report of the Gibbs sampler result.
     * 
     * @param iter
     */
    public void report(int iter) {
        String dir = outputDir + "/" + iter;
        new File(dir).mkdirs();
        try {
            writeDocumentTopic(dir + "/theta.csv", theta);
            writeTopicWord(dir + "/phi.csv", phi);
            writeTopWords(dir + "/topWords.csv", phi, 100);
        } catch (IOException e) {
            System.err.println("Error writing report!");
            e.printStackTrace();
        }
    }

    /**
     * Returns the estimated theta values of this sampler.
     * 
     * @return
     */
    public double[][] getTheta() {
        return theta;
    }

    /**
     * Returns the estimated phi values of this sampler.
     * 
     * @return
     */
    public double[][] getPhi() {
        return phi;
    }

    /**
     * Writes the per-document topic distribution to <code>file</code>.
     * 
     * @param file
     * @param theta
     * @throws IOException
     */
    public void writeDocumentTopic(String file, double[][] theta)
            throws IOException {
        PrintWriter out = new PrintWriter(file);
        for (int k = 0; k < numTopics; k++) {
            out.printf("topic %d,", k);
        }
        out.println();
        for (int m = 0; m < numDocuments; m++) {
            out.printf("doc %d,", m);
            for (int k = 0; k < numTopics; k++) {
                out.printf("%.4f,", theta[m][k]);
            }
            out.println();
        }
        out.close();
    }

    /**
     * Writes the per-topic word distribution to <code>file</code>.
     * 
     * @param file
     * @param phi
     * @throws IOException
     */
    public void writeTopicWord(String file, double[][] phi) throws IOException {
        PrintWriter out = new PrintWriter(file);
        out.print(",");
        for (int k = 0; k < numTopics; k++) {
            out.printf("topic %d,", k);
        }
        out.println();
        for (int i = 0; i < vocabularySize; i++) {
            out.printf("%s,", symbolTable.idToSymbol(i));
            for (int k = 0; k < numTopics; k++) {
                out.printf("%.4f,", phi[k][i]);
            }
            out.println();
        }
        out.close();
    }

    public void writeTopWords(String file, double[][] phi, int numTopWords)
            throws IOException {
        PrintWriter out = new PrintWriter(file);
        for (int topic = 0; topic < numTopics; topic++) {
            out.printf("topic %d,", topic);
        }
        out.println();
        // ranking according to phi[k][i]
        int[][] topWordMatrix = new int[numTopics][];
        for (int topic = 0; topic < numTopics; topic++) {
            topWordMatrix[topic] = Utils.topColumns(phi, topic, numTopWords);
        }
        // write the inverse of the top word matrix (for easy visualization)
        int wordId;
        for (int i = 0; i < numTopWords; i++) {
            for (int topic = 0; topic < numTopics; topic++) {
                wordId = topWordMatrix[topic][i];
                out.printf("%s(%.5f),", symbolTable.idToSymbol(wordId),
                        phi[topic][wordId]);
            }
            out.println();
        }
        out.close();
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

        // sample values of z_i randomly ([1..numTopics] as the initial state of
        // the Markov chain
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
        theta = new double[numDocuments][numTopics];
        phi = new double[numTopics][vocabularySize];
    }

    /**
     * Updates the parameters when a new sample is collected.
     */
    private void updateParams() {
        // thetasum[][] (D x K) -- sum of samples (to return the average sample)
        double tAlpha = numTopics * alpha;
        for (int m = 0; m < numDocuments; m++) {
            for (int k = 0; k < numTopics; k++) {
                theta[m][k] = (cdt[m][k] + alpha) / (cdtsum[m] + tAlpha);
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
     * Samples a topic for the n_th word in document m.
     * 
     * @param m
     *            the document
     * @param n
     *            the index (position) of the word in this document
     * 
     * @return the topic
     */
    private int sampleTopicForWord(int m, int n) {
        int word, topic = 0;
        word = documents[m][n];
        topic = z[m][n];
        // not counting i_th word
        cwt[word][topic]--;
        cdt[m][topic]--;
        cwtsum[topic]--;
        cdtsum[m]--;

        topic = sampleTopic(m, word);
        // assign new topic to the i_th word
        z[m][n] = topic;
        cwt[word][topic]++;
        cdt[m][topic]++;
        cwtsum[topic]++;
        cdtsum[m]++;

        return topic;
    }

    /**
     * Samples a topic by sampling a value from a discrete distribution.
     */
    int sampleTopic(int docIdx, int word) {
        double[] p = new double[numTopics];
        double vBeta = vocabularySize * beta;
        double tAlpha = numTopics * alpha;
        // cumulative probability of each topic
        for (int k = 0; k < numTopics; k++) {
            p[k] = (cwt[word][k] + beta) / (cwtsum[k] + vBeta)
                    * (cdt[docIdx][k] + alpha) / (cdtsum[docIdx] + tAlpha);
            if (k > 0)
                p[k] += p[k - 1];
        }
        double u = Math.random() * p[numTopics - 1];
        // find the interval which contains u
        for (int topic = 0; topic < numTopics; topic++) {
            if (u < p[topic]) {
                return topic;
            }
        }

        return -1;
    }
}
