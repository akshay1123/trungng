package edu.kaist.uilab.plda;

import java.io.File;
import java.io.IOException;

import edu.kaist.uilab.plda.data.CorpusProcessor;
import edu.kaist.uilab.plda.file.DefaultDocumentReader;

/**
 * TODO(trung):
 * 1. Try beta instead of uniform
 * 2. Add new data to corpus
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
    "continued",
    "moved",
    "become",
    "them",
    "took",
  };
  
  public static void main(String args[]) throws IOException {
    double alpha = 0.1;
    double beta = 0.01;
    double gamma = 0.1;
    double eta_d = 0.5;
    double eta_e = 2;
    int numTopics = 20;
    int minTokenCount = 3;
    int minEntityCount = 2;
    int topStopWords = 30;
    int maxEntitiesPerDoc = 3;
    String outputDir = "/home/trung/elda/bbc20_tok3_stop30_ent2_iter500_maxent3";
//    String outputDir = "C:/elda/bbc20_tok3_stop30_ent2_iter500_maxent3";    
    CorpusProcessor corpus;
    EntityLdaGibbsSampler2 sampler;
    (new File(outputDir)).mkdir();
    corpus = new CorpusProcessor("/home/trung/elda/data/bbchistory",
        new DefaultDocumentReader(), minTokenCount, minEntityCount,
        topStopWords, maxEntitiesPerDoc, stopword);
//    corpus = new CorpusProcessor("/home/trung/workspace/util/nytimes/general",
//        new NYTimesDocumentReader(), minTokenCount, minEntityCount,
//        topStopWords, maxEntitiesPerDoc, stopword);
//    corpus = new CorpusProcessor("C:/datasets/bbchistory", new DefaultDocumentReader(),
//        minTokenCount, minEntityCount, topStopWords, maxEntitiesPerDoc, stopword);
    corpus.process();
    corpus.reportCorpus(outputDir + "/corpus.txt",
        outputDir + "/docNames.txt",
        outputDir + "/entity.txt",
        outputDir + "/docEntity.txt",
        outputDir + "/token.txt");
    
    sampler = new EntityLdaGibbsSampler2(numTopics,
        corpus.getVocabularySize(),
        corpus.getNumEntities(),
        corpus.getDocumentTokens(),
        corpus.getDocumentEntities(),
        corpus.getCorpusEntitySet());
    sampler.setPriors(alpha, beta, gamma, eta_d, eta_e);
    sampler.setSamplerParameters(5000, 400, 10, 10);
    sampler.setOutputParameters(corpus.getSymbolTable(), outputDir, 30, 10, 10);
    System.out.println("Latent Dirichlet Allocation using Gibbs Sampling.");
    sampler.doGibbsSampling(false);
  }
}
