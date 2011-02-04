package edu.kaist.uilab.plda;

import java.io.File;
import java.io.IOException;

import edu.kaist.uilab.plda.data.CorpusProcessor;
import edu.kaist.uilab.plda.file.DefaultDocumentReader;

/**
 * TODO(trung):
 * 1. Compute likelihood of the trained model
 * 2. Test several corpus
 * 
 * @author trung nguyen
 */
public class ModelReporter {
  static String[] stopword = new String[] {
//    "one",
//    "two",
//    "three",
//    "four",
//    "five",
//    "six",
//    "seven",
//    "eight",
//    "nine",
//    "ten",
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
    "you",
    "off",
    "still",
    "several",
    "day",
    "days",
    "our",
    "go",
    "these",
    "very",
    "while",
    "then",
    "since",
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
//    "continued",
//    "moved",
//    "become",
//    "them",
//    "took",
  };
  
  public static void main(String args[]) throws IOException {
    double alpha_d = 0.1;
    // increase gamma in the hope of assigning more topics to an entity
    // instead of a bad dominant topic
    double alpha_e = 0.2;
    // note: beta_d = 1: very specific (detailed) topic, wonder why?
    // beta_e = 1: so few words assigned to a topic
    double beta_d = 0.1;
    double beta_e = 0.05;
    double eta_d = 5;
    double eta_e = 0.5;
    int numDocTopics = 15;
    int numEntityTopics = 15;
    int minTokenCount = 3;
    int minEntityCount = 2;
    int topStopWords = 30;
    int maxEntitiesPerDoc = 2;
//    String outputDir = "/home/trung/elda/bbc20_tok3_stop30_ent2_iter500_maxent3";
    String outputDir = "C:/elda/3bbc15-15_a01-02_b01-005_eta5-05";
    CorpusProcessor corpus;
//    EntityLdaGibbsSampler sampler;
    EntityLdaGibbsSampler3 sampler;
    (new File(outputDir)).mkdir();
//    corpus = new CorpusProcessor("/home/trung/elda/data/bbchistory",
//        new DefaultDocumentReader(), minTokenCount, minEntityCount,
//        topStopWords, maxEntitiesPerDoc, stopword);
//    corpus = new CorpusProcessor("/home/trung/workspace/util/nytimes/general",
//        new NYTimesDocumentReader(), minTokenCount, minEntityCount,
//        topStopWords, maxEntitiesPerDoc, stopword);
    corpus = new CorpusProcessor("C:/datasets/bbchistory", new DefaultDocumentReader(),
        minTokenCount, minEntityCount, topStopWords, maxEntitiesPerDoc, stopword);
    corpus.process();
    corpus.reportCorpus(outputDir + "/corpus.txt",
        outputDir + "/docNames.txt",
        outputDir + "/entity.txt",
        outputDir + "/docEntity.txt",
        outputDir + "/token.txt");
    
//    sampler = new EntityLdaGibbsSampler(numTopics,
//        corpus.getVocabularySize(),
//        corpus.getNumEntities(),
//        corpus.getDocumentTokens(),
//        corpus.getDocumentEntities(),
//        corpus.getCorpusEntitySet(),
//        alpha, beta, gamma);
    sampler = new EntityLdaGibbsSampler3(numDocTopics,
        numEntityTopics,
        corpus.getVocabularySize(),
        corpus.getNumEntities(),
        corpus.getDocumentTokens(),
        corpus.getDocumentEntities(),
        corpus.getCorpusEntitySet());
    sampler.setPriors(alpha_d, alpha_e, beta_d, beta_e, eta_d, eta_e);
    sampler.setSamplerParameters(5000, 200, 10, 10);
    sampler.setOutputParameters(corpus.getSymbolTable(), outputDir, 30, 10, 10);
    System.out.println("Latent Dirichlet Allocation using Gibbs Sampling.");
    sampler.doGibbsSampling(false);
  }
}
