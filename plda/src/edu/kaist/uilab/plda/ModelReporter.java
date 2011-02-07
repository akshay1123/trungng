package edu.kaist.uilab.plda;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import edu.kaist.uilab.plda.data.CorpusProcessor;
import edu.kaist.uilab.plda.file.DefaultDocumentReader;

/**
 * TODO(trung):
 * 
 * 2. Test several corpus
 * 
 * @author trung nguyen
 */
public class ModelReporter {
  static String[] stopword = new String[] {
    "one",
    "two",
    "three",
    "four",
    "five",
    "six",
    "seven",
    "eight",
    "nine",
    "ten",
    "did",
    "made",
    "make",
    "do",
    "said",
    "does",
//    "billion",
//    "&lt;",
//    "mln",
//    "cts",
//    "dlrs",
//    "qtr",
//    "pct",
//    "vs",
//    "lt",
//    "reuter",
//    "shr",
//    "billion",
    "what",
    "although",
    "though",
    "too",
    "just",
    "where",
    "while",
    "then",
    "since",
    "could",
    "should",
    "throughout",
    "off",
    "still",
    "several",
    "day",
    "days",
    "our",
    "go",
    "these",
    "very",
    "next",
    "me",
    "early",
    "until",
    "ago",
    "now",
    "today",
    "yesterday",
    "tomorrow",
    "week",
    "month",
    "year",
    "years",
    "weeks",
    "months",
    "january",
    "february",
    "march",
    "april",
    "may",
    "june",
    "july",
    "august",
    "september",
    "october",
    "november",
    "december",
    "continued",
    "born",
    "died",
    "get",
    "send",
    "sent",
    "got",
    "move",
    "moved",
    "become",
    "became",
    "took",
    "spent",
    "went",
    "going",
    "doing",
    "being",
    "meet",
    "met",
    "old",
    "through",
    "soon",
    "back",
    "never",
    "name",
    "another",
    "named",
    "able",
    "how",
    "few",
    "those",
    "them",
    "herself",
    "himself",
    "told",
  };
  
  public static void main(String args[]) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader("runs.txt"));
    String line;
    int minTokenCount = 4;
    int minEntityCount = 7;
    int topStopWords = 80;
    int maxEntitiesPerDoc = 3;

    double alpha_d = 0.1;
    // increase gamma in the hope of assigning more topics to an entity
    // instead of a bad dominant topic
    double alpha_e = 0.2;
    // note: beta_d = 1: very specific (detailed) topic, wonder why?
    // beta_e = 1: so few words assigned to a topic
    double beta_d = 0.01;
    double beta_e = 0.1;
    double eta_d = 0.5;
    double eta_e = 5;
    int numDocTopics = 15;
    int numEntityTopics = 15;
    CorpusProcessor corpus = new CorpusProcessor("C:/datasets/bbchistory",
        new DefaultDocumentReader(), minTokenCount, minEntityCount,
        topStopWords, maxEntitiesPerDoc, stopword);
    //  corpus = new CorpusProcessor("/home/trung/elda/data/bbchistory",
    //  new DefaultDocumentReader(), minTokenCount, minEntityCount,
    //  topStopWords, maxEntitiesPerDoc, stopword);
    //corpus = new CorpusProcessor("/home/trung/workspace/util/nytimes/general",
    //  new NYTimesDocumentReader(), minTokenCount, minEntityCount,
    //  topStopWords, maxEntitiesPerDoc, stopword);
    corpus.process();

    while ((line = in.readLine()) != null) {
      if (line.charAt(0) != '#' ) {
        // parse the parameters
        StringTokenizer tokenizer = new StringTokenizer(line, ",");
        alpha_d = Double.parseDouble(tokenizer.nextToken());
        alpha_e = Double.parseDouble(tokenizer.nextToken());
        numDocTopics = Integer.parseInt(tokenizer.nextToken());
        numEntityTopics = Integer.parseInt(tokenizer.nextToken());
        eta_d = Double.parseDouble(tokenizer.nextToken());
        eta_e = Double.parseDouble(tokenizer.nextToken());
        beta_d = Double.parseDouble(tokenizer.nextToken());
        beta_e = Double.parseDouble(tokenizer.nextToken());
//      String outputDir = "/home/trung/elda/bbc20_tok3_stop30_ent2_iter500_maxent3";
        String outputDir = String.format("C:/elda/3history%d-%d_a%.2f-%.2f_b%.2f-%.2f_eta%.2f-%.2f",
            numDocTopics, numEntityTopics, alpha_d, alpha_e, beta_d, beta_e, eta_d, eta_e);
        System.out.println(outputDir);
//        EntityLdaGibbsSampler sampler;
        EntityLdaGibbsSampler3 sampler;
        (new File(outputDir)).mkdir();
        corpus.reportCorpus(outputDir + "/corpus.txt",
            outputDir + "/docNames.txt",
            outputDir + "/entity.txt",
            outputDir + "/docEntity.txt",
            outputDir + "/token.txt");
        
//        sampler = new EntityLdaGibbsSampler(numTopics,
//            corpus.getVocabularySize(),
//            corpus.getNumEntities(),
//            corpus.getDocumentTokens(),
//            corpus.getDocumentEntities(),
//            corpus.getCorpusEntitySet(),
//            alpha, beta, gamma);
        sampler = new EntityLdaGibbsSampler3(numDocTopics,
            numEntityTopics,
            corpus.getVocabularySize(),
            corpus.getNumEntities(),
            corpus.getDocumentTokens(),
            corpus.getDocumentEntities(),
            corpus.getCorpusEntitySet());
        sampler.setPriors(alpha_d, alpha_e, beta_d, beta_e, eta_d, eta_e);
        sampler.setSamplerParameters(5000, 200, 20, 10);
        sampler.setOutputParameters(corpus.getSymbolTable(), outputDir, 30, 10, 10);
        System.out.println("Latent Dirichlet Allocation using Gibbs Sampling.");
        sampler.doGibbsSampling(false);
      }
    }
    in.close();
  }
}
