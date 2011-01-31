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
//    "now",
//    "today",
    "week",
    "month",
    "year",
    "years",
    "weeks",
    "months",
    "did",
    "made",
    "make",
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
    "said",
    "what",
    "you",
    "since",
    "off",
    "still",
    "do",
//    "much",
    "several",
    "does",
    "day",
    "days",
    "our",
    "go",
    "these",
    "very",
    "while",
    "then",
    "next",
    "me",
    "ago",
  };
  
  public static void main(String args[]) throws IOException {
    double alpha = 0.1;
    double beta = 0.01;
    double gamma = 0.1;
    int numTopics = 20;
    int minTokenCount = 2;
    int minEntityCount = 2;
    int topStopWords = 30;
    int maxEntitiesPerDoc = 3;
//    String outputDir = "/home/trung/elda/nytest10_ent10_iter200_maxent5";
    String outputDir = "C:/elda/bbc20_tok2_stop20_ent2_iter3000_maxent3";    
    CorpusProcessor corpus;
    EntityLdaGibbsSampler sampler;
    (new File(outputDir)).mkdir();
//    corpus = new CorpusProcessor("data/reuterstest", new DefaultDocumentReader(),
//        minTokenCount, minEntityCount, topStopWords, maxEntitiesPerDoc);
//    corpus = new CorpusProcessor("/home/trung/elda/data/nytimes/technology", new NYTimesDocumentReader(),
//        minTokenCount, minEntityCount, topStopWords, maxEntitiesPerDoc, stopword);
    corpus = new CorpusProcessor("C:/datasets/bbchistory", new DefaultDocumentReader(),
        minTokenCount, minEntityCount, topStopWords, maxEntitiesPerDoc, stopword);
    corpus.process();
    // TODO(trung): remove after debugging
    corpus.getCorpusEntitySet().closeOutput();
    corpus.reportCorpus(outputDir + "/corpus.txt",
        outputDir + "/docNames.txt",
        outputDir + "/entity.txt",
        outputDir + "/docEntity.txt",
        outputDir + "/token.txt");
    
    sampler = new EntityLdaGibbsSampler(numTopics,
        corpus.getVocabularySize(),
        corpus.getNumEntities(),
        corpus.getDocumentTokens(),
        corpus.getDocumentEntities(),
        corpus.getCorpusEntitySet(),
        alpha,
        beta,
        gamma);
    sampler.setSamplerParameters(5000, 500, 50, 50);
    sampler.setOutputParameters(corpus.getSymbolTable(), outputDir, 30, 10, 10);
    System.out.println("Latent Dirichlet Allocation using Gibbs Sampling.");
    sampler.doGibbsSampling(false);
  }
}
